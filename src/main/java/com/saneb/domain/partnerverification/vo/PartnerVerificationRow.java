package com.saneb.domain.partnerverification.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PartnerVerificationRow(
        UUID verificationId,
        UUID memberUserId,
        UUID partnerUserId,
        UUID businessProfileId,
        String statusCode,
        Boolean current,
        Boolean matchingBlocked,
        OffsetDateTime submittedAt,
        OffsetDateTime verifiedAt,
        UUID reviewedBy,
        String reviewNote,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
