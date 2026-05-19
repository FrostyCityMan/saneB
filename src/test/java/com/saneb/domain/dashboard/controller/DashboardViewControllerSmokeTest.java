package com.saneb.domain.dashboard.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class DashboardViewControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectDashboardPageReturnsThymeleafView() throws Exception {
        mockMvc.perform(get("/app/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/dashboard"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("현재 해야 할 행동")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("후보 결과")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("최종 매칭 결과")));
    }
}
