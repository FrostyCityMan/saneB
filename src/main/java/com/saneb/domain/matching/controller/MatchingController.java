package com.saneb.domain.matching.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.matching.dto.MatchingCandidateGenerateRequest;
import com.saneb.domain.matching.dto.MatchingCandidateGenerateResponse;
import com.saneb.domain.matching.dto.MatchingCaseCreateRequest;
import com.saneb.domain.matching.dto.MatchingCaseDetailsResponse;
import com.saneb.domain.matching.dto.MatchingCaseStatusUpdateRequest;
import com.saneb.domain.matching.dto.MatchingCaseSummaryResponse;
import com.saneb.domain.matching.dto.MatchingFinalRecalculateRequest;
import com.saneb.domain.matching.dto.MatchingMemberLookupResponse;
import com.saneb.domain.matching.dto.MatchingResultDetailResponse;
import com.saneb.domain.matching.service.MatchingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
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
@RequestMapping("/api/v1/matching/cases")
public class MatchingController {

    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<MatchingCaseDetailsResponse> insertMatchingCase(
            Authentication authentication,
            @Valid @RequestBody MatchingCaseCreateRequest request
    ) {
        return ApiResponse.success(matchingService.insertMatchingCase(authentication, request));
    }

    @PostMapping("/candidates")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<MatchingCandidateGenerateResponse> insertMatchingCandidates(
            Authentication authentication,
            @Valid @RequestBody MatchingCandidateGenerateRequest request
    ) {
        return ApiResponse.success(matchingService.insertMatchingCandidates(authentication, request));
    }

    @PostMapping("/final-recalculate")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<MatchingCandidateGenerateResponse> insertFinalMatchingCandidates(
            Authentication authentication,
            @Valid @RequestBody MatchingFinalRecalculateRequest request
    ) {
        return ApiResponse.success(matchingService.insertFinalMatchingCandidates(authentication, request));
    }

    @GetMapping("/basic-candidates")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<PageResponse<MatchingCaseSummaryResponse>> selectMyBasicMatchingCaseList(
            Authentication authentication,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(matchingService.selectMyBasicMatchingCaseList(authentication, page, size));
    }

    @GetMapping("/final")
    @PreAuthorize("hasAnyRole('OPERATOR', 'APPROVER', 'REVIEWER', 'ADMIN')")
    public ApiResponse<PageResponse<MatchingCaseSummaryResponse>> selectFinalMatchingCaseList(
            @RequestParam(required = false) UUID announcementId,
            @RequestParam(required = false) UUID memberUserId,
            @RequestParam(required = false) String statusCode,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(matchingService.selectFinalMatchingCaseList(
                announcementId,
                memberUserId,
                statusCode,
                page,
                size
        ));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'PARTNER', 'OPERATOR', 'APPROVER', 'REVIEWER', 'ADMIN')")
    public ApiResponse<PageResponse<MatchingCaseSummaryResponse>> selectMatchingCaseList(
            @RequestParam(required = false) UUID announcementId,
            @RequestParam(required = false) UUID memberUserId,
            @RequestParam(required = false) UUID verificationId,
            @RequestParam(required = false) String statusCode,
            @RequestParam(required = false) String matchingStageCode,
            @RequestParam(required = false) String matchingBasisCode,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(matchingService.selectMatchingCaseList(
                announcementId,
                memberUserId,
                verificationId,
                statusCode,
                matchingStageCode,
                matchingBasisCode,
                page,
                size
        ));
    }

    @GetMapping("/member-lookups")
    @PreAuthorize("hasAnyRole('OPERATOR', 'APPROVER', 'REVIEWER', 'ADMIN')")
    public ApiResponse<PageResponse<MatchingMemberLookupResponse>> selectMatchingMemberLookupList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
    ) {
        return ApiResponse.success(matchingService.selectMatchingMemberLookupList(keyword, page, size));
    }

    @GetMapping("/{matchingCaseId}")
    @PreAuthorize("hasAnyRole('USER', 'PARTNER', 'OPERATOR', 'APPROVER', 'REVIEWER', 'ADMIN')")
    public ApiResponse<MatchingCaseDetailsResponse> selectMatchingCaseDetails(
            @PathVariable UUID matchingCaseId
    ) {
        return ApiResponse.success(matchingService.selectMatchingCaseDetails(matchingCaseId));
    }

    @GetMapping("/{matchingCaseId}/results")
    @PreAuthorize("hasAnyRole('USER', 'PARTNER', 'OPERATOR', 'APPROVER', 'REVIEWER', 'ADMIN')")
    public ApiResponse<List<MatchingResultDetailResponse>> selectMatchingResultDetailList(
            @PathVariable UUID matchingCaseId
    ) {
        return ApiResponse.success(matchingService.selectMatchingResultDetailList(matchingCaseId));
    }

    @PatchMapping("/{matchingCaseId}/status")
    @PreAuthorize("hasAnyRole('OPERATOR', 'APPROVER', 'ADMIN')")
    public ApiResponse<MatchingCaseDetailsResponse> updateMatchingCaseStatus(
            Authentication authentication,
            @PathVariable UUID matchingCaseId,
            @Valid @RequestBody MatchingCaseStatusUpdateRequest request
    ) {
        return ApiResponse.success(matchingService.updateMatchingCaseStatus(
                authentication,
                matchingCaseId,
                request
        ));
    }
}
