/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceClassificationController.java
 * 작성자: 김도훈
 */

package com.saneb.domain.announcementsource.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceClassificationDetailsResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceConfirmedClassificationSaveRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceReclassificationRequest;
import com.saneb.domain.announcementsource.service.AnnouncementSourceClassificationManagementService;
import com.saneb.domain.announcementsource.service.AnnouncementSourceReclassificationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/announcement-sources/{sourceId}")
public class AnnouncementSourceClassificationController {

    private final AnnouncementSourceClassificationManagementService managementService;
    private final AnnouncementSourceReclassificationService reclassificationService;

    public AnnouncementSourceClassificationController(
            AnnouncementSourceClassificationManagementService managementService,
            AnnouncementSourceReclassificationService reclassificationService
    ) {
        this.managementService = managementService;
        this.reclassificationService = reclassificationService;
    }

    @PostMapping("/reclassifications")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AnnouncementSourceClassificationDetailsResponse> insertReclassification(
            Authentication authentication,
            @PathVariable UUID sourceId,
            @Valid @RequestBody AnnouncementSourceReclassificationRequest request
    ) {
        return ApiResponse.success(
                reclassificationService.insertReclassification(authentication, sourceId, request)
        );
    }

    @GetMapping("/classification")
    @PreAuthorize("hasAnyRole('OPERATOR', 'APPROVER', 'ADMIN')")
    public ApiResponse<AnnouncementSourceClassificationDetailsResponse> selectClassificationDetails(
            @PathVariable UUID sourceId
    ) {
        return ApiResponse.success(managementService.selectClassificationDetails(sourceId));
    }

    @PutMapping("/confirmed-classification")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<AnnouncementSourceClassificationDetailsResponse> saveConfirmedClassification(
            Authentication authentication,
            @PathVariable UUID sourceId,
            @Valid @RequestBody AnnouncementSourceConfirmedClassificationSaveRequest request
    ) {
        return ApiResponse.success(managementService.saveConfirmedClassification(authentication, sourceId, request));
    }
}
