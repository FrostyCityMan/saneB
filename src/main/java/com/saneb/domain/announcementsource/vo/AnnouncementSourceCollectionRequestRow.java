/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceCollectionRequestRow.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.vo;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AnnouncementSourceCollectionRequestRow(
        UUID requestId,
        String publicCode,
        String providerCode,
        String requestTypeCode,
        String requestStatusCode,
        UUID requestedBy,
        OffsetDateTime requestedAt,
        String requestedFrom,
        String searchKeyword,
        String searchRegionCode,
        String searchCategoryCode,
        LocalDate startDate,
        LocalDate endDate,
        Integer maxCount,
        String requestNote,
        UUID localGovernmentSourceId,
        UUID scheduleId,
        UUID approvedBy,
        OffsetDateTime approvedAt,
        String approvalNote,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public AnnouncementSourceCollectionRequestRow withSearchKeyword(String nextSearchKeyword) {
        return new AnnouncementSourceCollectionRequestRow(
                requestId,
                publicCode,
                providerCode,
                requestTypeCode,
                requestStatusCode,
                requestedBy,
                requestedAt,
                requestedFrom,
                nextSearchKeyword,
                searchRegionCode,
                searchCategoryCode,
                startDate,
                endDate,
                maxCount,
                requestNote,
                localGovernmentSourceId,
                scheduleId,
                approvedBy,
                approvedAt,
                approvalNote,
                createdAt,
                updatedAt
        );
    }
}
