package com.saneb.domain.matching.dto;

import jakarta.validation.constraints.NotBlank;

public record MatchingCaseStatusUpdateRequest(
        @NotBlank String statusCode,
        String blockedReasonCode
) {
}
