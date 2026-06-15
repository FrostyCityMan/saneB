ALTER TABLE standard_document_fields
    ADD COLUMN condition_usage_code varchar(40) NOT NULL DEFAULT 'INPUT_ONLY';

ALTER TABLE standard_document_fields
    ADD CONSTRAINT ck_standard_document_fields_condition_usage
        CHECK (condition_usage_code IN ('INPUT_ONLY', 'CONDITION_READY', 'STANDARDIZATION_REQUIRED'));

UPDATE standard_document_fields
SET condition_usage_code = 'CONDITION_READY'
WHERE is_condition_eligible = true;

UPDATE standard_document_fields
SET condition_usage_code = 'STANDARDIZATION_REQUIRED'
WHERE (document_type_code, field_key) IN (
    ('BUSINESS_REGISTRATION', 'WORKPLACE_ADDRESS'),
    ('BUSINESS_REGISTRATION', 'INDUSTRY_NAME'),
    ('BUSINESS_REGISTRATION', 'BUSINESS_CATEGORY'),
    ('BUSINESS_REGISTRATION', 'BUSINESS_ITEM'),
    ('TAX_EXEMPT_INCOME', 'BUSINESS_INFO'),
    ('RESIDENT_REGISTRATION', 'ADDRESS'),
    ('RESIDENT_REGISTRATION', 'HOUSEHOLD_MEMBER_INFO'),
    ('RESIDENT_REGISTRATION', 'HOUSEHOLD_COMPOSITION'),
    ('FAMILY_RELATION', 'SPOUSE_INFO'),
    ('FAMILY_RELATION', 'CHILD_INFO'),
    ('FAMILY_RELATION', 'PARENT_INFO'),
    ('FAMILY_RELATION', 'FAMILY_RELATION_DETAIL'),
    ('HEALTH_INSURANCE_QUALIFICATION', 'INSURED_PERSON_INFO')
);

CREATE INDEX ix_standard_document_fields_condition_usage
    ON standard_document_fields (condition_usage_code, document_type_code, sort_order);

CREATE TABLE standard_code_groups (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    group_code varchar(80) NOT NULL,
    group_name varchar(200) NOT NULL,
    source_name varchar(200) NOT NULL,
    source_url text,
    version_label varchar(80) NOT NULL,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_standard_code_groups_code UNIQUE (group_code)
);

CREATE INDEX ix_standard_code_groups_active_sort
    ON standard_code_groups (is_active, group_code);

CREATE TABLE standard_codes (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id uuid NOT NULL,
    code varchar(80) NOT NULL,
    code_name varchar(300) NOT NULL,
    parent_code varchar(80),
    level_no integer,
    sort_order integer NOT NULL DEFAULT 0,
    is_active boolean NOT NULL DEFAULT true,
    metadata_json jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_standard_codes_group FOREIGN KEY (group_id) REFERENCES standard_code_groups (id),
    CONSTRAINT uq_standard_codes_group_code UNIQUE (group_id, code),
    CONSTRAINT ck_standard_codes_level CHECK (level_no IS NULL OR level_no >= 0),
    CONSTRAINT ck_standard_codes_sort_order CHECK (sort_order >= 0)
);

CREATE INDEX ix_standard_codes_group_parent_sort
    ON standard_codes (group_id, parent_code, sort_order);
CREATE INDEX ix_standard_codes_group_name
    ON standard_codes (group_id, code_name);
CREATE INDEX ix_standard_codes_active
    ON standard_codes (group_id, is_active, sort_order);

CREATE TABLE standard_field_code_groups (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    standard_field_id uuid NOT NULL,
    group_id uuid NOT NULL,
    usage_code varchar(40) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_standard_field_code_groups_field FOREIGN KEY (standard_field_id)
        REFERENCES standard_document_fields (id),
    CONSTRAINT fk_standard_field_code_groups_group FOREIGN KEY (group_id)
        REFERENCES standard_code_groups (id),
    CONSTRAINT uq_standard_field_code_groups_field_group UNIQUE (standard_field_id, group_id),
    CONSTRAINT ck_standard_field_code_groups_usage
        CHECK (usage_code IN ('CONDITION_VALUE', 'DISPLAY_OPTION', 'REFERENCE_MAPPING'))
);

CREATE INDEX ix_standard_field_code_groups_group
    ON standard_field_code_groups (group_id, usage_code);

INSERT INTO standard_code_groups (
    group_code,
    group_name,
    source_name,
    source_url,
    version_label
) VALUES
    ('KSIC_11', '한국표준산업분류 제11차', '통계청', 'https://kssc.kostat.go.kr', '제11차'),
    ('NTS_BUSINESS_TYPE', '사업자 유형', '국세청', 'https://www.nts.go.kr', 'MVP'),
    ('NTS_TAX_TYPE', '과세 유형', '국세청', 'https://www.nts.go.kr', 'MVP'),
    ('REGION_SIDO', '시도', '행정안전부 행정표준코드', 'https://www.code.go.kr', 'MVP'),
    ('LEGAL_DONG', '법정동', '행정안전부 행정표준코드', 'https://www.code.go.kr', 'MVP_SUBSET'),
    ('HEALTH_INSURANCE_TYPE', '건강보험 자격 구분', '국민건강보험공단', 'https://www.nhis.or.kr', 'MVP'),
    ('TAX_PAYMENT_STATUS', '세금 완납 상태', '국세청/지방세 증명서', NULL, 'MVP'),
    ('FAMILY_RELATION_TYPE', '가족 관계', '가족관계증명서', NULL, 'MVP'),
    ('INCOME_PRESENCE', '소득 여부', '사내비 내부 표준', NULL, 'MVP')
ON CONFLICT (group_code) DO UPDATE SET
    group_name = EXCLUDED.group_name,
    source_name = EXCLUDED.source_name,
    source_url = EXCLUDED.source_url,
    version_label = EXCLUDED.version_label,
    is_active = true,
    updated_at = now();

WITH seed_codes(group_code, code, code_name, parent_code, level_no, sort_order, metadata_json) AS (
    VALUES
        ('KSIC_11', '10', '식료품 제조업', NULL, 2, 1010, NULL::jsonb),
        ('KSIC_11', '47', '소매업; 자동차 제외', NULL, 2, 1047, NULL::jsonb),
        ('KSIC_11', '56', '음식점 및 주점업', NULL, 2, 1056, NULL::jsonb),
        ('KSIC_11', '70', '연구개발업', NULL, 2, 1070, NULL::jsonb),
        ('KSIC_11', '71', '전문 서비스업', NULL, 2, 1071, NULL::jsonb),
        ('KSIC_11', '85', '교육 서비스업', NULL, 2, 1085, NULL::jsonb),
        ('KSIC_11', '47911', '전자상거래 소매업', '47', 5, 47911, '{"seedScope":"MVP"}'::jsonb),
        ('KSIC_11', '56111', '한식 일반 음식점업', '56', 5, 56111, '{"seedScope":"MVP"}'::jsonb),
        ('KSIC_11', '56112', '한식 면요리 전문점', '56', 5, 56112, '{"seedScope":"MVP"}'::jsonb),
        ('KSIC_11', '56121', '중식 음식점업', '56', 5, 56121, '{"seedScope":"MVP"}'::jsonb),
        ('KSIC_11', '56122', '일식 음식점업', '56', 5, 56122, '{"seedScope":"MVP"}'::jsonb),
        ('KSIC_11', '85501', '일반 교과 학원', '85', 5, 85501, '{"seedScope":"MVP"}'::jsonb),
        ('KSIC_11', '70209', '기타 인문 및 사회과학 연구개발업', '70', 5, 70209, '{"seedScope":"MVP"}'::jsonb),
        ('KSIC_11', '71101', '변호사업', '71', 5, 71101, '{"seedScope":"MVP"}'::jsonb),
        ('NTS_BUSINESS_TYPE', 'SOLE_PROPRIETOR', '개인사업자', NULL, 1, 10, NULL::jsonb),
        ('NTS_BUSINESS_TYPE', 'CORPORATION', '법인사업자', NULL, 1, 20, NULL::jsonb),
        ('NTS_TAX_TYPE', 'GENERAL_TAXPAYER', '일반과세자', NULL, 1, 10, NULL::jsonb),
        ('NTS_TAX_TYPE', 'SIMPLIFIED_TAXPAYER', '간이과세자', NULL, 1, 20, NULL::jsonb),
        ('NTS_TAX_TYPE', 'TAX_EXEMPT', '면세사업자', NULL, 1, 30, NULL::jsonb),
        ('REGION_SIDO', 'SEOUL', '서울', NULL, 1, 10, '{"legalDongCode":"1100000000"}'::jsonb),
        ('REGION_SIDO', 'BUSAN', '부산', NULL, 1, 20, '{"legalDongCode":"2600000000"}'::jsonb),
        ('REGION_SIDO', 'DAEGU', '대구', NULL, 1, 30, '{"legalDongCode":"2700000000"}'::jsonb),
        ('REGION_SIDO', 'INCHEON', '인천', NULL, 1, 40, '{"legalDongCode":"2800000000"}'::jsonb),
        ('REGION_SIDO', 'GWANGJU', '광주', NULL, 1, 50, '{"legalDongCode":"2900000000"}'::jsonb),
        ('REGION_SIDO', 'DAEJEON', '대전', NULL, 1, 60, '{"legalDongCode":"3000000000"}'::jsonb),
        ('REGION_SIDO', 'ULSAN', '울산', NULL, 1, 70, '{"legalDongCode":"3100000000"}'::jsonb),
        ('REGION_SIDO', 'SEJONG', '세종', NULL, 1, 80, '{"legalDongCode":"3600000000"}'::jsonb),
        ('REGION_SIDO', 'GYEONGGI', '경기', NULL, 1, 90, '{"legalDongCode":"4100000000"}'::jsonb),
        ('REGION_SIDO', 'GANGWON', '강원', NULL, 1, 100, '{"legalDongCode":"5100000000"}'::jsonb),
        ('REGION_SIDO', 'CHUNGBUK', '충북', NULL, 1, 110, '{"legalDongCode":"4300000000"}'::jsonb),
        ('REGION_SIDO', 'CHUNGNAM', '충남', NULL, 1, 120, '{"legalDongCode":"4400000000"}'::jsonb),
        ('REGION_SIDO', 'JEONBUK', '전북', NULL, 1, 130, '{"legalDongCode":"5200000000"}'::jsonb),
        ('REGION_SIDO', 'JEONNAM', '전남', NULL, 1, 140, '{"legalDongCode":"4600000000"}'::jsonb),
        ('REGION_SIDO', 'GYEONGBUK', '경북', NULL, 1, 150, '{"legalDongCode":"4700000000"}'::jsonb),
        ('REGION_SIDO', 'GYEONGNAM', '경남', NULL, 1, 160, '{"legalDongCode":"4800000000"}'::jsonb),
        ('REGION_SIDO', 'JEJU', '제주', NULL, 1, 170, '{"legalDongCode":"5000000000"}'::jsonb),
        ('LEGAL_DONG', '1100000000', '서울특별시', NULL, 1, 10, NULL::jsonb),
        ('LEGAL_DONG', '2600000000', '부산광역시', NULL, 1, 20, NULL::jsonb),
        ('LEGAL_DONG', '2700000000', '대구광역시', NULL, 1, 30, NULL::jsonb),
        ('LEGAL_DONG', '2800000000', '인천광역시', NULL, 1, 40, NULL::jsonb),
        ('LEGAL_DONG', '4100000000', '경기도', NULL, 1, 90, NULL::jsonb),
        ('HEALTH_INSURANCE_TYPE', 'WORKPLACE', '직장가입자', NULL, 1, 10, NULL::jsonb),
        ('HEALTH_INSURANCE_TYPE', 'LOCAL', '지역가입자', NULL, 1, 20, NULL::jsonb),
        ('HEALTH_INSURANCE_TYPE', 'DEPENDENT', '피부양자', NULL, 1, 30, NULL::jsonb),
        ('HEALTH_INSURANCE_TYPE', 'UNKNOWN', '잘 모름', NULL, 1, 40, NULL::jsonb),
        ('TAX_PAYMENT_STATUS', 'PAID', '완납', NULL, 1, 10, NULL::jsonb),
        ('TAX_PAYMENT_STATUS', 'DELINQUENT', '체납', NULL, 1, 20, NULL::jsonb),
        ('TAX_PAYMENT_STATUS', 'UNKNOWN', '확인 필요', NULL, 1, 30, NULL::jsonb),
        ('FAMILY_RELATION_TYPE', 'SPOUSE', '배우자', NULL, 1, 10, NULL::jsonb),
        ('FAMILY_RELATION_TYPE', 'CHILD', '자녀', NULL, 1, 20, NULL::jsonb),
        ('FAMILY_RELATION_TYPE', 'PARENT', '부모', NULL, 1, 30, NULL::jsonb),
        ('INCOME_PRESENCE', 'UNKNOWN', '잘 모름', NULL, 1, 10, NULL::jsonb),
        ('INCOME_PRESENCE', 'NONE', '소득 없음', NULL, 1, 20, NULL::jsonb),
        ('INCOME_PRESENCE', 'HAS_INCOME', '소득 있음', NULL, 1, 30, NULL::jsonb)
)
INSERT INTO standard_codes (
    group_id,
    code,
    code_name,
    parent_code,
    level_no,
    sort_order,
    metadata_json
)
SELECT
    scg.id,
    seed.code,
    seed.code_name,
    seed.parent_code,
    seed.level_no,
    seed.sort_order,
    seed.metadata_json
FROM seed_codes seed
INNER JOIN standard_code_groups scg ON scg.group_code = seed.group_code
ON CONFLICT (group_id, code) DO UPDATE SET
    code_name = EXCLUDED.code_name,
    parent_code = EXCLUDED.parent_code,
    level_no = EXCLUDED.level_no,
    sort_order = EXCLUDED.sort_order,
    is_active = true,
    metadata_json = EXCLUDED.metadata_json,
    updated_at = now();

WITH field_groups(document_type_code, field_key, group_code, usage_code) AS (
    VALUES
        ('BUSINESS_REGISTRATION', 'INDUSTRY_NAME', 'KSIC_11', 'REFERENCE_MAPPING'),
        ('BUSINESS_REGISTRATION', 'BUSINESS_CATEGORY', 'KSIC_11', 'REFERENCE_MAPPING'),
        ('BUSINESS_REGISTRATION', 'BUSINESS_ITEM', 'KSIC_11', 'REFERENCE_MAPPING'),
        ('BUSINESS_REGISTRATION', 'WORKPLACE_REGION_CODE', 'REGION_SIDO', 'CONDITION_VALUE'),
        ('BUSINESS_REGISTRATION', 'WORKPLACE_ADDRESS', 'LEGAL_DONG', 'REFERENCE_MAPPING'),
        ('BUSINESS_REGISTRATION', 'BUSINESS_TYPE_CODE', 'NTS_BUSINESS_TYPE', 'CONDITION_VALUE'),
        ('BUSINESS_REGISTRATION', 'TAX_TYPE_CODE', 'NTS_TAX_TYPE', 'CONDITION_VALUE'),
        ('VAT_TAX_BASE', 'TAX_TYPE_CODE', 'NTS_TAX_TYPE', 'CONDITION_VALUE'),
        ('NATIONAL_TAX_PAID', 'TAX_PAID_STATUS', 'TAX_PAYMENT_STATUS', 'CONDITION_VALUE'),
        ('LOCAL_TAX_PAID', 'TAX_PAID_STATUS', 'TAX_PAYMENT_STATUS', 'CONDITION_VALUE'),
        ('RESIDENT_REGISTRATION', 'REGION_CODE', 'REGION_SIDO', 'CONDITION_VALUE'),
        ('RESIDENT_REGISTRATION', 'ADDRESS', 'LEGAL_DONG', 'REFERENCE_MAPPING'),
        ('FAMILY_RELATION', 'HAS_SPOUSE', 'FAMILY_RELATION_TYPE', 'REFERENCE_MAPPING'),
        ('FAMILY_RELATION', 'CHILD_COUNT', 'FAMILY_RELATION_TYPE', 'REFERENCE_MAPPING'),
        ('FAMILY_RELATION', 'PARENT_COUNT', 'FAMILY_RELATION_TYPE', 'REFERENCE_MAPPING'),
        ('HEALTH_INSURANCE_PAYMENT', 'INSURANCE_SUBSCRIBER_TYPE', 'HEALTH_INSURANCE_TYPE', 'CONDITION_VALUE'),
        ('HEALTH_INSURANCE_QUALIFICATION', 'HEALTH_INSURANCE_BASIS_CODE', 'HEALTH_INSURANCE_TYPE', 'CONDITION_VALUE'),
        ('HEALTH_INSURANCE_QUALIFICATION', 'DEPENDENT_STATUS', 'HEALTH_INSURANCE_TYPE', 'CONDITION_VALUE'),
        ('HEALTH_INSURANCE_QUALIFICATION', 'INSURED_PERSON_INFO', 'HEALTH_INSURANCE_TYPE', 'REFERENCE_MAPPING')
)
INSERT INTO standard_field_code_groups (
    standard_field_id,
    group_id,
    usage_code
)
SELECT
    sdf.id,
    scg.id,
    field_groups.usage_code
FROM field_groups
INNER JOIN standard_document_fields sdf
        ON sdf.document_type_code = field_groups.document_type_code
       AND sdf.field_key = field_groups.field_key
INNER JOIN standard_code_groups scg ON scg.group_code = field_groups.group_code
ON CONFLICT (standard_field_id, group_id) DO UPDATE SET
    usage_code = EXCLUDED.usage_code;
