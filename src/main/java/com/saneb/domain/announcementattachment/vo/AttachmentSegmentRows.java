package com.saneb.domain.announcementattachment.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 추출 원문은 내부 입력에만 존재하며 분석 응답·감사 metadata로 복사하지 않는다. */
public final class AttachmentSegmentRows {
    private AttachmentSegmentRows() { }
    public record Input(UUID sourceId, UUID setId, UUID fileId, UUID extractionId, String qualityCode,
                        String extractedText, String blocksJson, Integer pageCount, String fileRoleCode, String fileRoleOriginCode) { }
    public record Stored(UUID id, UUID sourceId, UUID setId, UUID fileId, UUID extractionId,
                         String analysisJson, OffsetDateTime createdAt) { }
    public record Binding(UUID evaluationId, UUID sourceId, UUID setId, UUID fileId, UUID extractionId,
                          UUID policyId, UUID analysisId, String analysisVersion, String rulesHash,
                          String evaluatedFileRoleCode, Boolean evaluationCurrent) { }
    public record Insert(UUID id, UUID sourceId, UUID setId, UUID fileId, UUID extractionId, String analysisVersion,
                         String rulesHash, String textHash, String blocksHash, String analysisJson) { }
}
