package com.saneb.domain.partnerverification.vo;

public record VerificationFamilyValueRow(
        String relationTypeCode,
        Integer birthYear,
        String address,
        String schoolAgeStatusCode,
        String enrollmentStatusCode,
        Boolean cohabiting,
        Boolean supported,
        Boolean hasIncome
) {
}
