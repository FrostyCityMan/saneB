package com.saneb.domain.announcementattachment.dto;

import com.saneb.domain.announcementattachment.vo.AttachmentHistoryRows;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** 자동 판정 이력은 관리자 확인·최종 선정·활성 상태와 별개다. */
public final class AttachmentHistoryResponses {
    private AttachmentHistoryResponses() { }
    public record Summary(UUID evaluationId, UUID sourceId, UUID baseEvaluationId, UUID setId, UUID policyId,
            UUID ruleReleaseId, String engineVersion, String inputHash, String decisionHash,
            String semanticStatusCode, String reasonCode, List<String> warningCodes,
            String usageCode, OffsetDateTime evaluatedAt) { }
    public record Details(Summary evaluation, List<String> autoTargetCategoryCodes, List<String> autoSupportTypeCodes,
            long inputCount, long matchCount, int currentSourceVersion, int currentAttachmentVersion) { }
    public record Input(UUID fileId, UUID extractionId, String documentRoleCode, String inputStatusCode,
            String downloadStatusCode, String downloadErrorCode, String qualityCode, String extractionErrorCode,
            OffsetDateTime extractedAt, UUID reusedFromExtractionId) {
        public static Input from(AttachmentHistoryRows.Input row) {
            return new Input(row.fileId(),row.extractionId(),row.documentRoleCode(),row.inputStatusCode(),
                    row.downloadStatusCode(),row.downloadErrorCode(),row.qualityCode(),row.extractionErrorCode(),
                    row.extractedAt(),row.reusedFromExtractionId());
        }
    }
    public record Match(UUID matchId, UUID fileId, UUID extractionId, String documentRoleCode,
            UUID keywordGroupId, String groupCode, UUID keywordRuleId, String ruleCode,
            UUID keywordTermId, String termText, String appliedActionCode, int blockIndex, int startOffset, int endOffset) {
        public static Match from(AttachmentHistoryRows.Match row) {
            return new Match(row.matchId(),row.fileId(),row.extractionId(),row.documentRoleCode(),row.keywordGroupId(),
                    row.groupCode(),row.keywordRuleId(),row.ruleCode(),row.keywordTermId(),row.termText(),
                    row.appliedActionCode(),row.blockIndex(),row.startOffset(),row.endOffset());
        }
    }
}
