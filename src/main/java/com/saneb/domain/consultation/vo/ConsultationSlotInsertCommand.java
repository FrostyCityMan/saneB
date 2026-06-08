package com.saneb.domain.consultation.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsultationSlotInsertCommand(
        UUID slotId,
        UUID partnerUserId,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String note,
        UUID actorUserId
) {
}
