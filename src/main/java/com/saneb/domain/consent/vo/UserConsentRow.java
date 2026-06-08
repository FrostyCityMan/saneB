package com.saneb.domain.consent.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserConsentRow(
        UUID userConsentId,
        UUID consentVersionId,
        String consentCode,
        String consentName,
        int versionNo,
        Boolean consented,
        OffsetDateTime consentedAt
) {
}
