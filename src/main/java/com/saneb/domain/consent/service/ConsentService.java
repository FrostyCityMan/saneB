/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ConsentService.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.consent.service;

import com.saneb.domain.consent.dto.ConsentSaveRequest;
import com.saneb.domain.consent.dto.CurrentConsentResponse;
import com.saneb.domain.consent.dto.UserConsentResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface ConsentService {

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    List<CurrentConsentResponse> selectCurrentConsentList();

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @return 처리 결과
     */
    List<UserConsentResponse> selectMyConsentList(Authentication authentication);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param request 입력 값
     *
     * @param httpRequest 입력 값
     *
     * @return 처리 결과
     */
    UserConsentResponse insertMyConsent(
            Authentication authentication,
            ConsentSaveRequest request,
            HttpServletRequest httpRequest
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param userId 입력 값
     *
     * @param httpRequest 입력 값
     */
    void insertSignupRequiredConsents(UUID userId, HttpServletRequest httpRequest);
}
