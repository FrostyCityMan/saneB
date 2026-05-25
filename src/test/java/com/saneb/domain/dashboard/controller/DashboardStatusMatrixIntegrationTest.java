package com.saneb.domain.dashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@EnabledIfEnvironmentVariable(named = "SANEB_DASHBOARD_MATRIX_QA", matches = "true")
@ActiveProfiles("local")
@SpringBootTest
@AutoConfigureMockMvc
class DashboardStatusMatrixIntegrationTest {

    private static final String PASSWORD_HASH = "$2a$10$InQi9a3ehghCfxu2Z59DiegEEW4pfhxb4h19PCJb58D0/1OWmmQ2y";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void noVerificationOrDraftStateReturnsVerificationRequiredDashboard() throws Exception {
        String noVerificationPrivacyMarker = "matrix-private-no-verification-" + UUID.randomUUID();
        String draftPrivacyMarker = "matrix-private-draft-" + UUID.randomUUID();
        DashboardUser noVerificationUser = insertDashboardUser("matrix-no-verification", "USER", noVerificationPrivacyMarker);
        DashboardUser draftUser = insertDashboardUser("matrix-draft", "USER", draftPrivacyMarker);
        DashboardUser operator = insertDashboardUser("matrix-draft-operator", "OPERATOR");
        insertVerification(UUID.randomUUID(), draftUser.userId(), operator.userId(), "DRAFT");

        assertVerificationRequiredDashboard(noVerificationUser.loginId(), noVerificationPrivacyMarker);
        assertVerificationRequiredDashboard(draftUser.loginId(), draftPrivacyMarker);
    }

    @Test
    void readyOrInProgressStateReturnsProgressRouteAndDatabaseCounts() throws Exception {
        String privacyMarker = "matrix-private-ready-" + UUID.randomUUID();
        DashboardUser member = insertDashboardUser("matrix-ready-member", "USER", privacyMarker);
        DashboardUser operator = insertDashboardUser("matrix-ready-operator", "OPERATOR");
        UUID verificationId = insertVerification(UUID.randomUUID(), member.userId(), operator.userId(), "VERIFIED");
        UUID announcementId = insertAnnouncement(operator.userId(), "Matrix Ready Announcement", new BigDecimal("1200000"), new BigDecimal("4500000"), "LOAN");
        UUID matchingCaseId = insertMatchingCase(UUID.randomUUID(), announcementId, member.userId(), verificationId, operator.userId(), "PROGRESSED");
        UUID stepId = insertProgressStep(UUID.randomUUID(), announcementId, operator.userId(), "Matrix Ready Step");
        UUID progressId = insertApplicationProgress(
                UUID.randomUUID(),
                matchingCaseId,
                announcementId,
                member.userId(),
                stepId,
                "READY",
                null
        );
        insertStepState(progressId, stepId, member.userId(), "READY");

        mockMvc.perform(get("/api/v1/dashboard/me/summary").with(user(member.loginId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.serviceStatusCode").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.verificationStatusCode").value("VERIFIED"))
                .andExpect(jsonPath("$.data.candidateCounts.policyFund").value(1))
                .andExpect(jsonPath("$.data.candidateCounts.supportFund").value(0))
                .andExpect(jsonPath("$.data.candidateCounts.subsidy").value(0))
                .andExpect(jsonPath("$.data.finalMatchedCount").value(1))
                .andExpect(jsonPath("$.data.supportAmountRange.minAmount").value(1200000))
                .andExpect(jsonPath("$.data.supportAmountRange.maxAmount").value(4500000))
                .andExpect(jsonPath("$.data.supportAmountRange.basisCode").value("ANNOUNCEMENT_AMOUNT_RANGE"));

        mockMvc.perform(get("/api/v1/dashboard/me/current-action").with(user(member.loginId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actionCode").value("PROGRESS_ACTION_REQUIRED"))
                .andExpect(jsonPath("$.data.title").value("Matrix Ready Step"))
                .andExpect(jsonPath("$.data.primaryButtonLabel").value("Continue"))
                .andExpect(jsonPath("$.data.route").value("/app/application-progresses/" + progressId))
                .andExpect(jsonPath("$.data.displayOrder").value(20));

        mockMvc.perform(get("/api/v1/dashboard/me/progress-summary").with(user(member.loginId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.inProgressCount").value(1))
                .andExpect(jsonPath("$.data.waitingResultCount").value(0))
                .andExpect(jsonPath("$.data.approvedCount").value(0))
                .andExpect(jsonPath("$.data.totalReceivedAmount").value(0));

        mockMvc.perform(get("/api/v1/dashboard/me/reverification-status").with(user(member.loginId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.required").value(false));

        assertNoAuditMetadataLeak(privacyMarker);
    }

    @Test
    void approvedResultStateReturnsNoneActionAndReceivedAmountFromDatabase() throws Exception {
        String privacyMarker = "matrix-private-approved-" + UUID.randomUUID();
        DashboardUser member = insertDashboardUser("matrix-approved-member", "USER", privacyMarker);
        DashboardUser operator = insertDashboardUser("matrix-approved-operator", "OPERATOR");
        UUID verificationId = insertVerification(UUID.randomUUID(), member.userId(), operator.userId(), "VERIFIED");
        UUID announcementId = insertAnnouncement(operator.userId(), "Matrix Approved Announcement", new BigDecimal("2500000"), new BigDecimal("7600000"), "CASH");
        UUID matchingCaseId = insertMatchingCase(UUID.randomUUID(), announcementId, member.userId(), verificationId, operator.userId(), "PROGRESSED");
        insertApplicationProgress(
                UUID.randomUUID(),
                matchingCaseId,
                announcementId,
                member.userId(),
                null,
                "APPROVED",
                new BigDecimal("7654321")
        );

        mockMvc.perform(get("/api/v1/dashboard/me/summary").with(user(member.loginId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.serviceStatusCode").value("COMPLETED"))
                .andExpect(jsonPath("$.data.verificationStatusCode").value("VERIFIED"))
                .andExpect(jsonPath("$.data.candidateCounts.policyFund").value(0))
                .andExpect(jsonPath("$.data.candidateCounts.supportFund").value(0))
                .andExpect(jsonPath("$.data.candidateCounts.subsidy").value(1))
                .andExpect(jsonPath("$.data.finalMatchedCount").value(1))
                .andExpect(jsonPath("$.data.supportAmountRange.minAmount").value(2500000))
                .andExpect(jsonPath("$.data.supportAmountRange.maxAmount").value(7600000));

        MvcResult currentActionResult = mockMvc.perform(get("/api/v1/dashboard/me/current-action").with(user(member.loginId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actionCode").value("NONE"))
                .andReturn();
        assertCurrentActionRouteIsExplicitNull(currentActionResult);

        mockMvc.perform(get("/api/v1/dashboard/me/progress-summary").with(user(member.loginId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.inProgressCount").value(0))
                .andExpect(jsonPath("$.data.waitingResultCount").value(0))
                .andExpect(jsonPath("$.data.approvedCount").value(1))
                .andExpect(jsonPath("$.data.totalReceivedAmount").value(7654321));

        mockMvc.perform(get("/api/v1/dashboard/me/reverification-status").with(user(member.loginId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.required").value(false));

        assertNoAuditMetadataLeak(privacyMarker);
    }

    private void assertVerificationRequiredDashboard(String loginId, String privacyMarker) throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/me/summary").with(user(loginId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.serviceStatusCode").value("VERIFICATION_REQUIRED"))
                .andExpect(jsonPath("$.data.verificationStatusCode").value("DRAFT"))
                .andExpect(jsonPath("$.data.candidateCounts.policyFund").value(0))
                .andExpect(jsonPath("$.data.candidateCounts.supportFund").value(0))
                .andExpect(jsonPath("$.data.candidateCounts.subsidy").value(0))
                .andExpect(jsonPath("$.data.finalMatchedCount").value(0))
                .andExpect(jsonPath("$.data.supportAmountRange.minAmount").isEmpty())
                .andExpect(jsonPath("$.data.supportAmountRange.maxAmount").isEmpty())
                .andExpect(jsonPath("$.data.supportAmountRange.basisCode").value("ANNOUNCEMENT_AMOUNT_RANGE"));

        mockMvc.perform(get("/api/v1/dashboard/me/current-action").with(user(loginId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actionCode").value("VERIFICATION_DOCUMENT_REQUIRED"))
                .andExpect(jsonPath("$.data.route").value("/app/member/verifications/current"));

        mockMvc.perform(get("/api/v1/dashboard/me/progress-summary").with(user(loginId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.inProgressCount").value(0))
                .andExpect(jsonPath("$.data.waitingResultCount").value(0))
                .andExpect(jsonPath("$.data.approvedCount").value(0))
                .andExpect(jsonPath("$.data.totalReceivedAmount").value(0));

        mockMvc.perform(get("/api/v1/dashboard/me/reverification-status").with(user(loginId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.required").value(false));

        assertNoAuditMetadataLeak(privacyMarker);
    }

    private DashboardUser insertDashboardUser(String loginPrefix, String roleCode) {
        return insertDashboardUser(loginPrefix, roleCode, null);
    }

    private DashboardUser insertDashboardUser(String loginPrefix, String roleCode, String privacyMarker) {
        UUID userId = UUID.randomUUID();
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String loginId = loginPrefix + "-" + suffix;
        jdbcTemplate.update(
                """
                        INSERT INTO users (
                            id,
                            login_id,
                            password_hash,
                            name,
                            phone,
                            email,
                            status_code,
                            password_reset_required
                        ) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', false)
                        """,
                userId,
                loginId,
                PASSWORD_HASH,
                privacyMarker == null ? "Dashboard Matrix " + loginPrefix : privacyMarker,
                null,
                privacyMarker == null ? null : privacyMarker + "@example.test"
        );
        jdbcTemplate.update(
                """
                        INSERT INTO user_roles (
                            user_id,
                            role_code
                        ) VALUES (?, ?)
                        """,
                userId,
                roleCode
        );
        return new DashboardUser(userId, loginId);
    }

    private UUID insertVerification(UUID verificationId, UUID memberUserId, UUID operatorUserId, String statusCode) {
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
                        ) VALUES (?, ?, ?, ?, true, false, ?, ?, ?, ?, ?)
                        """,
                verificationId,
                memberUserId,
                operatorUserId,
                statusCode,
                "VERIFIED".equals(statusCode) ? java.time.OffsetDateTime.now() : null,
                "VERIFIED".equals(statusCode) ? java.time.OffsetDateTime.now() : null,
                "VERIFIED".equals(statusCode) ? operatorUserId : null,
                operatorUserId,
                operatorUserId
        );
        return verificationId;
    }

    private UUID insertAnnouncement(
            UUID operatorUserId,
            String titlePrefix,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String paymentMethodCode
    ) {
        UUID announcementId = UUID.randomUUID();
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
                        ) VALUES (?, 'BUSINESS', ?, 'Dashboard Matrix Agency', 'Dashboard matrix QA announcement',
                                  '2026-06-01', '2026-06-30', 'NORMAL', 'APPROVED', 'NO_LIMIT', ?, ?, ?, ?)
                        """,
                announcementId,
                titlePrefix + " " + UUID.randomUUID(),
                minAmount,
                maxAmount,
                operatorUserId,
                operatorUserId
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
                operatorUserId
        );
        return announcementId;
    }

    private UUID insertMatchingCase(
            UUID matchingCaseId,
            UUID announcementId,
            UUID memberUserId,
            UUID verificationId,
            UUID operatorUserId,
            String statusCode
    ) {
        jdbcTemplate.update(
                """
                        INSERT INTO matching_cases (
                            id,
                            announcement_id,
                            member_user_id,
                            verification_id,
                            status_code,
                            matched_at,
                            created_by,
                            updated_by
                        ) VALUES (?, ?, ?, ?, ?, now(), ?, ?)
                        """,
                matchingCaseId,
                announcementId,
                memberUserId,
                verificationId,
                statusCode,
                operatorUserId,
                operatorUserId
        );
        return matchingCaseId;
    }

    private UUID insertProgressStep(UUID stepId, UUID announcementId, UUID operatorUserId, String stepName) {
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
                        ) VALUES (?, ?, 1, ?, 'Review dashboard matrix guide.', 'Continue dashboard matrix progress.',
                                  'BUTTON_CLICK', 'CONTINUE', true, ?, ?)
                        """,
                stepId,
                announcementId,
                stepName,
                operatorUserId,
                operatorUserId
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
                operatorUserId,
                operatorUserId
        );
        return stepId;
    }

    private UUID insertApplicationProgress(
            UUID progressId,
            UUID matchingCaseId,
            UUID announcementId,
            UUID memberUserId,
            UUID currentStepId,
            String statusCode,
            BigDecimal receivedAmount
    ) {
        boolean approved = "APPROVED".equals(statusCode);
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
                memberUserId,
                currentStepId,
                statusCode,
                approved ? "APPROVED" : null,
                approved ? LocalDate.of(2026, 6, 30) : null,
                receivedAmount,
                memberUserId,
                memberUserId
        );
        return progressId;
    }

    private void insertStepState(UUID progressId, UUID stepId, UUID memberUserId, String statusCode) {
        jdbcTemplate.update(
                """
                        INSERT INTO application_step_states (
                            progress_id,
                            step_id,
                            status_code,
                            started_at,
                            created_by,
                            updated_by
                        ) VALUES (?, ?, ?, now(), ?, ?)
                        """,
                progressId,
                stepId,
                statusCode,
                memberUserId,
                memberUserId
        );
    }

    private void assertCurrentActionRouteIsExplicitNull(MvcResult result) throws Exception {
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertThat(data.has("route")).isTrue();
        assertThat(data.get("route").isNull()).isTrue();
    }

    private void assertNoAuditMetadataLeak(String privacyMarker) {
        Long leakCount = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM audit_logs
                        WHERE metadata_json::text LIKE ?
                        """,
                Long.class,
                "%" + privacyMarker + "%"
        );
        assertThat(leakCount).isZero();
    }

    private record DashboardUser(UUID userId, String loginId) {
    }
}
