package com.saneb.domain.adminreport.dto;

import java.math.BigDecimal;

public record AdminReportSummaryResponse(
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
