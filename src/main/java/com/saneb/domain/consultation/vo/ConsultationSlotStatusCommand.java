package com.saneb.domain.consultation.vo;

import java.util.UUID;

public record ConsultationSlotStatusCommand(
        UUID slotId,
        String statusCode,
        String note,
        UUID actorUserId
) {
}
