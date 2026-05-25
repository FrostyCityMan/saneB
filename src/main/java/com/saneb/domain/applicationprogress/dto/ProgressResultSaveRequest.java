package com.saneb.domain.applicationprogress.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProgressResultSaveRequest(
        @NotBlank String resultCode,
        String resultNote,
        @NotNull LocalDate resultDate,
        BigDecimal receivedAmount
) {
}
