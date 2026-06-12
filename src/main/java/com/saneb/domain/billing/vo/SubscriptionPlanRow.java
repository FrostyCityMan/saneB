package com.saneb.domain.billing.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SubscriptionPlanRow(
        UUID planId,
        String planCode,
        String planName,
        String billingCycleCode,
        BigDecimal priceAmount,
        String currencyCode,
        Boolean active,
        Integer sortOrder,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
