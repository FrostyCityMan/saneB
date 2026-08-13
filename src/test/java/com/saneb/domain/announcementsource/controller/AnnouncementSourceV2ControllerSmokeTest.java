/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceV2ControllerSmokeTest.java
 * 작성자: 김도훈
 */

package com.saneb.domain.announcementsource.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.domain.announcementsource.dto.AnnouncementSourceLinkResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceV2ToAnnouncementRequest;
import com.saneb.domain.announcementsource.service.AnnouncementSourceV2ConversionService;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class AnnouncementSourceV2ControllerSmokeTest {

    private static final UUID SOURCE_ID = UUID.fromString("96000000-0000-0000-0000-000000000001");
    private static final UUID DECISION_ID = UUID.fromString("96000000-0000-0000-0000-000000000002");
    private static final UUID ANNOUNCEMENT_ID = UUID.fromString("96000000-0000-0000-0000-000000000003");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnnouncementSourceV2ConversionService conversionService;

    @Test
    void insertOperationalAnnouncementAllowsOperatorAndKeepsApiResponseWrapper() throws Exception {
        when(conversionService.insertOperationalAnnouncement(any(), eq(SOURCE_ID), any()))
                .thenReturn(linkResponse());

        mockMvc.perform(post(
                        "/api/v2/admin/announcement-sources/{sourceId}/announcements", SOURCE_ID
                )
                        .with(user(principal("OPERATOR")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sourceId").value(SOURCE_ID.toString()))
                .andExpect(jsonPath("$.data.announcementId").value(ANNOUNCEMENT_ID.toString()));

        verify(conversionService).insertOperationalAnnouncement(
                any(), eq(SOURCE_ID), any(AnnouncementSourceV2ToAnnouncementRequest.class)
        );
    }

    @Test
    void insertOperationalAnnouncementAllowsAdmin() throws Exception {
        when(conversionService.insertOperationalAnnouncement(any(), eq(SOURCE_ID), any()))
                .thenReturn(linkResponse());

        mockMvc.perform(post(
                        "/api/v2/admin/announcement-sources/{sourceId}/announcements", SOURCE_ID
                )
                        .with(user(principal("ADMIN")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void insertOperationalAnnouncementRejectsInvalidRequestBeforeServiceDelegation() throws Exception {
        mockMvc.perform(post(
                        "/api/v2/admin/announcement-sources/{sourceId}/announcements", SOURCE_ID
                )
                        .with(user(principal("OPERATOR")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"primaryTargetCategoryCode\":\"BUSINESS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(conversionService, never()).insertOperationalAnnouncement(any(), any(), any());
    }

    @Test
    void insertOperationalAnnouncementRejectsUserRole() throws Exception {
        mockMvc.perform(post(
                        "/api/v2/admin/announcement-sources/{sourceId}/announcements", SOURCE_ID
                )
                        .with(user(principal("USER")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden());

        verify(conversionService, never()).insertOperationalAnnouncement(any(), any(), any());
    }

    private AnnouncementSourceLinkResponse linkResponse() {
        return new AnnouncementSourceLinkResponse(
                SOURCE_ID, "SRC-000001", ANNOUNCEMENT_ID, "ANN-000001"
        );
    }

    private String validRequest() {
        return """
                {
                  "primaryTargetCategoryCode": "BUSINESS",
                  "targetCategoryCodes": ["BUSINESS", "PERSONAL"],
                  "supportTypeCodes": ["POLICY_FINANCE"],
                  "incomeJudgementCode": "VAT_TAX_BASE_ONLY",
                  "expectedClassificationDecisionId": "%s",
                  "expectedVersion": 3
                }
                """.formatted(DECISION_ID);
    }

    private AuthenticatedUserDetails principal(String role) {
        return new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        UUID.randomUUID(), "tester", "password-hash", "테스터", "ACTIVE",
                        false, null, null, null
                ),
                List.of(role)
        );
    }
}
