package com.saneb.domain.matching.vo;

import java.util.UUID;

public record MatchingCaseStatusCommand(
        UUID matchingCaseId,
        String statusCode,
        String blockedReasonCode,
        UUID actorUserId
) {
}
