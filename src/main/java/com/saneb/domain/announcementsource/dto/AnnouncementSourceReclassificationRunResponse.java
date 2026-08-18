package com.saneb.domain.announcementsource.dto;

import com.saneb.domain.announcementsource.vo.AnnouncementSourceReclassificationRunRow;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AnnouncementSourceReclassificationRunResponse(
        UUID runId,
        UUID ruleReleaseId,
        String ruleReleaseCode,
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
        int rowVersion,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime previewCompletedAt,
        OffsetDateTime applicationCompletedAt,
        OffsetDateTime rollbackCompletedAt
) {

    public static AnnouncementSourceReclassificationRunResponse from(AnnouncementSourceReclassificationRunRow row) {
        return new AnnouncementSourceReclassificationRunResponse(
                row.runId(), row.ruleReleaseId(), row.ruleReleaseCode(), row.runStatusCode(),
                row.providerCode(), row.collectedFrom(), row.collectedTo(), row.includeLinkedAnnouncements(),
                row.maximumCount(), row.batchSize(), row.totalCount(), row.pendingCount(), row.previewedCount(),
                row.acceptedCount(), row.reviewRequiredCount(), row.excludedCount(), row.appliedCount(),
                row.conflictCount(), row.failedCount(), row.rolledBackCount(), row.rowVersion(), row.createdAt(),
                row.updatedAt(), row.previewCompletedAt(), row.applicationCompletedAt(), row.rollbackCompletedAt()
        );
    }
}
