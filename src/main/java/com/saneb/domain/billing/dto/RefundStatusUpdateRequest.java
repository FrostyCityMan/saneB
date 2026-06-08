package com.saneb.domain.billing.dto;

import jakarta.validation.constraints.NotBlank;

public record RefundStatusUpdateRequest(
        @NotBlank String statusCode,
        String providerRefundKey,
        String failureCode,
        String failureMessage
) {
}
