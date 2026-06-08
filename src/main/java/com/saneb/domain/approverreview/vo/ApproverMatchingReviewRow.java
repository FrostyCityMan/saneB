package com.saneb.domain.approverreview.vo;

public record ApproverMatchingReviewRow(
        int reviewRequiredCount,
        int blockedCount,
        int progressedCount
) {
}
