/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceController.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionApprovalRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionRequestCreateRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionRequestResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionRunDetailsResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionRunResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceDetailsResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceDuplicateDecisionRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceLinkResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceReviewStatusUpdateRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceSummaryResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceToAnnouncementRequest;
import com.saneb.domain.announcementsource.service.AnnouncementSourceService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin")
public class AnnouncementSourceController {

    private final AnnouncementSourceService announcementSourceService;

    public AnnouncementSourceController(AnnouncementSourceService announcementSourceService) {
        this.announcementSourceService = announcementSourceService;
    }

    @PostMapping("/announcement-source-collections/requests")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<AnnouncementSourceCollectionRequestResponse> insertCollectionRequest(
            Authentication authentication,
            @Valid @RequestBody AnnouncementSourceCollectionRequestCreateRequest request
    ) {
        return ApiResponse.success(announcementSourceService.insertCollectionRequest(authentication, request));
    }

    @GetMapping("/announcement-source-collections/requests")
    @PreAuthorize("hasAnyRole('OPERATOR', 'APPROVER', 'ADMIN')")
    public ApiResponse<PageResponse<AnnouncementSourceCollectionRequestResponse>> selectCollectionRequestList(
            @RequestParam(required = false) String providerCode,
            @RequestParam(required = false) String requestTypeCode,
            @RequestParam(required = false) String requestStatusCode,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(announcementSourceService.selectCollectionRequestList(
                providerCode,
                requestTypeCode,
                requestStatusCode,
                page,
                size
        ));
    }

    @GetMapping("/announcement-source-collections/requests/{requestId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'APPROVER', 'ADMIN')")
    public ApiResponse<AnnouncementSourceCollectionRequestResponse> selectCollectionRequestDetails(
            @PathVariable UUID requestId
    ) {
        return ApiResponse.success(announcementSourceService.selectCollectionRequestDetails(requestId));
    }

    @PatchMapping("/announcement-source-collections/requests/{requestId}/approval")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public ApiResponse<AnnouncementSourceCollectionRequestResponse> updateCollectionRequestApproval(
            Authentication authentication,
            @PathVariable UUID requestId,
            @Valid @RequestBody AnnouncementSourceCollectionApprovalRequest request
    ) {
        return ApiResponse.success(announcementSourceService.updateCollectionRequestApproval(
                authentication,
                requestId,
                request
        ));
    }

    @PostMapping("/announcement-source-collections/requests/{requestId}/runs")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public ApiResponse<AnnouncementSourceCollectionRunResponse> insertCollectionRun(@PathVariable UUID requestId) {
        return ApiResponse.success(announcementSourceService.insertCollectionRun(requestId));
    }

    @GetMapping("/announcement-source-collections/runs")
    @PreAuthorize("hasAnyRole('OPERATOR', 'APPROVER', 'ADMIN')")
    public ApiResponse<PageResponse<AnnouncementSourceCollectionRunResponse>> selectCollectionRunList(
            @RequestParam(required = false) UUID requestId,
            @RequestParam(required = false) String runStatusCode,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(announcementSourceService.selectCollectionRunList(
                requestId,
                runStatusCode,
                page,
                size
        ));
    }

    @GetMapping("/announcement-source-collections/runs/{runId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'APPROVER', 'ADMIN')")
    public ApiResponse<AnnouncementSourceCollectionRunDetailsResponse> selectCollectionRunDetails(
            @PathVariable UUID runId
    ) {
        return ApiResponse.success(announcementSourceService.selectCollectionRunDetails(runId));
    }

    @GetMapping("/announcement-sources")
    @PreAuthorize("hasAnyRole('OPERATOR', 'APPROVER', 'ADMIN')")
    public ApiResponse<PageResponse<AnnouncementSourceSummaryResponse>> selectSourceList(
            @RequestParam(required = false) String providerCode,
            @RequestParam(required = false) String reviewStatusCode,
            @RequestParam(required = false) String semanticStatusCode,
            @RequestParam(required = false) String targetCategoryCode,
            @RequestParam(required = false) String supportTypeCode,
            @RequestParam(required = false) String matchedGroupCode,
            @RequestParam(required = false) String matchedGroupKindCode,
            @RequestParam(required = false) String matchLocationCode,
            @RequestParam(required = false) UUID ruleReleaseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(announcementSourceService.selectSourceList(
                providerCode,
                reviewStatusCode,
                semanticStatusCode,
                targetCategoryCode,
                supportTypeCode,
                matchedGroupCode,
                matchedGroupKindCode,
                matchLocationCode,
                ruleReleaseId,
                keyword,
                page,
                size
        ));
    }

    @GetMapping("/announcement-sources/{sourceId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'APPROVER', 'ADMIN')")
    public ApiResponse<AnnouncementSourceDetailsResponse> selectSourceDetails(@PathVariable UUID sourceId) {
        return ApiResponse.success(announcementSourceService.selectSourceDetails(sourceId));
    }

    @PatchMapping("/announcement-sources/{sourceId}/review-status")
    @PreAuthorize("hasAnyRole('OPERATOR', 'APPROVER', 'ADMIN')")
    public ApiResponse<AnnouncementSourceDetailsResponse> updateSourceReviewStatus(
            Authentication authentication,
            @PathVariable UUID sourceId,
            @Valid @RequestBody AnnouncementSourceReviewStatusUpdateRequest request
    ) {
        return ApiResponse.success(announcementSourceService.updateSourceReviewStatus(authentication, sourceId, request));
    }

    @PatchMapping("/announcement-sources/{sourceId}/duplicate-candidates/{candidateId}/decision")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<AnnouncementSourceDetailsResponse> updateDuplicateCandidateDecision(
            Authentication authentication,
            @PathVariable UUID sourceId,
            @PathVariable UUID candidateId,
            @Valid @RequestBody AnnouncementSourceDuplicateDecisionRequest request
    ) {
        return ApiResponse.success(announcementSourceService.updateDuplicateCandidateDecision(
                authentication,
                sourceId,
                candidateId,
                request
        ));
    }

    @PatchMapping("/announcement-sources/{sourceId}/source-duplicates/{duplicateId}/decision")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<AnnouncementSourceDetailsResponse> updateSnapshotDuplicateDecision(
            Authentication authentication,
            @PathVariable UUID sourceId,
            @PathVariable UUID duplicateId,
            @Valid @RequestBody AnnouncementSourceDuplicateDecisionRequest request
    ) {
        return ApiResponse.success(announcementSourceService.updateSnapshotDuplicateDecision(
                authentication,
                sourceId,
                duplicateId,
                request
        ));
    }

    @PostMapping("/announcement-sources/{sourceId}/announcements")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<AnnouncementSourceLinkResponse> insertOperationalAnnouncement(
            Authentication authentication,
            @PathVariable UUID sourceId,
            @Valid @RequestBody AnnouncementSourceToAnnouncementRequest request
    ) {
        return ApiResponse.success(announcementSourceService.insertOperationalAnnouncement(
                authentication,
                sourceId,
                request
        ));
    }
}
