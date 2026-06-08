package com.saneb.domain.documentfile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentSubmissionReviewRequest(
        @NotBlank(message = "statusCode is required")
        @Size(max = 30, message = "statusCode must be 30 characters or less")
        String statusCode,

        @Size(max = 1000, message = "reviewNote must be 1000 characters or less")
        String reviewNote
) {
}
