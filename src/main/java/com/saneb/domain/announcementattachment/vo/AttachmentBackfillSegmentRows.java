package com.saneb.domain.announcementattachment.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class AttachmentBackfillSegmentRows {
    private AttachmentBackfillSegmentRows() { }
    public record Link(UUID runId,Long segmentNo,UUID batchId,Integer deletedBeforeReservation,Long expectedRunVersion,String segmentHash,
            UUID requestedBy,UUID idempotencyKey,String requestHash,Integer originalItemCount,Integer reservedItemCount,String currentBatchStatusCode,OffsetDateTime createdAt) { }
    public record Insert(UUID runId,long segmentNo,UUID batchId,int deletedBeforeReservation,long expectedRunVersion,String segmentHash,
            UUID requestedBy,UUID idempotencyKey,String requestHash,String reasonHash) { }
    public record Totals(Long remainingItemCount,Long reservedSegmentCount,Long reservedRemainingItemCount,Long unreservedItemCount,Long unreservedInputChangedCount,Long missingJobCount) { }
    public record Outcome(String dimensionCode,String statusCode,Long count) { }
}
