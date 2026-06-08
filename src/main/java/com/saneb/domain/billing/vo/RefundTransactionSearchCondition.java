package com.saneb.domain.billing.vo;

import java.util.UUID;

public record RefundTransactionSearchCondition(
        UUID userId,
        UUID paymentId,
        String statusCode,
        int page,
        int size,
        int offset
) {
}
