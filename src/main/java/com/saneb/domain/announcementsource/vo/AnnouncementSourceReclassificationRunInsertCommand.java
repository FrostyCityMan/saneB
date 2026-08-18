package com.saneb.domain.announcementsource.vo;

import java.time.LocalDate;
import java.util.UUID;

public record AnnouncementSourceReclassificationRunInsertCommand(
        UUID runId,
        UUID ruleReleaseId,
        String providerCode,
        LocalDate collectedFrom,
        LocalDate collectedTo,
        boolean includeLinkedAnnouncements,
        int maximumCount,
        int batchSize,
        String requestReasonHash,
        UUID requestedBy
) {
}
