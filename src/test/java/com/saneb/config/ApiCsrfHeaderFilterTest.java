/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ApiCsrfHeaderFilterTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiCsrfHeaderFilterTest {

    private final ApiCsrfHeaderFilter filter = new ApiCsrfHeaderFilter(new ObjectMapper());

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void authenticatedBrowserApiMutationRequiresMatchingCsrfHeader() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(apiPost(
                        "/api/v1/application-progresses",
                        new Cookie("JSESSIONID", "session-id"),
                        new Cookie("XSRF-TOKEN", "csrf-token")
                ),
                response,
                new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .contains("\"success\":false")
                .contains("CSRF_TOKEN_INVALID")
                .contains("화면을 새로고침한 뒤 다시 시도하세요.");
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void authenticatedBrowserApiMutationPassesWithMatchingCsrfHeader() throws Exception {
        MockHttpServletRequest request = apiPost(
                "/api/v1/application-progresses",
                new Cookie("JSESSIONID", "session-id"),
                new Cookie("XSRF-TOKEN", "csrf-token")
        );
        request.addHeader("X-XSRF-TOKEN", "csrf-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    /**
     * V2 API도 브라우저 세션에서 동일한 raw cookie-header 검증을 적용하는지 확인합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void authenticatedBrowserV2ApiMutationUsesSameCsrfHeaderContract() throws Exception {
        MockHttpServletRequest request = apiPost(
                "/api/v2/announcements",
                new Cookie("JSESSIONID", "session-id"),
                new Cookie("XSRF-TOKEN", "csrf-token")
        );
        MockHttpServletResponse missingHeaderResponse = new MockHttpServletResponse();
        filter.doFilter(request, missingHeaderResponse, new MockFilterChain());

        MockHttpServletRequest matchingRequest = apiPost(
                "/api/v2/announcements",
                new Cookie("JSESSIONID", "session-id"),
                new Cookie("XSRF-TOKEN", "csrf-token")
        );
        matchingRequest.addHeader("X-XSRF-TOKEN", "csrf-token");
        MockHttpServletResponse matchingResponse = new MockHttpServletResponse();
        filter.doFilter(matchingRequest, matchingResponse, new MockFilterChain());

        assertThat(missingHeaderResponse.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(matchingResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void apiMutationWithoutBrowserSessionCookiePassesForServerAndTestClients() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(apiPost("/api/v1/application-progresses"), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void paymentWebhookIsExcludedFromBrowserCsrfHeaderRule() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(apiPost(
                        "/api/v1/payment-webhooks/MANUAL",
                        new Cookie("JSESSIONID", "session-id")
                ),
                response,
                new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param uri 입력 값
     *
     * @return 처리 결과
     */
    private MockHttpServletRequest apiPost(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRequestURI(uri);
        return request;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param uri 입력 값
     *
     * @param cookies 입력 값
     *
     * @return 처리 결과
     */
    private MockHttpServletRequest apiPost(String uri, Cookie... cookies) {
        MockHttpServletRequest request = apiPost(uri);
        request.setCookies(cookies);
        return request;
    }
}
