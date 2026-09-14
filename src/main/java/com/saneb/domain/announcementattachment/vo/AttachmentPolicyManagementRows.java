package com.saneb.domain.announcementattachment.vo;

import com.saneb.domain.announcementattachment.dto.AttachmentPolicyResponses;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class AttachmentPolicyManagementRows {
    private AttachmentPolicyManagementRows() { }
    public record Row(UUID policyId,String policyCode,Integer versionNo,Integer rowVersion,String policyStatusCode,String modeCode,
            UUID ruleReleaseId,String ruleReleaseStatusCode,String policyHash,String settingsJson,String profileManifestJson,
            UUID createdBy,OffsetDateTime createdAt,OffsetDateTime updatedAt,OffsetDateTime publishedAt,
            UUID copiedFromPolicyId,UUID creationIdempotencyKey,String creationRequestHash,String creationOperationCode) {
        public AttachmentPolicyResponses.Summary selectSummary() {
            return new AttachmentPolicyResponses.Summary(policyId,policyCode,versionNo,rowVersion,policyStatusCode,modeCode,
                    ruleReleaseId,ruleReleaseStatusCode,policyHash,createdAt,publishedAt);
        }
        @Override public String toString() { return "AttachmentPolicyRow[policyId="+policyId+",configuration=REDACTED]"; }
    }
    public record Search(String policyStatusCode,UUID ruleReleaseId,int size,int offset) { }
    public record Insert(UUID policyId,String policyCode,int versionNo,String modeCode,UUID ruleReleaseId,
            String settingsJson,String profileManifestJson,UUID actorId,UUID copiedFromPolicyId,UUID idempotencyKey,String requestHash,String operationCode) { }
    public record Update(UUID policyId,int expectedVersion,String modeCode,UUID ruleReleaseId,String settingsJson,String profileManifestJson) { }
}
