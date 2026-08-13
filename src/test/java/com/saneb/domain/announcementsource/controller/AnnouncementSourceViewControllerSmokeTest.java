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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.nio.charset.StandardCharsets;

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
                .andExpect(content().string(containsString("분류 판정과 수집 상태를 나누어 확인합니다")))
                .andExpect(content().string(containsString("data-collected-view=\"ACTION_REQUIRED\"")))
                .andExpect(content().string(containsString("data-collected-view=\"EXCLUDED\"")))
                .andExpect(content().string(containsString("첨부파일 판정 제외")))
                .andExpect(content().string(containsString("name=\"targetCategoryCode\"")))
                .andExpect(content().string(containsString("name=\"supportTypeCode\"")))
                .andExpect(content().string(containsString("name=\"matchedGroupKindCode\"")))
                .andExpect(content().string(containsString("maxlength=\"36\"")))
                .andExpect(content().string(containsString("pattern=\"[0-9a-fA-F]{8}-")))
                .andExpect(content().string(containsString("data-can-manage=\"true\"")))
                .andExpect(content().string(containsString("data-classification-v2-enabled=\"false\"")))
                .andExpect(content().string(containsString("본인(개인)")))
                .andExpect(content().string(containsString("/api/v2/admin/announcement-sources")))
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
                .andExpect(content().string(containsString("classification.targetCategoryCodes")))
                .andExpect(content().string(containsString("classification.supportTypeCodes")))
                .andExpect(content().string(containsString("expectedClassificationDecisionId")))
                .andExpect(content().string(containsString("primaryTargetCategoryCode")))
                .andExpect(content().string(containsString("/confirmed-classification")))
                .andExpect(content().string(containsString("collection-diagnostic-panel")))
                .andExpect(content().string(containsString("const canManage = page.dataset.canManage === \"true\"")))
                .andExpect(content().string(containsString("const classificationV2Enabled")))
                .andExpect(result -> assertThat(
                        new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8),
                        containsString("기존 V1 계약")))
                .andExpect(content().string(containsString("const safeHttpUrl")))
                .andExpect(content().string(not(containsString("formatDateTime(item.collectedAt)"))));
    }

    /**
     * 내부 담당자가 키워드 규칙 버전 화면을 조회할 수 있는지 확인합니다.
     *
     * @throws Exception 요청 처리 오류
     */
    @Test
    @WithMockUser(username = "admin01", roles = "ADMIN")
    void selectAnnouncementKeywordPageReturnsVersionedRuleView() throws Exception {
        mockMvc.perform(get("/app/admin/announcement-keywords"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/announcement-keywords"))
                .andExpect(content().string(containsString("공고 키워드 관리")))
                .andExpect(content().string(containsString("data-keyword-rule-page")))
                .andExpect(content().string(containsString("/api/v1/admin/announcement-source-rule-releases")))
                .andExpect(content().string(containsString("유의어")))
                .andExpect(content().string(containsString("name=\"discoveryTerm\"")))
                .andExpect(content().string(containsString("name=\"discoveryOrder\"")))
                .andExpect(content().string(containsString("aria-describedby=\"keyword-rule-discovery-term-help\"")))
                .andExpect(content().string(containsString("수집 발견 검색어로 사용")))
                .andExpect(content().string(containsString("사용 중지")))
                .andExpect(content().string(containsString("V2에서는 첨부파일과 링크를 신규 수집·저장·다운로드·추출·분류하지 않습니다")))
                .andExpect(content().string(containsString("기존 V1 이력은 변경하지 않습니다")))
                .andExpect(content().string(containsString("Golden QA 실행 식별자")))
                .andExpect(content().string(containsString("QA 정답 세트와 영향 확인")))
                .andExpect(content().string(containsString("data-golden-set-run")))
                .andExpect(content().string(containsString("readonly")))
                .andExpect(content().string(containsString("기관명 보호 구간 오탐 방지")))
                .andExpect(content().string(not(containsString("th:utext"))));
    }

    /**
     * 키워드 화면 스크립트가 확정된 규칙 API와 관리자 권한 Gate를 사용하는지 확인합니다.
     *
     * @throws Exception 요청 처리 오류
     */
    @Test
    @WithMockUser(username = "admin01", roles = "ADMIN")
    void selectAnnouncementKeywordScriptUsesVersionedContracts() throws Exception {
        mockMvc.perform(get("/js/saneb-announcement-keywords.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/keyword-rules")))
                .andExpect(content().string(containsString("/publication")))
                .andExpect(content().string(containsString("ruleGroupCode")))
                .andExpect(content().string(containsString("canonicalKeyword")))
                .andExpect(content().string(containsString("expectedVersion")))
                .andExpect(content().string(containsString("isDiscoveryEligibleGroup")))
                .andExpect(content().string(containsString("startsWith(\"TARGET_\")")))
                .andExpect(content().string(containsString("startsWith(\"SUPPORT_\")")))
                .andExpect(content().string(containsString("discoveryTerm")))
                .andExpect(content().string(containsString("discoveryOrder")))
                .andExpect(result -> assertThat(
                        new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8),
                        containsString("수집 발견 검색어 ·")))
                .andExpect(content().string(containsString("const isAdmin = app.dataset.isAdmin === \"true\"")))
                .andExpect(content().string(containsString("requestAllPages")))
                .andExpect(content().string(containsString("goldenSetRunId")))
                .andExpect(content().string(containsString("/golden-set-runs")))
                .andExpect(content().string(not(containsString("truthSetExecutionId"))));
    }

    /**
     * 복수 역할 사용자가 OPERATOR 권한을 보유하면 수집 검수 조작 권한을 유지하는지 확인합니다.
     *
     * @throws Exception 요청 처리 오류
     */
    @Test
    @WithMockUser(username = "multi-role01", roles = {"APPROVER", "OPERATOR"})
    void selectCollectedAnnouncementPageUsesAllRolesForManagementPermission() throws Exception {
        mockMvc.perform(get("/app/admin/collected-announcements"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-can-manage=\"true\"")));
    }

    /**
     * 일반 사용자가 키워드 규칙 화면에 접근할 수 없는지 확인합니다.
     *
     * @throws Exception 요청 처리 오류
     */
    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectAnnouncementKeywordPageRejectsUser() throws Exception {
        mockMvc.perform(get("/app/admin/announcement-keywords"))
                .andExpect(status().isForbidden());
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
