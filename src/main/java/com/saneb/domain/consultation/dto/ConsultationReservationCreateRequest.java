package com.saneb.domain.consultation.dto;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ConsultationReservationCreateRequest(
        UUID slotId,

        UUID memberUserId,
        UUID partnerUserId,
        UUID progressId,
        UUID verificationId,

        @Size(max = 1000, message = "requestNote must be 1000 characters or less")
        String requestNote
) {
}
