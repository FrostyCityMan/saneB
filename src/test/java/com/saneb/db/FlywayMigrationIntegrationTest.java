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

            for (String tableName : selectRequiredTableNames()) {
                assertThat(selectText(statement, "select to_regclass('public." + tableName + "')"))
                        .as(tableName)
                        .isEqualTo(tableName);
            }

            assertThat(selectLong(statement, "select count(1) from roles")).isEqualTo(5);
            assertThat(selectLong(statement, "select count(1) from consent_versions")).isEqualTo(4);
        }
    }

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
                "payment_provider_events"
        );
    }

    private long selectLong(Statement statement, String sql) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getLong(1);
        }
    }

    private String selectText(Statement statement, String sql) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getString(1);
        }
    }
}
