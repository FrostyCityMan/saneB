package com.saneb.domain.dynamicinput.dto;

import java.util.UUID;

public record StandardDocumentFieldResponse(
        UUID standardFieldId,
        String documentTypeCode,
        String fieldKey,
        String fieldLabel,
        String fieldTypeCode,
        String scopeCode,
        boolean requiredDefault,
        int sortOrder,
        String helpText
) {
}
