package com.saneb.domain.announcementattachment.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/** 연결 공고 근거 전용 계약. 일반 배치의 includeLinked 우회 입력으로 사용하지 않는다. */
public final class AttachmentLinkedBatchRequests {
    private AttachmentLinkedBatchRequests() { }

    public record Scope(
            @NotNull(message="게시된 첨부 정책 ID가 필요합니다.") UUID policyId,
            @NotEmpty(message="근거를 다시 수집할 원문 ID를 지정하세요.")
            @Size(max=1000,message="한 번에 지정할 원문은 최대 1000개입니다.") List<@NotNull(message="원문 ID에 빈 값을 넣을 수 없습니다.") UUID> sourceIds,
            @NotNull(message="공고별 최대 다운로드 bytes를 지정하세요.")
            @Min(value=1,message="공고별 다운로드 상한은 1바이트 이상이어야 합니다.")
            @Max(value=83886080,message="공고별 다운로드 상한은 80 MiB 이하여야 합니다.") Long maximumSourceBytes) {
        public Scope {
            if(sourceIds!=null) sourceIds=Collections.unmodifiableList(new ArrayList<>(sourceIds));
        }
        @JsonIgnore @AssertTrue(message="원문 ID는 중복 없이 지정하세요.")
        public boolean isSourceIdsUnique() {return sourceIds==null || new HashSet<>(sourceIds).size()==sourceIds.size();}
        @JsonAnySetter public void rejectUnknown(String key,Object value) {throw new IllegalArgumentException("연결 근거 범위에는 정책 ID·원문 ID 목록·공고별 다운로드 상한만 지정할 수 있습니다.");}
        @Override public String toString() {return "LinkedEvidenceScope[count="+(sourceIds==null?0:sourceIds.size())+"]";}
    }

    public record Reservation(
            @NotNull(message="조회한 연결 근거 범위가 필요합니다.") @Valid Scope scope,
            @NotBlank(message="조회한 범위 지문이 필요합니다.")
            @Pattern(regexp="[0-9a-f]{64}",message="범위 지문은 소문자 16진수 64자리 SHA-256이어야 합니다.") String expectedScopeHash,
            @NotNull(message="운영 공고를 변경하지 않는 근거 전용 수집임을 확인하세요.")
            @AssertTrue(message="운영 공고를 변경하지 않는 근거 전용 수집임을 확인해야 예약할 수 있습니다.") Boolean evidenceOnlyAcknowledged,
            @NotBlank(message="연결 근거 재수집 사유를 입력하세요.")
            @Size(max=1000,message="재수집 사유는 1000자 이하여야 합니다.") String reason) {
        @JsonAnySetter public void rejectUnknown(String key,Object value) {throw new IllegalArgumentException("정의하지 않은 연결 근거 예약 입력입니다.");}
        @Override public String toString() {return "LinkedEvidenceReservation[reason=REDACTED]";}
    }
}
