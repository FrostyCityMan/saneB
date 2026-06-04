package com.saneb.domain.admindashboard.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.saneb.domain.admindashboard.service.AdminDashboardService;
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
class AdminDashboardViewControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDashboardService adminDashboardService;

    @BeforeEach
    void setUp() {
        when(adminDashboardService.selectSummary()).thenReturn(AdminDashboardControllerSmokeTest.sampleSummary());
    }

    @Test
    @WithMockUser(username = "admin01", roles = "ADMIN")
    void selectAdminDashboardPageReturnsThymeleafView() throws Exception {
        mockMvc.perform(get("/app/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/admin-dashboard"))
                .andExpect(content().string(containsString("관리자 대시보드")))
                .andExpect(content().string(containsString("운영 현황")))
                .andExpect(content().string(containsString("감사 로그")))
                .andExpect(content().string(not(containsString("전자증명 검증"))));
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectAdminDashboardPageRejectsNonAdminUser() throws Exception {
        mockMvc.perform(get("/app/admin/dashboard"))
                .andExpect(status().isForbidden());
    }
}
