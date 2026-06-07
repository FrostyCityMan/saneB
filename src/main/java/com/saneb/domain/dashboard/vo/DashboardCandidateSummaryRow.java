package com.saneb.domain.dashboard.vo;

import java.math.BigDecimal;

public record DashboardCandidateSummaryRow(
        int policyFundCount,
        int supportFundCount,
        int subsidyCount,
        int startableMatchedCount,
        int finalMatchedCount,
        BigDecimal minAmount,
        BigDecimal maxAmount
) {
}
