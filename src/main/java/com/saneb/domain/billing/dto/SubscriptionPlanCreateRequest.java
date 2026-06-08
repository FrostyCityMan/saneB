package com.saneb.domain.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SubscriptionPlanCreateRequest(
        @NotBlank String planCode,
        @NotBlank String planName,
        @NotBlank String billingCycleCode,
        @NotNull @DecimalMin("0.00") BigDecimal priceAmount,
        String currencyCode,
        Boolean active,
        Integer sortOrder,
        String description
) {
}
