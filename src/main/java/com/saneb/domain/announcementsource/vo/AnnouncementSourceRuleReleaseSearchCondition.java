package com.saneb.domain.announcementsource.vo;

/** 규칙 release 목록 검색 조건입니다. */
public record AnnouncementSourceRuleReleaseSearchCondition(
        String releaseStatusCode,
        int limit,
        int offset
) {
}
