package com.saneb.domain.operation.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationMessageRow(
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
