package com.saneb.domain.announcementattachment.vo;

import java.util.UUID;

public final class AttachmentEvidenceCommands {
    private AttachmentEvidenceCommands() { }
    public record SetInsert(UUID setId, UUID sourceId, UUID contentVersionId, UUID policyId,
                            String dataPurpose, String profileHash) { }
    public record FileInsert(UUID fileId, UUID setId, UUID sourceId, String locatorHash, String locatorJson,
                             String displayName, String detectedType, String role, String roleOrigin,
                             String downloadStatus, long downloadedBytes, String binaryHash,
                             int sortOrder, String errorCode) { }
    public record ExtractionInsert(UUID extractionId, UUID fileId, UUID setId, UUID sourceId,
                                   int attemptNo, String extractorCode, String extractorVersion,
                                   String extractorConfigHash, String quality, String text, String textHash,
                                   String blocksJson, int characterCount, Integer pageCount, int durationMs,
                                   String errorCode, java.time.OffsetDateTime completedAt) {
        public ExtractionInsert(UUID extractionId,UUID fileId,UUID setId,UUID sourceId,int attemptNo,String extractorCode,
                String extractorVersion,String extractorConfigHash,String quality,String text,String textHash,String blocksJson,
                int characterCount,Integer pageCount,int durationMs,String errorCode) {
            this(extractionId,fileId,setId,sourceId,attemptNo,extractorCode,extractorVersion,extractorConfigHash,quality,text,
                    textHash,blocksJson,characterCount,pageCount,durationMs,errorCode,null);
        }
    }
}
