package com.saneb.domain.applicationprogress.vo;

import java.util.UUID;

public record ApplicationStepStateCreateCommand(
        UUID stepStateId,
        UUID progressId,
        UUID stepId,
        String statusCode,
        UUID actorUserId
) {
}
