package com.saneb.domain.matching.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MatchingCaseSummaryResponse(
        UUID matchingCaseId,
        UUID announcementId,
        UUID memberUserId,
        UUID verificationId,
        String statusCode,
        String blockedReasonCode,
        OffsetDateTime matchedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
