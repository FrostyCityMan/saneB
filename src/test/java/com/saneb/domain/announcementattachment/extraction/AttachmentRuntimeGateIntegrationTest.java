package com.saneb.domain.announcementattachment.extraction;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

/** 명시적 task에서는 Windows/격리 부재도 실패다. 운영 DB/외부 URL/클라이언트의 성공값을 사용하지 않는다. */
@EnabledIfEnvironmentVariable(named="SANEB_ATTACHMENT_RUNTIME_QA", matches="true")
class AttachmentRuntimeGateIntegrationTest {
    @TempDir Path temporary;

    @Test @Timeout(110)
    void installedHwpParserRejectsRecordBombThenRecoversAndRemovesOriginals() throws Exception {
        String distribution = System.getProperty("saneb.attachment-qa.extractor-root");
        assertThat(distribution).isNotBlank();
        var extractor = new IsolatedAttachmentExtractor(new ObjectMapper(), distribution);
        var storage = new AttachmentTemporaryStorage(temporary.resolve("storage").toString());
        assertThat(selectFixtureResult(extractor, storage, "/hwp-record-limit-qa/paragraphs-20000.hwp")
                .path("qualityCode").asText()).isEqualTo("OCR_REQUIRED");
        var rejected = selectFixtureResult(extractor, storage, "/hwp-record-limit-qa/paragraphs-20001.hwp");
        assertThat(rejected.path("qualityCode").asText()).isEqualTo("LIMIT_EXCEEDED");
        assertThat(rejected.path("text").isNull()).isTrue();
        assertThat(rejected.path("errorCode").asText()).isEqualTo("LIMIT_EXCEEDED");
        assertThat(rejected.path("blocks").isArray()).isTrue();
        assertThat(rejected.path("blocks").size()).isZero();
        var recovered = selectFixtureResult(extractor, storage, "/attachment-runtime-qa/AR-003.bin");
        assertThat(recovered.path("qualityCode").asText()).isEqualTo("COMPLETE_TEXT");
        assertThat(recovered.path("blocks").size()).isPositive();
        validateStorageEmpty();
    }

    private JsonNode selectFixtureResult(IsolatedAttachmentExtractor extractor, AttachmentTemporaryStorage storage, String resource) throws Exception {
        Path original;
        JsonNode result;
        try (var workspace = storage.insertWorkspace(UUID.randomUUID(), UUID.randomUUID())) {
            original = workspace.selectBinaryPath();
            try (var input = getClass().getResourceAsStream(resource)) {
                assertThat(input).as("시험 전용 합성 입력이 있어야 합니다.").isNotNull();
                Files.copy(input, original);
            }
            result = extractor.selectExtraction(original);
        }
        assertThat(Files.exists(original)).isFalse();
        assertThat(Files.exists(original.getParent())).isFalse();
        return result;
    }

    @Test @Timeout(60)
    void childHeapExhaustionLeavesParentAliveAndOwnedOriginalsRemoved() throws Exception {
        Path distribution = selectFaultDistribution(temporary);
        var extractor = new IsolatedAttachmentExtractor(new ObjectMapper(), distribution.toString());
        var storage = new AttachmentTemporaryStorage(temporary.resolve("storage").toString());
        // 실제 128 MiB 자식 heap에서 256 MiB 단일 할당을 거부했는지 먼저 확인한다.
        var observed = selectFaultResult(extractor, storage, "OOM_REPORT");
        assertThat(observed.path("qualityCode").asText()).isEqualTo("LIMIT_EXCEEDED");
        assertThat(observed.path("syntheticHeapLimitReached").asBoolean()).isTrue();
        // 같은 코드의 비정상 종료는 정상 추출로 승격하지 않고, 부모는 다음 작업을 계속한다.
        var failed = selectFaultResult(extractor, storage, "OOM_FATAL");
        assertThat(failed.path("errorCode").asText()).isEqualTo("FAILED");
        assertThat(selectFaultResult(extractor, storage, "RECOVER").path("qualityCode").asText()).isEqualTo("COMPLETE_TEXT");
        validateStorageEmpty();
    }

    @Test @Timeout(75)
    void actualThirtySecondTimeoutKillsObservedChildTreeAndReleasesOwnedOriginal() throws Exception {
        Path distribution = selectFaultDistribution(temporary);
        var extractor = new IsolatedAttachmentExtractor(new ObjectMapper(), distribution.toString());
        var storage = new AttachmentTemporaryStorage(temporary.resolve("storage").toString());
        String marker = "saneb-synthetic-child-" + UUID.randomUUID();
        var executor = Executors.newSingleThreadExecutor();
        var execution = executor.submit(() -> selectFaultResult(extractor, storage, "TIMEOUT\n" + marker));
        List<ProcessHandle> ownedChildren = List.of();
        long started = System.nanoTime();
        try {
            long discoveryDeadline = started + TimeUnit.SECONDS.toNanos(12);
            while (ownedChildren.isEmpty() && !execution.isDone() && System.nanoTime() < discoveryDeadline) {
                try (var descendants = ProcessHandle.current().descendants()) {
                    ownedChildren = descendants.filter(handle -> selectOwnedChild(handle, marker)).toList();
                }
                if (ownedChildren.isEmpty()) Thread.sleep(25);
            }
            assertThat(ownedChildren.size()).as("호스트에서 이번 격리 작업의 실제 자식 JVM을 관측해야 합니다.").isEqualTo(1);
            var result = execution.get(45, TimeUnit.SECONDS);
            assertThat(result.path("errorCode").asText()).isEqualTo("TIMEOUT");
            assertThat(Duration.ofNanos(System.nanoTime() - started)).isBetween(Duration.ofSeconds(29), Duration.ofSeconds(60));
            for (var child : ownedChildren) {
                long exitDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while (child.isAlive() && System.nanoTime() < exitDeadline) Thread.sleep(25);
                assertThat(child.isAlive()).as("시간 초과 뒤 격리 자식 JVM이 남아 있으면 안 됩니다.").isFalse();
            }
            assertThat(selectFaultResult(extractor, storage, "RECOVER").path("qualityCode").asText()).isEqualTo("COMPLETE_TEXT");
            validateStorageEmpty();
        } finally {
            execution.cancel(true);
            // 실패한 시험도 이번 JVM의 고유 표식 자식만 정리한다. 다른 프로세스는 건드리지 않는다.
            for (var child : ownedChildren) if (child.isAlive() && selectOwnedChild(child, marker)) child.destroyForcibly();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).as("시험의 호출 스레드 종료").isTrue();
        }
    }

    private static boolean selectOwnedChild(ProcessHandle handle, String marker) {
        return Arrays.asList(handle.info().arguments().orElse(new String[0])).contains(marker);
    }

    private static JsonNode selectFaultResult(IsolatedAttachmentExtractor extractor, AttachmentTemporaryStorage storage, String scenario) throws Exception {
        Path original;
        JsonNode result;
        try (var workspace = storage.insertWorkspace(UUID.randomUUID(), UUID.randomUUID())) {
            original = workspace.selectBinaryPath();
            Files.writeString(original, scenario);
            result = extractor.selectExtraction(original);
        }
        assertThat(Files.exists(original)).as("소유한 합성 원본 정리").isFalse();
        assertThat(Files.exists(original.getParent())).as("소유한 임시 작업 폴더 정리").isFalse();
        return result;
    }

    private void validateStorageEmpty() throws Exception {
        try (var paths = Files.list(temporary.resolve("storage/announcement-attachment-tmp"))) {
            assertThat(paths.map(path -> path.getFileName().toString()).toList()).containsExactlyInAnyOrder(".owner", ".quota.lock");
        }
    }
    @Test @Timeout(60)
    void productionLauncherBlocksParentEnvironmentHostFilesAndHostNetwork() throws Exception {
        assertThat(System.getenv("SANEB_ATTACHMENT_SANDBOX_CANARY")).isEqualTo("synthetic-parent-only");
        Path hostCanary = temporary.resolve("host-only.txt");
        Files.writeString(hostCanary, "synthetic-host-only");
        Path distribution = selectCanaryDistribution(temporary);
        Path input = temporary.resolve("canary-input.txt");
        try (var endpoint = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            endpoint.setSoTimeout(200);
            Files.writeString(input, hostCanary.toAbsolutePath() + "\n" + endpoint.getLocalPort());
            var result = new IsolatedAttachmentExtractor(new ObjectMapper(), distribution.toString()).selectExtraction(input);
            assertThat(result.path("qualityCode").asText()).isEqualTo("COMPLETE_TEXT");
            assertThat(result.path("parentEnvironmentAbsent").asBoolean()).isTrue();
            assertThat(result.path("hostFileAbsent").asBoolean()).isTrue();
            assertThat(result.path("hostNetworkBlocked").asBoolean()).isTrue();
            assertThat(result.path("inputReadable").asBoolean()).isTrue();
            assertThat(result.path("inputReadOnly").asBoolean()).isTrue();
            assertThat(result.path("libraryReadOnly").asBoolean()).isTrue();
            assertThat(result.path("privateTemporaryWritable").asBoolean()).isTrue();
            assertThat(result.path("runtimeReadable").asBoolean()).isTrue();
            assertThatThrownBy(() -> {
                try (var connection = endpoint.accept()) {
                    fail("격리 프로세스가 호스트 loopback에 연결했습니다.");
                }
            }).isInstanceOf(SocketTimeoutException.class);
        }
        assertThat(Files.readString(hostCanary)).isEqualTo("synthetic-host-only");
        assertThat(Files.readString(input)).startsWith(hostCanary.toAbsolutePath() + "\n");
        assertThat(distribution.resolve("lib/forbidden-write")).doesNotExist();
    }

    static Path selectCanaryDistribution(Path temporary) throws Exception {
        return selectProbeDistribution(temporary, """
                package com.saneb.extractor;
                import java.nio.file.*;
                import java.net.*;
                import java.io.IOException;
                public class AttachmentExtractorMain {
                    public static void main(String[] args) throws Exception {
                        var input = Path.of(args[0]);
                        var lines = Files.readAllLines(input);
                        boolean absent = System.getenv("SANEB_ATTACHMENT_SANDBOX_CANARY") == null;
                        boolean hostAbsent = !Files.exists(Path.of(lines.get(0)));
                        boolean blocked;
                        try (var socket = new Socket()) {
                            socket.connect(new InetSocketAddress("127.0.0.1", Integer.parseInt(lines.get(1))), 1000);
                            blocked = false;
                        } catch (IOException expected) { blocked = true; }
                        boolean inputReadOnly = cannotWrite(input);
                        boolean libraryReadOnly = cannotWrite(Path.of("/extractor/forbidden-write"));
                        Path local = Path.of("/tmp/probe-private.txt");
                        Files.writeString(local, "synthetic-private");
                        boolean privateWritable = Files.readString(local).equals("synthetic-private");
                        Files.delete(local);
                        boolean runtimeReadable = Files.isReadable(Path.of("/jre/release"));
                        System.out.println("{\\"qualityCode\\":\\"COMPLETE_TEXT\\",\\"text\\":\\"probe\\","
                            + "\\"blocks\\":[{\\"startOffset\\":0,\\"endOffset\\":5,\\"scopeReliable\\":false}],"
                            + "\\"parentEnvironmentAbsent\\":" + absent + ",\\"hostFileAbsent\\":" + hostAbsent
                            + ",\\"hostNetworkBlocked\\":" + blocked + ",\\"inputReadable\\":" + (lines.size() == 2)
                            + ",\\"inputReadOnly\\":" + inputReadOnly + ",\\"libraryReadOnly\\":" + libraryReadOnly
                            + ",\\"privateTemporaryWritable\\":" + privateWritable + ",\\"runtimeReadable\\":" + runtimeReadable + "}");
                    }
                    private static boolean cannotWrite(Path path) {
                        try { Files.writeString(path, "forbidden"); return false; }
                        catch (IOException expected) { return true; }
                    }
                }
                """);
    }

    static Path selectFaultDistribution(Path temporary) throws Exception {
        return selectProbeDistribution(temporary, """
                package com.saneb.extractor;
                import java.nio.file.*;
                public class AttachmentExtractorMain {
                    private static volatile Object allocated;
                    public static void main(String[] args) throws Exception {
                        if (args.length == 2 && "HOLD".equals(args[0])) {
                            Thread.sleep(120000); return;
                        }
                        var input = Files.readAllLines(Path.of(args[0]));
                        String mode = input.get(0);
                        if ("TIMEOUT".equals(mode)) {
                            new ProcessBuilder("/jre/bin/java", "-Xms8m", "-Xmx16m",
                                "-XX:MaxMetaspaceSize=32m", "-XX:CompressedClassSpaceSize=8m",
                                "-XX:ReservedCodeCacheSize=16m", "-Xss256k", "-XX:ActiveProcessorCount=1",
                                "-XX:+UseSerialGC", "-cp", "/extractor/*",
                                "com.saneb.extractor.AttachmentExtractorMain", "HOLD", input.get(1))
                                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                                .redirectError(ProcessBuilder.Redirect.DISCARD).start();
                            Thread.sleep(120000); return;
                        }
                        if ("OOM_REPORT".equals(mode) || "OOM_FATAL".equals(mode)) {
                            try {
                                allocated = new byte[256 * 1024 * 1024];
                                throw new AssertionError("HEAP_LIMIT_NOT_ENFORCED");
                            } catch (OutOfMemoryError expected) {
                                if ("OOM_FATAL".equals(mode)) { System.exit(73); return; }
                                System.out.println("{\\"qualityCode\\":\\"LIMIT_EXCEEDED\\",\\"text\\":\\"\\","
                                    + "\\"blocks\\":[],\\"syntheticHeapLimitReached\\":true}");
                                return;
                            }
                        }
                        if (!"RECOVER".equals(mode)) throw new IllegalArgumentException("UNKNOWN_SCENARIO");
                        System.out.println("{\\"qualityCode\\":\\"COMPLETE_TEXT\\",\\"text\\":\\"probe\\","
                            + "\\"blocks\\":[{\\"startOffset\\":0,\\"endOffset\\":5,\\"scopeReliable\\":false}]}");
                    }
                }
                """);
    }

    private static Path selectProbeDistribution(Path temporary, String sourceText) throws Exception {
        Path source = temporary.resolve("AttachmentExtractorMain.java");
        Files.writeString(source, sourceText);
        Path classes = Files.createDirectory(temporary.resolve("canary-classes"));
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler).as("격리 합성 probe를 컴파일할 Java 21 JDK가 필요합니다.").isNotNull();
        try (var diagnostics = new ByteArrayOutputStream()) {
            assertThat(compiler.run(null, diagnostics, diagnostics, "--release", "21", "-d", classes.toString(), source.toString()))
                    .as("합성 probe 컴파일 결과(원문·경로는 로그에 출력하지 않음)").isZero();
        }
        Path distribution = Files.createDirectory(temporary.resolve("canary-distribution"));
        Path library = Files.createDirectory(distribution.resolve("lib"));
        try (var archive = new JarOutputStream(Files.newOutputStream(library.resolve("canary.jar")))) {
            String name = "com/saneb/extractor/AttachmentExtractorMain.class";
            archive.putNextEntry(new JarEntry(name));
            Files.copy(classes.resolve(name), archive);
            archive.closeEntry();
        }
        return distribution;
    }

    @Test void installedLinuxProcessChecksAllFixturesIncludingPdfScopesAndDeletesOriginals() throws Exception {
        String distribution = System.getProperty("saneb.attachment-qa.extractor-root");
        assertThat(distribution).as("설치된 추출기 경로를 전용 task에서 지정해야 합니다.").isNotBlank();
        var gate = new AttachmentRuntimeGate(new AttachmentRuntimeIdentity(distribution),
                new IsolatedAttachmentExtractor(new ObjectMapper(), distribution), new AttachmentTemporaryStorage(temporary.toString()));
        var result = org.junit.jupiter.api.Assertions.assertTimeout(Duration.ofMinutes(7), () -> { return gate.selectValidatedResult(); });
        assertThat(result.caseCount()).isEqualTo(14); assertThat(result.cases()).hasSize(14);
        assertThat(result.cases().subList(12,14)).allSatisfy(row -> {
            assertThat(row.format()).isEqualTo("PDF"); assertThat(row.qualityCode()).isEqualTo("COMPLETE_TEXT");
            assertThat(row.blockCount()).isEqualTo(2);
        });
        assertThat(result.cases()).allSatisfy(row -> assertThat(row.originalRemoved()).isTrue());
        assertThat(result.cases()).extracting(AttachmentRuntimeGate.CaseResult::qualityCode)
                .contains("COMPLETE_TEXT", "PARTIAL_TEXT", "OCR_REQUIRED", "ENCRYPTED", "CORRUPT", "LIMIT_EXCEEDED", "UNSUPPORTED");
        try (var files = Files.list(temporary.resolve("announcement-attachment-tmp"))) {
            assertThat(files.map(path -> path.getFileName().toString()).toList()).containsExactlyInAnyOrder(".owner", ".quota.lock");
        }
        // 식별자/지문만 보고한다. 원본·추출 text·서버 경로를 JUnit artifact에 기록하지 않는다.
        System.out.println("RUNTIME_QA scope=" + result.scope() + " cases=" + result.caseCount() + " runtimeHash=" + result.runtimeHash()
                + " suiteHash=" + result.suiteHash() + " resultHash=" + result.resultHash());
    }
}
