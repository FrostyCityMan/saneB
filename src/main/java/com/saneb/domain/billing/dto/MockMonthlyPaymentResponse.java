package com.saneb.domain.billing.dto;

public record MockMonthlyPaymentResponse(
        UserSubscriptionResponse subscription,
        PaymentTransactionResponse payment,
        String resultMessage
) {
}
