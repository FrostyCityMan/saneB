package com.saneb.domain.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record RefundCreateRequest(
        @NotNull UUID paymentId,
        @NotNull @DecimalMin("0.01") BigDecimal refundAmount,
        String reason
) {
}
