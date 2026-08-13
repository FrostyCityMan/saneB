package com.saneb.domain.announcementsource.vo;

import java.util.UUID;

public record AnnouncementSourceClassificationSupportMatchCommand(
        UUID id,
        UUID evaluationId,
        String supportTypeCode
) {
}
