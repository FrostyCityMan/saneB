package com.saneb.domain.announcement.vo;

import java.util.UUID;

public record AnnouncementManualStatusCommand(
        UUID announcementId,
        String manualStatusCode,
        UUID actorUserId
) {
}
