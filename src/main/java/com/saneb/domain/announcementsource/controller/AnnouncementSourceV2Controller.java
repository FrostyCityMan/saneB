/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceV2Controller.java
 * 작성자: 김도훈
 */

package com.saneb.domain.announcementsource.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceLinkResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceV2ToAnnouncementRequest;
import com.saneb.domain.announcementsource.service.AnnouncementSourceV2ConversionService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v2/admin/announcement-sources")
public class AnnouncementSourceV2Controller {

    private final AnnouncementSourceV2ConversionService conversionService;

    public AnnouncementSourceV2Controller(AnnouncementSourceV2ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @PostMapping("/{sourceId}/announcements")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<AnnouncementSourceLinkResponse> insertOperationalAnnouncement(
            Authentication authentication,
            @PathVariable UUID sourceId,
            @Valid @RequestBody AnnouncementSourceV2ToAnnouncementRequest request
    ) {
        return ApiResponse.success(conversionService.insertOperationalAnnouncement(authentication, sourceId, request));
    }
}
