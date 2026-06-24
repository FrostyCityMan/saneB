/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ApiAccessDeniedHandler.java
 * 작성자: 김도훈
 *
 */

package com.saneb.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.error.ErrorResponse;
import com.saneb.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /**
     * 객체를 생성합니다.
     *
     * @param objectMapper 입력 값
     */
    public ApiAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 업무 흐름을 처리합니다.
     *
     * @param request 입력 값
     *
     * @param response 입력 값
     *
     * @param accessDeniedException 입력 값
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        if (acceptsHtml(request)) {
            response.sendRedirect(request.getContextPath() + "/invalid-access?reason=forbidden");
            return;
        }
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.failure(ErrorResponse.of(ErrorCode.AUTH_FORBIDDEN), "권한이 없습니다.")
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    private boolean acceptsHtml(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.TEXT_HTML_VALUE);
    }
}
