package com.saneb.domain.announcementattachment.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;

public record AttachmentBatchRollbackRequest(
        @NotNull(message="원복 미리보기의 현재 버전이 필요합니다.") @Min(value=0,message="원복 버전은 0 이상이어야 합니다.") @Max(value=2147483646,message="원복 버전의 증가 한도를 초과했습니다.") Integer expectedVersion,
        @NotBlank(message="원복 미리보기 지문이 필요합니다.") @Pattern(regexp="[0-9a-f]{64}",message="미리보기 지문은 64자리 소문자 16진수여야 합니다.") String expectedPreviewHash,
        @NotNull(message="최초 전체 범위 건수가 필요합니다.") @Min(value=1,message="전체 범위는 1건 이상이어야 합니다.") @Max(value=1000,message="전체 범위는 1000건 이하여야 합니다.") Integer expectedScopeCount,
        @NotNull(message="원복 대상 건수가 필요합니다.") @Min(value=1,message="원복 대상은 1건 이상이어야 합니다.") @Max(value=1000,message="원복 대상은 1000건 이하여야 합니다.") Integer expectedTargetCount,
        @NotNull(message="삭제 건수를 명시하세요.") @Min(value=0,message="삭제 건수는 0 이상이어야 합니다.") @Max(value=1000,message="삭제 건수는 1000 이하여야 합니다.") Integer expectedDeletedCount,
        @NotNull(message="기본 판정 경로 재개 건수를 명시하세요.") @Min(value=0,message="경로 재개 건수는 0 이상이어야 합니다.") @Max(value=1000,message="경로 재개 건수는 1000 이하여야 합니다.") Integer expectedBaseReopenCount,
        @NotNull(message="이전 검수 확인 복원 건수를 명시하세요.") @Min(value=0,message="확인 복원 건수는 0 이상이어야 합니다.") @Max(value=1000,message="확인 복원 건수는 1000 이하여야 합니다.") Integer expectedConfirmationRestoreCount,
        @NotNull(message="취소할 적용 대기 건수를 명시하세요.") @Min(value=0,message="취소 건수는 0 이상이어야 합니다.") @Max(value=1000,message="취소 건수는 1000 이하여야 합니다.") Integer expectedCancelPendingCount,
        @NotNull(message="원복과 적용 대기 취소의 영향 확인이 필요합니다.") @AssertTrue(message="이전 판정·검수 복구와 남은 적용 대기 취소의 영향 범위를 확인하세요.") Boolean acknowledgeBindingRestoration,
        @NotBlank(message="원복 승인 사유를 입력하세요.") @Size(max=1000,message="원복 승인 사유는 1000자 이하여야 합니다.") String reason) {
    @JsonAnySetter public void rejectUnknown(String name,Object ignored){throw new IllegalArgumentException("원복 미리보기에 표시된 승인 필드만 입력하세요.");}
    @Override public String toString(){return "AttachmentBatchRollbackRequest[reason=REDACTED]";}
}
