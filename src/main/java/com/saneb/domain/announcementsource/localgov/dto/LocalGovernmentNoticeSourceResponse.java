/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: LocalGovernmentNoticeSourceResponse.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.localgov.dto;

import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeSourceRow;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LocalGovernmentNoticeSourceResponse(
        UUID sourceId,
        String publicCode,
        String sidoCode,
        String sidoName,
        String sigunguCode,
        String sigunguName,
        String institutionTypeCode,
        String institutionName,
        String homepageUrl,
        String noticeUrl,
        String collectionEndpointUrl,
        String pageTypeCode,
        String requestProfileCode,
        String requestMethodCode,
        String parserProfileCode,
        String parserProfileName,
        String collectionHint,
        String confidenceCode,
        String validationStatusCode,
        boolean enabled,
        String collectionStatusCode,
        String trafficLightCode,
        OffsetDateTime lastCollectedAt,
        OffsetDateTime lastSuccessAt,
        Integer lastHttpStatus,
        String lastErrorCode,
        String lastErrorMessage,
        OffsetDateTime updatedAt
) {

    /**
     * DB 조회 결과를 관리자 응답으로 변환합니다.
     *
     * @param row 지자체 공고 URL 조회 결과
     * @return 관리자 응답
     */
    public static LocalGovernmentNoticeSourceResponse from(LocalGovernmentNoticeSourceRow row) {
        return new LocalGovernmentNoticeSourceResponse(
                row.sourceId(), row.publicCode(), row.sidoCode(), row.sidoName(), row.sigunguCode(), row.sigunguName(),
                row.institutionTypeCode(), row.institutionName(), row.homepageUrl(), row.noticeUrl(),
                row.collectionEndpointUrl(), row.pageTypeCode(),
                row.requestProfileCode(), row.requestMethodCode(), row.parserProfileCode(), row.parserProfileName(),
                row.collectionHint(), row.confidenceCode(),
                row.validationStatusCode(), row.enabled(), row.collectionStatusCode(), trafficLight(row),
                row.lastCollectedAt(), row.lastSuccessAt(), row.lastHttpStatus(), row.lastErrorCode(),
                row.lastErrorMessage(), row.updatedAt()
        );
    }

    /**
     * 운영 상태를 관리자 신호등 코드로 변환합니다.
     *
     * @param row 지자체 공고 URL 조회 결과
     * @return RED, YELLOW 또는 GREEN
     */
    private static String trafficLight(LocalGovernmentNoticeSourceRow row) {
        if (row.lastErrorCode() != null || switch (row.collectionStatusCode()) {
            case "FAILED", "URL_ERROR", "ACCESS_BLOCKED", "PARSER_UNSUPPORTED" -> true;
            default -> false;
        }) {
            return "RED";
        }
        if (!row.enabled() || "CHECK_REQUIRED".equals(row.collectionStatusCode())
                || "CHECK_REQUIRED".equals(row.validationStatusCode())) {
            return "YELLOW";
        }
        return "GREEN";
    }
}
