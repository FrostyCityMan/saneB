package com.saneb.domain.consultation.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.consultation.dto.ConsultationReservationCreateRequest;
import com.saneb.domain.consultation.dto.ConsultationReservationResponse;
import com.saneb.domain.consultation.dto.ConsultationReservationStatusUpdateRequest;
import com.saneb.domain.consultation.dto.ConsultationSlotCreateRequest;
import com.saneb.domain.consultation.dto.ConsultationSlotResponse;
import com.saneb.domain.consultation.dto.ConsultationSlotStatusUpdateRequest;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface ConsultationService {

    PageResponse<ConsultationSlotResponse> selectConsultationSlotList(
            Authentication authentication,
            UUID partnerUserId,
            String statusCode,
            OffsetDateTime startFrom,
            OffsetDateTime startTo,
            int page,
            int size
    );

    ConsultationSlotResponse insertConsultationSlot(Authentication authentication, ConsultationSlotCreateRequest request);

    ConsultationSlotResponse updateConsultationSlotStatus(
            Authentication authentication,
            UUID slotId,
            ConsultationSlotStatusUpdateRequest request
    );

    PageResponse<ConsultationReservationResponse> selectConsultationReservationList(
            Authentication authentication,
            UUID memberUserId,
            UUID partnerUserId,
            String statusCode,
            int page,
            int size
    );

    ConsultationReservationResponse insertConsultationReservation(
            Authentication authentication,
            ConsultationReservationCreateRequest request
    );

    ConsultationReservationResponse updateConsultationReservationStatus(
            Authentication authentication,
            UUID reservationId,
            ConsultationReservationStatusUpdateRequest request
    );
}
