/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSaveRequest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AnnouncementSaveRequest(
        @NotBlank(message = "targetTypeCode is required")
        @Size(max = 30, message = "targetTypeCode must be 30 characters or less")
        String targetTypeCode,

        @NotBlank(message = "title is required")
        @Size(max = 300, message = "title must be 300 characters or less")
        String title,

        @NotBlank(message = "agencyName is required")
        @Size(max = 200, message = "agencyName must be 200 characters or less")
        String agencyName,

        String summary,

        LocalDate applicationStartDate,

        LocalDate applicationEndDate,

        @NotBlank(message = "incomeJudgementCode is required")
        @Size(max = 50, message = "incomeJudgementCode must be 50 characters or less")
        String incomeJudgementCode,

        @DecimalMin(value = "0", message = "minAmount must be 0 or greater")
        BigDecimal minAmount,

        @DecimalMin(value = "0", message = "maxAmount must be 0 or greater")
        BigDecimal maxAmount,

        @Valid
        List<OptionRequest> options
) {

    public record OptionRequest(
            @NotBlank(message = "optionGroupCode is required")
            @Size(max = 80, message = "optionGroupCode must be 80 characters or less")
            String optionGroupCode,

            @NotBlank(message = "optionCode is required")
            @Size(max = 80, message = "optionCode must be 80 characters or less")
            String optionCode
    ) {
    }
}
