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
        selectHwpPartialCauseList(result);
        if ("HWP".equals(result.path("format").asText()) && List.of("1.0.6", "1.0.7", "1.0.8", "1.0.9", "1.0.10").contains(result.path("extractorVersion").asText())
                && !result.has("hwpPartialCauses")) throw new IOException("INVALID_HWP_PARTIAL_DIAGNOSTIC");
        selectHwpxStructureDetails(result);
    }
    private static final List<String> HWP_PARTIAL_CAUSES = List.of(
            "UNATTACHED_PARAGRAPH", "PARAGRAPH_LEVEL_GAP", "UNATTACHED_TEXT", "CONTROL_LEVEL_GAP",
            "UNATTACHED_CONTROL", "UNSUPPORTED_RECORD", "LOOSE_STRUCTURE", "MULTIPLE_TEXT_RECORDS",
            "UNSUPPORTED_INLINE_CONTROL", "TABLE_ANCHOR_TYPE", "MISSING_CONTROL", "UNANCHORED_CONTROL",
            "UNSUPPORTED_CONTROL", "REPLACEMENT_CHARACTER", "TABLE_CONTROL_HEADER", "TABLE_PARAGRAPH_LEVEL",
            "TABLE_PARAGRAPH_WITHOUT_CELL", "TABLE_METADATA_INVALID", "TABLE_METADATA_MISSING", "TABLE_LOOSE_STRUCTURE",
            "CELL_HEADER_INVALID", "CELL_GEOMETRY_INVALID", "CELL_PARAGRAPH_COUNT", "CELL_ORDER_INVALID",
            "CELL_PARAGRAPH_HEADER", "CELL_OVERLAP", "TABLE_PARAGRAPH_COUNT", "TABLE_COVERAGE", "TABLE_ROW_COUNTS",
            "FIELD_HEADER_INVALID", "FIELD_RANGE_INVALID");
    /** 구 IPC는 선택 필드를 생략할 수 있다. 진단이 있으면 고정 코드·범위·순서·품질을 검증한다. */
    public static JsonNode selectHwpPartialCauseList(JsonNode result) throws IOException {
        if (result == null || !result.has("hwpPartialCauses")) return null;
        final String error="INVALID_HWP_PARTIAL_DIAGNOSTIC";
        JsonNode causes=result.path("hwpPartialCauses");
        String quality=result.path("qualityCode").asText();
        if (!"HWP".equals(result.path("format").asText()) || !causes.isArray() || causes.size()>HWP_PARTIAL_CAUSES.size()
                || !List.of("COMPLETE_TEXT","PARTIAL_TEXT","OCR_REQUIRED").contains(quality)
                || ("COMPLETE_TEXT".equals(quality) && !causes.isEmpty())
                || ("PARTIAL_TEXT".equals(quality) && causes.isEmpty())) throw new IOException(error);
        int previous=-1;
        for (var cause:causes) {
            int index=HWP_PARTIAL_CAUSES.indexOf(cause.path("code").asText());
            JsonNode count=cause.path("count");
            if (!cause.isObject() || cause.size()!=2 || !cause.path("code").isTextual() || index<=previous
                    || !count.isIntegralNumber() || !count.canConvertToInt() || count.intValue()<1 || count.intValue()>33_554_432)
                throw new IOException(error);
            previous=index;
        }
        return causes.deepCopy();
    }
    /** 격리 IPC의 수치 진단만 허용한다. 원문·가변 코드·미지 필드는 외부 보고로 전달하지 않는다. */
    public static JsonNode selectHwpStructureDetails(JsonNode result) throws IOException {
        if (result == null || !result.has("hwpStructure")) return null;
        JsonNode value = result.path("hwpStructure");
        if (!"HWP".equals(result.path("format").asText()) || !value.isObject() || value.size() != (value.has("controlHeaders")?5:4)
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
        if(value.has("controlHeaders"))validateControlHeaders(value.path("controlHeaders"),types);
        return value.deepCopy();
    }
    private static void validateControlHeaders(JsonNode headers,JsonNode types) throws IOException {
        final String error="INVALID_HWP_STRUCTURE_DIAGNOSTIC";
        if(!headers.isArray()||headers.size()>128)throw new IOException(error);
        long expected=0,sum=0;for(var type:types)if(type.path("tagId").intValue()==71)expected=type.path("count").intValue();
        String previousKind="",previousShape="";int previousBytes=-1,previousTail=-1;
        for(var row:headers) {
            String kind=row.path("kind").asText(),shape=row.path("shape").asText();
            if(!row.isObject()||row.size()!=5||!row.path("kind").isTextual()||!row.path("shape").isTextual()
                    ||!List.of("TABLE","SECTION","COLUMN","HYPERLINK","OTHER").contains(kind)
                    ||!List.of("NOT_TABLE","COMMON_ONLY","FIXED_ONLY","SHORT","DECLARED_TOO_LONG","EXTENDED_EXACT","EXTRA_ZERO","EXTRA_NONZERO").contains(shape))throw new IOException(error);
            int bytes=selectBoundedDiagnosticInt(row.path("bytes"),0,8388608),tail=selectBoundedDiagnosticInt(row.path("tailBytes"),0,8388608);
            sum+=selectBoundedDiagnosticInt(row.path("count"),1,33554432);
            int order=kind.compareTo(previousKind);if(order==0){order=Integer.compare(bytes,previousBytes);if(order==0){order=shape.compareTo(previousShape);if(order==0)order=Integer.compare(tail,previousTail);}}
            if(order<=0||tail>bytes)throw new IOException(error);
            boolean extra=List.of("EXTRA_ZERO","EXTRA_NONZERO").contains(shape);
            if(!"TABLE".equals(kind)) {if(!"NOT_TABLE".equals(shape)||tail!=0)throw new IOException(error);}
            else if("NOT_TABLE".equals(shape)||("COMMON_ONLY".equals(shape)&&bytes!=40)||("FIXED_ONLY".equals(shape)&&bytes!=44)
                    ||("SHORT".equals(shape)&&(bytes>=46||bytes==40||bytes==44))
                    ||(List.of("DECLARED_TOO_LONG","EXTENDED_EXACT","EXTRA_ZERO","EXTRA_NONZERO").contains(shape)&&bytes<46)
                    ||(extra?(tail<1||bytes-tail<46||(bytes-tail)%2!=0):tail!=0)
                    ||("EXTENDED_EXACT".equals(shape)&&bytes%2!=0))throw new IOException(error);
            previousKind=kind;previousBytes=bytes;previousShape=shape;previousTail=tail;
        }
        if(sum!=expected)throw new IOException(error);
    }
    private static int selectBoundedDiagnosticInt(JsonNode value, int minimum, int maximum) throws IOException {
        if (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < minimum || value.intValue() > maximum)
            throw new IOException("INVALID_HWP_STRUCTURE_DIAGNOSTIC");
        return value.intValue();
    }
    /** HWPX 진단은 선택적이다. 존재하면 형식·수치·품질·실제 대체 문자 개수를 함께 검증한다. */
    public static JsonNode selectHwpxStructureDetails(JsonNode result) throws IOException {
        if (result == null || !result.has("hwpxStructure")) return null;
        final String code = "INVALID_HWPX_STRUCTURE_DIAGNOSTIC";
        JsonNode value = result.path("hwpxStructure");
        var fields = List.of("sectionCount", "paragraphCount", "pictureCount", "oleCount", "equationCount", "replacementCharacterCount");
        if (!"HWPX".equals(result.path("format").asText()) || !value.isObject() || value.size() != fields.size()
                || !result.path("text").isTextual()) throw new IOException(code);
        for (String field : fields) {
            var number = value.path(field);
            int maximum = "sectionCount".equals(field) ? 2000 : "replacementCharacterCount".equals(field) ? 1_000_000 : 33_554_432;
            if (!number.isIntegralNumber() || !number.canConvertToInt() || number.intValue() < 0 || number.intValue() > maximum)
                throw new IOException(code);
        }
        String text = result.path("text").textValue(), quality = result.path("qualityCode").asText();
        long causes = (long) value.path("pictureCount").intValue() + value.path("oleCount").intValue()
                + value.path("equationCount").intValue() + value.path("replacementCharacterCount").intValue();
        if (value.path("sectionCount").intValue() < 1 || value.path("paragraphCount").intValue() < value.path("sectionCount").intValue()
                || value.path("replacementCharacterCount").intValue() != text.codePoints().filter(point -> point == 0xfffd).count()
                || !(text.isEmpty() ? "OCR_REQUIRED" : causes > 0 ? "PARTIAL_TEXT" : "COMPLETE_TEXT").equals(quality))
            throw new IOException(code);
        return value.deepCopy();
    }
    private JsonNode selectFailure(String code) { return mapper.createObjectNode().put("qualityCode",code).put("errorCode",code); }
    private static void deleteProcessTree(Process process) {
        process.descendants().forEach(handle -> { if (handle.isAlive()) handle.destroyForcibly(); });
        if (process.isAlive()) process.destroyForcibly();
        try { process.waitFor(2,TimeUnit.SECONDS); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
    }
}
