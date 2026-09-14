package com.saneb.domain.announcementattachment.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class AttachmentReviewRows {
    private AttachmentReviewRows() { }
    public record Confirmation(UUID id, UUID sourceId, UUID evaluationId, String setHash, String requestHash,
            String reviewMethod, Boolean current, Integer sourceVersion, Integer attachmentVersion, OffsetDateTime confirmedAt) { }
    public record RestoredBinding(UUID restorationId, UUID confirmationId, UUID sourceId,
            Integer sourceVersion, Integer attachmentVersion) { }
    public record ConfirmationInsert(UUID id, UUID sourceId, UUID evaluationId, String setHash, UUID actorId,
            String method, String acknowledgementJson, String reviewNote, UUID idempotencyKey, String requestHash,
            int sourceVersion, int attachmentVersion) {
        @Override public String toString() { return "AttachmentConfirmationInsert[reviewNote=REDACTED]"; }
    }
    public record Tag(UUID evaluationId, UUID confirmationId, String code) { }
    public record Link(UUID announcementId, String announcementCode, UUID confirmationId, String requestHash) { }
    public record LinkInsert(UUID id, UUID sourceId, UUID announcementId, UUID actorId, UUID confirmationId, String requestHash) { }
}
