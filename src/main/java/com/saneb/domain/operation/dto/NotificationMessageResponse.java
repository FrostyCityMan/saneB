package com.saneb.domain.operation.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationMessageResponse(
        UUID notificationId,
        UUID recipientUserId,
        String channelCode,
        String title,
        String body,
        String statusCode,
        String resourceType,
        UUID resourceId,
        OffsetDateTime readAt,
        OffsetDateTime sentAt,
        OffsetDateTime createdAt
) {
}
