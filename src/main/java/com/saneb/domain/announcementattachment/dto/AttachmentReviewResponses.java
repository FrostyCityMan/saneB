package com.saneb.domain.announcementattachment.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class AttachmentReviewResponses {
    private AttachmentReviewResponses() { }
    public record Context(UUID sourceId, AttachmentReviewRequests.Version version, String decisionStatusCode,
            String reasonCode, boolean manualSourceCheckRequired, List<String> requiredAcknowledgementCodes,
            ConfirmedClassification confirmedClassification, LinkedAnnouncement linkedAnnouncement) {
        public Context(UUID sourceId, AttachmentReviewRequests.Version version, String decisionStatusCode,
                String reasonCode, boolean manualSourceCheckRequired, List<String> requiredAcknowledgementCodes) {
            this(sourceId,version,decisionStatusCode,reasonCode,manualSourceCheckRequired,requiredAcknowledgementCodes,null,null);
        }
    }
    /** 현재 버전에 일치하는 확인만 제공한다. 검수 메모와 작성자 개인정보는 재노출하지 않는다. */
    public record ConfirmedClassification(Confirmation confirmation, List<String> targetCategoryCodes,
            List<String> supportTypeCodes, ConfirmationBinding binding) {
        public ConfirmedClassification(Confirmation confirmation, List<String> targetCategoryCodes, List<String> supportTypeCodes) {
            this(confirmation,targetCategoryCodes,supportTypeCodes,null);
        }
    }
    /** 원래 검수 이력을 바꾸지 않고 현재 유효한 버전을 표현한다. restorationId=null은 최초 확인이다. */
    public record ConfirmationBinding(UUID restorationId, UUID confirmationId, UUID sourceId,
            Integer sourceVersion, Integer attachmentVersion) { }
    /** 연결 존재는 현재 활성 상태를 뜻하지 않는다. 실제 상태는 공고 관리에서 조회한다. */
    public record LinkedAnnouncement(UUID announcementId, String announcementCode) { }
    public record Confirmation(UUID sourceId, UUID confirmationId, UUID evaluationId, String setHash,
            Integer sourceVersion, Integer attachmentVersion, String reviewMethodCode, boolean isCurrent,
            OffsetDateTime confirmedAt) { }
}
