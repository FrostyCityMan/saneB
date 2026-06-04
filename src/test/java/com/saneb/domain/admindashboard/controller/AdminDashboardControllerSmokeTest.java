package com.saneb.domain.admindashboard.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.domain.admindashboard.dto.AdminDashboardSummaryResponse;
import com.saneb.domain.admindashboard.service.AdminDashboardService;
import java.math.BigDecimal;
import java.util.List;
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
class AdminDashboardControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDashboardService adminDashboardService;

    @BeforeEach
    void setUp() {
        when(adminDashboardService.selectSummary()).thenReturn(sampleSummary());
    }

    @Test
    @WithMockUser(username = "admin01", roles = "ADMIN")
    void selectSummaryReturnsAdminApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userSummary.totalUserCount").value(10))
                .andExpect(jsonPath("$.data.verificationSummary.reviewQueueCount").value(3));
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectSummaryRejectsNonAdminUser() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/summary"))
                .andExpect(status().isForbidden());
    }

    static AdminDashboardSummaryResponse sampleSummary() {
        return new AdminDashboardSummaryResponse(
                new AdminDashboardSummaryResponse.UserSummaryResponse(10, 9, 6, 1, 1, 0, 1),
                new AdminDashboardSummaryResponse.AnnouncementSummaryResponse(20, 3, 2, 12, 1, 8, 1, 6),
                new AdminDashboardSummaryResponse.VerificationSummaryResponse(
                        5,
                        3,
                        2,
                        0,
                        List.of(
                                new AdminDashboardSummaryResponse.StatusCountResponse("DRAFT", 0),
                                new AdminDashboardSummaryResponse.StatusCountResponse("SUBMITTED", 1),
                                new AdminDashboardSummaryResponse.StatusCountResponse("REVIEWING", 2),
                                new AdminDashboardSummaryResponse.StatusCountResponse("VERIFIED", 2),
                                new AdminDashboardSummaryResponse.StatusCountResponse("REJECTED", 0),
                                new AdminDashboardSummaryResponse.StatusCountResponse("EXPIRED", 0)
                        )
                ),
                new AdminDashboardSummaryResponse.MatchingSummaryResponse(
                        4,
                        2,
                        1,
                        0,
                        1,
                        List.of(
                                new AdminDashboardSummaryResponse.StatusCountResponse("MATCHED", 2),
                                new AdminDashboardSummaryResponse.StatusCountResponse("NOT_MATCHED", 0),
                                new AdminDashboardSummaryResponse.StatusCountResponse("REVIEW_REQUIRED", 1),
                                new AdminDashboardSummaryResponse.StatusCountResponse("BLOCKED", 0),
                                new AdminDashboardSummaryResponse.StatusCountResponse("PROGRESSED", 1)
                        )
                ),
                new AdminDashboardSummaryResponse.ApplicationProgressSummaryResponse(
                        3,
                        1,
                        1,
                        1,
                        0,
                        0,
                        1,
                        BigDecimal.ZERO,
                        List.of(
                                new AdminDashboardSummaryResponse.StatusCountResponse("READY", 1),
                                new AdminDashboardSummaryResponse.StatusCountResponse("IN_PROGRESS", 0),
                                new AdminDashboardSummaryResponse.StatusCountResponse("WAITING_RESULT", 1),
                                new AdminDashboardSummaryResponse.StatusCountResponse("APPROVED", 0),
                                new AdminDashboardSummaryResponse.StatusCountResponse("REJECTED", 0),
                                new AdminDashboardSummaryResponse.StatusCountResponse("SUPPLEMENT_REQUESTED", 0),
                                new AdminDashboardSummaryResponse.StatusCountResponse("STOPPED", 0),
                                new AdminDashboardSummaryResponse.StatusCountResponse("COMPLETED", 1)
                        )
                ),
                new AdminDashboardSummaryResponse.AuditSummaryResponse(100, 4, 1)
        );
    }
}
