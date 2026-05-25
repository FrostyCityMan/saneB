package com.saneb.domain.matching.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MatchingResultDetailResponse(
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
