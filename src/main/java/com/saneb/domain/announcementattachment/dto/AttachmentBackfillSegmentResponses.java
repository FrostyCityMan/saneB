package com.saneb.domain.announcementattachment.dto;

import java.time.OffsetDateTime;
import java.util.*;

public final class AttachmentBackfillSegmentResponses {
    private AttachmentBackfillSegmentResponses() { }
    public record Preview(UUID runId,long segmentNo,long runVersion,int originalItemCount,int remainingItemCount,int deletedItemCount,
            String segmentHash,String readinessCode,boolean canReserve,UUID batchId,AttachmentBatchResponses.Preview batchPreview) { }
    public record Reservation(UUID runId,long segmentNo,UUID batchId,long inventoryVersionAtReservation,int originalItemCount,
            int reservedItemCount,int deletedBeforeReservation,String segmentHash,String currentBatchStatusCode,OffsetDateTime reservedAt) { }
    public record Summary(UUID runId,long candidateCount,long remainingItemCount,long deletedItemCount,long segmentCount,long reservedSegmentCount,
            long reservedRemainingItemCount,long unreservedItemCount,long unreservedInputChangedCount,Map<String,Long> collectionCounts,
            Map<String,Long> applicationCounts,Map<String,Long> rollbackCounts) { }
}
