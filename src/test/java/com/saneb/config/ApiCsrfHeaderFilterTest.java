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

    @Test
    void apiMutationWithoutBrowserSessionCookiePassesForServerAndTestClients() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(apiPost("/api/v1/application-progresses"), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

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

    private MockHttpServletRequest apiPost(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRequestURI(uri);
        return request;
    }

    private MockHttpServletRequest apiPost(String uri, Cookie... cookies) {
        MockHttpServletRequest request = apiPost(uri);
        request.setCookies(cookies);
        return request;
    }
}
