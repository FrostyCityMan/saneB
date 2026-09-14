package com.saneb.domain.announcementattachment.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record AttachmentBatchApplicationRequest(
        @NotNull(message="현재 배치 버전이 필요합니다.") @Min(value=0,message="배치 버전은 0 이상입니다.") @Max(value=2147483646,message="배치 버전 증가 한도를 초과했습니다.") Integer expectedVersion,
        @NotNull(message="승인할 미리보기 ID가 필요합니다.") UUID expectedPreviewId,
        @NotBlank(message="미리보기 지문이 필요합니다.") @Pattern(regexp="[0-9a-f]{64}",message="미리보기 지문은 64자리 SHA-256입니다.") String expectedPreviewHash,
        @NotNull(message="전체 건수가 필요합니다.") @Min(value=1,message="전체 건수는 1 이상입니다.") @Max(value=1000,message="배치 전체 건수는 최대 1000개입니다.") Integer expectedItemCount,
        @NotNull(message="선택 건수가 필요합니다.") @Min(value=1,message="적용 대상을 최소 1개 선택하세요.") @Max(value=1000,message="선택 건수는 최대 1000개입니다.") Integer expectedSelectedCount,
        @NotNull(message="현재 삭제 건수가 필요합니다.") @Min(value=0,message="삭제 건수는 0 이상입니다.") @Max(value=1000,message="삭제 건수는 최대 1000개입니다.") Integer expectedDeletedCount,
        @NotNull(message="재검수 필요 확인이 필요합니다.") @AssertTrue(message="적용 시 이전 첨부 확인이 만료되고 다시 검수해야 함을 확인하세요.") Boolean acknowledgeReviewReset,
        @NotBlank(message="실행 사유를 입력하세요.") @Size(max=1000,message="실행 사유는 1000자 이하여야 합니다.") String reason) {
    @JsonAnySetter public void rejectUnknown(String key,Object value) {throw new IllegalArgumentException("정의하지 않은 배치 적용 입력입니다.");}
    @Override public String toString() {return "AttachmentBatchApplicationRequest[reason=REDACTED]";}
}
