package com.saneb.domain.consultation.vo;

import java.util.UUID;

public record ConsultationReservationInsertCommand(
        UUID reservationId,
        UUID slotId,
        UUID memberUserId,
        UUID partnerUserId,
        UUID progressId,
        UUID verificationId,
        String requestNote,
        UUID actorUserId
) {
}
