\encoding UTF8
SET client_encoding = 'UTF8';

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT
            1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'announcements'
    ) THEN
        RAISE EXCEPTION 'announcements table does not exist. Apply Flyway migrations first.';
    END IF;

    IF NOT EXISTS (
        SELECT
            1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'standard_document_fields'
    ) THEN
        RAISE EXCEPTION 'standard_document_fields table does not exist. Apply V15 migration first.';
    END IF;

    IF NOT EXISTS (
        SELECT
            1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'announcements'
          AND column_name = 'public_code'
    ) THEN
        RAISE EXCEPTION 'announcements.public_code does not exist. Apply V18 migration first.';
    END IF;
END $$;

CREATE TEMP TABLE saneb_seed_actor ON COMMIT DROP AS
SELECT
    u.id AS actor_user_id
FROM users u
WHERE u.status_code = 'ACTIVE'
  AND EXISTS (
      SELECT
          1
      FROM user_roles ur
      WHERE ur.user_id = u.id
        AND ur.role_code IN ('ADMIN', 'OPERATOR', 'APPROVER')
  )
ORDER BY
    CASE
        WHEN EXISTS (
            SELECT
                1
            FROM user_roles ur
            WHERE ur.user_id = u.id
              AND ur.role_code = 'ADMIN'
        ) THEN 0
        WHEN EXISTS (
            SELECT
                1
            FROM user_roles ur
            WHERE ur.user_id = u.id
              AND ur.role_code = 'OPERATOR'
        ) THEN 1
        ELSE 2
    END,
    u.created_at ASC,
    u.id ASC
LIMIT 1;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT
            1
        FROM saneb_seed_actor
    ) THEN
        RAISE EXCEPTION 'Active ADMIN/OPERATOR/APPROVER user is required before seeding announcements.';
    END IF;
END $$;

CREATE TEMP TABLE saneb_seed_announcements (
    seed_key varchar(40) PRIMARY KEY,
    target_type_code varchar(30) NOT NULL,
    title varchar(300) NOT NULL,
    agency_name varchar(200) NOT NULL,
    summary text,
    application_start_date date NOT NULL,
    application_end_date date,
    manual_status_code varchar(30) NOT NULL,
    approval_status_code varchar(30) NOT NULL,
    income_judgement_code varchar(50) NOT NULL,
    min_amount numeric(18, 2),
    max_amount numeric(18, 2),
    selection_method_code varchar(80),
    payment_method_code varchar(80)
) ON COMMIT DROP;

INSERT INTO saneb_seed_announcements (
    seed_key,
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
    selection_method_code,
    payment_method_code
) VALUES
    (
        'REV_YEARS_SEOUL',
        'BUSINESS',
        '[운영테스트] 서울 소상공인 성장 운전자금',
        '서울신용보증재단',
        '업력과 신고 매출액을 함께 확인하는 사업자 대상 운전자금 공고입니다.',
        DATE '2026-06-01',
        DATE '2026-06-20',
        'NORMAL',
        'APPROVED',
        'VAT_TAX_BASE_ONLY',
        10000000,
        50000000,
        'REVIEW',
        'LOAN'
    ),
    (
        'REGION_GYEONGGI',
        'BUSINESS',
        '[운영테스트] 경기 창업초기 시설개선 지원',
        '경기도경제과학진흥원',
        '경기 지역 창업초기 사업장의 시설개선 비용을 지원하는 공고입니다.',
        DATE '2026-06-03',
        DATE '2026-06-25',
        'NORMAL',
        'APPROVED',
        'ANY_ONE_DOCUMENT',
        2000000,
        8000000,
        'FIRST_COME',
        'VOUCHER'
    ),
    (
        'BUSAN_YOUNG',
        'BUSINESS',
        '[운영테스트] 부산 청년 창업 보증지원',
        '부산경제진흥원',
        '부산 소재 청년 대표자의 초기 사업 운영을 보증 방식으로 지원하는 공고입니다.',
        DATE '2026-06-05',
        DATE '2026-07-05',
        'NORMAL',
        'APPROVED',
        'NO_LIMIT',
        30000000,
        100000000,
        'REVIEW',
        'GUARANTEE'
    ),
    (
        'CHILD_FAMILY',
        'CHILD',
        '[운영테스트] 다자녀 가구 생활안정 지원금',
        '한국가족지원재단',
        '자녀 수와 주소지 기준을 확인하는 가족 대상 현금성 지원 공고입니다.',
        DATE '2026-06-07',
        DATE '2026-07-12',
        'NORMAL',
        'APPROVED',
        'INCOME_OR_HEALTH_INSURANCE',
        500000,
        3000000,
        'ELIGIBILITY',
        'CASH'
    ),
    (
        'PARENT_CARE',
        'PARENT',
        '[운영테스트] 부모 부양 가구 지역지원금',
        '서울복지재단',
        '부모 부양 여부와 개인 소득 구간을 확인하는 가족 대상 지원 공고입니다.',
        DATE '2026-06-09',
        DATE '2026-07-20',
        'NORMAL',
        'APPROVED',
        'INCOME_CERT_ONLY',
        1000000,
        5000000,
        'REVIEW',
        'CASH'
    ),
    (
        'DUP_LIMIT',
        'BUSINESS',
        '[운영테스트] 중복지원 제한 소상공인 바우처',
        '중소벤처기업진흥공단',
        '정책자금과 보증 이용 이력이 없는 사업자를 우선 확인하는 바우처형 공고입니다.',
        DATE '2026-06-10',
        DATE '2026-07-31',
        'NORMAL',
        'APPROVED',
        'ANY_ONE_DOCUMENT',
        1000000,
        10000000,
        'ELIGIBILITY',
        'VOUCHER'
    ),
    (
        'BUDGET_EXHAUSTED',
        'BUSINESS',
        '[운영테스트] 예산 소진 공고 확인용',
        '인천테크노파크',
        '예산 소진 상태가 화면과 매칭 대상 제외 흐름에 반영되는지 확인하기 위한 공고입니다.',
        DATE '2026-06-01',
        DATE '2026-08-15',
        'BUDGET_EXHAUSTED',
        'APPROVED',
        'NO_LIMIT',
        1000000,
        7000000,
        'BUDGET',
        'REFUND'
    );

INSERT INTO announcements (
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
)
SELECT
    seed.target_type_code,
    seed.title,
    seed.agency_name,
    seed.summary,
    seed.application_start_date,
    seed.application_end_date,
    seed.manual_status_code,
    seed.approval_status_code,
    seed.income_judgement_code,
    seed.min_amount,
    seed.max_amount,
    actor.actor_user_id,
    actor.actor_user_id
FROM saneb_seed_announcements seed
CROSS JOIN saneb_seed_actor actor
ON CONFLICT (agency_name, title, application_start_date) DO UPDATE SET
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

CREATE TEMP TABLE saneb_seed_announcement_ids ON COMMIT DROP AS
SELECT
    seed.seed_key,
    a.id AS announcement_id
FROM saneb_seed_announcements seed
INNER JOIN announcements a ON a.agency_name = seed.agency_name
    AND a.title = seed.title
    AND a.application_start_date = seed.application_start_date;

INSERT INTO announcement_status_histories (
    announcement_id,
    before_status_code,
    after_status_code,
    reason,
    changed_by
)
SELECT
    ids.announcement_id,
    NULL,
    seed.approval_status_code,
    '배포 운영 테스트 공고 데이터 입력',
    actor.actor_user_id
FROM saneb_seed_announcements seed
INNER JOIN saneb_seed_announcement_ids ids ON ids.seed_key = seed.seed_key
CROSS JOIN saneb_seed_actor actor
WHERE NOT EXISTS (
    SELECT
        1
    FROM announcement_status_histories history
    WHERE history.announcement_id = ids.announcement_id
      AND history.after_status_code = seed.approval_status_code
      AND history.reason = '배포 운영 테스트 공고 데이터 입력'
);

CREATE TEMP TABLE saneb_seed_options (
    seed_key varchar(40) NOT NULL,
    option_group_code varchar(80) NOT NULL,
    option_code varchar(80) NOT NULL
) ON COMMIT DROP;

INSERT INTO saneb_seed_options (
    seed_key,
    option_group_code,
    option_code
)
SELECT
    seed_key,
    'SELECTION_METHOD',
    selection_method_code
FROM saneb_seed_announcements
WHERE selection_method_code IS NOT NULL
UNION ALL
SELECT
    seed_key,
    'PAYMENT_METHOD',
    payment_method_code
FROM saneb_seed_announcements
WHERE payment_method_code IS NOT NULL;

INSERT INTO announcement_options (
    announcement_id,
    option_group_code,
    option_code,
    created_by
)
SELECT
    ids.announcement_id,
    seed.option_group_code,
    seed.option_code,
    actor.actor_user_id
FROM saneb_seed_options seed
INNER JOIN saneb_seed_announcement_ids ids ON ids.seed_key = seed.seed_key
CROSS JOIN saneb_seed_actor actor
ON CONFLICT (announcement_id, option_group_code, option_code) DO NOTHING;

CREATE TEMP TABLE saneb_seed_numeric_conditions (
    seed_key varchar(40) NOT NULL,
    condition_scope_code varchar(30) NOT NULL,
    condition_key varchar(80) NOT NULL,
    comparator_code varchar(30) NOT NULL,
    value_number numeric(18, 2),
    min_number numeric(18, 2),
    max_number numeric(18, 2),
    unit_code varchar(30),
    document_type_code varchar(80),
    standard_field_key varchar(80)
) ON COMMIT DROP;

INSERT INTO saneb_seed_numeric_conditions (
    seed_key,
    condition_scope_code,
    condition_key,
    comparator_code,
    value_number,
    min_number,
    max_number,
    unit_code,
    document_type_code,
    standard_field_key
) VALUES
    ('REV_YEARS_SEOUL', 'BUSINESS', 'ANNUAL_REVENUE', 'BETWEEN', NULL, 50000000, 300000000, '원', 'VAT_TAX_BASE', 'ANNUAL_REVENUE'),
    ('REV_YEARS_SEOUL', 'BUSINESS', 'BUSINESS_YEARS', 'GTE', 1, NULL, NULL, '년', 'BUSINESS_REGISTRATION', 'OPENING_DATE'),
    ('REGION_GYEONGGI', 'BUSINESS', 'BUSINESS_YEARS', 'LTE', 3, NULL, NULL, '년', 'BUSINESS_REGISTRATION', 'OPENING_DATE'),
    ('BUSAN_YOUNG', 'PERSONAL', 'AGE', 'LTE', 39, NULL, NULL, '세', NULL, NULL),
    ('BUSAN_YOUNG', 'BUSINESS', 'BUSINESS_YEARS', 'LTE', 5, NULL, NULL, '년', 'BUSINESS_REGISTRATION', 'OPENING_DATE'),
    ('CHILD_FAMILY', 'CHILD', 'CHILD_COUNT', 'GTE', 2, NULL, NULL, '명', 'FAMILY_RELATION', 'CHILD_COUNT'),
    ('PARENT_CARE', 'PARENT', 'PARENT_COUNT', 'GTE', 1, NULL, NULL, '명', 'FAMILY_RELATION', 'PARENT_COUNT'),
    ('PARENT_CARE', 'PERSONAL', 'INCOME_AMOUNT', 'LTE', 50000000, NULL, NULL, '원', 'INCOME_CERTIFICATE', 'TOTAL_INCOME_AMOUNT'),
    ('DUP_LIMIT', 'BUSINESS', 'ANNUAL_REVENUE', 'LTE', 200000000, NULL, NULL, '원', 'VAT_TAX_BASE', 'ANNUAL_REVENUE'),
    ('BUDGET_EXHAUSTED', 'BUSINESS', 'ANNUAL_REVENUE', 'LTE', 150000000, NULL, NULL, '원', 'VAT_TAX_BASE', 'ANNUAL_REVENUE');

INSERT INTO announcement_numeric_conditions (
    announcement_id,
    condition_scope_code,
    condition_key,
    comparator_code,
    value_number,
    min_number,
    max_number,
    unit_code,
    standard_field_id,
    created_by,
    updated_by
)
SELECT
    ids.announcement_id,
    seed.condition_scope_code,
    seed.condition_key,
    seed.comparator_code,
    seed.value_number,
    seed.min_number,
    seed.max_number,
    seed.unit_code,
    field.id,
    actor.actor_user_id,
    actor.actor_user_id
FROM saneb_seed_numeric_conditions seed
INNER JOIN saneb_seed_announcement_ids ids ON ids.seed_key = seed.seed_key
CROSS JOIN saneb_seed_actor actor
LEFT JOIN standard_document_fields field ON field.document_type_code = seed.document_type_code
    AND field.field_key = seed.standard_field_key
ON CONFLICT (announcement_id, condition_scope_code, condition_key) DO UPDATE SET
    comparator_code = EXCLUDED.comparator_code,
    value_number = EXCLUDED.value_number,
    min_number = EXCLUDED.min_number,
    max_number = EXCLUDED.max_number,
    unit_code = EXCLUDED.unit_code,
    standard_field_id = EXCLUDED.standard_field_id,
    updated_at = now(),
    updated_by = EXCLUDED.updated_by;

CREATE TEMP TABLE saneb_seed_option_conditions (
    seed_key varchar(40) NOT NULL,
    condition_scope_code varchar(30) NOT NULL,
    condition_key varchar(80) NOT NULL,
    option_code varchar(80) NOT NULL,
    option_text varchar(500),
    document_type_code varchar(80),
    standard_field_key varchar(80)
) ON COMMIT DROP;

INSERT INTO saneb_seed_option_conditions (
    seed_key,
    condition_scope_code,
    condition_key,
    option_code,
    option_text,
    document_type_code,
    standard_field_key
) VALUES
    ('REV_YEARS_SEOUL', 'BUSINESS', 'WORKPLACE_REGION_CODE', 'SEOUL', '서울', 'BUSINESS_REGISTRATION', 'WORKPLACE_REGION_CODE'),
    ('REV_YEARS_SEOUL', 'BUSINESS', 'BUSINESS_TYPE_CODE', 'SOLE_PROPRIETOR', '개인사업자', 'BUSINESS_REGISTRATION', 'BUSINESS_TYPE_CODE'),
    ('REV_YEARS_SEOUL', 'BUSINESS', 'BUSINESS_TYPE_CODE', 'GENERAL_TAXPAYER', '일반과세자', 'BUSINESS_REGISTRATION', 'BUSINESS_TYPE_CODE'),
    ('REGION_GYEONGGI', 'BUSINESS', 'WORKPLACE_REGION_CODE', 'GYEONGGI', '경기', 'BUSINESS_REGISTRATION', 'WORKPLACE_REGION_CODE'),
    ('REGION_GYEONGGI', 'BUSINESS', 'COMPANY_STAGE', 'PRE_STARTUP', '예비창업', NULL, NULL),
    ('REGION_GYEONGGI', 'BUSINESS', 'COMPANY_STAGE', 'EARLY_STARTUP', '창업초기', NULL, NULL),
    ('BUSAN_YOUNG', 'BUSINESS', 'WORKPLACE_REGION_CODE', 'BUSAN', '부산', 'BUSINESS_REGISTRATION', 'WORKPLACE_REGION_CODE'),
    ('BUSAN_YOUNG', 'BUSINESS', 'COMPANY_STAGE', 'EARLY_STARTUP', '창업초기', NULL, NULL),
    ('CHILD_FAMILY', 'PERSONAL', 'REGION_CODE', 'SEOUL', '서울', 'RESIDENT_REGISTRATION', 'REGION_CODE'),
    ('CHILD_FAMILY', 'PERSONAL', 'REGION_CODE', 'GYEONGGI', '경기', 'RESIDENT_REGISTRATION', 'REGION_CODE'),
    ('CHILD_FAMILY', 'CHILD', 'HAS_CHILD', 'TRUE', '자녀 있음', 'FAMILY_RELATION', 'HAS_CHILD'),
    ('PARENT_CARE', 'PERSONAL', 'REGION_CODE', 'SEOUL', '서울', 'RESIDENT_REGISTRATION', 'REGION_CODE'),
    ('PARENT_CARE', 'PARENT', 'HAS_PARENT', 'TRUE', '부모 있음', 'FAMILY_RELATION', 'PARENT_COUNT'),
    ('DUP_LIMIT', 'BUSINESS', 'HAS_POLICY_FUND_USAGE', 'FALSE', '정책자금 이용 이력 없음', NULL, NULL),
    ('DUP_LIMIT', 'BUSINESS', 'HAS_GUARANTEE_USAGE', 'FALSE', '보증 이용 이력 없음', NULL, NULL),
    ('BUDGET_EXHAUSTED', 'BUSINESS', 'WORKPLACE_REGION_CODE', 'INCHEON', '인천', 'BUSINESS_REGISTRATION', 'WORKPLACE_REGION_CODE');

INSERT INTO announcement_option_conditions (
    announcement_id,
    condition_scope_code,
    condition_key,
    option_code,
    option_text,
    standard_field_id,
    created_by,
    updated_by
)
SELECT
    ids.announcement_id,
    seed.condition_scope_code,
    seed.condition_key,
    seed.option_code,
    seed.option_text,
    field.id,
    actor.actor_user_id,
    actor.actor_user_id
FROM saneb_seed_option_conditions seed
INNER JOIN saneb_seed_announcement_ids ids ON ids.seed_key = seed.seed_key
CROSS JOIN saneb_seed_actor actor
LEFT JOIN standard_document_fields field ON field.document_type_code = seed.document_type_code
    AND field.field_key = seed.standard_field_key
ON CONFLICT (announcement_id, condition_scope_code, condition_key, option_code) DO UPDATE SET
    option_text = EXCLUDED.option_text,
    standard_field_id = EXCLUDED.standard_field_id,
    updated_at = now(),
    updated_by = EXCLUDED.updated_by;

CREATE TEMP TABLE saneb_seed_document_requirements (
    seed_key varchar(40) NOT NULL,
    document_type_code varchar(80) NOT NULL,
    is_required boolean NOT NULL,
    sort_order integer NOT NULL
) ON COMMIT DROP;

INSERT INTO saneb_seed_document_requirements (
    seed_key,
    document_type_code,
    is_required,
    sort_order
) VALUES
    ('REV_YEARS_SEOUL', 'BUSINESS_REGISTRATION', true, 10),
    ('REV_YEARS_SEOUL', 'VAT_TAX_BASE', true, 20),
    ('REV_YEARS_SEOUL', 'NATIONAL_TAX_PAID', false, 30),
    ('REGION_GYEONGGI', 'BUSINESS_REGISTRATION', true, 10),
    ('REGION_GYEONGGI', 'VAT_TAX_BASE', false, 20),
    ('BUSAN_YOUNG', 'BUSINESS_REGISTRATION', true, 10),
    ('BUSAN_YOUNG', 'RESIDENT_REGISTRATION', false, 20),
    ('CHILD_FAMILY', 'RESIDENT_REGISTRATION', true, 10),
    ('CHILD_FAMILY', 'FAMILY_RELATION', true, 20),
    ('CHILD_FAMILY', 'HEALTH_INSURANCE_PAYMENT', false, 30),
    ('PARENT_CARE', 'FAMILY_RELATION', true, 10),
    ('PARENT_CARE', 'INCOME_CERTIFICATE', false, 20),
    ('DUP_LIMIT', 'BUSINESS_REGISTRATION', true, 10),
    ('DUP_LIMIT', 'VAT_TAX_BASE', false, 20),
    ('DUP_LIMIT', 'NATIONAL_TAX_PAID', false, 30),
    ('BUDGET_EXHAUSTED', 'BUSINESS_REGISTRATION', false, 10);

INSERT INTO announcement_document_requirements (
    announcement_id,
    document_type_code,
    is_required,
    sort_order,
    created_by,
    updated_by
)
SELECT
    ids.announcement_id,
    seed.document_type_code,
    seed.is_required,
    seed.sort_order,
    actor.actor_user_id,
    actor.actor_user_id
FROM saneb_seed_document_requirements seed
INNER JOIN saneb_seed_announcement_ids ids ON ids.seed_key = seed.seed_key
CROSS JOIN saneb_seed_actor actor
ON CONFLICT (announcement_id, document_type_code) DO UPDATE SET
    is_required = EXCLUDED.is_required,
    sort_order = EXCLUDED.sort_order,
    updated_at = now(),
    updated_by = EXCLUDED.updated_by;

CREATE TEMP TABLE saneb_seed_inputs (
    seed_key varchar(40) NOT NULL,
    field_key varchar(80) NOT NULL,
    field_label varchar(200) NOT NULL,
    field_type_code varchar(30) NOT NULL,
    scope_code varchar(30) NOT NULL,
    is_required boolean NOT NULL,
    is_sensitive boolean NOT NULL,
    sort_order integer NOT NULL,
    help_text text,
    document_type_code varchar(80),
    standard_field_key varchar(80)
) ON COMMIT DROP;

INSERT INTO saneb_seed_inputs (
    seed_key,
    field_key,
    field_label,
    field_type_code,
    scope_code,
    is_required,
    is_sensitive,
    sort_order,
    help_text,
    document_type_code,
    standard_field_key
) VALUES
    ('REV_YEARS_SEOUL', 'BUSINESS_REGISTRATION_NO', '사업자등록번호', 'TEXT', 'BUSINESS', false, true, 10, '사업자등록증의 등록번호를 선택 입력합니다.', 'BUSINESS_REGISTRATION', 'BUSINESS_REGISTRATION_NO'),
    ('REV_YEARS_SEOUL', 'OPENING_DATE', '개업일', 'DATE', 'BUSINESS', false, false, 20, '업력 계산에 필요한 값입니다.', 'BUSINESS_REGISTRATION', 'OPENING_DATE'),
    ('REV_YEARS_SEOUL', 'ANNUAL_REVENUE', '신고 매출액', 'AMOUNT', 'BUSINESS', false, false, 30, '부가세 과세표준증명원 기준 매출액입니다.', 'VAT_TAX_BASE', 'ANNUAL_REVENUE'),
    ('REV_YEARS_SEOUL', 'NATIONAL_TAX_DELINQUENT', '국세 체납 여부', 'BOOLEAN', 'BUSINESS', false, false, 40, '완납 여부 확인이 필요한 경우 입력합니다.', 'NATIONAL_TAX_PAID', 'NATIONAL_TAX_DELINQUENT'),
    ('REGION_GYEONGGI', 'WORKPLACE_REGION_CODE', '사업장 지역', 'SELECT', 'BUSINESS', false, false, 10, '사업자등록증 기준 사업장 지역입니다.', 'BUSINESS_REGISTRATION', 'WORKPLACE_REGION_CODE'),
    ('REGION_GYEONGGI', 'COMPANY_STAGE', '사업 상태', 'SELECT', 'BUSINESS', false, false, 20, '예비창업, 창업초기 등 현재 사업 상태입니다.', NULL, NULL),
    ('REGION_GYEONGGI', 'OPENING_DATE', '개업일', 'DATE', 'BUSINESS', false, false, 30, '창업초기 여부 판단에 활용합니다.', 'BUSINESS_REGISTRATION', 'OPENING_DATE'),
    ('BUSAN_YOUNG', 'WORKPLACE_REGION_CODE', '사업장 지역', 'SELECT', 'BUSINESS', false, false, 10, '부산 소재 여부 확인에 활용합니다.', 'BUSINESS_REGISTRATION', 'WORKPLACE_REGION_CODE'),
    ('BUSAN_YOUNG', 'REPRESENTATIVE_BIRTH_YEAR', '대표자 출생연도', 'NUMBER', 'PERSONAL', false, true, 20, '청년 여부 확인을 위해 선택 입력합니다.', NULL, NULL),
    ('BUSAN_YOUNG', 'COMPANY_STAGE', '사업 상태', 'SELECT', 'BUSINESS', false, false, 30, '창업초기 여부 확인에 활용합니다.', NULL, NULL),
    ('CHILD_FAMILY', 'REGION_CODE', '주소지 지역', 'SELECT', 'PERSONAL', false, false, 10, '주민등록등본 기준 주소지 지역입니다.', 'RESIDENT_REGISTRATION', 'REGION_CODE'),
    ('CHILD_FAMILY', 'CHILD_COUNT', '자녀 수', 'NUMBER', 'CHILD', false, false, 20, '가족관계증명서 기준 자녀 수입니다.', 'FAMILY_RELATION', 'CHILD_COUNT'),
    ('CHILD_FAMILY', 'MONTHLY_HEALTH_INSURANCE_PREMIUM', '월 건강보험료', 'AMOUNT', 'PERSONAL', false, false, 30, '소득 추정 참고값입니다.', 'HEALTH_INSURANCE_PAYMENT', 'MONTHLY_HEALTH_INSURANCE_PREMIUM'),
    ('PARENT_CARE', 'REGION_CODE', '주소지 지역', 'SELECT', 'PERSONAL', false, false, 10, '주민등록등본 기준 주소지 지역입니다.', 'RESIDENT_REGISTRATION', 'REGION_CODE'),
    ('PARENT_CARE', 'PARENT_COUNT', '부모 수', 'NUMBER', 'PARENT', false, false, 20, '가족관계증명서 기준 부모 수입니다.', 'FAMILY_RELATION', 'PARENT_COUNT'),
    ('PARENT_CARE', 'TOTAL_INCOME_AMOUNT', '총 소득금액', 'AMOUNT', 'PERSONAL', false, true, 30, '소득금액증명원 기준 금액입니다.', 'INCOME_CERTIFICATE', 'TOTAL_INCOME_AMOUNT'),
    ('DUP_LIMIT', 'ANNUAL_REVENUE', '신고 매출액', 'AMOUNT', 'BUSINESS', false, false, 10, '매출 제한 조건 확인용 선택 입력입니다.', 'VAT_TAX_BASE', 'ANNUAL_REVENUE'),
    ('DUP_LIMIT', 'HAS_POLICY_FUND_USAGE', '정책자금 이용 이력', 'BOOLEAN', 'BUSINESS', false, false, 20, '중복지원 제한 확인용 선택 입력입니다.', NULL, NULL),
    ('DUP_LIMIT', 'HAS_GUARANTEE_USAGE', '보증 이용 이력', 'BOOLEAN', 'BUSINESS', false, false, 30, '중복지원 제한 확인용 선택 입력입니다.', NULL, NULL),
    ('BUDGET_EXHAUSTED', 'WORKPLACE_REGION_CODE', '사업장 지역', 'SELECT', 'BUSINESS', false, false, 10, '예산 소진 상태 확인용 선택 입력입니다.', 'BUSINESS_REGISTRATION', 'WORKPLACE_REGION_CODE');

INSERT INTO announcement_input_requirements (
    announcement_id,
    field_key,
    field_label,
    field_type_code,
    scope_code,
    is_required,
    is_sensitive,
    sort_order,
    help_text,
    standard_field_id,
    created_by,
    updated_by
)
SELECT
    ids.announcement_id,
    seed.field_key,
    seed.field_label,
    seed.field_type_code,
    seed.scope_code,
    seed.is_required,
    seed.is_sensitive,
    seed.sort_order,
    seed.help_text,
    field.id,
    actor.actor_user_id,
    actor.actor_user_id
FROM saneb_seed_inputs seed
INNER JOIN saneb_seed_announcement_ids ids ON ids.seed_key = seed.seed_key
CROSS JOIN saneb_seed_actor actor
LEFT JOIN standard_document_fields field ON field.document_type_code = seed.document_type_code
    AND field.field_key = seed.standard_field_key
ON CONFLICT (announcement_id, field_key) DO UPDATE SET
    field_label = EXCLUDED.field_label,
    field_type_code = EXCLUDED.field_type_code,
    scope_code = EXCLUDED.scope_code,
    is_required = EXCLUDED.is_required,
    is_sensitive = EXCLUDED.is_sensitive,
    sort_order = EXCLUDED.sort_order,
    help_text = EXCLUDED.help_text,
    standard_field_id = EXCLUDED.standard_field_id,
    updated_at = now(),
    updated_by = EXCLUDED.updated_by;

CREATE TEMP TABLE saneb_seed_input_options (
    seed_key varchar(40) NOT NULL,
    field_key varchar(80) NOT NULL,
    option_code varchar(80) NOT NULL,
    option_label varchar(200) NOT NULL,
    sort_order integer NOT NULL
) ON COMMIT DROP;

INSERT INTO saneb_seed_input_options (
    seed_key,
    field_key,
    option_code,
    option_label,
    sort_order
) VALUES
    ('REGION_GYEONGGI', 'WORKPLACE_REGION_CODE', 'GYEONGGI', '경기', 10),
    ('REGION_GYEONGGI', 'WORKPLACE_REGION_CODE', 'SEOUL', '서울', 20),
    ('REGION_GYEONGGI', 'COMPANY_STAGE', 'PRE_STARTUP', '예비창업', 10),
    ('REGION_GYEONGGI', 'COMPANY_STAGE', 'EARLY_STARTUP', '창업초기', 20),
    ('REGION_GYEONGGI', 'COMPANY_STAGE', 'OPERATING', '운영 중', 30),
    ('BUSAN_YOUNG', 'WORKPLACE_REGION_CODE', 'BUSAN', '부산', 10),
    ('BUSAN_YOUNG', 'WORKPLACE_REGION_CODE', 'GYEONGGI', '경기', 20),
    ('BUSAN_YOUNG', 'COMPANY_STAGE', 'EARLY_STARTUP', '창업초기', 10),
    ('BUSAN_YOUNG', 'COMPANY_STAGE', 'OPERATING', '운영 중', 20),
    ('CHILD_FAMILY', 'REGION_CODE', 'SEOUL', '서울', 10),
    ('CHILD_FAMILY', 'REGION_CODE', 'GYEONGGI', '경기', 20),
    ('CHILD_FAMILY', 'REGION_CODE', 'INCHEON', '인천', 30),
    ('PARENT_CARE', 'REGION_CODE', 'SEOUL', '서울', 10),
    ('PARENT_CARE', 'REGION_CODE', 'GYEONGGI', '경기', 20),
    ('BUDGET_EXHAUSTED', 'WORKPLACE_REGION_CODE', 'INCHEON', '인천', 10),
    ('BUDGET_EXHAUSTED', 'WORKPLACE_REGION_CODE', 'SEOUL', '서울', 20);

CREATE TEMP TABLE saneb_seed_requirement_ids ON COMMIT DROP AS
SELECT
    inputs.seed_key,
    inputs.field_key,
    requirement.id AS requirement_id
FROM saneb_seed_inputs inputs
INNER JOIN saneb_seed_announcement_ids ids ON ids.seed_key = inputs.seed_key
INNER JOIN announcement_input_requirements requirement ON requirement.announcement_id = ids.announcement_id
    AND requirement.field_key = inputs.field_key;

INSERT INTO announcement_input_options (
    requirement_id,
    option_code,
    option_label,
    sort_order,
    created_by,
    updated_by
)
SELECT
    requirements.requirement_id,
    options.option_code,
    options.option_label,
    options.sort_order,
    actor.actor_user_id,
    actor.actor_user_id
FROM saneb_seed_input_options options
INNER JOIN saneb_seed_requirement_ids requirements ON requirements.seed_key = options.seed_key
    AND requirements.field_key = options.field_key
CROSS JOIN saneb_seed_actor actor
ON CONFLICT (requirement_id, option_code) DO UPDATE SET
    option_label = EXCLUDED.option_label,
    sort_order = EXCLUDED.sort_order,
    updated_at = now(),
    updated_by = EXCLUDED.updated_by;

CREATE TEMP TABLE saneb_seed_steps (
    seed_key varchar(40) NOT NULL,
    step_order integer NOT NULL,
    step_name varchar(100) NOT NULL,
    guide_message text,
    action_guide text,
    completion_condition_code varchar(80) NOT NULL,
    next_condition_code varchar(80),
    is_active boolean NOT NULL
) ON COMMIT DROP;

INSERT INTO saneb_seed_steps (
    seed_key,
    step_order,
    step_name,
    guide_message,
    action_guide,
    completion_condition_code,
    next_condition_code,
    is_active
)
SELECT
    seed_key,
    1,
    '기본 정보 확인',
    '신청 전 기본 정보와 공고 조건을 확인합니다.',
    '입력값이 맞으면 다음 단계로 이동합니다.',
    'BUTTON_CLICK',
    'MOVE_NEXT',
    true
FROM saneb_seed_announcements
UNION ALL
SELECT
    seed_key,
    2,
    '서류 전달 및 접수',
    '필요 서류를 확인하고 회사 또는 담당자에게 전달합니다.',
    '서류 전달이 끝나면 접수 단계로 이동합니다.',
    'DOCUMENT_SUBMITTED',
    'MOVE_NEXT',
    true
FROM saneb_seed_announcements
UNION ALL
SELECT
    seed_key,
    3,
    '결과 확인',
    '접수 결과와 최종 수령 금액을 확인합니다.',
    '관리자가 최종 결과를 기록하면 누적 현황에 반영됩니다.',
    'RESULT_RECORDED',
    NULL,
    true
FROM saneb_seed_announcements;

INSERT INTO announcement_progress_steps (
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
)
SELECT
    ids.announcement_id,
    steps.step_order,
    steps.step_name,
    steps.guide_message,
    steps.action_guide,
    steps.completion_condition_code,
    steps.next_condition_code,
    steps.is_active,
    actor.actor_user_id,
    actor.actor_user_id
FROM saneb_seed_steps steps
INNER JOIN saneb_seed_announcement_ids ids ON ids.seed_key = steps.seed_key
CROSS JOIN saneb_seed_actor actor
ON CONFLICT (announcement_id, step_order) DO UPDATE SET
    step_name = EXCLUDED.step_name,
    guide_message = EXCLUDED.guide_message,
    action_guide = EXCLUDED.action_guide,
    completion_condition_code = EXCLUDED.completion_condition_code,
    next_condition_code = EXCLUDED.next_condition_code,
    is_active = EXCLUDED.is_active,
    updated_at = now(),
    updated_by = EXCLUDED.updated_by;

CREATE TEMP TABLE saneb_seed_step_ids ON COMMIT DROP AS
SELECT
    steps.seed_key,
    steps.step_order,
    progress_step.id AS step_id
FROM saneb_seed_steps steps
INNER JOIN saneb_seed_announcement_ids ids ON ids.seed_key = steps.seed_key
INNER JOIN announcement_progress_steps progress_step ON progress_step.announcement_id = ids.announcement_id
    AND progress_step.step_order = steps.step_order;

INSERT INTO announcement_step_documents (
    step_id,
    document_type_code,
    is_required,
    sort_order,
    created_by,
    updated_by
)
SELECT
    step_ids.step_id,
    docs.document_type_code,
    docs.is_required,
    docs.sort_order,
    actor.actor_user_id,
    actor.actor_user_id
FROM saneb_seed_document_requirements docs
INNER JOIN saneb_seed_step_ids step_ids ON step_ids.seed_key = docs.seed_key
    AND step_ids.step_order = 2
CROSS JOIN saneb_seed_actor actor
ON CONFLICT (step_id, document_type_code) DO UPDATE SET
    is_required = EXCLUDED.is_required,
    sort_order = EXCLUDED.sort_order,
    updated_at = now(),
    updated_by = EXCLUDED.updated_by;

CREATE TEMP TABLE saneb_seed_buttons (
    seed_key varchar(40) NOT NULL,
    step_order integer NOT NULL,
    button_code varchar(80) NOT NULL,
    button_label varchar(100) NOT NULL,
    button_action_code varchar(80) NOT NULL,
    next_step_order integer,
    sort_order integer NOT NULL
) ON COMMIT DROP;

INSERT INTO saneb_seed_buttons (
    seed_key,
    step_order,
    button_code,
    button_label,
    button_action_code,
    next_step_order,
    sort_order
)
SELECT
    seed_key,
    1,
    'CONFIRM_BASIC_INFO',
    '기본 정보 확인 완료',
    'MOVE_NEXT',
    2,
    10
FROM saneb_seed_announcements
UNION ALL
SELECT
    seed_key,
    2,
    'SUBMIT_DOCUMENTS',
    '서류 전달 완료',
    'MOVE_NEXT',
    3,
    10
FROM saneb_seed_announcements
UNION ALL
SELECT
    seed_key,
    3,
    'CONFIRM_WAITING_RESULT',
    '결과 대기',
    'COMPLETE_STEP',
    NULL,
    10
FROM saneb_seed_announcements;

INSERT INTO announcement_step_buttons (
    step_id,
    button_code,
    button_label,
    button_action_code,
    next_step_id,
    sort_order,
    created_by,
    updated_by
)
SELECT
    current_step.step_id,
    buttons.button_code,
    buttons.button_label,
    buttons.button_action_code,
    next_step.step_id,
    buttons.sort_order,
    actor.actor_user_id,
    actor.actor_user_id
FROM saneb_seed_buttons buttons
INNER JOIN saneb_seed_step_ids current_step ON current_step.seed_key = buttons.seed_key
    AND current_step.step_order = buttons.step_order
LEFT JOIN saneb_seed_step_ids next_step ON next_step.seed_key = buttons.seed_key
    AND next_step.step_order = buttons.next_step_order
CROSS JOIN saneb_seed_actor actor
ON CONFLICT (step_id, button_code) DO UPDATE SET
    button_label = EXCLUDED.button_label,
    button_action_code = EXCLUDED.button_action_code,
    next_step_id = EXCLUDED.next_step_id,
    sort_order = EXCLUDED.sort_order,
    updated_at = now(),
    updated_by = EXCLUDED.updated_by;

SELECT
    a.public_code AS "공고코드",
    a.title AS "공고명",
    a.agency_name AS "기관명",
    a.target_type_code AS "지원주체",
    a.manual_status_code AS "수동상태",
    a.approval_status_code AS "승인상태",
    a.application_start_date AS "접수시작일",
    a.application_end_date AS "접수마감일",
    a.min_amount AS "최소금액",
    a.max_amount AS "최대금액"
FROM saneb_seed_announcement_ids ids
INNER JOIN announcements a ON a.id = ids.announcement_id
ORDER BY
    a.application_end_date ASC NULLS LAST,
    a.application_start_date ASC,
    a.created_at ASC;

COMMIT;
