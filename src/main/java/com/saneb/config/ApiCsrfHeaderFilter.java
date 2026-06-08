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

    private static final String API_PREFIX = "/api/v1/";
    private static final String PAYMENT_WEBHOOK_PREFIX = "/api/v1/payment-webhooks/";
    private static final String SESSION_COOKIE_NAME = "JSESSIONID";
    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final ObjectMapper objectMapper;

    public ApiCsrfHeaderFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

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

    private boolean requiresCsrfHeader(HttpServletRequest request) {
        if (SAFE_METHODS.contains(request.getMethod())) {
            return false;
        }

        String path = normalizedPath(request);
        if (!path.startsWith(API_PREFIX) || path.startsWith(PAYMENT_WEBHOOK_PREFIX)) {
            return false;
        }

        return StringUtils.hasText(selectCookieValue(request, SESSION_COOKIE_NAME));
    }

    private String normalizedPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && path.startsWith(contextPath)) {
            return path.substring(contextPath.length());
        }
        return path;
    }

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
