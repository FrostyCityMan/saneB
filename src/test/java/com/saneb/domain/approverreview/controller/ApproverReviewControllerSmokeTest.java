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

    @BeforeEach
    void setUp() {
        when(approverReviewService.selectSummary()).thenReturn(sampleSummary());
    }

    @Test
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

    @Test
    @WithMockUser(username = "admin01", roles = "ADMIN")
    void selectSummaryAllowsAdminUser() throws Exception {
        mockMvc.perform(get("/api/v1/approver/reviews/summary"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "operator01", roles = "OPERATOR")
    void selectSummaryRejectsOperatorUser() throws Exception {
        mockMvc.perform(get("/api/v1/approver/reviews/summary"))
                .andExpect(status().isForbidden());
    }

    static ApproverReviewSummaryResponse sampleSummary() {
        return new ApproverReviewSummaryResponse(
                new ApproverReviewSummaryResponse.AnnouncementReviewResponse(4, 1, 9),
                new ApproverReviewSummaryResponse.VerificationReviewResponse(5, 2, 8, 1),
                new ApproverReviewSummaryResponse.MatchingReviewResponse(3, 1, 7),
                new ApproverReviewSummaryResponse.ProgressReviewResponse(6, 4, 2, 1)
        );
    }
}
