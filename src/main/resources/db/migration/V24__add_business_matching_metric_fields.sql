-- 사업자 조건 매칭에 사용할 선택 입력 수치값을 사용자 기본정보에 추가한다.
ALTER TABLE business_profiles
    ADD COLUMN employee_count integer,
    ADD COLUMN regular_employee_count integer,
    ADD COLUMN planned_hire_count integer,
    ADD COLUMN nice_credit_score integer,
    ADD COLUMN kcb_credit_score integer,
    ADD CONSTRAINT ck_business_profiles_employee_count CHECK (employee_count IS NULL OR employee_count >= 0),
    ADD CONSTRAINT ck_business_profiles_regular_employee_count CHECK (regular_employee_count IS NULL OR regular_employee_count >= 0),
    ADD CONSTRAINT ck_business_profiles_planned_hire_count CHECK (planned_hire_count IS NULL OR planned_hire_count >= 0),
    ADD CONSTRAINT ck_business_profiles_nice_credit_score CHECK (nice_credit_score IS NULL OR nice_credit_score BETWEEN 0 AND 1000),
    ADD CONSTRAINT ck_business_profiles_kcb_credit_score CHECK (kcb_credit_score IS NULL OR kcb_credit_score BETWEEN 0 AND 1000);

CREATE INDEX ix_business_profiles_employee_count
    ON business_profiles (employee_count);

CREATE INDEX ix_business_profiles_regular_employee_count
    ON business_profiles (regular_employee_count);

CREATE INDEX ix_business_profiles_planned_hire_count
    ON business_profiles (planned_hire_count);

CREATE INDEX ix_business_profiles_nice_credit_score
    ON business_profiles (nice_credit_score);

CREATE INDEX ix_business_profiles_kcb_credit_score
    ON business_profiles (kcb_credit_score);
