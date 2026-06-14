package com.saneb.domain.matching.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MatchingCaseDetailsResponse(
        UUID matchingCaseId,
        String matchingCaseCode,
        UUID announcementId,
        String announcementCode,
        UUID memberUserId,
        String memberUserCode,
        UUID verificationId,
        String verificationCode,
        String statusCode,
        String blockedReasonCode,
        String matchingStageCode,
        String matchingBasisCode,
        OffsetDateTime matchedAt,
        UUID reviewedBy,
        OffsetDateTime reviewedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
