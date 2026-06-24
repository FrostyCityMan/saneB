/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AiAssistService.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.aiassist.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.aiassist.dto.AiAssistCreateRequest;
import com.saneb.domain.aiassist.dto.AiAssistResponse;
import com.saneb.domain.aiassist.dto.AiAssistReviewRequest;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AiAssistService {

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    AiAssistResponse insertAiAssistRequest(Authentication authentication, AiAssistCreateRequest request);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @param assistTypeCode 입력 값
     *
     * @param resourceType 입력 값
     *
     * @param reviewStatusCode 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    PageResponse<AiAssistResponse> selectAiAssistRequestList(
            Authentication authentication,
            String assistTypeCode,
            String resourceType,
            String reviewStatusCode,
            int page,
            int size
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @param requestId 입력 값
     *
     * @return 처리 결과
     */
    AiAssistResponse selectAiAssistRequestDetails(Authentication authentication, UUID requestId);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param resultId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    AiAssistResponse updateAiAssistResultReview(Authentication authentication, UUID resultId, AiAssistReviewRequest request);
}
