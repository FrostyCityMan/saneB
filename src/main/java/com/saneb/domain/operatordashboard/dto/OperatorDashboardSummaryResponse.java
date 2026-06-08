package com.saneb.domain.operatordashboard.dto;

import java.math.BigDecimal;

public record OperatorDashboardSummaryResponse(
        AnnouncementWorkResponse announcementWork,
        MatchingWorkResponse matchingWork,
        ApplicationProgressWorkResponse applicationProgressWork
) {

    public record AnnouncementWorkResponse(
            int draftCount,
            int requestedCount,
            int openAnnouncementCount,
            int pausedAnnouncementCount,
            int closedAnnouncementCount
    ) {
    }

    public record MatchingWorkResponse(
            int matchedCount,
            int reviewRequiredCount,
            int blockedCount,
            int progressedCount
    ) {
    }

    public record ApplicationProgressWorkResponse(
            int readyCount,
            int inProgressCount,
            int waitingResultCount,
            int approvedCount,
            int supplementRequestedCount,
            int stoppedCount,
            BigDecimal totalReceivedAmount
    ) {
    }
}
