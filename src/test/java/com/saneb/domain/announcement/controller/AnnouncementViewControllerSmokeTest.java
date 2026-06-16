package com.saneb.domain.announcement.controller;

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
class AnnouncementViewControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "operator01", roles = "OPERATOR")
    void selectAnnouncementInputPageReturnsThymeleafView() throws Exception {
        mockMvc.perform(get("/app/announcements/input"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/announcement-input"))
                .andExpect(content().string(containsString("공고 대표 대상")))
                .andExpect(content().string(containsString("공고 기준 지원금액 범위")))
                .andExpect(content().string(containsString("승인 상태")))
                .andExpect(content().string(containsString("data-announcement-approval-form")))
                .andExpect(content().string(containsString("/api/v1/announcements")))
                .andExpect(content().string(containsString("data-announcement-basic-form")))
                .andExpect(content().string(containsString("소득 기준")))
                .andExpect(content().string(containsString("가구원 수")))
                .andExpect(content().string(containsString("직원 수")))
                .andExpect(content().string(containsString("NICE 신용 점수")))
                .andExpect(content().string(containsString("배우자 소득")))
                .andExpect(content().string(containsString("자녀 재학 상태")))
                .andExpect(content().string(containsString("부모 부양 여부")))
                .andExpect(content().string(containsString("지원 품목, 제외 품목, 지원 용도")))
                .andExpect(content().string(containsString("기업 형태")))
                .andExpect(content().string(containsString("중복 수혜 제한 - 정책자금 이용 이력")))
                .andExpect(content().string(containsString("종합소득금액")))
                .andExpect(content().string(containsString("최근 건강보험료")))
                .andExpect(content().string(containsString("국세 체납 여부")))
                .andExpect(content().string(containsString("지방세 체납 여부")))
                .andExpect(content().string(containsString("세대주 여부")))
                .andExpect(content().string(containsString("피부양자 여부")))
                .andExpect(content().string(not(containsString("파트너 검증"))))
                .andExpect(content().string(not(containsString("th:utext"))));
    }
}
