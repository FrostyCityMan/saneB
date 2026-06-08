package com.saneb.domain.billing.vo;

import java.util.UUID;

public record SubscriptionPlanStatusCommand(
        UUID planId,
        boolean active,
        UUID actorUserId
) {
}
