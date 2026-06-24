/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AppLogControllerSmokeTest.java
 * 작성자: 김도훈
 *
 */

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

    /**
     * 업무 처리를 수행합니다.
     */
    @BeforeEach
    void setUp() {
        when(appLogService.selectAppLog(any(), any(), anyInt())).thenReturn(sampleResponse());
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
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

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void selectAppLogRejectsUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/app-logs")
                        .with(user(principal(List.of("USER")))))
                .andExpect(status().isForbidden());
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
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

    /**
     * 업무 처리를 수행합니다.
     *
     * @param roles 입력 값
     *
     * @return 처리 결과
     */
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
