package com.saneb.domain.announcementattachment.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class AttachmentPolicyCheckRows {
    private AttachmentPolicyCheckRows() { }
    public record Row(UUID checkId,UUID policyId,Integer policyVersion,String policySnapshotHash,UUID ruleReleaseId,
            Integer ruleVersion,String ruleSnapshotHash,String ruleContentHash,String checkTypeCode,String suiteVersion,
            String engineVersion,String resultHash,Integer caseCount,String caseIdsJson,UUID requestedBy,UUID idempotencyKey,
            String requestHash,Boolean isCurrent,OffsetDateTime createdAt) { }
    public record Insert(UUID checkId,UUID policyId,Integer policyVersion,String policySnapshotHash,UUID ruleReleaseId,
            Integer ruleVersion,String ruleSnapshotHash,String ruleContentHash,String suiteVersion,String engineVersion,
            String resultHash,Integer caseCount,String caseIdsJson,UUID requestedBy,UUID idempotencyKey,String requestHash) { }
    public record Search(UUID policyId,int size,int offset) { }
}
