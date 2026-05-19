package com.saneb.domain.announcement.vo;

import java.util.UUID;

public record AnnouncementOptionConditionCommand(
        UUID announcementId,
        String conditionScopeCode,
        String conditionKey,
        String optionCode,
        String optionText,
        UUID actorUserId
) {
}
