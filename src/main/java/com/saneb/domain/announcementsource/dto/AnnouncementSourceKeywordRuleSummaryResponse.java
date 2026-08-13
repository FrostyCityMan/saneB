package com.saneb.domain.announcementsource.dto;

import java.util.List;
import java.util.UUID;

/** 관리자 키워드 목록 한 행입니다. */
public record AnnouncementSourceKeywordRuleSummaryResponse(
        UUID ruleId,
        UUID releaseId,
        String releaseStatusCode,
        int rowVersion,
        String ruleCode,
        String ruleGroupCode,
        String ruleGroupName,
        String groupKindCode,
        String targetCategoryCode,
        String supportTypeCode,
        String canonicalKeyword,
        List<String> synonyms,
        String strengthCode,
        String matchModeCode,
        boolean discoveryTerm,
        Integer discoveryOrder,
        boolean enabled,
        int sortOrder
) {
}
