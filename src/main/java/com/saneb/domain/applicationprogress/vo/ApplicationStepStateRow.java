package com.saneb.domain.applicationprogress.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApplicationStepStateRow(
        UUID stepStateId,
        UUID progressId,
        UUID stepId,
        Integer stepOrder,
        String stepName,
        String guideMessage,
        String actionGuide,
        String completionConditionCode,
        String statusCode,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt
) {
}
