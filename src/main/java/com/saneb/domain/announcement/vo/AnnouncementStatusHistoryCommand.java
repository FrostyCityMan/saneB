package com.saneb.domain.announcement.vo;

import java.util.UUID;

public record AnnouncementStatusHistoryCommand(
        UUID announcementId,
        String beforeStatusCode,
        String afterStatusCode,
        String reason,
        UUID changedBy
) {
}
