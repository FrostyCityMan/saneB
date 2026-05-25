package com.saneb.domain.partnerverification.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VerificationDocumentRow(
        String documentTypeCode,
        String sourceTypeCode,
        Boolean checked,
        UUID checkedBy,
        OffsetDateTime checkedAt,
        String note
) {
}
