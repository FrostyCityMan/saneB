package com.saneb.domain.announcementattachment.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AttachmentFileSummaryRow(UUID fileId, UUID setId, String displayName, String detectedTypeCode,
        String documentRoleCode, String roleOriginCode, String downloadStatusCode, Long downloadedBytes,
        String binaryHash, String downloadErrorCode, UUID extractionId, String qualityCode,
        Integer characterCount, Integer pageCount, Integer durationMs, String extractionErrorCode,
        OffsetDateTime extractedAt, UUID reusedFromExtractionId, UUID roleExtractionId,
        com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.Assessment roleAssessment) {
    public AttachmentFileSummaryRow(UUID fileId,UUID setId,String displayName,String detectedTypeCode,String documentRoleCode,String roleOriginCode,
            String downloadStatusCode,Long downloadedBytes,String binaryHash,String downloadErrorCode,UUID extractionId,String qualityCode,
            Integer characterCount,Integer pageCount,Integer durationMs,String extractionErrorCode,OffsetDateTime extractedAt,UUID reusedFromExtractionId) {
        this(fileId,setId,displayName,detectedTypeCode,documentRoleCode,roleOriginCode,downloadStatusCode,downloadedBytes,binaryHash,
                downloadErrorCode,extractionId,qualityCode,characterCount,pageCount,durationMs,extractionErrorCode,extractedAt,reusedFromExtractionId,null,null);
    }
    public AttachmentFileSummaryRow(UUID fileId,UUID setId,String displayName,String detectedTypeCode,String documentRoleCode,String roleOriginCode,
            String downloadStatusCode,Long downloadedBytes,String binaryHash,String downloadErrorCode,UUID extractionId,String qualityCode,
            Integer characterCount,Integer pageCount,Integer durationMs,String extractionErrorCode,OffsetDateTime extractedAt) {
        this(fileId,setId,displayName,detectedTypeCode,documentRoleCode,roleOriginCode,downloadStatusCode,downloadedBytes,binaryHash,
                downloadErrorCode,extractionId,qualityCode,characterCount,pageCount,durationMs,extractionErrorCode,extractedAt,null);
    }
}
