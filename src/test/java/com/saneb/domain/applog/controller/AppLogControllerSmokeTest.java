package com.saneb.domain.applog.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.domain.applog.dto.AppLogResponse;
import com.saneb.domain.applog.dto.AppLogResponse.AppLogLineResponse;
import com.saneb.domain.applog.service.AppLogService;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class AppLogControllerSmokeTest {

    private static final UUID ADMIN_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppLogService appLogService;

    @BeforeEach
    void setUp() {
        when(appLogService.selectAppLog(any(), any(), anyInt())).thenReturn(sampleResponse());
    }

    @Test
    void selectAppLogReturnsApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/admin/app-logs")
                        .param("levelCode", "ERROR")
                        .param("keyword", "payment")
                        .param("lines", "80")
                        .with(user(principal(List.of("ADMIN")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.lines[0].content").value("2026-06-08 ERROR sample"));
    }

    @Test
    void selectAppLogRejectsUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/app-logs")
                        .with(user(principal(List.of("USER")))))
                .andExpect(status().isForbidden());
    }

    static AppLogResponse sampleResponse() {
        return new AppLogResponse(
                "/home/ubuntu/app/app.log",
                true,
                1024,
                OffsetDateTime.parse("2026-06-08T10:00:00+09:00"),
                80,
                1,
                "ERROR",
                "payment",
                "최근 로그를 조회했습니다.",
                List.of(new AppLogLineResponse(1, "2026-06-08 ERROR sample"))
        );
    }

    private AuthenticatedUserDetails principal(List<String> roles) {
        return new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        ADMIN_ID,
                        "admin",
                        "password-hash",
                        "Admin User",
                        "ACTIVE",
                        false,
                        null,
                        null,
                        null
                ),
                roles
        );
    }
}
