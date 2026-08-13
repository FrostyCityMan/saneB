package com.saneb.domain.announcementsource.vo;

import java.util.UUID;

/** 신규 DRAFT 키워드 규칙 저장 값입니다. */
public record AnnouncementSourceKeywordRuleInsertCommand(
        UUID ruleId,
        UUID groupId,
        String ruleCode,
        String strengthCode,
        int sortOrder,
        UUID actorUserId
) {
}
