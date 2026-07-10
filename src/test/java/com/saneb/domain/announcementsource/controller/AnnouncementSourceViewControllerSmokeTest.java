/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceViewControllerSmokeTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.controller;

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
class AnnouncementSourceViewControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    @WithMockUser(username = "operator01", roles = "OPERATOR")
    void selectAnnouncementSourcePageReturnsThymeleafView() throws Exception {
        mockMvc.perform(get("/app/admin/announcement-sources"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/announcement-sources"))
                .andExpect(content().string(containsString("외부 공고 수집")))
                .andExpect(content().string(containsString("data-announcement-source-page")))
                .andExpect(content().string(not(containsString("th:utext"))));
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectAnnouncementSourcePageRejectsUser() throws Exception {
        mockMvc.perform(get("/app/admin/announcement-sources"))
                .andExpect(status().isForbidden());
    }
}
