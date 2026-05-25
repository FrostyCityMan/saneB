package com.saneb.domain.partnerverification.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record VerificationBusinessValuesCommand(
        UUID verificationId,
        BigDecimal annualRevenue,
        Integer employeeCount,
        Integer regularEmployeeCount,
        String taxStatusCode,
        Integer niceCreditScore,
        Integer kcbCreditScore,
        Boolean hasExistingLoan,
        Boolean hasPolicyFundUsage,
        Boolean hasGuaranteeUsage,
        LocalDate financialCheckedOn,
        UUID actorUserId
) {
}
