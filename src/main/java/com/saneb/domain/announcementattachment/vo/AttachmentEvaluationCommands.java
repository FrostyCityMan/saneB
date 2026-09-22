package com.saneb.domain.announcementattachment.vo;

import java.util.UUID;

public final class AttachmentEvaluationCommands {
    private AttachmentEvaluationCommands() { }
    public record Evaluation(UUID id, UUID sourceId, UUID contentVersionId, UUID baseEvaluationId,
                             UUID setId, UUID policyId, UUID ruleReleaseId, String engineVersion,
                             String inputHash, String decisionHash, String status, String reason, String warningsJson) { }
    public record Input(UUID evaluationId, UUID sourceId, UUID setId, UUID fileId, UUID extractionId,
                        String role, String inputStatus, UUID segmentAnalysisId) {
        public Input(UUID evaluationId, UUID sourceId, UUID setId, UUID fileId, UUID extractionId, String role, String inputStatus) {
            this(evaluationId,sourceId,setId,fileId,extractionId,role,inputStatus,null);
        }
    }
    public record Match(UUID evaluationId, UUID sourceId, UUID setId, UUID fileId, UUID extractionId,
                        UUID releaseId, String groupCode, String ruleCode, String termText,
                        int blockIndex, int startOffset, int endOffset, String action, UUID segmentAnalysisId, Integer segmentIndex) {
        public Match(UUID evaluationId, UUID sourceId, UUID setId, UUID fileId, UUID extractionId, UUID releaseId,
                String groupCode, String ruleCode, String termText, int blockIndex, int startOffset, int endOffset, String action) {
            this(evaluationId,sourceId,setId,fileId,extractionId,releaseId,groupCode,ruleCode,termText,blockIndex,startOffset,endOffset,action,null,null);
        }
    }
}
