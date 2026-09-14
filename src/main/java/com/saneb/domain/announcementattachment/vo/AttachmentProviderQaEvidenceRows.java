package com.saneb.domain.announcementattachment.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class AttachmentProviderQaEvidenceRows {
    private AttachmentProviderQaEvidenceRows(){ }
    public record Scope(UUID policyId,String snapshotHash,String catalogHash,String planHash){ }
    public record Page(UUID runId,int size,int offset){ }
    public record Item(UUID caseId,UUID runId,Integer ordinal,String caseCode,String inputHash,String profileHash,Integer expectedFileCount,
                       Integer maximumSeconds,Integer maximumRequests,Long maximumBytes,Integer requestReservations,Long reservedBytes,
                       String statusCode,Integer rowVersion,OffsetDateTime startedAt,OffsetDateTime completedAt,String errorCode,String evidenceJson,String evidenceHash) {
        @Override public String toString(){return "ProviderQaEvidence[caseId="+caseId+",evidence=REDACTED]";}
    }
}
