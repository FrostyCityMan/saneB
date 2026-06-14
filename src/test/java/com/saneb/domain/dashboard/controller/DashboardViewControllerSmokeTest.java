package com.saneb.domain.dashboard.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.mockito.Mockito.when;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.dashboard.dto.DashboardCurrentActionResponse;
import com.saneb.domain.dashboard.dto.DashboardProgressSummaryResponse;
import com.saneb.domain.dashboard.dto.DashboardReverificationStatusResponse;
import com.saneb.domain.dashboard.dto.DashboardSummaryResponse;
import com.saneb.domain.dashboard.service.DashboardService;
import com.saneb.domain.matching.service.MatchingService;
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

    @MockitoBean
    private MatchingService matchingService;

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
        when(matchingService.selectMyBasicMatchingCaseList(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(1), org.mockito.ArgumentMatchers.eq(5)))
                .thenReturn(PageResponse.of(List.of(), 1, 5, 0));
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectDashboardPageReturnsThymeleafView() throws Exception {
        mockMvc.perform(get("/app/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/dashboard"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("현재 해야 할 행동")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("로그아웃")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("진행 가능 현황")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("누적 현황")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("파트너 검증")
                )))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("전자증명 검증")
                )))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("ACTION_REQUIRED")
                )));
    }

    @Test
    @WithMockUser(username = "admin01", roles = "ADMIN")
    void selectDashboardPageHidesUserActionCardForAdmin() throws Exception {
        mockMvc.perform(get("/app/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/dashboard"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("운영 계정")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("현재 해야 할 행동")
                )))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("전자증명 검증이 필요합니다.")
                )));
    }
}
