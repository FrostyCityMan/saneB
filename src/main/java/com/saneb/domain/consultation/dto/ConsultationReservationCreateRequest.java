/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ConsultationReservationCreateRequest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.consultation.dto;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ConsultationReservationCreateRequest(
        UUID slotId,

        UUID memberUserId,
        @Size(max = 32, message = "회원 코드는 32자 이내로 입력하세요.")
        String memberUserCode,
        UUID partnerUserId,
        @Size(max = 32, message = "담당자 코드는 32자 이내로 입력하세요.")
        String partnerUserCode,
        UUID progressId,
        UUID verificationId,

        @Size(max = 1000, message = "상담 요청 내용은 1000자 이내로 입력하세요.")
        String requestNote
) {
}
