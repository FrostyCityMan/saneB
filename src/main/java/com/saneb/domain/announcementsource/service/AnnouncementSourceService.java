/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceService.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionApprovalRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionRequestCreateRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionRequestResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionRunDetailsResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionRunResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceDetailsResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceDuplicateDecisionRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceLinkResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceReviewStatusUpdateRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceSummaryResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceToAnnouncementRequest;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementSourceService {

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementSourceCollectionRequestResponse insertCollectionRequest(
            Authentication authentication,
            AnnouncementSourceCollectionRequestCreateRequest request
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param providerCode 입력 값
     *
     * @param maxCount 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementSourceCollectionRequestResponse insertBatchCollectionRequest(String providerCode, Integer maxCount);

    /**
     * 단일 지자체 URL의 수동 수집 승인 요청을 등록합니다.
     *
     * @param authentication 인증 정보
     * @param localGovernmentSourceId 지자체 URL 식별자
     * @param maxCount 최대 수집 건수
     * @param requestNote 요청 메모
     * @return 수집 승인 요청
     */
    AnnouncementSourceCollectionRequestResponse insertLocalGovernmentCollectionRequest(
            Authentication authentication,
            UUID localGovernmentSourceId,
            Integer maxCount,
            String requestNote
    );

    /**
     * 사전 승인된 스케줄 근거로 자동 실행용 요청을 등록합니다.
     *
     * @param scheduleId 승인 스케줄 식별자
     * @param maxCount 최대 수집 URL 수
     * @return 승인된 수집 요청
     */
    AnnouncementSourceCollectionRequestResponse insertApprovedScheduledCollectionRequest(
            UUID scheduleId,
            Integer maxCount
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param providerCode 입력 값
     *
     * @param requestTypeCode 입력 값
     *
     * @param requestStatusCode 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    PageResponse<AnnouncementSourceCollectionRequestResponse> selectCollectionRequestList(
            String providerCode,
            String requestTypeCode,
            String requestStatusCode,
            int page,
            int size
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param requestId 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementSourceCollectionRequestResponse selectCollectionRequestDetails(UUID requestId);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param requestId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementSourceCollectionRequestResponse updateCollectionRequestApproval(
            Authentication authentication,
            UUID requestId,
            AnnouncementSourceCollectionApprovalRequest request
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param requestId 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementSourceCollectionRunResponse insertCollectionRun(UUID requestId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param requestId 입력 값
     *
     * @param runStatusCode 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    PageResponse<AnnouncementSourceCollectionRunResponse> selectCollectionRunList(
            UUID requestId,
            String runStatusCode,
            int page,
            int size
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param runId 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementSourceCollectionRunDetailsResponse selectCollectionRunDetails(UUID runId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param providerCode 입력 값
     *
     * @param reviewStatusCode 입력 값
     *
     * @param keyword 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    PageResponse<AnnouncementSourceSummaryResponse> selectSourceList(
            String providerCode,
            String reviewStatusCode,
            String keyword,
            int page,
            int size
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param sourceId 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementSourceDetailsResponse selectSourceDetails(UUID sourceId);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param sourceId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementSourceDetailsResponse updateSourceReviewStatus(
            Authentication authentication,
            UUID sourceId,
            AnnouncementSourceReviewStatusUpdateRequest request
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param sourceId 입력 값
     *
     * @param candidateId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementSourceDetailsResponse updateDuplicateCandidateDecision(
            Authentication authentication,
            UUID sourceId,
            UUID candidateId,
            AnnouncementSourceDuplicateDecisionRequest request
    );

    /**
     * 교차 제공자 유사 후보의 운영자 결정을 저장합니다.
     *
     * @param authentication 인증 정보
     * @param sourceId 원문 식별자
     * @param duplicateId 교차 중복 관계 식별자
     * @param request 결정 요청
     * @return 원문 상세
     */
    AnnouncementSourceDetailsResponse updateSnapshotDuplicateDecision(
            Authentication authentication,
            UUID sourceId,
            UUID duplicateId,
            AnnouncementSourceDuplicateDecisionRequest request
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param sourceId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementSourceLinkResponse insertOperationalAnnouncement(
            Authentication authentication,
            UUID sourceId,
            AnnouncementSourceToAnnouncementRequest request
    );
}
