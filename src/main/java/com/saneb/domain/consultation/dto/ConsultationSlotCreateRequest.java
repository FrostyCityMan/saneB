/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ConsultationSlotCreateRequest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.consultation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsultationSlotCreateRequest(
        UUID partnerUserId,

        @NotNull(message = "startAt is required")
        OffsetDateTime startAt,

        @NotNull(message = "endAt is required")
        OffsetDateTime endAt,

        @Size(max = 1000, message = "note must be 1000 characters or less")
        String note
) {
}
