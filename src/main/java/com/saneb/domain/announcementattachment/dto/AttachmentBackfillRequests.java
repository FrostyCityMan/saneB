package com.saneb.domain.announcementattachment.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

public final class AttachmentBackfillRequests {
    private AttachmentBackfillRequests() { }
    public record Scope(@NotNull(message="게시 정책 ID가 필요합니다.") UUID policyId,
            @NotEmpty(message="지원 출처를 지정하세요.") @Size(max=3,message="출처는 최대 3개입니다.") List<String> providerCodes,
            @NotNull(message="수집 시작 시각이 필요합니다.") OffsetDateTime collectedFrom,
            @NotNull(message="수집 종료 시각이 필요합니다.") OffsetDateTime collectedBefore,
            LocalDate deadlineFrom,LocalDate deadlineThrough,
            @NotNull(message="분할 크기가 필요합니다.") @Min(value=1,message="분할 크기는 1 이상입니다.")
            @Max(value=1000,message="한 분할은 최대 1000건입니다.") Integer segmentSize) {
        @JsonAnySetter public void rejectUnknown(String key,Object value) {throw new IllegalArgumentException("정의하지 않은 전체 범위 입력입니다.");}
    }
    public record Inventory(@NotNull(message="미리 본 전체 범위가 필요합니다.") @Valid Scope scope,
            @NotBlank(message="조회한 전체 범위 지문이 필요합니다.") @Pattern(regexp="[0-9a-f]{64}",message="범위 지문은 소문자 16진수 64자리입니다.") String expectedScopeHash,
            @NotNull(message="확인한 전체 후보 수가 필요합니다.") @Min(value=1,message="전체 후보 수는 1 이상입니다.") Long expectedCandidateCount,
            @NotBlank(message="전체 목록 고정 사유를 입력하세요.") @Size(max=1000,message="사유는 1000자 이하여야 합니다.") String reason) {
        @JsonAnySetter public void rejectUnknown(String key,Object value) {throw new IllegalArgumentException("정의하지 않은 전체 목록 고정 입력입니다.");}
        @Override public String toString() {return "AttachmentBackfillInventory[reason=REDACTED]";}
    }
}
