package com.saneb.domain.announcementattachment.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class AttachmentPolicyPublication {
    private AttachmentPolicyPublication() { }
    public record Request(@NotNull UUID scopeId,@NotBlank @Pattern(regexp="[0-9a-f]{64}") String scopeHash,
            @NotNull @Min(0) Integer expectedVersion,
            @NotNull @AssertTrue(message="고정 범위와 현재 모드의 신규 수집 영향을 확인해야 합니다.") Boolean acknowledgeNewCollectionBehavior,
            @NotNull @AssertTrue(message="기존 고정 작업은 원래 정책을 유지함을 확인해야 합니다.") Boolean acknowledgeExistingJobsUnchanged,
            @NotNull @AssertTrue(message="기존 데이터 일괄 적용은 별도 승인 배치임을 확인해야 합니다.") Boolean acknowledgeNoBackfill,
            @NotBlank @Size(max=1000) String reason){
        @JsonAnySetter public void rejectUnknown(String key,Object value){throw new IllegalArgumentException("정의하지 않은 정책 게시 입력입니다.");}
        @Override public String toString(){return "PolicyPublicationRequest[reason=REDACTED]";}
    }
    public record Receipt(UUID publicationId,UUID policyId,Integer publishedPolicyVersion,String policyHash,UUID previousPolicyId,
            Integer previousPolicyVersion,UUID scopeId,String scopeHash,UUID qaRunId,String modeCode,OffsetDateTime publishedAt){ }
    public record Result(Receipt publication,boolean existingDataApplied,boolean workerEnabledByRequest,int currentHttpRequests){ }
}
