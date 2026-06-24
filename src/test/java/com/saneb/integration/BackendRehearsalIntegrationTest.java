/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: BackendRehearsalIntegrationTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
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

@EnabledIfEnvironmentVariable(named = "SANEB_BACKEND_REHEARSAL", matches = "true")
@ActiveProfiles("local")
@SpringBootTest
@AutoConfigureMockMvc
class BackendRehearsalIntegrationTest {

    private static final UUID LOCAL_USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID LOCAL_OPERATOR_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final BigDecimal RECEIVED_AMOUNT = new BigDecimal("1234567");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void localOperatorCompletesBackendRehearsalFlowAndDashboardReflectsRealData() throws Exception {
        MockHttpSession operatorSession = login("local_operator");
        MockHttpSession userSession = login("local_user");
        String fixtureKey = UUID.randomUUID().toString();
        List<UUID> operationAnnouncementIds = new ArrayList<>();
        UUID announcementId = null;
        UUID stepId = null;
        RequirementIds requirementIds = null;

        for (int index = 1; index <= 3; index++) {
            String title = "Operational Announcement Rehearsal " + index + " " + fixtureKey;
            MvcResult announcementCreateResult = mockMvc.perform(post("/api/v1/announcements")
                            .session(operatorSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(announcementRequest(title)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.approvalStatusCode").value("DRAFT"))
                    .andReturn();
            UUID createdAnnouncementId = selectUuid(announcementCreateResult, "data", "announcementId");
            operationAnnouncementIds.add(createdAnnouncementId);

            String updatedTitle = "Operational Announcement Rehearsal Updated " + index + " " + fixtureKey;
            mockMvc.perform(put("/api/v1/announcements/{announcementId}", createdAnnouncementId)
                            .session(operatorSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(announcementRequest(updatedTitle)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value(updatedTitle));

            mockMvc.perform(put("/api/v1/announcements/{announcementId}/conditions", createdAnnouncementId)
                            .session(operatorSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(conditionsRequest()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            mockMvc.perform(put("/api/v1/announcements/{announcementId}/steps", createdAnnouncementId)
                            .session(operatorSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(stepsRequest()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            MvcResult announcementDetailsResult = mockMvc.perform(get("/api/v1/announcements/{announcementId}", createdAnnouncementId)
                            .session(operatorSession))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.conditions.numericConditions[0].conditionKey").value("ANNUAL_REVENUE"))
                    .andExpect(jsonPath("$.data.steps[0].buttons[0].buttonCode").value("SUBMIT_APPLICATION"))
                    .andReturn();

            MvcResult requirementsResult = mockMvc.perform(put("/api/v1/announcements/{announcementId}/input-requirements", createdAnnouncementId)
                            .session(operatorSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(inputRequirementsRequest()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.requirements").value(org.hamcrest.Matchers.hasSize(3)))
                    .andExpect(jsonPath("$.data.requirements[0].fieldTypeCode").value("TEXT"))
                    .andExpect(jsonPath("$.data.requirements[0].sensitive").value(true))
                    .andExpect(jsonPath("$.data.requirements[2].fieldTypeCode").value("MULTI_SELECT"))
                    .andReturn();

            if (index == 1) {
                announcementId = createdAnnouncementId;
                stepId = selectUuid(announcementDetailsResult, "data", "steps", "0", "stepId");
                requirementIds = selectRequirementIds(requirementsResult);
            }
        }

        assertThat(operationAnnouncementIds).hasSize(3);
        assertThat(operationAnnouncementIds).doesNotHaveDuplicates();
        assertThat(announcementId).isNotNull();
        assertThat(stepId).isNotNull();
        assertThat(requirementIds).isNotNull();

        mockMvc.perform(get("/api/v1/announcements")
                        .session(operatorSession)
                        .param("keyword", fixtureKey)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(3));

        mockMvc.perform(get("/api/v1/announcements/{announcementId}/input-requirements", announcementId)
                        .session(operatorSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requirements[2].options").value(org.hamcrest.Matchers.hasSize(2)));

        markAnnouncementApproved(announcementId);

        MvcResult verificationCreateResult = mockMvc.perform(post("/api/v1/partner-verifications")
                        .session(operatorSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberUserId": "%s"
                                }
                                """.formatted(LOCAL_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("DRAFT"))
                .andReturn();
        UUID verificationId = selectUuid(verificationCreateResult, "data", "verificationId");

        saveVerificationValues(operatorSession, verificationId);
        updateVerificationStatus(operatorSession, verificationId, "SUBMITTED");
        updateVerificationStatus(operatorSession, verificationId, "REVIEWING");
        updateVerificationStatus(operatorSession, verificationId, "VERIFIED");

        mockMvc.perform(get("/api/v1/partner-verifications/{verificationId}", verificationId)
                        .session(operatorSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("VERIFIED"))
                .andExpect(jsonPath("$.data.current").value(true));

        mockMvc.perform(get("/api/v1/partner-verifications")
                        .session(operatorSession)
                        .param("memberUserId", LOCAL_USER_ID.toString())
                        .param("current", "true")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(1));

        MvcResult matchingResult = mockMvc.perform(post("/api/v1/matching/cases")
                        .session(operatorSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "announcementId": "%s",
                                  "memberUserId": "%s",
                                  "verificationId": "%s"
                                }
                                """.formatted(announcementId, LOCAL_USER_ID, verificationId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("MATCHED"))
                .andReturn();
        UUID matchingCaseId = selectUuid(matchingResult, "data", "matchingCaseId");

        mockMvc.perform(get("/api/v1/matching/cases")
                        .session(operatorSession)
                        .param("announcementId", announcementId.toString())
                        .param("memberUserId", LOCAL_USER_ID.toString())
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(1));

        mockMvc.perform(get("/api/v1/matching/cases/{matchingCaseId}", matchingCaseId)
                        .session(operatorSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("MATCHED"));

        mockMvc.perform(get("/api/v1/matching/cases/{matchingCaseId}/results", matchingCaseId)
                        .session(operatorSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].resultCode").value("PASS"));

        mockMvc.perform(patch("/api/v1/matching/cases/{matchingCaseId}/status", matchingCaseId)
                        .session(operatorSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "statusCode": "MATCHED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("MATCHED"));

        MvcResult progressResult = mockMvc.perform(post("/api/v1/application-progresses")
                        .session(operatorSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "matchingCaseId": "%s"
                                }
                                """.formatted(matchingCaseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("READY"))
                .andExpect(jsonPath("$.data.currentStepId").value(stepId.toString()))
                .andReturn();
        UUID progressId = selectUuid(progressResult, "data", "progressId");

        mockMvc.perform(get("/api/v1/application-progresses")
                        .session(operatorSession)
                        .param("matchingCaseId", matchingCaseId.toString())
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(1));

        mockMvc.perform(get("/api/v1/application-progresses/{progressId}", progressId)
                        .session(operatorSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stepStates[0].statusCode").value("READY"));

        mockMvc.perform(patch("/api/v1/application-progresses/{progressId}/steps/{stepId}/action", progressId, stepId)
                        .session(operatorSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "buttonCode": "SUBMIT_APPLICATION",
                                  "input": {
                                    "memo": "raw action input must stay sanitized"
                                  }
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.errorCode").value("PROGRESS_CONDITION_NOT_MET"));

        String privateValue = "private-rehearsal-value-must-not-enter-audit";
        mockMvc.perform(put("/api/v1/application-progresses/{progressId}/input-values", progressId)
                        .session(operatorSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "values": [
                                    {
                                      "requirementId": "%s",
                                      "valueText": "%s"
                                    },
                                    {
                                      "requirementId": "%s",
                                      "valueNumber": 1234567
                                    },
                                    {
                                      "requirementId": "%s",
                                      "optionCodes": ["TRAINING", "CONSULTING"]
                                    }
                                  ]
                                }
                                """.formatted(
                                requirementIds.sensitiveMemoId(),
                                privateValue,
                                requirementIds.requestAmountId(),
                                requirementIds.supportTypesId()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.values[2].optionCodes").value(org.hamcrest.Matchers.hasSize(2)));

        mockMvc.perform(get("/api/v1/application-progresses/{progressId}/input-values", progressId)
                        .session(operatorSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.values[2].fieldTypeCode").value("MULTI_SELECT"))
                .andExpect(jsonPath("$.data.values[2].optionCodes").value(org.hamcrest.Matchers.hasSize(2)));

        mockMvc.perform(patch("/api/v1/application-progresses/{progressId}/steps/{stepId}/action", progressId, stepId)
                        .session(operatorSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "buttonCode": "SUBMIT_APPLICATION",
                                  "input": {
                                    "memo": "required dynamic inputs are complete"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("WAITING_RESULT"));

        mockMvc.perform(patch("/api/v1/application-progresses/{progressId}/receipt", progressId)
                        .session(operatorSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiptNo": "R-REHEARSAL-001",
                                  "receiptDate": "2026-06-20"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.receiptNo").value("R-REHEARSAL-001"));

        mockMvc.perform(patch("/api/v1/application-progresses/{progressId}/result", progressId)
                        .session(operatorSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resultCode": "APPROVED",
                                  "resultNote": "Backend rehearsal approved",
                                  "resultDate": "2026-07-01",
                                  "receivedAmount": 1234567
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("APPROVED"))
                .andExpect(jsonPath("$.data.receivedAmount").value(1234567));

        mockMvc.perform(get("/api/v1/dashboard/me/summary").session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.verificationStatusCode").value("VERIFIED"))
                .andExpect(jsonPath("$.data.supportAmountRange.basisCode").value("ANNOUNCEMENT_AMOUNT_RANGE"));

        mockMvc.perform(get("/api/v1/dashboard/me/current-action").session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actionCode").exists());

        MvcResult dashboardProgressResult = mockMvc.perform(get("/api/v1/dashboard/me/progress-summary")
                        .session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        JsonNode dashboardProgress = readJson(dashboardProgressResult).path("data");
        assertThat(dashboardProgress.path("approvedCount").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(dashboardProgress.path("totalReceivedAmount").decimalValue()).isGreaterThanOrEqualTo(RECEIVED_AMOUNT);

        mockMvc.perform(get("/api/v1/dashboard/me/reverification-status").session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.required").value(false));

        assertThat(selectApplicationInputValueCount(progressId)).isEqualTo(4);
        assertThat(selectApplicationInputOptionValueCount(progressId)).isEqualTo(2);
        assertThat(selectAuditPrivacyLeakCount(progressId, privateValue)).isZero();
        assertThat(selectApprovedProgressCount(progressId)).isEqualTo(1);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param loginId 입력 값
     *
     * @return 처리 결과
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    private MockHttpSession login(String loginId) throws Exception {
        for (String password : List.of("password", "new-password")) {
            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "loginId": "%s",
                                      "password": "%s"
                                    }
                                    """.formatted(loginId, password)))
                    .andReturn();
            if (result.getResponse().getStatus() == 200
                    && result.getRequest().getSession(false) instanceof MockHttpSession session) {
                return session;
            }
        }
        throw new IllegalStateException(loginId + " login failed for backend rehearsal.");
    }

    /**
     * 업무 데이터를 저장합니다.
     *
     * @param session 입력 값
     *
     * @param verificationId 입력 값
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    private void saveVerificationValues(MockHttpSession session, UUID verificationId) throws Exception {
        mockMvc.perform(put("/api/v1/partner-verifications/{verificationId}/member-values", verificationId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "birthYear": 1988,
                                  "address": "Rehearsal address",
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
                                  "hasExistingLoan": false,
                                  "hasPolicyFundUsage": false,
                                  "hasGuaranteeUsage": false,
                                  "financialCheckedOn": "2026-05-25"
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
                                      "note": "Backend rehearsal document check"
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
                                  "restrictionFlags": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param session 입력 값
     *
     * @param verificationId 입력 값
     *
     * @param statusCode 입력 값
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    private void updateVerificationStatus(MockHttpSession session, UUID verificationId, String statusCode) throws Exception {
        mockMvc.perform(patch("/api/v1/partner-verifications/{verificationId}/status", verificationId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "statusCode": "%s",
                                  "reviewNote": "Backend rehearsal"
                                }
                                """.formatted(statusCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value(statusCode));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param announcementId 입력 값
     */
    private void markAnnouncementApproved(UUID announcementId) {
        jdbcTemplate.update(
                """
                        UPDATE announcements
                        SET
                            approval_status_code = 'APPROVED',
                            updated_by = ?,
                            updated_at = now()
                        WHERE id = ?
                        """,
                LOCAL_OPERATOR_ID,
                announcementId
        );
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param result 입력 값
     *
     * @return 처리 결과
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    private RequirementIds selectRequirementIds(MvcResult result) throws Exception {
        JsonNode requirements = readJson(result).path("data").path("requirements");
        UUID sensitiveMemoId = null;
        UUID requestAmountId = null;
        UUID supportTypesId = null;
        for (JsonNode requirement : requirements) {
            String fieldKey = requirement.path("fieldKey").asText();
            UUID requirementId = UUID.fromString(requirement.path("requirementId").asText());
            if ("SENSITIVE_MEMO".equals(fieldKey)) {
                sensitiveMemoId = requirementId;
            } else if ("REQUEST_AMOUNT".equals(fieldKey)) {
                requestAmountId = requirementId;
            } else if ("SUPPORT_TYPES".equals(fieldKey)) {
                supportTypesId = requirementId;
            }
        }
        assertThat(sensitiveMemoId).isNotNull();
        assertThat(requestAmountId).isNotNull();
        assertThat(supportTypesId).isNotNull();
        return new RequirementIds(sensitiveMemoId, requestAmountId, supportTypesId);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param result 입력 값
     *
     * @param path 입력 값
     *
     * @return 처리 결과
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    private UUID selectUuid(MvcResult result, String... path) throws Exception {
        JsonNode node = readJson(result);
        for (String segment : path) {
            node = segment.chars().allMatch(Character::isDigit)
                    ? node.path(Integer.parseInt(segment))
                    : node.path(segment);
        }
        return UUID.fromString(node.asText());
    }

    /**
     * 입력 데이터를 읽어 처리합니다.
     *
     * @param result 입력 값
     *
     * @return 처리 결과
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param progressId 입력 값
     *
     * @return 처리 결과
     */
    private long selectApplicationInputValueCount(UUID progressId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM application_input_values
                        WHERE progress_id = ?
                        """,
                Long.class,
                progressId
        );
        return count == null ? 0 : count;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param progressId 입력 값
     *
     * @return 처리 결과
     */
    private long selectApplicationInputOptionValueCount(UUID progressId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM application_input_values
                        WHERE progress_id = ?
                          AND option_code IS NOT NULL
                        """,
                Long.class,
                progressId
        );
        return count == null ? 0 : count;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param progressId 입력 값
     *
     * @param privateValue 입력 값
     *
     * @return 처리 결과
     */
    private long selectAuditPrivacyLeakCount(UUID progressId, String privateValue) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM audit_logs
                        WHERE resource_type = 'APPLICATION_PROGRESS'
                          AND resource_id = ?
                          AND metadata_json::text ILIKE concat('%', ?, '%')
                        """,
                Long.class,
                progressId,
                privateValue
        );
        return count == null ? 0 : count;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param progressId 입력 값
     *
     * @return 처리 결과
     */
    private long selectApprovedProgressCount(UUID progressId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM application_progresses
                        WHERE id = ?
                          AND status_code = 'APPROVED'
                          AND result_code = 'APPROVED'
                          AND received_amount = ?
                        """,
                Long.class,
                progressId,
                RECEIVED_AMOUNT
        );
        return count == null ? 0 : count;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param title 입력 값
     *
     * @return 처리 결과
     */
    private String announcementRequest(String title) {
        return """
                {
                  "targetTypeCode": "BUSINESS",
                  "title": "%s",
                  "agencyName": "Backend Rehearsal Agency",
                  "summary": "Backend rehearsal announcement",
                  "applicationStartDate": "2026-06-01",
                  "applicationEndDate": "2026-06-30",
                  "incomeJudgementCode": "VAT_TAX_BASE_ONLY",
                  "minAmount": 1000000,
                  "maxAmount": 3000000,
                  "options": [
                    {
                      "optionGroupCode": "PAYMENT_METHOD",
                      "optionCode": "CASH"
                    }
                  ]
                }
                """.formatted(title);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private String conditionsRequest() {
        return """
                {
                  "industryConditions": [
                    {
                      "conditionTypeCode": "INCLUDE",
                      "ksicCode": "47911"
                    }
                  ],
                  "numericConditions": [
                    {
                      "conditionScopeCode": "BUSINESS",
                      "conditionKey": "ANNUAL_REVENUE",
                      "comparatorCode": "LTE",
                      "valueNumber": 300000000,
                      "unitCode": "KRW"
                    }
                  ],
                  "optionConditions": [
                    {
                      "conditionScopeCode": "BUSINESS",
                      "conditionKey": "BUSINESS_TYPE",
                      "optionCode": "SOLE_PROPRIETOR"
                    }
                  ],
                  "documentRequirements": [
                    {
                      "documentTypeCode": "BUSINESS_REGISTRATION",
                      "required": true,
                      "sortOrder": 1
                    }
                  ]
                }
                """;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private String stepsRequest() {
        return """
                {
                  "steps": [
                    {
                      "stepOrder": 1,
                      "stepName": "Submit Application",
                      "guideMessage": "Submit dynamic input values.",
                      "actionGuide": "Complete required input values before submit.",
                      "completionConditionCode": "BUTTON_CLICK",
                      "nextConditionCode": null,
                      "active": true,
                      "buttons": [
                        {
                          "buttonCode": "SUBMIT_APPLICATION",
                          "buttonLabel": "Submit",
                          "buttonActionCode": "COMPLETE_STEP",
                          "sortOrder": 1
                        }
                      ],
                      "documents": []
                    }
                  ]
                }
                """;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private String inputRequirementsRequest() {
        return """
                {
                  "requirements": [
                    {
                      "fieldKey": "SENSITIVE_MEMO",
                      "fieldLabel": "Sensitive memo",
                      "fieldTypeCode": "TEXT",
                      "scopeCode": "APPLICATION",
                      "required": true,
                      "sensitive": true,
                      "sortOrder": 1,
                      "helpText": "Do not write raw values to audit metadata.",
                      "options": []
                    },
                    {
                      "fieldKey": "REQUEST_AMOUNT",
                      "fieldLabel": "Request amount",
                      "fieldTypeCode": "AMOUNT",
                      "scopeCode": "SUPPORT",
                      "required": true,
                      "sensitive": false,
                      "sortOrder": 2,
                      "helpText": "Requested amount",
                      "options": []
                    },
                    {
                      "fieldKey": "SUPPORT_TYPES",
                      "fieldLabel": "Support types",
                      "fieldTypeCode": "MULTI_SELECT",
                      "scopeCode": "SUPPORT",
                      "required": true,
                      "sensitive": false,
                      "sortOrder": 3,
                      "helpText": "Multiple support types",
                      "options": [
                        {
                          "optionCode": "TRAINING",
                          "optionLabel": "Training",
                          "sortOrder": 1
                        },
                        {
                          "optionCode": "CONSULTING",
                          "optionLabel": "Consulting",
                          "sortOrder": 2
                        }
                      ]
                    }
                  ]
                }
                """;
    }

    private record RequirementIds(
            UUID sensitiveMemoId,
            UUID requestAmountId,
            UUID supportTypesId
    ) {
    }
}
