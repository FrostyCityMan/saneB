package com.saneb.domain.admindashboard.vo;

public record AdminAnnouncementSummaryRow(
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
