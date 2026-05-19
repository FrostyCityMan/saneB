CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE roles (
    role_code varchar(30) PRIMARY KEY,
    role_name varchar(100) NOT NULL,
    sort_order integer NOT NULL,
    CONSTRAINT uq_roles_role_name UNIQUE (role_name),
    CONSTRAINT ck_roles_role_code CHECK (role_code IN ('USER', 'PARTNER', 'OPERATOR', 'APPROVER', 'ADMIN'))
);

CREATE TABLE users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    login_id varchar(100) NOT NULL,
    password_hash varchar(255) NOT NULL,
    name varchar(100) NOT NULL,
    phone varchar(30),
    email varchar(255),
    status_code varchar(30) NOT NULL DEFAULT 'ACTIVE',
    password_reset_required boolean NOT NULL DEFAULT true,
    last_login_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT uq_users_login_id UNIQUE (login_id),
    CONSTRAINT uq_users_phone UNIQUE (phone),
    CONSTRAINT ck_users_status_code CHECK (status_code IN ('ACTIVE', 'LOCKED', 'DISABLED', 'DELETED'))
);

CREATE INDEX ix_users_status_code ON users (status_code);

CREATE TABLE user_roles (
    user_id uuid NOT NULL,
    role_code varchar(30) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    PRIMARY KEY (user_id, role_code),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_code) REFERENCES roles (role_code)
);

CREATE INDEX ix_user_roles_role_code ON user_roles (role_code);

CREATE TABLE auth_login_histories (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid,
    login_id varchar(100) NOT NULL,
    login_result_code varchar(30) NOT NULL,
    ip_address inet,
    user_agent varchar(500),
    failure_reason_code varchar(100),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_auth_login_histories_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_auth_login_histories_result CHECK (login_result_code IN ('SUCCESS', 'FAIL'))
);

CREATE INDEX ix_auth_login_histories_user_created_at ON auth_login_histories (user_id, created_at);
CREATE INDEX ix_auth_login_histories_login_id_created_at ON auth_login_histories (login_id, created_at);

CREATE TABLE member_profiles (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    birth_year integer,
    address varchar(500),
    region_code varchar(30),
    is_householder boolean NOT NULL DEFAULT false,
    is_household_member boolean NOT NULL DEFAULT false,
    health_insurance_basis_code varchar(50),
    has_income boolean,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_member_profiles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_member_profiles_user UNIQUE (user_id),
    CONSTRAINT ck_member_profiles_birth_year CHECK (birth_year IS NULL OR birth_year BETWEEN 1900 AND 2200)
);

CREATE INDEX ix_member_profiles_region_code ON member_profiles (region_code);

CREATE TABLE business_profiles (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    representative_name varchar(100),
    business_registration_no varchar(30) NOT NULL,
    business_name varchar(200) NOT NULL,
    workplace_address varchar(500),
    workplace_region_code varchar(30),
    opening_date date,
    industry_name varchar(200),
    business_category varchar(200),
    business_item varchar(200),
    ksic_code varchar(30),
    business_type_code varchar(50),
    company_stage_code varchar(50),
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_business_profiles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_business_profiles_business_registration_no UNIQUE (business_registration_no),
    CONSTRAINT ck_business_profiles_business_type CHECK (
        business_type_code IS NULL OR business_type_code IN (
            'SOLE_PROPRIETOR', 'CORPORATION', 'SIMPLIFIED_TAXPAYER', 'GENERAL_TAXPAYER', 'TAX_EXEMPT'
        )
    ),
    CONSTRAINT ck_business_profiles_company_stage CHECK (
        company_stage_code IS NULL OR company_stage_code IN (
            'PRE_STARTUP', 'EARLY_STARTUP', 'OPERATING', 'SUSPENDED', 'CLOSURE_PLANNED', 'CLOSED', 'RESTART_PREPARING'
        )
    )
);

CREATE INDEX ix_business_profiles_user_id ON business_profiles (user_id);
CREATE INDEX ix_business_profiles_ksic_code ON business_profiles (ksic_code);
CREATE INDEX ix_business_profiles_workplace_region_code ON business_profiles (workplace_region_code);

CREATE TABLE family_members (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    relation_type_code varchar(30) NOT NULL,
    birth_year integer,
    address varchar(500),
    school_age_status_code varchar(50),
    enrollment_status_code varchar(50),
    is_cohabiting boolean,
    is_supported boolean,
    has_income boolean,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_family_members_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_family_members_relation_type CHECK (relation_type_code IN ('SPOUSE', 'CHILD', 'PARENT')),
    CONSTRAINT ck_family_members_birth_year CHECK (birth_year IS NULL OR birth_year BETWEEN 1900 AND 2200)
);

CREATE INDEX ix_family_members_user_relation ON family_members (user_id, relation_type_code);

CREATE TABLE partner_profiles (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    partner_name varchar(200) NOT NULL,
    business_registration_no varchar(30) NOT NULL,
    status_code varchar(30) NOT NULL DEFAULT 'PENDING',
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_partner_profiles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_partner_profiles_user UNIQUE (user_id),
    CONSTRAINT uq_partner_profiles_business_registration_no UNIQUE (business_registration_no),
    CONSTRAINT ck_partner_profiles_status CHECK (status_code IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'TERMINATED'))
);

CREATE INDEX ix_partner_profiles_status_code ON partner_profiles (status_code);

CREATE TABLE partner_verifications (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    member_user_id uuid NOT NULL,
    partner_user_id uuid NOT NULL,
    business_profile_id uuid,
    status_code varchar(30) NOT NULL DEFAULT 'DRAFT',
    is_current boolean NOT NULL DEFAULT true,
    is_matching_blocked boolean NOT NULL DEFAULT false,
    submitted_at timestamptz,
    verified_at timestamptz,
    reviewed_by uuid,
    review_note text,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_partner_verifications_member_user FOREIGN KEY (member_user_id) REFERENCES users (id),
    CONSTRAINT fk_partner_verifications_partner_user FOREIGN KEY (partner_user_id) REFERENCES users (id),
    CONSTRAINT fk_partner_verifications_business_profile FOREIGN KEY (business_profile_id) REFERENCES business_profiles (id),
    CONSTRAINT fk_partner_verifications_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (id),
    CONSTRAINT ck_partner_verifications_status CHECK (status_code IN ('DRAFT', 'SUBMITTED', 'REVIEWING', 'VERIFIED', 'REJECTED', 'EXPIRED'))
);

CREATE UNIQUE INDEX uq_partner_verifications_current_member ON partner_verifications (member_user_id) WHERE is_current = true;
CREATE INDEX ix_partner_verifications_partner_status ON partner_verifications (partner_user_id, status_code);
CREATE INDEX ix_partner_verifications_member_status ON partner_verifications (member_user_id, status_code);

CREATE TABLE verification_member_values (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    verification_id uuid NOT NULL,
    birth_year integer,
    address varchar(500),
    region_code varchar(30),
    is_householder boolean,
    is_household_member boolean,
    health_insurance_basis_code varchar(50),
    has_income boolean,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_verification_member_values_verification FOREIGN KEY (verification_id) REFERENCES partner_verifications (id),
    CONSTRAINT uq_verification_member_values_verification UNIQUE (verification_id),
    CONSTRAINT ck_verification_member_values_birth_year CHECK (birth_year IS NULL OR birth_year BETWEEN 1900 AND 2200)
);

CREATE INDEX ix_verification_member_values_region_code ON verification_member_values (region_code);

CREATE TABLE verification_business_values (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    verification_id uuid NOT NULL,
    annual_revenue numeric(18, 2),
    employee_count integer,
    regular_employee_count integer,
    tax_status_code varchar(50),
    nice_credit_score integer,
    kcb_credit_score integer,
    has_existing_loan boolean,
    has_policy_fund_usage boolean,
    has_guarantee_usage boolean,
    financial_checked_on date,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_verification_business_values_verification FOREIGN KEY (verification_id) REFERENCES partner_verifications (id),
    CONSTRAINT uq_verification_business_values_verification UNIQUE (verification_id),
    CONSTRAINT ck_verification_business_values_employee_count CHECK (employee_count IS NULL OR employee_count >= 0),
    CONSTRAINT ck_verification_business_values_regular_employee_count CHECK (regular_employee_count IS NULL OR regular_employee_count >= 0),
    CONSTRAINT ck_verification_business_values_nice_score CHECK (nice_credit_score IS NULL OR nice_credit_score BETWEEN 0 AND 1000),
    CONSTRAINT ck_verification_business_values_kcb_score CHECK (kcb_credit_score IS NULL OR kcb_credit_score BETWEEN 0 AND 1000)
);

CREATE TABLE verification_family_values (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    verification_id uuid NOT NULL,
    relation_type_code varchar(30) NOT NULL,
    birth_year integer,
    address varchar(500),
    school_age_status_code varchar(50),
    enrollment_status_code varchar(50),
    is_cohabiting boolean,
    is_supported boolean,
    has_income boolean,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_verification_family_values_verification FOREIGN KEY (verification_id) REFERENCES partner_verifications (id),
    CONSTRAINT ck_verification_family_values_relation_type CHECK (relation_type_code IN ('SPOUSE', 'CHILD', 'PARENT')),
    CONSTRAINT ck_verification_family_values_birth_year CHECK (birth_year IS NULL OR birth_year BETWEEN 1900 AND 2200)
);

CREATE INDEX ix_verification_family_values_verification_relation ON verification_family_values (verification_id, relation_type_code);

CREATE TABLE verification_restriction_flags (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    verification_id uuid NOT NULL,
    restriction_code varchar(80) NOT NULL,
    is_checked boolean NOT NULL DEFAULT false,
    note text,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_verification_restriction_flags_verification FOREIGN KEY (verification_id) REFERENCES partner_verifications (id),
    CONSTRAINT uq_verification_restriction_flags_code UNIQUE (verification_id, restriction_code),
    CONSTRAINT ck_verification_restriction_flags_code CHECK (
        restriction_code IN (
            'SAME_BUSINESS_SUSPECTED', 'SPOUSE_TRANSFER_SUSPECTED', 'FAMILY_BYPASS_SUSPECTED',
            'CLOSED_REOPEN_SUSPECTED', 'POLICY_FUND_RESTRICTED', 'GUARANTEE_RESTRICTED',
            'CREDIT_RECOVERY', 'PERSONAL_REHABILITATION', 'BANKRUPTCY_HISTORY',
            'TAX_DELINQUENCY', 'OVERDUE_HISTORY', 'NEEDS_REVIEW'
        )
    )
);

CREATE TABLE verification_documents (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    verification_id uuid NOT NULL,
    document_type_code varchar(80) NOT NULL,
    source_type_code varchar(50) NOT NULL,
    is_checked boolean NOT NULL DEFAULT false,
    checked_by uuid,
    checked_at timestamptz,
    note text,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_verification_documents_verification FOREIGN KEY (verification_id) REFERENCES partner_verifications (id),
    CONSTRAINT fk_verification_documents_checked_by FOREIGN KEY (checked_by) REFERENCES users (id),
    CONSTRAINT uq_verification_documents_type UNIQUE (verification_id, document_type_code),
    CONSTRAINT ck_verification_documents_source_type CHECK (source_type_code IN ('USER_UPLOAD', 'E_CERT', 'PARTNER_CHECK', 'OPERATOR_CHECK')),
    CONSTRAINT ck_verification_documents_type CHECK (
        document_type_code IN (
            'BUSINESS_REGISTRATION', 'VAT_TAX_BASE', 'TAX_EXEMPT_INCOME', 'INCOME_CERTIFICATE',
            'NATIONAL_TAX_PAID', 'LOCAL_TAX_PAID', 'RESIDENT_REGISTRATION', 'FAMILY_RELATION',
            'HEALTH_INSURANCE_PAYMENT', 'HEALTH_INSURANCE_QUALIFICATION'
        )
    )
);

CREATE INDEX ix_verification_documents_checked ON verification_documents (verification_id, is_checked);

CREATE TABLE announcements (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    target_type_code varchar(30) NOT NULL,
    title varchar(300) NOT NULL,
    agency_name varchar(200) NOT NULL,
    summary text,
    application_start_date date,
    application_end_date date,
    manual_status_code varchar(30) NOT NULL DEFAULT 'NORMAL',
    approval_status_code varchar(30) NOT NULL DEFAULT 'DRAFT',
    income_judgement_code varchar(50) NOT NULL DEFAULT 'NO_LIMIT',
    min_amount numeric(18, 2),
    max_amount numeric(18, 2),
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_announcements_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_announcements_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT uq_announcements_agency_title_start UNIQUE (agency_name, title, application_start_date),
    CONSTRAINT ck_announcements_target_type CHECK (target_type_code IN ('BUSINESS', 'PERSONAL', 'SPOUSE', 'CHILD', 'PARENT')),
    CONSTRAINT ck_announcements_manual_status CHECK (manual_status_code IN ('NORMAL', 'PAUSED', 'EARLY_CLOSED', 'SUSPENDED', 'BUDGET_EXHAUSTED', 'CLOSED', 'HIDDEN')),
    CONSTRAINT ck_announcements_approval_status CHECK (approval_status_code IN ('DRAFT', 'REQUESTED', 'APPROVED', 'REJECTED', 'CANCELED')),
    CONSTRAINT ck_announcements_income_judgement CHECK (
        income_judgement_code IN (
            'INCOME_CERT_ONLY', 'HEALTH_INSURANCE_ONLY', 'VAT_TAX_BASE_ONLY',
            'ANY_ONE_DOCUMENT', 'INCOME_OR_HEALTH_INSURANCE', 'NO_LIMIT'
        )
    ),
    CONSTRAINT ck_announcements_amount_range CHECK (
        min_amount IS NULL OR max_amount IS NULL OR min_amount <= max_amount
    )
);

CREATE INDEX ix_announcements_target_dates ON announcements (target_type_code, application_start_date, application_end_date);
CREATE INDEX ix_announcements_status ON announcements (manual_status_code, approval_status_code);

CREATE TABLE announcement_options (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    announcement_id uuid NOT NULL,
    option_group_code varchar(80) NOT NULL,
    option_code varchar(80) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    CONSTRAINT fk_announcement_options_announcement FOREIGN KEY (announcement_id) REFERENCES announcements (id),
    CONSTRAINT uq_announcement_options_code UNIQUE (announcement_id, option_group_code, option_code)
);

CREATE TABLE announcement_approval_requests (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    announcement_id uuid NOT NULL,
    requested_by uuid NOT NULL,
    decided_by uuid,
    approval_status_code varchar(30) NOT NULL DEFAULT 'REQUESTED',
    request_note text,
    decision_note text,
    requested_at timestamptz NOT NULL DEFAULT now(),
    decided_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_announcement_approval_requests_announcement FOREIGN KEY (announcement_id) REFERENCES announcements (id),
    CONSTRAINT fk_announcement_approval_requests_requested_by FOREIGN KEY (requested_by) REFERENCES users (id),
    CONSTRAINT fk_announcement_approval_requests_decided_by FOREIGN KEY (decided_by) REFERENCES users (id),
    CONSTRAINT ck_announcement_approval_requests_status CHECK (approval_status_code IN ('REQUESTED', 'APPROVED', 'REJECTED', 'CANCELED'))
);

CREATE INDEX ix_announcement_approval_requests_status ON announcement_approval_requests (announcement_id, approval_status_code);
CREATE INDEX ix_announcement_approval_requests_requested_by ON announcement_approval_requests (requested_by, requested_at);

CREATE TABLE announcement_status_histories (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    announcement_id uuid NOT NULL,
    before_status_code varchar(30),
    after_status_code varchar(30) NOT NULL,
    reason text,
    changed_by uuid NOT NULL,
    changed_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_status_histories_announcement FOREIGN KEY (announcement_id) REFERENCES announcements (id),
    CONSTRAINT fk_announcement_status_histories_changed_by FOREIGN KEY (changed_by) REFERENCES users (id)
);

CREATE INDEX ix_announcement_status_histories_announcement_changed_at ON announcement_status_histories (announcement_id, changed_at);

CREATE TABLE announcement_industry_conditions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    announcement_id uuid NOT NULL,
    condition_type_code varchar(30) NOT NULL,
    ksic_code varchar(30) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_announcement_industry_conditions_announcement FOREIGN KEY (announcement_id) REFERENCES announcements (id),
    CONSTRAINT uq_announcement_industry_conditions_code UNIQUE (announcement_id, condition_type_code, ksic_code),
    CONSTRAINT ck_announcement_industry_conditions_type CHECK (condition_type_code IN ('INCLUDE', 'EXCLUDE'))
);

CREATE TABLE announcement_numeric_conditions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    announcement_id uuid NOT NULL,
    condition_scope_code varchar(30) NOT NULL,
    condition_key varchar(80) NOT NULL,
    comparator_code varchar(30) NOT NULL,
    value_number numeric(18, 2),
    min_number numeric(18, 2),
    max_number numeric(18, 2),
    unit_code varchar(30),
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_announcement_numeric_conditions_announcement FOREIGN KEY (announcement_id) REFERENCES announcements (id),
    CONSTRAINT uq_announcement_numeric_conditions_key UNIQUE (announcement_id, condition_scope_code, condition_key),
    CONSTRAINT ck_announcement_numeric_conditions_scope CHECK (condition_scope_code IN ('BUSINESS', 'PERSONAL', 'SPOUSE', 'CHILD', 'PARENT', 'APPLICATION', 'SUPPORT')),
    CONSTRAINT ck_announcement_numeric_conditions_comparator CHECK (comparator_code IN ('GTE', 'LTE', 'GT', 'LT', 'EQ', 'BETWEEN')),
    CONSTRAINT ck_announcement_numeric_conditions_between CHECK (
        (comparator_code = 'BETWEEN' AND min_number IS NOT NULL AND max_number IS NOT NULL AND min_number <= max_number)
        OR (comparator_code <> 'BETWEEN' AND value_number IS NOT NULL)
    )
);

CREATE INDEX ix_announcement_numeric_conditions_scope_key ON announcement_numeric_conditions (condition_scope_code, condition_key);

CREATE TABLE announcement_option_conditions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    announcement_id uuid NOT NULL,
    condition_scope_code varchar(30) NOT NULL,
    condition_key varchar(80) NOT NULL,
    option_code varchar(80) NOT NULL,
    option_text varchar(500),
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_announcement_option_conditions_announcement FOREIGN KEY (announcement_id) REFERENCES announcements (id),
    CONSTRAINT uq_announcement_option_conditions_code UNIQUE (announcement_id, condition_scope_code, condition_key, option_code),
    CONSTRAINT ck_announcement_option_conditions_scope CHECK (condition_scope_code IN ('BUSINESS', 'PERSONAL', 'SPOUSE', 'CHILD', 'PARENT', 'APPLICATION', 'SUPPORT'))
);

CREATE INDEX ix_announcement_option_conditions_scope_key ON announcement_option_conditions (condition_scope_code, condition_key);

CREATE TABLE announcement_document_requirements (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    announcement_id uuid NOT NULL,
    document_type_code varchar(80) NOT NULL,
    is_required boolean NOT NULL DEFAULT true,
    sort_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_announcement_document_requirements_announcement FOREIGN KEY (announcement_id) REFERENCES announcements (id),
    CONSTRAINT uq_announcement_document_requirements_type UNIQUE (announcement_id, document_type_code)
);

CREATE TABLE matching_cases (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    announcement_id uuid NOT NULL,
    member_user_id uuid NOT NULL,
    verification_id uuid NOT NULL,
    status_code varchar(30) NOT NULL,
    blocked_reason_code varchar(100),
    matched_at timestamptz,
    reviewed_by uuid,
    reviewed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_matching_cases_announcement FOREIGN KEY (announcement_id) REFERENCES announcements (id),
    CONSTRAINT fk_matching_cases_member_user FOREIGN KEY (member_user_id) REFERENCES users (id),
    CONSTRAINT fk_matching_cases_verification FOREIGN KEY (verification_id) REFERENCES partner_verifications (id),
    CONSTRAINT fk_matching_cases_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (id),
    CONSTRAINT uq_matching_cases_business_key UNIQUE (announcement_id, member_user_id, verification_id),
    CONSTRAINT ck_matching_cases_status CHECK (status_code IN ('MATCHED', 'NOT_MATCHED', 'REVIEW_REQUIRED', 'BLOCKED', 'PROGRESSED'))
);

CREATE INDEX ix_matching_cases_member_status ON matching_cases (member_user_id, status_code);
CREATE INDEX ix_matching_cases_announcement_status ON matching_cases (announcement_id, status_code);

CREATE TABLE matching_result_details (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    matching_case_id uuid NOT NULL,
    condition_scope_code varchar(30) NOT NULL,
    condition_key varchar(80) NOT NULL,
    result_code varchar(30) NOT NULL,
    basis_value varchar(500),
    required_value varchar(500),
    reason text,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    CONSTRAINT fk_matching_result_details_matching_case FOREIGN KEY (matching_case_id) REFERENCES matching_cases (id),
    CONSTRAINT uq_matching_result_details_key UNIQUE (matching_case_id, condition_scope_code, condition_key),
    CONSTRAINT ck_matching_result_details_result CHECK (result_code IN ('PASS', 'FAIL', 'SKIPPED', 'REVIEW_REQUIRED'))
);

CREATE INDEX ix_matching_result_details_result_code ON matching_result_details (result_code);

CREATE TABLE announcement_progress_steps (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    announcement_id uuid NOT NULL,
    step_order integer NOT NULL,
    step_name varchar(100) NOT NULL,
    guide_message text,
    action_guide text,
    completion_condition_code varchar(80) NOT NULL,
    next_condition_code varchar(80),
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_announcement_progress_steps_announcement FOREIGN KEY (announcement_id) REFERENCES announcements (id),
    CONSTRAINT uq_announcement_progress_steps_order UNIQUE (announcement_id, step_order),
    CONSTRAINT ck_announcement_progress_steps_order CHECK (step_order > 0)
);

CREATE INDEX ix_announcement_progress_steps_active ON announcement_progress_steps (announcement_id, is_active);

CREATE TABLE announcement_step_documents (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    step_id uuid NOT NULL,
    document_type_code varchar(80) NOT NULL,
    is_required boolean NOT NULL DEFAULT true,
    sort_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_announcement_step_documents_step FOREIGN KEY (step_id) REFERENCES announcement_progress_steps (id),
    CONSTRAINT uq_announcement_step_documents_type UNIQUE (step_id, document_type_code)
);

CREATE TABLE announcement_step_buttons (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    step_id uuid NOT NULL,
    button_code varchar(80) NOT NULL,
    button_label varchar(100) NOT NULL,
    button_action_code varchar(80) NOT NULL,
    next_step_id uuid,
    sort_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_announcement_step_buttons_step FOREIGN KEY (step_id) REFERENCES announcement_progress_steps (id),
    CONSTRAINT fk_announcement_step_buttons_next_step FOREIGN KEY (next_step_id) REFERENCES announcement_progress_steps (id),
    CONSTRAINT uq_announcement_step_buttons_code UNIQUE (step_id, button_code)
);

CREATE TABLE application_progresses (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    matching_case_id uuid NOT NULL,
    announcement_id uuid NOT NULL,
    member_user_id uuid NOT NULL,
    current_step_id uuid,
    status_code varchar(30) NOT NULL DEFAULT 'READY',
    receipt_no varchar(100),
    receipt_date date,
    result_code varchar(50),
    result_note text,
    result_date date,
    received_amount numeric(18, 2),
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_application_progresses_matching_case FOREIGN KEY (matching_case_id) REFERENCES matching_cases (id),
    CONSTRAINT fk_application_progresses_announcement FOREIGN KEY (announcement_id) REFERENCES announcements (id),
    CONSTRAINT fk_application_progresses_member_user FOREIGN KEY (member_user_id) REFERENCES users (id),
    CONSTRAINT fk_application_progresses_current_step FOREIGN KEY (current_step_id) REFERENCES announcement_progress_steps (id),
    CONSTRAINT uq_application_progresses_matching_case UNIQUE (matching_case_id),
    CONSTRAINT ck_application_progresses_status CHECK (
        status_code IN ('READY', 'IN_PROGRESS', 'WAITING_RESULT', 'APPROVED', 'REJECTED', 'SUPPLEMENT_REQUESTED', 'STOPPED', 'COMPLETED')
    ),
    CONSTRAINT ck_application_progresses_result CHECK (
        result_code IS NULL OR result_code IN ('APPROVED', 'REJECTED', 'SUPPLEMENT_REQUESTED', 'STOPPED')
    )
);

CREATE INDEX ix_application_progresses_member_status ON application_progresses (member_user_id, status_code);
CREATE INDEX ix_application_progresses_announcement_status ON application_progresses (announcement_id, status_code);

CREATE TABLE application_step_states (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    progress_id uuid NOT NULL,
    step_id uuid NOT NULL,
    status_code varchar(30) NOT NULL DEFAULT 'LOCKED',
    started_at timestamptz,
    completed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_application_step_states_progress FOREIGN KEY (progress_id) REFERENCES application_progresses (id),
    CONSTRAINT fk_application_step_states_step FOREIGN KEY (step_id) REFERENCES announcement_progress_steps (id),
    CONSTRAINT uq_application_step_states_progress_step UNIQUE (progress_id, step_id),
    CONSTRAINT ck_application_step_states_status CHECK (status_code IN ('LOCKED', 'READY', 'IN_PROGRESS', 'COMPLETED', 'SKIPPED', 'BLOCKED'))
);

CREATE TABLE application_action_logs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    progress_id uuid NOT NULL,
    step_id uuid NOT NULL,
    actor_user_id uuid NOT NULL,
    action_code varchar(80) NOT NULL,
    button_code varchar(80),
    input_json jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_application_action_logs_progress FOREIGN KEY (progress_id) REFERENCES application_progresses (id),
    CONSTRAINT fk_application_action_logs_step FOREIGN KEY (step_id) REFERENCES announcement_progress_steps (id),
    CONSTRAINT fk_application_action_logs_actor_user FOREIGN KEY (actor_user_id) REFERENCES users (id)
);

CREATE INDEX ix_application_action_logs_progress_created_at ON application_action_logs (progress_id, created_at);
CREATE INDEX ix_application_action_logs_actor_created_at ON application_action_logs (actor_user_id, created_at);

CREATE TABLE application_step_checklists (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    progress_id uuid NOT NULL,
    step_document_id uuid NOT NULL,
    is_checked boolean NOT NULL DEFAULT false,
    checked_at timestamptz,
    checked_by uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_application_step_checklists_progress FOREIGN KEY (progress_id) REFERENCES application_progresses (id),
    CONSTRAINT fk_application_step_checklists_step_document FOREIGN KEY (step_document_id) REFERENCES announcement_step_documents (id),
    CONSTRAINT fk_application_step_checklists_checked_by FOREIGN KEY (checked_by) REFERENCES users (id),
    CONSTRAINT uq_application_step_checklists_progress_document UNIQUE (progress_id, step_document_id)
);

CREATE TABLE progress_reminder_logs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    progress_id uuid NOT NULL,
    step_id uuid NOT NULL,
    reminder_type_code varchar(80) NOT NULL,
    attempt_no integer NOT NULL,
    scheduled_at timestamptz NOT NULL,
    sent_at timestamptz,
    result_code varchar(30),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_progress_reminder_logs_progress FOREIGN KEY (progress_id) REFERENCES application_progresses (id),
    CONSTRAINT fk_progress_reminder_logs_step FOREIGN KEY (step_id) REFERENCES announcement_progress_steps (id),
    CONSTRAINT ck_progress_reminder_logs_attempt CHECK (attempt_no > 0),
    CONSTRAINT ck_progress_reminder_logs_result CHECK (result_code IS NULL OR result_code IN ('SUCCESS', 'FAIL', 'SKIPPED'))
);

CREATE INDEX ix_progress_reminder_logs_progress_scheduled_at ON progress_reminder_logs (progress_id, scheduled_at);
CREATE INDEX ix_progress_reminder_logs_result_code ON progress_reminder_logs (result_code);

CREATE TABLE audit_logs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id uuid,
    action_code varchar(100) NOT NULL,
    resource_type varchar(100) NOT NULL,
    resource_id uuid,
    result_code varchar(30) NOT NULL,
    ip_address inet,
    user_agent varchar(500),
    metadata_json jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_audit_logs_actor_user FOREIGN KEY (actor_user_id) REFERENCES users (id),
    CONSTRAINT ck_audit_logs_result CHECK (result_code IN ('SUCCESS', 'FAIL'))
);

CREATE INDEX ix_audit_logs_actor_created_at ON audit_logs (actor_user_id, created_at);
CREATE INDEX ix_audit_logs_resource ON audit_logs (resource_type, resource_id);
CREATE INDEX ix_audit_logs_created_at ON audit_logs (created_at);

INSERT INTO roles (role_code, role_name, sort_order)
VALUES
    ('USER', '사용자', 10),
    ('PARTNER', '파트너', 20),
    ('OPERATOR', '운영자', 30),
    ('APPROVER', '승인자', 40),
    ('ADMIN', '관리자', 50);
