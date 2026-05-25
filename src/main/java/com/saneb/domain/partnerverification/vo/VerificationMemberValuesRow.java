package com.saneb.domain.partnerverification.vo;

public record VerificationMemberValuesRow(
        Integer birthYear,
        String address,
        String regionCode,
        Boolean householder,
        Boolean householdMember,
        String healthInsuranceBasisCode,
        Boolean hasIncome
) {
}
