package com.saneb.domain.announcementsource.vo;

import java.util.UUID;

/** 대표어 또는 유의어 저장 값입니다. */
public record AnnouncementSourceKeywordTermInsertCommand(
        UUID termId,
        UUID ruleId,
        UUID groupId,
        String termTypeCode,
        String termText,
        String normalizedTermText,
        String matchModeCode,
        boolean discoveryTerm,
        Integer discoveryOrder,
        boolean classificationTerm,
        UUID actorUserId
) {
}
