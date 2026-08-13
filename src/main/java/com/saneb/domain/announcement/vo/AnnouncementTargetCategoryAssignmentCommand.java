/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementTargetCategoryAssignmentCommand.java
 * 작성자: 김도훈
 */

package com.saneb.domain.announcement.vo;

import java.util.UUID;

public record AnnouncementTargetCategoryAssignmentCommand(
        UUID id,
        UUID announcementId,
        String targetCategoryCode,
        boolean primary,
        String assignmentSourceCode,
        UUID actorUserId
) {
}
