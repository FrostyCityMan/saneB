package com.saneb.domain.member.controller;

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
class MemberBasicInfoViewControllerSmokeTest {

    private static final String LEGACY_PRIMARY_MATCHING_LABEL = "1차 매칭" + " 공고";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectMemberBasicInfoPageReturnsCurrentMatchingLink() throws Exception {
        mockMvc.perform(get("/app/member/basic-info"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/member-basic-info"))
                .andExpect(content().string(containsString("기본 정보 입력")))
                .andExpect(content().string(containsString("결과 확인하기")))
                .andExpect(content().string(containsString("saneb-loading.js")))
                .andExpect(content().string(not(containsString(LEGACY_PRIMARY_MATCHING_LABEL))))
                .andExpect(content().string(not(containsString("th:utext"))));
    }
}
