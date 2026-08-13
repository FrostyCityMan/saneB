package com.saneb.domain.announcementsource.vo;

import java.util.UUID;

/** 관리자 키워드 규칙 목록·상세 조회 행입니다. */
public record AnnouncementSourceKeywordRuleRow(
        UUID ruleId,
        UUID releaseId,
        String releaseStatusCode,
        Integer releaseRowVersion,
        Integer ruleRowVersion,
        UUID groupId,
        String groupCode,
        String groupName,
        String groupKindCode,
        String targetCategoryCode,
        String supportTypeCode,
        String ruleCode,
        String strengthCode,
        Boolean ruleEnabled,
        Integer sortOrder,
        String canonicalKeyword,
        String matchModeCode,
        Boolean discoveryTerm,
        Integer discoveryOrder,
        String synonymsText
) {
}
