/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: BillingViewControllerSmokeTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.billing.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class BillingViewControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

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
    void selectMockBillingPageAllowsUserOnly() throws Exception {
        when(authService.selectAuthMe(any())).thenReturn(authResponse("USER"));

        mockMvc.perform(get("/app/billing/mock"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/mock-billing"))
                .andExpect(content().string(containsString("구독 결제")))
                .andExpect(content().string(containsString("data-mock-billing-app")));
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
    void selectMockBillingPageRejectsAdmin() throws Exception {
        mockMvc.perform(get("/app/billing/mock"))
                .andExpect(status().isForbidden());
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
    void selectSubscriptionPlanSettingsPageAllowsAdmin() throws Exception {
        when(authService.selectAuthMe(any())).thenReturn(authResponse("ADMIN"));

        mockMvc.perform(get("/app/billing/plans"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/subscription-plan-settings"))
                .andExpect(content().string(containsString("구독 금액 설정")))
                .andExpect(content().string(containsString("월 구독으로 받을 금액")))
                .andExpect(content().string(containsString("data-subscription-plan-settings-app")));
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
    void selectSubscriptionPlanSettingsPageRejectsUser() throws Exception {
        mockMvc.perform(get("/app/billing/plans"))
                .andExpect(status().isForbidden());
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param role 입력 값
     *
     * @return 처리 결과
     */
    private AuthMeResponse authResponse(String role) {
        String defaultRoute = switch (role) {
            case "ADMIN" -> "/app/admin/dashboard";
            case "OPERATOR" -> "/app/operator/dashboard";
            default -> "/app/dashboard";
        };
        return new AuthMeResponse(
                UUID.fromString("10000000-0000-0000-0000-000000000002"),
                role.toLowerCase() + "01",
                role.equals("ADMIN") ? "관리자" : "일반 사용자",
                List.of(role),
                role,
                defaultRoute,
                false,
                new AuthMeResponse.ProfileResponse(null, null, null)
        );
    }
}
