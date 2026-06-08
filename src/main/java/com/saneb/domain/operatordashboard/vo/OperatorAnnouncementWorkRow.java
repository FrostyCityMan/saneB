package com.saneb.domain.operatordashboard.vo;

public record OperatorAnnouncementWorkRow(
        int draftCount,
        int requestedCount,
        int openAnnouncementCount,
        int pausedAnnouncementCount,
        int closedAnnouncementCount
) {
}
