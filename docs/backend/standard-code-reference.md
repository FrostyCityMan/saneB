# 표준 코드 적용 기준

## 현재 단계

- 공고 입력의 업종 조건은 `announcement_industry_conditions.ksic_code`를 사용한다.
- 사용자 기본정보에는 `business_profiles.ksic_code`가 이미 존재한다.
- 업태/종목 원문은 사용자 입력값으로 보관하되, 자동 매칭 조건은 KSIC 코드 기준으로 처리한다.
- 국세청 업종코드는 세무/사업자등록 참조 코드이며, 정부지원사업 업종 조건은 우선 KSIC 기준으로 적용한다.
- `V21__create_standard_code_catalogs.sql`부터 표준 코드는 외부 API 호출 없이 PostgreSQL 정적 seed로 관리한다.

## 표준 코드 우선순위

| 대상 항목 | 현재 필드 | 적용 표준 | 처리 방향 |
|---|---|---|---|
| 업태/종목 | `business_category`, `business_item`, `industry_name`, `ksic_code` | 통계청 한국표준산업분류(KSIC), 국세청 업종코드-KSIC 연계표 | 자동 매칭은 KSIC 코드 기준. 국세청 업종코드는 추후 참조/연계 코드로 별도 저장 검토 |
| 사업장/거주지 지역 | `workplace_region_code`, `region_code`, `workplace_address` | 행정안전부 행정표준코드, 법정동코드 | MVP는 시도 단위 유지. 시군구/동 조건이 필요하면 법정동코드 저장 구조 추가 |
| 사업자 유형/과세 유형 | `business_type_code`, `tax_type_code` | 국세청 사업자등록 상태/과세 유형 기준 | 현재 내부 enum 유지. 추후 국세청 상태조회 결과와 코드 매핑 검토 |
| 국세/지방세 완납 여부 | `national_tax_delinquent`, `local_tax_delinquent`, `tax_paid_status` | 홈택스/위택스 증명서 기재값 | 현재 boolean/status 조건 유지. 발급일 유효기간 조건은 날짜 조건으로 분리 |
| 주민등록 세대 정보 | `household_member_count`, `is_householder`, `move_in_date` | 법정동코드, 주민등록등본 기재값 | 인원/날짜/지역은 자동 조건 가능. 세대원 관계 원문은 수동 확인 또는 가족 테이블로 정규화 |
| 가족관계 | `has_spouse`, `child_count`, `parent_count`, `family_member_birth_year` | 가족관계증명서 기재 관계 | MVP는 배우자/자녀/부모 1단계만 사용. 세부 관계 확장은 별도 관계 코드 테이블 필요 |
| 건강보험 가입 유형 | `health_insurance_basis_code`, `insurance_subscriber_type`, `dependent_status` | 국민건강보험 자격 구분 | 직장가입자/지역가입자/피부양자 중심 enum 유지. 공단 세부 부호는 추후 별도 코드 테이블 검토 |
| 건강보험료 | `monthly_health_insurance_premium`, `annual_health_insurance_premium` | 건강보험료 납부확인서 금액 | 금액 조건으로 자동 매칭 가능 |

## 조건 사용 상태

`standard_document_fields.condition_usage_code`는 다음 세 상태를 구분한다. 기존 `is_condition_eligible`은 호환 필드로 유지한다.

| 상태 | 의미 | 예시 |
|---|---|---|
| `INPUT_ONLY` | 입력/확인만 가능하고 조건화하지 않음 | 사업자등록번호, 대표자명, 상호명 |
| `CONDITION_READY` | 바로 자동 조건 비교 가능 | 매출액, 개업일, 지역, 자녀 수, 체납 여부 |
| `STANDARDIZATION_REQUIRED` | 조건 후보이지만 코드표/정규화가 필요 | 업태, 종목, 상세 주소, 세대원 세부 관계, 건강보험 가입자 세부 부호 |

운영 정책:

- `CONDITION_READY` 항목만 공고 수치/선택 조건의 `standard_field_id`로 저장할 수 있다.
- `STANDARDIZATION_REQUIRED` 항목은 공고 입력 화면에 노출하지만 자동 조건 저장은 차단한다.
- 업태/종목은 `STANDARDIZATION_REQUIRED` 성격이지만 업종 조건에서 KSIC 코드로 선택하면 `announcement_industry_conditions.ksic_code`로 저장한다.
- `INPUT_ONLY` 항목은 필요 서류나 동적 입력 항목으로 요청할 수 있지만 자동 조건 저장에는 사용하지 않는다.

## V21 Seed 범위

운영 migration에는 다음 code group과 MVP 대표 subset을 seed한다.

| groupCode | 내용 | Seed 범위 |
|---|---|---|
| `KSIC_11` | 한국표준산업분류 제11차 | 음식점, 소매/전자상거래, 제조, 교육, 연구개발, 전문서비스 대표 subset |
| `NTS_BUSINESS_TYPE` | 사업자 유형 | 개인사업자, 법인사업자 |
| `NTS_TAX_TYPE` | 과세 유형 | 일반과세자, 간이과세자, 면세사업자 |
| `REGION_SIDO` | 시도 | 17개 시도 전체 |
| `LEGAL_DONG` | 법정동 | MVP 대표 시도 subset |
| `HEALTH_INSURANCE_TYPE` | 건강보험 자격 구분 | 직장가입자, 지역가입자, 피부양자, 잘 모름 |
| `TAX_PAYMENT_STATUS` | 세금 완납 상태 | 완납, 체납, 확인 필요 |
| `FAMILY_RELATION_TYPE` | 가족 관계 | 배우자, 자녀, 부모 |
| `INCOME_PRESENCE` | 소득 여부 | 잘 모름, 소득 없음, 소득 있음 |

전체 KSIC/법정동/국세청 업종코드는 운영 migration에 직접 대량 삽입하지 않고, 검증된 원천 파일을 사용하는 별도 운영 import 스크립트로 분리한다.

## 후속 개발 기준

- KSIC는 업종 조건의 1차 표준으로 사용한다.
- 국세청 업종코드는 KSIC와 별도 컬럼 또는 연계 테이블로 관리한다.
- 법정동코드는 시군구/읍면동 조건이 필요한 시점에 도입한다.
- `STANDARDIZATION_REQUIRED` 항목은 관리자 화면에 노출하되, 자동 탈락 조건으로 즉시 사용하지 않는다.
- 자동 추천도, 선정확률, 점수, 우선순위 계산은 도입하지 않는다.
