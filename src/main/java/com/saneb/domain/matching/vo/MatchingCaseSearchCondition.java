package com.saneb.domain.matching.vo;

import java.util.UUID;

public record MatchingCaseSearchCondition(
        UUID announcementId,
        UUID memberUserId,
        UUID verificationId,
        String statusCode,
        int page,
        int size,
        int offset
) {
}
