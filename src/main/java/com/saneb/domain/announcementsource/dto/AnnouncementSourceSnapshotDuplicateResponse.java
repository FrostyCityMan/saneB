/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceSnapshotDuplicateResponse.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.dto;

import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotDuplicateRow;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AnnouncementSourceSnapshotDuplicateResponse(
        UUID duplicateId,
        UUID candidateSourceId,
        String candidatePublicCode,
        String candidateProviderCode,
        String candidateTitle,
        String candidateAgencyName,
        LocalDate candidatePostedDate,
        String candidateSourceUrl,
        String matchTypeCode,
        boolean titleMatched,
        boolean agencyMatched,
        boolean postedDateMatched,
        boolean sourceUrlMatched,
        String matchReason,
        String decisionStatusCode,
        OffsetDateTime decidedAt,
        String decisionNote
) {

    /**
     * 교차 제공자 중복 조회 결과를 API 응답으로 변환합니다.
     *
     * @param row 교차 중복 조회 결과
     * @return 교차 중복 응답
     */
    public static AnnouncementSourceSnapshotDuplicateResponse from(AnnouncementSourceSnapshotDuplicateRow row) {
        return new AnnouncementSourceSnapshotDuplicateResponse(
                row.duplicateId(), row.candidateSourceId(), row.candidatePublicCode(), row.candidateProviderCode(),
                row.candidateTitle(), row.candidateAgencyName(), row.candidatePostedDate(), row.candidateSourceUrl(),
                row.matchTypeCode(), row.titleMatched(), row.agencyMatched(), row.postedDateMatched(),
                row.sourceUrlMatched(), row.matchReason(), row.decisionStatusCode(), row.decidedAt(), row.decisionNote()
        );
    }
}
