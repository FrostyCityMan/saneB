package com.saneb.domain.announcementattachment.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** 조회 시점의 영향 관측값이며 게시 승인이나 실행 영수증이 아니다. */
public record AttachmentPolicyPublicationImpact(AttachmentPolicyResponses.Summary policy,
        AttachmentPolicyResponses.Summary activePolicyForRule,Counts matchingRule,Counts allRules,
        Long maximumSourceBytes,boolean wouldStopNewExternalRequests,boolean wouldLiftGlobalOffStop,
        Qa latestQa,List<String> blockingReasonCodes,boolean requiresPublicationRevalidation,
        String observedImpactHash,OffsetDateTime observedAt,int currentHttpRequests) {
    public AttachmentPolicyPublicationImpact {blockingReasonCodes=List.copyOf(blockingReasonCodes);}
    public record Counts(Long boundSourceCount,Long reviewRequiredSourceCount,Long effectiveAttachmentSourceCount,
            Long linkedSourceCount,Long frozenCollectionJobCount,Long runningCollectionJobCount,
            Long applicationPendingJobCount,Long rollbackPendingJobCount,Long frozenCollectionPlanCount) { }
    public record Qa(UUID runId,String statusCode,Integer rowVersion,Integer policyVersion,Integer ruleVersion,
            boolean inputVersionsCurrent,String snapshotHash,List<Step> steps,OffsetDateTime completedAt) {
        public Qa {steps=List.copyOf(steps);}
    }
    public record Step(String stepCode,String statusCode,String evidenceHash) { }
}
