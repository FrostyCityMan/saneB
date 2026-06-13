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
