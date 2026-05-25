package com.saneb.domain.partnerverification.vo;

import java.util.UUID;

public record VerificationRestrictionFlagCommand(
        UUID verificationId,
        String restrictionCode,
        Boolean checked,
        String note,
        UUID actorUserId
) {
}
