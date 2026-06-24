/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ConsultationViewControllerSmokeTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.consultation.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
class ConsultationViewControllerSmokeTest {

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
    void selectConsultationPageUsesRequestLabelForUser() throws Exception {
        when(authService.selectAuthMe(any())).thenReturn(authResponse("USER"));

        mockMvc.perform(get("/app/consultations"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/consultations"))
                .andExpect(content().string(containsString("상담 요청 | 사내비")))
                .andExpect(content().string(containsString("상담 요청")))
                .andExpect(content().string(containsString("상담이 필요한 내용을 남기면 운영자가 확인한 뒤 담당자를 배정합니다.")))
                .andExpect(content().string(not(containsString("상담 관리"))))
                .andExpect(content().string(not(containsString("담당자 배정 및 상태 변경"))));
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
    void selectConsultationPageUsesManagementLabelForAdmin() throws Exception {
        when(authService.selectAuthMe(any())).thenReturn(authResponse("ADMIN"));

        mockMvc.perform(get("/app/consultations"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/consultations"))
                .andExpect(content().string(containsString("상담 관리 | 사내비")))
                .andExpect(content().string(containsString("상담 관리")))
                .andExpect(content().string(containsString("운영자는 요청 건을 확인해 담당자와 상태를 수동으로 배정합니다.")))
                .andExpect(content().string(containsString("담당자 배정 및 상태 변경")));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param role 입력 값
     *
     * @return 처리 결과
     */
    private AuthMeResponse authResponse(String role) {
        String defaultRoute = "ADMIN".equals(role) ? "/app/admin/dashboard" : "/app/dashboard";
        return new AuthMeResponse(
                UUID.fromString("10000000-0000-0000-0000-000000000002"),
                role.toLowerCase() + "01",
                "ADMIN".equals(role) ? "관리자" : "일반 사용자",
                List.of(role),
                role,
                defaultRoute,
                false,
                new AuthMeResponse.ProfileResponse(null, null, null)
        );
    }
}
