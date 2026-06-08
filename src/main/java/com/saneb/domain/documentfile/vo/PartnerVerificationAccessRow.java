package com.saneb.domain.documentfile.vo;

import java.util.UUID;

public record PartnerVerificationAccessRow(
        UUID verificationId,
        UUID memberUserId,
        UUID partnerUserId
) {
}
