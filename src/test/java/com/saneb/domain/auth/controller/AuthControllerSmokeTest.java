package com.saneb.domain.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.domain.auth.dao.AuthDao;
import com.saneb.domain.auth.vo.AuthLoginHistoryCommand;
import com.saneb.domain.auth.vo.AuthPasswordUpdateCommand;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class AuthControllerSmokeTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private AuthDao authDao;

    @TestConfiguration
    static class AuthControllerSmokeTestConfig {

        @Bean
        @Primary
        PlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override
                protected Object doGetTransaction() {
                    return new Object();
                }

                @Override
                protected void doBegin(Object transaction, TransactionDefinition definition) {
                }

                @Override
                protected void doCommit(DefaultTransactionStatus status) {
                }

                @Override
                protected void doRollback(DefaultTransactionStatus status) {
                }
            };
        }
    }

    @Test
    void loginReturnsApiResponseAndWritesSuccessHistory() throws Exception {
        when(authDao.selectAuthUserDetailsByLoginId("user01"))
                .thenReturn(activeUser(passwordEncoder.encode("password")));
        when(authDao.selectRoleCodeListByUserId(USER_ID)).thenReturn(List.of("USER"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "user01",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.loginId").value("user01"))
                .andExpect(jsonPath("$.data.primaryRole").value("USER"))
                .andExpect(jsonPath("$.data.defaultRoute").value("/app/dashboard"));

        ArgumentCaptor<AuthLoginHistoryCommand> historyCaptor =
                ArgumentCaptor.forClass(AuthLoginHistoryCommand.class);
        verify(authDao).updateUserLastLoginAt(USER_ID);
        verify(authDao).insertAuthLoginHistory(historyCaptor.capture());
        assertThat(historyCaptor.getValue().loginResultCode()).isEqualTo("SUCCESS");
        assertThat(historyCaptor.getValue().failureReasonCode()).isNull();
    }

    @Test
    void loginFailureReturnsErrorCodeAndWritesFailHistory() throws Exception {
        when(authDao.selectAuthUserDetailsByLoginId("missing")).thenReturn(null);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "missing",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.errorCode").value("AUTH_INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("로그인 정보가 올바르지 않습니다."));

        ArgumentCaptor<AuthLoginHistoryCommand> historyCaptor =
                ArgumentCaptor.forClass(AuthLoginHistoryCommand.class);
        verify(authDao).insertAuthLoginHistory(historyCaptor.capture());
        verify(authDao, never()).updateUserLastLoginAt(any());
        assertThat(historyCaptor.getValue().loginResultCode()).isEqualTo("FAIL");
        assertThat(historyCaptor.getValue().failureReasonCode()).isEqualTo("USER_NOT_FOUND");
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void logoutReturnsApiResponse() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectAuthMeReturnsApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loginId").value("user01"))
                .andExpect(jsonPath("$.data.primaryRole").value("USER"))
                .andExpect(jsonPath("$.data.defaultRoute").value("/app/dashboard"));
    }

    @Test
    void selectAuthMeWithoutAuthenticationReturnsErrorCode() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.errorCode").value("AUTH_REQUIRED"));
    }

    @Test
    @WithMockUser(username = "admin01", roles = "ADMIN")
    void selectAuthMeReturnsDashboardDefaultRouteForAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.primaryRole").value("ADMIN"))
                .andExpect(jsonPath("$.data.defaultRoute").value("/app/dashboard"));
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void updatePasswordUpdatesBcryptHash() throws Exception {
        when(authDao.selectAuthUserDetailsByLoginId("user01"))
                .thenReturn(activeUser(passwordEncoder.encode("old-password")));
        when(authDao.selectRoleCodeListByUserId(USER_ID)).thenReturn(List.of("USER"));

        mockMvc.perform(patch("/api/v1/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "old-password",
                                  "newPassword": "new-password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<AuthPasswordUpdateCommand> passwordCaptor =
                ArgumentCaptor.forClass(AuthPasswordUpdateCommand.class);
        verify(authDao).updatePassword(passwordCaptor.capture());
        assertThat(passwordCaptor.getValue().userId()).isEqualTo(USER_ID);
        assertThat(passwordEncoder.matches("new-password", passwordCaptor.getValue().passwordHash())).isTrue();
    }

    private AuthUserDetailsRow activeUser(String passwordHash) {
        return new AuthUserDetailsRow(
                USER_ID,
                "user01",
                passwordHash,
                "사용자",
                "ACTIVE",
                false,
                null,
                null,
                null
        );
    }
}
