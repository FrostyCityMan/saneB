/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ApproverReviewViewControllerSmokeTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.approverreview.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.saneb.domain.approverreview.service.ApproverReviewService;
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
class ApproverReviewViewControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApproverReviewService approverReviewService;

    /**
     * 업무 처리를 수행합니다.
     */
    @BeforeEach
    void setUp() {
        when(approverReviewService.selectSummary()).thenReturn(ApproverReviewControllerSmokeTest.sampleSummary());
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
    void selectApproverReviewPageReturnsThymeleafView() throws Exception {
        mockMvc.perform(get("/app/approver/reviews"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/approver-reviews"))
                .andExpect(content().string(containsString("승인자 큐")))
                .andExpect(content().string(containsString("공고 승인")))
                .andExpect(content().string(containsString("검증 목록")))
                .andExpect(content().string(containsString("매칭 관리")))
                .andExpect(content().string(containsString("신청 진행")))
                .andExpect(content().string(not(containsString("Backend"))))
                .andExpect(content().string(not(containsString("API"))))
                .andExpect(content().string(not(containsString("th:utext"))));
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
    void selectApproverReviewPageAllowsAdminUser() throws Exception {
        mockMvc.perform(get("/app/approver/reviews"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/approver-reviews"));
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
    void selectApproverReviewPageRejectsOperatorUser() throws Exception {
        mockMvc.perform(get("/app/approver/reviews"))
                .andExpect(status().isForbidden());
    }
}
