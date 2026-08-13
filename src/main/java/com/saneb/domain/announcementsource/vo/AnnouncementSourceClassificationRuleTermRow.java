/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceClassificationRuleTermRow.java
 * 작성자: 김도훈
 */

package com.saneb.domain.announcementsource.vo;

import java.util.UUID;

public record AnnouncementSourceClassificationRuleTermRow(
        UUID releaseId,
        String releaseCode,
        String ruleCode,
        String groupCode,
        String groupKindCode,
        String targetCategoryCode,
        String supportTypeCode,
        String strengthCode,
        Boolean ruleEnabled,
        String termTypeCode,
        String termText,
        String matchModeCode,
        Boolean discoveryTerm,
        Integer discoveryOrder,
        Boolean classificationTerm,
        Boolean termEnabled
) {
}
