package com.saneb.domain.billing.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentProviderEventResponse(
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
