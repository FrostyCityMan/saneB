package com.saneb.domain.billing.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SubscriptionPlanResponse(
        UUID planId,
        String planCode,
        String planName,
        String billingCycleCode,
        BigDecimal priceAmount,
        String currencyCode,
        boolean active,
        int sortOrder,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
