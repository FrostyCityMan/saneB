package com.saneb.domain.announcementsource.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 규칙 release의 상태와 정책 고정값입니다. */
public record AnnouncementSourceRuleReleaseSummaryResponse(
        UUID releaseId,
        String releaseCode,
        int versionNo,
        int rowVersion,
        String releaseStatusCode,
        String ruleSnapshotHash,
        String combinationOperatorCode,
        String bodyUnavailableActionCode,
        boolean attachmentAnalysisEnabled,
        boolean autoActivationEnabled,
        String changeNote,
        long ruleCount,
        long enabledRuleCount,
        OffsetDateTime createdAt,
        OffsetDateTime activatedAt,
        OffsetDateTime retiredAt
) {
}
