package com.saneb.domain.announcementsource.dto;

import java.util.List;
import java.util.UUID;

/** 규칙 미리보기의 판정과 위치별 근거입니다. */
public record AnnouncementSourceRulePreviewResponse(
        UUID releaseId,
        String releaseCode,
        int version,
        String semanticStatusCode,
        String reasonCode,
        String titleStageCode,
        String bodyStageCode,
        String bodySourceCode,
        String bodyAvailabilityCode,
        List<String> targetCategoryCodes,
        List<String> supportTypeCodes,
        List<String> groupACodes,
        List<String> groupBCodes,
        List<MatchResponse> matches
) {
    /** 원문 일치 근거입니다. */
    public record MatchResponse(
            String ruleCode,
            String ruleGroupCode,
            String canonicalKeyword,
            String matchedTerm,
            String locationCode,
            int startOffset,
            int endOffset,
            String appliedActionCode,
            boolean maskedByProtectedMetadata
    ) {
    }
}
