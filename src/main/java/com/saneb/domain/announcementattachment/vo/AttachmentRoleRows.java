package com.saneb.domain.announcementattachment.vo;

import java.util.UUID;

/** 원문을 JVM으로 읽지 않고 같은 source의 검증된 extraction을 DB 안에서 복제한다. */
public final class AttachmentRoleRows {
    private AttachmentRoleRows() { }
    public record File(UUID fileId, UUID extractionId, String role, String roleOrigin, String locatorHash,
            String binaryHash, String downloadStatus, Integer sortOrder, String extractorVersion, String extractorConfigHash) { }
    public record Copy(UUID sourceId, UUID originalSetId, UUID originalFileId, UUID originalExtractionId,
            UUID newSetId, UUID newFileId, UUID newExtractionId, String role, String roleOrigin) { }
}
