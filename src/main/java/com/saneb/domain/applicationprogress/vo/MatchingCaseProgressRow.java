package com.saneb.domain.applicationprogress.vo;

import java.util.UUID;

public record MatchingCaseProgressRow(
        UUID matchingCaseId,
        UUID announcementId,
        UUID memberUserId,
        String statusCode
) {
}
