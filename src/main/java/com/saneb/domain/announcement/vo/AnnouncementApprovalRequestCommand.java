package com.saneb.domain.announcement.vo;

import java.util.UUID;

public record AnnouncementApprovalRequestCommand(
        UUID announcementId,
        UUID actorUserId,
        String requestNote
) {
}
