/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AdminDashboardViewControllerSmokeTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.admindashboard.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.saneb.domain.admindashboard.service.AdminDashboardService;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceAutomationStatusResponse;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeCollectionSummaryResponse;
import com.saneb.domain.announcementsource.localgov.service.LocalGovernmentNoticeService;
import com.saneb.domain.announcementsource.service.AnnouncementSourceAutomationStatusService;
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
class AdminDashboardViewControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDashboardService adminDashboardService;

    @MockitoBean
    private LocalGovernmentNoticeService localGovernmentNoticeService;

    @MockitoBean
    private AnnouncementSourceAutomationStatusService automationStatusService;

    /**
     * 업무 처리를 수행합니다.
     */
    @BeforeEach
    void setUp() {
        when(adminDashboardService.selectSummary()).thenReturn(AdminDashboardControllerSmokeTest.sampleSummary());
        when(localGovernmentNoticeService.selectCollectionSummary()).thenReturn(
                new LocalGovernmentNoticeCollectionSummaryResponse(244, 240, 238, 2, 1, 7, "RED")
        );
        when(automationStatusService.selectStatus()).thenReturn(
                new AnnouncementSourceAutomationStatusResponse(true, true, true, false)
        );
    }

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
    @WithMockUser(username = "admin01", roles = "ADMIN")
    void selectAdminDashboardPageReturnsThymeleafView() throws Exception {
        mockMvc.perform(get("/app/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/admin-dashboard"))
                .andExpect(content().string(containsString("관리자 대시보드")))
                .andExpect(content().string(containsString("운영 현황")))
                .andExpect(content().string(containsString("수집 신호등")))
                .andExpect(content().string(containsString("수집 오류 확인 필요")))
                .andExpect(content().string(containsString("지자체 정기 실행")))
                .andExpect(content().string(containsString("정부24 연결")))
                .andExpect(content().string(containsString("설정 필요")))
                .andExpect(content().string(containsString("고객·상담")))
                .andExpect(content().string(containsString("공고·수집")))
                .andExpect(content().string(containsString("매칭·진행")))
                .andExpect(content().string(containsString("결제·구독")))
                .andExpect(content().string(containsString("검수·시스템")))
                .andExpect(content().string(containsString("data-nav-group-id=\"announcements\"")))
                .andExpect(content().string(containsString("aria-controls=\"navGroupAnnouncements\"")))
                .andExpect(content().string(containsString("감사 로그")))
                .andExpect(content().string(not(containsString("전자증명 검증"))));
    }

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
    void selectAdminDashboardPageRejectsNonAdminUser() throws Exception {
        mockMvc.perform(get("/app/admin/dashboard"))
                .andExpect(status().isForbidden());
    }
}
