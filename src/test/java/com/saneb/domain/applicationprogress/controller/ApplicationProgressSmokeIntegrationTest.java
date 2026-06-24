/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ApplicationProgressSmokeIntegrationTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.applicationprogress.controller;

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

@EnabledIfEnvironmentVariable(named = "SANEB_APPLICATION_PROGRESS_SMOKE", matches = "true")
@ActiveProfiles("local")
@SpringBootTest
@AutoConfigureMockMvc
class ApplicationProgressSmokeIntegrationTest {

    private static final UUID LOCAL_OPERATOR_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final String TEST_PASSWORD_HASH = "$2a$10$InQi9a3ehghCfxu2Z59DiegEEW4pfhxb4h19PCJb58D0/1OWmmQ2y";

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
    void localOperatorCreatesAndCompletesApplicationProgressFlow() throws Exception {
        MockHttpSession session = loginLocalOperator();
        ProgressFixture fixture = insertFixture();

        MvcResult createResult = mockMvc.perform(post("/api/v1/application-progresses")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "matchingCaseId": "%s"
                                }
                                """.formatted(fixture.matchingCaseId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.matchingCaseId").value(fixture.matchingCaseId().toString()))
                .andExpect(jsonPath("$.data.currentStepId").value(fixture.firstStepId().toString()))
                .andExpect(jsonPath("$.data.statusCode").value("READY"))
                .andExpect(jsonPath("$.data.stepStates[0].statusCode").value("READY"))
                .andExpect(jsonPath("$.data.stepStates[1].statusCode").value("LOCKED"))
                .andReturn();

        UUID progressId = selectProgressId(createResult);
        assertThat(selectProgressCount(fixture.matchingCaseId())).isEqualTo(1);
        assertThat(selectMatchingStatus(fixture.matchingCaseId())).isEqualTo("PROGRESSED");
        assertThat(selectStepStateCount(progressId, "READY")).isEqualTo(1);
        assertThat(selectStepStateCount(progressId, "LOCKED")).isEqualTo(1);

        mockMvc.perform(post("/api/v1/application-progresses")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "matchingCaseId": "%s"
                                }
                                """.formatted(fixture.matchingCaseId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.progressId").value(progressId.toString()));
        assertThat(selectProgressCount(fixture.matchingCaseId())).isEqualTo(1);

        mockMvc.perform(put("/api/v1/application-progresses/{progressId}/steps/{stepId}/documents", progressId, fixture.firstStepId())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documents": [
                                    {
                                      "stepDocumentId": "%s",
                                      "checked": true
                                    }
                                  ]
                                }
                                """.formatted(fixture.stepDocumentId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.checklists[0].checked").value(true));

        mockMvc.perform(patch("/api/v1/application-progresses/{progressId}/steps/{stepId}/action", progressId, fixture.firstStepId())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "buttonCode": "WANTS_TO_PROGRESS",
                                  "input": {
                                    "address": "private address must not be stored"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.currentStepId").value(fixture.secondStepId().toString()));
        assertThat(selectSingleStepState(progressId, fixture.firstStepId())).isEqualTo("COMPLETED");
        assertThat(selectSingleStepState(progressId, fixture.secondStepId())).isEqualTo("READY");

        mockMvc.perform(patch("/api/v1/application-progresses/{progressId}/steps/{stepId}/action", progressId, fixture.secondStepId())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "buttonCode": "SUBMIT_RESULT",
                                  "input": {
                                    "memo": "result wait"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("WAITING_RESULT"))
                .andExpect(jsonPath("$.data.currentStepId").value(org.hamcrest.Matchers.nullValue()));

        mockMvc.perform(patch("/api/v1/application-progresses/{progressId}/receipt", progressId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiptNo": "A-2026-0001",
                                  "receiptDate": "2026-06-15"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.receiptNo").value("A-2026-0001"));

        mockMvc.perform(patch("/api/v1/application-progresses/{progressId}/result", progressId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resultCode": "APPROVED",
                                  "resultNote": "Approved by smoke",
                                  "resultDate": "2026-07-01",
                                  "receivedAmount": 7000000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("APPROVED"))
                .andExpect(jsonPath("$.data.resultCode").value("APPROVED"))
                .andExpect(jsonPath("$.data.receivedAmount").value(7000000));

        mockMvc.perform(get("/api/v1/application-progresses/{progressId}", progressId)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stepStates[0].statusCode").value("COMPLETED"))
                .andExpect(jsonPath("$.data.stepStates[1].statusCode").value("COMPLETED"));

        mockMvc.perform(get("/api/v1/application-progresses")
                        .session(session)
                        .param("matchingCaseId", fixture.matchingCaseId().toString())
                        .param("statusCode", "APPROVED")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(1));

        assertThat(selectReceivedAmount(progressId)).isEqualByComparingTo(new BigDecimal("7000000.00"));
        assertThat(selectChecklistCheckedCount(progressId)).isEqualTo(1);
        assertThat(selectActionLogCount(progressId)).isEqualTo(2);
        assertThat(selectActionLogPrivacyLeakCount(progressId)).isEqualTo(0);
        assertThat(selectAuditCount(progressId)).isEqualTo(7);
        assertThat(selectAuditMetadataPrivacyLeakCount(progressId)).isEqualTo(0);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
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
        throw new IllegalStateException("local_operator login failed for application progress smoke.");
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @return 처리 결과
     */
    private ProgressFixture insertFixture() {
        String fixtureKey = UUID.randomUUID().toString();
        UUID memberUserId = UUID.randomUUID();
        UUID announcementId = UUID.randomUUID();
        UUID verificationId = UUID.randomUUID();
        UUID matchingCaseId = UUID.randomUUID();
        UUID firstStepId = UUID.randomUUID();
        UUID secondStepId = UUID.randomUUID();
        UUID stepDocumentId = UUID.randomUUID();

        insertMemberUser(memberUserId, "progress_" + fixtureKey.replace("-", ""));
        insertAnnouncement(announcementId, "Application Progress Gate " + fixtureKey);
        insertStep(secondStepId, announcementId, 2, "Submit Result", null);
        insertStep(firstStepId, announcementId, 1, "Guide Sent", secondStepId);
        insertStepDocument(stepDocumentId, firstStepId);
        insertStepButton(firstStepId, "WANTS_TO_PROGRESS", "MOVE_NEXT", secondStepId);
        insertStepButton(secondStepId, "SUBMIT_RESULT", "COMPLETE_STEP", null);
        insertVerification(verificationId, memberUserId);
        insertMatchingCase(matchingCaseId, announcementId, memberUserId, verificationId);
        return new ProgressFixture(matchingCaseId, firstStepId, secondStepId, stepDocumentId);
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param memberUserId 입력 값
     *
     * @param loginId 입력 값
     */
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
                "Progress Smoke User"
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

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param announcementId 입력 값
     *
     * @param title 입력 값
     */
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
                            ?, 'BUSINESS', ?, 'Progress Gate Agency', 'Application progress smoke announcement',
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

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param stepId 입력 값
     *
     * @param announcementId 입력 값
     *
     * @param stepOrder 입력 값
     *
     * @param stepName 입력 값
     *
     * @param nextStepId 입력 값
     */
    private void insertStep(UUID stepId, UUID announcementId, int stepOrder, String stepName, UUID nextStepId) {
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
                        ) VALUES (?, ?, ?, ?, 'Guide', 'Action', 'BUTTON_CLICK', 'NEXT', true, ?, ?)
                        """,
                stepId,
                announcementId,
                stepOrder,
                stepName,
                LOCAL_OPERATOR_ID,
                LOCAL_OPERATOR_ID
        );
        if (nextStepId != null) {
            jdbcTemplate.update(
                    """
                            UPDATE announcement_progress_steps
                            SET next_condition_code = 'NEXT',
                                updated_by = ?,
                                updated_at = now()
                            WHERE id = ?
                            """,
                    LOCAL_OPERATOR_ID,
                    stepId
            );
        }
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param stepDocumentId 입력 값
     *
     * @param stepId 입력 값
     */
    private void insertStepDocument(UUID stepDocumentId, UUID stepId) {
        jdbcTemplate.update(
                """
                        INSERT INTO announcement_step_documents (
                            id,
                            step_id,
                            document_type_code,
                            is_required,
                            sort_order,
                            created_by,
                            updated_by
                        ) VALUES (?, ?, 'BUSINESS_REGISTRATION', true, 1, ?, ?)
                        """,
                stepDocumentId,
                stepId,
                LOCAL_OPERATOR_ID,
                LOCAL_OPERATOR_ID
        );
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param stepId 입력 값
     *
     * @param buttonCode 입력 값
     *
     * @param buttonActionCode 입력 값
     *
     * @param nextStepId 입력 값
     */
    private void insertStepButton(UUID stepId, String buttonCode, String buttonActionCode, UUID nextStepId) {
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
                        ) VALUES (?, ?, ?, ?, ?, 1, ?, ?)
                        """,
                stepId,
                buttonCode,
                buttonCode,
                buttonActionCode,
                nextStepId,
                LOCAL_OPERATOR_ID,
                LOCAL_OPERATOR_ID
        );
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param verificationId 입력 값
     *
     * @param memberUserId 입력 값
     */
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

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param matchingCaseId 입력 값
     *
     * @param announcementId 입력 값
     *
     * @param memberUserId 입력 값
     *
     * @param verificationId 입력 값
     */
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
                        ) VALUES (?, ?, ?, ?, 'MATCHED', 'FINAL', 'DOCUMENT_INPUT', now(), ?, ?)
                        """,
                matchingCaseId,
                announcementId,
                memberUserId,
                verificationId,
                LOCAL_OPERATOR_ID,
                LOCAL_OPERATOR_ID
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
    private UUID selectProgressId(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return UUID.fromString(root.path("data").path("progressId").asText());
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param matchingCaseId 입력 값
     *
     * @return 처리 결과
     */
    private long selectProgressCount(UUID matchingCaseId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM application_progresses
                        WHERE matching_case_id = ?
                        """,
                Long.class,
                matchingCaseId
        );
        return count == null ? 0 : count;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param matchingCaseId 입력 값
     *
     * @return 처리 결과
     */
    private String selectMatchingStatus(UUID matchingCaseId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT status_code
                        FROM matching_cases
                        WHERE id = ?
                        """,
                String.class,
                matchingCaseId
        );
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param progressId 입력 값
     *
     * @param statusCode 입력 값
     *
     * @return 처리 결과
     */
    private long selectStepStateCount(UUID progressId, String statusCode) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM application_step_states
                        WHERE progress_id = ?
                          AND status_code = ?
                        """,
                Long.class,
                progressId,
                statusCode
        );
        return count == null ? 0 : count;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param progressId 입력 값
     *
     * @param stepId 입력 값
     *
     * @return 처리 결과
     */
    private String selectSingleStepState(UUID progressId, UUID stepId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT status_code
                        FROM application_step_states
                        WHERE progress_id = ?
                          AND step_id = ?
                        """,
                String.class,
                progressId,
                stepId
        );
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param progressId 입력 값
     *
     * @return 처리 결과
     */
    private BigDecimal selectReceivedAmount(UUID progressId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT received_amount
                        FROM application_progresses
                        WHERE id = ?
                          AND result_code = 'APPROVED'
                          AND status_code = 'APPROVED'
                        """,
                BigDecimal.class,
                progressId
        );
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param progressId 입력 값
     *
     * @return 처리 결과
     */
    private long selectChecklistCheckedCount(UUID progressId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM application_step_checklists
                        WHERE progress_id = ?
                          AND is_checked = true
                          AND checked_by IS NOT NULL
                          AND checked_at IS NOT NULL
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
    private long selectActionLogCount(UUID progressId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM application_action_logs
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
    private long selectActionLogPrivacyLeakCount(UUID progressId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM application_action_logs
                        WHERE progress_id = ?
                          AND input_json::text ILIKE '%address%'
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
    private long selectAuditCount(UUID progressId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM audit_logs
                        WHERE resource_type = 'APPLICATION_PROGRESS'
                          AND resource_id = ?
                          AND action_code IN (
                              'APPLICATION_PROGRESS_CREATE',
                              'APPLICATION_PROGRESS_DOCUMENTS_SAVE',
                              'APPLICATION_PROGRESS_STEP_ACTION',
                              'APPLICATION_PROGRESS_RECEIPT_SAVE',
                              'APPLICATION_PROGRESS_RESULT_SAVE'
                          )
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
    private long selectAuditMetadataPrivacyLeakCount(UUID progressId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM audit_logs
                        WHERE resource_type = 'APPLICATION_PROGRESS'
                          AND resource_id = ?
                          AND (
                              metadata_json::text ILIKE '%address%'
                              OR metadata_json::text ILIKE '%phone%'
                              OR metadata_json::text ILIKE '%password%'
                          )
                        """,
                Long.class,
                progressId
        );
        return count == null ? 0 : count;
    }

    private record ProgressFixture(
            UUID matchingCaseId,
            UUID firstStepId,
            UUID secondStepId,
            UUID stepDocumentId
    ) {
    }
}
