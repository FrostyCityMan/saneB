package com.saneb.domain.consent.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsentVersionRow(
        UUID consentVersionId,
        String consentCode,
        String consentName,
        int versionNo,
        Boolean required,
        OffsetDateTime effectiveFrom
) {
}
