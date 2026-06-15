-- 빠른 기본정보 입력에서는 대표자명, 사업 시작일, 사업장 지역, 업종만으로도 1차 후보 확인이 가능해야 한다.
ALTER TABLE business_profiles
    ALTER COLUMN business_registration_no DROP NOT NULL,
    ALTER COLUMN business_name DROP NOT NULL;
