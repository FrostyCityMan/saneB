package com.saneb.domain.announcementsource.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 규칙 release 조회 행입니다. */
public record AnnouncementSourceRuleReleaseRow(
        UUID releaseId,
        String releaseCode,
        Integer versionNo,
        Integer rowVersion,
        String releaseStatusCode,
        String ruleSnapshotHash,
        String combinationOperatorCode,
        String bodyUnavailableActionCode,
        Boolean attachmentAnalysisEnabled,
        Boolean autoActivationEnabled,
        String changeNote,
        Long ruleCount,
        Long enabledRuleCount,
        OffsetDateTime createdAt,
        OffsetDateTime activatedAt,
        OffsetDateTime retiredAt
) {
}
