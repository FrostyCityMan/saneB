package com.saneb.domain.announcementattachment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;

public final class AttachmentCollectionRequests {
    private AttachmentCollectionRequests() { }
    /** 최초 수집에는 첨부 판정 ID가 없다. null도 조회 당시 값으로 비교한다. */
    public record Version(
            @NotNull(message="현재 기본 판정 ID가 필요합니다.") UUID expectedBaseDecisionId,
            UUID expectedAttachmentDecisionId,
            @NotNull(message="수집 조건 조회의 원문 버전이 필요합니다.") @Min(value=0,message="원문 버전은 0 이상이어야 합니다.") Integer expectedSourceVersion,
            @NotNull(message="수집 조건 조회의 첨부 버전이 필요합니다.") @Min(value=0,message="첨부 버전은 0 이상이어야 합니다.") @Max(value=2147483646,message="첨부 버전이 상한에 도달했습니다.") Integer expectedAttachmentVersion) { }
    public record Request(
            @NotNull(message="수집 조건 조회의 현재 버전이 필요합니다.") @Valid Version version,
            @NotNull(message="조회한 게시 정책 ID가 필요합니다.") UUID expectedPolicyId,
            @NotNull(message="조회한 정책 SHA-256이 필요합니다.") @Pattern(regexp="[0-9a-f]{64}",message="조회한 정책 SHA-256이 필요합니다.") String expectedPolicyHash,
            @NotNull(message="조회한 실행 SHA-256이 필요합니다.") @Pattern(regexp="[0-9a-f]{64}",message="조회한 실행 SHA-256이 필요합니다.") String expectedExecutionHash,
            @NotNull(message="이번 작업의 다운로드 상한을 바이트 단위로 입력하세요.") @Min(value=1,message="다운로드 상한은 1바이트 이상이어야 합니다.")
            @Max(value=83886080,message="다운로드 상한은 공고별 80 MiB 이하여야 합니다.") Long maximumDownloadBytes,
            @NotBlank(message="전체 첨부 수집 사유를 입력하세요.") @Size(max=1000,message="수집 사유는 1000자 이하여야 합니다.") String reason) {
        @Override public String toString() { return "AttachmentCollectionRequest[reason=REDACTED]"; }
    }
}
