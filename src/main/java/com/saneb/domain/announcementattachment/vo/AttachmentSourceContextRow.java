package com.saneb.domain.announcementattachment.vo;

import java.util.UUID;

public record AttachmentSourceContextRow(
        UUID sourceId, String providerCode, String dataPurposeCode,
        String semanticStatusCode, UUID baseEvaluationId, UUID contentVersionId,
        UUID ruleReleaseId, String titleStageCode, Integer sourceVersion,
        Integer attachmentVersion, Boolean attachmentReviewRequired,
        UUID attachmentPolicyId, UUID currentAttachmentEvaluationId
) { }
