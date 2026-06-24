/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: MatchingService.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.matching.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.matching.dto.MatchingCandidateGenerateRequest;
import com.saneb.domain.matching.dto.MatchingCandidateGenerateResponse;
import com.saneb.domain.matching.dto.MatchingCaseCreateRequest;
import com.saneb.domain.matching.dto.MatchingCaseDetailsResponse;
import com.saneb.domain.matching.dto.MatchingCaseStatusUpdateRequest;
import com.saneb.domain.matching.dto.MatchingCaseSummaryResponse;
import com.saneb.domain.matching.dto.MatchingFinalRecalculateRequest;
import com.saneb.domain.matching.dto.MatchingMemberLookupResponse;
import com.saneb.domain.matching.dto.MatchingResultDetailResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface MatchingService {

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    MatchingCaseDetailsResponse insertMatchingCase(
            Authentication authentication,
            MatchingCaseCreateRequest request
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
    MatchingCandidateGenerateResponse insertMatchingCandidates(
            Authentication authentication,
            MatchingCandidateGenerateRequest request
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param actorUserId 입력 값
     *
     * @param memberUserId 입력 값
     *
     * @return 처리 결과
     */
    MatchingCandidateGenerateResponse insertBasicMatchingCandidates(
            UUID actorUserId,
            UUID memberUserId
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
    MatchingCandidateGenerateResponse insertFinalMatchingCandidates(
            Authentication authentication,
            MatchingFinalRecalculateRequest request
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @param memberUserId 입력 값
     *
     * @param verificationId 입력 값
     *
     * @param statusCode 입력 값
     *
     * @param matchingStageCode 입력 값
     *
     * @param matchingBasisCode 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    PageResponse<MatchingCaseSummaryResponse> selectMatchingCaseList(
            UUID announcementId,
            UUID memberUserId,
            UUID verificationId,
            String statusCode,
            String matchingStageCode,
            String matchingBasisCode,
            int page,
            int size
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    PageResponse<MatchingCaseSummaryResponse> selectMyBasicMatchingCaseList(
            Authentication authentication,
            int page,
            int size
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @param memberUserId 입력 값
     *
     * @param statusCode 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    PageResponse<MatchingCaseSummaryResponse> selectFinalMatchingCaseList(
            UUID announcementId,
            UUID memberUserId,
            String statusCode,
            int page,
            int size
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param keyword 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    PageResponse<MatchingMemberLookupResponse> selectMatchingMemberLookupList(
            String keyword,
            int page,
            int size
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param matchingCaseId 입력 값
     *
     * @return 처리 결과
     */
    MatchingCaseDetailsResponse selectMatchingCaseDetails(UUID matchingCaseId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param matchingCaseId 입력 값
     *
     * @return 처리 결과
     */
    List<MatchingResultDetailResponse> selectMatchingResultDetailList(UUID matchingCaseId);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param matchingCaseId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    MatchingCaseDetailsResponse updateMatchingCaseStatus(
            Authentication authentication,
            UUID matchingCaseId,
            MatchingCaseStatusUpdateRequest request
    );
}
