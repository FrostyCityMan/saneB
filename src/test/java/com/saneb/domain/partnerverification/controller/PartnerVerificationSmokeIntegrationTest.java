package com.saneb.domain.partnerverification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@EnabledIfEnvironmentVariable(named = "SANEB_PARTNER_VERIFICATION_SMOKE", matches = "true")
@ActiveProfiles("local")
@SpringBootTest
@AutoConfigureMockMvc
class PartnerVerificationSmokeIntegrationTest {

    private static final UUID LOCAL_USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void localOperatorCompletesPartnerVerificationInputFlowWithoutMatchingCreation() throws Exception {
        MockHttpSession session = loginLocalOperator();

        MvcResult createResult = mockMvc.perform(post("/api/v1/partner-verifications")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberUserId": "%s"
                                }
                                """.formatted(LOCAL_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("DRAFT"))
                .andExpect(jsonPath("$.data.current").value(true))
                .andExpect(jsonPath("$.data.matchingBlocked").value(false))
                .andReturn();

        UUID verificationId = selectVerificationId(createResult);

        mockMvc.perform(put("/api/v1/partner-verifications/{verificationId}/member-values", verificationId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "birthYear": 1988,
                                  "address": "Seoul test address",
                                  "regionCode": "SEOUL",
                                  "householder": true,
                                  "householdMember": false,
                                  "healthInsuranceBasisCode": "REGIONAL",
                                  "hasIncome": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(put("/api/v1/partner-verifications/{verificationId}/business-values", verificationId)
                        .session(session)
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

        mockMvc.perform(put("/api/v1/partner-verifications/{verificationId}/family-values", verificationId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "familyValues": [
                                    {
                                      "relationTypeCode": "SPOUSE",
                                      "birthYear": 1990,
                                      "address": "Family test address",
                                      "schoolAgeStatusCode": "NONE",
                                      "enrollmentStatusCode": "NONE",
                                      "cohabiting": true,
                                      "supported": true,
                                      "hasIncome": false
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(put("/api/v1/partner-verifications/{verificationId}/restriction-flags", verificationId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "restrictionFlags": [
                                    {
                                      "restrictionCode": "NEEDS_REVIEW",
                                      "checked": true,
                                      "note": "Gate check"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(put("/api/v1/partner-verifications/{verificationId}/documents", verificationId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documents": [
                                    {
                                      "documentTypeCode": "BUSINESS_REGISTRATION",
                                      "sourceTypeCode": "PARTNER_CHECK",
                                      "checked": true,
                                      "note": "Checked by smoke"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        updateStatus(session, verificationId, "SUBMITTED");
        updateStatus(session, verificationId, "REVIEWING");

        mockMvc.perform(patch("/api/v1/partner-verifications/{verificationId}/status", verificationId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "statusCode": "VERIFIED",
                                  "reviewNote": "Smoke verification complete"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("VERIFIED"))
                .andExpect(jsonPath("$.data.verifiedAt").exists())
                .andExpect(jsonPath("$.data.reviewedBy").exists());

        mockMvc.perform(get("/api/v1/partner-verifications/{verificationId}", verificationId)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberValues.regionCode").value("SEOUL"))
                .andExpect(jsonPath("$.data.businessValues.niceCreditScore").value(820))
                .andExpect(jsonPath("$.data.familyValues[0].relationTypeCode").value("SPOUSE"))
                .andExpect(jsonPath("$.data.restrictionFlags[0].restrictionCode").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data.documents[0].documentTypeCode").value("BUSINESS_REGISTRATION"));

        mockMvc.perform(get("/api/v1/partner-verifications")
                        .session(session)
                        .param("memberUserId", LOCAL_USER_ID.toString())
                        .param("current", "true")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].verificationId").value(verificationId.toString()));

        assertThat(selectPartnerVerificationCount(verificationId, "VERIFIED")).isEqualTo(1);
        assertThat(selectCountByVerification("verification_member_values", verificationId)).isEqualTo(1);
        assertThat(selectCountByVerification("verification_business_values", verificationId)).isEqualTo(1);
        assertThat(selectCountByVerification("verification_family_values", verificationId)).isEqualTo(1);
        assertThat(selectCountByVerification("verification_restriction_flags", verificationId)).isEqualTo(1);
        assertThat(selectCountByVerification("verification_documents", verificationId)).isEqualTo(1);
        assertThat(selectCheckedDocumentCount(verificationId)).isEqualTo(1);
        assertThat(selectCountByVerification("matching_cases", verificationId)).isEqualTo(0);
        assertThat(selectApplicationProgressCountByVerification(verificationId)).isEqualTo(0);
        assertThat(selectAuditCount(verificationId)).isEqualTo(8);
        assertThat(selectAuditMetadataPrivacyLeakCount(verificationId)).isEqualTo(0);
    }

    private void updateStatus(MockHttpSession session, UUID verificationId, String statusCode) throws Exception {
        mockMvc.perform(patch("/api/v1/partner-verifications/{verificationId}/status", verificationId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "statusCode": "%s"
                                }
                                """.formatted(statusCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value(statusCode));
    }

    private MockHttpSession loginLocalOperator() throws Exception {
        for (String password : List.of("password", "new-password")) {
            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "loginId": "local_operator",
                                      "password": "%s"
                                    }
                                    """.formatted(password)))
                    .andReturn();
            if (result.getResponse().getStatus() == 200 && result.getRequest().getSession(false) instanceof MockHttpSession session) {
                return session;
            }
        }
        throw new IllegalStateException("local_operator login failed for partner verification smoke.");
    }

    private UUID selectVerificationId(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return UUID.fromString(root.path("data").path("verificationId").asText());
    }

    private long selectPartnerVerificationCount(UUID verificationId, String statusCode) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM partner_verifications
                        WHERE id = ?
                          AND status_code = ?
                          AND is_current = true
                          AND is_matching_blocked = false
                        """,
                Long.class,
                verificationId,
                statusCode
        );
        return count == null ? 0 : count;
    }

    private long selectCountByVerification(String tableName, UUID verificationId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT count(1) FROM " + tableName + " WHERE verification_id = ?",
                Long.class,
                verificationId
        );
        return count == null ? 0 : count;
    }

    private long selectCheckedDocumentCount(UUID verificationId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM verification_documents
                        WHERE verification_id = ?
                          AND is_checked = true
                          AND checked_by IS NOT NULL
                          AND checked_at IS NOT NULL
                        """,
                Long.class,
                verificationId
        );
        return count == null ? 0 : count;
    }

    private long selectAuditCount(UUID verificationId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM audit_logs
                        WHERE resource_type = 'PARTNER_VERIFICATION'
                          AND resource_id = ?
                          AND action_code IN (
                              'PARTNER_VERIFICATION_CREATE',
                              'PARTNER_VERIFICATION_MEMBER_VALUES_SAVE',
                              'PARTNER_VERIFICATION_BUSINESS_VALUES_SAVE',
                              'PARTNER_VERIFICATION_FAMILY_VALUES_SAVE',
                              'PARTNER_VERIFICATION_RESTRICTION_FLAGS_SAVE',
                              'PARTNER_VERIFICATION_DOCUMENTS_SAVE',
                              'PARTNER_VERIFICATION_SUBMIT',
                              'PARTNER_VERIFICATION_VERIFY'
                          )
                        """,
                Long.class,
                verificationId
        );
        return count == null ? 0 : count;
    }

    private long selectApplicationProgressCountByVerification(UUID verificationId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM application_progresses ap
                        INNER JOIN matching_cases mc ON mc.id = ap.matching_case_id
                        WHERE mc.verification_id = ?
                        """,
                Long.class,
                verificationId
        );
        return count == null ? 0 : count;
    }

    private long selectAuditMetadataPrivacyLeakCount(UUID verificationId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM audit_logs
                        WHERE resource_type = 'PARTNER_VERIFICATION'
                          AND resource_id = ?
                          AND metadata_json::text ILIKE '%address%'
                        """,
                Long.class,
                verificationId
        );
        return count == null ? 0 : count;
    }
}
