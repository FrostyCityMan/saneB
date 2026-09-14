package com.saneb.domain.announcementattachment.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 고정된 준비 범위이며 게시 승인·QA 성공·실행 영수증이 아니다. */
public final class AttachmentPolicyPublicationScope {
    private AttachmentPolicyPublicationScope() { }
    public record Prepare(@NotNull(message="조회한 초안 버전을 입력하세요.") @Min(value=0,message="조회 버전은 0 이상이어야 합니다.") Integer expectedVersion,
            @NotBlank(message="게시 준비 범위를 고정하는 사유를 입력하세요.") @Size(max=1000,message="사유는 1000자 이하여야 합니다.") String reason) {
        @JsonAnySetter public void rejectUnknown(String key,Object value) { throw new IllegalArgumentException("정의하지 않은 게시 준비 입력입니다."); }
        @Override public String toString() { return "PublicationScopePrepare[reason=REDACTED]"; }
    }
    public record Summary(UUID scopeId,UUID policyId,Integer policyVersion,UUID ruleReleaseId,Integer ruleVersion,String modeCode,
            UUID qaRunId,String qaSnapshotHash,Long itemCount,String scopeHash,OffsetDateTime createdAt,OffsetDateTime expiresAt) { }
    public record Details(Summary scope,boolean isExpired,boolean isScopeCurrent,boolean isApproval,
            boolean requiresPublicationRevalidation,int currentHttpRequests) { }
    public record Item(String entityTypeCode,UUID entityId,String stateHash) { }
}
