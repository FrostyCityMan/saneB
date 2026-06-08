package com.saneb.domain.billing.vo;

import java.util.UUID;

public record RefundTransactionStatusCommand(
        UUID refundId,
        String statusCode,
        String providerRefundKey,
        String failureCode,
        String failureMessage,
        UUID actorUserId
) {
}
