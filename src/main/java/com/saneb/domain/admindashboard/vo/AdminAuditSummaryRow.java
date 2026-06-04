package com.saneb.domain.admindashboard.vo;

public record AdminAuditSummaryRow(
        int totalAuditCount,
        int failAuditCount,
        int recentFailAuditCount
) {
}
