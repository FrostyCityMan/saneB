package com.saneb.domain.partnerverification.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.partnerverification.dto.PartnerVerificationCreateRequest;
import com.saneb.domain.partnerverification.dto.PartnerVerificationDetailsResponse;
import com.saneb.domain.partnerverification.dto.PartnerVerificationStatusUpdateRequest;
import com.saneb.domain.partnerverification.dto.PartnerVerificationSummaryResponse;
import com.saneb.domain.partnerverification.dto.VerificationBusinessValuesSaveRequest;
import com.saneb.domain.partnerverification.dto.VerificationDocumentsSaveRequest;
import com.saneb.domain.partnerverification.dto.VerificationFamilyValuesSaveRequest;
import com.saneb.domain.partnerverification.dto.VerificationMemberValuesSaveRequest;
import com.saneb.domain.partnerverification.dto.VerificationRestrictionFlagsSaveRequest;
import com.saneb.domain.partnerverification.service.PartnerVerificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/partner-verifications")
public class PartnerVerificationController {

    private final PartnerVerificationService partnerVerificationService;

    public PartnerVerificationController(PartnerVerificationService partnerVerificationService) {
        this.partnerVerificationService = partnerVerificationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PARTNER', 'OPERATOR', 'APPROVER', 'REVIEWER', 'ADMIN')")
    public ApiResponse<PageResponse<PartnerVerificationSummaryResponse>> selectPartnerVerificationList(
            @RequestParam(required = false) UUID memberUserId,
            @RequestParam(required = false) UUID partnerUserId,
            @RequestParam(required = false) String statusCode,
            @RequestParam(required = false) Boolean current,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(partnerVerificationService.selectPartnerVerificationList(
                memberUserId,
                partnerUserId,
                statusCode,
                current,
                page,
                size
        ));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PARTNER', 'OPERATOR', 'ADMIN')")
    public ApiResponse<PartnerVerificationDetailsResponse> insertPartnerVerification(
            Authentication authentication,
            @Valid @RequestBody PartnerVerificationCreateRequest request
    ) {
        return ApiResponse.success(partnerVerificationService.insertPartnerVerification(authentication, request));
    }

    @GetMapping("/{verificationId}")
    @PreAuthorize("hasAnyRole('PARTNER', 'OPERATOR', 'APPROVER', 'REVIEWER', 'ADMIN')")
    public ApiResponse<PartnerVerificationDetailsResponse> selectPartnerVerificationDetails(
            @PathVariable UUID verificationId
    ) {
        return ApiResponse.success(partnerVerificationService.selectPartnerVerificationDetails(verificationId));
    }

    @PutMapping("/{verificationId}/member-values")
    @PreAuthorize("hasAnyRole('PARTNER', 'OPERATOR', 'ADMIN')")
    public ApiResponse<Void> updateVerificationMemberValues(
            Authentication authentication,
            @PathVariable UUID verificationId,
            @Valid @RequestBody VerificationMemberValuesSaveRequest request
    ) {
        partnerVerificationService.updateVerificationMemberValues(authentication, verificationId, request);
        return ApiResponse.success(null);
    }

    @PutMapping("/{verificationId}/business-values")
    @PreAuthorize("hasAnyRole('PARTNER', 'OPERATOR', 'ADMIN')")
    public ApiResponse<Void> updateVerificationBusinessValues(
            Authentication authentication,
            @PathVariable UUID verificationId,
            @Valid @RequestBody VerificationBusinessValuesSaveRequest request
    ) {
        partnerVerificationService.updateVerificationBusinessValues(authentication, verificationId, request);
        return ApiResponse.success(null);
    }

    @PutMapping("/{verificationId}/family-values")
    @PreAuthorize("hasAnyRole('PARTNER', 'OPERATOR', 'ADMIN')")
    public ApiResponse<Void> updateVerificationFamilyValues(
            Authentication authentication,
            @PathVariable UUID verificationId,
            @Valid @RequestBody VerificationFamilyValuesSaveRequest request
    ) {
        partnerVerificationService.updateVerificationFamilyValues(authentication, verificationId, request);
        return ApiResponse.success(null);
    }

    @PutMapping("/{verificationId}/documents")
    @PreAuthorize("hasAnyRole('PARTNER', 'OPERATOR', 'ADMIN')")
    public ApiResponse<Void> updateVerificationDocuments(
            Authentication authentication,
            @PathVariable UUID verificationId,
            @Valid @RequestBody VerificationDocumentsSaveRequest request
    ) {
        partnerVerificationService.updateVerificationDocuments(authentication, verificationId, request);
        return ApiResponse.success(null);
    }

    @PutMapping("/{verificationId}/restriction-flags")
    @PreAuthorize("hasAnyRole('PARTNER', 'OPERATOR', 'ADMIN')")
    public ApiResponse<Void> updateVerificationRestrictionFlags(
            Authentication authentication,
            @PathVariable UUID verificationId,
            @Valid @RequestBody VerificationRestrictionFlagsSaveRequest request
    ) {
        partnerVerificationService.updateVerificationRestrictionFlags(authentication, verificationId, request);
        return ApiResponse.success(null);
    }

    @PatchMapping("/{verificationId}/status")
    @PreAuthorize("hasAnyRole('PARTNER', 'OPERATOR', 'APPROVER', 'ADMIN')")
    public ApiResponse<PartnerVerificationDetailsResponse> updatePartnerVerificationStatus(
            Authentication authentication,
            @PathVariable UUID verificationId,
            @Valid @RequestBody PartnerVerificationStatusUpdateRequest request
    ) {
        return ApiResponse.success(partnerVerificationService.updatePartnerVerificationStatus(
                authentication,
                verificationId,
                request
        ));
    }
}
