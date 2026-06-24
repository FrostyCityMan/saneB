/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: MatchingSmokeIntegrationTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.matching.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

@EnabledIfEnvironmentVariable(named = "SANEB_MATCHING_SMOKE", matches = "true")
@ActiveProfiles("local")
@SpringBootTest
@AutoConfigureMockMvc
class MatchingSmokeIntegrationTest {

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
    void localOperatorCreatesMatchingCasesByVerifiedVerificationOnly() throws Exception {
        MockHttpSession session = loginLocalOperator();
        String fixtureKey = UUID.randomUUID().toString();

        MatchingFixture matchedFixture = insertFixture(fixtureKey, "matched", null);
        MatchingFixture reviewFixture = insertFixture(fixtureKey, "review", "NEEDS_REVIEW");
        MatchingFixture blockedFixture = insertFixture(fixtureKey, "blocked", "POLICY_FUND_RESTRICTED");
        MatchingFixture draftFixture = insertFixture(fixtureKey, "draft", null, "DRAFT");
        MatchingFixture noVerificationFixture = insertFixtureWithoutVerification(fixtureKey, "no_verification");

        UUID matchedCaseId = createMatchingCase(session, matchedFixture, "MATCHED", "PASS");
        UUID reviewCaseId = createMatchingCase(session, reviewFixture, "REVIEW_REQUIRED", "REVIEW_REQUIRED");
        UUID blockedCaseId = createMatchingCase(session, blockedFixture, "BLOCKED", "FAIL");
        UUID noVerificationCaseId = createMatchingCaseWithoutVerification(session, noVerificationFixture);

        mockMvc.perform(post("/api/v1/matching/cases")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(matchedFixture)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.matchingCaseId").value(matchedCaseId.toString()))
                .andExpect(jsonPath("$.data.statusCode").value("MATCHED"));

        mockMvc.perform(get("/api/v1/matching/cases")
                        .session(session)
                        .param("announcementId", matchedFixture.announcementId().toString())
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.items[0].matchingCaseId").value(matchedCaseId.toString()));

        mockMvc.perform(get("/api/v1/matching/cases/{matchingCaseId}", blockedCaseId)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("BLOCKED"))
                .andExpect(jsonPath("$.data.blockedReasonCode").value("POLICY_FUND_RESTRICTED"));

        mockMvc.perform(get("/api/v1/matching/cases/{matchingCaseId}/results", reviewCaseId)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].conditionScopeCode").value("APPLICATION"))
                .andExpect(jsonPath("$.data[0].conditionKey").value("RESTRICTION_FLAGS"))
                .andExpect(jsonPath("$.data[0].resultCode").value("REVIEW_REQUIRED"));

        mockMvc.perform(patch("/api/v1/matching/cases/{matchingCaseId}/status", reviewCaseId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "statusCode": "NOT_MATCHED",
                                  "blockedReasonCode": "MANUAL_REVIEW"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("NOT_MATCHED"))
                .andExpect(jsonPath("$.data.blockedReasonCode").value("MANUAL_REVIEW"))
                .andExpect(jsonPath("$.data.reviewedBy").value(LOCAL_OPERATOR_ID.toString()));

        mockMvc.perform(post("/api/v1/matching/cases")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(draftFixture)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.errorCode").value("ANNOUNCEMENT_NOT_APPROVED"));

        assertThat(selectMatchingCaseCount(matchedFixture)).isEqualTo(1);
        assertThat(selectMatchingCaseCount(noVerificationFixture)).isEqualTo(1);
        assertThat(selectMatchingCaseCount(draftFixture)).isEqualTo(0);
        assertThat(selectResultCode(matchedCaseId)).isEqualTo("PASS");
        assertThat(selectResultCode(reviewCaseId)).isEqualTo("REVIEW_REQUIRED");
        assertThat(selectResultCode(blockedCaseId)).isEqualTo("FAIL");
        assertThat(selectRequiredValue(noVerificationCaseId)).isEqualTo("VERIFICATION_NOT_REQUIRED");
        assertThat(selectApplicationProgressCount(List.of(matchedCaseId, reviewCaseId, blockedCaseId))).isEqualTo(0);
        assertThat(selectMatchingCreateAuditCount(List.of(matchedCaseId, reviewCaseId, blockedCaseId), "1", "0")).isEqualTo(3);
        assertThat(selectMatchingCreateAuditCount(List.of(noVerificationCaseId), "1", "0")).isEqualTo(1);
        assertThat(selectMatchingCreateAuditCount(List.of(matchedCaseId), "0", "1")).isEqualTo(1);
        assertThat(selectFailureAuditCount("ANNOUNCEMENT_NOT_APPROVED")).isGreaterThanOrEqualTo(1);
        assertThat(selectStatusUpdateAuditCount(reviewCaseId)).isEqualTo(1);
        assertThat(selectAuditMetadataPrivacyLeakCount(List.of(matchedCaseId, reviewCaseId, blockedCaseId))).isEqualTo(0);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param session 입력 값
     *
     * @param fixture 입력 값
     *
     * @param statusCode 입력 값
     *
     * @param resultCode 입력 값
     *
     * @return 처리 결과
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    private UUID createMatchingCase(
            MockHttpSession session,
            MatchingFixture fixture,
            String statusCode,
            String resultCode
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/matching/cases")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(fixture)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.announcementId").value(fixture.announcementId().toString()))
                .andExpect(jsonPath("$.data.memberUserId").value(fixture.memberUserId().toString()))
                .andExpect(jsonPath("$.data.verificationId").value(fixture.verificationId().toString()))
                .andExpect(jsonPath("$.data.statusCode").value(statusCode))
                .andReturn();

        UUID matchingCaseId = selectMatchingCaseId(result);
        mockMvc.perform(get("/api/v1/matching/cases/{matchingCaseId}/results", matchingCaseId)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].resultCode").value(resultCode));
        return matchingCaseId;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param session 입력 값
     *
     * @param fixture 입력 값
     *
     * @return 처리 결과
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    private UUID createMatchingCaseWithoutVerification(
            MockHttpSession session,
            MatchingFixture fixture
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/matching/cases")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestWithoutVerification(fixture)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.announcementId").value(fixture.announcementId().toString()))
                .andExpect(jsonPath("$.data.memberUserId").value(fixture.memberUserId().toString()))
                .andExpect(jsonPath("$.data.statusCode").value("MATCHED"))
                .andReturn();

        UUID matchingCaseId = selectMatchingCaseId(result);
        mockMvc.perform(post("/api/v1/matching/cases")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestWithoutVerification(fixture)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.matchingCaseId").value(matchingCaseId.toString()));
        return matchingCaseId;
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
        throw new IllegalStateException("local_operator login failed for matching smoke.");
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param fixtureKey 입력 값
     *
     * @param suffix 입력 값
     *
     * @param restrictionCode 입력 값
     *
     * @return 처리 결과
     */
    private MatchingFixture insertFixture(String fixtureKey, String suffix, String restrictionCode) {
        return insertFixture(fixtureKey, suffix, restrictionCode, "APPROVED");
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param fixtureKey 입력 값
     *
     * @param suffix 입력 값
     *
     * @param restrictionCode 입력 값
     *
     * @param approvalStatusCode 입력 값
     *
     * @return 처리 결과
     */
    private MatchingFixture insertFixture(String fixtureKey, String suffix, String restrictionCode, String approvalStatusCode) {
        UUID memberUserId = UUID.randomUUID();
        UUID announcementId = UUID.randomUUID();
        UUID verificationId = UUID.randomUUID();
        insertMemberUser(memberUserId, "matching_" + suffix + "_" + fixtureKey.replace("-", ""));
        insertAnnouncement(announcementId, "Gate Matching " + suffix + " " + fixtureKey, approvalStatusCode);
        insertVerification(verificationId, memberUserId);
        if (restrictionCode != null) {
            insertRestrictionFlag(verificationId, restrictionCode);
        }
        return new MatchingFixture(announcementId, memberUserId, verificationId);
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param fixtureKey 입력 값
     *
     * @param suffix 입력 값
     *
     * @return 처리 결과
     */
    private MatchingFixture insertFixtureWithoutVerification(String fixtureKey, String suffix) {
        UUID memberUserId = UUID.randomUUID();
        UUID announcementId = UUID.randomUUID();
        insertMemberUser(memberUserId, "matching_" + suffix + "_" + fixtureKey.replace("-", ""));
        insertAnnouncement(announcementId, "Gate Matching " + suffix + " " + fixtureKey, "APPROVED");
        return new MatchingFixture(announcementId, memberUserId, null);
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
                "Matching Smoke User"
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
     *
     * @param approvalStatusCode 입력 값
     */
    private void insertAnnouncement(UUID announcementId, String title, String approvalStatusCode) {
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
                            ?, 'BUSINESS', ?, 'Matching Gate Agency', 'Matching smoke approved announcement',
                            ?, ?, 'NORMAL', ?, 'NO_LIMIT', 1000000, 5000000, ?, ?
                        )
                        """,
                announcementId,
                title,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                approvalStatusCode,
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
     * @param verificationId 입력 값
     *
     * @param restrictionCode 입력 값
     */
    private void insertRestrictionFlag(UUID verificationId, String restrictionCode) {
        jdbcTemplate.update(
                """
                        INSERT INTO verification_restriction_flags (
                            verification_id,
                            restriction_code,
                            is_checked,
                            note,
                            created_by,
                            updated_by
                        ) VALUES (?, ?, true, 'Matching smoke flag', ?, ?)
                        """,
                verificationId,
                restrictionCode,
                LOCAL_OPERATOR_ID,
                LOCAL_OPERATOR_ID
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param fixture 입력 값
     *
     * @return 처리 결과
     */
    private String createRequest(MatchingFixture fixture) {
        return """
                {
                  "announcementId": "%s",
                  "memberUserId": "%s",
                  "verificationId": "%s"
                }
                """.formatted(fixture.announcementId(), fixture.memberUserId(), fixture.verificationId());
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param fixture 입력 값
     *
     * @return 처리 결과
     */
    private String createRequestWithoutVerification(MatchingFixture fixture) {
        return """
                {
                  "announcementId": "%s",
                  "memberUserId": "%s"
                }
                """.formatted(fixture.announcementId(), fixture.memberUserId());
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
    private UUID selectMatchingCaseId(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return UUID.fromString(root.path("data").path("matchingCaseId").asText());
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param fixture 입력 값
     *
     * @return 처리 결과
     */
    private long selectMatchingCaseCount(MatchingFixture fixture) {
        if (fixture.verificationId() == null) {
            Long count = jdbcTemplate.queryForObject(
                    """
                            SELECT count(1)
                            FROM matching_cases
                            WHERE announcement_id = ?
                              AND member_user_id = ?
                              AND verification_id IS NULL
                            """,
                    Long.class,
                    fixture.announcementId(),
                    fixture.memberUserId()
            );
            return count == null ? 0 : count;
        }
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM matching_cases
                        WHERE announcement_id = ?
                          AND member_user_id = ?
                          AND verification_id = ?
                        """,
                Long.class,
                fixture.announcementId(),
                fixture.memberUserId(),
                fixture.verificationId()
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
    private String selectRequiredValue(UUID matchingCaseId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT required_value
                        FROM matching_result_details
                        WHERE matching_case_id = ?
                          AND condition_scope_code = 'APPLICATION'
                          AND condition_key = 'RESTRICTION_FLAGS'
                        """,
                String.class,
                matchingCaseId
        );
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param matchingCaseId 입력 값
     *
     * @return 처리 결과
     */
    private String selectResultCode(UUID matchingCaseId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT result_code
                        FROM matching_result_details
                        WHERE matching_case_id = ?
                          AND condition_scope_code = 'APPLICATION'
                          AND condition_key = 'RESTRICTION_FLAGS'
                        """,
                String.class,
                matchingCaseId
        );
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param matchingCaseIds 입력 값
     *
     * @return 처리 결과
     */
    private long selectApplicationProgressCount(List<UUID> matchingCaseIds) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM application_progresses
                        WHERE matching_case_id IN (?, ?, ?)
                        """,
                Long.class,
                matchingCaseIds.get(0),
                matchingCaseIds.get(1),
                matchingCaseIds.get(2)
        );
        return count == null ? 0 : count;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param matchingCaseIds 입력 값
     *
     * @param createdCount 입력 값
     *
     * @param skippedCount 입력 값
     *
     * @return 처리 결과
     */
    private long selectMatchingCreateAuditCount(List<UUID> matchingCaseIds, String createdCount, String skippedCount) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM audit_logs
                        WHERE resource_type = 'MATCHING_CASE'
                          AND action_code = 'MATCHING_CASE_CREATE'
                          AND resource_id IN (?, ?, ?)
                          AND metadata_json ->> 'createdCount' = ?
                          AND metadata_json ->> 'skippedCount' = ?
                        """,
                Long.class,
                matchingCaseIds.get(0),
                matchingCaseIds.size() > 1 ? matchingCaseIds.get(1) : matchingCaseIds.get(0),
                matchingCaseIds.size() > 2 ? matchingCaseIds.get(2) : matchingCaseIds.get(0),
                createdCount,
                skippedCount
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
    private long selectStatusUpdateAuditCount(UUID matchingCaseId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM audit_logs
                        WHERE resource_type = 'MATCHING_CASE'
                          AND action_code = 'MATCHING_CASE_STATUS_UPDATE'
                          AND resource_id = ?
                          AND metadata_json ->> 'afterStatusCode' = 'NOT_MATCHED'
                        """,
                Long.class,
                matchingCaseId
        );
        return count == null ? 0 : count;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param failureReasonCode 입력 값
     *
     * @return 처리 결과
     */
    private long selectFailureAuditCount(String failureReasonCode) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM audit_logs
                        WHERE resource_type = 'MATCHING_CASE'
                          AND action_code = 'MATCHING_CASE_CREATE'
                          AND result_code = 'FAIL'
                          AND actor_user_id = ?
                          AND metadata_json ->> 'failureReasonCode' = ?
                        """,
                Long.class,
                LOCAL_OPERATOR_ID,
                failureReasonCode
        );
        return count == null ? 0 : count;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param matchingCaseIds 입력 값
     *
     * @return 처리 결과
     */
    private long selectAuditMetadataPrivacyLeakCount(List<UUID> matchingCaseIds) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM audit_logs
                        WHERE resource_type = 'MATCHING_CASE'
                          AND resource_id IN (?, ?, ?)
                          AND (
                              metadata_json::text ILIKE '%address%'
                              OR metadata_json::text ILIKE '%phone%'
                              OR metadata_json::text ILIKE '%password%'
                          )
                        """,
                Long.class,
                matchingCaseIds.get(0),
                matchingCaseIds.get(1),
                matchingCaseIds.get(2)
        );
        return count == null ? 0 : count;
    }

    private record MatchingFixture(
            UUID announcementId,
            UUID memberUserId,
            UUID verificationId
    ) {
    }
}
