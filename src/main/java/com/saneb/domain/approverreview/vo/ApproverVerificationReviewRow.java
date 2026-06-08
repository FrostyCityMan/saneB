package com.saneb.domain.approverreview.vo;

public record ApproverVerificationReviewRow(
        int submittedCount,
        int reviewingCount,
        int verifiedCount,
        int rejectedCount
) {
}
