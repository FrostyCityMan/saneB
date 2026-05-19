package com.saneb.domain.announcement.vo;

import java.util.UUID;

public record AnnouncementStepButtonCommand(
        UUID stepId,
        String buttonCode,
        String buttonLabel,
        String buttonActionCode,
        UUID nextStepId,
        Integer sortOrder,
        UUID actorUserId
) {
}
