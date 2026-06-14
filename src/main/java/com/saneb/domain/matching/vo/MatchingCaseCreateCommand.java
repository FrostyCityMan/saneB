package com.saneb.domain.matching.vo;

import java.util.UUID;

public record MatchingCaseCreateCommand(
        UUID matchingCaseId,
        UUID announcementId,
        UUID memberUserId,
        UUID verificationId,
        String statusCode,
        String blockedReasonCode,
        String matchingStageCode,
        String matchingBasisCode,
        UUID actorUserId
) {
}
