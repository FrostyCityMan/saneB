package com.saneb.domain.announcementattachment.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 분류 내부 입력은 원문을 포함할 수 있으므로 API/감사 로그에 직접 반환하지 않는다. */
public final class AttachmentEvaluationRows {
    private AttachmentEvaluationRows() { }
    public record Base(UUID evaluationId, UUID contentVersionId, UUID releaseId, String releaseCode,
                       String releaseHash, String contentHash, String providerCode, String agencyName,
                       String status, String reason, String titleStage, String bodyStage,
                       String bodySource, String bodyAvailability) { }
    public record File(UUID fileId, UUID extractionId, String role, String downloadStatus,
                       String quality, String text, String blocksJson, String errorCode) { }
    public record Evaluation(UUID evaluationId, UUID sourceId, UUID baseEvaluationId, UUID setId,
                             UUID policyId, UUID ruleReleaseId, String engineVersion,
                             String inputHash, String decisionHash, String status, String reason,
                             String warningCodesJson, Boolean current, OffsetDateTime evaluatedAt) { }
}
