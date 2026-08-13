package com.saneb.domain.announcementsource.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceKeywordRuleDeleteRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceKeywordRuleSaveRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceKeywordRuleStatusUpdateRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceKeywordRuleSummaryResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRulePreviewRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRulePreviewResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRulePublicationRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRulePublicationResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRuleReleaseCreateRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRuleReleaseSummaryResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRuleGoldenSetRunRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRuleGoldenSetRunResponse;
import com.saneb.domain.announcementsource.service.AnnouncementSourceRuleReleaseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

/** 공고 분류 release와 키워드 규칙 관리자 API입니다. */
@Validated
@RestController
@RequestMapping("/api/v1/admin/announcement-source-rule-releases")
public class AnnouncementSourceRuleReleaseController {

    private final AnnouncementSourceRuleReleaseService ruleReleaseService;

    public AnnouncementSourceRuleReleaseController(AnnouncementSourceRuleReleaseService ruleReleaseService) {
        this.ruleReleaseService = ruleReleaseService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OPERATOR', 'APPROVER', 'ADMIN')")
    public ApiResponse<PageResponse<AnnouncementSourceRuleReleaseSummaryResponse>> selectRuleReleaseList(
            @RequestParam(required = false) String releaseStatusCode,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(ruleReleaseService.selectRuleReleaseList(releaseStatusCode, page, size));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AnnouncementSourceRuleReleaseSummaryResponse> insertRuleReleaseDraft(
            Authentication authentication,
            @Valid @RequestBody AnnouncementSourceRuleReleaseCreateRequest request
    ) {
        return ApiResponse.success(ruleReleaseService.insertRuleReleaseDraft(authentication, request));
    }

    @GetMapping("/{releaseId}/keyword-rules")
    @PreAuthorize("hasAnyRole('OPERATOR', 'APPROVER', 'ADMIN')")
    public ApiResponse<PageResponse<AnnouncementSourceKeywordRuleSummaryResponse>> selectKeywordRuleList(
            @PathVariable UUID releaseId,
            @RequestParam(required = false) String groupKindCode,
            @RequestParam(required = false) String groupCode,
            @RequestParam(required = false) String strengthCode,
            @RequestParam(required = false) String matchModeCode,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(ruleReleaseService.selectKeywordRuleList(
                releaseId,
                groupKindCode,
                groupCode,
                strengthCode,
                matchModeCode,
                enabled,
                keyword,
                page,
                size
        ));
    }

    @PostMapping("/{releaseId}/keyword-rules")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AnnouncementSourceKeywordRuleSummaryResponse> insertKeywordRule(
            Authentication authentication,
            @PathVariable UUID releaseId,
            @Valid @RequestBody AnnouncementSourceKeywordRuleSaveRequest request
    ) {
        return ApiResponse.success(ruleReleaseService.insertKeywordRule(authentication, releaseId, request));
    }

    @PutMapping("/{releaseId}/keyword-rules/{ruleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AnnouncementSourceKeywordRuleSummaryResponse> updateKeywordRule(
            Authentication authentication,
            @PathVariable UUID releaseId,
            @PathVariable UUID ruleId,
            @Valid @RequestBody AnnouncementSourceKeywordRuleSaveRequest request
    ) {
        return ApiResponse.success(ruleReleaseService.updateKeywordRule(
                authentication,
                releaseId,
                ruleId,
                request
        ));
    }

    @PatchMapping("/{releaseId}/keyword-rules/{ruleId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AnnouncementSourceKeywordRuleSummaryResponse> updateKeywordRuleStatus(
            Authentication authentication,
            @PathVariable UUID releaseId,
            @PathVariable UUID ruleId,
            @Valid @RequestBody AnnouncementSourceKeywordRuleStatusUpdateRequest request
    ) {
        return ApiResponse.success(ruleReleaseService.updateKeywordRuleStatus(
                authentication,
                releaseId,
                ruleId,
                request
        ));
    }

    @DeleteMapping("/{releaseId}/keyword-rules/{ruleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteKeywordRule(
            Authentication authentication,
            @PathVariable UUID releaseId,
            @PathVariable UUID ruleId,
            @Valid @RequestBody AnnouncementSourceKeywordRuleDeleteRequest request
    ) {
        ruleReleaseService.deleteKeywordRule(authentication, releaseId, ruleId, request);
        return ApiResponse.success(null, "키워드 규칙을 삭제했습니다.");
    }

    @PostMapping("/{releaseId}/preview")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AnnouncementSourceRulePreviewResponse> selectPreview(
            @PathVariable UUID releaseId,
            @Valid @RequestBody AnnouncementSourceRulePreviewRequest request
    ) {
        return ApiResponse.success(ruleReleaseService.selectPreview(releaseId, request));
    }

    @PostMapping("/{releaseId}/publication")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AnnouncementSourceRulePublicationResponse> updateRuleReleasePublication(
            Authentication authentication,
            @PathVariable UUID releaseId,
            @Valid @RequestBody AnnouncementSourceRulePublicationRequest request
    ) {
        return ApiResponse.success(ruleReleaseService.updateRuleReleasePublication(
                authentication,
                releaseId,
                request
        ));
    }

    @PostMapping("/{releaseId}/golden-set-runs")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AnnouncementSourceRuleGoldenSetRunResponse> insertGoldenSetRun(
            Authentication authentication,
            @PathVariable UUID releaseId,
            @Valid @RequestBody AnnouncementSourceRuleGoldenSetRunRequest request
    ) {
        return ApiResponse.success(
                ruleReleaseService.insertGoldenSetRun(authentication, releaseId, request)
        );
    }
}
