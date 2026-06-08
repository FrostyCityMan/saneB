package com.saneb.domain.billing.dto;

import jakarta.validation.constraints.NotNull;

public record SubscriptionPlanStatusUpdateRequest(
        @NotNull Boolean active
) {
}
