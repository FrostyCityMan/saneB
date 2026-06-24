/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ApplicationProgressService.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.applicationprogress.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.applicationprogress.dto.ApplicationProgressDetailsResponse;
import com.saneb.domain.applicationprogress.dto.ApplicationProgressStartRequest;
import com.saneb.domain.applicationprogress.dto.ApplicationProgressSummaryResponse;
import com.saneb.domain.applicationprogress.dto.ProgressActionRequest;
import com.saneb.domain.applicationprogress.dto.ProgressChecklistSaveRequest;
import com.saneb.domain.applicationprogress.dto.ProgressReceiptSaveRequest;
import com.saneb.domain.applicationprogress.dto.ProgressResultSaveRequest;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface ApplicationProgressService {

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    ApplicationProgressDetailsResponse insertApplicationProgress(
            Authentication authentication,
            ApplicationProgressStartRequest request
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @param announcementId 입력 값
     *
     * @param memberUserId 입력 값
     *
     * @param matchingCaseId 입력 값
     *
     * @param statusCode 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    PageResponse<ApplicationProgressSummaryResponse> selectApplicationProgressList(
            Authentication authentication,
            UUID announcementId,
            UUID memberUserId,
            UUID matchingCaseId,
            String statusCode,
            int page,
            int size
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param progressId 입력 값
     *
     * @return 처리 결과
     */
    ApplicationProgressDetailsResponse selectApplicationProgressDetails(UUID progressId);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param progressId 입력 값
     *
     * @param stepId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    ApplicationProgressDetailsResponse updateProgressStepAction(
            Authentication authentication,
            UUID progressId,
            UUID stepId,
            ProgressActionRequest request
    );

    /**
     * 업무 데이터를 저장합니다.
     *
     * @param authentication 입력 값
     *
     * @param progressId 입력 값
     *
     * @param stepId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    ApplicationProgressDetailsResponse saveProgressStepDocuments(
            Authentication authentication,
            UUID progressId,
            UUID stepId,
            ProgressChecklistSaveRequest request
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param progressId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    ApplicationProgressDetailsResponse updateProgressReceipt(
            Authentication authentication,
            UUID progressId,
            ProgressReceiptSaveRequest request
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param progressId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    ApplicationProgressDetailsResponse updateProgressResult(
            Authentication authentication,
            UUID progressId,
            ProgressResultSaveRequest request
    );
}
