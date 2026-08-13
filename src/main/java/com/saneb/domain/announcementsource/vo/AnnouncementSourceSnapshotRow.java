/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceSnapshotRow.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.vo;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AnnouncementSourceSnapshotRow(
        UUID sourceId,
        String publicCode,
        String providerCode,
        String providerNoticeId,
        String title,
        String agencyName,
        LocalDate applicationStartDate,
        LocalDate applicationEndDate,
        OffsetDateTime postedAt,
        OffsetDateTime modifiedAt,
        String sourceUrl,
        String bodyText,
        String inquiryText,
        String applicationMethodText,
        String sourceCompletenessCode,
        String missingFieldsJson,
        String rawHash,
        String reviewStatusCode,
        String semanticStatusCode,
        String semanticReasonCode,
        String semanticMatchedKeywords,
        Integer classificationRowVersion,
        OffsetDateTime collectedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
