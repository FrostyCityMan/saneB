/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementInputRequirementsSaveRequest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.dynamicinput.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record AnnouncementInputRequirementsSaveRequest(
        @Valid List<RequirementRequest> requirements
) {

    public record RequirementRequest(
            @NotBlank String fieldKey,
            @NotBlank String fieldLabel,
            @NotBlank String fieldTypeCode,
            @NotBlank String scopeCode,
            @NotNull Boolean required,
            @NotNull Boolean sensitive,
            @Min(0) int sortOrder,
            UUID standardFieldId,
            String helpText,
            @Valid List<OptionRequest> options
    ) {
    }

    public record OptionRequest(
            @NotBlank String optionCode,
            @NotBlank String optionLabel,
            @Min(0) int sortOrder
    ) {
    }
}
