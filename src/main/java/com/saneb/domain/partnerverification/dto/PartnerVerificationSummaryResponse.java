package com.saneb.domain.partnerverification.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PartnerVerificationSummaryResponse(
        UUID verificationId,
        UUID memberUserId,
        UUID partnerUserId,
        UUID businessProfileId,
        String verificationCode,
        String memberUserCode,
        String partnerUserCode,
        String statusCode,
        boolean current,
        boolean matchingBlocked,
        OffsetDateTime submittedAt,
        OffsetDateTime verifiedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
