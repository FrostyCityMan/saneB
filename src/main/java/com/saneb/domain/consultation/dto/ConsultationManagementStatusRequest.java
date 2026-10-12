package com.saneb.domain.consultation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsultationManagementStatusRequest(
        @NotBlank(message = "처리할 상담 상태를 선택하세요.") @Size(max = 30) String statusCode,
        UUID partnerUserId,
        @Size(max = 1000, message = "상담 메모는 1000자 이내로 입력하세요.") String note,
        @NotNull(message = "최신 상담 내용을 다시 조회한 뒤 저장하세요.") OffsetDateTime expectedUpdatedAt) {
}
