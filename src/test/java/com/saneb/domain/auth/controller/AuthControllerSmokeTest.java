/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AuthControllerSmokeTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.saneb.domain.auth.vo.AuthSignupCommand;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.consent.service.ConsentService;
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

    @MockBean
    private ConsentService consentService;

    @TestConfiguration
    static class AuthControllerSmokeTestConfig {

        /**
         * 업무 처리를 수행합니다.
         *
         * @return 처리 결과
         */
        @Bean
        /**
         * 업무 처리를 수행합니다.
         *
         * @return 처리 결과
         */
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

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
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

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void loginReturnsOperatorDashboardDefaultRoute() throws Exception {
        when(authDao.selectAuthUserDetailsByLoginId("operator01"))
                .thenReturn(activeUser(passwordEncoder.encode("password")));
        when(authDao.selectRoleCodeListByUserId(USER_ID)).thenReturn(List.of("OPERATOR"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "operator01",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.primaryRole").value("OPERATOR"))
                .andExpect(jsonPath("$.data.defaultRoute").value("/app/operator/dashboard"));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void signupCreatesUserRoleAndSession() throws Exception {
        when(authDao.selectAuthUserDetailsByLoginId("newuser")).thenReturn(null);
        when(authDao.selectUserIdByPhone("010-1000-2000")).thenReturn(null);
        when(authDao.insertUser(any())).thenReturn(USER_ID);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "newuser",
                                  "password": "new-password",
                                  "passwordConfirm": "new-password",
                                  "name": "신규 사용자",
                                  "phone": "010-1000-2000",
                                  "email": "newuser@example.com",
                                  "termsAgreed": true,
                                  "privacyAgreed": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.loginId").value("newuser"))
                .andExpect(jsonPath("$.data.primaryRole").value("USER"))
                .andExpect(jsonPath("$.data.defaultRoute").value("/app/dashboard"));

        ArgumentCaptor<AuthSignupCommand> signupCaptor = ArgumentCaptor.forClass(AuthSignupCommand.class);
        verify(authDao).insertUser(signupCaptor.capture());
        verify(authDao).insertUserRole(USER_ID, "USER");
        verify(consentService).insertSignupRequiredConsents(eq(USER_ID), any());
        verify(authDao).updateUserLastLoginAt(USER_ID);
        assertThat(signupCaptor.getValue().loginId()).isEqualTo("newuser");
        assertThat(signupCaptor.getValue().name()).isEqualTo("신규 사용자");
        assertThat(signupCaptor.getValue().phone()).isEqualTo("010-1000-2000");
        assertThat(signupCaptor.getValue().email()).isEqualTo("newuser@example.com");
        assertThat(passwordEncoder.matches("new-password", signupCaptor.getValue().passwordHash())).isTrue();
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void signupRejectsDuplicateLoginId() throws Exception {
        when(authDao.selectAuthUserDetailsByLoginId("user01"))
                .thenReturn(activeUser(passwordEncoder.encode("password")));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "user01",
                                  "password": "new-password",
                                  "passwordConfirm": "new-password",
                                  "name": "중복 사용자",
                                  "termsAgreed": true,
                                  "privacyAgreed": true
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.errorCode").value("DUPLICATE_LOGIN_ID"));

        verify(authDao, never()).insertUser(any());
        verify(authDao, never()).insertUserRole(any(), any());
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void signupRejectsPasswordConfirmMismatch() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "newuser",
                                  "password": "new-password",
                                  "passwordConfirm": "another-password",
                                  "name": "신규 사용자",
                                  "termsAgreed": true,
                                  "privacyAgreed": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.errorCode").value("VALIDATION_FAILED"));

        verify(authDao, never()).insertUser(any());
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void loginReturnsPasswordRouteWhenResetIsRequired() throws Exception {
        when(authDao.selectAuthUserDetailsByLoginId("user01"))
                .thenReturn(activeUser(passwordEncoder.encode("password"), true));
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
                .andExpect(jsonPath("$.data.passwordResetRequired").value(true))
                .andExpect(jsonPath("$.data.defaultRoute").value("/password"));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
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

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    /**
     * 업무 처리를 수행합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @WithMockUser(username = "user01", roles = "USER")
    void logoutReturnsApiResponse() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @WithMockUser(username = "user01", roles = "USER")
    void selectAuthMeReturnsApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loginId").value("user01"))
                .andExpect(jsonPath("$.data.primaryRole").value("USER"))
                .andExpect(jsonPath("$.data.defaultRoute").value("/app/dashboard"));
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void selectAuthMeWithoutAuthenticationReturnsErrorCode() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.errorCode").value("AUTH_REQUIRED"));
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @WithMockUser(username = "admin01", roles = "ADMIN")
    void selectAuthMeReturnsAdminDashboardDefaultRouteForAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.primaryRole").value("ADMIN"))
                .andExpect(jsonPath("$.data.defaultRoute").value("/app/admin/dashboard"));
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @WithMockUser(username = "approver01", roles = "APPROVER")
    void selectAuthMeReturnsApproverReviewDefaultRouteForApprover() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.primaryRole").value("APPROVER"))
                .andExpect(jsonPath("$.data.defaultRoute").value("/app/approver/reviews"));
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @WithMockUser(username = "operator01", roles = "OPERATOR")
    void selectAuthMeReturnsOperatorDashboardDefaultRouteForOperator() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.primaryRole").value("OPERATOR"))
                .andExpect(jsonPath("$.data.defaultRoute").value("/app/operator/dashboard"));
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @WithMockUser(username = "partner01", roles = "PARTNER")
    void selectAuthMeReturnsPartnerVerificationListDefaultRouteForPartner() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.primaryRole").value("PARTNER"))
                .andExpect(jsonPath("$.data.defaultRoute").value("/app/partner/verifications"));
    }

    /**
     * 업무 데이터를 수정합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    /**
     * 업무 데이터를 수정합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
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

    /**
     * 업무 데이터를 수정합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    /**
     * 업무 데이터를 수정합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @WithMockUser(username = "user01", roles = "USER")
    void updatePasswordReturnsKoreanFieldErrorWhenNewPasswordIsTooShort() throws Exception {
        mockMvc.perform(patch("/api/v1/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "old-password",
                                  "newPassword": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.data.fieldErrors[0].field").value("newPassword"))
                .andExpect(jsonPath("$.data.fieldErrors[0].message").value("새 비밀번호는 8~16자로 입력해 주세요."));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param passwordHash 입력 값
     *
     * @return 처리 결과
     */
    private AuthUserDetailsRow activeUser(String passwordHash) {
        return activeUser(passwordHash, false);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param passwordHash 입력 값
     *
     * @param passwordResetRequired 입력 값
     *
     * @return 처리 결과
     */
    private AuthUserDetailsRow activeUser(String passwordHash, boolean passwordResetRequired) {
        return new AuthUserDetailsRow(
                USER_ID,
                "user01",
                passwordHash,
                "사용자",
                "ACTIVE",
                passwordResetRequired,
                null,
                null,
                null
        );
    }
}
