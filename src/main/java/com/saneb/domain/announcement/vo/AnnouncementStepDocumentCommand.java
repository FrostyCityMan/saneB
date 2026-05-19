package com.saneb.domain.announcement.vo;

import java.util.UUID;

public record AnnouncementStepDocumentCommand(
        UUID stepId,
        String documentTypeCode,
        Boolean required,
        Integer sortOrder,
        UUID actorUserId
) {
}
