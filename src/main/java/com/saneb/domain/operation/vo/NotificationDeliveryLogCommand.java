package com.saneb.domain.operation.vo;

import java.util.UUID;

public record NotificationDeliveryLogCommand(
        UUID logId,
        UUID messageId,
        String channelCode,
        String providerCode,
        String deliveryStatusCode,
        int attemptNo,
        String providerMessageKey,
        String failureCode,
        String failureMessage,
        String metadataJson
) {
}
