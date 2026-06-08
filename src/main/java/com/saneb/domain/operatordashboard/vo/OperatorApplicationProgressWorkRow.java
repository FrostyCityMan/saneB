package com.saneb.domain.operatordashboard.vo;

import java.math.BigDecimal;

public record OperatorApplicationProgressWorkRow(
        int readyCount,
        int inProgressCount,
        int waitingResultCount,
        int approvedCount,
        int supplementRequestedCount,
        int stoppedCount,
        BigDecimal totalReceivedAmount
) {
}
