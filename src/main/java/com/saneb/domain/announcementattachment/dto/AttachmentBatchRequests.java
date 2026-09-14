package com.saneb.domain.announcementattachment.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

public final class AttachmentBatchRequests {
    private AttachmentBatchRequests() { }
    public record Scope(@NotNull(message="게시 정책 ID가 필요합니다.") UUID policyId,
            @NotEmpty(message="출처를 하나 이상 지정하세요.") @Size(max=3,message="출처는 최대 3개입니다.") List<String> providerCodes,
            @NotNull(message="수집 시작 시각이 필요합니다.") OffsetDateTime collectedFrom,
            @NotNull(message="수집 종료 시각이 필요합니다.") OffsetDateTime collectedBefore,
            LocalDate deadlineFrom,LocalDate deadlineThrough,
            @NotNull(message="최대 대상 건수를 지정하세요.") @Min(value=1,message="최대 건수는 1 이상입니다.") @Max(value=1000,message="한 배치는 최대 1000건입니다.") Integer maximumCount) {
        @JsonAnySetter public void rejectUnknown(String key,Object value) {throw new IllegalArgumentException("정의하지 않은 배치 범위 입력입니다.");}
    }
    public record Reservation(@NotNull(message="조회한 범위가 필요합니다.") @Valid Scope scope,
            @NotBlank(message="조회한 범위 지문이 필요합니다.") @Pattern(regexp="[0-9a-f]{64}",message="범위 지문은 64자리 SHA-256입니다.") String expectedScopeHash,
            @NotBlank(message="배치 예약 사유를 입력하세요.") @Size(max=1000,message="사유는 1000자 이하여야 합니다.") String reason) {
        @JsonAnySetter public void rejectUnknown(String key,Object value) {throw new IllegalArgumentException("정의하지 않은 배치 예약 입력입니다.");}
        @Override public String toString() {return "AttachmentBatchReservation[reason=REDACTED]";}
    }
    public record Cancellation(@NotNull(message="조회한 배치 버전이 필요합니다.") @Min(value=0,message="배치 버전은 0 이상입니다.") Integer expectedVersion,
            @NotBlank(message="취소 사유를 입력하세요.") @Size(max=1000,message="사유는 1000자 이하여야 합니다.") String reason) {
        @JsonAnySetter public void rejectUnknown(String key,Object value) {throw new IllegalArgumentException("정의하지 않은 배치 취소 입력입니다.");}
        @Override public String toString() {return "AttachmentBatchCancellation[reason=REDACTED]";}
    }
    public record Collection(@NotNull(message="조회한 배치 버전이 필요합니다.") @Min(value=0,message="배치 버전은 0 이상입니다.") Integer expectedVersion,
            @NotBlank(message="고정 범위 지문이 필요합니다.") @Pattern(regexp="[0-9a-f]{64}",message="범위 지문은 소문자 16진수 64자리 SHA-256입니다.") String expectedScopeHash,
            @NotNull(message="고정 대상 수를 확인하세요.") @Min(value=1,message="고정 대상 수는 1 이상입니다.") @Max(value=1000,message="고정 대상 수는 1000 이하입니다.") Integer expectedItemCount,
            @NotNull(message="삭제된 대상 수를 확인하세요.") @Min(value=0,message="삭제된 대상 수는 0 이상입니다.") Integer expectedDeletedItemCount,
            @NotNull(message="고정 최대 다운로드 bytes를 확인하세요.") @Min(value=1,message="최대 다운로드 bytes는 1 이상입니다.") Long expectedMaximumDownloadBytes,
            @NotNull(message="고정 최대 HTTP 요청 수를 확인하세요.") @Min(value=1,message="최대 HTTP 요청 수는 1 이상입니다.") Long expectedMaximumHttpRequests,
            @NotBlank(message="수집 실행 사유를 입력하세요.") @Size(max=1000,message="수집 실행 사유는 1000자 이하여야 합니다.") String reason) {
        @JsonAnySetter public void rejectUnknown(String key,Object value) {throw new IllegalArgumentException("정의하지 않은 배치 수집 입력입니다.");}
        @Override public String toString() {return "AttachmentBatchCollection[reason=REDACTED]";}
    }
    public record Pause(@NotNull(message="조회한 배치 버전이 필요합니다.") @Min(value=0,message="배치 버전은 0 이상입니다.") Integer expectedVersion,
            @NotBlank(message="중지 사유를 입력하세요.") @Size(max=1000,message="중지 사유는 1000자 이하여야 합니다.") String reason) {
        @JsonAnySetter public void rejectUnknown(String key,Object value) {throw new IllegalArgumentException("정의하지 않은 배치 중지 입력입니다.");}
        @Override public String toString() {return "AttachmentBatchPause[reason=REDACTED]";}
    }
}
