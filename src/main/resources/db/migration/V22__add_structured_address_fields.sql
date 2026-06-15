ALTER TABLE member_profiles
    ADD COLUMN postal_code varchar(20),
    ADD COLUMN road_address varchar(500),
    ADD COLUMN jibun_address varchar(500),
    ADD COLUMN detail_address varchar(300),
    ADD COLUMN sido_name varchar(100),
    ADD COLUMN sigungu_name varchar(100),
    ADD COLUMN eupmyeondong_name varchar(100),
    ADD COLUMN legal_dong_code varchar(30),
    ADD COLUMN road_name_code varchar(30),
    ADD COLUMN building_management_no varchar(50),
    ADD COLUMN address_source_code varchar(30),
    ADD CONSTRAINT ck_member_profiles_address_source CHECK (
        address_source_code IS NULL
        OR address_source_code IN ('JUSO_API', 'MANUAL')
    );

ALTER TABLE business_profiles
    ADD COLUMN workplace_postal_code varchar(20),
    ADD COLUMN workplace_road_address varchar(500),
    ADD COLUMN workplace_jibun_address varchar(500),
    ADD COLUMN workplace_detail_address varchar(300),
    ADD COLUMN workplace_sido_name varchar(100),
    ADD COLUMN workplace_sigungu_name varchar(100),
    ADD COLUMN workplace_eupmyeondong_name varchar(100),
    ADD COLUMN workplace_legal_dong_code varchar(30),
    ADD COLUMN workplace_road_name_code varchar(30),
    ADD COLUMN workplace_building_management_no varchar(50),
    ADD COLUMN workplace_address_source_code varchar(30),
    ADD CONSTRAINT ck_business_profiles_workplace_address_source CHECK (
        workplace_address_source_code IS NULL
        OR workplace_address_source_code IN ('JUSO_API', 'MANUAL')
    );

CREATE INDEX ix_member_profiles_legal_dong_code
    ON member_profiles (legal_dong_code);

CREATE INDEX ix_member_profiles_sido_sigungu
    ON member_profiles (sido_name, sigungu_name);

CREATE INDEX ix_business_profiles_workplace_legal_dong_code
    ON business_profiles (workplace_legal_dong_code);

CREATE INDEX ix_business_profiles_workplace_sido_sigungu
    ON business_profiles (workplace_sido_name, workplace_sigungu_name);
