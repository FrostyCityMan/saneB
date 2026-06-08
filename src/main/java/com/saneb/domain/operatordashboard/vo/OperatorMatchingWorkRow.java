package com.saneb.domain.operatordashboard.vo;

public record OperatorMatchingWorkRow(
        int matchedCount,
        int reviewRequiredCount,
        int blockedCount,
        int progressedCount
) {
}
