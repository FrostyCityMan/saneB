/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementStepsSaveRequest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record AnnouncementStepsSaveRequest(
        @Valid
        List<StepRequest> steps
) {

    public record StepRequest(
            @NotNull(message = "stepOrder is required")
            @Min(value = 1, message = "stepOrder must be greater than 0")
            Integer stepOrder,

            @NotBlank(message = "stepName is required")
            @Size(max = 100, message = "stepName must be 100 characters or less")
            String stepName,

            String guideMessage,

            String actionGuide,

            @NotBlank(message = "completionConditionCode is required")
            @Size(max = 80, message = "completionConditionCode must be 80 characters or less")
            String completionConditionCode,

            @Size(max = 80, message = "nextConditionCode must be 80 characters or less")
            String nextConditionCode,

            Boolean active,

            @Valid
            List<ButtonRequest> buttons,

            @Valid
            List<StepDocumentRequest> documents
    ) {
    }

    public record ButtonRequest(
            @NotBlank(message = "buttonCode is required")
            @Size(max = 80, message = "buttonCode must be 80 characters or less")
            String buttonCode,

            @NotBlank(message = "buttonLabel is required")
            @Size(max = 100, message = "buttonLabel must be 100 characters or less")
            String buttonLabel,

            @NotBlank(message = "buttonActionCode is required")
            @Size(max = 80, message = "buttonActionCode must be 80 characters or less")
            String buttonActionCode,

            UUID nextStepId,

            @NotNull(message = "sortOrder is required")
            Integer sortOrder
    ) {
    }

    public record StepDocumentRequest(
            @NotBlank(message = "documentTypeCode is required")
            @Size(max = 80, message = "documentTypeCode must be 80 characters or less")
            String documentTypeCode,

            @NotNull(message = "required is required")
            Boolean required,

            @NotNull(message = "sortOrder is required")
            Integer sortOrder
    ) {
    }
}
