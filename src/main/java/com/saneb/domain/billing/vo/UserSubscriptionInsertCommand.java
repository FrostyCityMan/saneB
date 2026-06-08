package com.saneb.domain.billing.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserSubscriptionInsertCommand(
        UUID subscriptionId,
        UUID userId,
        UUID planId,
        String statusCode,
        OffsetDateTime currentPeriodStart,
        OffsetDateTime currentPeriodEnd,
        UUID actorUserId
) {
}
