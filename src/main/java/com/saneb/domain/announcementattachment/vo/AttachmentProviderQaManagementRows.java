package com.saneb.domain.announcementattachment.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class AttachmentProviderQaManagementRows {
    private AttachmentProviderQaManagementRows() { }
    public record PlanInsert(UUID runId,String planHash,int segmentNo,int segmentCount,int catalogCaseCount,int executableCaseCount,
                             boolean expectationCoverageComplete,int maximumSecondsIncludingMargin) { }
    public record Search(UUID policyId,int size,int offset) { }
    public record CaseSearch(UUID runId,int size,int offset) { }
    public record Run(UUID runId,UUID policyId,Integer policyVersion,UUID ruleReleaseId,Integer ruleVersion,String snapshotHash,String catalogHash,
                      String executionCodeHash,String runtimeHash,Integer expectedCaseCount,Long maximumRequests,Long maximumBytes,Long requestReservations,Long reservedBytes,
                      String statusCode,Integer rowVersion,UUID requestedBy,UUID idempotencyKey,String requestHash,OffsetDateTime createdAt,OffsetDateTime expiresAt,
                      OffsetDateTime completedAt,String planHash,Integer segmentNo,Integer segmentCount,Integer catalogCaseCount,Integer executableCaseCount,
                      Boolean expectationCoverageComplete,Integer maximumSecondsIncludingMargin,Boolean inputVersionsCurrent) {
        @Override public String toString(){return "ProviderQaRun[id="+runId+",status="+statusCode+"]";}
    }
    public record Item(UUID caseId,Integer ordinal,String caseCode,String inputHash,String profileHash,Integer expectedFileCount,String statusCode,
                       Integer rowVersion,Integer requestReservations,Long reservedBytes,OffsetDateTime startedAt,OffsetDateTime completedAt,String errorCode,String evidenceHash) { }
}
