package com.saneb.domain.consultation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ConsultationReservationCreateRequest(
        @NotNull(message = "slotId is required")
        UUID slotId,

        UUID memberUserId,
        UUID progressId,
        UUID verificationId,

        @Size(max = 1000, message = "requestNote must be 1000 characters or less")
        String requestNote
) {
}
