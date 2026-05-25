package com.saneb.domain.partnerverification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PartnerVerificationStatusUpdateRequest(
        @NotBlank(message = "statusCode is required")
        @Size(max = 30, message = "statusCode must be 30 characters or less")
        String statusCode,

        @Size(max = 2000, message = "reviewNote must be 2000 characters or less")
        String reviewNote
) {
}
