package com.saneb.domain.approverreview.vo;

public record ApproverAnnouncementReviewRow(
        int requestedCount,
        int rejectedCount,
        int approvedCount
) {
}
