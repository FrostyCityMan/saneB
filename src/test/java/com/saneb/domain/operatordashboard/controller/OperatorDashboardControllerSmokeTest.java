/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: OperatorDashboardControllerSmokeTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.operatordashboard.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.domain.operatordashboard.dto.OperatorDashboardSummaryResponse;
import com.saneb.domain.operatordashboard.service.OperatorDashboardService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class OperatorDashboardControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OperatorDashboardService operatorDashboardService;

    /**
     * 업무 처리를 수행합니다.
     */
    @BeforeEach
    void setUp() {
        when(operatorDashboardService.selectSummary()).thenReturn(sampleSummary());
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
    void selectSummaryReturnsOperatorApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/operator/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.announcementWork.requestedCount").value(2))
                .andExpect(jsonPath("$.data.matchingWork.matchedCount").value(7))
                .andExpect(jsonPath("$.data.applicationProgressWork.waitingResultCount").value(3));
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
    void selectSummaryAllowsAdminUser() throws Exception {
        mockMvc.perform(get("/api/v1/operator/dashboard/summary"))
                .andExpect(status().isOk());
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
    void selectSummaryRejectsUser() throws Exception {
        mockMvc.perform(get("/api/v1/operator/dashboard/summary"))
                .andExpect(status().isForbidden());
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    static OperatorDashboardSummaryResponse sampleSummary() {
        return new OperatorDashboardSummaryResponse(
                new OperatorDashboardSummaryResponse.AnnouncementWorkResponse(3, 2, 8, 1, 6),
                new OperatorDashboardSummaryResponse.MatchingWorkResponse(7, 2, 1, 4),
                new OperatorDashboardSummaryResponse.ApplicationProgressWorkResponse(
                        1,
                        2,
                        3,
                        4,
                        0,
                        1,
                        new BigDecimal("5000000.00")
                )
        );
    }
}
