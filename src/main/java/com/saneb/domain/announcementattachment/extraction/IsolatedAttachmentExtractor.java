package com.saneb.domain.announcementattachment.extraction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Linux의 빈 파일시스템·PID/네트워크 namespace와 address-space 제한을 모두 요구한다. */
@Component
public class IsolatedAttachmentExtractor {
    private static final int MAX_OUTPUT_BYTES = 8 * 1024 * 1024;
    private final Path distribution;
    private final ObjectMapper mapper;
    public IsolatedAttachmentExtractor(ObjectMapper mapper,
            @Value("${saneb.announcement-attachment.extractor-root:/opt/saneb/attachment-extractor}") String root) {
        this.mapper = mapper;
        this.distribution = Path.of(root).toAbsolutePath().normalize();
    }

    public JsonNode selectExtraction(Path input) throws IOException {
        if (!System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("linux")
                || !Files.isExecutable(Path.of("/usr/bin/bwrap"))
                || !Files.isExecutable(Path.of("/usr/bin/prlimit"))) return selectFailure("ISOLATION_UNAVAILABLE");
        if (!Files.isRegularFile(input,LinkOption.NOFOLLOW_LINKS) || Files.size(input)>20L*1024*1024)
            return selectFailure("LIMIT_EXCEEDED");
        Path javaHome = Path.of(System.getProperty("java.home")).toRealPath();
        Path lib = distribution.resolve("lib").toRealPath();
        if (!lib.startsWith(distribution.toRealPath())) return selectFailure("ISOLATION_UNAVAILABLE");
        List<String> command = selectCommand(javaHome,lib,input.toRealPath());
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().clear();
        builder.environment().put("LANG","C.UTF-8");
        builder.environment().put("HOME","/tmp");
        builder.directory(input.toAbsolutePath().getParent().toFile());
        builder.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process process = builder.start();
        try (var readers = Executors.newVirtualThreadPerTaskExecutor()) {
            var output = readers.submit(() -> {
                try (var stream=process.getInputStream(); var bytes=new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    for (int count; (count=stream.read(buffer))!=-1;) {
                        if (bytes.size()+count>MAX_OUTPUT_BYTES) { deleteProcessTree(process); throw new IOException("LIMIT_EXCEEDED"); }
                        bytes.write(buffer,0,count);
                    }
                    return bytes.toByteArray();
                }
            });
            try {
                if (!process.waitFor(30,TimeUnit.SECONDS)) { deleteProcessTree(process); return selectFailure("TIMEOUT"); }
                if (process.exitValue()!=0) return selectFailure("FAILED");
                JsonNode result=mapper.readTree(output.get(2,TimeUnit.SECONDS));
                validateResult(result);
                return result;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt(); return selectFailure("CANCELLED");
            } catch (Exception exception) {
                return selectFailure("FAILED");
            } finally { deleteProcessTree(process); }
        }
    }

    static List<String> selectCommand(Path javaHome,Path library,Path input) throws IOException {
        var args = new ArrayList<>(List.of("/usr/bin/prlimit","--as=536870912","--cpu=30","--fsize=8388608","--",
                "/usr/bin/bwrap","--die-with-parent","--new-session","--unshare-all","--cap-drop","ALL",
                "--clearenv","--setenv","LANG","C.UTF-8","--setenv","HOME","/tmp",
                // glibc의 thread별 arena 예약이 제한된 주소 공간을 소진하지 않도록 고정한다.
                "--setenv","MALLOC_ARENA_MAX","1",
                "--ro-bind",javaHome.toString(),"/jre","--ro-bind",library.toString(),"/extractor",
                "--ro-bind",input.toString(),"/input.bin","--proc","/proc","--dev","/dev","--tmpfs","/tmp"));
        // Java 런타임에 필요한 시스템 공유 라이브러리만 읽기 전용으로 제공한다. /home, /var, 환경파일은 없음.
        for (String systemLib : List.of("/lib","/lib64","/usr/lib/x86_64-linux-gnu","/usr/lib/aarch64-linux-gnu")) {
            if (Files.exists(Path.of(systemLib))) args.addAll(List.of("--ro-bind",systemLib,systemLib));
        }
        if (Files.exists(Path.of("/etc/ld.so.cache"))) args.addAll(List.of("--ro-bind","/etc/ld.so.cache","/etc/ld.so.cache"));
        // Ubuntu JDK의 java.security는 /etc 아래의 파일을 가리킬 수 있다. 전체 /etc는 노출하지 않는다.
        Path javaSecurity=javaHome.resolve("conf/security/java.security");
        if (Files.isRegularFile(javaSecurity)) {
            Path resolvedSecurity=javaSecurity.toRealPath();
            if (!resolvedSecurity.startsWith(javaHome))
                args.addAll(List.of("--ro-bind",resolvedSecurity.toString(),resolvedSecurity.toString()));
        }
        // 512 MiB 주소 공간에는 heap 외 JVM·metaspace·공유 라이브러리 예약도 포함된다.
        // Ubuntu Java 21 실증에서 256 MiB heap은 VM 초기화에 실패하므로 128 MiB로 제한한다.
        args.addAll(List.of("--chdir","/tmp","--","/jre/bin/java","-Xms16m","-Xmx128m",
                "-XX:MaxMetaspaceSize=64m","-XX:CompressedClassSpaceSize=16m","-XX:ReservedCodeCacheSize=32m",
                "-Xss256k","-XX:ActiveProcessorCount=1","-XX:+UseSerialGC","-Djava.awt.headless=true",
                "-cp","/extractor/*","com.saneb.extractor.AttachmentExtractorMain","/input.bin"));
        return List.copyOf(args);
    }
    private void validateResult(JsonNode result) throws IOException {
        if (result==null || !result.isObject() || !List.of("COMPLETE_TEXT","PARTIAL_TEXT","OCR_REQUIRED",
                "ENCRYPTED","CORRUPT","UNSUPPORTED","LIMIT_EXCEEDED").contains(result.path("qualityCode").asText()))
            throw new IOException("INVALID_EXTRACTOR_RESULT");
        String text=result.path("text").asText("");
        int length=text.codePointCount(0,text.length());
        if (length>1_000_000 || text.indexOf('\0')>=0 || !result.path("blocks").isArray()
                || result.path("blocks").size()>20000) throw new IOException("INVALID_EXTRACTOR_RESULT");
        int previous=0;
        for (JsonNode block : result.path("blocks")) {
            int start=block.path("startOffset").asInt(-1), end=block.path("endOffset").asInt(-1);
            if (start<previous || end<=start || end>length || block.path("locator").asText().length()>300
                    || !block.path("scopeReliable").isBoolean()) throw new IOException("INVALID_EXTRACTOR_RESULT");
            previous=end;
        }
        if ("COMPLETE_TEXT".equals(result.path("qualityCode").asText()) && (text.isBlank() || result.path("blocks").isEmpty()))
            throw new IOException("INVALID_EXTRACTOR_RESULT");
        selectHwpStructureDetails(result);
    }
    /** 격리 IPC의 수치 진단만 허용한다. 원문·가변 코드·미지 필드는 외부 보고로 전달하지 않는다. */
    public static JsonNode selectHwpStructureDetails(JsonNode result) throws IOException {
        if (result == null || !result.has("hwpStructure")) return null;
        JsonNode value = result.path("hwpStructure");
        if (!"HWP".equals(result.path("format").asText()) || !value.isObject() || value.size() != 4
                || !value.has("sectionCount") || !value.has("recordCount")
                || !value.has("maximumLevel") || !value.has("recordTypes"))
            throw new IOException("INVALID_HWP_STRUCTURE_DIAGNOSTIC");
        selectBoundedDiagnosticInt(value.path("sectionCount"), 1, 33_554_432);
        int count = selectBoundedDiagnosticInt(value.path("recordCount"), 0, 33_554_432);
        int level = selectBoundedDiagnosticInt(value.path("maximumLevel"), 0, 1023);
        JsonNode types = value.path("recordTypes");
        if (!types.isArray() || types.size() > 1024 || (count == 0 && level != 0))
            throw new IOException("INVALID_HWP_STRUCTURE_DIAGNOSTIC");
        long sum = 0;
        int previousTag = -1;
        for (JsonNode type : types) {
            if (!type.isObject() || type.size() != 2 || !type.has("tagId") || !type.has("count"))
                throw new IOException("INVALID_HWP_STRUCTURE_DIAGNOSTIC");
            int tag = selectBoundedDiagnosticInt(type.path("tagId"), 0, 1023);
            if (tag <= previousTag) throw new IOException("INVALID_HWP_STRUCTURE_DIAGNOSTIC");
            previousTag = tag;
            sum += selectBoundedDiagnosticInt(type.path("count"), 1, 33_554_432);
        }
        if (sum != count) throw new IOException("INVALID_HWP_STRUCTURE_DIAGNOSTIC");
        return value.deepCopy();
    }
    private static int selectBoundedDiagnosticInt(JsonNode value, int minimum, int maximum) throws IOException {
        if (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < minimum || value.intValue() > maximum)
            throw new IOException("INVALID_HWP_STRUCTURE_DIAGNOSTIC");
        return value.intValue();
    }
    private JsonNode selectFailure(String code) { return mapper.createObjectNode().put("qualityCode",code).put("errorCode",code); }
    private static void deleteProcessTree(Process process) {
        process.descendants().forEach(handle -> { if (handle.isAlive()) handle.destroyForcibly(); });
        if (process.isAlive()) process.destroyForcibly();
        try { process.waitFor(2,TimeUnit.SECONDS); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
    }
}
