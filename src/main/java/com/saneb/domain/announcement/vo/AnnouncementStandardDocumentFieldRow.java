package com.saneb.domain.announcement.vo;

import java.util.UUID;

public record AnnouncementStandardDocumentFieldRow(
        UUID standardFieldId,
        String documentTypeCode,
        String fieldKey,
        String fieldLabel,
        String fieldTypeCode,
        String scopeCode,
        Boolean conditionEligible,
        Boolean selectable
) {
}
