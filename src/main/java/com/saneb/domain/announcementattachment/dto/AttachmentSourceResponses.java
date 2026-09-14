package com.saneb.domain.announcementattachment.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** API에 공개할 판정과 본문. 실행 snapshot/다운로드 locator/원본 payload는 포함하지 않는다. */
public final class AttachmentSourceResponses {
    private AttachmentSourceResponses() { }
    public record Classification(UUID decisionId, String semanticStatusCode, String reasonCode, UUID ruleReleaseId,
            UUID setId, String setHash, String inputHash, List<String> targetCategoryCodes, List<String> supportTypeCodes) { }
    public record Summary(UUID sourceId, String publicCode, String providerCode, String title, String agencyName,
            LocalDate applicationEndDate, OffsetDateTime collectedAt, String reviewStatusCode,
            Classification baseClassification, Classification effectiveClassification, Classification previewClassification,
            AttachmentState attachmentSummary, boolean isAttachmentReviewRequired, UUID attachmentPolicyId,
            int sourceVersion, int attachmentVersion, UUID confirmationId, String confirmationStatusCode,
            ProcessingFlow processingFlow) { }
    /** 조회 시점의 작업 흐름이다. 검수 저장 권한이나 자동 승인 결과가 아니다. */
    public record ProcessingFlow(String statusCode, boolean isAutomaticAnalysisComplete, boolean isFinalReviewAvailable) { }
    public record AttachmentState(UUID jobId, String jobStatusCode, String errorCode, String intakeStatusCode,
            boolean isStale, String discoveryStatusCode, Boolean isDiscoveryComplete,
            Integer totalCount, Integer processedCount) { }
    public record Content(String sourceUrl, String bodyText, String inquiryText, String applicationMethodText,
            String sourceCompletenessCode) { }
    public record Details(Summary source, Content content) { }
}
