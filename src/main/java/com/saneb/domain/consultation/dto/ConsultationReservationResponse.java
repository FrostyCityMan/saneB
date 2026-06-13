package com.saneb.domain.consultation.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsultationReservationResponse(
        UUID reservationId,
        String reservationCode,
        UUID slotId,
        UUID memberUserId,
        String memberUserCode,
        UUID partnerUserId,
        String partnerUserCode,
        UUID progressId,
        String progressCode,
        UUID verificationId,
        String verificationCode,
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
