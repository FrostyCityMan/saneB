package com.saneb.domain.consultation.vo;

import java.util.UUID;

public record ConsultationReservationStatusCommand(
        UUID reservationId,
        String statusCode,
        String note,
        UUID actorUserId
) {
}
