package com.saneb.domain.announcementattachment.extraction;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.io.ByteArrayOutputStream;
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
        Path source = temporary.resolve("AttachmentExtractorMain.java");
        Files.writeString(source, """
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

    @Test void installedLinuxProcessChecksAllTwelveFixturesAndDeletesOriginals() throws Exception {
        String distribution = System.getProperty("saneb.attachment-qa.extractor-root");
        assertThat(distribution).as("설치된 추출기 경로를 전용 task에서 지정해야 합니다.").isNotBlank();
        var gate = new AttachmentRuntimeGate(new AttachmentRuntimeIdentity(distribution),
                new IsolatedAttachmentExtractor(new ObjectMapper(), distribution), new AttachmentTemporaryStorage(temporary.toString()));
        var result = org.junit.jupiter.api.Assertions.assertTimeout(Duration.ofMinutes(7), () -> { return gate.selectValidatedResult(); });
        assertThat(result.caseCount()).isEqualTo(12); assertThat(result.cases()).hasSize(12);
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
