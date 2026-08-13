/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ApiCsrfHeaderFilter.java
 * 작성자: 김도훈
 *
 */

package com.saneb.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.error.ErrorResponse;
import com.saneb.common.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiCsrfHeaderFilter extends OncePerRequestFilter {

    private static final Set<String> API_PREFIXES = Set.of("/api/v1/", "/api/v2/");
    private static final String PAYMENT_WEBHOOK_PREFIX = "/api/v1/payment-webhooks/";
    private static final String SESSION_COOKIE_NAME = "JSESSIONID";
    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final ObjectMapper objectMapper;

    /**
     * 객체를 생성합니다.
     *
     * @param objectMapper 입력 값
     */
    public ApiCsrfHeaderFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param request 입력 값
     *
     * @param response 입력 값
     *
     * @param filterChain 입력 값
     *
     * @throws ServletException 처리 중 예외가 발생한 경우
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!requiresCsrfHeader(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String csrfCookie = selectCookieValue(request, CSRF_COOKIE_NAME);
        String csrfHeader = request.getHeader(CSRF_HEADER_NAME);
        if (StringUtils.hasText(csrfCookie) && csrfCookie.equals(csrfHeader)) {
            filterChain.doFilter(request, response);
            return;
        }

        writeForbiddenResponse(response);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    private boolean requiresCsrfHeader(HttpServletRequest request) {
        if (SAFE_METHODS.contains(request.getMethod())) {
            return false;
        }

        String path = normalizedPath(request);
        if (API_PREFIXES.stream().noneMatch(path::startsWith) || path.startsWith(PAYMENT_WEBHOOK_PREFIX)) {
            return false;
        }

        return StringUtils.hasText(selectCookieValue(request, SESSION_COOKIE_NAME));
    }

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    private String normalizedPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && path.startsWith(contextPath)) {
            return path.substring(contextPath.length());
        }
        return path;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param request 입력 값
     *
     * @param cookieName 입력 값
     *
     * @return 처리 결과
     */
    private String selectCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return "";
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return "";
    }

    /**
     * 응답 데이터를 작성합니다.
     *
     * @param response 입력 값
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private void writeForbiddenResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.failure(
                        ErrorResponse.of(ErrorCode.CSRF_TOKEN_INVALID),
                        "보안 확인 값이 올바르지 않습니다. 화면을 새로고침한 뒤 다시 시도하세요."
                )
        );
    }
}
