package com.saneb.domain.announcementsource.vo;

import java.util.UUID;

/** 낙관적 잠금이 포함된 DRAFT 키워드 규칙 수정 값입니다. */
public record AnnouncementSourceKeywordRuleUpdateCommand(
        UUID ruleId,
        UUID groupId,
        String strengthCode,
        int sortOrder,
        int expectedVersion,
        UUID actorUserId
) {
}
