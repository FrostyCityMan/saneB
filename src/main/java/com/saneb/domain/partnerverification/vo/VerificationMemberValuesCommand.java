package com.saneb.domain.partnerverification.vo;

import java.util.UUID;

public record VerificationMemberValuesCommand(
        UUID verificationId,
        Integer birthYear,
        String address,
        String regionCode,
        Boolean householder,
        Boolean householdMember,
        String healthInsuranceBasisCode,
        Boolean hasIncome,
        UUID actorUserId
) {
}
