/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: VerificationBusinessValuesSaveRequest.java
 * 작성자: 김도훈
 *
 */

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
