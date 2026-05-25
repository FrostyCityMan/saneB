package com.saneb.domain.matching.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MatchingCaseRow(
        UUID matchingCaseId,
        UUID announcementId,
        UUID memberUserId,
        UUID verificationId,
        String statusCode,
        String blockedReasonCode,
        OffsetDateTime matchedAt,
        UUID reviewedBy,
        OffsetDateTime reviewedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
