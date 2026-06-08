package com.saneb.domain.billing.vo;

import java.util.UUID;

public record PaymentTransactionStatusCommand(
        UUID paymentId,
        String statusCode,
        String providerPaymentKey,
        String failureCode,
        String failureMessage,
        UUID actorUserId
) {
}
