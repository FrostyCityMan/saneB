package com.saneb.domain.billing.vo;

import java.util.UUID;

public record PaymentProviderEventInsertCommand(
        UUID eventId,
        String providerCode,
        String providerEventId,
        String eventTypeCode,
        UUID paymentId,
        UUID refundId,
        String resultCode,
        String metadataJson
) {
}
