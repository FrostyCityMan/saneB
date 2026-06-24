/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: PartnerVerificationViewControllerSmokeTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.partnerverification.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import com.saneb.domain.partnerverification.dto.PartnerVerificationDetailsResponse;
import com.saneb.domain.partnerverification.dto.PartnerVerificationSummaryResponse;
import com.saneb.domain.partnerverification.service.PartnerVerificationService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class PartnerVerificationViewControllerSmokeTest {

    private static final UUID USER_ID = UUID.fromString("72000000-0000-0000-0000-000000000001");
    private static final UUID PARTNER_ID = UUID.fromString("72000000-0000-0000-0000-000000000002");
    private static final UUID VERIFICATION_ID = UUID.fromString("72000000-0000-0000-0000-000000000003");
    private static final UUID BUSINESS_PROFILE_ID = UUID.fromString("72000000-0000-0000-0000-000000000004");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private PartnerVerificationService partnerVerificationService;

    /**
     * 업무 처리를 수행합니다.
     */
    @BeforeEach
    void setUp() {
        PartnerVerificationSummaryResponse summary = summary();
        PartnerVerificationDetailsResponse details = details();
        when(authService.selectAuthMe(any())).thenReturn(userAuth());
        when(partnerVerificationService.selectPartnerVerificationList(eq(USER_ID), any(), any(), eq(true), eq(1), eq(1)))
                .thenReturn(PageResponse.of(List.of(summary), 1, 1, 1));
        when(partnerVerificationService.selectPartnerVerificationList(any(), eq(PARTNER_ID), any(), any(), eq(1), eq(20)))
                .thenReturn(PageResponse.of(List.of(summary), 1, 20, 1));
        when(partnerVerificationService.selectPartnerVerificationDetails(VERIFICATION_ID)).thenReturn(details);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void selectPartnerVerificationListPageUsesKoreanLabels() throws Exception {
        when(authService.selectAuthMe(any())).thenReturn(partnerAuth());

        mockMvc.perform(get("/app/partner/verifications")
                        .with(user("partner01").roles("PARTNER")))
                .andExpect(status().isOk())
                .andExpect(view().name("app/partner-verification-list"))
                .andExpect(content().string(containsString("검증 목록")))
                .andExpect(content().string(containsString("검증 번호")))
                .andExpect(content().string(containsString("입력 화면")))
                .andExpect(content().string(not(containsString("Partner Verification"))))
                .andExpect(content().string(not(containsString("검증 ID"))));
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void selectCurrentVerificationProgressPageUsesKoreanLabels() throws Exception {
        mockMvc.perform(get("/app/member/verifications/current")
                        .with(user("local_user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("app/verification-progress"))
                .andExpect(content().string(containsString("파트너 검증")))
                .andExpect(content().string(containsString("검증 번호")))
                .andExpect(content().string(containsString("서류")))
                .andExpect(content().string(not(containsString("Partner Verification"))))
                .andExpect(content().string(not(containsString("Documents"))))
                .andExpect(content().string(not(containsString("검증 ID"))));
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void selectPartnerVerificationInputPageUsesKoreanLabels() throws Exception {
        when(authService.selectAuthMe(any())).thenReturn(partnerAuth());

        mockMvc.perform(get("/app/partner-verifications/{verificationId}/input", VERIFICATION_ID)
                        .with(user("partner01").roles("PARTNER")))
                .andExpect(status().isOk())
                .andExpect(view().name("app/partner-verification-input"))
                .andExpect(content().string(containsString("검증 입력")))
                .andExpect(content().string(containsString("검증 번호")))
                .andExpect(content().string(containsString("대상")))
                .andExpect(content().string(not(containsString("Partner Input"))))
                .andExpect(content().string(not(containsString("Target"))))
                .andExpect(content().string(not(containsString("검증 ID"))));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private AuthMeResponse userAuth() {
        return new AuthMeResponse(
                USER_ID,
                "local_user",
                "Local User",
                List.of("USER"),
                "USER",
                "/app/dashboard",
                false,
                new AuthMeResponse.ProfileResponse(null, BUSINESS_PROFILE_ID, null)
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private AuthMeResponse partnerAuth() {
        return new AuthMeResponse(
                PARTNER_ID,
                "partner01",
                "Partner User",
                List.of("PARTNER"),
                "PARTNER",
                "/app/partner/verifications",
                false,
                new AuthMeResponse.ProfileResponse(null, null, null)
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private PartnerVerificationSummaryResponse summary() {
        OffsetDateTime now = OffsetDateTime.now();
        return new PartnerVerificationSummaryResponse(
                VERIFICATION_ID,
                USER_ID,
                PARTNER_ID,
                BUSINESS_PROFILE_ID,
                "VRF-000001",
                "USR-000001",
                "USR-000002",
                "DRAFT",
                true,
                false,
                null,
                null,
                now,
                now
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private PartnerVerificationDetailsResponse details() {
        OffsetDateTime now = OffsetDateTime.now();
        return new PartnerVerificationDetailsResponse(
                VERIFICATION_ID,
                USER_ID,
                PARTNER_ID,
                BUSINESS_PROFILE_ID,
                "VRF-000001",
                "USR-000001",
                "USR-000002",
                "DRAFT",
                true,
                false,
                null,
                null,
                null,
                "검토 메모",
                now,
                now,
                null,
                null,
                List.of(),
                List.of(),
                List.of(new PartnerVerificationDetailsResponse.DocumentResponse(
                        "BUSINESS_REGISTRATION",
                        "PARTNER_CHECK",
                        false,
                        null,
                        null,
                        null
                ))
        );
    }
}
