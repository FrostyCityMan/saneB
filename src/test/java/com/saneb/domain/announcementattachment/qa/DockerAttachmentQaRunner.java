package com.saneb.domain.announcementattachment.qa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** 실파일 QA 전용. 운영 bwrap 경로를 Windows host 파싱으로 우회하지 않는다. */
final class DockerAttachmentQaRunner {
    static final String IMAGE = "eclipse-temurin@sha256:8ec353b20d3aab0758572236b81b967c7077c40c4d0819ce97f9a1329d684603";
    private static final int MAX_OUTPUT = 8 * 1024 * 1024;
    private final ObjectMapper mapper = new ObjectMapper();

    JsonNode selectExtraction(Path library, Path input) throws Exception {
        if (!Files.isDirectory(library, LinkOption.NOFOLLOW_LINKS)
                || !Files.isRegularFile(input, LinkOption.NOFOLLOW_LINKS)
                || Files.size(input) > 20L * 1024 * 1024) throw new IOException("QA_INPUT_INVALID");
        String name = "saneb-attachment-qa-" + UUID.randomUUID();
        List<String> command = new ArrayList<>(selectContainerCommand(name));
        command.addAll(List.of("--mount", "type=bind,source=" + library.toRealPath() + ",target=/extractor,readonly",
                "--mount", "type=bind,source=" + input.toRealPath() + ",target=/input.bin,readonly",
                "--entrypoint", "/opt/java/openjdk/bin/java", IMAGE,
                "-Xms16m", "-Xmx256m", "-XX:MaxMetaspaceSize=64m", "-XX:ReservedCodeCacheSize=32m",
                "-XX:ActiveProcessorCount=1", "-XX:+UseSerialGC", "-Djava.awt.headless=true",
                "-cp", "/extractor/*", "com.saneb.extractor.AttachmentExtractorMain", "/input.bin"));
        JsonNode result = mapper.readTree(selectOutput(command, name, 35));
        if (result == null || !result.isObject() || !result.hasNonNull("qualityCode"))
            throw new IOException("QA_INVALID_IPC");
        return result;
    }

    String selectIsolationProbe() throws Exception {
        String name = "saneb-attachment-qa-" + UUID.randomUUID();
        List<String> command = new ArrayList<>(selectContainerCommand(name));
        command.addAll(List.of("-i", "--entrypoint", "/bin/sh", IMAGE, "-s"));
        // Windows ProcessBuilder의 중첩 따옴표 변환에 의존하지 않고 고정 점검문만 stdin으로 보낸다.
        String probe = "test \"$(id -u)\" = 65534 && test -z \"$SANEB_QA_SECRET_CANARY\" "
                + "&& test ! -e /sys/class/net/eth0 && test ! -e /host "
                + "&& test \"$(cat /sys/fs/cgroup/memory.max)\" = 536870912 "
                + "&& test \"$(cat /sys/fs/cgroup/pids.max)\" = 64 "
                + "&& test \"$(cat /sys/fs/cgroup/memory.swap.max)\" = 0 "
                + "&& test \"$(cat /sys/fs/cgroup/cpu.max)\" = '100000 100000' "
                + "&& grep -q ' / / ro,' /proc/self/mountinfo "
                + "&& grep -q '^CapEff:[[:space:]]*0000000000000000$' /proc/self/status "
                + "&& grep -q '^NoNewPrivs:[[:space:]]*1$' /proc/self/status && printf ISOLATION_PROBE_OK\n";
        return new String(selectOutput(command, name, 20, probe), StandardCharsets.UTF_8);
    }

    static List<String> selectContainerCommand(String name) {
        if (!name.matches("saneb-attachment-qa-[0-9a-f-]{36}")) throw new IllegalArgumentException("QA_CONTAINER_NAME_INVALID");
        return List.of("docker", "run", "--rm", "--pull=never", "--name", name, "--init",
                "--network=none", "--read-only", "--log-driver=none", "--cap-drop=ALL",
                "--security-opt=no-new-privileges", "--pids-limit=64", "--cpus=1",
                "--memory=512m", "--memory-swap=512m", "--user=65534:65534",
                "--tmpfs", "/tmp:rw,noexec,nosuid,nodev,size=67108864");
    }

    private byte[] selectOutput(List<String> command, String name, int seconds) throws Exception {
        return selectOutput(command, name, seconds, null);
    }

    private byte[] selectOutput(List<String> command, String name, int seconds, String stdin) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD);
        builder.environment().put("SANEB_QA_SECRET_CANARY", "synthetic-not-a-secret");
        Process process = builder.start();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var output = executor.submit(() -> {
                try (var input = process.getInputStream(); var bytes = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    for (int count; (count = input.read(buffer)) != -1;) {
                        if (bytes.size() + count > MAX_OUTPUT) {
                            process.destroyForcibly();
                            throw new IOException("QA_OUTPUT_LIMIT");
                        }
                        bytes.write(buffer, 0, count);
                    }
                    return bytes.toByteArray();
                }
            });
            try {
                try (var input = process.getOutputStream()) {
                    if (stdin != null) input.write(stdin.getBytes(StandardCharsets.UTF_8));
                }
                if (!process.waitFor(seconds, TimeUnit.SECONDS)) throw new IOException("QA_CONTAINER_TIMEOUT");
                if (process.exitValue() != 0) throw new IOException("QA_CONTAINER_FAILED");
                return output.get(2, TimeUnit.SECONDS);
            } finally {
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
                // 정확히 이번 호출이 정한 UUID 컨테이너만 제거한다. 사용자 컨테이너·볼륨은 대상이 아니다.
                Process cleanup = new ProcessBuilder("docker", "rm", "--force", name)
                        .redirectError(ProcessBuilder.Redirect.DISCARD).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
                if (!cleanup.waitFor(8, TimeUnit.SECONDS)) { cleanup.destroyForcibly(); throw new IOException("QA_CLEANUP_TIMEOUT"); }
            }
        }
    }
}
