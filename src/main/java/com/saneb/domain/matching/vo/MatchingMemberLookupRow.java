package com.saneb.domain.matching.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MatchingMemberLookupRow(
        UUID userId,
        String userCode,
        String loginId,
        String name,
        String statusCode,
        OffsetDateTime createdAt
) {
}
