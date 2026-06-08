package com.saneb.domain.billing.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RefundTransactionRow(
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
