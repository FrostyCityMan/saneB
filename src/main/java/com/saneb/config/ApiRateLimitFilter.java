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

    private static final String API_PREFIX = "/api/v1/";
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

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!enabled || !isApiV1Request(request)) {
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

    private void cleanupExpiredWindows(long nowMillis) {
        if (nowMillis - lastCleanupMillis < windowMillis) {
            return;
        }
        lastCleanupMillis = nowMillis;
        windows.entrySet().removeIf(entry ->
                nowMillis - entry.getValue().windowStartMillis() >= windowMillis * 2
        );
    }

    private boolean isApiV1Request(HttpServletRequest request) {
        return normalizedPath(request).startsWith(API_PREFIX);
    }

    private String resolveLimitKey(HttpServletRequest request) {
        return resolveClientIp(request) + "|" + request.getMethod() + "|" + normalizedPath(request);
    }

    private String normalizedPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && path.startsWith(contextPath)) {
            return path.substring(contextPath.length());
        }
        return path;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeRateLimitHeaders(HttpServletResponse response, RateDecision decision) {
        response.setHeader("X-Rate-Limit-Limit", String.valueOf(maxRequests));
        response.setHeader("X-Rate-Limit-Remaining", String.valueOf(decision.remaining()));
        if (!decision.allowed()) {
            response.setHeader("Retry-After", String.valueOf(toRetryAfterSeconds(decision.retryAfterMillis())));
        }
    }

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

    private long toRetryAfterSeconds(long retryAfterMillis) {
        return Math.max(1L, (retryAfterMillis + MILLIS_PER_SECOND - 1L) / MILLIS_PER_SECOND);
    }

    private record RateWindow(long windowStartMillis, int count) {

        private RateWindow incremented() {
            return new RateWindow(windowStartMillis, count + 1);
        }
    }

    private record RateDecision(boolean allowed, int remaining, long retryAfterMillis) {
    }
}
