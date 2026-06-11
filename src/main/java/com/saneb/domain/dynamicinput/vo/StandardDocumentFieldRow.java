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
        Integer sortOrder,
        String helpText
) {
}
