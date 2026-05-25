package com.saneb.domain.matching.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MatchingCaseDetailsResponse(
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
