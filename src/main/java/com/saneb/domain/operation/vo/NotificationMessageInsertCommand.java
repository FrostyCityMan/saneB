package com.saneb.domain.operation.vo;

import java.util.UUID;

public record NotificationMessageInsertCommand(
        UUID notificationId,
        UUID recipientUserId,
        UUID templateId,
        String channelCode,
        String title,
        String body,
        String statusCode,
        String resourceType,
        UUID resourceId,
        UUID actorUserId
) {
}
