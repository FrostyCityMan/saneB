package com.saneb.domain.consultation.vo;

import java.util.UUID;

public record ConsultationReservationStatusCommand(
        UUID reservationId,
        String statusCode,
        UUID partnerUserId,
        UUID slotId,
        String note,
        UUID actorUserId
) {
}
