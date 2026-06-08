package com.saneb.domain.consultation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConsultationReservationStatusUpdateRequest(
        @NotBlank(message = "statusCode is required")
        @Size(max = 30, message = "statusCode must be 30 characters or less")
        String statusCode,

        @Size(max = 1000, message = "note must be 1000 characters or less")
        String note
) {
}
