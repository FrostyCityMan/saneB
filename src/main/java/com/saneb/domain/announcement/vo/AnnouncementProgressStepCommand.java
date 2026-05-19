package com.saneb.domain.announcement.vo;

import java.util.UUID;

public record AnnouncementProgressStepCommand(
        UUID id,
        UUID announcementId,
        Integer stepOrder,
        String stepName,
        String guideMessage,
        String actionGuide,
        String completionConditionCode,
        String nextConditionCode,
        Boolean active,
        UUID actorUserId
) {
}
