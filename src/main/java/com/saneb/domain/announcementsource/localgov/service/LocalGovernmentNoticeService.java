/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: LocalGovernmentNoticeService.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.localgov.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionRequestResponse;
import com.saneb.domain.announcementsource.localgov.dto.AnnouncementSourceScheduleCreateRequest;
import com.saneb.domain.announcementsource.localgov.dto.AnnouncementSourceScheduleResponse;
import com.saneb.domain.announcementsource.localgov.dto.AnnouncementSourceScheduleStatusRequest;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeCollectionRequest;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeCollectionSummaryResponse;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeParserProfileResponse;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeSourceEnabledRequest;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeSourceResponse;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeSourceSaveRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface LocalGovernmentNoticeService {

    /**
     * 지자체 공고 URL 목록을 조회합니다.
     */
    PageResponse<LocalGovernmentNoticeSourceResponse> selectSourceList(
            String sidoName,
            String validationStatusCode,
            String collectionStatusCode,
            Boolean enabled,
            String keyword,
            int page,
            int size
    );

    /**
     * 지자체 공고 URL 상세를 조회합니다.
     */
    LocalGovernmentNoticeSourceResponse selectSourceDetails(UUID sourceId);

    /**
     * 지자체 공고 URL을 등록합니다.
     */
    LocalGovernmentNoticeSourceResponse insertSource(
            Authentication authentication,
            LocalGovernmentNoticeSourceSaveRequest request
    );

    /**
     * 지자체 공고 URL을 수정합니다.
     */
    LocalGovernmentNoticeSourceResponse updateSource(
            Authentication authentication,
            UUID sourceId,
            LocalGovernmentNoticeSourceSaveRequest request
    );

    /**
     * 지자체 공고 URL의 ON/OFF를 변경합니다.
     */
    LocalGovernmentNoticeSourceResponse updateSourceEnabled(
            Authentication authentication,
            UUID sourceId,
            LocalGovernmentNoticeSourceEnabledRequest request
    );

    /**
     * 지자체 공고 URL을 소프트 삭제합니다.
     */
    void deleteSource(Authentication authentication, UUID sourceId);

    /**
     * 수동 수집 승인 요청을 생성합니다.
     */
    AnnouncementSourceCollectionRequestResponse insertCollectionRequest(
            Authentication authentication,
            UUID sourceId,
            LocalGovernmentNoticeCollectionRequest request
    );

    /**
     * 파서 프로필 목록을 조회합니다.
     */
    List<LocalGovernmentNoticeParserProfileResponse> selectParserProfileList();

    /**
     * 지자체 수집 신호등 현황을 조회합니다.
     */
    LocalGovernmentNoticeCollectionSummaryResponse selectCollectionSummary();

    /**
     * 정기 수집 스케줄을 등록합니다.
     */
    AnnouncementSourceScheduleResponse insertSchedule(
            Authentication authentication,
            AnnouncementSourceScheduleCreateRequest request
    );

    /**
     * 정기 수집 스케줄 목록을 조회합니다.
     */
    List<AnnouncementSourceScheduleResponse> selectScheduleList();

    /**
     * 정기 수집 스케줄 상태를 변경합니다.
     */
    AnnouncementSourceScheduleResponse updateScheduleStatus(
            Authentication authentication,
            UUID scheduleId,
            AnnouncementSourceScheduleStatusRequest request
    );

    /**
     * 실행 예정인 승인 스케줄을 처리합니다.
     */
    void executeDueSchedules();
}
