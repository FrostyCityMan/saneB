package com.saneb.domain.matching.controller;

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
class MatchingViewControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "operator01", roles = "OPERATOR")
    void selectMatchingCasePageReturnsThymeleafView() throws Exception {
        mockMvc.perform(get("/app/matching/cases"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/matching-cases"))
                .andExpect(content().string(containsString("매칭 관리")))
                .andExpect(content().string(containsString("data-matching-app")))
                .andExpect(content().string(containsString("/api/v1/matching/cases")))
                .andExpect(content().string(containsString("data-lookup-open=\"announcement\"")))
                .andExpect(content().string(containsString("data-lookup-open=\"member\"")))
                .andExpect(content().string(containsString("/api/v1/matching/cases/member-lookups")))
                .andExpect(content().string(not(containsString("검증 ID"))))
                .andExpect(content().string(not(containsString("th:utext"))));
    }

    @Test
    @WithMockUser(username = "approver01", roles = "APPROVER")
    void selectMatchingCasePageAllowsApprover() throws Exception {
        mockMvc.perform(get("/app/matching/cases"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/matching-cases"));
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectMatchingCasePageRejectsUser() throws Exception {
        mockMvc.perform(get("/app/matching/cases"))
                .andExpect(status().isForbidden());
    }
}
