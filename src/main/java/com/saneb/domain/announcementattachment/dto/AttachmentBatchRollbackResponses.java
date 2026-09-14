package com.saneb.domain.announcementattachment.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class AttachmentBatchRollbackResponses {
    private AttachmentBatchRollbackResponses() { }
    public record Preview(UUID batchId,Integer version,String statusCode,String previewHash,int scopeCount,int remainingCount,int deletedCount,
            int targetCount,int eligibleCount,int conflictCount,int baseReopenCount,int confirmationRestoreCount,int staleConfirmationCount,int cancelPendingCount,int currentHttpRequests) { }
    public record Item(UUID jobId,UUID sourceId,String providerCode,String applicationStatusCode,String rollbackStatusCode,String readinessCode,
            boolean target,boolean eligible,boolean baseReopens,boolean confirmationRestores,boolean staleConfirmationRemains,
            String errorCode,int attemptCount,OffsetDateTime nextAttemptAt) { }
    public record Receipt(UUID actionId,UUID batchId,int acceptedFromVersion,String statusCode,int currentVersion,
            int scopeCount,int approvedTargetCount,int approvedEligibleCount,int approvedBaseReopenCount,int approvedConfirmationRestoreCount,int cancelledPendingCount,
            int remainingTargetCount,int deletedCount,int pendingCount,int rolledBackCount,int conflictCount,int failedCount,int currentHttpRequests,OffsetDateTime acceptedAt) { }
}
