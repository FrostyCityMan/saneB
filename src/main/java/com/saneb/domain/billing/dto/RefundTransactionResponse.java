package com.saneb.domain.billing.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RefundTransactionResponse(
        UUID refundId,
        UUID paymentId,
        UUID userId,
        String providerCode,
        String providerRefundKey,
        String statusCode,
        BigDecimal refundAmount,
        String reason,
        UUID requestedBy,
        OffsetDateTime requestedAt,
        OffsetDateTime completedAt,
        String failureCode,
        String failureMessage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
