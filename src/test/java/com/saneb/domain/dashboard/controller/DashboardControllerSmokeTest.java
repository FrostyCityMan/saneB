package com.saneb.domain.dashboard.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class DashboardControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectDashboardSummaryReturnsApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/me/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.serviceStatusCode").value("VERIFICATION_REQUIRED"));
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectDashboardCurrentActionReturnsApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/me/current-action"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actionCode").value("VERIFICATION_DOCUMENT_REQUIRED"));
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
