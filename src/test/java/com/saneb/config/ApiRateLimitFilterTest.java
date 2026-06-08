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
            Instant.parse("2026-06-08T00:00:00Z"),
            ZoneOffset.UTC
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

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

    private MockHttpServletRequest apiRequest(String uri) {
        return request(uri);
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        request.setRemoteAddr("203.0.113.10");
        return request;
    }
}
