/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSupportTypeAssignmentCommand.java
 * 작성자: 김도훈
 */

package com.saneb.domain.announcement.vo;

import java.util.UUID;

public record AnnouncementSupportTypeAssignmentCommand(
        UUID id,
        UUID announcementId,
        String supportTypeCode,
        String assignmentSourceCode,
        UUID actorUserId
) {
}
