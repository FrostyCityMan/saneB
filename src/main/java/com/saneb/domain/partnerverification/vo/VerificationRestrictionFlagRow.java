package com.saneb.domain.partnerverification.vo;

public record VerificationRestrictionFlagRow(
        String restrictionCode,
        Boolean checked,
        String note
) {
}
