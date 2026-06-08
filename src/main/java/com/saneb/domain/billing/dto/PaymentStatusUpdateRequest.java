package com.saneb.domain.billing.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentStatusUpdateRequest(
        @NotBlank String statusCode,
        String providerPaymentKey,
        String failureCode,
        String failureMessage
) {
}
