/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceCollectionRunItemResponse.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.dto;

import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRunItemRow;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AnnouncementSourceCollectionRunItemResponse(
        UUID itemId,
        UUID runId,
        UUID sourceId,
        String sourcePublicCode,
        String providerNoticeId,
        String sourceUrl,
        String itemStatusCode,
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
    public static AnnouncementSourceCollectionRunItemResponse from(AnnouncementSourceCollectionRunItemRow row) {
        return new AnnouncementSourceCollectionRunItemResponse(
                row.itemId(),
                row.runId(),
                row.sourceId(),
                row.sourcePublicCode(),
                row.providerNoticeId(),
                row.sourceUrl(),
                row.itemStatusCode(),
                row.errorMessage(),
                row.createdAt()
        );
    }
}
