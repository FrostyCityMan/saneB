package com.saneb.domain.announcement.vo;

import java.util.UUID;

public record AnnouncementApprovalStatusCommand(
        UUID announcementId,
        String approvalStatusCode,
        UUID actorUserId
) {
}
