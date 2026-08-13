package com.saneb.domain.announcementsource.vo;

import java.util.UUID;

/** release 내 키워드 규칙 목록 검색 조건입니다. */
public record AnnouncementSourceKeywordRuleSearchCondition(
        UUID releaseId,
        String groupKindCode,
        String groupCode,
        String strengthCode,
        String matchModeCode,
        Boolean enabled,
        String keyword,
        int limit,
        int offset
) {
}
