package com.saneb.domain.billing.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentProviderEventRequest(
        @NotBlank String eventId,
        UUID paymentId,
        UUID refundId,
        String merchantUid,
        String providerPaymentKey,
        String providerRefundKey,
        @NotBlank String eventTypeCode,
        BigDecimal amount,
        String currencyCode,
        String failureCode,
        String failureMessage
) {
}
