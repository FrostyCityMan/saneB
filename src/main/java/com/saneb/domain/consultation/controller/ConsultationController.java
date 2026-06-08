package com.saneb.domain.consultation.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.consultation.dto.ConsultationReservationCreateRequest;
import com.saneb.domain.consultation.dto.ConsultationReservationResponse;
import com.saneb.domain.consultation.dto.ConsultationReservationStatusUpdateRequest;
import com.saneb.domain.consultation.dto.ConsultationSlotCreateRequest;
import com.saneb.domain.consultation.dto.ConsultationSlotResponse;
import com.saneb.domain.consultation.dto.ConsultationSlotStatusUpdateRequest;
import com.saneb.domain.consultation.service.ConsultationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @GetMapping("/consultation-slots")
    @PreAuthorize("hasAnyRole('USER', 'PARTNER', 'OPERATOR', 'REVIEWER', 'ADMIN')")
    public ApiResponse<PageResponse<ConsultationSlotResponse>> selectConsultationSlotList(
            Authentication authentication,
            @RequestParam(required = false) UUID partnerUserId,
            @RequestParam(required = false) String statusCode,
            @RequestParam(required = false) OffsetDateTime startFrom,
            @RequestParam(required = false) OffsetDateTime startTo,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(consultationService.selectConsultationSlotList(
                authentication,
                partnerUserId,
                statusCode,
                startFrom,
                startTo,
                page,
                size
        ));
    }

    @PostMapping("/consultation-slots")
    @PreAuthorize("hasAnyRole('PARTNER', 'OPERATOR', 'ADMIN')")
    public ApiResponse<ConsultationSlotResponse> insertConsultationSlot(
            Authentication authentication,
            @Valid @RequestBody ConsultationSlotCreateRequest request
    ) {
        return ApiResponse.success(consultationService.insertConsultationSlot(authentication, request));
    }

    @PatchMapping("/consultation-slots/{slotId}/status")
    @PreAuthorize("hasAnyRole('PARTNER', 'OPERATOR', 'ADMIN')")
    public ApiResponse<ConsultationSlotResponse> updateConsultationSlotStatus(
            Authentication authentication,
            @PathVariable UUID slotId,
            @Valid @RequestBody ConsultationSlotStatusUpdateRequest request
    ) {
        return ApiResponse.success(consultationService.updateConsultationSlotStatus(authentication, slotId, request));
    }

    @GetMapping("/consultation-reservations")
    @PreAuthorize("hasAnyRole('USER', 'PARTNER', 'OPERATOR', 'REVIEWER', 'ADMIN')")
    public ApiResponse<PageResponse<ConsultationReservationResponse>> selectConsultationReservationList(
            Authentication authentication,
            @RequestParam(required = false) UUID memberUserId,
            @RequestParam(required = false) UUID partnerUserId,
            @RequestParam(required = false) String statusCode,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(consultationService.selectConsultationReservationList(
                authentication,
                memberUserId,
                partnerUserId,
                statusCode,
                page,
                size
        ));
    }

    @PostMapping("/consultation-reservations")
    @PreAuthorize("hasAnyRole('USER', 'OPERATOR', 'ADMIN')")
    public ApiResponse<ConsultationReservationResponse> insertConsultationReservation(
            Authentication authentication,
            @Valid @RequestBody ConsultationReservationCreateRequest request
    ) {
        return ApiResponse.success(consultationService.insertConsultationReservation(authentication, request));
    }

    @PatchMapping("/consultation-reservations/{reservationId}/status")
    @PreAuthorize("hasAnyRole('USER', 'PARTNER', 'OPERATOR', 'ADMIN')")
    public ApiResponse<ConsultationReservationResponse> updateConsultationReservationStatus(
            Authentication authentication,
            @PathVariable UUID reservationId,
            @Valid @RequestBody ConsultationReservationStatusUpdateRequest request
    ) {
        return ApiResponse.success(consultationService.updateConsultationReservationStatus(
                authentication,
                reservationId,
                request
        ));
    }
}
