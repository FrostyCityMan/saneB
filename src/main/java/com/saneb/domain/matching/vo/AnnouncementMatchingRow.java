package com.saneb.domain.matching.vo;

import java.util.UUID;

public record AnnouncementMatchingRow(
        UUID announcementId,
        String manualStatusCode,
        String approvalStatusCode
) {
}
