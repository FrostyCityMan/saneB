package com.saneb.domain.consultation.vo;

import java.util.UUID;

public record PartnerVerificationOwnerRow(
        UUID verificationId,
        UUID memberUserId
) {
}
