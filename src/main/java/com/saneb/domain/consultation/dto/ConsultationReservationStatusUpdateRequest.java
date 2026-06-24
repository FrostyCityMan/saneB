/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ConsultationReservationStatusUpdateRequest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.consultation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ConsultationReservationStatusUpdateRequest(
        @NotBlank(message = "상담 상태를 선택하세요.")
        @Size(max = 30, message = "상담 상태 값은 30자 이내로 입력하세요.")
        String statusCode,

        UUID partnerUserId,
        @Size(max = 32, message = "담당자 코드는 32자 이내로 입력하세요.")
        String partnerUserCode,
        UUID slotId,

        @Size(max = 1000, message = "상담 메모는 1000자 이내로 입력하세요.")
        String note
) {
}
