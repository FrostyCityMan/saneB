package com.saneb.domain.applicationprogress.vo;

import java.util.UUID;

public record AuditLogCommand(
        UUID actorUserId,
        String actionCode,
        String resourceType,
        UUID resourceId,
        String resultCode,
        String metadataJson
) {
}
