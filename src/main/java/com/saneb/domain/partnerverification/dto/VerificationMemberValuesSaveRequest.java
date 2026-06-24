/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: VerificationMemberValuesSaveRequest.java
 * 작성자: 김도훈
 *
 */

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
