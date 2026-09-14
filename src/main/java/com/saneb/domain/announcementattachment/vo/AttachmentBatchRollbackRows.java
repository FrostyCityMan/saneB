package com.saneb.domain.announcementattachment.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class AttachmentBatchRollbackRows {
    private AttachmentBatchRollbackRows() { }
    public record Candidate(UUID jobId,UUID sourceId,String providerCode,String applicationStatusCode,String rollbackStatusCode,
            UUID previousEvaluationId,UUID previousConfirmationId,Boolean previousReviewRequired,Boolean currentBindingMatches,Boolean previousBindingValid,
            Boolean previousConfirmationValid,String appliedInputHash,String errorCode,Integer attemptCount,OffsetDateTime nextAttemptAt) { }
    public record Action(UUID id,UUID batchId,Integer expectedVersion,String previewHash,Integer scopeCount,Integer targetCount,Integer eligibleCount,
            Integer deletedCount,Integer baseReopenCount,Integer confirmationRestoreCount,Integer cancelPendingCount,
            UUID actorId,UUID key,String requestHash,String reasonHash,OffsetDateTime createdAt) { }
    public record Target(UUID actionId,UUID batchId,UUID jobId,String inputHash,String readinessCode,Boolean eligible,Boolean baseReopens,Boolean confirmationRestores) { }
    public record Work(UUID actionId,UUID batchId,UUID jobId,UUID sourceId,UUID actorId,String inputHash,String readinessCode,Boolean eligible,Boolean confirmationRestores) { }
    public record Counts(Integer remaining,Integer pending,Integer rolledBack,Integer conflicts,Integer failed) { }
    public record Restoration(UUID id,UUID jobId,UUID actorId,UUID key,String requestHash) { }
}
