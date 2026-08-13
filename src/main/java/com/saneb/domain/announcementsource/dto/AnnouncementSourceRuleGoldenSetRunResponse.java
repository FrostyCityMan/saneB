package com.saneb.domain.announcementsource.dto;

public record AnnouncementSourceRuleGoldenSetRunResponse(
        String goldenSetRunId,
        String ruleSnapshotHash,
        int goldenCaseCount
) {
}
