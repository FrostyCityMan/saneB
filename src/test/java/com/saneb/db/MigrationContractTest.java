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

    @Test
    void v4MigrationContainsDynamicAnnouncementInputTables() throws IOException {
        String sql = selectV4Migration();

        assertThat(sql).contains(
                "CREATE TABLE announcement_input_requirements",
                "CREATE TABLE announcement_input_options",
                "CREATE TABLE application_input_values",
                "CONSTRAINT uq_announcement_input_requirements_field_key",
                "CONSTRAINT uq_announcement_input_options_code",
                "CREATE UNIQUE INDEX uq_application_input_values_single_value",
                "CREATE UNIQUE INDEX uq_application_input_values_option_value"
        );
    }

    @Test
    void v6MigrationAllowsMatchingWithoutVerification() throws IOException {
        String sql = selectV6Migration();

        assertThat(sql).contains(
                "ALTER COLUMN verification_id DROP NOT NULL",
                "CREATE UNIQUE INDEX uq_matching_cases_without_verification"
        );
    }

    @Test
    void v7MigrationContainsUserConsentTables() throws IOException {
        String sql = selectV7Migration();

        assertThat(sql).contains(
                "CREATE TABLE consent_versions",
                "CREATE TABLE user_consents",
                "CREATE UNIQUE INDEX uq_consent_versions_current",
                "CREATE INDEX ix_user_consents_user_code_consented_at",
                "'TERMS_OF_SERVICE'",
                "'PRIVACY_POLICY'",
                "'E_CERT'",
                "'CREDIT_CHECK'"
        );
    }

    @Test
    void v8MigrationContainsDocumentFileSubmissionTables() throws IOException {
        String sql = selectV8Migration();

        assertThat(sql).contains(
                "CREATE TABLE stored_files",
                "CREATE TABLE document_submissions",
                "CREATE TABLE document_submission_reviews",
                "CONSTRAINT uq_stored_files_storage_key",
                "CREATE INDEX ix_document_submissions_resource",
                "PARTNER_VERIFICATION",
                "APPLICATION_PROGRESS"
        );
    }

    @Test
    void v9MigrationContainsConsultationReservationTables() throws IOException {
        String sql = selectV9Migration();

        assertThat(sql).contains(
                "CREATE TABLE partner_availability_slots",
                "CREATE TABLE consultation_reservations",
                "CREATE TABLE consultation_histories",
                "CREATE UNIQUE INDEX uq_consultation_reservations_active_slot",
                "'REQUESTED'",
                "'CONFIRMED'",
                "'CANCELED'"
        );
    }

    private String selectV1Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V1__create_mvp_schema.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    private String selectV4Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V4__create_dynamic_announcement_inputs.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    private String selectV6Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V6__allow_matching_without_verification.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    private String selectV7Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V7__create_user_consents.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    private String selectV8Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V8__create_document_file_submissions.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    private String selectV9Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V9__create_consultation_reservations.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
