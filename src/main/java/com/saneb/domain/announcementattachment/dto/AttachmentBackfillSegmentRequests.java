package com.saneb.domain.announcementattachment.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;

public final class AttachmentBackfillSegmentRequests {
    private AttachmentBackfillSegmentRequests() { }
    public record Reservation(@NotNull(message="조회한 전체 목록 버전이 필요합니다.") @Min(value=0,message="목록 버전은 0 이상입니다.") Long expectedRunVersion,
            @NotBlank(message="조회한 분할 지문이 필요합니다.") @Pattern(regexp="[0-9a-f]{64}",message="분할 지문은 소문자 16진수64자리입니다.") String expectedSegmentHash,
            @NotNull(message="잔여 대상 수를 확인하세요.") @Min(value=1,message="잔여 대상은 1건 이상이어야 합니다.") @Max(value=1000,message="한 분할은 최대1000건입니다.") Integer expectedRemainingItemCount,
            @NotNull(message="삭제된 대상 수를 확인하세요.") @Min(value=0,message="삭제 건수는 0 이상입니다.") @Max(value=999,message="전체 삭제 분할은 예약할 수 없습니다.") Integer expectedDeletedItemCount,
            @NotBlank(message="분할 예약 사유를 입력하세요.") @Size(max=1000,message="사유는1000자 이하여야 합니다.") String reason) {
        @JsonAnySetter public void rejectUnknown(String key,Object value) {throw new IllegalArgumentException("정의하지 않은 분할 예약 입력입니다.");}
        @Override public String toString() {return "AttachmentBackfillSegmentReservation[reason=REDACTED]";}
    }
}
