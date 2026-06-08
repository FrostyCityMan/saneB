package com.saneb.domain.operatordashboard.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.saneb.domain.operatordashboard.service.OperatorDashboardService;
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
class OperatorDashboardViewControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OperatorDashboardService operatorDashboardService;

    @BeforeEach
    void setUp() {
        when(operatorDashboardService.selectSummary()).thenReturn(OperatorDashboardControllerSmokeTest.sampleSummary());
    }

    @Test
    @WithMockUser(username = "operator01", roles = "OPERATOR")
    void selectOperatorDashboardPageReturnsThymeleafView() throws Exception {
        mockMvc.perform(get("/app/operator/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/operator-dashboard"))
                .andExpect(content().string(containsString("운영자 대시보드")))
                .andExpect(content().string(containsString("공고 입력")))
                .andExpect(content().string(containsString("매칭 관리")))
                .andExpect(content().string(containsString("신청 진행")))
                .andExpect(content().string(not(containsString("파트너 검증"))))
                .andExpect(content().string(not(containsString("Backend"))))
                .andExpect(content().string(not(containsString("th:utext"))));
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectOperatorDashboardPageRejectsUser() throws Exception {
        mockMvc.perform(get("/app/operator/dashboard"))
                .andExpect(status().isForbidden());
    }
}
