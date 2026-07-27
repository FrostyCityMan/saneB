/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: LocalGovernmentNoticeCollectionSummaryResponse.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.localgov.dto;

import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeCollectionSummaryRow;

public record LocalGovernmentNoticeCollectionSummaryResponse(
        long totalCount,
        long enabledCount,
        long successCount,
        long warningCount,
        long failedCount,
        long reviewPendingCount,
        long transportFailureCount,
        long parserFailureCount,
        long partialFieldsCount,
        long semanticMismatchCount,
        long irrelevantContentCount,
        long unverifiedSourceCount,
        String trafficLightCode
) {

    /**
     * 집계 결과를 관리자 신호등 응답으로 변환합니다.
     *
     * @param row 수집 현황 집계 결과
     * @return 관리자 신호등 응답
     */
    public static LocalGovernmentNoticeCollectionSummaryResponse from(LocalGovernmentNoticeCollectionSummaryRow row) {
        String trafficLightCode = row.failedCount() > 0 ? "RED"
                : row.warningCount() > 0 || row.reviewPendingCount() > 0 ? "YELLOW" : "GREEN";
        return new LocalGovernmentNoticeCollectionSummaryResponse(
                row.totalCount(), row.enabledCount(), row.successCount(), row.warningCount(), row.failedCount(),
                row.reviewPendingCount(), row.transportFailureCount(), row.parserFailureCount(),
                row.partialFieldsCount(), row.semanticMismatchCount(), row.irrelevantContentCount(),
                row.unverifiedSourceCount(), trafficLightCode
        );
    }
}
