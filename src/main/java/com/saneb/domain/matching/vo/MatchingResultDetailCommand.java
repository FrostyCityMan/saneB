package com.saneb.domain.matching.vo;

import java.util.UUID;

public record MatchingResultDetailCommand(
        UUID matchingResultDetailId,
        UUID matchingCaseId,
        String conditionScopeCode,
        String conditionKey,
        String resultCode,
        String basisValue,
        String requiredValue,
        String reason,
        UUID actorUserId
) {
}
