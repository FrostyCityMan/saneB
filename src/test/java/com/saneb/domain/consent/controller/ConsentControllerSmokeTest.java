/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ConsentControllerSmokeTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.consent.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.domain.consent.dto.CurrentConsentResponse;
import com.saneb.domain.consent.dto.UserConsentResponse;
import com.saneb.domain.consent.service.ConsentService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class ConsentControllerSmokeTest {

    static final UUID CONSENT_VERSION_ID = UUID.fromString("70000000-0000-0000-0000-000000000001");
    static final UUID USER_CONSENT_ID = UUID.fromString("71000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsentService consentService;

    /**
     * 업무 처리를 수행합니다.
     */
    @BeforeEach
    void setUp() {
        when(consentService.selectCurrentConsentList()).thenReturn(List.of(sampleCurrentConsent()));
        when(consentService.selectMyConsentList(any())).thenReturn(List.of(sampleUserConsent()));
        when(consentService.insertMyConsent(any(), any(), any())).thenReturn(sampleUserConsent());
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void selectCurrentConsentListAllowsAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/v1/consents/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].consentCode").value("PRIVACY_POLICY"))
                .andExpect(jsonPath("$.data[0].consentName").value("개인정보 처리방침"));
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
    void selectMyConsentListReturnsApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/consents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].userConsentId").value(USER_CONSENT_ID.toString()));
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    /**
     * 업무 데이터를 등록합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @WithMockUser(username = "user01", roles = "USER")
    void insertMyConsentReturnsApiResponse() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/consents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "consentCode": "E_CERT",
                                  "consented": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.consentCode").value("PRIVACY_POLICY"));
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void selectMyConsentListRejectsAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/consents"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    static CurrentConsentResponse sampleCurrentConsent() {
        return new CurrentConsentResponse(
                CONSENT_VERSION_ID,
                "PRIVACY_POLICY",
                "개인정보 처리방침",
                1,
                true,
                OffsetDateTime.parse("2026-06-08T10:00:00+09:00")
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    static UserConsentResponse sampleUserConsent() {
        return new UserConsentResponse(
                USER_CONSENT_ID,
                CONSENT_VERSION_ID,
                "PRIVACY_POLICY",
                "개인정보 처리방침",
                1,
                true,
                OffsetDateTime.parse("2026-06-08T10:00:00+09:00")
        );
    }
}
