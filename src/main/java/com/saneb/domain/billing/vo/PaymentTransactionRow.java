package com.saneb.domain.billing.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentTransactionRow(
        UUID paymentId,
        UUID subscriptionId,
        UUID userId,
        UUID planId,
        String providerCode,
        String merchantUid,
        String providerPaymentKey,
        String statusCode,
        BigDecimal amount,
        String currencyCode,
        OffsetDateTime requestedAt,
        OffsetDateTime approvedAt,
        OffsetDateTime failedAt,
        String failureCode,
        String failureMessage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
