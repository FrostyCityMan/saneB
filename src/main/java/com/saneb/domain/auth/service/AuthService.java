/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AuthService.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.auth.service;

import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.dto.LoginRequest;
import com.saneb.domain.auth.dto.LoginResponse;
import com.saneb.domain.auth.dto.PasswordChangeRequest;
import com.saneb.domain.auth.dto.SignupRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;

public interface AuthService {

    /**
     * 업무 처리를 수행합니다.
     *
     * @param request 입력 값
     *
     * @param httpRequest 입력 값
     *
     * @param httpResponse 입력 값
     *
     * @return 처리 결과
     */
    LoginResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    /**
     * 업무 처리를 수행합니다.
     *
     * @param request 입력 값
     *
     * @param httpRequest 입력 값
     *
     * @param httpResponse 입력 값
     *
     * @return 처리 결과
     */
    LoginResponse signup(SignupRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    /**
     * 업무 처리를 수행합니다.
     *
     * @param httpRequest 입력 값
     *
     * @param httpResponse 입력 값
     */
    void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @return 처리 결과
     */
    AuthMeResponse selectAuthMe(Authentication authentication);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param request 입력 값
     *
     * @param httpRequest 입력 값
     *
     * @param httpResponse 입력 값
     */
    void updatePassword(
            Authentication authentication,
            PasswordChangeRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    );
}
