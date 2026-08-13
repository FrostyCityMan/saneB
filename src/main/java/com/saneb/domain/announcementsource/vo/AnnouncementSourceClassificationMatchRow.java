/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceClassificationMatchRow.java
 * 작성자: 김도훈
 */

package com.saneb.domain.announcementsource.vo;

public record AnnouncementSourceClassificationMatchRow(
        String ruleGroupCode,
        String canonicalKeyword,
        String matchedTerm,
        String locationCode,
        Integer startOffset,
        Integer endOffset,
        String appliedActionCode
) {
}
