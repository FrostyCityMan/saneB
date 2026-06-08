package com.saneb.domain.billing.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserSubscriptionResponse(
        UUID subscriptionId,
        UUID userId,
        UUID planId,
        String planCode,
        String planName,
        String billingCycleCode,
        BigDecimal priceAmount,
        String currencyCode,
        String statusCode,
        OffsetDateTime currentPeriodStart,
        OffsetDateTime currentPeriodEnd,
        OffsetDateTime canceledAt,
        String cancelReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
