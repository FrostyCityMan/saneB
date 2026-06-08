package com.saneb.domain.consultation.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsultationSlotResponse(
        UUID slotId,
        UUID partnerUserId,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String statusCode,
        String note,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
