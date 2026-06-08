package com.saneb.domain.billing.vo;

import java.util.UUID;

public record PaymentTransactionSearchCondition(
        UUID userId,
        UUID subscriptionId,
        String statusCode,
        int page,
        int size,
        int offset
) {
}
