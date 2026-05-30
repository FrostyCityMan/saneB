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
