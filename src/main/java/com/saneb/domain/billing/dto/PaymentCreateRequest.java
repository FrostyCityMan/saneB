package com.saneb.domain.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCreateRequest(
        @NotNull UUID subscriptionId,
        @NotBlank String providerCode,
        @NotNull @DecimalMin("0.00") BigDecimal amount,
        String currencyCode
) {
}
