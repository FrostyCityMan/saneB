package com.saneb.domain.announcementattachment.dto;

import com.saneb.domain.announcementattachment.vo.AttachmentBatchRows;
import java.time.OffsetDateTime;
import java.util.*;

public final class AttachmentBatchResponses {
    private AttachmentBatchResponses() { }
    public record Candidate(UUID sourceId,String providerCode,UUID baseEvaluationId,Integer sourceVersion,Integer attachmentVersion,
            String readinessCode,String executionHash) { }
    public record Preview(AttachmentBatchRequests.Scope scope,String scopeHash,UUID ruleReleaseId,String policyHash,
            List<AttachmentBatchRows.Bucket> counts,long candidateCount,int selectedCount,long remainingCount,
            long maximumDownloadBytes,long maximumHttpRequests,int currentHttpRequests,boolean canReserve,
            List<Candidate> items) { }
    public record Batch(UUID batchId,UUID policyId,String statusCode,String scopeHash,Integer rowVersion,Integer itemCount,
            Integer remainingItemCount,Integer deletedItemCount,Map<String,Long> jobCounts,Map<String,Object> frozenScope,
            OffsetDateTime createdAt) { }
}
