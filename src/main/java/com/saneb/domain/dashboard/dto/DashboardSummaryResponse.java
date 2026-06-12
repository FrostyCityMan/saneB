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
