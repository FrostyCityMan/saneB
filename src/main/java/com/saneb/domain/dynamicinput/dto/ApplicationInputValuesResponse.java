package com.saneb.domain.dynamicinput.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ApplicationInputValuesResponse(
        UUID progressId,
        UUID announcementId,
        List<InputValueResponse> values
) {

    public record InputValueResponse(
            UUID requirementId,
            String fieldKey,
            String fieldLabel,
            String fieldTypeCode,
            String scopeCode,
            boolean required,
            boolean sensitive,
            int sortOrder,
            String helpText,
            String valueText,
            BigDecimal valueNumber,
            LocalDate valueDate,
            Boolean valueBoolean,
            String optionCode,
            List<String> optionCodes,
            UUID submittedBy,
            OffsetDateTime submittedAt
    ) {
    }
}
