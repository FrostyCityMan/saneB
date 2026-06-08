package com.saneb.domain.billing.vo;

import java.math.BigDecimal;
import java.util.UUID;

public record SubscriptionPlanInsertCommand(
        UUID planId,
        String planCode,
        String planName,
        String billingCycleCode,
        BigDecimal priceAmount,
        String currencyCode,
        boolean active,
        int sortOrder,
        String description,
        UUID actorUserId
) {
}
