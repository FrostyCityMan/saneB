/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceSummaryResponse.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.dto;

import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotRow;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;

public record AnnouncementSourceSummaryResponse(
        UUID sourceId,
        String publicCode,
        String providerCode,
        String providerNoticeId,
        String title,
        String agencyName,
        LocalDate applicationStartDate,
        LocalDate applicationEndDate,
        String sourceUrl,
        String sourceCompletenessCode,
        String reviewStatusCode,
        String semanticStatusCode,
        String semanticReasonCode,
        String semanticMatchedKeywords,
        OffsetDateTime postedAt,
        OffsetDateTime collectedAt,
        List<String> targetCategoryCodes,
        List<String> supportTypeCodes
) {

    public AnnouncementSourceSummaryResponse {
        targetCategoryCodes = targetCategoryCodes == null ? List.of() : List.copyOf(targetCategoryCodes);
        supportTypeCodes = supportTypeCodes == null ? List.of() : List.copyOf(supportTypeCodes);
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    public static AnnouncementSourceSummaryResponse from(AnnouncementSourceSnapshotRow row) {
        return new AnnouncementSourceSummaryResponse(
                row.sourceId(),
                row.publicCode(),
                row.providerCode(),
                row.providerNoticeId(),
                row.title(),
                row.agencyName(),
                row.applicationStartDate(),
                row.applicationEndDate(),
                row.sourceUrl(),
                row.sourceCompletenessCode(),
                row.reviewStatusCode(),
                row.semanticStatusCode(),
                row.semanticReasonCode(),
                row.semanticMatchedKeywords(),
                row.postedAt(),
                row.collectedAt(),
                List.of(),
                List.of()
        );
    }

    public static AnnouncementSourceSummaryResponse from(
            AnnouncementSourceSnapshotRow row,
            List<String> targetCategoryCodes,
            List<String> supportTypeCodes
    ) {
        AnnouncementSourceSummaryResponse base = from(row);
        return new AnnouncementSourceSummaryResponse(
                base.sourceId(), base.publicCode(), base.providerCode(), base.providerNoticeId(),
                base.title(), base.agencyName(), base.applicationStartDate(), base.applicationEndDate(),
                base.sourceUrl(), base.sourceCompletenessCode(), base.reviewStatusCode(),
                base.semanticStatusCode(), base.semanticReasonCode(), base.semanticMatchedKeywords(),
                base.postedAt(), base.collectedAt(), targetCategoryCodes, supportTypeCodes
        );
    }
}
