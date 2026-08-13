/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementV2Controller.java
 * 작성자: 김도훈
 */

package com.saneb.domain.announcement.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.domain.announcement.dto.AnnouncementDetailsResponse;
import com.saneb.domain.announcement.dto.AnnouncementV2SaveRequest;
import com.saneb.domain.announcement.service.AnnouncementService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v2/announcements")
public class AnnouncementV2Controller {

    private final AnnouncementService announcementService;

    public AnnouncementV2Controller(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<AnnouncementDetailsResponse> insertAnnouncement(
            Authentication authentication,
            @Valid @RequestBody AnnouncementV2SaveRequest request
    ) {
        return ApiResponse.success(announcementService.insertAnnouncementV2(authentication, request));
    }

    @PutMapping("/{announcementId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<AnnouncementDetailsResponse> updateAnnouncement(
            Authentication authentication,
            @PathVariable UUID announcementId,
            @Valid @RequestBody AnnouncementV2SaveRequest request
    ) {
        return ApiResponse.success(announcementService.updateAnnouncementV2(authentication, announcementId, request));
    }
}
