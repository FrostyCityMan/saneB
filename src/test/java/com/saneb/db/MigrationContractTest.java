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
     * 제한형 휴리스틱 파서 프로필 추가 계약을 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v30MigrationContainsHeuristicLocalGovernmentParser() throws IOException {
        String sql = selectV30Migration();

        assertThat(sql).contains(
                "HEURISTIC_NOTICE",
                "제한형 공고 링크 탐색",
                "ck_local_government_notice_parser_profiles_type"
        );
    }

    /**
     * 전수 QA 통과·보류·실패 결과 반영 계약을 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v31MigrationContainsFullParserQaResult() throws IOException {
        String sql = selectV31Migration();

        assertThat(sql).contains(
                "WITH qa_pass",
                "('LGS-000002', 'HEURISTIC_NOTICE')",
                "('LGS-000244', 'SPRING_BBS')",
                "validation_status_code = 'CHECK_REQUIRED'",
                "validation_status_code = 'FAILED'",
                "is_enabled = false"
        );
    }

    /**
     * 검증 URL 보정과 기관별 HTTP 요청 프로필 계약을 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v32MigrationContainsReviewedUrlsAndRequestProfiles() throws IOException {
        String sql = selectV32Migration();

        assertThat(sql).contains(
                "ADD COLUMN request_profile_code",
                "BROWSER_HTTP1",
                "('LGS-000011', 'https://www.dobong.go.kr/bbs.asp?code=10008769')",
                "('LGS-000233', 'https://eminwon.haman.go.kr/emwp/jsp/ofr/OfrNotAncmtLSub.jsp?not_ancmt_se_code=01,04')",
                "validation_status_code = 'CHECK_REQUIRED'",
                "is_enabled = false"
        );
    }

    /**
     * 파서 보강 후 추가 전수 QA 통과 결과 계약을 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v33MigrationContainsHardenedParserQaResult() throws IOException {
        String sql = selectV33Migration();

        assertThat(sql).contains(
                "WITH qa_pass",
                "('LGS-000021', 'SPRING_BBS')",
                "('LGS-000066', 'HEURISTIC_NOTICE')",
                "('LGS-000117', 'CHUNCHEON_NOTICE_JSON')",
                "('LGS-000223', 'SPRING_BBS')",
                "('LGS-000239', 'HEURISTIC_NOTICE')",
                "validation_status_code = 'VERIFIED'"
        );
    }

    /**
     * 안전한 상세 링크 템플릿과 공통 플랫폼 프로필 계약을 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v34MigrationContainsSafeLinkTemplateProfiles() throws IOException {
        String sql = selectV34Migration();

        assertThat(sql).contains(
                "ADD COLUMN link_strategy_code",
                "SAFE_TEMPLATE",
                "SAFE_BOARD_VIEW",
                "SAFE_BOARD_VIEW_SITE",
                "SAFE_YH_BOARD_POST",
                "SAFE_ICMS_BOARD",
                "SAFE_OPENWORKS_BOARD",
                "SAFE_BD_SELECT_BBS",
                "SAFE_GOTO_VIEW",
                "SAFE_ICMS_BOARD_EXTENDED",
                "SAFE_ANSAN_BBS",
                "SAFE_GWD_BULLETIN",
                "SAFE_SANGJU_GOSI",
                "SAFE_GORYEONG_BOARD",
                "{arg:6}",
                "{attr:data-req-get-p-idx}",
                "{input:bbsId}"
        );
    }

    /**
     * 안전 링크 상세 URL 검증을 통과한 출처만 승격하는 계약을 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v35MigrationContainsSafeLinkTemplateQaResult() throws IOException {
        String sql = selectV35Migration();

        assertThat(sql).contains(
                "WITH qa_pass",
                "('LGS-000016', 'SAFE_YANGCHEON_SEOL')",
                "('LGS-000037', 'SAFE_BOARD_VIEW')",
                "('LGS-000049', 'SAFE_ICMS_BOARD_EXTENDED')",
                "('LGS-000082', 'SAFE_GOTO_VIEW')",
                "('LGS-000092', 'SAFE_ANSAN_BBS')",
                "('LGS-000098', 'SAFE_BOARD_VIEW_SITE')",
                "('LGS-000116', 'SAFE_GWD_BULLETIN')",
                "('LGS-000203', 'SAFE_GOTO_VIEW_EXTENDED')",
                "('LGS-000205', 'SAFE_YH_BOARD_POST')",
                "('LGS-000208', 'SAFE_SANGJU_GOSI')",
                "('LGS-000216', 'SAFE_GORYEONG_BOARD')",
                "('LGS-000231', 'SAFE_YH_BOARD_POST')",
                "validation_status_code = 'VERIFIED'",
                "is_enabled = false"
        );
    }

    /**
     * 공식 지자체 URL 보정이 자동 활성화 없이 적용되는지 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v36MigrationContainsReviewedOfficialUrls() throws IOException {
        String sql = selectV36Migration();

        assertThat(sql).contains(
                "WITH reviewed_url (public_code, notice_url)",
                "('LGS-000014', 'https://www.sdm.go.kr/news/notice.do')",
                "parser_profile_code = 'MANUAL_ONLY'",
                "validation_status_code = 'CHECK_REQUIRED'",
                "is_enabled = false"
        );
    }

    /**
     * 반복되는 공공 게시판 구조의 공통 프로필 계약을 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v37MigrationContainsCommonParserProfiles() throws IOException {
        String sql = selectV37Migration();

        assertThat(sql).contains(
                "SAFE_SEODAEMUN_NOTICE",
                "/news/notice/notice.do?mode=view&sdmBoardSeq={arg:1}",
                "SAFE_SAEOL_EMINWON",
                "SUBJECT_NOTICE_TABLE",
                "SCMS_CARD_NOTICE",
                "is_enabled = false"
        );
    }

    /**
     * JSON, 대전 통합, 셀 클릭형 등 잔여 공통 구조 지원 계약을 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v38MigrationContainsRemainingParserProfiles() throws IOException {
        String sql = selectV38Migration();

        assertThat(sql).contains(
                "DAEJEON_EMINWON",
                "SAFE_SAEOL_EMINWON_CELL",
                "table tr:has(td:nth-of-type(3)[onclick*=searchDetail])",
                "DAMYANG_NOTICE_JSON",
                "('LGS-000071', 'DAEJEON_EMINWON_AGGREGATOR')",
                "('LGS-000183', 'DAMYANG_NOTICE_JSON')",
                "is_enabled = false"
        );
    }

    /**
     * 공개 게시판 폼 POST와 최종 QA 승격 계약을 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v39MigrationContainsPostFormAndFinalQaResult() throws IOException {
        String sql = selectV39Migration();

        assertThat(sql).contains(
                "ADD COLUMN request_method_code",
                "ADD COLUMN request_form_json",
                "request_method_code = 'POST_FORM'",
                "selectListOfrNotAncmtHomepage",
                "('LGS-000089', 'SAFE_SAEOL_EMINWON')",
                "('LGS-000145', 'SAFE_SAEOL_EMINWON_CELL')",
                "validation_status_code = 'VERIFIED'",
                "is_enabled = false"
        );
    }

    /**
     * 244개 전수 QA의 통과 및 부분 통과 상태 동기화 계약을 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v40MigrationContainsComprehensiveQaResult() throws IOException {
        String sql = selectV40Migration();

        assertThat(sql).contains(
                "WITH qa_pass(public_code, parser_profile_code)",
                "('LGS-000014', 'SAFE_SEODAEMUN_NOTICE')",
                "('LGS-000071', 'DAEJEON_EMINWON_AGGREGATOR')",
                "('LGS-000089', 'SAFE_SAEOL_EMINWON')",
                "('LGS-000183', 'DAMYANG_NOTICE_JSON')",
                "('LGS-000244', 'SPRING_BBS')",
                "WITH qa_partial(public_code, parser_profile_code)",
                "validation_status_code = 'CHECK_REQUIRED'",
                "is_enabled = false"
        );
        assertThat(sql.split("\\('LGS-", -1).length - 1).isEqualTo(229);
    }

    /**
     * 추가 공식 URL 보정이 비활성 검증대기 상태를 유지하는지 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v41MigrationContainsAdditionalOfficialUrlCorrections() throws IOException {
        String sql = selectV41Migration();

        assertThat(sql).contains(
                "('LGS-000045', 'https://eminwon.jung.daegu.kr/",
                "('LGS-000130', 'https://eminwon.ihc.go.kr/",
                "('LGS-000141', 'https://www.yd21.go.kr/kr/html/sub02/020103.html?GotoPage=1&mode=L')",
                "('LGS-000230', 'https://www.geoje.go.kr/index.geoje?menuCd=DOM_000008902001002001')",
                "validation_status_code = 'CHECK_REQUIRED'",
                "is_enabled = false"
        );
    }

    /**
     * 좁은 공통 파서 프로필이 상세 링크 서명을 기준으로 등록되는지 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v42MigrationContainsNarrowReusableParserProfiles() throws IOException {
        String sql = selectV42Migration();

        assertThat(sql).contains(
                "SAFE_SAEOL_EMINWON_LEGACY",
                "SAFE_SAEOL_EMINWON_HREF",
                "SAFE_EGOV_DETAIL_BUTTON",
                "RFC_BLOGLIST_NOTICE",
                "GURYE_BOARD_NOTICE",
                "tr:has(td:nth-of-type(3) a[onclick*=searchDetail])",
                "tr:has(td.subject button[onclick*=fn_search_detail])",
                "is_enabled"
        );
        assertThat(sql.split("2a57f03e-2b48-4c3f-88cc-cc7bc1e142", -1).length - 1).isEqualTo(5);
    }

    /**
     * 격리 QA로 확인한 8개 수집원만 검증 완료로 승격하는지 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v43MigrationContainsNarrowParserQaResults() throws IOException {
        String sql = selectV43Migration();

        assertThat(sql).contains(
                "('LGS-000029', 'RFC_BLOGLIST_NOTICE')",
                "('LGS-000045', 'SAFE_SAEOL_EMINWON_LEGACY')",
                "('LGS-000074', 'SAFE_EGOV_DETAIL_BUTTON')",
                "('LGS-000130', 'SAFE_SAEOL_EMINWON_LEGACY')",
                "('LGS-000141', 'SAEOL_GOSI')",
                "('LGS-000161', 'SAFE_EGOV_DETAIL_BUTTON')",
                "('LGS-000185', 'GURYE_BOARD_NOTICE')",
                "('LGS-000230', 'SAEOL_GOSI')",
                "validation_status_code = 'VERIFIED'",
                "is_enabled = false"
        );
        assertThat(sql.split("\\('LGS-", -1).length - 1).isEqualTo(8);
    }

    /**
     * 공주형 전자정부 게시판 프로필 계약을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v44MigrationContainsSafeEgovDetailCellParserProfile() throws IOException {
        String sql = selectV44Migration();

        assertThat(sql).contains(
                "SAFE_EGOV_DETAIL_CELL",
                "table.table-default tbody tr:has(td.subject a[onclick*=fn_search_detail])",
                "td[data-cell-header=\"등록일\"]",
                "view.do?notAncmtMgtNo={arg:1}",
                "SAFE_TEMPLATE"
        );
    }

    /**
     * 공주시청의 실사이트 QA 결과 반영 계약을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v45MigrationAppliesGongjuQaResultWithoutEnablingCollection() throws IOException {
        String sql = selectV45Migration();

        assertThat(sql).contains(
                "public_code = 'LGS-000149'",
                "parser_profile_code = 'SAFE_EGOV_DETAIL_CELL'",
                "validation_status_code = 'VERIFIED'",
                "collection_status_code = 'READY'",
                "is_enabled = false"
        );
    }

    /**
     * 구형 공공사이트 호환 요청 프로필과 적용 대상을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v46MigrationAddsLegacyBrowserRequestProfile() throws IOException {
        String sql = selectV46Migration();

        assertThat(sql).contains(
                "'DEFAULT', 'BROWSER_HTTP1', 'LEGACY_BROWSER'",
                "request_profile_code = 'LEGACY_BROWSER'",
                "'LGS-000008'",
                "'LGS-000093'",
                "'LGS-000148'",
                "'LGS-000158'",
                "validation_status_code = 'CHECK_REQUIRED'",
                "is_enabled = false"
        );
    }

    /**
     * 구형 게시판별 좁은 파서와 안전한 상세 URL 계약을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v47MigrationAddsLegacyBoardParserProfiles() throws IOException {
        String sql = selectV47Migration();

        assertThat(sql).contains(
                "JUNGNANG_CONTEST_BOARD",
                "SAFE_PYEONGTAEK_BOARD_RENEWAL",
                "SAFE_EGOV_BOARD_BUTTON",
                "SAFE_EGOV_DATA_BUTTON",
                "/pyeongtaek/board/post/view.do?bcIdx={arg:4}&idx={arg:5}&mid={arg:6}",
                "view.do?nttId={attr:data-ntt-id}",
                "SAFE_TEMPLATE"
        );
    }

    /**
     * 구형 게시판 실사이트 QA 결과와 중랑구 보류 사유를 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v48MigrationAppliesLegacyBoardQaResults() throws IOException {
        String sql = selectV48Migration();

        assertThat(sql).contains(
                "('LGS-000093', 'SAFE_PYEONGTAEK_BOARD_RENEWAL')",
                "('LGS-000148', 'SAFE_EGOV_BOARD_BUTTON')",
                "('LGS-000158', 'SAFE_EGOV_DATA_BUTTON')",
                "validation_status_code = 'VERIFIED'",
                "public_code = 'LGS-000008'",
                "last_error_code = 'STALE_SOURCE_CONTENT'",
                "is_enabled = false"
        );
    }

    /**
     * 은평·강릉·순천의 현재 공식 목록 URL과 요청 프로필을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v49MigrationCorrectsRemainingOfficialNoticeUrls() throws IOException {
        String sql = selectV49Migration();

        assertThat(sql).contains(
                "https://eminwon.ep.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do",
                "https://www.gn.go.kr/www/selectGosiNttList.do",
                "https://www.suncheon.go.kr/kr/news/0001/0001/?mode=list",
                "'LEGACY_BROWSER'",
                "validation_status_code = 'CHECK_REQUIRED'",
                "is_enabled = false"
        );
    }

    /**
     * 축약 열 구조의 새올 전자민원 파서 계약을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v50MigrationAddsCompactSaeolParserProfile() throws IOException {
        String sql = selectV50Migration();

        assertThat(sql).contains(
                "SAFE_SAEOL_EMINWON_COMPACT",
                "table.board1 tbody tr:has(td:nth-of-type(2) a[onclick*=searchDetail])",
                "td:nth-of-type(4)",
                "not_ancmt_mgt_no={arg:1}",
                "SAFE_TEMPLATE"
        );
    }

    /**
     * 공식 URL 교체 후 은평·강릉·순천의 실사이트 QA 결과를 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v51MigrationAppliesCorrectedOfficialUrlQaResults() throws IOException {
        String sql = selectV51Migration();

        assertThat(sql).contains(
                "('LGS-000013', 'SAFE_SAEOL_EMINWON_COMPACT')",
                "('LGS-000119', 'SAEOL_GOSI')",
                "('LGS-000180', 'SUBJECT_NOTICE_TABLE')",
                "validation_status_code = 'VERIFIED'",
                "collection_status_code = 'READY'",
                "is_enabled = false"
        );
    }

    /**
     * 강동구의 느린 전자민원 응답을 명시적 호환 요청으로 분리하는 계약을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v52MigrationStabilizesGangdongRequestProfile() throws IOException {
        String sql = selectV52Migration();

        assertThat(sql).contains(
                "request_profile_code = 'LEGACY_BROWSER'",
                "public_code IN ('LGS-000026')",
                "validation_status_code = 'CHECK_REQUIRED'",
                "is_enabled = false"
        );
    }

    /**
     * 강동구 호환 전송 적용 후 실사이트 QA 결과를 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v53MigrationAppliesGangdongTransportQaResult() throws IOException {
        String sql = selectV53Migration();

        assertThat(sql).contains(
                "public_code = 'LGS-000026'",
                "parser_profile_code = 'SAFE_SAEOL_EMINWON_HREF'",
                "validation_status_code = 'VERIFIED'",
                "collection_status_code = 'READY'",
                "is_enabled = false"
        );
    }

    /**
     * 잔여 3개 실사이트 QA 통과 결과가 수동 파서를 대체하는지 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v54MigrationAppliesRemainingVerifiedParserResults() throws IOException {
        String sql = selectV54Migration();

        assertThat(sql).contains(
                "('LGS-000034', 'SAFE_SAEOL_EMINWON_LEGACY')",
                "('LGS-000108', 'SAEOL_GOSI')",
                "('LGS-000135', 'SAEOL_GOSI')",
                "validation_status_code = 'VERIFIED'",
                "collection_status_code = 'READY'",
                "is_enabled = false"
        );
    }

    /**
     * 잔여 지자체의 현재 공식 공고 목록과 수집 endpoint 복구 계약을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v55MigrationRecoversRemainingLocalGovernmentNoticeSources() throws IOException {
        String sql = selectV55Migration();

        assertThat(sql).contains(
                "'LGS-000008'",
                "https://www.jungnang.go.kr/portal/bbs/list/B0000117.do?menuNo=200475",
                "'SPRING_BBS'",
                "'LGS-000019'",
                "https://www.geumcheon.go.kr/portal/tblSeolGosiDetailList.do?key=294&rep=1",
                "'SAEOL_GOSI'",
                "'LGS-000036'",
                "https://www.haeundae.go.kr/board/list.do?boardId=BBS_0000038",
                "'LGS-000089'",
                "https://www.seongnam.go.kr/notice/publicNotice.do?menuIdx=1000499&returnURL=/main.do",
                "http://eminwon.seongnam.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do",
                "request_method_code = 'POST_FORM'",
                "'LGS-000094'",
                "https://www.anyang.go.kr/main/emwsWebList.do",
                "'LGS-000122'",
                "https://www.sokcho.go.kr/sc/portal/sokchonews/notification",
                "http://eminwon.sokcho.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do",
                "http://eminwon.gangdong.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do",
                "http://eminwon.bsnamgu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do",
                "request_profile_code = 'LEGACY_BROWSER'",
                "WHERE public_code IN ('LGS-000034')",
                "'LGS-000167'",
                "https://www.jeongeup.go.kr/board/list.jeongeup?boardId=BBS_0000012",
                "'LGS-000174'",
                "https://www.imsil.go.kr/board/list.imsil?boardId=BBS_0000002",
                "WHERE public_code IN ('LGS-000036', 'LGS-000167', 'LGS-000174')",
                "'LGS-000224'",
                "https://www.changwon.go.kr/cwportal/10310/10438/10439.web?section=gosi",
                "request_profile_code = 'BROWSER_HTTP1'",
                "validation_status_code = 'VERIFIED'",
                "collection_status_code = 'READY'",
                "is_enabled = false"
        );
    }

    /**
     * 안양시·밀양시·함양군의 공식 고시·공고 URL 및 기존 공통 파서 재사용 계약을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v57MigrationCorrectsMiryangAndHamyangOfficialNoticeSources() throws IOException {
        String sql = selectV57Migration();

        assertThat(sql).contains(
                "'LGS-000094'",
                "https://www.anyang.go.kr/main/selectEminwonList.do",
                "parser_profile_code = 'SPRING_BBS'",
                "'LGS-000229'",
                "https://miryang.go.kr/web/eMiryangMinwonList.do",
                "parser_profile_code = 'SPRING_BBS'",
                "'LGS-000239'",
                "https://eminwon.hygn.go.kr/emwp/jsp/ofr/OfrNotAncmtLSub.jsp",
                "'LGS-000230'",
                "https://www.geoje.go.kr/index.geoje?menuCd=DOM_000008902001002001&startPage=1",
                "'LGS-000059'",
                "https://biz.namdong.go.kr/main/news/announce.jsp",
                "'LGS-000122'",
                "https://www.sokcho.go.kr/sc/portal/sokchonews/notification",
                "https://eminwon.hygn.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do",
                "parser_profile_code = 'SAFE_SAEOL_EMINWON'",
                "request_method_code = 'POST_FORM'",
                "\"not_ancmt_se_code\":\"01,02,03,04,07\"",
                "source_board_type_code = 'LEGAL_NOTICE'",
                "collection_policy_code = 'COLLECT_ALL'",
                "'TLS12_BROWSER'",
                "request_profile_code = 'TLS12_BROWSER'",
                "WHERE public_code IN ('LGS-000094')",
                "WHERE public_code IN ('LGS-000059', 'LGS-000078', 'LGS-000229', 'LGS-000239')",
                "WHERE public_code IN ('LGS-000122')",
                "is_enabled = false"
        );
    }

    /**
     * 밀양시·함양군 사용자 바로가기와 내부 수집 endpoint 분리 계약을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v58MigrationSeparatesOfficialNoticeLinksFromCollectionEndpoints() throws IOException {
        String sql = selectV58Migration();

        assertThat(sql).contains(
                "'LGS-000229'",
                "https://www.miryang.go.kr/web/eMiryangMinwonList.do",
                "'LGS-000239'",
                "https://www.hygn.go.kr/00429/00543/00549.web",
                "https://eminwon.hygn.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do",
                "is_enabled = false"
        );
    }

    /**
     * 새올 셀 클릭형 파서가 상위 레이아웃 행을 공고 행으로 중복 선택하지 않는지 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v59MigrationSelectsOnlyDirectSaeolNoticeRows() throws IOException {
        String sql = selectV59Migration();

        assertThat(sql).contains(
                "profile_code = 'SAFE_SAEOL_EMINWON_CELL'",
                "tr:has(> td:nth-of-type(3)[onclick*=searchDetail])"
        );
    }

    /**
     * 남은 부분 실패 출처가 실제 공고 행만 선택하는 좁은 프로필을 사용하는지 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v60MigrationStabilizesRemainingPartialRows() throws IOException {
        String sql = selectV60Migration();

        assertThat(sql).contains(
                "td.subject > a, td.title > a, td.bb-list-title > a",
                "'SEONGBUK_EMINWON_TABLE'",
                "table.p-table.simple tbody.text_center > tr:has(> td.p-subject > a)",
                "'CHANGWON_GOSI_TABLE'",
                "table.t3 tbody.tb > tr:has(> td.tal > a.a1)",
                "public_code = 'LGS-000009'",
                "public_code = 'LGS-000224'"
        );
    }

    /**
     * 일반 공지 출처를 공식 고시공고 게시판으로 보정하는 정적 계약을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v61MigrationCorrectsReviewedSourcesToOfficialLegalNoticeBoards() throws IOException {
        String sql = selectV61Migration();
        String reviewedSources = sql.substring(
                sql.indexOf("WITH reviewed_source ("),
                sql.indexOf("UPDATE local_government_notice_sources AS source", sql.indexOf("WITH reviewed_source ("))
        );

        assertThat(countOccurrences(reviewedSources, "('LGS-")).isEqualTo(203);
        assertThat(sql).contains(
                "source_board_type_code = 'LEGAL_NOTICE'",
                "collection_policy_code = 'KEYWORD_FILTERED'",
                "validation_status_code = 'VERIFIED'",
                "is_semantically_verified = true",
                "parser_profile_code = 'MANUAL_ONLY'",
                "request_method_code = 'POST_FORM'",
                "'SAFE_SAEOL_EMINWON_LIST'",
                "'MAPO_LEGAL_NOTICE_TABLE'",
                "'SAFE_DAEGU_LEGAL_NOTICE'",
                "'SAFE_INCHEON_CITYNET_NOTICE'",
                "'SAFE_GWANGJU_NAMGU_NOTICE'",
                "'SAFE_DAEJEON_DATA_KEY_NOTICE'",
                "'SAFE_YUSEONG_LEGAL_NOTICE'",
                "'SAFE_HWASEONG_LEGAL_NOTICE'",
                "'SAFE_PORTAL_SAEOL_BOARD_VIEW'",
                "'SAFE_GWANGMYEONG_LEGAL_NOTICE'",
                "'SAFE_EGOV_DATA_LIST_NOTICE'",
                "'YEONGCHEON_LEGAL_NOTICE'",
                "https://www.gangnam.go.kr/notice/list.do?mid=ID05_040201",
                "https://www.donggu.go.kr/dg/kor/contents/916",
                "https://www.yongin.go.kr/home/yiNw/yiNwStable/yiNwStable02/yiNwStable02_01.jsp",
                "https://www.seocheon.go.kr/prog/saeolGosi/03/kor/sub04_06_03/list.do",
                "'LGS-000158', 'https://eminwon.seocheon.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do'"
        );
        assertThat(sql.toLowerCase()).doesNotContain("delete from", "truncate table");
    }

    /**
     * 전수 QA를 통과한 출처만 활성화하고 실패 출처는 진단값과 함께 격리하는 계약을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v62MigrationActivatesOnlyQaPassedSourcesAndIsolatesFailures() throws IOException {
        String sql = selectV62Migration();
        String passedSources = sql.substring(
                sql.indexOf("WITH qa_pass_source"),
                sql.indexOf("UPDATE local_government_notice_sources AS source")
        );
        String blockedSources = sql.substring(
                sql.indexOf("WITH qa_blocked_source"),
                sql.indexOf("UPDATE local_government_notice_sources AS source", sql.indexOf("WITH qa_blocked_source"))
        );

        assertThat(countOccurrences(passedSources, "('LGS-")).isEqualTo(190);
        assertThat(countOccurrences(blockedSources, "('LGS-")).isEqualTo(13);
        assertThat(sql).contains(
                "is_enabled = true",
                "is_enabled = false",
                "'ITEM_FIELDS_MISSING'",
                "'LIST_SELECTOR_NOT_MATCHED'",
                "'HTTP_ERROR'",
                "enabled_count <> 190 OR blocked_count <> 13"
        );
        assertThat(sql.toLowerCase()).doesNotContain("delete from", "truncate table");
    }

    /**
     * 수집 승인 요청 INSERT가 nullable UUID의 PostgreSQL 타입을 명시하는지 확인합니다.
     *
     * @throws IOException Mapper 읽기 오류
     */
    @Test
    void announcementSourceMapperCastsNullableApprovedByAsUuid() throws IOException {
        String mapper = selectAnnouncementSourceMapper();

        assertThat(mapper)
                .contains("CASE WHEN CAST(#{approvedBy} AS uuid) IS NOT NULL THEN now() ELSE NULL END")
                .contains(
                        "<arg column=\"total_count\" javaType=\"_int\"/>",
                        "<arg column=\"collected_count\" javaType=\"_int\"/>",
                        "<arg column=\"skipped_ended_count\" javaType=\"_int\"/>",
                        "<arg column=\"duplicate_count\" javaType=\"_int\"/>",
                        "<arg column=\"failed_count\" javaType=\"_int\"/>"
                )
                .doesNotContain("CASE WHEN #{approvedBy} IS NOT NULL THEN now() ELSE NULL END");
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

    /**
     * V30 migration을 UTF-8로 조회합니다.
     *
     * @return V30 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV30Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V30__add_heuristic_local_government_notice_parser.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V31 migration을 UTF-8로 조회합니다.
     *
     * @return V31 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV31Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V31__apply_local_government_parser_qa_results.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V32 migration을 UTF-8로 조회합니다.
     *
     * @return V32 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV32Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V32__harden_local_government_notice_source_requests.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V33 migration을 UTF-8로 조회합니다.
     *
     * @return V33 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV33Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V33__apply_local_government_parser_qa_hardening_results.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V34 migration을 UTF-8로 조회합니다.
     *
     * @return V34 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV34Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V34__add_safe_local_government_link_templates.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V35 migration을 UTF-8로 조회합니다.
     *
     * @return V35 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV35Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V35__apply_safe_link_template_parser_qa_results.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V36 migration을 UTF-8로 조회합니다.
     *
     * @return V36 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV36Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V36__correct_official_local_government_notice_urls.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V37 migration을 UTF-8로 조회합니다.
     *
     * @return V37 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV37Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V37__add_common_local_government_notice_parser_profiles.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V38 migration을 UTF-8로 조회합니다.
     *
     * @return V38 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV38Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V38__support_remaining_local_government_notice_structures.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V39 migration을 UTF-8로 조회합니다.
     *
     * @return V39 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV39Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V39__correct_final_reviewed_local_government_notice_urls.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V40 migration을 UTF-8로 조회합니다.
     *
     * @return V40 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV40Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V40__apply_comprehensive_local_government_parser_qa_results.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V41 migration을 UTF-8로 조회합니다.
     *
     * @return V41 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV41Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V41__correct_additional_official_local_government_notice_urls.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V42 migration을 UTF-8로 조회합니다.
     *
     * @return V42 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV42Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V42__add_narrow_local_government_notice_parser_profiles.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V43 migration을 UTF-8로 조회합니다.
     *
     * @return V43 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV43Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V43__apply_narrow_parser_qa_results.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V44 migration을 UTF-8로 조회합니다.
     *
     * @return V44 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV44Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V44__add_egov_detail_cell_parser_profile.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V45 migration을 UTF-8로 조회합니다.
     *
     * @return V45 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV45Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V45__apply_gongju_parser_qa_result.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V46 migration을 UTF-8로 조회합니다.
     *
     * @return V46 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV46Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V46__add_legacy_browser_request_profile.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V47 migration을 UTF-8로 조회합니다.
     *
     * @return V47 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV47Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V47__add_legacy_board_parser_profiles.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V48 migration을 UTF-8로 조회합니다.
     *
     * @return V48 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV48Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V48__apply_legacy_board_parser_qa_results.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V49 migration을 UTF-8로 조회합니다.
     *
     * @return V49 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV49Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V49__correct_remaining_official_notice_urls.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V50 migration을 UTF-8로 조회합니다.
     *
     * @return V50 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV50Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V50__add_compact_saeol_parser_profile.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V51 migration을 UTF-8로 조회합니다.
     *
     * @return V51 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV51Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V51__apply_corrected_official_url_qa_results.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V52 migration을 UTF-8로 조회합니다.
     *
     * @return V52 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV52Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V52__stabilize_gangdong_notice_request.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V53 migration을 UTF-8로 조회합니다.
     *
     * @return V53 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV53Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V53__apply_gangdong_transport_qa_result.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V54 migration을 UTF-8로 조회합니다.
     *
     * @return V54 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV54Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V54__apply_remaining_verified_parser_results.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V55 migration을 UTF-8로 조회합니다.
     *
     * @return V55 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV55Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V55__recover_remaining_local_government_notice_sources.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V57 migration을 UTF-8로 조회합니다.
     *
     * @return V57 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV57Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V57__correct_miryang_hamyang_official_notice_sources.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V58 migration을 UTF-8로 조회합니다.
     *
     * @return V58 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV58Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V58__separate_official_notice_links_from_collection_endpoints.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V59 migration을 UTF-8로 조회합니다.
     *
     * @return V59 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV59Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V59__tighten_local_government_notice_row_detection.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V60 migration을 UTF-8로 조회합니다.
     *
     * @return V60 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV60Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V60__stabilize_remaining_local_government_notice_rows.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V61 migration을 UTF-8로 조회합니다.
     *
     * @return V61 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV61Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V61__correct_general_notice_sources_to_official_legal_boards.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V62 migration을 UTF-8로 조회합니다.
     *
     * @return V62 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV62Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V62__apply_corrected_legal_notice_parser_qa_results.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 대상 문자열에서 검색 문자열이 나타난 횟수를 계산합니다.
     *
     * @param source 대상 문자열
     * @param token 검색 문자열
     * @return 검색 문자열 출현 횟수
     */
    private int countOccurrences(String source, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }

    /**
     * 외부 공고 수집 Mapper를 UTF-8로 조회합니다.
     *
     * @return 외부 공고 수집 Mapper XML
     * @throws IOException Mapper 읽기 오류
     */
    private String selectAnnouncementSourceMapper() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "mapper/announcementsource/AnnouncementSourceMapper.xml"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
