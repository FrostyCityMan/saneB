package com.saneb.domain.dashboard.dto;

import java.math.BigDecimal;

public record DashboardProgressSummaryResponse(
        int inProgressCount,
        int waitingResultCount,
        int approvedCount,
        int supplementRequestedCount,
        int stoppedCount,
        BigDecimal totalReceivedAmount
) {
}
