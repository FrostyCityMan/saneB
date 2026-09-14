package com.saneb.domain.announcementattachment.vo;

import com.saneb.domain.announcementattachment.dto.AttachmentBatchRequests;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class AttachmentBatchRows {
    private AttachmentBatchRows() { }
    /** Controller 요청 DTO가 아니다. 고정 ledger 소속을 내부 예약 경로로만 전달한다. */
    public record FixedScope(UUID runId,long segmentNo,java.util.List<UUID> sourceIds) { }
    public record Bucket(String providerCode,String reasonCode,Long count) { }
    public record Candidate(UUID sourceId,String providerCode,UUID contentVersionId,UUID baseEvaluationId,UUID ruleReleaseId,
            Integer sourceVersion,Integer attachmentVersion,UUID currentEvaluationId,UUID currentPolicyId,UUID confirmationId,
            Boolean reviewRequired,UUID activeJobId) { }
    public record Row(UUID batchId,UUID policyId,String statusCode,String scopeHash,Integer maximumCount,Integer itemCount,
            Integer deletedItemCount,Integer rowVersion,String scopeJson,String policySnapshotJson,UUID requestedBy,
            UUID idempotencyKey,String requestHash,OffsetDateTime createdAt) { }
    public record Insert(UUID batchId,UUID policyId,String scopeHash,Integer maximumCount,Integer itemCount,String scopeJson,
            String policySnapshotJson,UUID requestedBy,UUID idempotencyKey,String requestHash,String reasonHash) { }
    public record ItemInsert(UUID batchId,AttachmentJobInsertCommand job,String providerCode) { }
    public record Item(UUID jobId,UUID sourceId,String providerCode,String statusCode,Integer sourceVersion,Integer attachmentVersion,
            String errorCode,String applicationStatusCode,String rollbackStatusCode) { }
    public record Search(UUID batchId,int size,int offset) { }
    public record ExecutionItem(UUID jobId,UUID sourceId,String providerCode,String statusCode,String requestHash,
            String executionSnapshotJson,Long downloadBudgetBytes,Boolean locatorUnchanged) { }
}
