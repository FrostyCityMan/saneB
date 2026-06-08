package com.saneb.domain.billing.vo;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentTransactionInsertCommand(
        UUID paymentId,
        UUID subscriptionId,
        UUID userId,
        UUID planId,
        String providerCode,
        String merchantUid,
        BigDecimal amount,
        String currencyCode,
        UUID actorUserId
) {
}
