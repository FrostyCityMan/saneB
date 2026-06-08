package com.saneb.domain.consultation.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsultationReservationResponse(
        UUID reservationId,
        UUID slotId,
        UUID memberUserId,
        UUID partnerUserId,
        UUID progressId,
        UUID verificationId,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String statusCode,
        String requestNote,
        String statusNote,
        OffsetDateTime confirmedAt,
        OffsetDateTime canceledAt,
        OffsetDateTime completedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
