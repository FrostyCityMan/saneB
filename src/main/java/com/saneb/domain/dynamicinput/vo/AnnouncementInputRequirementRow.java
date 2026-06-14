package com.saneb.domain.dynamicinput.vo;

import java.util.UUID;

public record AnnouncementInputRequirementRow(
        UUID requirementId,
        UUID announcementId,
        String fieldKey,
        String fieldLabel,
        String fieldTypeCode,
        String scopeCode,
        Boolean required,
        Boolean sensitive,
        Integer sortOrder,
        UUID standardFieldId,
        String helpText
) {
}
