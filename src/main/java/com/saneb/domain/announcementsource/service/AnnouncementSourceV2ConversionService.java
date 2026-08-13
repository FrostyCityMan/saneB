/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceV2ConversionService.java
 * 작성자: 김도훈
 */

package com.saneb.domain.announcementsource.service;

import com.saneb.domain.announcementsource.dto.AnnouncementSourceLinkResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceV2ToAnnouncementRequest;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementSourceV2ConversionService {

    AnnouncementSourceLinkResponse insertOperationalAnnouncement(
            Authentication authentication,
            UUID sourceId,
            AnnouncementSourceV2ToAnnouncementRequest request
    );
}
