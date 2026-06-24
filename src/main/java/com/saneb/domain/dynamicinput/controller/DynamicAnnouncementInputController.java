/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: DynamicAnnouncementInputController.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.dynamicinput.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.domain.dynamicinput.dto.AnnouncementInputRequirementsResponse;
import com.saneb.domain.dynamicinput.dto.AnnouncementInputRequirementsSaveRequest;
import com.saneb.domain.dynamicinput.dto.ApplicationInputValuesResponse;
import com.saneb.domain.dynamicinput.dto.ApplicationInputValuesSaveRequest;
import com.saneb.domain.dynamicinput.dto.StandardDocumentFieldResponse;
import com.saneb.domain.dynamicinput.service.DynamicAnnouncementInputService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class DynamicAnnouncementInputController {

    private final DynamicAnnouncementInputService dynamicAnnouncementInputService;

    public DynamicAnnouncementInputController(DynamicAnnouncementInputService dynamicAnnouncementInputService) {
        this.dynamicAnnouncementInputService = dynamicAnnouncementInputService;
    }

    @GetMapping("/announcements/{announcementId}/input-requirements")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<AnnouncementInputRequirementsResponse> selectAnnouncementInputRequirements(
            @PathVariable UUID announcementId
    ) {
        return ApiResponse.success(dynamicAnnouncementInputService.selectAnnouncementInputRequirements(announcementId));
    }

    @PutMapping("/announcements/{announcementId}/input-requirements")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<AnnouncementInputRequirementsResponse> saveAnnouncementInputRequirements(
            Authentication authentication,
            @PathVariable UUID announcementId,
            @Valid @RequestBody AnnouncementInputRequirementsSaveRequest request
    ) {
        return ApiResponse.success(dynamicAnnouncementInputService.saveAnnouncementInputRequirements(
                authentication,
                announcementId,
                request
        ));
    }

    @GetMapping("/application-progresses/{progressId}/input-values")
    @PreAuthorize("hasAnyRole('USER', 'PARTNER', 'OPERATOR', 'ADMIN')")
    public ApiResponse<ApplicationInputValuesResponse> selectApplicationInputValues(
            Authentication authentication,
            @PathVariable UUID progressId
    ) {
        return ApiResponse.success(dynamicAnnouncementInputService.selectApplicationInputValues(authentication, progressId));
    }

    @PutMapping("/application-progresses/{progressId}/input-values")
    @PreAuthorize("hasAnyRole('USER', 'PARTNER', 'OPERATOR', 'ADMIN')")
    public ApiResponse<ApplicationInputValuesResponse> saveApplicationInputValues(
            Authentication authentication,
            @PathVariable UUID progressId,
            @Valid @RequestBody ApplicationInputValuesSaveRequest request
    ) {
        return ApiResponse.success(dynamicAnnouncementInputService.saveApplicationInputValues(
                authentication,
                progressId,
                request
        ));
    }

    @GetMapping("/standard-document-fields")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<StandardDocumentFieldResponse>> selectStandardDocumentFieldList(
            @RequestParam(required = false) String documentTypeCode,
            @RequestParam(required = false) String scopeCode
    ) {
        return ApiResponse.success(dynamicAnnouncementInputService.selectStandardDocumentFieldList(
                documentTypeCode,
                scopeCode
        ));
    }
}
