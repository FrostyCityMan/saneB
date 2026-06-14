package com.saneb.domain.dynamicinput.controller;

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
import java.time.LocalDate;
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

@EnabledIfEnvironmentVariable(named = "SANEB_DYNAMIC_INPUT_SMOKE", matches = "true")
@ActiveProfiles("local")
@SpringBootTest
@AutoConfigureMockMvc
class DynamicAnnouncementInputSmokeIntegrationTest {

    private static final UUID LOCAL_OPERATOR_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final String TEST_PASSWORD_HASH = "$2a$10$InQi9a3ehghCfxu2Z59DiegEEW4pfhxb4h19PCJb58D0/1OWmmQ2y";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void localOperatorSavesDynamicRequirementsAndProgressInputValues() throws Exception {
        MockHttpSession session = loginLocalOperator();
        DynamicInputFixture fixture = insertFixture();

        MvcResult requirementsResult = mockMvc.perform(put("/api/v1/announcements/{announcementId}/input-requirements", fixture.announcementId())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requirements": [
                                    {
                                      "fieldKey": "PRIVATE_MEMO",
                                      "fieldLabel": "민감 메모",
                                      "fieldTypeCode": "TEXT",
                                      "scopeCode": "APPLICATION",
                                      "required": true,
                                      "sensitive": true,
                                      "sortOrder": 1,
                                      "helpText": "감사 로그 원문 저장 금지",
                                      "options": []
                                    },
                                    {
                                      "fieldKey": "REQUEST_AMOUNT",
                                      "fieldLabel": "신청 금액",
                                      "fieldTypeCode": "AMOUNT",
                                      "scopeCode": "SUPPORT",
                                      "required": true,
                                      "sensitive": false,
                                      "sortOrder": 2,
                                      "helpText": "신청 예정 금액",
                                      "options": []
                                    },
                                    {
                                      "fieldKey": "APPLICATION_METHOD",
                                      "fieldLabel": "신청 방법",
                                      "fieldTypeCode": "SELECT",
                                      "scopeCode": "APPLICATION",
                                      "required": true,
                                      "sensitive": false,
                                      "sortOrder": 3,
                                      "helpText": "신청 방법 선택",
                                      "options": [
                                        {
                                          "optionCode": "ONLINE",
                                          "optionLabel": "온라인",
                                          "sortOrder": 1
                                        },
                                        {
                                          "optionCode": "VISIT",
                                          "optionLabel": "방문",
                                          "sortOrder": 2
                                        }
                                      ]
                                    },
                                    {
                                      "fieldKey": "SUPPORT_TYPES",
                                      "fieldLabel": "희망 지원 항목",
                                      "fieldTypeCode": "MULTI_SELECT",
                                      "scopeCode": "SUPPORT",
                                      "required": false,
                                      "sensitive": false,
                                      "sortOrder": 4,
                                      "helpText": "복수 선택",
                                      "options": [
                                        {
                                          "optionCode": "TRAINING",
                                          "optionLabel": "교육",
                                          "sortOrder": 1
                                        },
                                        {
                                          "optionCode": "CONSULTING",
                                          "optionLabel": "컨설팅",
                                          "sortOrder": 2
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requirements").value(org.hamcrest.Matchers.hasSize(4)))
                .andExpect(jsonPath("$.data.requirements[0].fieldKey").value("PRIVATE_MEMO"))
                .andReturn();

        RequirementIds requirementIds = selectRequirementIds(requirementsResult);
        assertThat(selectRequirementCount(fixture.announcementId())).isEqualTo(4);
        assertThat(selectOptionCount(fixture.announcementId())).isEqualTo(4);

        mockMvc.perform(patch("/api/v1/application-progresses/{progressId}/steps/{stepId}/action", fixture.progressId(), fixture.stepId())
                        .session(session)
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

        String privateText = "private-address-should-not-enter-audit";
        mockMvc.perform(put("/api/v1/application-progresses/{progressId}/input-values", fixture.progressId())
                        .session(session)
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
                                      "valueNumber": 3000000
                                    },
                                    {
                                      "requirementId": "%s",
                                      "optionCode": "ONLINE"
                                    },
                                    {
                                      "requirementId": "%s",
                                      "optionCodes": ["TRAINING", "CONSULTING"]
                                    }
                                  ]
                                }
                                """.formatted(
                                requirementIds.privateMemoId(),
                                privateText,
                                requirementIds.requestAmountId(),
                                requirementIds.applicationMethodId(),
                                requirementIds.supportTypesId()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.values").value(org.hamcrest.Matchers.hasSize(4)))
                .andExpect(jsonPath("$.data.values[0].valueText").value(privateText))
                .andExpect(jsonPath("$.data.values[3].optionCodes").value(org.hamcrest.Matchers.hasSize(2)));

        assertThat(selectInputValueCount(fixture.progressId())).isEqualTo(5);
        assertThat(selectInputValueOptionCount(fixture.progressId())).isEqualTo(3);
        assertThat(selectInputAuditCount(fixture.progressId())).isEqualTo(1);
        assertThat(selectAuditPrivacyLeakCount(fixture.progressId(), privateText)).isZero();

        mockMvc.perform(get("/api/v1/application-progresses/{progressId}/input-values", fixture.progressId())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.values[0].fieldKey").value("PRIVATE_MEMO"))
                .andExpect(jsonPath("$.data.values[2].optionCode").value("ONLINE"));

        mockMvc.perform(patch("/api/v1/application-progresses/{progressId}/steps/{stepId}/action", fixture.progressId(), fixture.stepId())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "buttonCode": "SUBMIT_APPLICATION",
                                  "input": {
                                    "memo": "now requirements are complete"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("WAITING_RESULT"));

        mockMvc.perform(put("/api/v1/announcements/{announcementId}/input-requirements", fixture.announcementId())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requirements": [
                                    {
                                      "fieldKey": "PRIVATE_MEMO",
                                      "fieldLabel": "민감 메모",
                                      "fieldTypeCode": "NUMBER",
                                      "scopeCode": "APPLICATION",
                                      "required": true,
                                      "sensitive": true,
                                      "sortOrder": 1,
                                      "helpText": "불변 필드 변경 차단",
                                      "options": []
                                    },
                                    {
                                      "fieldKey": "REQUEST_AMOUNT",
                                      "fieldLabel": "신청 금액",
                                      "fieldTypeCode": "AMOUNT",
                                      "scopeCode": "SUPPORT",
                                      "required": true,
                                      "sensitive": false,
                                      "sortOrder": 2,
                                      "helpText": "신청 예정 금액",
                                      "options": []
                                    },
                                    {
                                      "fieldKey": "APPLICATION_METHOD",
                                      "fieldLabel": "신청 방법",
                                      "fieldTypeCode": "SELECT",
                                      "scopeCode": "APPLICATION",
                                      "required": true,
                                      "sensitive": false,
                                      "sortOrder": 3,
                                      "helpText": "신청 방법 선택",
                                      "options": [
                                        {
                                          "optionCode": "ONLINE",
                                          "optionLabel": "온라인",
                                          "sortOrder": 1
                                        },
                                        {
                                          "optionCode": "VISIT",
                                          "optionLabel": "방문",
                                          "sortOrder": 2
                                        }
                                      ]
                                    },
                                    {
                                      "fieldKey": "SUPPORT_TYPES",
                                      "fieldLabel": "희망 지원 항목",
                                      "fieldTypeCode": "MULTI_SELECT",
                                      "scopeCode": "SUPPORT",
                                      "required": false,
                                      "sensitive": false,
                                      "sortOrder": 4,
                                      "helpText": "복수 선택",
                                      "options": [
                                        {
                                          "optionCode": "TRAINING",
                                          "optionLabel": "교육",
                                          "sortOrder": 1
                                        },
                                        {
                                          "optionCode": "CONSULTING",
                                          "optionLabel": "컨설팅",
                                          "sortOrder": 2
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.errorCode").value("VALIDATION_FAILED"));
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
        throw new IllegalStateException("local_operator login failed for dynamic input smoke.");
    }

    private DynamicInputFixture insertFixture() {
        String fixtureKey = UUID.randomUUID().toString();
        UUID memberUserId = UUID.randomUUID();
        UUID announcementId = UUID.randomUUID();
        UUID verificationId = UUID.randomUUID();
        UUID matchingCaseId = UUID.randomUUID();
        UUID stepId = UUID.randomUUID();
        UUID progressId = UUID.randomUUID();

        insertMemberUser(memberUserId, "dynamic_input_" + fixtureKey.replace("-", ""));
        insertAnnouncement(announcementId, "Dynamic Input Gate " + fixtureKey);
        insertStep(stepId, announcementId);
        insertStepButton(stepId);
        insertVerification(verificationId, memberUserId);
        insertMatchingCase(matchingCaseId, announcementId, memberUserId, verificationId);
        insertApplicationProgress(progressId, matchingCaseId, announcementId, memberUserId, stepId);
        return new DynamicInputFixture(announcementId, progressId, stepId);
    }

    private void insertMemberUser(UUID memberUserId, String loginId) {
        jdbcTemplate.update(
                """
                        INSERT INTO users (
                            id,
                            login_id,
                            password_hash,
                            name,
                            status_code,
                            password_reset_required,
                            created_at,
                            updated_at
                        ) VALUES (?, ?, ?, ?, 'ACTIVE', false, now(), now())
                        """,
                memberUserId,
                loginId,
                TEST_PASSWORD_HASH,
                "Dynamic Input Smoke User"
        );
        jdbcTemplate.update(
                """
                        INSERT INTO user_roles (
                            user_id,
                            role_code,
                            created_at
                        ) VALUES (?, 'USER', now())
                        """,
                memberUserId
        );
    }

    private void insertAnnouncement(UUID announcementId, String title) {
        jdbcTemplate.update(
                """
                        INSERT INTO announcements (
                            id,
                            target_type_code,
                            title,
                            agency_name,
                            summary,
                            application_start_date,
                            application_end_date,
                            manual_status_code,
                            approval_status_code,
                            income_judgement_code,
                            min_amount,
                            max_amount,
                            created_by,
                            updated_by
                        ) VALUES (
                            ?, 'BUSINESS', ?, 'Dynamic Input Gate Agency', 'Dynamic input smoke announcement',
                            ?, ?, 'NORMAL', 'APPROVED', 'NO_LIMIT', 1000000, 5000000, ?, ?
                        )
                        """,
                announcementId,
                title,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                LOCAL_OPERATOR_ID,
                LOCAL_OPERATOR_ID
        );
    }

    private void insertStep(UUID stepId, UUID announcementId) {
        jdbcTemplate.update(
                """
                        INSERT INTO announcement_progress_steps (
                            id,
                            announcement_id,
                            step_order,
                            step_name,
                            guide_message,
                            action_guide,
                            completion_condition_code,
                            next_condition_code,
                            is_active,
                            created_by,
                            updated_by
                        ) VALUES (?, ?, 1, 'Dynamic Input Submit', 'Guide', 'Action', 'BUTTON_CLICK', null, true, ?, ?)
                        """,
                stepId,
                announcementId,
                LOCAL_OPERATOR_ID,
                LOCAL_OPERATOR_ID
        );
    }

    private void insertStepButton(UUID stepId) {
        jdbcTemplate.update(
                """
                        INSERT INTO announcement_step_buttons (
                            step_id,
                            button_code,
                            button_label,
                            button_action_code,
                            next_step_id,
                            sort_order,
                            created_by,
                            updated_by
                        ) VALUES (?, 'SUBMIT_APPLICATION', '제출', 'COMPLETE_STEP', null, 1, ?, ?)
                        """,
                stepId,
                LOCAL_OPERATOR_ID,
                LOCAL_OPERATOR_ID
        );
    }

    private void insertVerification(UUID verificationId, UUID memberUserId) {
        jdbcTemplate.update(
                """
                        INSERT INTO partner_verifications (
                            id,
                            member_user_id,
                            partner_user_id,
                            status_code,
                            is_current,
                            is_matching_blocked,
                            submitted_at,
                            verified_at,
                            reviewed_by,
                            created_by,
                            updated_by
                        ) VALUES (?, ?, ?, 'VERIFIED', true, false, now(), now(), ?, ?, ?)
                        """,
                verificationId,
                memberUserId,
                LOCAL_OPERATOR_ID,
                LOCAL_OPERATOR_ID,
                LOCAL_OPERATOR_ID,
                LOCAL_OPERATOR_ID
        );
    }

    private void insertMatchingCase(UUID matchingCaseId, UUID announcementId, UUID memberUserId, UUID verificationId) {
        jdbcTemplate.update(
                """
                        INSERT INTO matching_cases (
                            id,
                            announcement_id,
                            member_user_id,
                            verification_id,
                            status_code,
                            matching_stage_code,
                            matching_basis_code,
                            matched_at,
                            created_by,
                            updated_by
                        ) VALUES (?, ?, ?, ?, 'PROGRESSED', 'FINAL', 'DOCUMENT_INPUT', now(), ?, ?)
                        """,
                matchingCaseId,
                announcementId,
                memberUserId,
                verificationId,
                LOCAL_OPERATOR_ID,
                LOCAL_OPERATOR_ID
        );
    }

    private void insertApplicationProgress(
            UUID progressId,
            UUID matchingCaseId,
            UUID announcementId,
            UUID memberUserId,
            UUID stepId
    ) {
        jdbcTemplate.update(
                """
                        INSERT INTO application_progresses (
                            id,
                            matching_case_id,
                            announcement_id,
                            member_user_id,
                            current_step_id,
                            status_code,
                            created_by,
                            updated_by
                        ) VALUES (?, ?, ?, ?, ?, 'READY', ?, ?)
                        """,
                progressId,
                matchingCaseId,
                announcementId,
                memberUserId,
                stepId,
                LOCAL_OPERATOR_ID,
                LOCAL_OPERATOR_ID
        );
        jdbcTemplate.update(
                """
                        INSERT INTO application_step_states (
                            progress_id,
                            step_id,
                            status_code,
                            started_at,
                            created_by,
                            updated_by
                        ) VALUES (?, ?, 'READY', now(), ?, ?)
                        """,
                progressId,
                stepId,
                LOCAL_OPERATOR_ID,
                LOCAL_OPERATOR_ID
        );
    }

    private RequirementIds selectRequirementIds(MvcResult result) throws Exception {
        JsonNode requirements = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data")
                .path("requirements");
        UUID privateMemoId = null;
        UUID requestAmountId = null;
        UUID applicationMethodId = null;
        UUID supportTypesId = null;
        for (JsonNode requirement : requirements) {
            String fieldKey = requirement.path("fieldKey").asText();
            UUID requirementId = UUID.fromString(requirement.path("requirementId").asText());
            if ("PRIVATE_MEMO".equals(fieldKey)) {
                privateMemoId = requirementId;
            } else if ("REQUEST_AMOUNT".equals(fieldKey)) {
                requestAmountId = requirementId;
            } else if ("APPLICATION_METHOD".equals(fieldKey)) {
                applicationMethodId = requirementId;
            } else if ("SUPPORT_TYPES".equals(fieldKey)) {
                supportTypesId = requirementId;
            }
        }
        return new RequirementIds(privateMemoId, requestAmountId, applicationMethodId, supportTypesId);
    }

    private long selectRequirementCount(UUID announcementId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM announcement_input_requirements
                        WHERE announcement_id = ?
                        """,
                Long.class,
                announcementId
        );
        return count == null ? 0 : count;
    }

    private long selectOptionCount(UUID announcementId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM announcement_input_options aio
                        INNER JOIN announcement_input_requirements air ON air.id = aio.requirement_id
                        WHERE air.announcement_id = ?
                        """,
                Long.class,
                announcementId
        );
        return count == null ? 0 : count;
    }

    private long selectInputValueCount(UUID progressId) {
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

    private long selectInputValueOptionCount(UUID progressId) {
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

    private long selectInputAuditCount(UUID progressId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM audit_logs
                        WHERE resource_type = 'APPLICATION_PROGRESS'
                          AND resource_id = ?
                          AND action_code = 'APPLICATION_INPUT_VALUES_SAVE'
                        """,
                Long.class,
                progressId
        );
        return count == null ? 0 : count;
    }

    private long selectAuditPrivacyLeakCount(UUID progressId, String privateText) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM audit_logs
                        WHERE resource_type = 'APPLICATION_PROGRESS'
                          AND resource_id = ?
                          AND (
                              metadata_json::text ILIKE concat('%', ?, '%')
                              OR metadata_json::text ILIKE '%address%'
                              OR metadata_json::text ILIKE '%phone%'
                              OR metadata_json::text ILIKE '%password%'
                          )
                        """,
                Long.class,
                progressId,
                privateText
        );
        return count == null ? 0 : count;
    }

    private record DynamicInputFixture(
            UUID announcementId,
            UUID progressId,
            UUID stepId
    ) {
    }

    private record RequirementIds(
            UUID privateMemoId,
            UUID requestAmountId,
            UUID applicationMethodId,
            UUID supportTypesId
    ) {
    }
}
