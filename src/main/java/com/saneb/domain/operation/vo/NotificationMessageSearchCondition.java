package com.saneb.domain.operation.vo;

import java.util.UUID;

public record NotificationMessageSearchCondition(
        UUID recipientUserId,
        Boolean unreadOnly,
        int page,
        int size,
        int offset
) {
}
