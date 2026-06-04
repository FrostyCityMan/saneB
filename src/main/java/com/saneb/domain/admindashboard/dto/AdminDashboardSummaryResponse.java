package com.saneb.domain.admindashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardSummaryResponse(
        UserSummaryResponse userSummary,
        AnnouncementSummaryResponse announcementSummary,
        VerificationSummaryResponse verificationSummary,
        MatchingSummaryResponse matchingSummary,
        ApplicationProgressSummaryResponse applicationProgressSummary,
        AuditSummaryResponse auditSummary
) {

    public record UserSummaryResponse(
            int totalUserCount,
            int activeUserCount,
            int userRoleCount,
            int partnerRoleCount,
            int operatorRoleCount,
            int approverRoleCount,
            int adminRoleCount
    ) {
    }

    public record AnnouncementSummaryResponse(
            int totalAnnouncementCount,
            int draftCount,
            int requestedCount,
            int approvedCount,
            int rejectedCount,
            int openAnnouncementCount,
            int pausedAnnouncementCount,
            int closedAnnouncementCount
    ) {
    }

    public record VerificationSummaryResponse(
            int totalVerificationCount,
            int reviewQueueCount,
            int verifiedCount,
            int rejectedCount,
            List<StatusCountResponse> statusCounts
    ) {
    }

    public record MatchingSummaryResponse(
            int totalMatchingCaseCount,
            int matchedCount,
            int reviewRequiredCount,
            int blockedCount,
            int progressedCount,
            List<StatusCountResponse> statusCounts
    ) {
    }

    public record ApplicationProgressSummaryResponse(
            int totalProgressCount,
            int activeProgressCount,
            int waitingResultCount,
            int approvedCount,
            int supplementRequestedCount,
            int stoppedCount,
            int completedCount,
            BigDecimal totalReceivedAmount,
            List<StatusCountResponse> statusCounts
    ) {
    }

    public record AuditSummaryResponse(
            int totalAuditCount,
            int failAuditCount,
            int recentFailAuditCount
    ) {
    }

    public record StatusCountResponse(
            String statusCode,
            int count
    ) {
    }
}
