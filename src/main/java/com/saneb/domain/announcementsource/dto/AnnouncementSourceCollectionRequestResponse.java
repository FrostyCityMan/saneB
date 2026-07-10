/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceCollectionRequestResponse.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.dto;

import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRequestRow;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AnnouncementSourceCollectionRequestResponse(
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
        UUID approvedBy,
        OffsetDateTime approvedAt,
        String approvalNote,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    public static AnnouncementSourceCollectionRequestResponse from(AnnouncementSourceCollectionRequestRow row) {
        return new AnnouncementSourceCollectionRequestResponse(
                row.requestId(),
                row.publicCode(),
                row.providerCode(),
                row.requestTypeCode(),
                row.requestStatusCode(),
                row.requestedBy(),
                row.requestedAt(),
                row.requestedFrom(),
                row.searchKeyword(),
                row.searchRegionCode(),
                row.searchCategoryCode(),
                row.startDate(),
                row.endDate(),
                row.maxCount(),
                row.requestNote(),
                row.approvedBy(),
                row.approvedAt(),
                row.approvalNote(),
                row.createdAt(),
                row.updatedAt()
        );
    }
}
