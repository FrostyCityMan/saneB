package com.saneb.domain.consultation.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsultationReservationRow(
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
