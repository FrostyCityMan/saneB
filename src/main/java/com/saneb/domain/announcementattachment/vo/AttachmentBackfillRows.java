package com.saneb.domain.announcementattachment.vo;

import com.saneb.domain.announcementattachment.dto.AttachmentBackfillRequests;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class AttachmentBackfillRows {
    private AttachmentBackfillRows() { }
    public record Digest(Long candidateCount,String candidateHash) { }
    public record Run(UUID runId,UUID policyId,String scopeJson,String policySnapshotJson,String scopeHash,String candidateHash,
            Long candidateCount,Integer segmentSize,Long segmentCount,Long deletedItemCount,Long rowVersion,
            UUID requestedBy,UUID idempotencyKey,String requestHash,OffsetDateTime createdAt) { }
    public record Insert(UUID runId,UUID policyId,String scopeJson,String policySnapshotJson,String scopeHash,String candidateHash,
            Long candidateCount,Integer segmentSize,Long segmentCount,UUID requestedBy,UUID idempotencyKey,String requestHash,String reasonHash) { }
    public record Materialize(UUID runId,AttachmentBackfillRequests.Scope scope) { }
    public record Segment(UUID runId,Long segmentNo,Integer itemCount,Integer deletedItemCount,Long remainingItemCount) { }
    public record Item(Long ordinal,UUID sourceId,UUID contentVersionId,UUID baseEvaluationId,UUID ruleReleaseId,
            String providerCode,String inputHash,Boolean currentInputMatches) { }
    public record Totals(Long remainingItemCount,Long segmentCount,Long initialSegmentItemCount,Long deletedSegmentItemCount) { }
    public record Search(UUID runId,Long segmentNo,int size,long offset) { }
}
