package com.saneb.domain.matching.vo;

import java.util.UUID;

public record MatchingCaseSearchCondition(
        UUID announcementId,
        UUID memberUserId,
        UUID verificationId,
        String statusCode,
        String matchingStageCode,
        String matchingBasisCode,
        int page,
        int size,
        int offset
) {
}
