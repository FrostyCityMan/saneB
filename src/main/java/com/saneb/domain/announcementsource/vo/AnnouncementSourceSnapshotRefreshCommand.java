/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceSnapshotRefreshCommand.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.vo;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 동일 provider 원문의 변경된 current snapshot을 낙관적으로 갱신합니다.
 */
public record AnnouncementSourceSnapshotRefreshCommand(
        UUID sourceId,
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
        String rawPayloadJson,
        String rawHash,
        UUID localGovernmentSourceId,
        String canonicalSourceUrl,
        String normalizedTitle,
        String normalizedAgencyName,
        LocalDate postedDate,
        String expectedRawHash,
        int expectedVersion
) {
}
