package com.saneb.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class MigrationContractTest {

    @Test
    void v1MigrationContainsMvpTables() throws IOException {
        String sql = selectV1Migration();

        assertThat(sql).contains(
                "CREATE TABLE roles",
                "CREATE TABLE users",
                "CREATE TABLE user_roles",
                "CREATE TABLE auth_login_histories",
                "CREATE TABLE member_profiles",
                "CREATE TABLE business_profiles",
                "CREATE TABLE family_members",
                "CREATE TABLE partner_profiles",
                "CREATE TABLE partner_verifications",
                "CREATE TABLE verification_documents",
                "CREATE TABLE announcements",
                "CREATE TABLE announcement_numeric_conditions",
                "CREATE TABLE matching_cases",
                "CREATE TABLE matching_result_details",
                "CREATE TABLE announcement_progress_steps",
                "CREATE TABLE application_progresses",
                "CREATE TABLE application_action_logs",
                "CREATE TABLE audit_logs"
        );
    }

    @Test
    void v1MigrationDoesNotContainExcludedMatchingMetrics() throws IOException {
        String sql = selectV1Migration().toLowerCase();

        assertThat(sql).doesNotContain(
                "recommendation_score",
                "priority_score",
                "selection_probability",
                "bonus_score",
                "ai_decision"
        );
    }

    private String selectV1Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V1__create_mvp_schema.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
