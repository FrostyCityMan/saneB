package com.saneb.domain.admindashboard.vo;

import java.math.BigDecimal;

public record AdminApplicationProgressSummaryRow(
        int totalProgressCount,
        int readyCount,
        int inProgressCount,
        int waitingResultCount,
        int approvedCount,
        int rejectedCount,
        int supplementRequestedCount,
        int stoppedCount,
        int completedCount,
        BigDecimal totalReceivedAmount
) {
}
