package com.saneb.domain.announcementattachment.service;

import com.saneb.domain.announcementattachment.dto.AttachmentSourceResponses.ProcessingFlow;
import com.saneb.domain.announcementattachment.vo.AttachmentCurrentSourceRow;
import java.util.Set;

/** 동일 snapshot의 현재 판정 projection만 해석한다. 판정 이력·작업·관리자 확인을 변경하지 않는다. */
public final class AttachmentProcessingFlow {
    private AttachmentProcessingFlow() { }
    public static final Set<String> STATUS_CODES = Set.of("NOT_APPLIED", "CLASSIFICATION_PENDING", "CONFIGURATION_REQUIRED",
            "AUTOMATIC_PROCESSING", "EVIDENCE_STALE", "TECHNICAL_EXCEPTION", "READY_FOR_FINAL_REVIEW",
            "FINAL_REVIEW_EXCEPTION", "FINAL_REVIEW_CONFIRMED");
    private static final Set<String> CONFIGURATION = Set.of("PROFILE_REQUIRED", "BASE_RECLASSIFICATION_REQUIRED", "POLICY_BINDING_CHANGED");
    private static final Set<String> ACTIVE = Set.of("SCOPE_READY", "PENDING", "RUNNING", "RETRY_WAIT", "PAUSED");
    private static final Set<String> INACTIVE = Set.of("NOT_REQUESTED", "SUCCEEDED", "FAILED", "PARTIAL_FAILED", "CANCELLED", "CONFLICT");
    // 엔진은 기술 불완전을 A/B·문맥 사유보다 먼저 반환한다. 미지 사유를 완료로 승격하지 않는다.
    private static final Set<String> COMPLETE_REASONS = Set.of("TARGET_SUPPORT_CONFIRMED", "EXTENDED_TARGET_SUPPORT_CONFIRMED",
            "TITLE_GROUP_A_MATCHED", "BODY_GROUP_A_MATCHED", "BODY_GROUP_B_MATCHED", "ATTACHMENT_GROUP_A_MATCHED",
            "ATTACHMENT_GROUP_B_MATCHED", "ATTACHMENT_CONTEXT_REVIEW", "EXTENDED_COMBINATION_NOT_CONFIRMED",
            "BODY_COMBINATION_NOT_CONFIRMED");

    public static ProcessingFlow selectFlowDetails(AttachmentCurrentSourceRow row) {
        if (!Boolean.TRUE.equals(row.reviewRequired())) return flow("NOT_APPLIED", false, false);
        if (row.baseDecisionId() == null) return flow("CLASSIFICATION_PENDING", false, false);
        if (contains(CONFIGURATION, row.intakeStatus())) return flow("CONFIGURATION_REQUIRED", false, false);
        if ("ATTACHMENT_STALE".equals(row.effectiveReason())) return flow("EVIDENCE_STALE", false, false);
        if (Boolean.TRUE.equals(row.activeNormalJob()) || contains(ACTIVE, row.jobStatus()))
            return flow("AUTOMATIC_PROCESSING", false, false);
        if (Boolean.TRUE.equals(row.attachmentStale())) return flow("EVIDENCE_STALE", false, false);
        if (row.attachmentDecisionId() == null) {
            boolean waiting = "NOT_REQUESTED".equals(row.jobStatus());
            return flow(waiting ? "AUTOMATIC_PROCESSING" : "TECHNICAL_EXCEPTION", false, false);
        }
        // Mapper는 현재 source/base/rule/policy와 SEALED set을 결합한다. 결합 정보 누락도 실패 시 차단한다.
        if (row.setId() == null || row.setHash() == null || !row.setHash().matches("[0-9a-f]{64}")
                || !contains(INACTIVE, row.jobStatus())
                || !Set.of("ACCEPTED", "REVIEW_REQUIRED").contains(row.attachmentStatus() == null ? "" : row.attachmentStatus()))
            return flow("TECHNICAL_EXCEPTION", false, false);

        boolean complete = selectCompleteEvidence(row)
                && contains(COMPLETE_REASONS, row.attachmentReason())
                && ("SUCCEEDED".equals(row.jobStatus()) || "NOT_REQUESTED".equals(row.jobStatus()));
        // 수동 원문 확인으로 현재 확인이 생겨도 기술 실패/본문 부족이 자동 해결됐다고 표시하지 않는다.
        if (row.confirmationId() != null && "CURRENT".equals(row.confirmationStatus()))
            return flow("FINAL_REVIEW_CONFIRMED", complete, true);
        if (!complete) return flow("TECHNICAL_EXCEPTION", false, true);
        return flow("ACCEPTED".equals(row.attachmentStatus()) ? "READY_FOR_FINAL_REVIEW" : "FINAL_REVIEW_EXCEPTION", true, true);
    }

    private static boolean selectCompleteEvidence(AttachmentCurrentSourceRow row) {
        if (!Boolean.TRUE.equals(row.discoveryComplete()) || row.discoveredCount() == null || row.processedCount() == null
                || !row.discoveredCount().equals(row.processedCount())) return false;
        return "NO_FILES".equals(row.discoveryStatus()) ? row.discoveredCount() == 0
                : "FOUND".equals(row.discoveryStatus()) && row.discoveredCount() > 0 && row.discoveredCount() <= 10;
    }
    private static boolean contains(Set<String> values, String value) { return value != null && values.contains(value); }
    private static ProcessingFlow flow(String status, boolean complete, boolean available) {
        return new ProcessingFlow(status, complete, available);
    }
}
