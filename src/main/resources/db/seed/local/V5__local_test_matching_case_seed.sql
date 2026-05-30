-- local profile only: one test matching case with an active application progress.
INSERT INTO users (
    id,
    login_id,
    password_hash,
    name,
    status_code,
    password_reset_required,
    created_at,
    created_by,
    updated_at,
    updated_by
) VALUES (
    '10000000-0000-0000-0000-000000000003',
    'local_match_user',
    '$2a$10$InQi9a3ehghCfxu2Z59DiegEEW4pfhxb4h19PCJb58D0/1OWmmQ2y',
    '로컬 매칭 테스트 사용자',
    'ACTIVE',
    false,
    now(),
    '10000000-0000-0000-0000-000000000002',
    now(),
    '10000000-0000-0000-0000-000000000002'
)
ON CONFLICT (login_id) DO UPDATE
SET
    password_hash = EXCLUDED.password_hash,
    name = EXCLUDED.name,
    status_code = EXCLUDED.status_code,
    password_reset_required = EXCLUDED.password_reset_required,
    updated_at = now(),
    updated_by = EXCLUDED.updated_by;

INSERT INTO user_roles (
    user_id,
    role_code,
    created_at,
    created_by
) VALUES (
    '10000000-0000-0000-0000-000000000003',
    'USER',
    now(),
    '10000000-0000-0000-0000-000000000002'
)
ON CONFLICT (user_id, role_code) DO NOTHING;

INSERT INTO member_profiles (
    id,
    user_id,
    birth_year,
    address,
    region_code,
    is_householder,
    is_household_member,
    health_insurance_basis_code,
    has_income,
    created_by,
    updated_by
) VALUES (
    '20000000-0000-0000-0000-000000000003',
    '10000000-0000-0000-0000-000000000003',
    1988,
    '서울특별시 강남구 테스트로 10',
    'SEOUL',
    true,
    false,
    'WORKPLACE',
    true,
    '10000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000002'
)
ON CONFLICT (user_id) DO UPDATE
SET
    birth_year = EXCLUDED.birth_year,
    address = EXCLUDED.address,
    region_code = EXCLUDED.region_code,
    is_householder = EXCLUDED.is_householder,
    is_household_member = EXCLUDED.is_household_member,
    health_insurance_basis_code = EXCLUDED.health_insurance_basis_code,
    has_income = EXCLUDED.has_income,
    updated_at = now(),
    updated_by = EXCLUDED.updated_by;

INSERT INTO business_profiles (
    id,
    user_id,
    representative_name,
    business_registration_no,
    business_name,
    workplace_address,
    workplace_region_code,
    opening_date,
    industry_name,
    business_category,
    business_item,
    ksic_code,
    business_type_code,
    company_stage_code,
    created_by,
    updated_by
) VALUES (
    '21000000-0000-0000-0000-000000000003',
    '10000000-0000-0000-0000-000000000003',
    '로컬대표',
    '999-99-00003',
    '로컬 매칭 테스트 상점',
    '서울특별시 강남구 테스트로 10',
    'SEOUL',
    DATE '2023-01-10',
    '음식점업',
    '소상공인',
    '테스트 상품',
    'I56111',
    'SOLE_PROPRIETOR',
    'OPERATING',
    '10000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000002'
)
ON CONFLICT (business_registration_no) DO UPDATE
SET
    user_id = EXCLUDED.user_id,
    representative_name = EXCLUDED.representative_name,
    business_name = EXCLUDED.business_name,
    workplace_address = EXCLUDED.workplace_address,
    workplace_region_code = EXCLUDED.workplace_region_code,
    opening_date = EXCLUDED.opening_date,
    industry_name = EXCLUDED.industry_name,
    business_category = EXCLUDED.business_category,
    business_item = EXCLUDED.business_item,
    ksic_code = EXCLUDED.ksic_code,
    business_type_code = EXCLUDED.business_type_code,
    company_stage_code = EXCLUDED.company_stage_code,
    updated_at = now(),
    updated_by = EXCLUDED.updated_by;

INSERT INTO partner_verifications (
    id,
    member_user_id,
    partner_user_id,
    business_profile_id,
    status_code,
    is_current,
    is_matching_blocked,
    submitted_at,
    verified_at,
    reviewed_by,
    review_note,
    created_by,
    updated_by
) VALUES (
    '30000000-0000-0000-0000-000000000003',
    '10000000-0000-0000-0000-000000000003',
    '10000000-0000-0000-0000-000000000002',
    '21000000-0000-0000-0000-000000000003',
    'VERIFIED',
    true,
    false,
    now(),
    now(),
    '10000000-0000-0000-0000-000000000002',
    'local test matching verification',
    '10000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000002'
)
ON CONFLICT (id) DO UPDATE
SET
    member_user_id = EXCLUDED.member_user_id,
    partner_user_id = EXCLUDED.partner_user_id,
    business_profile_id = EXCLUDED.business_profile_id,
    status_code = EXCLUDED.status_code,
    is_current = EXCLUDED.is_current,
    is_matching_blocked = EXCLUDED.is_matching_blocked,
    submitted_at = EXCLUDED.submitted_at,
    verified_at = EXCLUDED.verified_at,
    reviewed_by = EXCLUDED.reviewed_by,
    review_note = EXCLUDED.review_note,
    updated_at = now(),
    updated_by = EXCLUDED.updated_by;

INSERT INTO verification_member_values (
    verification_id,
    birth_year,
    address,
    region_code,
    is_householder,
    is_household_member,
    health_insurance_basis_code,
    has_income,
    created_by,
    updated_by
) VALUES (
    '30000000-0000-0000-0000-000000000003',
    1988,
    '서울특별시 강남구 테스트로 10',
    'SEOUL',
    true,
    false,
    'WORKPLACE',
    true,
    '10000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000002'
)
ON CONFLICT (verification_id) DO UPDATE
SET
    birth_year = EXCLUDED.birth_year,
    address = EXCLUDED.address,
    region_code = EXCLUDED.region_code,
    is_householder = EXCLUDED.is_householder,
    is_household_member = EXCLUDED.is_household_member,
    health_insurance_basis_code = EXCLUDED.health_insurance_basis_code,
    has_income = EXCLUDED.has_income,
    updated_at = now(),
    updated_by = EXCLUDED.updated_by;

INSERT INTO verification_business_values (
    verification_id,
    annual_revenue,
    employee_count,
    regular_employee_count,
    tax_status_code,
    nice_credit_score,
    kcb_credit_score,
    has_existing_loan,
    has_policy_fund_usage,
    has_guarantee_usage,
    financial_checked_on,
    created_by,
    updated_by
) VALUES (
    '30000000-0000-0000-0000-000000000003',
    120000000,
    3,
    2,
    'NORMAL',
    720,
    710,
    false,
    false,
    false,
    DATE '2026-05-30',
    '10000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000002'
)
ON CONFLICT (verification_id) DO UPDATE
SET
    annual_revenue = EXCLUDED.annual_revenue,
    employee_count = EXCLUDED.employee_count,
    regular_employee_count = EXCLUDED.regular_employee_count,
    tax_status_code = EXCLUDED.tax_status_code,
    nice_credit_score = EXCLUDED.nice_credit_score,
    kcb_credit_score = EXCLUDED.kcb_credit_score,
    has_existing_loan = EXCLUDED.has_existing_loan,
    has_policy_fund_usage = EXCLUDED.has_policy_fund_usage,
    has_guarantee_usage = EXCLUDED.has_guarantee_usage,
    financial_checked_on = EXCLUDED.financial_checked_on,
    updated_at = now(),
    updated_by = EXCLUDED.updated_by;

INSERT INTO verification_documents (
    verification_id,
    document_type_code,
    source_type_code,
    is_checked,
    checked_by,
    checked_at,
    note,
    created_by,
    updated_by
) VALUES (
    '30000000-0000-0000-0000-000000000003',
    'BUSINESS_REGISTRATION',
    'PARTNER_CHECK',
    true,
    '10000000-0000-0000-0000-000000000002',
    now(),
    'local test document check',
    '10000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000002'
)
ON CONFLICT (verification_id, document_type_code) DO UPDATE
SET
    source_type_code = EXCLUDED.source_type_code,
    is_checked = EXCLUDED.is_checked,
    checked_by = EXCLUDED.checked_by,
    checked_at = EXCLUDED.checked_at,
    note = EXCLUDED.note,
    updated_at = now(),
    updated_by = EXCLUDED.updated_by;

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
    '40000000-0000-0000-0000-000000000003',
    'BUSINESS',
    '로컬 테스트 매칭 공고',
    'saneB Local Agency',
    '로컬 테스트용 매칭 케이스 공고입니다.',
    DATE '2026-05-01',
    DATE '2026-12-31',
    'NORMAL',
    'APPROVED',
    'NO_LIMIT',
    2500000,
    6000000,
    '10000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000002'
)
ON CONFLICT (agency_name, title, application_start_date) DO UPDATE
SET
    target_type_code = EXCLUDED.target_type_code,
    summary = EXCLUDED.summary,
    application_end_date = EXCLUDED.application_end_date,
    manual_status_code = EXCLUDED.manual_status_code,
    approval_status_code = EXCLUDED.approval_status_code,
    income_judgement_code = EXCLUDED.income_judgement_code,
    min_amount = EXCLUDED.min_amount,
    max_amount = EXCLUDED.max_amount,
    updated_at = now(),
    updated_by = EXCLUDED.updated_by;

INSERT INTO announcement_options (
    announcement_id,
    option_group_code,
    option_code,
    created_by
) VALUES (
    '40000000-0000-0000-0000-000000000003',
    'PAYMENT_METHOD',
    'LOAN',
    '10000000-0000-0000-0000-000000000002'
)
ON CONFLICT (announcement_id, option_group_code, option_code) DO NOTHING;

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
) VALUES (
    '50000000-0000-0000-0000-000000000003',
    '40000000-0000-0000-0000-000000000003',
    1,
    '진행 의사 확인',
    '테스트 공고 신청 진행을 시작합니다.',
    '진행 의사를 확인하면 결과 대기 단계로 이동합니다.',
    'BUTTON_CLICK',
    'WAITING_RESULT',
    true,
    '10000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000002'
)
ON CONFLICT (announcement_id, step_order) DO UPDATE
SET
    step_name = EXCLUDED.step_name,
    guide_message = EXCLUDED.guide_message,
    action_guide = EXCLUDED.action_guide,
    completion_condition_code = EXCLUDED.completion_condition_code,
    next_condition_code = EXCLUDED.next_condition_code,
    is_active = EXCLUDED.is_active,
    updated_at = now(),
    updated_by = EXCLUDED.updated_by;

INSERT INTO announcement_step_buttons (
    step_id,
    button_code,
    button_label,
    button_action_code,
    sort_order,
    created_by,
    updated_by
) VALUES (
    '50000000-0000-0000-0000-000000000003',
    'WANTS_TO_PROGRESS',
    '진행 원함',
    'MOVE_NEXT',
    1,
    '10000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000002'
)
ON CONFLICT (step_id, button_code) DO UPDATE
SET
    button_label = EXCLUDED.button_label,
    button_action_code = EXCLUDED.button_action_code,
    sort_order = EXCLUDED.sort_order,
    updated_at = now(),
    updated_by = EXCLUDED.updated_by;

INSERT INTO matching_cases (
    id,
    announcement_id,
    member_user_id,
    verification_id,
    status_code,
    matched_at,
    reviewed_by,
    reviewed_at,
    created_by,
    updated_by
) VALUES (
    '60000000-0000-0000-0000-000000000003',
    '40000000-0000-0000-0000-000000000003',
    '10000000-0000-0000-0000-000000000003',
    '30000000-0000-0000-0000-000000000003',
    'PROGRESSED',
    now(),
    '10000000-0000-0000-0000-000000000002',
    now(),
    '10000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000002'
)
ON CONFLICT (announcement_id, member_user_id, verification_id) DO UPDATE
SET
    status_code = EXCLUDED.status_code,
    matched_at = EXCLUDED.matched_at,
    reviewed_by = EXCLUDED.reviewed_by,
    reviewed_at = EXCLUDED.reviewed_at,
    updated_at = now(),
    updated_by = EXCLUDED.updated_by;

INSERT INTO matching_result_details (
    matching_case_id,
    condition_scope_code,
    condition_key,
    result_code,
    basis_value,
    required_value,
    reason,
    created_by
) VALUES (
    '60000000-0000-0000-0000-000000000003',
    'BUSINESS',
    'LOCAL_TEST_ELIGIBILITY',
    'PASS',
    'local test fixture',
    'local test fixture',
    'Local test matching case passes the MVP eligibility fixture.',
    '10000000-0000-0000-0000-000000000002'
)
ON CONFLICT (matching_case_id, condition_scope_code, condition_key) DO UPDATE
SET
    result_code = EXCLUDED.result_code,
    basis_value = EXCLUDED.basis_value,
    required_value = EXCLUDED.required_value,
    reason = EXCLUDED.reason;

INSERT INTO application_progresses (
    id,
    matching_case_id,
    announcement_id,
    member_user_id,
    current_step_id,
    status_code,
    created_by,
    updated_by
) VALUES (
    '70000000-0000-0000-0000-000000000003',
    '60000000-0000-0000-0000-000000000003',
    '40000000-0000-0000-0000-000000000003',
    '10000000-0000-0000-0000-000000000003',
    '50000000-0000-0000-0000-000000000003',
    'READY',
    '10000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000002'
)
ON CONFLICT (matching_case_id) DO UPDATE
SET
    current_step_id = EXCLUDED.current_step_id,
    status_code = EXCLUDED.status_code,
    receipt_no = NULL,
    receipt_date = NULL,
    result_code = NULL,
    result_note = NULL,
    result_date = NULL,
    received_amount = NULL,
    updated_at = now(),
    updated_by = EXCLUDED.updated_by;

INSERT INTO application_step_states (
    id,
    progress_id,
    step_id,
    status_code,
    started_at,
    created_by,
    updated_by
) VALUES (
    '71000000-0000-0000-0000-000000000003',
    '70000000-0000-0000-0000-000000000003',
    '50000000-0000-0000-0000-000000000003',
    'READY',
    NULL,
    '10000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000002'
)
ON CONFLICT (progress_id, step_id) DO UPDATE
SET
    status_code = EXCLUDED.status_code,
    started_at = EXCLUDED.started_at,
    completed_at = NULL,
    updated_at = now(),
    updated_by = EXCLUDED.updated_by;
