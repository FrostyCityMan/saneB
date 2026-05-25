package com.saneb.domain.partnerverification.vo;

import java.util.UUID;

public record VerificationFamilyValueCommand(
        UUID verificationId,
        String relationTypeCode,
        Integer birthYear,
        String address,
        String schoolAgeStatusCode,
        String enrollmentStatusCode,
        Boolean cohabiting,
        Boolean supported,
        Boolean hasIncome,
        UUID actorUserId
) {
}
