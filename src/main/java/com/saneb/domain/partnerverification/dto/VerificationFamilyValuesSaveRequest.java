package com.saneb.domain.partnerverification.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record VerificationFamilyValuesSaveRequest(
        @Valid
        List<FamilyValueRequest> familyValues
) {

    public record FamilyValueRequest(
            @NotBlank(message = "relationTypeCode is required")
            @Size(max = 30, message = "relationTypeCode must be 30 characters or less")
            String relationTypeCode,

            @Min(value = 1900, message = "birthYear must be 1900 or later")
            @Max(value = 2200, message = "birthYear must be 2200 or earlier")
            Integer birthYear,

            @Size(max = 500, message = "address must be 500 characters or less")
            String address,

            @Size(max = 50, message = "schoolAgeStatusCode must be 50 characters or less")
            String schoolAgeStatusCode,

            @Size(max = 50, message = "enrollmentStatusCode must be 50 characters or less")
            String enrollmentStatusCode,

            Boolean cohabiting,

            Boolean supported,

            Boolean hasIncome
    ) {
    }
}
