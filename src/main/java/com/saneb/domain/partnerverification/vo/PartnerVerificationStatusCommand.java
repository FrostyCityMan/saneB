package com.saneb.domain.partnerverification.vo;

import java.util.UUID;

public record PartnerVerificationStatusCommand(
        UUID verificationId,
        String statusCode,
        UUID reviewedBy,
        String reviewNote
) {
}
