CREATE TABLE standard_document_fields (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    document_type_code varchar(80) NOT NULL,
    field_key varchar(80) NOT NULL,
    field_label varchar(200) NOT NULL,
    field_type_code varchar(30) NOT NULL,
    scope_code varchar(30) NOT NULL,
    required_default boolean NOT NULL DEFAULT false,
    is_selectable boolean NOT NULL DEFAULT true,
    sort_order integer NOT NULL DEFAULT 0,
    help_text text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_standard_document_fields_key UNIQUE (document_type_code, field_key),
    CONSTRAINT ck_standard_document_fields_document_type CHECK (
        document_type_code IN (
            'BUSINESS_REGISTRATION',
            'VAT_TAX_BASE',
            'TAX_EXEMPT_INCOME',
            'INCOME_CERTIFICATE',
            'NATIONAL_TAX_PAID',
            'LOCAL_TAX_PAID',
            'RESIDENT_REGISTRATION',
            'FAMILY_RELATION',
            'HEALTH_INSURANCE_PAYMENT',
            'HEALTH_INSURANCE_QUALIFICATION'
        )
    ),
    CONSTRAINT ck_standard_document_fields_field_type CHECK (
        field_type_code IN (
            'TEXT', 'TEXTAREA', 'NUMBER', 'AMOUNT', 'DATE', 'BOOLEAN', 'SELECT', 'RADIO', 'MULTI_SELECT'
        )
    ),
    CONSTRAINT ck_standard_document_fields_scope CHECK (
        scope_code IN ('BUSINESS', 'PERSONAL', 'SPOUSE', 'CHILD', 'PARENT', 'APPLICATION', 'SUPPORT')
    ),
    CONSTRAINT ck_standard_document_fields_sort_order CHECK (sort_order >= 0)
);

CREATE INDEX ix_standard_document_fields_document_sort
    ON standard_document_fields (document_type_code, sort_order);
CREATE INDEX ix_standard_document_fields_scope_sort
    ON standard_document_fields (scope_code, sort_order);

ALTER TABLE announcement_numeric_conditions
    ADD COLUMN standard_field_id uuid,
    ADD CONSTRAINT fk_announcement_numeric_conditions_standard_field
        FOREIGN KEY (standard_field_id) REFERENCES standard_document_fields (id);

ALTER TABLE announcement_option_conditions
    ADD COLUMN standard_field_id uuid,
    ADD CONSTRAINT fk_announcement_option_conditions_standard_field
        FOREIGN KEY (standard_field_id) REFERENCES standard_document_fields (id);

ALTER TABLE announcement_document_requirements
    ADD COLUMN standard_field_id uuid,
    ADD CONSTRAINT fk_announcement_document_requirements_standard_field
        FOREIGN KEY (standard_field_id) REFERENCES standard_document_fields (id);

ALTER TABLE announcement_input_requirements
    ADD COLUMN standard_field_id uuid,
    ADD CONSTRAINT fk_announcement_input_requirements_standard_field
        FOREIGN KEY (standard_field_id) REFERENCES standard_document_fields (id);

ALTER TABLE member_profiles
    ADD COLUMN income_presence_code varchar(30),
    ADD COLUMN income_amount numeric(18, 2),
    ADD COLUMN income_period_code varchar(30),
    ADD COLUMN income_note text,
    ADD CONSTRAINT ck_member_profiles_income_presence CHECK (
        income_presence_code IS NULL OR income_presence_code IN ('UNKNOWN', 'NONE', 'HAS_INCOME')
    ),
    ADD CONSTRAINT ck_member_profiles_income_amount CHECK (
        income_amount IS NULL OR income_amount >= 0
    );

ALTER TABLE business_profiles
    ADD COLUMN annual_revenue numeric(18, 2),
    ADD COLUMN annual_revenue_year integer,
    ADD COLUMN has_policy_fund_usage boolean,
    ADD COLUMN has_guarantee_usage boolean,
    ADD CONSTRAINT ck_business_profiles_annual_revenue CHECK (
        annual_revenue IS NULL OR annual_revenue >= 0
    ),
    ADD CONSTRAINT ck_business_profiles_annual_revenue_year CHECK (
        annual_revenue_year IS NULL OR annual_revenue_year BETWEEN 1900 AND 2200
    );

ALTER TABLE family_members
    ADD COLUMN income_presence_code varchar(30),
    ADD COLUMN income_amount numeric(18, 2),
    ADD COLUMN income_period_code varchar(30),
    ADD COLUMN income_note text,
    ADD CONSTRAINT ck_family_members_income_presence CHECK (
        income_presence_code IS NULL OR income_presence_code IN ('UNKNOWN', 'NONE', 'HAS_INCOME')
    ),
    ADD CONSTRAINT ck_family_members_income_amount CHECK (
        income_amount IS NULL OR income_amount >= 0
    );

ALTER TABLE verification_family_values
    ADD COLUMN income_presence_code varchar(30),
    ADD COLUMN income_amount numeric(18, 2),
    ADD COLUMN income_period_code varchar(30),
    ADD COLUMN income_note text,
    ADD CONSTRAINT ck_verification_family_values_income_presence CHECK (
        income_presence_code IS NULL OR income_presence_code IN ('UNKNOWN', 'NONE', 'HAS_INCOME')
    ),
    ADD CONSTRAINT ck_verification_family_values_income_amount CHECK (
        income_amount IS NULL OR income_amount >= 0
    );

CREATE INDEX ix_announcement_numeric_conditions_standard_field
    ON announcement_numeric_conditions (standard_field_id);
CREATE INDEX ix_announcement_option_conditions_standard_field
    ON announcement_option_conditions (standard_field_id);
CREATE INDEX ix_announcement_document_requirements_standard_field
    ON announcement_document_requirements (standard_field_id);
CREATE INDEX ix_announcement_input_requirements_standard_field
    ON announcement_input_requirements (standard_field_id);
CREATE INDEX ix_member_profiles_income_presence
    ON member_profiles (income_presence_code);
CREATE INDEX ix_business_profiles_annual_revenue
    ON business_profiles (annual_revenue);
CREATE INDEX ix_family_members_income_presence
    ON family_members (user_id, relation_type_code, income_presence_code);
CREATE UNIQUE INDEX uq_matching_cases_no_verification
    ON matching_cases (announcement_id, member_user_id)
    WHERE verification_id IS NULL;

INSERT INTO standard_document_fields (
    document_type_code,
    field_key,
    field_label,
    field_type_code,
    scope_code,
    required_default,
    sort_order,
    help_text
) VALUES
    ('BUSINESS_REGISTRATION', 'BUSINESS_REGISTRATION_NO', '사업자등록번호', 'TEXT', 'BUSINESS', false, 10, '사업자등록증에 표시된 등록번호입니다.'),
    ('BUSINESS_REGISTRATION', 'BUSINESS_NAME', '상호명', 'TEXT', 'BUSINESS', false, 20, '사업자등록증에 표시된 상호명입니다.'),
    ('BUSINESS_REGISTRATION', 'REPRESENTATIVE_NAME', '대표자명', 'TEXT', 'BUSINESS', false, 30, '사업자등록증에 표시된 대표자명입니다.'),
    ('BUSINESS_REGISTRATION', 'OPENING_DATE', '개업일', 'DATE', 'BUSINESS', false, 40, '업력 계산에 활용할 수 있습니다.'),
    ('BUSINESS_REGISTRATION', 'WORKPLACE_REGION_CODE', '사업장 지역', 'SELECT', 'BUSINESS', false, 50, '지역 제한 조건에 활용할 수 있습니다.'),
    ('BUSINESS_REGISTRATION', 'INDUSTRY_NAME', '업태/종목', 'TEXT', 'BUSINESS', false, 60, '업종 확인에 활용할 수 있습니다.'),
    ('BUSINESS_REGISTRATION', 'BUSINESS_TYPE_CODE', '사업자 유형', 'SELECT', 'BUSINESS', false, 70, '개인사업자, 법인 등 구분값입니다.'),
    ('BUSINESS_REGISTRATION', 'TAX_TYPE_CODE', '과세 유형', 'SELECT', 'BUSINESS', false, 80, '일반과세, 간이과세, 면세 여부를 기록합니다.'),
    ('VAT_TAX_BASE', 'TAX_PERIOD', '과세기간', 'TEXT', 'BUSINESS', false, 10, '부가세 과세표준증명원의 과세기간입니다.'),
    ('VAT_TAX_BASE', 'SUPPLY_AMOUNT', '공급가액', 'AMOUNT', 'BUSINESS', false, 20, '매출 규모 판단에 활용할 수 있습니다.'),
    ('VAT_TAX_BASE', 'ANNUAL_REVENUE', '신고 매출액', 'AMOUNT', 'BUSINESS', false, 30, '연 매출 조건에 활용할 수 있습니다.'),
    ('VAT_TAX_BASE', 'TAX_TYPE_CODE', '과세 유형', 'SELECT', 'BUSINESS', false, 40, '과세 유형 조건에 활용할 수 있습니다.'),
    ('TAX_EXEMPT_INCOME', 'TOTAL_INCOME_AMOUNT', '총 수입금액', 'AMOUNT', 'BUSINESS', false, 10, '면세사업자 매출 조건에 활용할 수 있습니다.'),
    ('TAX_EXEMPT_INCOME', 'ATTRIBUTION_YEAR', '귀속연도', 'NUMBER', 'BUSINESS', false, 20, '수입금액의 기준 연도입니다.'),
    ('TAX_EXEMPT_INCOME', 'BUSINESS_REGISTRATION_NO', '사업자등록번호', 'TEXT', 'BUSINESS', false, 30, '사업자 식별에 활용할 수 있습니다.'),
    ('INCOME_CERTIFICATE', 'TOTAL_INCOME_AMOUNT', '총 소득금액', 'AMOUNT', 'PERSONAL', false, 10, '개인 또는 가구소득 참고값입니다.'),
    ('INCOME_CERTIFICATE', 'BUSINESS_INCOME_AMOUNT', '사업소득', 'AMOUNT', 'PERSONAL', false, 20, '사업소득 조건에 활용할 수 있습니다.'),
    ('INCOME_CERTIFICATE', 'LABOR_INCOME_AMOUNT', '근로소득', 'AMOUNT', 'PERSONAL', false, 30, '근로소득 조건에 활용할 수 있습니다.'),
    ('INCOME_CERTIFICATE', 'OTHER_INCOME_AMOUNT', '기타소득', 'AMOUNT', 'PERSONAL', false, 40, '기타소득 참고값입니다.'),
    ('NATIONAL_TAX_PAID', 'NATIONAL_TAX_DELINQUENT', '국세 체납 여부', 'BOOLEAN', 'BUSINESS', false, 10, '국세 체납 제한 조건에 활용할 수 있습니다.'),
    ('NATIONAL_TAX_PAID', 'ISSUE_DATE', '발급일', 'DATE', 'BUSINESS', false, 20, '증명서 기준일입니다.'),
    ('LOCAL_TAX_PAID', 'LOCAL_TAX_DELINQUENT', '지방세 체납 여부', 'BOOLEAN', 'BUSINESS', false, 10, '지방세 체납 제한 조건에 활용할 수 있습니다.'),
    ('LOCAL_TAX_PAID', 'ISSUE_DATE', '발급일', 'DATE', 'BUSINESS', false, 20, '증명서 기준일입니다.'),
    ('RESIDENT_REGISTRATION', 'REGION_CODE', '주소지 지역', 'SELECT', 'PERSONAL', false, 10, '거주지 지역 조건에 활용할 수 있습니다.'),
    ('RESIDENT_REGISTRATION', 'IS_HOUSEHOLDER', '세대주 여부', 'BOOLEAN', 'PERSONAL', false, 20, '세대주 조건에 활용할 수 있습니다.'),
    ('RESIDENT_REGISTRATION', 'HOUSEHOLD_MEMBER_COUNT', '세대원 수', 'NUMBER', 'APPLICATION', false, 30, '세대 구성 조건에 활용할 수 있습니다.'),
    ('RESIDENT_REGISTRATION', 'MOVE_IN_DATE', '전입일', 'DATE', 'PERSONAL', false, 40, '거주 기간 확인에 활용할 수 있습니다.'),
    ('FAMILY_RELATION', 'HAS_SPOUSE', '배우자 여부', 'BOOLEAN', 'SPOUSE', false, 10, '배우자 조건에 활용할 수 있습니다.'),
    ('FAMILY_RELATION', 'CHILD_COUNT', '자녀 수', 'NUMBER', 'CHILD', false, 20, '자녀 수 조건에 활용할 수 있습니다.'),
    ('FAMILY_RELATION', 'PARENT_COUNT', '부모 수', 'NUMBER', 'PARENT', false, 30, '부모 관련 조건에 활용할 수 있습니다.'),
    ('FAMILY_RELATION', 'FAMILY_MEMBER_BIRTH_YEAR', '가족 출생연도', 'NUMBER', 'APPLICATION', false, 40, '가족 나이 조건에 활용할 수 있습니다.'),
    ('HEALTH_INSURANCE_PAYMENT', 'MONTHLY_HEALTH_INSURANCE_PREMIUM', '월 건강보험료', 'AMOUNT', 'PERSONAL', false, 10, '소득 추정 참고값입니다.'),
    ('HEALTH_INSURANCE_PAYMENT', 'ANNUAL_HEALTH_INSURANCE_PREMIUM', '연 건강보험료', 'AMOUNT', 'PERSONAL', false, 20, '소득 추정 참고값입니다.'),
    ('HEALTH_INSURANCE_PAYMENT', 'INSURANCE_SUBSCRIBER_TYPE', '건강보험 가입 유형', 'SELECT', 'PERSONAL', false, 30, '직장/지역/피부양자 구분입니다.'),
    ('HEALTH_INSURANCE_QUALIFICATION', 'HEALTH_INSURANCE_BASIS_CODE', '건강보험 자격', 'SELECT', 'PERSONAL', false, 10, '가입 자격 조건에 활용할 수 있습니다.'),
    ('HEALTH_INSURANCE_QUALIFICATION', 'DEPENDENT_STATUS', '피부양자 여부', 'BOOLEAN', 'PERSONAL', false, 20, '피부양자 조건에 활용할 수 있습니다.')
ON CONFLICT (document_type_code, field_key) DO NOTHING;
