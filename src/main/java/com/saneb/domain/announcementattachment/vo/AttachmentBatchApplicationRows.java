package com.saneb.domain.announcementattachment.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class AttachmentBatchApplicationRows {
    private AttachmentBatchApplicationRows() { }
    public record Action(UUID id,UUID batchId,UUID previewId,String actionCode,Integer expectedVersion,
            Integer itemCount,Integer selectedCount,Integer deletedCount,UUID actorId,UUID key,String requestHash,String reasonHash,OffsetDateTime createdAt) { }
    public record Work(UUID batchId,UUID jobId,UUID sourceId,UUID policyId,UUID evaluationId,UUID previewId,String itemInputHash,UUID actorId) { }
    public record Counts(Integer remaining,Integer pending,Integer applied,Integer conflicts,Integer failed,Integer selectedRemaining) { }
    public record Item(UUID jobId,UUID sourceId,String collectionStatusCode,Boolean selected,String applicationStatusCode,String applicationErrorCode,
            Integer applicationAttemptCount,OffsetDateTime nextAttemptAt,UUID appliedEvaluationId,Integer appliedSourceVersion,Integer appliedAttachmentVersion,String rollbackStatusCode) { }
}
