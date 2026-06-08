package com.saneb.domain.applog.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.saneb.domain.applog.service.AppLogService;
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
class AppLogViewControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppLogService appLogService;

    @BeforeEach
    void setUp() {
        when(appLogService.selectAppLog(any(), any(), anyInt()))
                .thenReturn(AppLogControllerSmokeTest.sampleResponse());
    }

    @Test
    @WithMockUser(username = "admin01", roles = "ADMIN")
    void selectAppLogPageReturnsThymeleafView() throws Exception {
        mockMvc.perform(get("/app/admin/app-logs"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/admin-app-logs"))
                .andExpect(content().string(containsString("앱 로그 관리")))
                .andExpect(content().string(containsString("최근 로그")))
                .andExpect(content().string(containsString("2026-06-08 ERROR sample")))
                .andExpect(content().string(not(containsString("th:utext"))));
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectAppLogPageRejectsUserRole() throws Exception {
        mockMvc.perform(get("/app/admin/app-logs"))
                .andExpect(status().isForbidden());
    }
}
