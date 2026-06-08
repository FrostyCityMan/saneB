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

    @BeforeEach
    void setUp() {
        when(operatorDashboardService.selectSummary()).thenReturn(sampleSummary());
    }

    @Test
    @WithMockUser(username = "operator01", roles = "OPERATOR")
    void selectSummaryReturnsOperatorApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/operator/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.announcementWork.requestedCount").value(2))
                .andExpect(jsonPath("$.data.matchingWork.matchedCount").value(7))
                .andExpect(jsonPath("$.data.applicationProgressWork.waitingResultCount").value(3));
    }

    @Test
    @WithMockUser(username = "admin01", roles = "ADMIN")
    void selectSummaryAllowsAdminUser() throws Exception {
        mockMvc.perform(get("/api/v1/operator/dashboard/summary"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectSummaryRejectsUser() throws Exception {
        mockMvc.perform(get("/api/v1/operator/dashboard/summary"))
                .andExpect(status().isForbidden());
    }

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
