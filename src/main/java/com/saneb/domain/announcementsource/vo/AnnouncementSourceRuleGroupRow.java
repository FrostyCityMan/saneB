package com.saneb.domain.announcementsource.vo;

import java.util.UUID;

/** 키워드가 배치될 release 규칙 그룹입니다. */
public record AnnouncementSourceRuleGroupRow(
        UUID groupId,
        UUID releaseId,
        String releaseStatusCode,
        String groupCode,
        String groupName,
        String groupKindCode,
        String targetCategoryCode,
        String supportTypeCode,
        Boolean enabled
) {
}
