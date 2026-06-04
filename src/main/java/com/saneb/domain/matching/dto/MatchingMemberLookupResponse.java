package com.saneb.domain.matching.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MatchingMemberLookupResponse(
        UUID userId,
        String loginId,
        String name,
        String statusCode,
        OffsetDateTime createdAt
) {
}
