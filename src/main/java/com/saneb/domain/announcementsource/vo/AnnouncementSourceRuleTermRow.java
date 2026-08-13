package com.saneb.domain.announcementsource.vo;

import java.util.UUID;

/** release hash와 판정 엔진 구성에 사용하는 정렬된 규칙·term 행입니다. */
public record AnnouncementSourceRuleTermRow(
        UUID releaseId,
        String releaseCode,
        String releaseStatusCode,
        Integer releaseVersionNo,
        Integer releaseRowVersion,
        UUID groupId,
        String groupCode,
        String groupKindCode,
        String titleActionCode,
        String bodyActionCode,
        Integer groupSortOrder,
        Boolean groupEnabled,
        String targetCategoryCode,
        String supportTypeCode,
        UUID ruleId,
        String ruleCode,
        String strengthCode,
        Integer ruleSortOrder,
        Integer ruleRowVersion,
        Boolean ruleEnabled,
        UUID termId,
        String termTypeCode,
        String termText,
        String normalizedTermText,
        String matchModeCode,
        Boolean discoveryTerm,
        Integer discoveryOrder,
        Boolean classificationTerm,
        Boolean termEnabled
) {
}
