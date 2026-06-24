/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ApiRateLimitFilterTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiRateLimitFilterTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            /**
             * 업무 처리를 수행합니다.
             *
             * @param 20260608T000000Z 입력 값
             *
             * @param ZoneOffsetUTC 입력 값
             *
             * @return 처리 결과
             */
            Instant.parse("2026-06-08T00:00:00Z"),
            ZoneOffset.UTC
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void apiRequestReturnsTooManyRequestsWhenLimitExceeded() throws Exception {
        ApiRateLimitFilter filter = new ApiRateLimitFilter(objectMapper, true, 1, 60, FIXED_CLOCK);

        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(apiRequest("/api/v1/auth/me"), firstResponse, new MockFilterChain());

        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(apiRequest("/api/v1/auth/me"), secondResponse, new MockFilterChain());

        assertThat(firstResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(secondResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(secondResponse.getHeader("X-Rate-Limit-Limit")).isEqualTo("1");
        assertThat(secondResponse.getHeader("X-Rate-Limit-Remaining")).isEqualTo("0");
        assertThat(secondResponse.getHeader("Retry-After")).isEqualTo("60");
        assertThat(secondResponse.getContentAsString(StandardCharsets.UTF_8))
                .contains("\"success\":false")
                .contains("RATE_LIMIT_EXCEEDED")
                .contains("요청이 너무 많습니다. 잠시 후 다시 시도하세요.");
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void nonApiRequestIsNotRateLimited() throws Exception {
        ApiRateLimitFilter filter = new ApiRateLimitFilter(objectMapper, true, 1, 60, FIXED_CLOCK);

        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(request("/app/dashboard"), firstResponse, new MockFilterChain());

        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(request("/app/dashboard"), secondResponse, new MockFilterChain());

        assertThat(firstResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(secondResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(secondResponse.getHeader("X-Rate-Limit-Limit")).isNull();
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void disabledRateLimitDoesNotBlockApiRequest() throws Exception {
        ApiRateLimitFilter filter = new ApiRateLimitFilter(objectMapper, false, 1, 60, FIXED_CLOCK);

        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(apiRequest("/api/v1/auth/me"), firstResponse, new MockFilterChain());

        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(apiRequest("/api/v1/auth/me"), secondResponse, new MockFilterChain());

        assertThat(firstResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(secondResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(secondResponse.getHeader("X-Rate-Limit-Limit")).isNull();
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param uri 입력 값
     *
     * @return 처리 결과
     */
    private MockHttpServletRequest apiRequest(String uri) {
        return request(uri);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param uri 입력 값
     *
     * @return 처리 결과
     */
    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        request.setRemoteAddr("203.0.113.10");
        return request;
    }
}
