package com.saneb.domain.announcementattachment.dto;

import com.saneb.domain.announcementattachment.vo.AttachmentBlockRow;
import com.saneb.domain.announcementattachment.vo.AttachmentFileSummaryRow;
import com.saneb.domain.announcementattachment.vo.AttachmentSetRow;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 조회용 계약에는 binary 경로, fetch URL, safe locator 원문, lease token을 포함하지 않습니다. */
public final class AttachmentEvidenceResponses {
    private AttachmentEvidenceResponses() { }
    public record SetSummary(UUID setId, UUID contentVersionId, UUID policyId, String discoveryStatusCode,
            String setStatusCode, String manifestHash, String profileHash, int discoveredCount, int processedCount,
            boolean discoveryComplete, OffsetDateTime createdAt, OffsetDateTime discoveredAt, OffsetDateTime sealedAt,
            java.util.List<String> warningCodes) {
        public static SetSummary from(AttachmentSetRow row) {
            return new SetSummary(row.setId(), row.contentVersionId(), row.policyId(), row.discoveryStatus(), row.setStatus(),
                    row.manifestHash(), row.profileHash(), row.discoveredCount(), row.processedCount(), row.discoveryComplete(),
                    row.createdAt(), row.discoveredAt(), row.sealedAt(), row.warningCodes());
        }
    }
    public record FileSummary(UUID fileId, UUID setId, String displayName, String detectedTypeCode,
            String documentRoleCode, String roleOriginCode, String downloadStatusCode, long downloadedBytes,
            String binaryHash, String downloadErrorCode, UUID extractionId, String qualityCode, Integer characterCount,
            Integer pageCount, Integer durationMs, String extractionErrorCode, OffsetDateTime extractedAt, UUID reusedFromExtractionId) {
        public static FileSummary from(AttachmentFileSummaryRow row) {
            return new FileSummary(row.fileId(), row.setId(), row.displayName(), row.detectedTypeCode(), row.documentRoleCode(),
                    row.roleOriginCode(), row.downloadStatusCode(), row.downloadedBytes(), row.binaryHash(), row.downloadErrorCode(),
                    row.extractionId(), row.qualityCode(), row.characterCount(), row.pageCount(), row.durationMs(), row.extractionErrorCode(), row.extractedAt(),row.reusedFromExtractionId());
        }
    }
    public record Block(int blockIndex, int startOffset, int endOffset, String evidenceScopeId, boolean scopeReliable,
            String locator, String text, int textStartOffset, int textEndOffset, boolean hasMoreText) {
        public static Block from(AttachmentBlockRow row) {
            return new Block(row.blockIndex(), row.startOffset(), row.endOffset(), row.evidenceScopeId(), row.scopeReliable(),
                    row.locator(), row.text(), row.textStartOffset(), row.textEndOffset(), row.hasMoreText());
        }
    }
}
