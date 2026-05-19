package com.saneb.domain.dashboard.dto;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        String serviceStatusCode,
        CandidateCountsResponse candidateCounts,
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

    public record SupportAmountRangeResponse(
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String basisCode
    ) {
    }
}
