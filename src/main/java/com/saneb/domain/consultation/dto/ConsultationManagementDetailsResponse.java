package com.saneb.domain.consultation.dto;

import java.util.List;

public record ConsultationManagementDetailsResponse(
        ConsultationReservationResponse reservation, List<String> allowedNextStatusCodes) {
}
