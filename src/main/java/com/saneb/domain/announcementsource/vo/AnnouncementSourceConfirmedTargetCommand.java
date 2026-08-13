/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceConfirmedTargetCommand.java
 * 작성자: 김도훈
 */

package com.saneb.domain.announcementsource.vo;

import java.util.UUID;

public record AnnouncementSourceConfirmedTargetCommand(
        UUID id,
        UUID sourceId,
        String targetCategoryCode,
        UUID evaluationId,
        UUID actorUserId
) {
}
