package com.saneb.domain.billing.vo;

import java.util.UUID;

public record UserSubscriptionSearchCondition(
        UUID userId,
        String statusCode,
        int page,
        int size,
        int offset
) {
}
