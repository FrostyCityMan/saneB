package com.saneb.domain.announcement.vo;

import java.util.UUID;

public record AnnouncementApprovalDecisionCommand(
        UUID announcementId,
        String approvalStatusCode,
        String decisionNote,
        UUID actorUserId
) {
}
