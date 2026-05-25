package com.saneb.domain.applicationprogress.vo;

import java.util.UUID;

public record ApplicationProgressSearchCondition(
        UUID announcementId,
        UUID memberUserId,
        UUID matchingCaseId,
        String statusCode,
        int page,
        int size,
        int offset
) {
}
