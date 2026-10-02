package com.saneb.domain.announcementattachment.dto;

import java.util.List;
import java.util.UUID;

public final class AttachmentLinkedBatchResponses {
    private AttachmentLinkedBatchResponses() { }
    public record Candidate(UUID sourceId,String readinessCode,String connectionSnapshotHash) { }
    public record Preview(UUID policyId,String scopeHash,int requestedCount,boolean canReserve,
            long maximumDownloadBytes,long maximumHttpRequests,List<Candidate> candidates) { }
}
