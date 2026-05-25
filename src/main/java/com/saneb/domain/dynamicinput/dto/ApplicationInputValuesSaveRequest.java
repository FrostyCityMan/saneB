package com.saneb.domain.dynamicinput.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ApplicationInputValuesSaveRequest(
        @Valid List<InputValueRequest> values
) {

    public record InputValueRequest(
            @NotNull UUID requirementId,
            String valueText,
            BigDecimal valueNumber,
            LocalDate valueDate,
            Boolean valueBoolean,
            String optionCode,
            List<String> optionCodes
    ) {
    }
}
