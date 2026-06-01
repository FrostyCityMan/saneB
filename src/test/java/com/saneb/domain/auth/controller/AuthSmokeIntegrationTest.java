package com.saneb.domain.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.http.MediaType;

@EnabledIfEnvironmentVariable(named = "SANEB_AUTH_SMOKE", matches = "true")
@ActiveProfiles("local")
@SpringBootTest
@AutoConfigureMockMvc
class AuthSmokeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void localSeedAccountCompletesSessionAuthFlow() throws Exception {
        String signupLoginId = "signup_" + Long.toString(System.nanoTime(), 36);
        String signupPhone = "010-" + Long.toString(System.nanoTime()).substring(0, 4) + "-7788";
        long beforeSuccessCount = selectLoginHistoryCount("SUCCESS");
        long beforeFailCount = selectLoginHistoryCount("FAIL");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "local_user",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loginId").value("local_user"))
                .andExpect(jsonPath("$.data.primaryRole").value("USER"))
                .andExpect(jsonPath("$.data.defaultRoute").value("/app/dashboard"))
                .andReturn();

        HttpSession httpSession = loginResult.getRequest().getSession(false);
        assertThat(httpSession).isInstanceOf(MockHttpSession.class);
        MockHttpSession session = (MockHttpSession) httpSession;

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loginId").value("local_user"))
                .andExpect(jsonPath("$.data.defaultRoute").value("/app/dashboard"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "local_user",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.errorCode").value("AUTH_INVALID_CREDENTIALS"));

        mockMvc.perform(patch("/api/v1/auth/password")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "password",
                                  "newPassword": "new-password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/v1/auth/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        assertThat(session.isInvalid()).isTrue();

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.errorCode").value("AUTH_REQUIRED"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "local_user",
                                  "password": "new-password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.defaultRoute").value("/app/dashboard"));

        MvcResult signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "%s",
                                  "password": "signup-password",
                                  "passwordConfirm": "signup-password",
                                  "name": "가입 테스트",
                                  "phone": "%s",
                                  "email": "%s@example.com",
                                  "termsAgreed": true,
                                  "privacyAgreed": true
                                }
                                """.formatted(signupLoginId, signupPhone, signupLoginId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loginId").value(signupLoginId))
                .andExpect(jsonPath("$.data.primaryRole").value("USER"))
                .andExpect(jsonPath("$.data.defaultRoute").value("/app/dashboard"))
                .andReturn();

        HttpSession signupSession = signupResult.getRequest().getSession(false);
        assertThat(signupSession).isInstanceOf(MockHttpSession.class);
        mockMvc.perform(get("/api/v1/auth/me").session((MockHttpSession) signupSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loginId").value(signupLoginId));
        assertThat(selectUserRoleCount(signupLoginId, "USER")).isEqualTo(1);

        assertThat(selectLoginHistoryCount("SUCCESS")).isEqualTo(beforeSuccessCount + 2);
        assertThat(selectLoginHistoryCount("FAIL")).isEqualTo(beforeFailCount + 1);
    }

    private long selectLoginHistoryCount(String resultCode) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM auth_login_histories
                        WHERE login_id = ?
                          AND login_result_code = ?
                        """,
                Long.class,
                "local_user",
                resultCode
        );
        return count == null ? 0 : count;
    }

    private long selectUserRoleCount(String loginId, String roleCode) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM users u
                        INNER JOIN user_roles ur ON ur.user_id = u.id
                        WHERE u.login_id = ?
                          AND ur.role_code = ?
                        """,
                Long.class,
                loginId,
                roleCode
        );
        return count == null ? 0 : count;
    }
}
