/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceClassificationStateRow.java
 * 작성자: 김도훈
 */

package com.saneb.domain.announcementsource.vo;

import java.util.UUID;

public record AnnouncementSourceClassificationStateRow(
        UUID sourceId,
        UUID decisionId,
        String decisionStatusCode,
        String reasonCode,
        String reviewStatusCode,
        Integer classificationRowVersion,
        Long confirmedTargetCount,
        Long confirmedSupportCount,
        Long staleConfirmedCount
) {

    public boolean confirmedForCurrentDecision() {
        return confirmedTargetCount != null
                && confirmedTargetCount > 0
                && confirmedSupportCount != null
                && confirmedSupportCount > 0
                && (staleConfirmedCount == null || staleConfirmedCount == 0);
    }
}
