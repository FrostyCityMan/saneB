package com.saneb.domain.announcementsource.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AnnouncementSourceContentVersionCommand(
        UUID id,
        UUID sourceId,
        String rawHash,
        String title,
        String bodyText,
        String bodySourceCode,
        String bodyAvailabilityCode,
        String sourceUrl,
        String rawPayloadJson,
        OffsetDateTime collectedAt
) {
}
