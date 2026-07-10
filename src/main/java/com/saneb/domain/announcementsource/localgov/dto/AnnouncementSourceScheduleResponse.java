/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceScheduleResponse.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.localgov.dto;

import com.saneb.domain.announcementsource.localgov.vo.AnnouncementSourceScheduleRow;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AnnouncementSourceScheduleResponse(
        UUID scheduleId,
        String publicCode,
        String providerCode,
        String scheduleName,
        String cronExpression,
        String timezone,
        String scheduleStatusCode,
        Integer maxCount,
        OffsetDateTime approvedAt,
        String approvalNote,
        OffsetDateTime lastRunAt,
        OffsetDateTime nextRunAt,
        OffsetDateTime createdAt
) {

    /**
     * 스케줄 조회 결과를 응답으로 변환합니다.
     *
     * @param row 스케줄 조회 결과
     * @return 스케줄 응답
     */
    public static AnnouncementSourceScheduleResponse from(AnnouncementSourceScheduleRow row) {
        return new AnnouncementSourceScheduleResponse(
                row.scheduleId(), row.publicCode(), row.providerCode(), row.scheduleName(), row.cronExpression(),
                row.timezone(), row.scheduleStatusCode(), row.maxCount(), row.approvedAt(), row.approvalNote(),
                row.lastRunAt(), row.nextRunAt(), row.createdAt()
        );
    }
}
