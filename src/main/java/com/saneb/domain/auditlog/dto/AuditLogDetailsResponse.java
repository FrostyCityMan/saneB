package com.saneb.domain.auditlog.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogDetailsResponse(
        UUID auditLogId,
        UUID actorUserId,
        String actorDisplayName,
        String actionCode,
        String actionLabel,
        String resourceType,
        String resourceLabel,
        UUID resourceId,
        String resultCode,
        String resultLabel,
        String ipAddress,
        String userAgent,
        String metadataJson,
        OffsetDateTime createdAt
) {
}
