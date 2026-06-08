package com.saneb.domain.consultation.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsultationSlotRow(
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
