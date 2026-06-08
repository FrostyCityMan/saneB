package com.saneb.domain.aiassist.dto;

import jakarta.validation.constraints.NotBlank;

public record AiAssistReviewRequest(
        @NotBlank String reviewStatusCode
) {
}
