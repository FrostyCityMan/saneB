package com.saneb.domain.approverreview.dto;

public record ApproverReviewSummaryResponse(
        AnnouncementReviewResponse announcementReview,
        VerificationReviewResponse verificationReview,
        MatchingReviewResponse matchingReview,
        ProgressReviewResponse progressReview
) {

    public record AnnouncementReviewResponse(
            int requestedCount,
            int rejectedCount,
            int approvedCount
    ) {
    }

    public record VerificationReviewResponse(
            int submittedCount,
            int reviewingCount,
            int verifiedCount,
            int rejectedCount
    ) {
    }

    public record MatchingReviewResponse(
            int reviewRequiredCount,
            int blockedCount,
            int progressedCount
    ) {
    }

    public record ProgressReviewResponse(
            int waitingResultCount,
            int approvedCount,
            int supplementRequestedCount,
            int stoppedCount
    ) {
    }
}
