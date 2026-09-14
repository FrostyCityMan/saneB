package com.saneb.domain.announcementattachment.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;
import java.util.*;

public final class AttachmentBatchPreviewRequests {
    private AttachmentBatchPreviewRequests() { }
    public record Preparation(@NotNull(message="현재 배치 버전이 필요합니다.") @Min(value=0,message="배치 버전은 0 이상입니다.") Integer expectedVersion,
            @NotBlank(message="고정 범위 지문이 필요합니다.") @Pattern(regexp="[0-9a-f]{64}",message="범위 지문은 64자리 SHA-256입니다.") String expectedScopeHash,
            @NotBlank(message="미리보기 생성 사유를 입력하세요.") @Size(max=1000,message="사유는 1000자 이하여야 합니다.") String reason) {
        @JsonAnySetter public void rejectUnknown(String key,Object value) {throw new IllegalArgumentException("정의하지 않은 미리보기 입력입니다.");}
        @Override public String toString() {return "AttachmentBatchPreviewPreparation[reason=REDACTED]";}
    }
    public record Selection(@NotNull(message="현재 배치 버전이 필요합니다.") @Min(value=0,message="배치 버전은 0 이상입니다.") Integer expectedVersion,
            @NotBlank(message="현재 미리보기 지문이 필요합니다.") @Pattern(regexp="[0-9a-f]{64}",message="미리보기 지문은 64자리 SHA-256입니다.") String expectedPreviewHash,
            @NotNull(message="선택 작업 ID 목록을 입력하세요. 선택 해제는 빈 목록입니다.") @Size(max=1000,message="선택 항목은 최대 1000개입니다.") List<@NotNull(message="선택 작업 ID는 비어 있을 수 없습니다.") UUID> selectedJobIds,
            @NotBlank(message="선택 변경 사유를 입력하세요.") @Size(max=1000,message="사유는 1000자 이하여야 합니다.") String reason) {
        @JsonAnySetter public void rejectUnknown(String key,Object value) {throw new IllegalArgumentException("정의하지 않은 미리보기 선택 입력입니다.");}
        @Override public String toString() {return "AttachmentBatchPreviewSelection[reason=REDACTED]";}
    }
}
