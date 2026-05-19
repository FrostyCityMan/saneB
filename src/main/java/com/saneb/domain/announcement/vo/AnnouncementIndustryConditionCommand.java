package com.saneb.domain.announcement.vo;

import java.util.UUID;

public record AnnouncementIndustryConditionCommand(
        UUID announcementId,
        String conditionTypeCode,
        String ksicCode,
        UUID actorUserId
) {
}
