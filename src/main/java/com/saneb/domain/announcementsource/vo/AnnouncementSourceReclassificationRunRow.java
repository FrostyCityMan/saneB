package com.saneb.domain.announcementsource.vo;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AnnouncementSourceReclassificationRunRow(
        UUID runId,
        UUID ruleReleaseId,
        String ruleReleaseCode,
        String ruleSnapshotHash,
        String runStatusCode,
        String providerCode,
        LocalDate collectedFrom,
        LocalDate collectedTo,
        boolean includeLinkedAnnouncements,
        int maximumCount,
        int batchSize,
        int totalCount,
        int pendingCount,
        int previewedCount,
        int acceptedCount,
        int reviewRequiredCount,
        int excludedCount,
        int appliedCount,
        int conflictCount,
        int failedCount,
        int rolledBackCount,
        UUID requestedBy,
        int rowVersion,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime previewCompletedAt,
        OffsetDateTime applicationCompletedAt,
        OffsetDateTime rollbackCompletedAt
) {
}
