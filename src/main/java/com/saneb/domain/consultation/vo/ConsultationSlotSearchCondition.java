package com.saneb.domain.consultation.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsultationSlotSearchCondition(
        UUID partnerUserId,
        String statusCode,
        OffsetDateTime startFrom,
        OffsetDateTime startTo,
        int page,
        int size,
        int offset
) {
}
