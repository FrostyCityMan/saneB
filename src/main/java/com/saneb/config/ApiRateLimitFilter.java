/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ApiRateLimitFilter.java
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> API_PREFIXES = Set.of("/api/v1/", "/api/v2/");
    private static final int MIN_MAX_REQUESTS = 1;
    private static final long MIN_WINDOW_SECONDS = 1L;
    private static final long MILLIS_PER_SECOND = 1000L;

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int maxRequests;
    private final long windowMillis;
    private final Clock clock;
    private final ConcurrentHashMap<String, RateWindow> windows = new ConcurrentHashMap<>();
    private volatile long lastCleanupMillis;

    /**
     * 객체를 생성합니다.
     *
     * @param objectMapper 입력 값
     *
     * @param environment 입력 값
     */
    @Autowired
    public ApiRateLimitFilter(ObjectMapper objectMapper, Environment environment) {
        this(
                objectMapper,
                environment.getProperty("saneb.security.rate-limit.enabled", Boolean.class, true),
                environment.getProperty("saneb.security.rate-limit.max-requests", Integer.class, 600),
                environment.getProperty("saneb.security.rate-limit.window-seconds", Long.class, 60L),
                Clock.systemUTC()
        );
    }

    /**
     * 객체를 생성합니다.
     *
     * @param objectMapper 입력 값
     *
     * @param enabled 입력 값
     *
     * @param maxRequests 입력 값
     *
     * @param windowSeconds 입력 값
     *
     * @param clock 입력 값
     */
    ApiRateLimitFilter(
            ObjectMapper objectMapper,
            boolean enabled,
            int maxRequests,
            long windowSeconds,
            Clock clock
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.maxRequests = Math.max(MIN_MAX_REQUESTS, maxRequests);
        this.windowMillis = Math.max(MIN_WINDOW_SECONDS, windowSeconds) * MILLIS_PER_SECOND;
        this.clock = clock;
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
        if (!enabled || !isVersionedApiRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        long nowMillis = clock.millis();
        cleanupExpiredWindows(nowMillis);
        RateDecision decision = recordRequest(resolveLimitKey(request), nowMillis);
        writeRateLimitHeaders(response, decision);

        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        writeRateLimitResponse(response, decision);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param key 입력 값
     *
     * @param nowMillis 입력 값
     *
     * @return 처리 결과
     */
    private RateDecision recordRequest(String key, long nowMillis) {
        RateWindow window = windows.compute(key, (ignored, current) -> {
            if (current == null || nowMillis - current.windowStartMillis() >= windowMillis) {
                return new RateWindow(nowMillis, 1);
            }
            return current.incremented();
        });

        int remaining = Math.max(0, maxRequests - window.count());
        long retryAfterMillis = Math.max(0, windowMillis - (nowMillis - window.windowStartMillis()));
        return new RateDecision(window.count() <= maxRequests, remaining, retryAfterMillis);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param nowMillis 입력 값
     */
    private void cleanupExpiredWindows(long nowMillis) {
        if (nowMillis - lastCleanupMillis < windowMillis) {
            return;
        }
        lastCleanupMillis = nowMillis;
        windows.entrySet().removeIf(entry ->
                nowMillis - entry.getValue().windowStartMillis() >= windowMillis * 2
        );
    }

    /**
     * 조건 충족 여부를 확인합니다.
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    private boolean isVersionedApiRequest(HttpServletRequest request) {
        String path = normalizedPath(request);
        return API_PREFIXES.stream().anyMatch(path::startsWith);
    }

    /**
     * 업무 처리에 필요한 값을 해석합니다.
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    private String resolveLimitKey(HttpServletRequest request) {
        return resolveClientIp(request) + "|" + request.getMethod() + "|" + normalizedPath(request);
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
     * 업무 처리에 필요한 값을 해석합니다.
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 응답 데이터를 작성합니다.
     *
     * @param response 입력 값
     *
     * @param decision 입력 값
     */
    private void writeRateLimitHeaders(HttpServletResponse response, RateDecision decision) {
        response.setHeader("X-Rate-Limit-Limit", String.valueOf(maxRequests));
        response.setHeader("X-Rate-Limit-Remaining", String.valueOf(decision.remaining()));
        if (!decision.allowed()) {
            response.setHeader("Retry-After", String.valueOf(toRetryAfterSeconds(decision.retryAfterMillis())));
        }
    }

    /**
     * 응답 데이터를 작성합니다.
     *
     * @param response 입력 값
     *
     * @param decision 입력 값
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private void writeRateLimitResponse(HttpServletResponse response, RateDecision decision) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(toRetryAfterSeconds(decision.retryAfterMillis())));
        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.failure(
                        ErrorResponse.of(ErrorCode.RATE_LIMIT_EXCEEDED),
                        "요청이 너무 많습니다. 잠시 후 다시 시도하세요."
                )
        );
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param retryAfterMillis 입력 값
     *
     * @return 처리 결과
     */
    private long toRetryAfterSeconds(long retryAfterMillis) {
        return Math.max(1L, (retryAfterMillis + MILLIS_PER_SECOND - 1L) / MILLIS_PER_SECOND);
    }

    private record RateWindow(long windowStartMillis, int count) {

        /**
         * 업무 처리를 수행합니다.
         *
         * @return 처리 결과
         */
        private RateWindow incremented() {
            return new RateWindow(windowStartMillis, count + 1);
        }
    }

    private record RateDecision(boolean allowed, int remaining, long retryAfterMillis) {
    }
}
