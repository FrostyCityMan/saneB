package com.saneb.domain.matching.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MatchingResultDetailRow(
        UUID matchingResultDetailId,
        UUID matchingCaseId,
        String conditionScopeCode,
        String conditionKey,
        String resultCode,
        String basisValue,
        String requiredValue,
        String reason,
        OffsetDateTime createdAt
) {
}
