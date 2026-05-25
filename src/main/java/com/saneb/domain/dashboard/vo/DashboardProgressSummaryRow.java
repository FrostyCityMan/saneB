package com.saneb.domain.dashboard.vo;

import java.math.BigDecimal;

public record DashboardProgressSummaryRow(
        int inProgressCount,
        int waitingResultCount,
        int approvedCount,
        int supplementRequestedCount,
        int stoppedCount,
        BigDecimal totalReceivedAmount
) {
}
