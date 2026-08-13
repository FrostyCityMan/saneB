/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceClassificationDetailsRow.java
 * 작성자: 김도훈
 */

package com.saneb.domain.announcementsource.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AnnouncementSourceClassificationDetailsRow(
        UUID sourceId,
        UUID decisionId,
        String ruleReleaseCode,
        String semanticStatusCode,
        String reasonCode,
        String titleStageCode,
        String bodyStageCode,
        String bodySourceCode,
        String bodyAvailabilityCode,
        Integer version,
        OffsetDateTime evaluatedAt
) {
}
