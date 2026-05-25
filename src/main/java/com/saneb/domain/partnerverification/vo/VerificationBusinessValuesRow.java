package com.saneb.domain.partnerverification.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VerificationBusinessValuesRow(
        BigDecimal annualRevenue,
        Integer employeeCount,
        Integer regularEmployeeCount,
        String taxStatusCode,
        Integer niceCreditScore,
        Integer kcbCreditScore,
        Boolean hasExistingLoan,
        Boolean hasPolicyFundUsage,
        Boolean hasGuaranteeUsage,
        LocalDate financialCheckedOn
) {
}
