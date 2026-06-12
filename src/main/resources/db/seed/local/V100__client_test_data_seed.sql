-- local profile only: reviewer account, monthly plan, and condition-type sample announcements.
INSERT INTO users (
    id,
    login_id,
    password_hash,
    name,
    status_code,
    password_reset_required,
    created_at,
    updated_at
) VALUES (
    '10000000-0000-0000-0000-000000000010',
    'local_reviewer',
    '$2a$10$InQi9a3ehghCfxu2Z59DiegEEW4pfhxb4h19PCJb58D0/1OWmmQ2y',
    '로컬 검수자',
    'ACTIVE',
    false,
    now(),
    now()
)
ON CONFLICT (login_id) DO UPDATE
SET
    password_hash = EXCLUDED.password_hash,
    name = EXCLUDED.name,
    status_code = EXCLUDED.status_code,
    password_reset_required = EXCLUDED.password_reset_required,
    updated_at = now();

INSERT INTO user_roles (
    user_id,
    role_code,
    created_at
) VALUES (
    '10000000-0000-0000-0000-000000000010',
    'REVIEWER',
    now()
)
ON CONFLICT (user_id, role_code) DO NOTHING;

INSERT INTO subscription_plans (
    id,
    plan_code,
    plan_name,
    billing_cycle_code,
    price_amount,
    currency_code,
    is_active,
    sort_order,
    description
) VALUES (
    '90000000-0000-0000-0000-000000000001',
    'MONTHLY_BASIC',
    '월 단순 구독',
    'MONTHLY',
    99000,
    'KRW',
    true,
    10,
    'TossPayments 연동 준비용 월 단순 구독 상품입니다. 실제 결제 승인과 billing key 저장은 운영 계약 확정 후 연결합니다.'
)
ON CONFLICT (plan_code) DO UPDATE
SET
    plan_name = EXCLUDED.plan_name,
    billing_cycle_code = EXCLUDED.billing_cycle_code,
    price_amount = EXCLUDED.price_amount,
    currency_code = EXCLUDED.currency_code,
    is_active = EXCLUDED.is_active,
    sort_order = EXCLUDED.sort_order,
    description = EXCLUDED.description,
    updated_at = now();

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
    max_amount
) VALUES
    (
        '40000000-0000-0000-0000-000000000010',
        'BUSINESS',
        '샘플 공고 - 업력·매출 조건형',
        'saneB 운영 테스트',
        '업력과 연매출 조건을 함께 확인하는 운영 테스트 공고입니다.',
        DATE '2026-01-01',
        DATE '2026-12-31',
        'NORMAL',
        'APPROVED',
        'VAT_TAX_BASE_ONLY',
        3000000,
        10000000
    ),
    (
        '40000000-0000-0000-0000-000000000011',
        'BUSINESS',
        '샘플 공고 - 지역 제한형',
        'saneB 운영 테스트',
        '사업장 지역 제한 조건을 확인하는 운영 테스트 공고입니다.',
        DATE '2026-01-01',
        DATE '2026-12-31',
        'NORMAL',
        'APPROVED',
        'NO_LIMIT',
        1000000,
        5000000
    ),
    (
        '40000000-0000-0000-0000-000000000012',
        'CHILD',
        '샘플 공고 - 가족 조건형',
        'saneB 운영 테스트',
        '자녀 또는 가족 구성 조건을 확인하는 운영 테스트 공고입니다.',
        DATE '2026-01-01',
        DATE '2026-12-31',
        'NORMAL',
        'APPROVED',
        'INCOME_OR_HEALTH_INSURANCE',
        500000,
        3000000
    ),
    (
        '40000000-0000-0000-0000-000000000013',
        'BUSINESS',
        '샘플 공고 - 중복 제한형',
        'saneB 운영 테스트',
        '정책자금·보증 이용 여부와 중복 제한 확인을 위한 운영 테스트 공고입니다.',
        DATE '2026-01-01',
        DATE '2026-12-31',
        'NORMAL',
        'APPROVED',
        'ANY_ONE_DOCUMENT',
        2000000,
        7000000
    ),
    (
        '40000000-0000-0000-0000-000000000014',
        'BUSINESS',
        '샘플 공고 - 예산 소진형',
        'saneB 운영 테스트',
        '예산 소진 상태의 노출과 신청 차단을 확인하는 운영 테스트 공고입니다.',
        DATE '2026-01-01',
        DATE '2026-12-31',
        'BUDGET_EXHAUSTED',
        'APPROVED',
        'NO_LIMIT',
        1000000,
        4000000
    )
ON CONFLICT (id) DO UPDATE
SET
    target_type_code = EXCLUDED.target_type_code,
    title = EXCLUDED.title,
    agency_name = EXCLUDED.agency_name,
    summary = EXCLUDED.summary,
    application_start_date = EXCLUDED.application_start_date,
    application_end_date = EXCLUDED.application_end_date,
    manual_status_code = EXCLUDED.manual_status_code,
    approval_status_code = EXCLUDED.approval_status_code,
    income_judgement_code = EXCLUDED.income_judgement_code,
    min_amount = EXCLUDED.min_amount,
    max_amount = EXCLUDED.max_amount,
    updated_at = now();

INSERT INTO announcement_numeric_conditions (
    announcement_id,
    condition_scope_code,
    condition_key,
    comparator_code,
    value_number,
    min_number,
    max_number,
    unit_code
) VALUES
    ('40000000-0000-0000-0000-000000000010', 'BUSINESS', 'BUSINESS_YEARS', 'GTE', 1, NULL, NULL, 'YEAR'),
    ('40000000-0000-0000-0000-000000000010', 'BUSINESS', 'ANNUAL_REVENUE', 'BETWEEN', NULL, 50000000, 300000000, 'KRW')
ON CONFLICT (announcement_id, condition_scope_code, condition_key) DO UPDATE
SET
    comparator_code = EXCLUDED.comparator_code,
    value_number = EXCLUDED.value_number,
    min_number = EXCLUDED.min_number,
    max_number = EXCLUDED.max_number,
    unit_code = EXCLUDED.unit_code,
    updated_at = now();

INSERT INTO announcement_option_conditions (
    announcement_id,
    condition_scope_code,
    condition_key,
    option_code,
    option_text
) VALUES
    ('40000000-0000-0000-0000-000000000011', 'BUSINESS', 'WORKPLACE_REGION_CODE', 'SEOUL', '서울'),
    ('40000000-0000-0000-0000-000000000011', 'BUSINESS', 'WORKPLACE_REGION_CODE', 'GYEONGGI', '경기'),
    ('40000000-0000-0000-0000-000000000012', 'CHILD', 'HAS_CHILD', 'TRUE', '자녀 있음'),
    ('40000000-0000-0000-0000-000000000013', 'BUSINESS', 'HAS_POLICY_FUND_USAGE', 'FALSE', '정책자금 이용 이력 없음'),
    ('40000000-0000-0000-0000-000000000013', 'BUSINESS', 'HAS_GUARANTEE_USAGE', 'FALSE', '보증 이용 이력 없음')
ON CONFLICT (announcement_id, condition_scope_code, condition_key, option_code) DO UPDATE
SET
    option_text = EXCLUDED.option_text,
    updated_at = now();

INSERT INTO announcement_document_requirements (
    announcement_id,
    document_type_code,
    is_required,
    sort_order
) VALUES
    ('40000000-0000-0000-0000-000000000010', 'BUSINESS_REGISTRATION', false, 1),
    ('40000000-0000-0000-0000-000000000010', 'VAT_TAX_BASE', false, 2),
    ('40000000-0000-0000-0000-000000000011', 'BUSINESS_REGISTRATION', false, 1),
    ('40000000-0000-0000-0000-000000000012', 'FAMILY_RELATION', false, 1),
    ('40000000-0000-0000-0000-000000000013', 'BUSINESS_REGISTRATION', false, 1),
    ('40000000-0000-0000-0000-000000000014', 'BUSINESS_REGISTRATION', false, 1)
ON CONFLICT (announcement_id, document_type_code) DO UPDATE
SET
    is_required = EXCLUDED.is_required,
    sort_order = EXCLUDED.sort_order,
    updated_at = now();

INSERT INTO announcement_input_requirements (
    id,
    announcement_id,
    field_key,
    field_label,
    field_type_code,
    scope_code,
    is_required,
    is_sensitive,
    sort_order,
    help_text
) VALUES
    ('42000000-0000-0000-0000-000000000010', '40000000-0000-0000-0000-000000000010', 'ANNUAL_REVENUE', '연매출', 'AMOUNT', 'BUSINESS', false, true, 10, '부가세 과세표준 또는 면세사업자 수입금액 기준으로 입력합니다.'),
    ('42000000-0000-0000-0000-000000000011', '40000000-0000-0000-0000-000000000010', 'BUSINESS_YEARS', '업력', 'NUMBER', 'BUSINESS', false, false, 20, '사업자등록증의 개업일 기준으로 계산한 연수를 입력합니다.'),
    ('42000000-0000-0000-0000-000000000012', '40000000-0000-0000-0000-000000000011', 'WORKPLACE_REGION_CODE', '사업장 지역', 'SELECT', 'BUSINESS', false, false, 10, '사업자등록증 또는 임대차계약서의 사업장 소재지를 선택합니다.'),
    ('42000000-0000-0000-0000-000000000013', '40000000-0000-0000-0000-000000000012', 'HAS_CHILD', '자녀 여부', 'RADIO', 'CHILD', false, false, 10, '가족관계증명서 기준으로 자녀 여부를 선택합니다.'),
    ('42000000-0000-0000-0000-000000000014', '40000000-0000-0000-0000-000000000013', 'HAS_POLICY_FUND_USAGE', '정책자금 이용 이력', 'RADIO', 'BUSINESS', false, false, 10, '최근 동일·유사 정책자금 이용 여부를 선택합니다.'),
    ('42000000-0000-0000-0000-000000000015', '40000000-0000-0000-0000-000000000013', 'HAS_GUARANTEE_USAGE', '보증 이용 이력', 'RADIO', 'BUSINESS', false, false, 20, '보증기관 보증 이용 여부를 선택합니다.')
ON CONFLICT (announcement_id, field_key) DO UPDATE
SET
    field_label = EXCLUDED.field_label,
    field_type_code = EXCLUDED.field_type_code,
    scope_code = EXCLUDED.scope_code,
    is_required = EXCLUDED.is_required,
    is_sensitive = EXCLUDED.is_sensitive,
    sort_order = EXCLUDED.sort_order,
    help_text = EXCLUDED.help_text,
    updated_at = now();

INSERT INTO announcement_input_options (
    requirement_id,
    option_code,
    option_label,
    sort_order
) VALUES
    ('42000000-0000-0000-0000-000000000012', 'SEOUL', '서울', 1),
    ('42000000-0000-0000-0000-000000000012', 'GYEONGGI', '경기', 2),
    ('42000000-0000-0000-0000-000000000013', 'TRUE', '예', 1),
    ('42000000-0000-0000-0000-000000000013', 'FALSE', '아니오', 2),
    ('42000000-0000-0000-0000-000000000014', 'TRUE', '있음', 1),
    ('42000000-0000-0000-0000-000000000014', 'FALSE', '없음', 2),
    ('42000000-0000-0000-0000-000000000015', 'TRUE', '있음', 1),
    ('42000000-0000-0000-0000-000000000015', 'FALSE', '없음', 2)
ON CONFLICT (requirement_id, option_code) DO UPDATE
SET
    option_label = EXCLUDED.option_label,
    sort_order = EXCLUDED.sort_order,
    updated_at = now();

INSERT INTO announcement_progress_steps (
    id,
    announcement_id,
    step_order,
    step_name,
    guide_message,
    action_guide,
    completion_condition_code,
    next_condition_code,
    is_active
) VALUES
    ('50000000-0000-0000-0000-000000000010', '40000000-0000-0000-0000-000000000010', 1, '조건 정보 확인', '업력과 매출 조건 정보를 확인합니다.', '필수 입력값과 서류를 확인한 뒤 다음 단계로 이동합니다.', 'REQUIRED_INPUT_AND_DOCUMENTS', 'MOVE_NEXT', true),
    ('50000000-0000-0000-0000-000000000011', '40000000-0000-0000-0000-000000000010', 2, '접수 확인', '접수번호와 접수일을 확인합니다.', '운영자가 접수 정보를 저장합니다.', 'RECEIPT_SAVED', 'WAITING_RESULT', true),
    ('50000000-0000-0000-0000-000000000012', '40000000-0000-0000-0000-000000000011', 1, '지역 확인', '사업장 지역 조건을 확인합니다.', '지역 정보와 사업자등록증을 확인합니다.', 'REQUIRED_INPUT_AND_DOCUMENTS', 'MOVE_NEXT', true),
    ('50000000-0000-0000-0000-000000000013', '40000000-0000-0000-0000-000000000012', 1, '가족 정보 확인', '가족관계 조건을 확인합니다.', '가족관계증명서와 입력값을 확인합니다.', 'REQUIRED_INPUT_AND_DOCUMENTS', 'MOVE_NEXT', true),
    ('50000000-0000-0000-0000-000000000014', '40000000-0000-0000-0000-000000000013', 1, '중복 제한 확인', '정책자금과 보증 이용 이력을 확인합니다.', '중복 제한 항목을 확인합니다.', 'REQUIRED_INPUT_AND_DOCUMENTS', 'MOVE_NEXT', true),
    ('50000000-0000-0000-0000-000000000015', '40000000-0000-0000-0000-000000000014', 1, '예산 상태 확인', '예산 소진 상태를 확인합니다.', '예산 소진 공고는 신청 진행이 차단되어야 합니다.', 'BUDGET_STATUS_CHECK', 'BLOCKED', true)
ON CONFLICT (announcement_id, step_order) DO UPDATE
SET
    step_name = EXCLUDED.step_name,
    guide_message = EXCLUDED.guide_message,
    action_guide = EXCLUDED.action_guide,
    completion_condition_code = EXCLUDED.completion_condition_code,
    next_condition_code = EXCLUDED.next_condition_code,
    is_active = EXCLUDED.is_active,
    updated_at = now();

INSERT INTO announcement_step_buttons (
    step_id,
    button_code,
    button_label,
    button_action_code,
    sort_order
) VALUES
    ('50000000-0000-0000-0000-000000000010', 'CONFIRM_CONDITION', '조건 확인 완료', 'MOVE_NEXT', 1),
    ('50000000-0000-0000-0000-000000000011', 'CONFIRM_RECEIPT', '접수 확인 완료', 'COMPLETE_STEP', 1),
    ('50000000-0000-0000-0000-000000000012', 'CONFIRM_REGION', '지역 확인 완료', 'COMPLETE_STEP', 1),
    ('50000000-0000-0000-0000-000000000013', 'CONFIRM_FAMILY', '가족 조건 확인 완료', 'COMPLETE_STEP', 1),
    ('50000000-0000-0000-0000-000000000014', 'CONFIRM_DUPLICATE_LIMIT', '중복 제한 확인 완료', 'COMPLETE_STEP', 1),
    ('50000000-0000-0000-0000-000000000015', 'CONFIRM_BUDGET_EXHAUSTED', '예산 소진 확인', 'COMPLETE_STEP', 1)
ON CONFLICT (step_id, button_code) DO UPDATE
SET
    button_label = EXCLUDED.button_label,
    button_action_code = EXCLUDED.button_action_code,
    sort_order = EXCLUDED.sort_order,
    updated_at = now();
