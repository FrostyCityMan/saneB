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
                .andExpect(content().string(containsString("announcement-source-hero")))
                .andExpect(content().string(containsString("수집 공고 검수")))
                .andExpect(content().string(containsString("name=\"executionTime\"")))
                .andExpect(content().string(containsString("type=\"time\"")))
                .andExpect(content().string(containsString("서울 시간 기준으로 실행됩니다")))
                .andExpect(content().string(containsString("<option value=\"SESSION_BROWSER\">세션 유지 브라우저 요청</option>")))
                .andExpect(content().string(not(containsString("name=\"parserProfileCode\""))))
                .andExpect(content().string(not(containsString("data-local-parser-select"))))
                .andExpect(content().string(not(containsString("data-local-parser-url"))))
                .andExpect(content().string(not(containsString("name=\"cronExpression\""))))
                .andExpect(content().string(not(containsString("name=\"timezone\""))))
                .andExpect(content().string(not(containsString("data-local-qa-cleanup-form"))))
                .andExpect(content().string(not(containsString("지자체 수집 시험 데이터 삭제"))))
                .andExpect(content().string(not(containsString("수집 공고 원문"))))
                .andExpect(content().string(not(containsString("data-source-filter-form"))))
                .andExpect(content().string(not(containsString("data-source-list"))))
                .andExpect(content().string(not(containsString("data-source-detail"))))
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

    /**
     * 지자체 URL 수정 폼이 이전 행의 선택 입력값을 남기지 않는지 확인합니다.
     *
     * @throws Exception 요청 처리 오류
     */
    @Test
    @WithMockUser(username = "operator01", roles = "OPERATOR")
    void selectAnnouncementSourceScriptClearsNullableFormValues() throws Exception {
        mockMvc.perform(get("/js/saneb-announcement-sources.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("localSourceForm.reset();")))
                .andExpect(content().string(containsString("field.value = value == null ? \"\" : value;")))
                .andExpect(content().string(containsString("automaticCollectionReady")))
                .andExpect(content().string(not(containsString("renderLocalParsers"))))
                .andExpect(content().string(not(containsString("localParserUrl"))));
    }

    /**
     * 수집 공고 검수 화면이 전용 목록 구조와 메뉴를 렌더링하는지 확인합니다.
     *
     * @throws Exception 요청 처리 오류
     */
    @Test
    @WithMockUser(username = "operator01", roles = "OPERATOR")
    void selectCollectedAnnouncementPageReturnsDedicatedView() throws Exception {
        mockMvc.perform(get("/app/admin/collected-announcements"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/collected-announcements"))
                .andExpect(content().string(containsString("수집 공고 검수")))
                .andExpect(content().string(containsString("data-collected-announcement-page")))
                .andExpect(content().string(containsString("collected-announcement-metrics")))
                .andExpect(content().string(containsString("수집 공고만 모아 확인")))
                .andExpect(content().string(containsString("<option value=\"REVIEW_PENDING\" selected>검수대기</option>")))
                .andExpect(content().string(not(containsString("th:utext"))));
    }

    /**
     * 수집 공고 목록이 원문 등록일을 표시하는지 확인합니다.
     *
     * @throws Exception 요청 처리 오류
     */
    @Test
    @WithMockUser(username = "operator01", roles = "OPERATOR")
    void selectCollectedAnnouncementScriptShowsOriginalPostedDate() throws Exception {
        mockMvc.perform(get("/js/saneb-collected-announcements.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("formatDateTime(item.postedAt)")))
                .andExpect(content().string(not(containsString("formatDateTime(item.collectedAt)"))));
    }

    /**
     * 일반 사용자가 수집 공고 검수 화면에 접근할 수 없는지 확인합니다.
     *
     * @throws Exception 요청 처리 오류
     */
    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectCollectedAnnouncementPageRejectsUser() throws Exception {
        mockMvc.perform(get("/app/admin/collected-announcements"))
                .andExpect(status().isForbidden());
    }
}
