package com.saneb.domain.announcementattachment.vo;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 목록/count/상세가 공유하는 현재 판정 projection. 본문·locator·lease는 포함하지 않는다. */
public record AttachmentCurrentSourceRow(
        UUID sourceId,
        String publicCode,
        String providerCode,
        String title,
        String agencyName,
        LocalDate applicationEndDate,
        OffsetDateTime collectedAt,
        String reviewStatusCode,
        UUID baseDecisionId,
        String baseStatus,
        String baseReason,
        UUID ruleReleaseId,
        String baseTargetCodes,
        String baseSupportCodes,
        UUID attachmentDecisionId,
        String attachmentStatus,
        String attachmentReason,
        UUID setId,
        String setHash,
        String inputHash,
        String attachmentTargetCodes,
        String attachmentSupportCodes,
        String effectiveStatus,
        String effectiveReason,
        Boolean reviewRequired,
        Boolean attachmentStale,
        UUID policyId,
        UUID confirmationId,
        String confirmationStatus,
        Integer sourceVersion,
        Integer attachmentVersion,
        UUID jobId,
        String jobStatus,
        String jobError,
        String intakeStatus,
        String discoveryStatus,
        Boolean discoveryComplete,
        Integer discoveredCount,
        Integer processedCount,
        Boolean activeNormalJob
) { }
