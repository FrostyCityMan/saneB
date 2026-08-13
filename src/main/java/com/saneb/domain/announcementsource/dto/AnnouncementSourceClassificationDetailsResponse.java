/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceClassificationDetailsResponse.java
 * 작성자: 김도훈
 */

package com.saneb.domain.announcementsource.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AnnouncementSourceClassificationDetailsResponse(
        UUID sourceId,
        UUID decisionId,
        String ruleReleaseCode,
        String semanticStatusCode,
        String reasonCode,
        String titleStageCode,
        String bodyStageCode,
        String bodySourceCode,
        String bodyAvailabilityCode,
        List<String> targetCategoryCodes,
        List<String> supportTypeCodes,
        String confirmedClassificationStatusCode,
        List<String> confirmedTargetCategoryCodes,
        List<String> confirmedSupportTypeCodes,
        Integer version,
        OffsetDateTime evaluatedAt,
        List<MatchResponse> matches
) {

    public record MatchResponse(
            String ruleGroupCode,
            String canonicalKeyword,
            String matchedTerm,
            String locationCode,
            Integer startOffset,
            Integer endOffset,
            String appliedActionCode
    ) {
    }
}
