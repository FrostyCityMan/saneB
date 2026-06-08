package com.saneb.domain.auditlog.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogDetailsRow(
        UUID auditLogId,
        UUID actorUserId,
        String actorLoginId,
        String actorName,
        String actionCode,
        String resourceType,
        UUID resourceId,
        String resultCode,
        String ipAddress,
        String userAgent,
        String metadataJson,
        OffsetDateTime createdAt
) {
}
