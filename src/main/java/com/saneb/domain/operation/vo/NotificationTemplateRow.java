package com.saneb.domain.operation.vo;

import java.util.UUID;

public record NotificationTemplateRow(
        UUID templateId,
        String templateCode,
        String channelCode,
        String titleTemplate,
        String bodyTemplate,
        boolean active
) {
}
