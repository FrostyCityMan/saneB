/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementV2ControllerSmokeTest.java
 * 작성자: 김도훈
 */

package com.saneb.domain.announcement.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.domain.announcement.dto.AnnouncementDetailsResponse;
import com.saneb.domain.announcement.dto.AnnouncementV2SaveRequest;
import com.saneb.domain.announcement.service.AnnouncementService;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
class AnnouncementV2ControllerSmokeTest {

    private static final UUID ANNOUNCEMENT_ID =
            UUID.fromString("95000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnnouncementService announcementService;

    @Test
    void insertAnnouncementAllowsOperatorAndKeepsApiResponseWrapper() throws Exception {
        when(announcementService.insertAnnouncementV2(any(), any())).thenReturn(detailsResponse());

        mockMvc.perform(post("/api/v2/announcements")
                        .with(user(principal("OPERATOR")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.announcementId").value(ANNOUNCEMENT_ID.toString()))
                .andExpect(jsonPath("$.data.targetCategoryCodes[1]").value("PERSONAL"))
                .andExpect(jsonPath("$.data.supportTypeCodes[0]").value("POLICY_FINANCE"));

        verify(announcementService).insertAnnouncementV2(any(), any(AnnouncementV2SaveRequest.class));
    }

    @Test
    void updateAnnouncementAllowsAdminAndDelegatesAnnouncementId() throws Exception {
        when(announcementService.updateAnnouncementV2(any(), eq(ANNOUNCEMENT_ID), any()))
                .thenReturn(detailsResponse());

        mockMvc.perform(put("/api/v2/announcements/{announcementId}", ANNOUNCEMENT_ID)
                        .with(user(principal("ADMIN")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.announcementId").value(ANNOUNCEMENT_ID.toString()));

        verify(announcementService).updateAnnouncementV2(
                any(), eq(ANNOUNCEMENT_ID), any(AnnouncementV2SaveRequest.class)
        );
    }

    @Test
    void insertAnnouncementRejectsInvalidRequestBeforeServiceDelegation() throws Exception {
        mockMvc.perform(post("/api/v2/announcements")
                        .with(user(principal("OPERATOR")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목만 존재\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(announcementService, never()).insertAnnouncementV2(any(), any());
    }

    @Test
    void insertAnnouncementRejectsUserRole() throws Exception {
        mockMvc.perform(post("/api/v2/announcements")
                        .with(user(principal("USER")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden());

        verify(announcementService, never()).insertAnnouncementV2(any(), any());
    }

    private String validRequest() {
        return """
                {
                  "primaryTargetCategoryCode": "BUSINESS",
                  "targetCategoryCodes": ["BUSINESS", "PERSONAL"],
                  "supportTypeCodes": ["POLICY_FINANCE"],
                  "title": "소상공인 정책자금 지원",
                  "agencyName": "중소벤처기업부",
                  "summary": "지원사업 본문",
                  "applicationStartDate": "2026-08-01",
                  "applicationEndDate": "2026-08-31",
                  "incomeJudgementCode": "VAT_TAX_BASE_ONLY",
                  "minAmount": 0,
                  "maxAmount": 0,
                  "options": []
                }
                """;
    }

    private AnnouncementDetailsResponse detailsResponse() {
        return new AnnouncementDetailsResponse(
                ANNOUNCEMENT_ID,
                "ANN-000001",
                "BUSINESS",
                List.of("BUSINESS", "PERSONAL"),
                List.of("POLICY_FINANCE"),
                "소상공인 정책자금 지원",
                "중소벤처기업부",
                "지원사업 본문",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                "NORMAL",
                "OPEN",
                "접수중",
                "OPEN",
                "접수중",
                "DRAFT",
                "VAT_TAX_BASE_ONLY",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                OffsetDateTime.parse("2026-08-12T10:00:00+09:00"),
                OffsetDateTime.parse("2026-08-12T10:00:00+09:00"),
                List.of(),
                new AnnouncementDetailsResponse.ConditionsResponse(
                        List.of(), List.of(), List.of(), List.of()
                ),
                List.of()
        );
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
