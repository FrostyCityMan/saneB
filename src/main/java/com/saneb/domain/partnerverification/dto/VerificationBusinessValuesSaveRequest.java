package com.saneb.domain.partnerverification.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record VerificationBusinessValuesSaveRequest(
        BigDecimal annualRevenue,

        @Min(value = 0, message = "employeeCount must be zero or positive")
        Integer employeeCount,

        @Min(value = 0, message = "regularEmployeeCount must be zero or positive")
        Integer regularEmployeeCount,

        @Size(max = 50, message = "taxStatusCode must be 50 characters or less")
        String taxStatusCode,

        @Min(value = 0, message = "niceCreditScore must be zero or positive")
        @Max(value = 1000, message = "niceCreditScore must be 1000 or less")
        Integer niceCreditScore,

        @Min(value = 0, message = "kcbCreditScore must be zero or positive")
        @Max(value = 1000, message = "kcbCreditScore must be 1000 or less")
        Integer kcbCreditScore,

        Boolean hasExistingLoan,
        Boolean hasPolicyFundUsage,
        Boolean hasGuaranteeUsage,
        LocalDate financialCheckedOn
) {
}
