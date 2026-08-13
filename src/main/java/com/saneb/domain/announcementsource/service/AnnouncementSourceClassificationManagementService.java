/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceClassificationManagementService.java
 * 작성자: 김도훈
 */

package com.saneb.domain.announcementsource.service;

import com.saneb.domain.announcementsource.dto.AnnouncementSourceClassificationDetailsResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceConfirmedClassificationSaveRequest;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementSourceClassificationManagementService {

    AnnouncementSourceClassificationDetailsResponse selectClassificationDetails(UUID sourceId);

    AnnouncementSourceClassificationDetailsResponse saveConfirmedClassification(
            Authentication authentication,
            UUID sourceId,
            AnnouncementSourceConfirmedClassificationSaveRequest request
    );
}
