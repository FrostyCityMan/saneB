/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AdminBootstrapSmokeIntegrationTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@EnabledIfEnvironmentVariable(named = "SANEB_ADMIN_BOOTSTRAP_SMOKE", matches = "true")
@ActiveProfiles("local")
@SpringBootTest(properties = {
        "saneb.bootstrap.admin.enabled=true",
        "saneb.bootstrap.admin.login-id=bootstrap_admin_smoke",
        "saneb.bootstrap.admin.password=BootstrapAdmin!234",
        "saneb.bootstrap.admin.name=Bootstrap Admin Smoke"
})
@AutoConfigureMockMvc
class AdminBootstrapSmokeIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void bootstrapCreatesAdminWhenActiveAdminDoesNotExist() throws Exception {
        Integer adminCount = jdbcTemplate.queryForObject(
                """
                        SELECT
                            CAST(count(1) AS integer)
                        FROM users u
                        INNER JOIN user_roles ur ON ur.user_id = u.id
                        WHERE u.login_id = ?
                          AND u.status_code = 'ACTIVE'
                          AND u.password_reset_required = true
                          AND ur.role_code = 'ADMIN'
                        """,
                Integer.class,
                "bootstrap_admin_smoke"
        );

        assertThat(adminCount).isEqualTo(1);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "bootstrap_admin_smoke",
                                  "password": "BootstrapAdmin!234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.primaryRole").value("ADMIN"))
                .andExpect(jsonPath("$.data.passwordResetRequired").value(true));
    }
}
