package com.saneb.domain.consultation.vo;

import java.util.UUID;

public record ConsultationHistoryCommand(
        UUID historyId,
        UUID reservationId,
        UUID actorUserId,
        String beforeStatusCode,
        String afterStatusCode,
        String note
) {
}
