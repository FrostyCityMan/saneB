package com.saneb.domain.consent.vo;

import java.util.UUID;

public record UserConsentInsertCommand(
        UUID userId,
        UUID consentVersionId,
        String consentCode,
        Boolean consented,
        String ipAddress,
        String userAgent
) {
}
