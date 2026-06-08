package com.saneb.domain.consultation.vo;

import java.util.UUID;

public record ConsultationReservationSearchCondition(
        UUID memberUserId,
        UUID partnerUserId,
        String statusCode,
        int page,
        int size,
        int offset
) {
}
