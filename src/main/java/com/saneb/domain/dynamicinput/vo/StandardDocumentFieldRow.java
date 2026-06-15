package com.saneb.domain.dynamicinput.vo;

import java.util.UUID;

public record StandardDocumentFieldRow(
        UUID standardFieldId,
        String documentTypeCode,
        String fieldKey,
        String fieldLabel,
        String fieldTypeCode,
        String scopeCode,
        Boolean requiredDefault,
        Boolean conditionEligible,
        String conditionUsageCode,
        Boolean selectable,
        Integer sortOrder,
        String helpText
) {
}
