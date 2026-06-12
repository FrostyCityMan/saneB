CREATE TABLE member_document_input_values (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    standard_field_id uuid NOT NULL,
    value_text text,
    value_number numeric(18, 2),
    value_date date,
    value_boolean boolean,
    submitted_by uuid,
    submitted_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_member_document_input_values_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_member_document_input_values_standard_field FOREIGN KEY (standard_field_id) REFERENCES standard_document_fields (id),
    CONSTRAINT fk_member_document_input_values_submitted_by FOREIGN KEY (submitted_by) REFERENCES users (id),
    CONSTRAINT uq_member_document_input_values_field UNIQUE (user_id, standard_field_id),
    CONSTRAINT ck_member_document_input_values_single_value CHECK (
        num_nonnulls(value_text, value_number, value_date, value_boolean) <= 1
    ),
    CONSTRAINT ck_member_document_input_values_number CHECK (
        value_number IS NULL OR value_number >= 0
    )
);

CREATE INDEX ix_member_document_input_values_user
    ON member_document_input_values (user_id, updated_at DESC);
CREATE INDEX ix_member_document_input_values_standard_field
    ON member_document_input_values (standard_field_id);

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
    ('BUSINESS_REGISTRATION', 'WORKPLACE_ADDRESS', '사업장 주소', 'TEXT', 'BUSINESS', false, 45, '사업자등록증에 표시된 사업장 주소입니다.'),
    ('BUSINESS_REGISTRATION', 'BUSINESS_CATEGORY', '업태', 'TEXT', 'BUSINESS', false, 55, '사업자등록증에 표시된 업태입니다.'),
    ('BUSINESS_REGISTRATION', 'BUSINESS_ITEM', '종목', 'TEXT', 'BUSINESS', false, 56, '사업자등록증에 표시된 종목입니다.'),
    ('TAX_EXEMPT_INCOME', 'BUSINESS_INFO', '사업자 정보', 'TEXTAREA', 'BUSINESS', false, 40, '수입금액증명원에 표시된 사업자 정보입니다.'),
    ('INCOME_CERTIFICATE', 'COMPREHENSIVE_INCOME_AMOUNT', '종합소득금액', 'AMOUNT', 'PERSONAL', false, 5, '소득금액증명원의 종합소득금액입니다.'),
    ('NATIONAL_TAX_PAID', 'TAX_PAID_STATUS', '국세 완납 여부', 'BOOLEAN', 'BUSINESS', false, 15, '국세완납증명서의 완납 여부입니다.'),
    ('LOCAL_TAX_PAID', 'TAX_PAID_STATUS', '지방세 완납 여부', 'BOOLEAN', 'BUSINESS', false, 15, '지방세완납증명서의 완납 여부입니다.'),
    ('RESIDENT_REGISTRATION', 'ADDRESS', '주소', 'TEXT', 'PERSONAL', false, 5, '주민등록등본에 표시된 주소입니다.'),
    ('RESIDENT_REGISTRATION', 'HOUSEHOLD_MEMBER_INFO', '세대원 정보', 'TEXTAREA', 'APPLICATION', false, 25, '주민등록등본의 세대원 정보를 요약해 입력합니다.'),
    ('RESIDENT_REGISTRATION', 'HOUSEHOLD_COMPOSITION', '세대 구성', 'TEXT', 'APPLICATION', false, 35, '1인가구, 다인가구 등 세대 구성을 입력합니다.'),
    ('FAMILY_RELATION', 'SPOUSE_INFO', '배우자 정보', 'TEXTAREA', 'SPOUSE', false, 12, '가족관계증명서의 배우자 정보를 요약해 입력합니다.'),
    ('FAMILY_RELATION', 'CHILD_INFO', '자녀 정보', 'TEXTAREA', 'CHILD', false, 22, '가족관계증명서의 자녀 정보를 요약해 입력합니다.'),
    ('FAMILY_RELATION', 'PARENT_INFO', '부모 정보', 'TEXTAREA', 'PARENT', false, 32, '가족관계증명서의 부모 정보를 요약해 입력합니다.'),
    ('FAMILY_RELATION', 'FAMILY_RELATION_DETAIL', '가족관계', 'TEXTAREA', 'APPLICATION', false, 50, '가족관계증명서의 가족관계를 요약해 입력합니다.'),
    ('HEALTH_INSURANCE_PAYMENT', 'RECENT_HEALTH_INSURANCE_PREMIUM', '최근 건강보험료', 'AMOUNT', 'PERSONAL', false, 5, '최근 고지 또는 납부된 건강보험료입니다.'),
    ('HEALTH_INSURANCE_QUALIFICATION', 'WORKPLACE_INSURED_STATUS', '직장가입 여부', 'BOOLEAN', 'PERSONAL', false, 5, '건강보험 자격확인서 기준 직장가입 여부입니다.'),
    ('HEALTH_INSURANCE_QUALIFICATION', 'LOCAL_INSURED_STATUS', '지역가입 여부', 'BOOLEAN', 'PERSONAL', false, 6, '건강보험 자격확인서 기준 지역가입 여부입니다.'),
    ('HEALTH_INSURANCE_QUALIFICATION', 'INSURED_PERSON_INFO', '가입자 정보', 'TEXTAREA', 'PERSONAL', false, 30, '건강보험 자격확인서의 가입자 정보를 요약해 입력합니다.')
ON CONFLICT (document_type_code, field_key) DO NOTHING;
