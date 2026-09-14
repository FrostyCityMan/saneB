package com.saneb.domain.announcementattachment.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 당시 평가 입력의 식별자와 상태만 조회한다. 원문·fetch URL·감사 메모는 포함하지 않는다. */
public final class AttachmentHistoryRows {
    private AttachmentHistoryRows() { }
    public record Input(UUID fileId, UUID extractionId, String documentRoleCode, String inputStatusCode,
            String downloadStatusCode, String downloadErrorCode, String qualityCode, String extractionErrorCode,
            OffsetDateTime extractedAt, UUID reusedFromExtractionId) { }
    public record Match(UUID matchId, UUID fileId, UUID extractionId, String documentRoleCode,
            UUID keywordGroupId, String groupCode, UUID keywordRuleId, String ruleCode,
            UUID keywordTermId, String termText, String appliedActionCode,
            Integer blockIndex, Integer startOffset, Integer endOffset) { }
}
