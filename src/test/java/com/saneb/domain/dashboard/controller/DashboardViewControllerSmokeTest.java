package com.saneb.domain.dashboard.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
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
class DashboardViewControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        when(dashboardService.selectMySummary(org.mockito.ArgumentMatchers.any())).thenReturn(new DashboardSummaryResponse(
                "VERIFICATION_REQUIRED",
                new DashboardSummaryResponse.CandidateCountsResponse(0, 0, 0),
                0,
                new DashboardSummaryResponse.SupportAmountRangeResponse(null, null, "ANNOUNCEMENT_AMOUNT_RANGE"),
                "DRAFT",
                "전자증명 검증 전 참고 결과입니다."
        ));
        when(dashboardService.selectMyCurrentAction(org.mockito.ArgumentMatchers.any())).thenReturn(new DashboardCurrentActionResponse(
                "VERIFICATION_DOCUMENT_REQUIRED",
                "전자증명 검증이 필요합니다.",
                "최종 매칭 전 파트너 검증과 필수 서류 확인이 필요합니다.",
                "검증 진행하기",
                "/app/member/verifications/current",
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
    void selectDashboardPageReturnsThymeleafView() throws Exception {
        mockMvc.perform(get("/app/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/dashboard"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("현재 해야 할 행동")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("로그아웃")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("후보 결과")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("최종 매칭 결과")));
    }
}
