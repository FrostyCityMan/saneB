package com.saneb.domain.applicationprogress.vo;

import java.util.UUID;

public record ApplicationProgressCreateCommand(
        UUID progressId,
        UUID matchingCaseId,
        UUID announcementId,
        UUID memberUserId,
        UUID currentStepId,
        UUID actorUserId
) {
}
