package com.saneb.domain.operation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record NotificationSendRequest(
        @NotNull UUID recipientUserId,
        String templateCode,
        @NotBlank String channelCode,
        @NotBlank String title,
        @NotBlank String body,
        String resourceType,
        UUID resourceId
) {
}
