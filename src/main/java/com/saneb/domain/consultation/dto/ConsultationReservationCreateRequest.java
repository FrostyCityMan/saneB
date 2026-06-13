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
