package com.saneb.domain.documentfile.vo;

import java.util.UUID;

public record DocumentSubmissionReviewCommand(
        UUID reviewId,
        UUID submissionId,
        UUID reviewerUserId,
        String beforeStatusCode,
        String afterStatusCode,
        String reviewNote
) {
}
