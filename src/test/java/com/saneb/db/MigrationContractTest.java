/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: MigrationContractTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class MigrationContractTest {

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
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

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
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

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
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

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v6MigrationAllowsMatchingWithoutVerification() throws IOException {
        String sql = selectV6Migration();

        assertThat(sql).contains(
                "ALTER COLUMN verification_id DROP NOT NULL",
                "CREATE UNIQUE INDEX uq_matching_cases_without_verification"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
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

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
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

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
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

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v10MigrationContainsSubscriptionPaymentTables() throws IOException {
        String sql = selectV10Migration();

        assertThat(sql).contains(
                "CREATE TABLE subscription_plans",
                "CREATE TABLE user_subscriptions",
                "CREATE TABLE payment_transactions",
                "CREATE TABLE refund_transactions",
                "CREATE TABLE payment_provider_events",
                "CREATE UNIQUE INDEX uq_user_subscriptions_current",
                "CREATE UNIQUE INDEX uq_payment_transactions_provider_key",
                "'PAYMENT_APPROVED'",
                "'REFUND_APPROVED'"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v11MigrationContainsNotificationAndOperationTaskTables() throws IOException {
        String sql = selectV11Migration();

        assertThat(sql).contains(
                "CREATE TABLE notification_templates",
                "CREATE TABLE notification_messages",
                "CREATE TABLE notification_delivery_logs",
                "CREATE TABLE operation_tasks",
                "CREATE TABLE operation_task_comments",
                "CREATE TABLE operation_task_assignments",
                "'SUPPLEMENT_REQUEST'",
                "'CONSULTATION_PENDING'",
                "'IN_APP'",
                "'KAKAO'"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v12MigrationContainsAdminReportExportTables() throws IOException {
        String sql = selectV12Migration();

        assertThat(sql).contains(
                "CREATE TABLE report_exports",
                "CREATE TABLE admin_report_snapshots",
                "'OPERATION_SUMMARY'",
                "'CSV'",
                "'EXCEL'",
                "'COMPLETED'"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v14MigrationContainsReviewerAndManualConsultationChanges() throws IOException {
        String sql = selectV14Migration();

        assertThat(sql).contains(
                "'REVIEWER'",
                "'검수자'",
                "ALTER COLUMN slot_id DROP NOT NULL",
                "ALTER COLUMN partner_user_id DROP NOT NULL",
                "'ASSIGNED'",
                "CREATE UNIQUE INDEX uq_progress_reminder_logs_progress_type",
                "CREATE INDEX ix_operation_tasks_open_resource_type"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v15MigrationContainsStandardDocumentFieldsAndBasicIncomeAdditions() throws IOException {
        String sql = selectV15Migration();

        assertThat(sql).contains(
                "CREATE TABLE standard_document_fields",
                "CONSTRAINT uq_standard_document_fields_key",
                "ADD COLUMN standard_field_id uuid",
                "ADD COLUMN income_presence_code varchar(30)",
                "ADD COLUMN annual_revenue numeric(18, 2)",
                "CREATE UNIQUE INDEX uq_matching_cases_no_verification",
                "'BUSINESS_REGISTRATION'",
                "'HEALTH_INSURANCE_QUALIFICATION'"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v16MigrationContainsMemberDocumentInputValues() throws IOException {
        String sql = selectV16Migration();

        assertThat(sql).contains(
                "CREATE TABLE member_document_input_values",
                "CONSTRAINT uq_member_document_input_values_field",
                "CONSTRAINT ck_member_document_input_values_single_value",
                "WORKPLACE_ADDRESS",
                "INSURED_PERSON_INFO"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v17MigrationContainsMockMonthlySubscriptionPlan() throws IOException {
        String sql = selectV17Migration();

        assertThat(sql).contains(
                "INSERT INTO subscription_plans",
                "SANEB_MONTHLY_MOCK",
                "사내비 월 구독",
                "MONTHLY",
                "12900.00",
                "ON CONFLICT (plan_code) DO UPDATE"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v19MigrationContainsMatchingStageFlow() throws IOException {
        String sql = selectV19Migration();

        assertThat(sql).contains(
                "ADD COLUMN matching_stage_code",
                "ADD COLUMN matching_basis_code",
                "CREATE UNIQUE INDEX uq_matching_cases_stage_no_verification",
                "'BASIC'",
                "'FINAL'",
                "'BASIC_INFO'",
                "'DOCUMENT_INPUT'"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v21MigrationContainsStandardCodeCatalogs() throws IOException {
        String sql = selectV21Migration();

        assertThat(sql).contains(
                "ADD COLUMN condition_usage_code",
                "'INPUT_ONLY'",
                "'CONDITION_READY'",
                "'STANDARDIZATION_REQUIRED'",
                "CREATE TABLE standard_code_groups",
                "CREATE TABLE standard_codes",
                "CREATE TABLE standard_field_code_groups",
                "CONSTRAINT uq_standard_code_groups_code",
                "CONSTRAINT uq_standard_codes_group_code",
                "CONSTRAINT ck_standard_field_code_groups_usage",
                "'KSIC_11'",
                "'REGION_SIDO'",
                "'HEALTH_INSURANCE_TYPE'",
                "'REFERENCE_MAPPING'"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v22MigrationContainsStructuredAddressFields() throws IOException {
        String sql = selectV22Migration();

        assertThat(sql).contains(
                "ADD COLUMN postal_code varchar(20)",
                "ADD COLUMN road_address varchar(500)",
                "ADD COLUMN legal_dong_code varchar(30)",
                "ADD COLUMN workplace_postal_code varchar(20)",
                "ADD COLUMN workplace_road_address varchar(500)",
                "ADD COLUMN workplace_legal_dong_code varchar(30)",
                "ck_member_profiles_address_source",
                "ck_business_profiles_workplace_address_source",
                "ix_member_profiles_legal_dong_code",
                "ix_business_profiles_workplace_legal_dong_code"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v25MigrationContainsMemberInterviewResponses() throws IOException {
        String sql = selectV25Migration();

        assertThat(sql).contains(
                "ADD COLUMN has_existing_loan boolean",
                "CREATE TABLE member_interview_responses",
                "CONSTRAINT uq_member_interview_responses_question",
                "CONSTRAINT ck_member_interview_responses_question",
                "SAME_BUSINESS_IN_PROGRESS",
                "DUPLICATE_SUPPORT_USAGE",
                "BUSINESS_ACTUALLY_OPERATING",
                "OTHER_RESTRICTION",
                "CONSTRAINT ck_member_interview_responses_answer"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v26MigrationContainsAnnouncementSourceCollectionTables() throws IOException {
        String sql = selectV26Migration();

        assertThat(sql).contains(
                "CREATE TABLE announcement_source_collection_requests",
                "CREATE TABLE announcement_source_collection_runs",
                "CREATE TABLE announcement_source_collection_run_items",
                "CREATE TABLE announcement_source_snapshots",
                "CREATE TABLE announcement_source_attachments",
                "CREATE TABLE announcement_source_highlights",
                "CREATE TABLE announcement_source_review_histories",
                "CREATE TABLE announcement_source_links",
                "'APPROVAL_PENDING'",
                "'BATCH'",
                "'MANUAL'",
                "'REVIEW_PENDING'",
                "'ACTIVATED'",
                "'SKIPPED_ENDED'"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v27MigrationContainsAnnouncementSourceDuplicateCandidates() throws IOException {
        String sql = selectV27Migration();

        assertThat(sql).contains(
                "CREATE TABLE announcement_source_duplicate_candidates",
                "CONSTRAINT uq_announcement_source_duplicate_candidates_source_announcement",
                "EXACT_DUPLICATE",
                "SIMILAR",
                "CREATE_NEW_SELECTED",
                "UPDATE_EXISTING_SELECTED",
                "IGNORED"
        );
    }

    /**
     * 지자체 URL 관리, URL별 결과, 교차 중복과 승인 스케줄 스키마를 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v28MigrationContainsLocalGovernmentNoticeCollectionContracts() throws IOException {
        String sql = selectV28Migration();

        assertThat(sql).contains(
                "CREATE TABLE local_government_notice_sources",
                "CREATE TABLE local_government_notice_parser_profiles",
                "CREATE TABLE announcement_source_collection_source_results",
                "CREATE TABLE announcement_source_snapshot_duplicates",
                "CREATE TABLE announcement_source_collection_schedules",
                "CREATE TABLE announcement_source_schedule_executions",
                "LOCAL_GOV_NOTICE",
                "ck_announcement_source_snapshot_duplicates_order",
                "uq_announcement_source_schedule_executions_slot"
        );
    }

    /**
     * 지자체 URL 정적 seed의 수량·보정·기본 OFF 정책을 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v29MigrationContainsReviewedLocalGovernmentNoticeSeed() throws IOException {
        String sql = selectV29Migration();

        assertThat(sql).contains(
                "Expected 244 local-government notice sources",
                "Expected 244 unique local-government district codes",
                "https://www.dalseong.daegu.kr/index.do?menu_id=00000194",
                "https://seohae.go.kr/open_content/main/community/news/gosi.jsp",
                "https://www.osan.go.kr/portal/saeol/gosi/list.do?mId=0302010000",
                "https://www.dh.go.kr/www/selectBbsNttList.do?bbsNo=87&key=478",
                "https://www.sangju.go.kr/page/10297/10606.tc",
                "'MANUAL_ONLY'",
                "false"
        );
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV1Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V1__create_mvp_schema.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV4Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V4__create_dynamic_announcement_inputs.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV6Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V6__allow_matching_without_verification.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV7Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V7__create_user_consents.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV8Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V8__create_document_file_submissions.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV9Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V9__create_consultation_reservations.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV10Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V10__create_subscription_payments.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV11Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V11__create_notifications_operation_tasks.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV12Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V12__create_admin_report_exports.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV14Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V14__add_reviewer_and_manual_consultation.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV15Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V15__create_standard_document_fields.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV16Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V16__create_member_document_input_values.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV17Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V17__seed_mock_subscription_plan.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV19Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V19__add_matching_stage_flow.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV21Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V21__create_standard_code_catalogs.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV22Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V22__add_structured_address_fields.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV25Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V25__add_member_interview_responses.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV26Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V26__create_announcement_source_collection.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV27Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V27__create_announcement_source_duplicate_candidates.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V28 migration을 UTF-8로 조회합니다.
     *
     * @return V28 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV28Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V28__create_local_government_notice_collection.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V29 migration을 UTF-8로 조회합니다.
     *
     * @return V29 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV29Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V29__seed_local_government_notice_sources.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
