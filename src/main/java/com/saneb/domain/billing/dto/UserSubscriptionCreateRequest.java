package com.saneb.domain.billing.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UserSubscriptionCreateRequest(
        UUID userId,
        @NotNull UUID planId
) {
}
