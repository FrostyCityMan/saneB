/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: DashboardSummaryResponse.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.dashboard.dto;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        String serviceStatusCode,
        CandidateCountsResponse candidateCounts,
        TargetCandidateCountsResponse targetCandidateCounts,
        int finalMatchedCount,
        SupportAmountRangeResponse supportAmountRange,
        String verificationStatusCode,
        String noticeMessage
) {

    public record CandidateCountsResponse(
            int policyFund,
            int supportFund,
            int subsidy
    ) {
    }

    public record TargetCandidateCountsResponse(
            int business,
            int personal,
            int family
    ) {
    }

    public record SupportAmountRangeResponse(
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String basisCode
    ) {
    }
}
