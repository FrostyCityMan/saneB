package com.saneb.domain.billing.vo;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundTransactionInsertCommand(
        UUID refundId,
        UUID paymentId,
        UUID userId,
        String providerCode,
        BigDecimal refundAmount,
        String reason,
        UUID actorUserId
) {
}
