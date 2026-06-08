package com.saneb.domain.billing.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserSubscriptionStatusCommand(
        UUID subscriptionId,
        String statusCode,
        OffsetDateTime currentPeriodStart,
        OffsetDateTime currentPeriodEnd,
        String cancelReason,
        UUID actorUserId
) {
}
