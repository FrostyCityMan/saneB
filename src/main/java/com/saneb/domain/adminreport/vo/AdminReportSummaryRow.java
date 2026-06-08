package com.saneb.domain.adminreport.vo;

import java.math.BigDecimal;

public record AdminReportSummaryRow(
        long totalUserCount,
        long activeUserCount,
        long approvedAnnouncementCount,
        long openAnnouncementCount,
        long matchedCaseCount,
        long progressedCaseCount,
        long activeProgressCount,
        long completedProgressCount,
        long activeSubscriptionCount,
        long approvedPaymentCount,
        BigDecimal approvedPaymentAmount,
        long openOperationTaskCount,
        long unreadNotificationCount
) {
}
