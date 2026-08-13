/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementService.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcement.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcement.dto.AnnouncementApprovalDecisionRequest;
import com.saneb.domain.announcement.dto.AnnouncementApprovalRequestCreateRequest;
import com.saneb.domain.announcement.dto.AnnouncementConditionsSaveRequest;
import com.saneb.domain.announcement.dto.AnnouncementDetailsResponse;
import com.saneb.domain.announcement.dto.AnnouncementManualStatusUpdateRequest;
import com.saneb.domain.announcement.dto.AnnouncementSaveRequest;
import com.saneb.domain.announcement.dto.AnnouncementStepsSaveRequest;
import com.saneb.domain.announcement.dto.AnnouncementSummaryResponse;
import com.saneb.domain.announcement.dto.AnnouncementV2SaveRequest;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementService {

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param keyword 입력 값
     *
     * @param targetTypeCode 입력 값
     *
     * @param manualStatusCode 입력 값
     *
     * @param approvalStatusCode 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    PageResponse<AnnouncementSummaryResponse> selectAnnouncementList(
            String keyword,
            String targetTypeCode,
            String manualStatusCode,
            String approvalStatusCode,
            int page,
            int size
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementDetailsResponse insertAnnouncement(Authentication authentication, AnnouncementSaveRequest request);

    /**
     * 다중 지원대상·지원형태 계약으로 공고를 등록합니다.
     *
     * @param authentication 인증 정보
     * @param request v2 저장 요청
     * @return 저장된 공고 상세
     */
    AnnouncementDetailsResponse insertAnnouncementV2(
            Authentication authentication,
            AnnouncementV2SaveRequest request
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementDetailsResponse selectAnnouncementDetails(UUID announcementId);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param announcementId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementDetailsResponse updateAnnouncement(
            Authentication authentication,
            UUID announcementId,
            AnnouncementSaveRequest request
    );

    /**
     * 다중 지원대상·지원형태 계약으로 공고를 수정합니다.
     *
     * @param authentication 인증 정보
     * @param announcementId 공고 식별자
     * @param request v2 저장 요청
     * @return 수정된 공고 상세
     */
    AnnouncementDetailsResponse updateAnnouncementV2(
            Authentication authentication,
            UUID announcementId,
            AnnouncementV2SaveRequest request
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param announcementId 입력 값
     *
     * @param request 입력 값
     */
    void updateAnnouncementConditions(
            Authentication authentication,
            UUID announcementId,
            AnnouncementConditionsSaveRequest request
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param announcementId 입력 값
     *
     * @param request 입력 값
     */
    void updateAnnouncementSteps(
            Authentication authentication,
            UUID announcementId,
            AnnouncementStepsSaveRequest request
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param announcementId 입력 값
     *
     * @param request 입력 값
     */
    void updateAnnouncementManualStatus(
            Authentication authentication,
            UUID announcementId,
            AnnouncementManualStatusUpdateRequest request
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param announcementId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementDetailsResponse insertAnnouncementApprovalRequest(
            Authentication authentication,
            UUID announcementId,
            AnnouncementApprovalRequestCreateRequest request
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param announcementId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementDetailsResponse updateAnnouncementApproval(
            Authentication authentication,
            UUID announcementId,
            AnnouncementApprovalDecisionRequest request
    );
}
