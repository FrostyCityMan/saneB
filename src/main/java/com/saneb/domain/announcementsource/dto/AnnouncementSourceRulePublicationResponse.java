package com.saneb.domain.announcementsource.dto;

/** 게시 transaction 결과와 서버측 Golden QA 식별자입니다. */
public record AnnouncementSourceRulePublicationResponse(
        AnnouncementSourceRuleReleaseSummaryResponse previousRelease,
        AnnouncementSourceRuleReleaseSummaryResponse activeRelease,
        int addedRuleCount,
        int modifiedRuleCount,
        int disabledRuleCount,
        int expectedDecisionChangeCount,
        String goldenSetRunId,
        int goldenCaseCount
) {
}
