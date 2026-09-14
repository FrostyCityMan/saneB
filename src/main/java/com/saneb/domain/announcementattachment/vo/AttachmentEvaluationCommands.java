package com.saneb.domain.announcementattachment.vo;

import java.util.UUID;

public final class AttachmentEvaluationCommands {
    private AttachmentEvaluationCommands() { }
    public record Evaluation(UUID id, UUID sourceId, UUID contentVersionId, UUID baseEvaluationId,
                             UUID setId, UUID policyId, UUID ruleReleaseId, String engineVersion,
                             String inputHash, String decisionHash, String status, String reason, String warningsJson) { }
    public record Input(UUID evaluationId, UUID sourceId, UUID setId, UUID fileId, UUID extractionId,
                        String role, String inputStatus) { }
    public record Match(UUID evaluationId, UUID sourceId, UUID setId, UUID fileId, UUID extractionId,
                        UUID releaseId, String groupCode, String ruleCode, String termText,
                        int blockIndex, int startOffset, int endOffset, String action) { }
}
