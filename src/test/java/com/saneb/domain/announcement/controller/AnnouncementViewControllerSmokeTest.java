/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementViewControllerSmokeTest.java
 * 작성자: 김도훈
 *
 */

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

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
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
                .andExpect(content().string(containsString("신용점수 조건")))
                .andExpect(content().string(containsString("data-credit-condition-list")))
                .andExpect(content().string(containsString("data-credit-condition-add")))
                .andExpect(content().string(containsString("배우자 소득")))
                .andExpect(content().string(containsString("자녀 재학 상태")))
                .andExpect(content().string(containsString("부모 부양 여부")))
                .andExpect(content().string(containsString("지원 품목, 제외 품목, 지원 용도")))
                .andExpect(content().string(containsString("사업자 유형")))
                .andExpect(content().string(containsString("최종 노출 상태")))
                .andExpect(content().string(containsString("중복 수혜 제한 - 정책자금 이용 이력")))
                .andExpect(content().string(containsString("종합소득금액")))
                .andExpect(content().string(containsString("최근 건강보험료")))
                .andExpect(content().string(containsString("국세 체납 여부")))
                .andExpect(content().string(containsString("지방세 체납 여부")))
                .andExpect(content().string(containsString("세대주 여부")))
                .andExpect(content().string(containsString("피부양자 여부")))
                .andExpect(content().string(containsString("data-option-value-select")))
                .andExpect(content().string(containsString("조건 항목을 선택하면 선택값이 표시됩니다.")))
                .andExpect(content().string(containsString("단계별 버튼")))
                .andExpect(content().string(containsString("단계 필요 서류")))
                .andExpect(content().string(containsString("필수 서류 전체 확인")))
                .andExpect(content().string(containsString("접수 정보 저장")))
                .andExpect(content().string(containsString("최종 결과 저장")))
                .andExpect(content().string(containsString("진행 원함")))
                .andExpect(content().string(not(containsString("placeholder=\"예: 개인사업자\""))))
                .andExpect(content().string(not(containsString("파트너 검증"))))
                .andExpect(content().string(not(containsString("th:utext"))));
    }
}
