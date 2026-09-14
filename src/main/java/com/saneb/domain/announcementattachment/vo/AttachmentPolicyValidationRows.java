package com.saneb.domain.announcementattachment.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class AttachmentPolicyValidationRows {
    private AttachmentPolicyValidationRows() { }
    public record Run(UUID runId, UUID policyId, Integer policyVersion, UUID ruleReleaseId, Integer ruleVersion,
            String snapshotHash, String inputSnapshotJson, String statusCode, Integer rowVersion, UUID requestedBy,
            UUID idempotencyKey, String requestHash, UUID leaseToken, OffsetDateTime leaseExpiresAt, String errorCode,
            OffsetDateTime createdAt, OffsetDateTime startedAt, OffsetDateTime completedAt, Boolean inputVersionsCurrent) {
        @Override public String toString() { return "PolicyValidationRun[id=" + runId + ",status=" + statusCode + "]"; }
    }
    public record Insert(UUID runId, UUID policyId, int policyVersion, UUID ruleReleaseId, int ruleVersion,
            String snapshotHash, String inputSnapshotJson, UUID requestedBy, UUID idempotencyKey, String requestHash) { }
    public record Search(UUID policyId, int size, int offset) { }
    public record Step(UUID runId, String stepCode, String statusCode, String evidenceJson, String evidenceHash, OffsetDateTime createdAt) { }
    public record Target(UUID sourceId, String publicCode, String parserProfileCode, String noticeUrl,
            String profileConfigurationJson) {
        @Override public String toString() { return "PolicyValidationTarget[id=" + sourceId + "]"; }
    }
}
