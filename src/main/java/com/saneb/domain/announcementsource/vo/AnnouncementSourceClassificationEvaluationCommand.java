package com.saneb.domain.announcementsource.vo;

import java.util.UUID;

public record AnnouncementSourceClassificationEvaluationCommand(
        UUID id,
        UUID sourceId,
        UUID contentVersionId,
        UUID runId,
        UUID ruleReleaseId,
        String engineVersion,
        String bodySourceCode,
        String bodyAvailabilityCode,
        String titleStageCode,
        String bodyStageCode,
        String decisionStatusCode,
        String reasonCode
) {
}
