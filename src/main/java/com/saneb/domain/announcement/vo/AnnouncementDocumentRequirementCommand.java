package com.saneb.domain.announcement.vo;

import java.util.UUID;

public record AnnouncementDocumentRequirementCommand(
        UUID announcementId,
        String documentTypeCode,
        Boolean required,
        Integer sortOrder,
        UUID actorUserId
) {
}
