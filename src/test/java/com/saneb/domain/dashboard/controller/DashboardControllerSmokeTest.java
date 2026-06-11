package com.saneb.domain.dashboard.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

import com.saneb.domain.dashboard.dto.DashboardCurrentActionResponse;
import com.saneb.domain.dashboard.dto.DashboardProgressSummaryResponse;
import com.saneb.domain.dashboard.dto.DashboardReverificationStatusResponse;
import com.saneb.domain.dashboard.dto.DashboardSummaryResponse;
import com.saneb.domain.dashboard.service.DashboardService;
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
class DashboardControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        when(dashboardService.selectMySummary(org.mockito.ArgumentMatchers.any())).thenReturn(new DashboardSummaryResponse(
                "BASIC_INFO_REQUIRED",
                new DashboardSummaryResponse.CandidateCountsResponse(0, 0, 0),
                new DashboardSummaryResponse.TargetCandidateCountsResponse(0, 0, 0),
                0,
                new DashboardSummaryResponse.SupportAmountRangeResponse(null, null, "ANNOUNCEMENT_AMOUNT_RANGE"),
                "DRAFT",
                "저장된 기본정보 기준으로 진행 가능한 공고가 아직 없습니다."
        ));
        when(dashboardService.selectMyCurrentAction(org.mockito.ArgumentMatchers.any())).thenReturn(new DashboardCurrentActionResponse(
                "BASIC_INFO_REQUIRED",
                "기본 정보를 입력해 주세요.",
                "사업자·개인·가족 기본정보를 입력하면 공고 조건과 비교해 진행 가능 현황을 확인합니다.",
                "기본 정보 입력",
                "/app/member/basic-info",
                null,
                5
        ));
        when(dashboardService.selectMyProgressSummary(org.mockito.ArgumentMatchers.any())).thenReturn(
                new DashboardProgressSummaryResponse(0, 0, 0, 0, 0, BigDecimal.ZERO)
        );
        when(dashboardService.selectMyReverificationStatus(org.mockito.ArgumentMatchers.any())).thenReturn(
                new DashboardReverificationStatusResponse(false, null, null, List.of())
        );
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectDashboardSummaryReturnsApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/me/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.serviceStatusCode").value("BASIC_INFO_REQUIRED"));
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectDashboardCurrentActionReturnsApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/me/current-action"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actionCode").value("BASIC_INFO_REQUIRED"));
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectDashboardProgressSummaryReturnsApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/me/progress-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.inProgressCount").value(0));
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectDashboardReverificationStatusReturnsApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/me/reverification-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.required").value(false));
    }
}
