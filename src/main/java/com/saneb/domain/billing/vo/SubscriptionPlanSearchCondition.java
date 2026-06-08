package com.saneb.domain.billing.vo;

public record SubscriptionPlanSearchCondition(
        Boolean active,
        int page,
        int size,
        int offset
) {
}
