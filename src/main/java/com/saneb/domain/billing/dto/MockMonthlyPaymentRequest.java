package com.saneb.domain.billing.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MockMonthlyPaymentRequest(
        @NotNull(message = "요금제를 선택하세요.")
        UUID planId,

        Boolean simulateFailure
) {
}
