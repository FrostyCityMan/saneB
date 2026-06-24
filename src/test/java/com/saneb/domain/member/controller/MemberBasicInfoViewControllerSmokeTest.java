/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: MemberBasicInfoViewControllerSmokeTest.java
 * 작성자: 김도훈
 *
 */

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
