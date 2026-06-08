package com.saneb.domain.consultation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsultationSlotCreateRequest(
        UUID partnerUserId,

        @NotNull(message = "startAt is required")
        OffsetDateTime startAt,

        @NotNull(message = "endAt is required")
        OffsetDateTime endAt,

        @Size(max = 1000, message = "note must be 1000 characters or less")
        String note
) {
}
