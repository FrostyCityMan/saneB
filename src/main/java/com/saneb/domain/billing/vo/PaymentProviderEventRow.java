package com.saneb.domain.billing.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentProviderEventRow(
        UUID eventId,
        String providerCode,
        String providerEventId,
        String eventTypeCode,
        UUID paymentId,
        UUID refundId,
        String resultCode,
        OffsetDateTime receivedAt
) {
}
