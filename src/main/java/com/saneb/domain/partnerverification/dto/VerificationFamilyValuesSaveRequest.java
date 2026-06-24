/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: VerificationFamilyValuesSaveRequest.java
 * 작성자: 김도훈
 *
 */

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
