package com.saneb.domain.partnerverification.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record VerificationMemberValuesSaveRequest(
        @Min(value = 1900, message = "birthYear must be 1900 or later")
        @Max(value = 2200, message = "birthYear must be 2200 or earlier")
        Integer birthYear,

        @Size(max = 500, message = "address must be 500 characters or less")
        String address,

        @Size(max = 30, message = "regionCode must be 30 characters or less")
        String regionCode,

        Boolean householder,
        Boolean householdMember,

        @Size(max = 50, message = "healthInsuranceBasisCode must be 50 characters or less")
        String healthInsuranceBasisCode,

        Boolean hasIncome
) {
}
