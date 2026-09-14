package com.saneb.domain.announcementattachment.dto;

import com.saneb.domain.announcementattachment.vo.AttachmentBatchRows;
import java.time.OffsetDateTime;
import java.util.*;

public final class AttachmentBackfillResponses {
    private AttachmentBackfillResponses() { }
    public record Preview(AttachmentBackfillRequests.Scope scope,String scopeHash,String candidateHash,UUID ruleReleaseId,String policyHash,
            List<AttachmentBatchRows.Bucket> counts,long candidateCount,long segmentCount,int previewHttpRequests,boolean canInventory) { }
    public record Inventory(UUID runId,UUID policyId,String statusCode,String scopeHash,String candidateHash,long rowVersion,
            long candidateCount,long remainingItemCount,long deletedItemCount,int segmentSize,long segmentCount,
            Map<String,Object> frozenScope,OffsetDateTime createdAt) { }
}
