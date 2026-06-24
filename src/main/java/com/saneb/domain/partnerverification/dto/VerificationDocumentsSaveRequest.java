/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: VerificationDocumentsSaveRequest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.partnerverification.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record VerificationDocumentsSaveRequest(
        @Valid
        List<DocumentRequest> documents
) {

    public record DocumentRequest(
            @NotBlank(message = "documentTypeCode is required")
            @Size(max = 80, message = "documentTypeCode must be 80 characters or less")
            String documentTypeCode,

            @NotBlank(message = "sourceTypeCode is required")
            @Size(max = 50, message = "sourceTypeCode must be 50 characters or less")
            String sourceTypeCode,

            @NotNull(message = "checked is required")
            Boolean checked,

            @Size(max = 2000, message = "note must be 2000 characters or less")
            String note
    ) {
    }
}
