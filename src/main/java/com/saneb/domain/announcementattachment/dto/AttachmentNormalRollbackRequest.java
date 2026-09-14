package com.saneb.domain.announcementattachment.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;

public record AttachmentNormalRollbackRequest(
        @NotNull(message="현재 원문 버전을 입력하세요.") @Min(value=0,message="원문 버전은 0 이상이어야 합니다.") Integer expectedSourceVersion,
        @NotNull(message="현재 첨부 버전을 입력하세요.") @Min(value=0,message="첨부 버전은 0 이상이어야 합니다.") @Max(value=2147483646,message="첨부 버전 증가 한도를 초과했습니다.") Integer expectedAttachmentVersion,
        @NotBlank(message="원복 미리보기 지문을 입력하세요.") @Pattern(regexp="[0-9a-f]{64}",message="원복 지문은 64자리 소문자 16진수여야 합니다.") String expectedPreviewHash,
        @NotNull(message="기본 판정 경로 재개 여부를 확인하세요.") Boolean expectedBaseReopen,
        @NotNull(message="이전 검수 확인 복원 여부를 확인하세요.") Boolean expectedConfirmationRestore,
        @NotNull(message="원복 영향 확인이 필요합니다.") @AssertTrue(message="이전 판정·검수 복구의 영향을 확인하세요.") Boolean acknowledgeBindingRestoration,
        @NotBlank(message="원복 사유를 입력하세요.") @Size(max=1000,message="원복 사유는 1000자 이하여야 합니다.") String reason) {
    @JsonAnySetter public void rejectUnknown(String name,Object ignored){throw new IllegalArgumentException("미리보기의 버전·지문·영향 확인 필드만 입력하세요.");}
    @Override public String toString(){return "AttachmentNormalRollbackRequest[reason=REDACTED]";}
}
