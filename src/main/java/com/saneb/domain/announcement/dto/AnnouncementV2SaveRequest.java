/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementV2SaveRequest.java
 * 작성자: 김도훈
 */

package com.saneb.domain.announcement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AnnouncementV2SaveRequest(
        @NotBlank(message = "primaryTargetCategoryCode is required")
        @Size(max = 30, message = "primaryTargetCategoryCode must be 30 characters or less")
        String primaryTargetCategoryCode,

        @NotEmpty(message = "targetCategoryCodes must contain at least one value")
        @Size(max = 5, message = "targetCategoryCodes must contain 5 values or less")
        List<@NotBlank @Size(max = 30) String> targetCategoryCodes,

        @NotEmpty(message = "supportTypeCodes must contain at least one value")
        @Size(max = 7, message = "supportTypeCodes must contain 7 values or less")
        List<@NotBlank @Size(max = 40) String> supportTypeCodes,

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
        List<AnnouncementSaveRequest.OptionRequest> options
) {

    public AnnouncementSaveRequest toV1Request() {
        return new AnnouncementSaveRequest(
                primaryTargetCategoryCode,
                title,
                agencyName,
                summary,
                applicationStartDate,
                applicationEndDate,
                incomeJudgementCode,
                minAmount,
                maxAmount,
                options
        );
    }
}
