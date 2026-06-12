package com.saneb.domain.member.vo;

import java.util.UUID;

public record MemberDocumentFieldRow(
        UUID standardFieldId,
        String documentTypeCode,
        String fieldKey,
        String fieldLabel,
        String fieldTypeCode,
        String scopeCode,
        Boolean requiredDefault,
        int sortOrder,
        String helpText
) {
}
