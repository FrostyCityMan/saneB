/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: FlywayMigrationIntegrationTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@EnabledIfEnvironmentVariable(named = "SANEB_FLYWAY_INTEGRATION", matches = "true")
@SpringBootTest(properties = "spring.main.web-application-type=none")
class FlywayMigrationIntegrationTest {

    @Autowired
    private DataSource dataSource;

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws SQLException 처리 중 예외가 발생한 경우
     */
    @Test
    void mvpMigrationsApplyToPostgreSql() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '1'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '4'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '6'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '7'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '8'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '9'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '10'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '11'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '12'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '13'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '14'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '28'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '29'
                      and success = true
                    """)).isEqualTo(1);

            for (String tableName : selectRequiredTableNames()) {
                assertThat(selectText(statement, "select to_regclass('public." + tableName + "')"))
                        .as(tableName)
                        .isEqualTo(tableName);
            }

            assertThat(selectLong(statement, "select count(1) from roles")).isEqualTo(6);
            assertThat(selectLong(statement, "select count(1) from roles where role_code = 'REVIEWER'"))
                    .isEqualTo(1);
            assertThat(selectLong(statement, "select count(1) from consent_versions")).isEqualTo(4);
            assertThat(selectLong(statement, "select count(1) from local_government_notice_sources"))
                    .isEqualTo(244);
            assertThat(selectLong(statement, "select count(1) from local_government_notice_sources where is_enabled = false"))
                    .isEqualTo(244);
            assertThat(selectLong(statement, "select count(distinct sigungu_code) from local_government_notice_sources"))
                    .isEqualTo(244);
            assertThat(selectLong(statement, """
                    select count(1)
                    from local_government_notice_sources
                    where validation_status_code = 'CHECK_REQUIRED'
                    """)).isEqualTo(13);
        }
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    private List<String> selectRequiredTableNames() {
        return List.of(
                "roles",
                "users",
                "user_roles",
                "auth_login_histories",
                "member_profiles",
                "business_profiles",
                "family_members",
                "partner_profiles",
                "partner_verifications",
                "verification_documents",
                "announcements",
                "announcement_numeric_conditions",
                "matching_cases",
                "matching_result_details",
                "announcement_progress_steps",
                "application_progresses",
                "application_action_logs",
                "audit_logs",
                "announcement_input_requirements",
                "announcement_input_options",
                "application_input_values",
                "consent_versions",
                "user_consents",
                "stored_files",
                "document_submissions",
                "document_submission_reviews",
                "partner_availability_slots",
                "consultation_reservations",
                "consultation_histories",
                "subscription_plans",
                "user_subscriptions",
                "payment_transactions",
                "refund_transactions",
                "payment_provider_events",
                "notification_templates",
                "notification_messages",
                "notification_delivery_logs",
                "operation_tasks",
                "operation_task_comments",
                "operation_task_assignments",
                "report_exports",
                "admin_report_snapshots",
                "ai_assist_requests",
                "ai_assist_results",
                "announcement_source_collection_requests",
                "announcement_source_collection_runs",
                "announcement_source_collection_run_items",
                "announcement_source_snapshots",
                "announcement_source_attachments",
                "announcement_source_highlights",
                "announcement_source_duplicate_candidates",
                "announcement_source_review_histories",
                "announcement_source_links",
                "local_government_notice_parser_profiles",
                "local_government_notice_sources",
                "announcement_source_collection_source_results",
                "announcement_source_snapshot_duplicates",
                "announcement_source_collection_schedules",
                "announcement_source_schedule_executions"
        );
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param statement 입력 값
     *
     * @param sql 입력 값
     *
     * @return 처리 결과
     *
     * @throws SQLException 처리 중 예외가 발생한 경우
     */
    private long selectLong(Statement statement, String sql) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getLong(1);
        }
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param statement 입력 값
     *
     * @param sql 입력 값
     *
     * @return 처리 결과
     *
     * @throws SQLException 처리 중 예외가 발생한 경우
     */
    private String selectText(Statement statement, String sql) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getString(1);
        }
    }
}
