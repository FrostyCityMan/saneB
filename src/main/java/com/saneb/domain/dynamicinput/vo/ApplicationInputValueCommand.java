package com.saneb.domain.dynamicinput.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ApplicationInputValueCommand(
        UUID inputValueId,
        UUID progressId,
        UUID requirementId,
        String valueText,
        BigDecimal valueNumber,
        LocalDate valueDate,
        Boolean valueBoolean,
        String optionCode,
        UUID submittedBy
) {
}
