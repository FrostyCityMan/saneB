package com.saneb.domain.operation.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
class OperationViewControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectNotificationPageReturnsUserFacingView() throws Exception {
        mockMvc.perform(get("/app/notifications"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/notifications"))
                .andExpect(content().string(containsString("알림")))
                .andExpect(content().string(containsString("data-notification-page")))
                .andExpect(content().string(containsString("/api/v1/notifications/me")))
                .andExpect(content().string(not(containsString("th:utext"))));
    }

    @Test
    @WithMockUser(username = "operator01", roles = "OPERATOR")
    void selectOperationTaskPageReturnsOperatingView() throws Exception {
        mockMvc.perform(get("/app/operation-tasks"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/operation-tasks"))
                .andExpect(content().string(containsString("운영 업무 큐")))
                .andExpect(content().string(containsString("data-operation-task-page")))
                .andExpect(content().string(containsString("/api/v1/operation-tasks")))
                .andExpect(content().string(not(containsString("th:utext"))));
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectOperationTaskPageRejectsUser() throws Exception {
        mockMvc.perform(get("/app/operation-tasks"))
                .andExpect(status().isForbidden());
    }
}
