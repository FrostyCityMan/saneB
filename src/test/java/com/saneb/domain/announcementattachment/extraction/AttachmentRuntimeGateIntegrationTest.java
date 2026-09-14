package com.saneb.domain.announcementattachment.extraction;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

/** 명시적 task에서는 Windows/격리 부재도 실패다. 운영 DB/외부 URL/클라이언트의 성공값을 사용하지 않는다. */
@EnabledIfEnvironmentVariable(named="SANEB_ATTACHMENT_RUNTIME_QA", matches="true")
class AttachmentRuntimeGateIntegrationTest {
    @TempDir Path temporary;
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
