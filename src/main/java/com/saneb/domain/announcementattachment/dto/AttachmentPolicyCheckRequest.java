package com.saneb.domain.announcementattachment.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;

public record AttachmentPolicyCheckRequest(
        @NotNull(message="검증할 정책의 조회 버전이 필요합니다.") @Min(value=0,message="조회 버전은 0 이상이어야 합니다.") Integer expectedVersion,
        @NotBlank(message="검증 사유를 입력하세요.") @Size(max=1000,message="검증 사유는 1000자 이하여야 합니다.") String reason) {
    @JsonAnySetter public void rejectUnknown(String key,Object value) { throw new IllegalArgumentException("정의하지 않은 검증 입력입니다."); }
    @Override public String toString() { return "AttachmentPolicyCheckRequest[reason=REDACTED]"; }
}
