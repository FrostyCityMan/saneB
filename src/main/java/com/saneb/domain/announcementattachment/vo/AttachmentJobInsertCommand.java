package com.saneb.domain.announcementattachment.vo;

import java.util.UUID;

public record AttachmentJobInsertCommand(
        UUID jobId, UUID sourceId, UUID contentVersionId, UUID baseEvaluationId,
        UUID ruleReleaseId, UUID policyId, Integer generation,
        Integer expectedSourceVersion, Integer expectedAttachmentVersion,
        UUID idempotencyKey, String requestHash, String executionSnapshotJson,
        Long downloadBudgetBytes, UUID collectionRunId, boolean applyToSource, boolean previousReviewRequired,
        UUID previousPolicyId, UUID previousEvaluationId, UUID previousConfirmationId,
        String operationCode, UUID referenceSetId, UUID requestedBy, UUID readySetId
) {
    public AttachmentJobInsertCommand(UUID jobId,UUID sourceId,UUID contentVersionId,UUID baseEvaluationId,
            UUID ruleReleaseId,UUID policyId,Integer generation,Integer expectedSourceVersion,Integer expectedAttachmentVersion,
            UUID idempotencyKey,String requestHash,String executionSnapshotJson,Long downloadBudgetBytes,UUID collectionRunId,
            boolean applyToSource,boolean previousReviewRequired,UUID previousPolicyId,UUID previousEvaluationId,UUID previousConfirmationId) {
        this(jobId,sourceId,contentVersionId,baseEvaluationId,ruleReleaseId,policyId,generation,expectedSourceVersion,expectedAttachmentVersion,
                idempotencyKey,requestHash,executionSnapshotJson,downloadBudgetBytes,collectionRunId,applyToSource,previousReviewRequired,
                previousPolicyId,previousEvaluationId,previousConfirmationId,"COLLECT",null,null,null);
    }
}
