/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: LocalGovernmentNoticeController.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.localgov.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionRequestResponse;
import com.saneb.domain.announcementsource.localgov.dto.AnnouncementSourceScheduleCreateRequest;
import com.saneb.domain.announcementsource.localgov.dto.AnnouncementSourceScheduleResponse;
import com.saneb.domain.announcementsource.localgov.dto.AnnouncementSourceScheduleStatusRequest;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeCollectionRequest;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeCollectionSummaryResponse;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeParserProfileResponse;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeQaCleanupRequest;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeQaCleanupResponse;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeSourceEnabledRequest;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeSourceResponse;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeSourceSaveRequest;
import com.saneb.domain.announcementsource.localgov.service.LocalGovernmentNoticeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('OPERATOR', 'APPROVER', 'ADMIN')")
public class LocalGovernmentNoticeController {

    private final LocalGovernmentNoticeService localGovernmentNoticeService;

    public LocalGovernmentNoticeController(LocalGovernmentNoticeService localGovernmentNoticeService) {
        this.localGovernmentNoticeService = localGovernmentNoticeService;
    }

    @GetMapping("/local-government-notice-sources")
    public ApiResponse<PageResponse<LocalGovernmentNoticeSourceResponse>> selectSourceList(
            @RequestParam(required = false) String sidoName,
            @RequestParam(required = false) String validationStatusCode,
            @RequestParam(required = false) String collectionStatusCode,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(localGovernmentNoticeService.selectSourceList(
                sidoName, validationStatusCode, collectionStatusCode, enabled, keyword, page, size
        ));
    }

    @GetMapping("/local-government-notice-sources/{sourceId}")
    public ApiResponse<LocalGovernmentNoticeSourceResponse> selectSourceDetails(@PathVariable UUID sourceId) {
        return ApiResponse.success(localGovernmentNoticeService.selectSourceDetails(sourceId));
    }

    @PostMapping("/local-government-notice-sources")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<LocalGovernmentNoticeSourceResponse> insertSource(
            Authentication authentication,
            @Valid @RequestBody LocalGovernmentNoticeSourceSaveRequest request
    ) {
        return ApiResponse.success(localGovernmentNoticeService.insertSource(authentication, request));
    }

    @PutMapping("/local-government-notice-sources/{sourceId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<LocalGovernmentNoticeSourceResponse> updateSource(
            Authentication authentication,
            @PathVariable UUID sourceId,
            @Valid @RequestBody LocalGovernmentNoticeSourceSaveRequest request
    ) {
        return ApiResponse.success(localGovernmentNoticeService.updateSource(authentication, sourceId, request));
    }

    @PatchMapping("/local-government-notice-sources/{sourceId}/enabled")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<LocalGovernmentNoticeSourceResponse> updateSourceEnabled(
            Authentication authentication,
            @PathVariable UUID sourceId,
            @Valid @RequestBody LocalGovernmentNoticeSourceEnabledRequest request
    ) {
        return ApiResponse.success(localGovernmentNoticeService.updateSourceEnabled(authentication, sourceId, request));
    }

    @DeleteMapping("/local-government-notice-sources/{sourceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteSource(Authentication authentication, @PathVariable UUID sourceId) {
        localGovernmentNoticeService.deleteSource(authentication, sourceId);
        return ApiResponse.success(null, "지자체 공고 URL을 삭제했습니다.");
    }

    @DeleteMapping("/local-government-notice-sources/qa-artifacts")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LocalGovernmentNoticeQaCleanupResponse> deleteQaArtifacts(
            Authentication authentication,
            @Valid @RequestBody LocalGovernmentNoticeQaCleanupRequest request
    ) {
        return ApiResponse.success(
                localGovernmentNoticeService.deleteQaArtifacts(authentication, request),
                "지자체 공고 QA 원문과 수집 이력을 정리했습니다."
        );
    }

    @PostMapping("/local-government-notice-sources/{sourceId}/collection-requests")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<AnnouncementSourceCollectionRequestResponse> insertCollectionRequest(
            Authentication authentication,
            @PathVariable UUID sourceId,
            @Valid @RequestBody(required = false) LocalGovernmentNoticeCollectionRequest request
    ) {
        return ApiResponse.success(localGovernmentNoticeService.insertCollectionRequest(authentication, sourceId, request));
    }

    @GetMapping("/local-government-notice-parser-profiles")
    public ApiResponse<List<LocalGovernmentNoticeParserProfileResponse>> selectParserProfileList() {
        return ApiResponse.success(localGovernmentNoticeService.selectParserProfileList());
    }

    @GetMapping("/local-government-notice-sources/collection-summary")
    public ApiResponse<LocalGovernmentNoticeCollectionSummaryResponse> selectCollectionSummary() {
        return ApiResponse.success(localGovernmentNoticeService.selectCollectionSummary());
    }

    @GetMapping("/announcement-source-collection-schedules")
    public ApiResponse<List<AnnouncementSourceScheduleResponse>> selectScheduleList() {
        return ApiResponse.success(localGovernmentNoticeService.selectScheduleList());
    }

    @PostMapping("/announcement-source-collection-schedules")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<AnnouncementSourceScheduleResponse> insertSchedule(
            Authentication authentication,
            @Valid @RequestBody AnnouncementSourceScheduleCreateRequest request
    ) {
        return ApiResponse.success(localGovernmentNoticeService.insertSchedule(authentication, request));
    }

    @PatchMapping("/announcement-source-collection-schedules/{scheduleId}/status")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public ApiResponse<AnnouncementSourceScheduleResponse> updateScheduleStatus(
            Authentication authentication,
            @PathVariable UUID scheduleId,
            @Valid @RequestBody AnnouncementSourceScheduleStatusRequest request
    ) {
        return ApiResponse.success(localGovernmentNoticeService.updateScheduleStatus(authentication, scheduleId, request));
    }
}
