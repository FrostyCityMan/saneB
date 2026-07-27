/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: LocalGovernmentNoticeCollectionResultResponseTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.localgov.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeCollectionResultRow;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LocalGovernmentNoticeCollectionResultResponseTest {

    /**
     * JSON 필수 필드 누락을 접속 실패와 분리합니다.
     */
    @Test
    void jsonRequiredFieldsMissingIsPartialFields() {
        LocalGovernmentNoticeCollectionResultResponse response = LocalGovernmentNoticeCollectionResultResponse.from(
                row("PARSER_UNSUPPORTED", 0, 0, "JSON_REQUIRED_FIELDS_MISSING")
        );

        assertThat(response.diagnosticReasonCode()).isEqualTo("PARTIAL_FIELDS");
    }

    /**
     * JSON 파서 미설정을 파싱 실패로 분류합니다.
     */
    @Test
    void jsonParserNotConfiguredIsParserFailed() {
        LocalGovernmentNoticeCollectionResultResponse response = LocalGovernmentNoticeCollectionResultResponse.from(
                row("PARSER_UNSUPPORTED", 0, 0, "JSON_PARSER_NOT_CONFIGURED")
        );

        assertThat(response.diagnosticReasonCode()).isEqualTo("PARSER_FAILED");
    }

    /**
     * 정적 키워드 제외 건수를 무관 게시물 제외로 분류합니다.
     */
    @Test
    void excludedItemsAreIrrelevantContent() {
        LocalGovernmentNoticeCollectionResultResponse response = LocalGovernmentNoticeCollectionResultResponse.from(
                row("SUCCESS", 5, 3, null)
        );

        assertThat(response.diagnosticReasonCode()).isEqualTo("IRRELEVANT_CONTENT");
    }

    /**
     * 알 수 없는 오류를 접속 실패로 오분류하지 않습니다.
     */
    @Test
    void unknownErrorIsUnclassified() {
        LocalGovernmentNoticeCollectionResultResponse response = LocalGovernmentNoticeCollectionResultResponse.from(
                row("FAILED", 0, 0, "NEW_PROVIDER_ERROR")
        );

        assertThat(response.diagnosticReasonCode()).isEqualTo("UNCLASSIFIED_ERROR");
        assertThat(response.diagnosticTitle()).isEqualTo("분류되지 않은 수집 오류");
    }

    /**
     * 출처별 내부 처리 오류가 관리자용 독립 진단으로 변환되는지 검증합니다.
     */
    @Test
    void processingFailureHasDedicatedDiagnostic() {
        LocalGovernmentNoticeCollectionResultResponse response = LocalGovernmentNoticeCollectionResultResponse.from(
                row("FAILED", 0, 0, "PROCESSING_FAILED")
        );

        assertThat(response.diagnosticReasonCode()).isEqualTo("PROCESSING_FAILED");
        assertThat(response.diagnosticTitle()).isEqualTo("내부 처리 실패");
        assertThat(response.recommendedAction()).contains("서버 오류 로그");
    }

    /**
     * 일부 행의 필수 필드 누락을 데이터 품질 문제로 분류합니다.
     */
    @Test
    void itemFieldsMissingIsPartialFields() {
        LocalGovernmentNoticeCollectionResultResponse response = LocalGovernmentNoticeCollectionResultResponse.from(
                row("PARTIAL_FAILED", 3, 0, "ITEM_FIELDS_MISSING")
        );

        assertThat(response.diagnosticReasonCode()).isEqualTo("PARTIAL_FIELDS");
    }

    /**
     * URL 단위 수집 결과 fixture를 생성합니다.
     *
     * @param resultStatusCode 수집 결과 상태
     * @param discoveredCount 발견 건수
     * @param excludedCount 제외 건수
     * @param errorCode 오류 코드
     * @return URL 단위 수집 결과
     */
    private LocalGovernmentNoticeCollectionResultRow row(
            String resultStatusCode,
            int discoveredCount,
            int excludedCount,
            String errorCode
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        return new LocalGovernmentNoticeCollectionResultRow(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ASRUN-000001",
                UUID.randomUUID(),
                "LGS-000001",
                "서울특별시청",
                "https://www.seoul.go.kr/news/news_notice.do",
                resultStatusCode,
                discoveredCount,
                Math.max(0, discoveredCount - excludedCount),
                0,
                errorCode == null ? 0 : 1,
                excludedCount,
                errorCode == null ? 200 : null,
                errorCode,
                errorCode == null ? null : "테스트 오류",
                now.minusMinutes(1),
                now
        );
    }
}
