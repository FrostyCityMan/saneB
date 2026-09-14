package com.saneb.domain.announcementattachment.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AttachmentJobRow(
        UUID jobId, UUID sourceId, UUID contentVersionId, UUID baseEvaluationId,
        UUID ruleReleaseId, UUID policyId, UUID batchId, UUID setId,
        Integer generation, Integer expectedSourceVersion, Integer expectedAttachmentVersion,
        String jobStatusCode, Integer attemptCount, OffsetDateTime nextAttemptAt,
        UUID leaseToken, OffsetDateTime leaseExpiresAt, String errorCode,
        UUID idempotencyKey, String requestHash, String executionSnapshotJson,
        Long downloadBudgetBytes, Long reservedDownloadBytes, Integer rowVersion,
        String operationCode, UUID referenceSetId, UUID requestedBy
) {
    public AttachmentJobRow(UUID jobId,UUID sourceId,UUID contentVersionId,UUID baseEvaluationId,UUID ruleReleaseId,UUID policyId,UUID batchId,UUID setId,
            Integer generation,Integer expectedSourceVersion,Integer expectedAttachmentVersion,String jobStatusCode,Integer attemptCount,OffsetDateTime nextAttemptAt,
            UUID leaseToken,OffsetDateTime leaseExpiresAt,String errorCode,UUID idempotencyKey,String requestHash,String executionSnapshotJson,
            Long downloadBudgetBytes,Long reservedDownloadBytes,Integer rowVersion) {
        this(jobId,sourceId,contentVersionId,baseEvaluationId,ruleReleaseId,policyId,batchId,setId,generation,expectedSourceVersion,expectedAttachmentVersion,
                jobStatusCode,attemptCount,nextAttemptAt,leaseToken,leaseExpiresAt,errorCode,idempotencyKey,requestHash,executionSnapshotJson,
                downloadBudgetBytes,reservedDownloadBytes,rowVersion,"COLLECT",null,null);
    }
}
