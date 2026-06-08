package com.saneb.domain.consent.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserConsentResponse(
        UUID userConsentId,
        UUID consentVersionId,
        String consentCode,
        String consentName,
        int versionNo,
        Boolean consented,
        OffsetDateTime consentedAt
) {
}
