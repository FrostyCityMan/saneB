/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ApproverReviewControllerSmokeTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.approverreview.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.domain.approverreview.dto.ApproverReviewSummaryResponse;
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
class ApproverReviewControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApproverReviewService approverReviewService;

    /**
     * 업무 처리를 수행합니다.
     */
    @BeforeEach
    void setUp() {
        when(approverReviewService.selectSummary()).thenReturn(sampleSummary());
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
    void selectSummaryReturnsApproverReviewApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/approver/reviews/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.announcementReview.requestedCount").value(4))
                .andExpect(jsonPath("$.data.verificationReview.reviewingCount").value(2))
                .andExpect(jsonPath("$.data.matchingReview.reviewRequiredCount").value(3))
                .andExpect(jsonPath("$.data.progressReview.waitingResultCount").value(6));
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
        mockMvc.perform(get("/api/v1/approver/reviews/summary"))
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
    @WithMockUser(username = "operator01", roles = "OPERATOR")
    void selectSummaryRejectsOperatorUser() throws Exception {
        mockMvc.perform(get("/api/v1/approver/reviews/summary"))
                .andExpect(status().isForbidden());
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    static ApproverReviewSummaryResponse sampleSummary() {
        return new ApproverReviewSummaryResponse(
                new ApproverReviewSummaryResponse.AnnouncementReviewResponse(4, 1, 9),
                new ApproverReviewSummaryResponse.VerificationReviewResponse(5, 2, 8, 1),
                new ApproverReviewSummaryResponse.MatchingReviewResponse(3, 1, 7),
                new ApproverReviewSummaryResponse.ProgressReviewResponse(6, 4, 2, 1)
        );
    }
}
