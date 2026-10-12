package com.saneb.domain.consultation.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.domain.consultation.dto.ConsultationManagementDetailsResponse;
import com.saneb.domain.consultation.dto.ConsultationManagementStatusRequest;
import com.saneb.domain.consultation.service.ConsultationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/admin/consultation-reservations")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
public class ConsultationManagementController {
    private final ConsultationService service;

    public ConsultationManagementController(ConsultationService service) {
        this.service = service;
    }

    @GetMapping("/{reservationId}")
    public ResponseEntity<ApiResponse<ConsultationManagementDetailsResponse>> selectConsultationDetails(
            Authentication authentication, @PathVariable UUID reservationId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(
                service.selectConsultationManagementDetails(authentication, reservationId)));
    }

    @PatchMapping("/{reservationId}/status")
    public ResponseEntity<ApiResponse<ConsultationManagementDetailsResponse>> updateConsultationStatus(
            Authentication authentication, @PathVariable UUID reservationId,
            @Valid @RequestBody ConsultationManagementStatusRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(
                service.updateConsultationManagementStatus(authentication, reservationId, request)));
    }
}
