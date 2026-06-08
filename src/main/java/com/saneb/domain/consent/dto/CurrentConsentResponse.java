package com.saneb.domain.consent.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CurrentConsentResponse(
        UUID consentVersionId,
        String consentCode,
        String consentName,
        int versionNo,
        Boolean required,
        OffsetDateTime effectiveFrom
) {
}
