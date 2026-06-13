package com.saneb.domain.partnerverification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.common.response.PageResponse;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class PartnerVerificationControllerSmokeTest {

    private static final UUID VERIFICATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MEMBER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PARTNER_USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PartnerVerificationService partnerVerificationService;

    @BeforeEach
    void setUp() {
        PartnerVerificationDetailsResponse details = details("DRAFT");
        when(partnerVerificationService.selectPartnerVerificationList(
                any(),
                any(),
                any(),
                any(),
                eq(1),
                eq(20)
        )).thenReturn(PageResponse.of(List.of(summary("DRAFT")), 1, 20, 1));
        when(partnerVerificationService.insertPartnerVerification(any(), any())).thenReturn(details);
        when(partnerVerificationService.selectPartnerVerificationDetails(VERIFICATION_ID)).thenReturn(details);
        when(partnerVerificationService.updatePartnerVerificationStatus(any(), eq(VERIFICATION_ID), any()))
                .thenReturn(details("SUBMITTED"));
    }

    @Test
    @WithMockUser(username = "operator", roles = "OPERATOR")
    void selectPartnerVerificationListReturnsApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/partner-verifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].verificationId").value(VERIFICATION_ID.toString()))
                .andExpect(jsonPath("$.data.totalCount").value(1));
    }

    @Test
    @WithMockUser(username = "operator", roles = "OPERATOR")
    void insertPartnerVerificationReturnsApiResponse() throws Exception {
        mockMvc.perform(post("/api/v1/partner-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberUserId": "%s"
                                }
                                """.formatted(MEMBER_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("DRAFT"));
    }

    @Test
    @WithMockUser(username = "operator", roles = "OPERATOR")
    void selectPartnerVerificationDetailsReturnsApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/partner-verifications/{verificationId}", VERIFICATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.verificationId").value(VERIFICATION_ID.toString()));
    }

    @Test
    @WithMockUser(username = "operator", roles = "OPERATOR")
    void updatePartnerVerificationValuesReturnApiResponse() throws Exception {
        mockMvc.perform(put("/api/v1/partner-verifications/{verificationId}/member-values", VERIFICATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "birthYear": 1988,
                                  "regionCode": "SEOUL",
                                  "householder": true,
                                  "householdMember": false,
                                  "hasIncome": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(put("/api/v1/partner-verifications/{verificationId}/business-values", VERIFICATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "annualRevenue": 120000000,
                                  "employeeCount": 5,
                                  "regularEmployeeCount": 3,
                                  "taxStatusCode": "PAID",
                                  "niceCreditScore": 820,
                                  "kcbCreditScore": 805,
                                  "hasExistingLoan": true,
                                  "hasPolicyFundUsage": false,
                                  "hasGuaranteeUsage": false,
                                  "financialCheckedOn": "2026-05-24"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(partnerVerificationService).updateVerificationMemberValues(any(), eq(VERIFICATION_ID), any());
        verify(partnerVerificationService).updateVerificationBusinessValues(any(), eq(VERIFICATION_ID), any());
    }

    @Test
    @WithMockUser(username = "operator", roles = "OPERATOR")
    void updatePartnerVerificationCollectionsReturnApiResponse() throws Exception {
        mockMvc.perform(put("/api/v1/partner-verifications/{verificationId}/family-values", VERIFICATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "familyValues": [
                                    {
                                      "relationTypeCode": "SPOUSE",
                                      "birthYear": 1990,
                                      "cohabiting": true,
                                      "supported": true,
                                      "hasIncome": false
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(put("/api/v1/partner-verifications/{verificationId}/documents", VERIFICATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documents": [
                                    {
                                      "documentTypeCode": "BUSINESS_REGISTRATION",
                                      "sourceTypeCode": "PARTNER_CHECK",
                                      "checked": true
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(put("/api/v1/partner-verifications/{verificationId}/restriction-flags", VERIFICATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "restrictionFlags": [
                                    {
                                      "restrictionCode": "NEEDS_REVIEW",
                                      "checked": true
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "operator", roles = "OPERATOR")
    void updatePartnerVerificationStatusReturnsApiResponse() throws Exception {
        mockMvc.perform(patch("/api/v1/partner-verifications/{verificationId}/status", VERIFICATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "statusCode": "SUBMITTED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("SUBMITTED"));
    }

    private PartnerVerificationSummaryResponse summary(String statusCode) {
        return new PartnerVerificationSummaryResponse(
                VERIFICATION_ID,
                MEMBER_USER_ID,
                PARTNER_USER_ID,
                null,
                "VRF-000001",
                "USR-000001",
                "USR-000002",
                statusCode,
                true,
                false,
                null,
                null,
                OffsetDateTime.parse("2026-05-24T10:00:00+09:00"),
                OffsetDateTime.parse("2026-05-24T10:00:00+09:00")
        );
    }

    private PartnerVerificationDetailsResponse details(String statusCode) {
        return new PartnerVerificationDetailsResponse(
                VERIFICATION_ID,
                MEMBER_USER_ID,
                PARTNER_USER_ID,
                null,
                "VRF-000001",
                "USR-000001",
                "USR-000002",
                statusCode,
                true,
                false,
                null,
                null,
                null,
                null,
                OffsetDateTime.parse("2026-05-24T10:00:00+09:00"),
                OffsetDateTime.parse("2026-05-24T10:00:00+09:00"),
                null,
                null,
                List.of(),
                List.of(),
                List.of()
        );
    }
}
