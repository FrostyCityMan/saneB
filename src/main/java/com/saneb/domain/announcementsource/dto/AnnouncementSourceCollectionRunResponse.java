/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceCollectionRunResponse.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.dto;

import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRunRow;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AnnouncementSourceCollectionRunResponse(
        UUID runId,
        String publicCode,
        UUID requestId,
        String requestPublicCode,
        String providerCode,
        String requestTypeCode,
        String runStatusCode,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        int totalCount,
        int collectedCount,
        int skippedEndedCount,
        int duplicateCount,
        int failedCount,
        String errorMessage,
        OffsetDateTime createdAt
) {

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    public static AnnouncementSourceCollectionRunResponse from(AnnouncementSourceCollectionRunRow row) {
        return new AnnouncementSourceCollectionRunResponse(
                row.runId(),
                row.publicCode(),
                row.requestId(),
                row.requestPublicCode(),
                row.providerCode(),
                row.requestTypeCode(),
                row.runStatusCode(),
                row.startedAt(),
                row.finishedAt(),
                row.totalCount(),
                row.collectedCount(),
                row.skippedEndedCount(),
                row.duplicateCount(),
                row.failedCount(),
                row.errorMessage(),
                row.createdAt()
        );
    }
}
