package com.saneb.domain.announcementattachment.qa;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/** 기본 회귀에서는 Docker 실행 없이 명령 구성의 안전 경계만 검사한다. */
class DockerAttachmentQaRunnerTest {
    @Test void containerCommandKeepsQaIsolationConstraints() {
        var command = DockerAttachmentQaRunner.selectContainerCommand(
                "saneb-attachment-qa-12345678-1234-1234-1234-123456789abc");
        assertTrue(command.containsAll(List.of("--rm", "--pull=never", "--init", "--network=none",
                "--read-only", "--log-driver=none", "--cap-drop=ALL", "--security-opt=no-new-privileges",
                "--pids-limit=64", "--cpus=1", "--memory=512m", "--memory-swap=512m",
                "--user=65534:65534", "/tmp:rw,noexec,nosuid,nodev,size=67108864")));
        assertFalse(command.contains("--privileged"));
        assertFalse(command.contains("--env"));
        assertFalse(command.contains("--volume"));
        assertTrue(DockerAttachmentQaRunner.IMAGE.matches("eclipse-temurin@sha256:[0-9a-f]{64}"));
    }

    @Test void refusesNamesThatCouldTargetUserContainersDuringCleanup() {
        for (String name : List.of("postgres", "saneb-attachment-qa-*", "--all", "saneb-attachment-qa-probe"))
            assertThrows(IllegalArgumentException.class, () -> DockerAttachmentQaRunner.selectContainerCommand(name));
    }
}
