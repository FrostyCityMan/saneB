package com.saneb.domain.dynamicinput.vo;

import java.util.UUID;

public record AnnouncementInputRequirementCommand(
        UUID requirementId,
        UUID announcementId,
        String fieldKey,
        String fieldLabel,
        String fieldTypeCode,
        String scopeCode,
        Boolean required,
        Boolean sensitive,
        int sortOrder,
        String helpText,
        UUID actorUserId
) {
}
