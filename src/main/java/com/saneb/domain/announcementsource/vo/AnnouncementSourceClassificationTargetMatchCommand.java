package com.saneb.domain.announcementsource.vo;

import java.util.UUID;

public record AnnouncementSourceClassificationTargetMatchCommand(
        UUID id,
        UUID evaluationId,
        String targetCategoryCode
) {
}
