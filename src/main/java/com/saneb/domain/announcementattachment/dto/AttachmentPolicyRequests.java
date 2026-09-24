package com.saneb.domain.announcementattachment.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;
import java.util.UUID;

/** 실행 URL, parser, 임의 설정 JSON, 게시/검증 성공값은 입력으로 받지 않는다. */
public final class AttachmentPolicyRequests {
    private AttachmentPolicyRequests() { }
    public record Create(
            @NotNull(message="정책에 연결할 키워드 규칙을 선택하세요.") UUID ruleReleaseId,
            @NotNull(message="첨부 정책 모드를 선택하세요.") @Pattern(regexp="OFF|COLLECT_ONLY|ENFORCE",message="정책 모드는 OFF, COLLECT_ONLY, ENFORCE 중 하나여야 합니다.") String modeCode,
            @NotNull(message="공고별 다운로드 한도를 입력하세요.") @Min(value=1,message="다운로드 한도는 1바이트 이상이어야 합니다.") @Max(value=83886080,message="공고별 다운로드 한도는 80 MiB를 초과할 수 없습니다.") Long maximumSourceBytes,
            @NotBlank(message="정책 생성 사유를 입력하세요.") @Size(max=1000,message="정책 사유는 1000자 이하여야 합니다.") String reason,
            @Pattern(regexp="segment-role-1\\.0\\.[023]",message="구간 규칙은 segment-role-1.0.0, segment-role-1.0.2, segment-role-1.0.3 중 하나를 선택하세요.") String segmentRuleVersion) {
        public Create(UUID ruleReleaseId,String modeCode,Long maximumSourceBytes,String reason) {
            this(ruleReleaseId,modeCode,maximumSourceBytes,reason,null);
        }
        @JsonAnySetter public void rejectUnknown(String key,Object value) { throw new IllegalArgumentException("정의하지 않은 정책 입력입니다."); }
        @Override public String toString() { return "AttachmentPolicyCreate[reason=REDACTED]"; }
    }
    public record Update(
            @NotNull(message="조회한 정책 버전이 필요합니다.") @Min(value=0,message="조회 버전은 0 이상이어야 합니다.") @Max(value=2147483646,message="조회 버전이 최대값에 도달했습니다. 새 개정 초안을 만드세요.") Integer expectedVersion,
            @NotNull(message="정책에 연결할 키워드 규칙을 선택하세요.") UUID ruleReleaseId,
            @NotNull(message="첨부 정책 모드를 선택하세요.") @Pattern(regexp="OFF|COLLECT_ONLY|ENFORCE",message="정책 모드는 OFF, COLLECT_ONLY, ENFORCE 중 하나여야 합니다.") String modeCode,
            @NotNull(message="공고별 다운로드 한도를 입력하세요.") @Min(value=1,message="다운로드 한도는 1바이트 이상이어야 합니다.") @Max(value=83886080,message="공고별 다운로드 한도는 80 MiB를 초과할 수 없습니다.") Long maximumSourceBytes,
            @NotBlank(message="정책 수정 사유를 입력하세요.") @Size(max=1000,message="정책 사유는 1000자 이하여야 합니다.") String reason,
            @Pattern(regexp="segment-role-1\\.0\\.[023]",message="구간 규칙은 segment-role-1.0.0, segment-role-1.0.2, segment-role-1.0.3 중 하나를 선택하세요.") String segmentRuleVersion) {
        public Update(Integer expectedVersion,UUID ruleReleaseId,String modeCode,Long maximumSourceBytes,String reason) {
            this(expectedVersion,ruleReleaseId,modeCode,maximumSourceBytes,reason,null);
        }
        @JsonAnySetter public void rejectUnknown(String key,Object value) { throw new IllegalArgumentException("정의하지 않은 정책 입력입니다."); }
        @Override public String toString() { return "AttachmentPolicyUpdate[reason=REDACTED]"; }
    }
    public record Revision(@NotNull(message="복사할 정책의 조회 버전이 필요합니다.") @Min(value=0,message="조회 버전은 0 이상이어야 합니다.") Integer expectedVersion,
            @NotBlank(message="개정 사유를 입력하세요.") @Size(max=1000,message="정책 사유는 1000자 이하여야 합니다.") String reason) {
        @JsonAnySetter public void rejectUnknown(String key,Object value) { throw new IllegalArgumentException("정의하지 않은 정책 입력입니다."); }
        @Override public String toString() { return "AttachmentPolicyRevision[reason=REDACTED]"; }
    }
}
