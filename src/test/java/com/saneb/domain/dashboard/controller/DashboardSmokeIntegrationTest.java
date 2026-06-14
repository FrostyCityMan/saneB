package com.saneb.domain.dashboard.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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

@EnabledIfEnvironmentVariable(named = "SANEB_DASHBOARD_SMOKE", matches = "true")
@ActiveProfiles("local")
@SpringBootTest
@AutoConfigureMockMvc
class DashboardSmokeIntegrationTest {

    private static final UUID LOCAL_USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID LOCAL_OPERATOR_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID LOCAL_MATCH_PROGRESS_ID = UUID.fromString("70000000-0000-0000-0000-000000000003");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void localSeedUserDashboardReadsMatchingAndProgressDataFromDatabase() throws Exception {
        DashboardFixture fixture = insertDashboardFixture();
        MockHttpSession session = loginLocalUser();

        mockMvc.perform(get("/api/v1/dashboard/me/summary").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.serviceStatusCode").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.candidateCounts.policyFund").value(1))
                .andExpect(jsonPath("$.data.candidateCounts.supportFund").value(1))
                .andExpect(jsonPath("$.data.candidateCounts.subsidy").value(1))
                .andExpect(jsonPath("$.data.finalMatchedCount").value(2))
                .andExpect(jsonPath("$.data.supportAmountRange.minAmount").value(1000000))
                .andExpect(jsonPath("$.data.supportAmountRange.maxAmount").value(7000000))
                .andExpect(jsonPath("$.data.supportAmountRange.basisCode").value("ANNOUNCEMENT_AMOUNT_RANGE"))
                .andExpect(jsonPath("$.data.verificationStatusCode").value("VERIFIED"));

        mockMvc.perform(get("/api/v1/dashboard/me/current-action").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actionCode").value("PROGRESS_ACTION_REQUIRED"))
                .andExpect(jsonPath("$.data.title").value("Dashboard Step"))
                .andExpect(jsonPath("$.data.description").value("Select the next dashboard action."))
                .andExpect(jsonPath("$.data.primaryButtonLabel").value("Continue"))
                .andExpect(jsonPath("$.data.dueDate").value("2026-06-30"));

        mockMvc.perform(get("/api/v1/dashboard/me/progress-summary").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.inProgressCount").value(1))
                .andExpect(jsonPath("$.data.waitingResultCount").value(1))
                .andExpect(jsonPath("$.data.approvedCount").value(1))
                .andExpect(jsonPath("$.data.supplementRequestedCount").value(0))
                .andExpect(jsonPath("$.data.stoppedCount").value(0))
                .andExpect(jsonPath("$.data.totalReceivedAmount").value(7000000));

        mockMvc.perform(get("/api/v1/dashboard/me/reverification-status").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.required").value(false))
                .andExpect(jsonPath("$.data.reasonCode").doesNotExist());

        assertMatchingCasesRemainOwnedByFixture(fixture);
    }

    @Test
    void localSeedMatchUserCanOpenSeededMatchingProgress() throws Exception {
        MockHttpSession session = login("local_match_user");

        mockMvc.perform(get("/api/v1/dashboard/me/summary").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.serviceStatusCode").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.candidateCounts.policyFund").value(1))
                .andExpect(jsonPath("$.data.finalMatchedCount").value(1))
                .andExpect(jsonPath("$.data.supportAmountRange.minAmount").value(2500000))
                .andExpect(jsonPath("$.data.supportAmountRange.maxAmount").value(6000000))
                .andExpect(jsonPath("$.data.verificationStatusCode").value("VERIFIED"));

        mockMvc.perform(get("/api/v1/dashboard/me/current-action").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actionCode").value("PROGRESS_ACTION_REQUIRED"))
                .andExpect(jsonPath("$.data.title").value("진행 의사 확인"))
                .andExpect(jsonPath("$.data.primaryButtonLabel").value("진행 원함"))
                .andExpect(jsonPath("$.data.route").value("/app/application-progresses/" + LOCAL_MATCH_PROGRESS_ID));

        mockMvc.perform(get("/app/application-progresses/{progressId}", LOCAL_MATCH_PROGRESS_ID).session(session))
                .andExpect(status().isOk());
    }

    private DashboardFixture insertDashboardFixture() {
        deleteDashboardFixtureRows();

        UUID verificationId = UUID.randomUUID();
        UUID policyAnnouncementId = UUID.randomUUID();
        UUID supportAnnouncementId = UUID.randomUUID();
        UUID subsidyAnnouncementId = UUID.randomUUID();
        UUID policyMatchingCaseId = UUID.randomUUID();
        UUID supportMatchingCaseId = UUID.randomUUID();
        UUID subsidyMatchingCaseId = UUID.randomUUID();
        UUID stepId = UUID.randomUUID();
        UUID inProgressId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                        UPDATE partner_verifications
                        SET
                            is_current = false,
                            updated_at = now(),
                            updated_by = ?
                        WHERE member_user_id = ?
                          AND is_current = true
                        """,
                LOCAL_OPERATOR_ID,
                LOCAL_USER_ID
        );
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
                LOCAL_USER_ID,
                LOCAL_OPERATOR_ID,
                LOCAL_OPERATOR_ID,
                LOCAL_OPERATOR_ID,
                LOCAL_OPERATOR_ID
        );

        insertAnnouncement(policyAnnouncementId, "Dashboard Policy", new BigDecimal("1000000"), new BigDecimal("3000000"), "LOAN");
        insertAnnouncement(supportAnnouncementId, "Dashboard Support", new BigDecimal("2000000"), new BigDecimal("5000000"), "VOUCHER");
        insertAnnouncement(subsidyAnnouncementId, "Dashboard Subsidy", new BigDecimal("1500000"), new BigDecimal("7000000"), "CASH");

        insertMatchingCase(policyMatchingCaseId, policyAnnouncementId, verificationId, "MATCHED");
        insertMatchingCase(supportMatchingCaseId, supportAnnouncementId, verificationId, "REVIEW_REQUIRED");
        insertMatchingCase(subsidyMatchingCaseId, subsidyAnnouncementId, verificationId, "PROGRESSED");

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
                        ) VALUES (?, ?, 1, 'Dashboard Step', 'Review dashboard guide.', 'Select the next dashboard action.',
                                  'BUTTON_CLICK', 'CONTINUE', true, ?, ?)
                        """,
                stepId,
                policyAnnouncementId,
                LOCAL_OPERATOR_ID,
                LOCAL_OPERATOR_ID
        );
        jdbcTemplate.update(
                """
                        INSERT INTO announcement_step_buttons (
                            step_id,
                            button_code,
                            button_label,
                            button_action_code,
                            sort_order,
                            created_by,
                            updated_by
                        ) VALUES (?, 'CONTINUE', 'Continue', 'MOVE_NEXT', 1, ?, ?)
                        """,
                stepId,
                LOCAL_OPERATOR_ID,
                LOCAL_OPERATOR_ID
        );

        insertApplicationProgress(inProgressId, policyMatchingCaseId, policyAnnouncementId, stepId, "IN_PROGRESS", null);
        insertApplicationProgress(UUID.randomUUID(), supportMatchingCaseId, supportAnnouncementId, null, "WAITING_RESULT", null);
        insertApplicationProgress(UUID.randomUUID(), subsidyMatchingCaseId, subsidyAnnouncementId, null, "APPROVED", new BigDecimal("7000000"));

        jdbcTemplate.update(
                """
                        INSERT INTO application_step_states (
                            progress_id,
                            step_id,
                            status_code,
                            started_at,
                            created_by,
                            updated_by
                        ) VALUES (?, ?, 'IN_PROGRESS', now(), ?, ?)
                        """,
                inProgressId,
                stepId,
                LOCAL_USER_ID,
                LOCAL_USER_ID
        );

        return new DashboardFixture(List.of(policyMatchingCaseId, supportMatchingCaseId, subsidyMatchingCaseId));
    }

    private void deleteDashboardFixtureRows() {
        jdbcTemplate.update(
                """
                        DELETE FROM application_step_states
                        WHERE progress_id IN (
                            SELECT id
                            FROM application_progresses
                            WHERE member_user_id = ?
                        )
                        """,
                LOCAL_USER_ID
        );
        jdbcTemplate.update(
                """
                        DELETE FROM application_action_logs
                        WHERE progress_id IN (
                            SELECT id
                            FROM application_progresses
                            WHERE member_user_id = ?
                        )
                        """,
                LOCAL_USER_ID
        );
        jdbcTemplate.update(
                """
                        DELETE FROM application_step_checklists
                        WHERE progress_id IN (
                            SELECT id
                            FROM application_progresses
                            WHERE member_user_id = ?
                        )
                        """,
                LOCAL_USER_ID
        );
        jdbcTemplate.update(
                """
                        DELETE FROM progress_reminder_logs
                        WHERE progress_id IN (
                            SELECT id
                            FROM application_progresses
                            WHERE member_user_id = ?
                        )
                        """,
                LOCAL_USER_ID
        );
        jdbcTemplate.update(
                """
                        DELETE FROM application_input_values
                        WHERE progress_id IN (
                            SELECT id
                            FROM application_progresses
                            WHERE member_user_id = ?
                        )
                        """,
                LOCAL_USER_ID
        );
        jdbcTemplate.update(
                """
                        UPDATE consultation_reservations
                        SET progress_id = null,
                            updated_at = now(),
                            updated_by = ?
                        WHERE progress_id IN (
                            SELECT id
                            FROM application_progresses
                            WHERE member_user_id = ?
                        )
                        """,
                LOCAL_OPERATOR_ID,
                LOCAL_USER_ID
        );
        jdbcTemplate.update(
                """
                        DELETE FROM application_progresses
                        WHERE member_user_id = ?
                        """,
                LOCAL_USER_ID
        );
        jdbcTemplate.update(
                """
                        DELETE FROM matching_result_details
                        WHERE matching_case_id IN (
                            SELECT id
                            FROM matching_cases
                            WHERE member_user_id = ?
                        )
                        """,
                LOCAL_USER_ID
        );
        jdbcTemplate.update(
                """
                        DELETE FROM matching_cases
                        WHERE member_user_id = ?
                        """,
                LOCAL_USER_ID
        );
    }

    private void insertAnnouncement(
            UUID announcementId,
            String title,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String paymentMethodCode
    ) {
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
                        ) VALUES (?, 'BUSINESS', ?, 'Dashboard Gate Agency', 'Dashboard smoke announcement',
                                  '2026-06-01', '2026-06-30', 'NORMAL', 'APPROVED', 'NO_LIMIT', ?, ?, ?, ?)
                        """,
                announcementId,
                title + " " + UUID.randomUUID(),
                minAmount,
                maxAmount,
                LOCAL_OPERATOR_ID,
                LOCAL_OPERATOR_ID
        );
        jdbcTemplate.update(
                """
                        INSERT INTO announcement_options (
                            announcement_id,
                            option_group_code,
                            option_code,
                            created_by
                        ) VALUES (?, 'PAYMENT_METHOD', ?, ?)
                        """,
                announcementId,
                paymentMethodCode,
                LOCAL_OPERATOR_ID
        );
    }

    private void insertMatchingCase(UUID matchingCaseId, UUID announcementId, UUID verificationId, String statusCode) {
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
                        ) VALUES (?, ?, ?, null, ?, 'BASIC', 'BASIC_INFO', now(), ?, ?)
                        """,
                UUID.randomUUID(),
                announcementId,
                LOCAL_USER_ID,
                statusCode,
                LOCAL_OPERATOR_ID,
                LOCAL_OPERATOR_ID
        );
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
                        ) VALUES (?, ?, ?, ?, ?, 'FINAL', 'DOCUMENT_INPUT', now(), ?, ?)
                        """,
                matchingCaseId,
                announcementId,
                LOCAL_USER_ID,
                verificationId,
                statusCode,
                LOCAL_OPERATOR_ID,
                LOCAL_OPERATOR_ID
        );
    }

    private void insertApplicationProgress(
            UUID progressId,
            UUID matchingCaseId,
            UUID announcementId,
            UUID currentStepId,
            String statusCode,
            BigDecimal receivedAmount
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
                            result_code,
                            result_date,
                            received_amount,
                            created_by,
                            updated_by
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                progressId,
                matchingCaseId,
                announcementId,
                LOCAL_USER_ID,
                currentStepId,
                statusCode,
                "APPROVED".equals(statusCode) ? "APPROVED" : null,
                "APPROVED".equals(statusCode) ? LocalDate.of(2026, 6, 15) : null,
                receivedAmount,
                LOCAL_USER_ID,
                LOCAL_USER_ID
        );
    }

    private MockHttpSession loginLocalUser() throws Exception {
        return login("local_user");
    }

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
            if (result.getResponse().getStatus() == 200 && result.getRequest().getSession(false) instanceof MockHttpSession session) {
                return session;
            }
        }
        throw new IllegalStateException(loginId + " login failed for dashboard smoke.");
    }

    private void assertMatchingCasesRemainOwnedByFixture(DashboardFixture fixture) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM matching_cases
                        WHERE id IN (?, ?, ?)
                          AND member_user_id = ?
                        """,
                Long.class,
                fixture.matchingCaseIds().get(0),
                fixture.matchingCaseIds().get(1),
                fixture.matchingCaseIds().get(2),
                LOCAL_USER_ID
        );
        org.assertj.core.api.Assertions.assertThat(count).isEqualTo(3);
    }

    private record DashboardFixture(List<UUID> matchingCaseIds) {
    }
}
