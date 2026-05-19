package com.saneb.domain.announcement.vo;

import java.util.UUID;

public record AnnouncementProgressStepRow(
        UUID stepId,
        Integer stepOrder,
        String stepName,
        String guideMessage,
        String actionGuide,
        String completionConditionCode,
        String nextConditionCode,
        Boolean active
) {
}
