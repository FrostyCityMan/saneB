# saneB Backend DB Model v1

> 2026-09-22 구간 분석 확장: additive **V84** 분석 이력에 이어 **V85**로 신규 엔진의 evaluation input·match를 구간 분석에 결합한다. 기존 파일 역할·evaluation·정책은 자동 변경하지 않는다. V84의 일반 Linux PostgreSQL4/4는 통과했지만 독립 QA는 실패했고 수정 후 재검증 중이다. V85도 검증 중이며 운영에는 반영하지 않았다. 아래 V83 수치는 과거 검증 이력이다. [구간 분석 DB/API 상세 계약](announcement-attachment-segment-analysis-design-2026-09-22.md)을 따른다.

> 첨부 DB 검증 기준(2026-09-16): 저장소의 최신 Flyway는 V83이다. `85c7f65`의 Linux35050057694에서 빈 DB·V71부터 V83까지 순차 업그레이드·기존 checksum·복합 FK·불변 이력·동시성 시험을 확인했다. 마지막 운영 DB 직접 확인은 **2026-09-15 16시대 V83/실패0**이며 현재 운영 재조회 결과가 아니다. 스키마 적용과 첨부 worker·정책 활성화는 구분한다. [계약 검증 근거](announcement-attachment-contract-evidence-2026-09-16.md), [운영 기준선](../deployment/attachment-runtime-baseline-2026-09-15.md), [장기 진행 기록](announcement-attachment-end-to-end-progress-2026-09-09.md)을 따른다. 기존 V26을 포함한 실제 컬럼과 제약의 source of truth는 Flyway다.

작성일: 2026-05-14

## 1. 기준 문서

- `saneB.md`
- `doc/공고입력창.txt`
- `doc/파트너 입력창.txt`
- `doc/참고.pdf`

현재 저장소에는 Spring Boot scaffold가 생성되어 있으며, 이 문서는 `src/main/resources/db/migration/V1__create_mvp_schema.sql`의 기준 계약이다.

Scaffold 상태:

- Java 21 + Spring Boot + Gradle scaffold 생성 완료
- PostgreSQL + Flyway 설정 추가 완료
- `ApiResponse`, `PageResponse` 기본 구조 생성 완료
- `/api/v1/auth/me`와 `/api/v1/dashboard/me/...` skeleton 생성 완료
- local/dev seed와 운영 migration 분리 경로 생성 완료
- 빈 PostgreSQL DB 기준 V1 migration 적용 검증은 Backend Gate에서 별도로 수행한다.

## 2. MVP 도메인 경계

### 포함

- 사용자, 역할, 인증 이력
- 회원 기본 프로필, 사업자 프로필, 가족 구성원 프로필
- 파트너 전자증명/증빙 검증
- 검증 서류 체크, 검증 제한 플래그
- 공고 등록, 승인, 상태 관리
- 공고 필수조건 저장
- 파트너 검증값 기준 매칭 케이스와 조건별 결과
- 공고별 진행 단계, 단계 문서, 단계 버튼, 사용자 진행 상태, 행동 로그
- 감사 로그와 상태 변경 이력

### 제외

- AI 자동판단
- 자동 추천
- 추천도 계산
- 우선순위 계산
- 선정확률 계산
- 가점/우대조건 계산
- 운영 secret 저장
- 프론트엔드 화면 구현

## 3. 모델링 원칙

- PostgreSQL과 Flyway migration을 schema source of truth로 둔다.
- 테이블명과 컬럼명은 `snake_case`를 사용한다.
- PK는 `uuid`를 기본으로 한다.
- FK, index, unique constraint는 migration에 명시한다.
- boolean 컬럼은 `is_`, `has_`, `can_` 접두어를 사용한다.
- 운영 migration에는 테스트 계정과 샘플 업무 데이터를 넣지 않는다.
- 검증 ID가 있는 매칭은 회원 입력값보다 파트너 전자증명/증빙 검증값을 우선한다.
- 현재 운영 흐름에서는 `matching_cases.verification_id` 없이도 기본정보 기준 후보와 최종 매칭을 분리해 생성할 수 있다.
- `matching_stage_code='BASIC'`은 사용자 기본정보 기준의 넓은 후보이고, `matching_stage_code='FINAL'`은 상담과 서류별 선택 입력 이후 관리자가 진행할 공고를 고르는 최종 후보이다.
- `matching_basis_code`는 `BASIC_INFO`, `PARTNER_INPUT`, `DOCUMENT_INPUT` 중 하나이며, 추천도·선정확률·점수·우선순위 의미를 갖지 않는다.
- 입력되지 않은 조건은 매칭에서 제외하고 결과에는 `SKIPPED`로 기록한다.
- 매칭 결과는 점수나 순위가 아니라 필수조건 통과 여부만 저장한다.
- 사용자 대시보드는 별도 저장 테이블을 만들지 않고 검증, 매칭, 진행 상태 테이블을 집계하는 읽기 모델로 제공한다.
- 화면과 운영자가 직접 식별하는 주요 업무 리소스는 내부 UUID와 별도로 `public_code`를 가진다. 내부 PK/FK는 UUID를 유지하고, 화면 노출·검색·수기 입력에는 `USR-000001`, `ANN-000001`, `MCH-000001`, `APP-000001`, `VRF-000001`, `CNS-000001` 형식의 공개 코드를 우선 사용한다.

## 4. 공통 컬럼

업무 테이블은 별도 사유가 없으면 다음 컬럼을 가진다.

| 컬럼 | 타입 | 설명 |
|---|---:|---|
| `id` | `uuid` | PK |
| `created_at` | `timestamptz` | 생성 일시 |
| `created_by` | `uuid` | 생성 사용자, nullable 가능 |
| `updated_at` | `timestamptz` | 수정 일시 |
| `updated_by` | `uuid` | 수정 사용자, nullable 가능 |

## 5. 테이블 목록

### 5.1 Users / Roles / Auth

| 테이블 | 핵심 컬럼 | PK/FK | Index / Unique |
|---|---|---|---|
| `users` | `public_code`, `login_id`, `password_hash`, `name`, `phone`, `email`, `status_code`, `password_reset_required`, `last_login_at` | PK `id` | UQ `public_code`, UQ `login_id`, UQ `phone`, IDX `status_code` |
| `roles` | `role_code`, `role_name`, `sort_order` | PK `role_code` | UQ `role_name` |
| `user_roles` | `user_id`, `role_code` | PK `(user_id, role_code)`, FK `users.id`, FK `roles.role_code` | IDX `role_code` |
| `auth_login_histories` | `user_id`, `login_id`, `login_result_code`, `ip_address`, `user_agent`, `failure_reason_code` | PK `id`, FK `users.id` nullable | IDX `(user_id, created_at)`, IDX `(login_id, created_at)` |

역할 seed:

- `USER`
- `PARTNER`
- `OPERATOR`
- `APPROVER`
- `REVIEWER`
- `ADMIN`

### 5.2 Member / Business / Family Profile

| 테이블 | 핵심 컬럼 | PK/FK | Index / Unique |
|---|---|---|---|
| `member_profiles` | `user_id`, `birth_year`, `address`, `region_code`, `postal_code`, `road_address`, `jibun_address`, `detail_address`, `sido_name`, `sigungu_name`, `eupmyeondong_name`, `legal_dong_code`, `road_name_code`, `building_management_no`, `address_source_code`, `is_householder`, `is_household_member`, `health_insurance_basis_code`, `has_income`, `income_presence_code`, `income_amount`, `income_period_code`, `income_note` | PK `id`, FK `users.id` | UQ `user_id`, IDX `region_code`, IDX `income_presence_code`, IDX `legal_dong_code`, IDX `(sido_name, sigungu_name)` |
| `business_profiles` | `user_id`, `representative_name`, `business_registration_no`, `business_name`, `workplace_address`, `workplace_region_code`, `workplace_postal_code`, `workplace_road_address`, `workplace_jibun_address`, `workplace_detail_address`, `workplace_sido_name`, `workplace_sigungu_name`, `workplace_eupmyeondong_name`, `workplace_legal_dong_code`, `workplace_road_name_code`, `workplace_building_management_no`, `workplace_address_source_code`, `opening_date`, `industry_name`, `business_category`, `business_item`, `ksic_code`, `business_type_code`, `company_stage_code`, `annual_revenue`, `annual_revenue_year`, `employee_count`, `regular_employee_count`, `planned_hire_count`, `nice_credit_score`, `kcb_credit_score`, `has_existing_loan`, `has_policy_fund_usage`, `has_guarantee_usage` | PK `id`, FK `users.id` | UQ `business_registration_no`, IDX `user_id`, IDX `ksic_code`, IDX `workplace_region_code`, IDX `annual_revenue`, IDX `employee_count`, IDX `regular_employee_count`, IDX `planned_hire_count`, IDX `nice_credit_score`, IDX `kcb_credit_score`, IDX `has_existing_loan`, IDX `workplace_legal_dong_code`, IDX `(workplace_sido_name, workplace_sigungu_name)` |
| `family_members` | `user_id`, `relation_type_code`, `birth_year`, `address`, `school_age_status_code`, `enrollment_status_code`, `is_cohabiting`, `is_supported`, `has_income`, `income_presence_code`, `income_amount`, `income_period_code`, `income_note` | PK `id`, FK `users.id` | IDX `(user_id, relation_type_code)`, IDX `(user_id, relation_type_code, income_presence_code)` |
| `member_interview_responses` | `member_user_id`, `question_code`, `answer_code`, `note` | PK `id`, FK `users.id` | UQ `(member_user_id, question_code)`, IDX `member_user_id`, IDX `(question_code, answer_code)` |
| `member_document_input_values` | `user_id`, `standard_field_id`, `value_text`, `value_number`, `value_date`, `value_boolean`, `submitted_by`, `submitted_at` | PK `id`, FK `users.id`, FK `standard_document_fields.id`, FK `users.id` | UQ `(user_id, standard_field_id)`, IDX `(user_id, updated_at DESC)`, IDX `standard_field_id` |

MVP에서는 회원이 입력한 정보와 파트너가 검증한 정보를 분리한다. 현재 운영 테스트에서는 검증값 없이도 운영자 수동 매칭을 생성할 수 있으며, 검증 ID가 있는 경우에는 파트너 검증값을 회원 입력값보다 우선 사용한다.
사용자 기본정보 입력 하단의 서류별 선택 입력값은 `member_document_input_values`에 저장한다. 한 표준 필드에는 문자, 숫자, 날짜, boolean 중 한 값만 저장하며 모든 서류 값은 선택 입력이다.
`V23__relax_business_profile_minimal_fields.sql` 이후 `business_registration_no`, `business_name`은 빠른 기본정보 입력을 위해 선택값이다. 사업자등록번호를 입력한 경우 기존 unique constraint는 유지된다.
`V24__add_business_matching_metric_fields.sql` 이후 직원 수, 상시근로자 수, 신규 채용 예정 인원, NICE/KCB 신용 점수는 기본정보 선택 입력값으로 저장되며, 공고 수치 조건 매칭에 사용할 수 있다.
`V25__add_member_interview_responses.sql` 이후 기대출 여부는 `business_profiles.has_existing_loan`에 저장하고, 기존 동일 사업 진행 여부·중복 지원 여부·실제 사업 운영 여부·기타 제한 여부는 `member_interview_responses`에 선택 응답으로 저장한다. NICE/KCB와 간단 인터뷰는 외부 자동조회가 아니라 사용자 또는 운영자 수동 입력값이다.
관리자가 회원을 대신해 서류별 선택 입력값을 저장하는 경우에도 `user_id`는 대상 회원 ID를 유지하고, `submitted_by`에 입력 관리자 ID를 기록한다.
`V22__add_structured_address_fields.sql`은 행정안전부 도로명주소 검색 결과의 우편번호, 도로명주소, 지번주소, 법정동코드, 도로명코드, 건물관리번호를 회원 거주지와 사업장 주소에 additive로 저장한다. 기존 `region_code`, `workplace_region_code`는 시도 단위 매칭 코드로 유지하며, `address_source_code`, `workplace_address_source_code`는 `JUSO_API`, `MANUAL`만 허용한다.

### 5.3 Partner Verification

| 테이블 | 핵심 컬럼 | PK/FK | Index / Unique |
|---|---|---|---|
| `partner_profiles` | `user_id`, `partner_name`, `business_registration_no`, `status_code` | PK `id`, FK `users.id` | UQ `user_id`, UQ `business_registration_no`, IDX `status_code` |
| `partner_verifications` | `public_code`, `member_user_id`, `partner_user_id`, `business_profile_id`, `status_code`, `is_current`, `is_matching_blocked`, `submitted_at`, `verified_at`, `reviewed_by`, `review_note` | PK `id`, FK `users.id`, FK `business_profiles.id` | UQ `public_code`, Partial UQ `(member_user_id) WHERE is_current = true`, IDX `(partner_user_id, status_code)`, IDX `(member_user_id, status_code)` |
| `verification_member_values` | `verification_id`, `birth_year`, `address`, `region_code`, `is_householder`, `is_household_member`, `health_insurance_basis_code`, `has_income` | PK `id`, FK `partner_verifications.id` | UQ `verification_id`, IDX `region_code` |
| `verification_business_values` | `verification_id`, `annual_revenue`, `employee_count`, `regular_employee_count`, `tax_status_code`, `nice_credit_score`, `kcb_credit_score`, `has_existing_loan`, `has_policy_fund_usage`, `has_guarantee_usage`, `financial_checked_on` | PK `id`, FK `partner_verifications.id` | UQ `verification_id` |
| `verification_family_values` | `verification_id`, `relation_type_code`, `birth_year`, `address`, `school_age_status_code`, `enrollment_status_code`, `is_cohabiting`, `is_supported`, `has_income` | PK `id`, FK `partner_verifications.id` | IDX `(verification_id, relation_type_code)` |
| `verification_restriction_flags` | `verification_id`, `restriction_code`, `is_checked`, `note` | PK `id`, FK `partner_verifications.id` | UQ `(verification_id, restriction_code)` |

제한 플래그가 하나라도 `is_checked = true`이면 매칭 케이스는 `BLOCKED` 또는 `REVIEW_REQUIRED`로 분류한다.

### 5.4 Verification Documents

| 테이블 | 핵심 컬럼 | PK/FK | Index / Unique |
|---|---|---|---|
| `verification_documents` | `verification_id`, `document_type_code`, `source_type_code`, `is_checked`, `checked_by`, `checked_at`, `note` | PK `id`, FK `partner_verifications.id`, FK `users.id` | UQ `(verification_id, document_type_code)`, IDX `(verification_id, is_checked)` |

기본 서류 코드:

- `BUSINESS_REGISTRATION`
- `VAT_TAX_BASE`
- `TAX_EXEMPT_INCOME`
- `INCOME_CERTIFICATE`
- `NATIONAL_TAX_PAID`
- `LOCAL_TAX_PAID`
- `RESIDENT_REGISTRATION`
- `FAMILY_RELATION`
- `HEALTH_INSURANCE_PAYMENT`
- `HEALTH_INSURANCE_QUALIFICATION`

### 5.5 Announcements

| 테이블 | 핵심 컬럼 | PK/FK | Index / Unique |
|---|---|---|---|
| `announcements` | `public_code`, `target_type_code`, `title`, `agency_name`, `summary`, `application_start_date`, `application_end_date`, `manual_status_code`, `approval_status_code`, `income_judgement_code`, `min_amount`, `max_amount`, `created_by`, `updated_by` | PK `id`, FK `users.id` | UQ `public_code`, UQ `(agency_name, title, application_start_date)`, IDX `(target_type_code, application_start_date, application_end_date)`, IDX `(manual_status_code, approval_status_code)` |
| `announcement_options` | `announcement_id`, `option_group_code`, `option_code` | PK `id`, FK `announcements.id` | UQ `(announcement_id, option_group_code, option_code)` |
| `announcement_approval_requests` | `announcement_id`, `requested_by`, `decided_by`, `approval_status_code`, `request_note`, `decision_note`, `requested_at`, `decided_at` | PK `id`, FK `announcements.id`, FK `users.id` | IDX `(announcement_id, approval_status_code)`, IDX `(requested_by, requested_at)` |
| `announcement_status_histories` | `announcement_id`, `before_status_code`, `after_status_code`, `reason`, `changed_by`, `changed_at` | PK `id`, FK `announcements.id`, FK `users.id` | IDX `(announcement_id, changed_at)` |

자동 상태는 별도 저장 컬럼이 아니라 `application_start_date`, `application_end_date`, 기준일로 조회 시 계산한다. 계산값은 `UPCOMING`(모집예정), `OPEN`(접수중), `CLOSING_SOON`(마감임박), `ENDED`(종료)이다. 수동 상태가 `NORMAL`이 아니면 `manual_status_code`가 최종 노출 상태로 우선 적용되고, `NORMAL`이면 자동 상태가 최종 노출 상태가 된다.

### 5.6 Announcement Conditions

| 테이블 | 핵심 컬럼 | PK/FK | Index / Unique |
|---|---|---|---|
| `announcement_industry_conditions` | `announcement_id`, `condition_type_code`, `ksic_code` | PK `id`, FK `announcements.id` | UQ `(announcement_id, condition_type_code, ksic_code)` |
| `announcement_numeric_conditions` | `announcement_id`, nullable `standard_field_id`, `condition_scope_code`, `condition_key`, `comparator_code`, `value_number`, `min_number`, `max_number`, `unit_code` | PK `id`, FK `announcements.id`, FK `standard_document_fields.id` | UQ `(announcement_id, condition_scope_code, condition_key)`, IDX `(condition_scope_code, condition_key)` |
| `announcement_option_conditions` | `announcement_id`, nullable `standard_field_id`, `condition_scope_code`, `condition_key`, `option_code`, `option_text` | PK `id`, FK `announcements.id`, FK `standard_document_fields.id` | UQ `(announcement_id, condition_scope_code, condition_key, option_code)`, IDX `(condition_scope_code, condition_key)` |
| `announcement_document_requirements` | `announcement_id`, nullable `standard_field_id`, `document_type_code`, `is_required`, `sort_order` | PK `id`, FK `announcements.id`, FK `standard_document_fields.id` | UQ `(announcement_id, document_type_code)` |

조건 scope:

- `BUSINESS`
- `PERSONAL`
- `SPOUSE`
- `CHILD`
- `PARENT`
- `APPLICATION`
- `SUPPORT`

수치 조건은 `comparator_code`와 값을 함께 저장한다. `BETWEEN`은 `min_number`, `max_number`를 사용한다.

### 5.7 Matching Cases / Results

| 테이블 | 핵심 컬럼 | PK/FK | Index / Unique |
|---|---|---|---|
| `matching_cases` | `public_code`, `announcement_id`, `member_user_id`, nullable `verification_id`, `status_code`, `blocked_reason_code`, `matching_stage_code`, `matching_basis_code`, `matched_at`, `reviewed_by`, `reviewed_at` | PK `id`, FK `announcements.id`, FK `users.id`, FK `partner_verifications.id` | UQ `public_code`, UQ `(announcement_id, member_user_id, verification_id)`, partial UQ `(announcement_id, member_user_id, matching_stage_code) WHERE verification_id IS NULL`, IDX `(member_user_id, matching_stage_code, status_code)`, IDX `(matching_stage_code, status_code, matched_at)` |
| `matching_result_details` | `matching_case_id`, `condition_scope_code`, `condition_key`, `result_code`, `basis_value`, `required_value`, `reason` | PK `id`, FK `matching_cases.id` | UQ `(matching_case_id, condition_scope_code, condition_key)`, IDX `(result_code)` |

공고와 회원은 다대다 관계다. `matching_cases`는 `announcements`와 `users` 사이의 매칭 관계 엔티티이며, 한 회원은 여러 공고 후보를 가질 수 있고 한 공고는 여러 회원 후보를 가질 수 있다. unique 제약은 같은 공고와 같은 회원의 동일 검증 기준 후보 중복 생성을 막기 위한 장치이며, 공고 또는 회원 단위의 일대일 관계를 의미하지 않는다.

매칭은 `approval_status_code = APPROVED`이고 신청 기간이 유효한 공고를 기준으로 수행한다. `BASIC` 후보는 사용자 기본정보 저장 후 자동 생성되며 사용자가 대시보드에서 넓은 후보를 확인하는 용도다. `FINAL` 후보는 구독, 상담 요청, 서류별 선택 입력 이후 관리자가 최종 재계산하며, 관리자 매칭 화면 기본 목록에는 `FINAL + MATCHED`만 노출한다. 신청 진행 생성은 `FINAL + MATCHED` 매칭만 허용한다.

### 5.8 Progress Steps / Logs

| 테이블 | 핵심 컬럼 | PK/FK | Index / Unique |
|---|---|---|---|
| `announcement_progress_steps` | `announcement_id`, `step_order`, `step_name`, `guide_message`, `action_guide`, `completion_condition_code`, `next_condition_code`, `is_active` | PK `id`, FK `announcements.id` | UQ `(announcement_id, step_order)`, IDX `(announcement_id, is_active)` |
| `announcement_step_documents` | `step_id`, `document_type_code`, `is_required`, `sort_order` | PK `id`, FK `announcement_progress_steps.id` | UQ `(step_id, document_type_code)` |
| `announcement_step_buttons` | `step_id`, `button_code`, `button_label`, `button_action_code`, `next_step_id`, `sort_order` | PK `id`, FK `announcement_progress_steps.id` | UQ `(step_id, button_code)` |
| `application_progresses` | `public_code`, `matching_case_id`, `announcement_id`, `member_user_id`, `current_step_id`, `status_code`, `receipt_no`, `receipt_date`, `result_code`, `result_note`, `result_date` | PK `id`, FK `matching_cases.id`, FK `announcements.id`, FK `users.id`, FK `announcement_progress_steps.id` | UQ `public_code`, UQ `matching_case_id`, IDX `(member_user_id, status_code)`, IDX `(announcement_id, status_code)` |
| `application_step_states` | `progress_id`, `step_id`, `status_code`, `started_at`, `completed_at` | PK `id`, FK `application_progresses.id`, FK `announcement_progress_steps.id` | UQ `(progress_id, step_id)` |
| `application_action_logs` | `progress_id`, `step_id`, `actor_user_id`, `action_code`, `button_code`, `input_json` | PK `id`, FK `application_progresses.id`, FK `announcement_progress_steps.id`, FK `users.id` | IDX `(progress_id, created_at)`, IDX `(actor_user_id, created_at)` |
| `application_step_checklists` | `progress_id`, `step_document_id`, `is_checked`, `checked_at`, `checked_by` | PK `id`, FK `application_progresses.id`, FK `announcement_step_documents.id`, FK `users.id` | UQ `(progress_id, step_document_id)` |
| `progress_reminder_logs` | `progress_id`, `step_id`, `reminder_type_code`, `attempt_no`, `scheduled_at`, `sent_at`, `result_code` | PK `id`, FK `application_progresses.id`, FK `announcement_progress_steps.id` | UQ `(progress_id, reminder_type_code)`, IDX `(progress_id, scheduled_at)`, IDX `(result_code)` |

진행 단계는 사용자의 단일 행동 완료를 중심으로 설계한다. 완료 조건 충족 전 다음 단계 이동은 서버에서 차단한다. `completion_condition_code`는 `BUTTON_CLICK`, `ALL_REQUIRED_DOCUMENTS_CHECKED`, `REQUIRED_INPUTS_SAVED`, `RECEIPT_SAVED`, `RESULT_SAVED`를 기본 계약으로 사용한다. 기존 호환 코드인 `DOCUMENT_SUBMITTED`, `STATUS_CONFIRMED`은 조회 호환만 유지한다. 버튼 행동은 `MOVE_NEXT`, `COMPLETE_STEP`, `STOP_PROGRESS`를 사용한다.
24시간/48시간/마감 2일 전/7일/14일 미진행 분류는 `progress_reminder_logs`로 중복 발송을 차단한다. 사용자가 단계 문서 또는 공고별 입력값을 저장하면 `application_progresses.updated_at`을 갱신해 이후 미진행 기준 시간이 다시 계산된다. 상시 접수 또는 마감일 미입력 공고는 마감 2일 전 리마인드 대상에서 제외한다.

### 5.9 Audit / Status Histories

| 테이블 | 핵심 컬럼 | PK/FK | Index / Unique |
|---|---|---|---|
| `audit_logs` | `actor_user_id`, `action_code`, `resource_type`, `resource_id`, `result_code`, `ip_address`, `user_agent`, `metadata_json` | PK `id`, FK `users.id` nullable | IDX `(actor_user_id, created_at)`, IDX `(resource_type, resource_id)`, IDX `created_at` |

개인정보 원문과 secret은 `audit_logs.metadata_json`에 저장하지 않는다. metadata는 비식별 값, 코드, 해시, 처리 결과 중심으로 제한한다.

### 5.10 Consents

| 테이블 | 핵심 컬럼 | PK/FK | Index / Unique |
|---|---|---|---|
| `consent_versions` | `consent_code`, `consent_name`, `version_no`, `is_required`, `effective_from`, `effective_to`, `content_hash` | PK `id` | UQ `(consent_code, version_no)`, partial UQ current `(consent_code) WHERE effective_to IS NULL`, IDX `(consent_code, effective_from)` |
| `user_consents` | `user_id`, `consent_version_id`, `consent_code`, `is_consented`, `consented_at`, `ip_address`, `user_agent` | PK `id`, FK `users.id`, FK `consent_versions.id` | IDX `(user_id, consent_code, consented_at)`, IDX `consent_version_id` |

동의 이력은 운영 감사 로그와 분리한다. `ip_address`, `user_agent`는 동의 증적용으로만 저장하며, 외부 API 응답 원문이나 개인정보 원문은 저장하지 않는다.

### 5.11 Stored Files / Document Submissions

| 테이블 | 핵심 컬럼 | PK/FK | Index / Unique |
|---|---|---|---|
| `stored_files` | `owner_user_id`, `original_filename`, `stored_filename`, `storage_key`, `content_type`, `file_size`, `checksum_sha256`, `status_code` | PK `id`, FK `users.id` | UQ `storage_key`, IDX `(owner_user_id, created_at)`, IDX `(status_code, created_at)` |
| `document_submissions` | `file_id`, `submitted_by`, `resource_type_code`, `resource_id`, `document_type_code`, `status_code`, `review_note`, `reviewed_by`, `reviewed_at` | PK `id`, FK `stored_files.id`, FK `users.id` | IDX `(resource_type_code, resource_id, created_at)`, IDX `(submitted_by, status_code, created_at)`, IDX `file_id` |
| `document_submission_reviews` | `submission_id`, `reviewer_user_id`, `before_status_code`, `after_status_code`, `review_note` | PK `id`, FK `document_submissions.id`, FK `users.id` | IDX `(submission_id, created_at)`, IDX `(reviewer_user_id, created_at)` |

파일 원문은 DB에 저장하지 않는다. `stored_files.storage_key`는 `STORAGE_ROOT` 하위 상대 경로이며 공개 URL이 아니다. `document_submissions.resource_id`는 검증 건 또는 신청 진행 건을 가리키는 업무 ID이고, 서비스 계층에서 접근 권한과 존재 여부를 검증한다.

### 5.12 Consultation Reservations

| 테이블 | 핵심 컬럼 | PK/FK | Index / Unique |
|---|---|---|---|
| `partner_availability_slots` | `partner_user_id`, `start_at`, `end_at`, `status_code`, `note` | PK `id`, FK `users.id` | UQ `(partner_user_id, start_at, end_at)`, IDX `(partner_user_id, start_at)`, IDX `(status_code, start_at)` |
| `consultation_reservations` | `public_code`, nullable `slot_id`, `member_user_id`, nullable `partner_user_id`, `progress_id`, `verification_id`, `status_code`, `request_note`, `status_note` | PK `id`, FK `partner_availability_slots.id`, FK `users.id`, FK `application_progresses.id`, FK `partner_verifications.id` | UQ `public_code`, partial UQ active `slot_id`, IDX `(member_user_id, status_code, created_at)`, IDX `(partner_user_id, status_code, created_at)` |
| `consultation_histories` | `reservation_id`, `actor_user_id`, `before_status_code`, `after_status_code`, `note` | PK `id`, FK `consultation_reservations.id`, FK `users.id` | IDX `(reservation_id, created_at)`, IDX `(actor_user_id, created_at)` |

MVP 상담은 자동 예약이 아니라 수기 배정 방식이다. 일반 사용자는 `slot_id`와 `partner_user_id` 없이 `REQUESTED` 상태로 상담 요청을 접수할 수 있고, 운영자 또는 관리자가 담당자와 시간을 배정하면 `ASSIGNED` 상태로 전환한다. 상담 예약 취소/배정/확정/완료 상태 변경은 `consultation_histories`에 남긴다. 상담 메모에는 상담에 필요한 최소 내용만 저장하며, 감사 로그 metadata에는 개인정보 원문을 저장하지 않는다.

### 5.13 Subscription / Payment

| 테이블 | 핵심 컬럼 | PK/FK | Index / Unique |
|---|---|---|---|
| `subscription_plans` | `plan_code`, `plan_name`, `billing_cycle_code`, `price_amount`, `currency_code`, `is_active`, `sort_order` | PK `id`, FK `users.id` 감사 컬럼 | UQ `plan_code`, IDX `(is_active, sort_order, plan_code)` |
| `user_subscriptions` | `user_id`, `plan_id`, `status_code`, `current_period_start`, `current_period_end`, `canceled_at`, `cancel_reason` | PK `id`, FK `users.id`, FK `subscription_plans.id` | partial UQ current `(user_id)`, IDX `(user_id, status_code)`, IDX `(plan_id, status_code)` |
| `payment_transactions` | `subscription_id`, `user_id`, `plan_id`, `provider_code`, `merchant_uid`, `provider_payment_key`, `status_code`, `amount`, `currency_code` | PK `id`, FK `user_subscriptions.id`, FK `users.id`, FK `subscription_plans.id` | UQ `merchant_uid`, partial UQ `(provider_code, provider_payment_key)`, IDX `(user_id, status_code)` |
| `refund_transactions` | `payment_id`, `user_id`, `provider_code`, `provider_refund_key`, `status_code`, `refund_amount`, `reason`, `requested_by` | PK `id`, FK `payment_transactions.id`, FK `users.id` | partial UQ `(provider_code, provider_refund_key)`, IDX `(payment_id, status_code)`, IDX `(user_id, status_code)` |
| `payment_provider_events` | `provider_code`, `provider_event_id`, `event_type_code`, `payment_id`, `refund_id`, `result_code`, `metadata_json` | PK `id`, FK `payment_transactions.id`, FK `refund_transactions.id` | UQ `(provider_code, provider_event_id)`, IDX `(payment_id, received_at)`, IDX `(refund_id, received_at)` |

PG사는 TossPayments를 우선 기준으로 둔다. DB 계약은 `provider_code='TOSS'`를 허용하되 TossPayments 운영 key, webhook secret, redirect URL은 환경변수와 운영 설정으로만 관리한다. 결제사 webhook 원문 payload와 secret은 DB에 저장하지 않는다. `payment_provider_events.metadata_json`에는 event type, 실패 코드 존재 여부, 금액 제공 여부 같은 비식별 metadata만 저장한다.

### 5.14 Notifications / Operation Tasks

| 테이블 | 핵심 컬럼 | PK/FK | Index / Unique |
|---|---|---|---|
| `notification_templates` | `template_code`, `channel_code`, `title_template`, `body_template`, `is_active` | PK `id`, FK `users.id` 감사 컬럼 | UQ `(template_code, channel_code)`, IDX `(is_active, template_code)` |
| `notification_messages` | `recipient_user_id`, `template_id`, `channel_code`, `title`, `body`, `status_code`, `resource_type`, `resource_id`, `read_at`, `sent_at` | PK `id`, FK `users.id`, FK `notification_templates.id` | IDX `(recipient_user_id, created_at)`, IDX `(recipient_user_id, read_at)`, IDX `(resource_type, resource_id)` |
| `notification_delivery_logs` | `message_id`, `channel_code`, `provider_code`, `delivery_status_code`, `attempt_no`, `provider_message_key`, `failure_code`, `failure_message` | PK `id`, FK `notification_messages.id` | IDX `(message_id, created_at)`, IDX `(delivery_status_code, created_at)` |
| `operation_tasks` | `task_type_code`, `status_code`, `priority_code`, `title`, `description`, `resource_type`, `resource_id`, `due_at`, `completed_at` | PK `id`, FK `users.id` 감사 컬럼 | IDX `(status_code, due_at)`, IDX `(resource_type, resource_id)`, IDX `(task_type_code, status_code)` |
| `operation_task_comments` | `task_id`, `author_user_id`, `comment_text` | PK `id`, FK `operation_tasks.id`, FK `users.id` | IDX `(task_id, created_at)` |
| `operation_task_assignments` | `task_id`, `assignee_user_id`, `status_code`, `assigned_by`, `assigned_at`, `completed_at` | PK `id`, FK `operation_tasks.id`, FK `users.id` | UQ `(task_id, assignee_user_id)`, IDX `(assignee_user_id, status_code)` |

인앱 알림은 `/app/notifications`에서 사용자에게 노출한다. 외부 이메일/SMS/카카오 provider는 연결하지 않으며, `IN_APP` 알림만 즉시 `SENT`로 저장한다. 장기 미진행과 TM 재접촉은 `operation_tasks`에 함께 적재하여 `/app/operation-tasks`에서 운영자가 처리한다. 6개월 정보 재확인은 `notification_messages`에만 남기고 별도 운영 업무는 생성하지 않는다.

외부 알림 provider payload 원문은 저장하지 않는다. `notification_delivery_logs.metadata_json`과 `audit_logs.metadata_json`에는 channel, resource type, provider 설정 여부 같은 비식별 metadata만 저장한다.

### 5.15 Admin Reports

| 테이블 | 핵심 컬럼 | PK/FK | Index / Unique |
|---|---|---|---|
| `report_exports` | `report_type_code`, `format_code`, `status_code`, `requested_by`, `row_count`, `file_name`, `content_text`, `completed_at` | PK `id`, FK `users.id` | IDX `requested_at`, IDX `(requested_by, status_code)` |
| `admin_report_snapshots` | `snapshot_type_code`, `snapshot_json`, `created_by` | PK `id`, FK `users.id` | IDX `(snapshot_type_code, created_at)` |

관리자 리포트 snapshot은 집계 수치만 저장한다. 사용자명, 연락처, 결제사 원문 payload, 파일 원문은 snapshot과 export content에 포함하지 않는다.

### 5.16 AI Assist

| 테이블 | 핵심 컬럼 | PK/FK | Index / Unique |
|---|---|---|---|
| `ai_assist_requests` | `assist_type_code`, `resource_type`, `resource_id`, `input_hash_sha256`, `input_length`, `requested_by`, `status_code`, `provider_code`, `model_code`, `completed_at` | PK `id`, FK `users.id` | IDX `(requested_by, status_code, created_at)`, IDX `(resource_type, resource_id)`, IDX `(assist_type_code, created_at)` |
| `ai_assist_results` | `request_id`, `result_text`, `review_status_code`, `prompt_token_count`, `completion_token_count`, `latency_ms`, `metadata_json`, `reviewed_by`, `reviewed_at` | PK `id`, FK `ai_assist_requests.id`, FK `users.id` | UQ `request_id`, IDX `(review_status_code, created_at)` |

AI 보조 입력 원문은 DB에 저장하지 않는다. `ai_assist_requests.input_hash_sha256`과 `input_length`만 저장하며, `ai_assist_results.result_text`는 운영자 검토용 초안이다. 외부 provider payload 원문과 secret은 저장하지 않는다.

### 5.17 Announcement Source Collection

| 테이블 | 핵심 컬럼 | PK/FK | Index / Unique |
|---|---|---|---|
| `announcement_source_collection_requests` | `public_code`, `request_type_code`, `provider_code`, `request_status_code`, `search_keyword`, `search_region_code`, `search_category_code`, `max_count`, `requested_by`, `requested_at`, `approved_by`, `approved_at`, `approval_note` | PK `id`, FK `users.id` | UQ `public_code`, IDX `(request_status_code, requested_at)`, IDX `(provider_code, requested_at)` |
| `announcement_source_collection_runs` | `public_code`, `request_id`, `provider_code`, `run_status_code`, `total_count`, `collected_count`, `duplicate_count`, `skipped_ended_count`, `failed_count`, `excluded_count`, `started_at`, `finished_at`, `error_message` | PK `id`, FK `announcement_source_collection_requests.id` | UQ `public_code`, IDX `(provider_code, started_at)`, IDX `(run_status_code, started_at)` |
| `announcement_source_collection_run_items` | `run_id`, `source_snapshot_id`, `provider_notice_id`, `source_url`, `item_status_code`, `semantic_reason_code`, `semantic_matched_keywords`, `error_message` | PK `id`, FK `announcement_source_collection_runs.id`, FK `announcement_source_snapshots.id` | IDX `(run_id, item_status_code)` |
| `announcement_source_snapshots` | `public_code`, `provider_code`, `provider_notice_id`, `title`, `agency_name`, `application_start_date`, `application_end_date`, `posted_date`, `modified_date`, `source_url`, `body_text`, `inquiry_text`, `application_method_text`, `raw_payload_json`, `raw_hash`, `review_status_code`, `semantic_status_code`, `semantic_reason_code`, `semantic_matched_keywords`, `reviewed_by`, `reviewed_at` | PK `id`, FK `users.id` | UQ `public_code`, partial UQ `(provider_code, provider_notice_id)`, partial UQ `(provider_code, source_url)`, UQ `(provider_code, raw_hash)`, IDX `(review_status_code, created_at)`, IDX `(application_end_date)`, IDX `(provider_code, semantic_status_code, review_status_code, collected_at)` |
| `announcement_source_attachments` | `source_snapshot_id`, `attachment_name`, `attachment_url`, `sort_order` | PK `id`, FK `announcement_source_snapshots.id` | IDX `(source_snapshot_id, sort_order)` |
| `announcement_source_highlights` | `source_snapshot_id`, `highlight_type_code`, `highlight_text`, `start_offset`, `end_offset`, `sort_order` | PK `id`, FK `announcement_source_snapshots.id` | IDX `(source_snapshot_id, highlight_type_code)` |
| `announcement_source_duplicate_candidates` | `source_snapshot_id`, `announcement_id`, `match_type_code`, `title_matched`, `agency_matched`, `provider_notice_matched`, `period_matched`, `source_url_matched`, `similarity_reason`, `decision_status_code`, `decided_by`, `decided_at`, `decision_note` | PK `id`, FK `announcement_source_snapshots.id`, FK `announcements.id`, FK `users.id` | UQ `(source_snapshot_id, announcement_id)`, IDX `(source_snapshot_id, match_type_code, decision_status_code)`, IDX `(announcement_id, decision_status_code)` |
| `announcement_source_review_histories` | `source_snapshot_id`, `before_status_code`, `after_status_code`, `review_note`, `changed_by`, `changed_at` | PK `id`, FK `announcement_source_snapshots.id`, FK `users.id` | IDX `(source_snapshot_id, changed_at)` |
| `announcement_source_links` | `source_id`, `announcement_id`, `linked_by`, `linked_at` | PK `id`, FK `announcement_source_snapshots.id`, FK `announcements.id`, FK `users.id` | UQ `source_id`, IDX `announcement_id` |
| `announcement_source_exclusion_tombstones` | `provider_code`, `identity_hash`, `last_raw_hash`, `run_id`, `rule_release_id`, `semantic_reason_code`, `title_stage_code`, `body_stage_code`, `decision_source_code`, `occurrence_count`, `first_excluded_at`, `last_seen_at` | PK `id`, FK `announcement_source_collection_runs.id`, FK `announcement_source_classification_rule_releases.id` | UQ `(provider_code, identity_hash)`, IDX `(provider_code, last_seen_at)` |
| `announcement_source_exclusion_rule_matches` | `exclusion_id`, `keyword_rule_id`, `keyword_term_id`, `match_location_code`, `applied_action_code` | PK `id`, FK tombstone·rule·term | UQ `(exclusion_id, keyword_rule_id, keyword_term_id, match_location_code, applied_action_code)` |

외부 공고 중 `ACCEPTED`와 `REVIEW_REQUIRED` 원문은 `announcement_source_snapshots`에 보존하고, 실제 매칭에 사용하는 운영 공고는 기존 `announcements`와 조건 테이블에 운영자가 별도로 입력한다. 제목 단계 `EXCLUDED` 공고는 snapshot을 생성하지 않고 비가역 tombstone과 규칙 참조만 남긴다. 하이라이트는 검수 참고용이며 `announcement_numeric_conditions`, `announcement_option_conditions`, `announcement_industry_conditions`에 자동 저장하지 않는다.

수집 실행은 `announcement_source_collection_requests.request_status_code='APPROVED'`인 요청만 허용한다. 배치와 버튼 실행은 모두 먼저 요청을 만들고 승인 후 실행하는 동일한 절차를 따른다. 종료된 과거 공고는 `announcement_source_collection_run_items.item_status_code='SKIPPED_ENDED'`로 기록하고 사용자 매칭 대상 운영 공고로 전환하지 않는다.

신규 수집 원문은 운영 공고 전환 전에 기존 활성 공고와 자동 비교한다. 비교 기준은 사업명, 주관기관, provider 공고번호, 신청기간, 원문 URL이다. 동일 공고는 `match_type_code='EXACT_DUPLICATE'`, 유사 공고는 `match_type_code='SIMILAR'`로 `announcement_source_duplicate_candidates`에 저장한다. 보류 후보가 있으면 신규 운영 공고 DRAFT 생성은 차단되며, 운영자가 `CREATE_NEW_SELECTED`, `UPDATE_EXISTING_SELECTED`, `IGNORED` 중 하나를 결정해야 한다. 하이라이트와 마찬가지로 중복 후보도 검수 보조 데이터이며 매칭 조건으로 자동 저장하지 않는다.

### 5.18 Local Government Notice Collection

| 테이블 | 역할 | 주요 제약 |
|---|---|---|
| `local_government_notice_sources` | 시·도, 시·군·구, 기관, 사용자용 URL, 선택적 수집 endpoint, HTTP 호환 프로필, 게시판 유형, 수집 정책, 의미 검증 근거, ON/OFF, 마지막 수집 상태 관리 | `public_code` unique, active `(sigungu_code, notice_url)` unique, 파서·URL·게시판 의미 검증 완료 및 `collection_policy_code != 'EXCLUDED'`인 출처만 ON |
| `local_government_notice_parser_profiles` | CSS selector 기반 HTML 파서, 제한형 링크 탐색, 검증된 JSON 필드 매핑, 안전 링크 템플릿 | `profile_code` unique, 허용 placeholder와 리터럴 함수 인자만 사용, 임의 스크립트·동적 표현식 저장 금지 |
| `announcement_source_semantic_keyword_rules` | 일반 공지 게시물의 포함·제외 정적 키워드 | `rule_code` unique, `(rule_type_code, keyword_text)` unique, `INCLUDE`/`EXCLUDE`만 허용 |
| `announcement_source_collection_source_results` | 수집 실행의 URL별 성공·신규·중복·실패·제외 결과 | `(run_id, local_government_source_id)` unique |
| `announcement_source_snapshot_duplicates` | 기업마당·정부24·지자체 원문 간 정확·유사 중복 관계 | UUID canonical 순서 check, `(source_id, candidate_source_id)` unique |
| `announcement_source_collection_schedules` | 최초 승인 후 자동 실행되는 정기 수집 일정 | 승인·중지·반려·만료 상태, 다음 실행 시각 index |
| `announcement_source_schedule_executions` | 동일 예정시각 중복 실행 방지 | `(schedule_id, scheduled_for)` unique |

지자체 provider code는 `LOCAL_GOV_NOTICE`다. V29는 검토 대상 244개 고유 행정구역 URL을 모두 OFF로 seed하며, 실행 가능한 파서를 운영자가 검증한 URL만 개별 ON 처리한다. “226개”는 코드나 DB 제약으로 고정하지 않는다.

V30은 상세 URL 패턴, 동일 기관 host, 반복 목록 컨테이너, 인접 등록일을 모두 확인하는 `HEURISTIC_NOTICE`를 추가한다. V31은 2026-07-10 전수 QA에서 통과한 142곳에 검증 파서를 지정한다. V32는 검증된 16개 목록 URL을 보정하고 `DEFAULT`, `BROWSER_HTTP1` 요청 정책과 선택적 JSON 수집 endpoint를 추가한다. `GENERIC_JSON`은 DB에 고정된 목록 경로·제목·등록일·링크 식별자·동일 기관 링크 template만 사용한다. JavaScript 실행, TLS 검증 우회, 응답 원문 전체의 매칭 조건 자동 반영은 허용하지 않는다.

V33은 2026-07-13 전수 QA와 춘천시 JSON endpoint 검증을 통과한 19곳을 추가하여 누적 161곳에 파서를 지정한다. 모든 지자체 URL은 계속 OFF 상태로 유지하며 운영자가 표본을 확인한 뒤 개별 ON 처리한다. 나머지는 `CHECK_REQUIRED` 또는 `FAILED` 상태로 유지한다. 2자리 연도는 2000년대로 제한해 해석하고, data 속성 및 스크립트 문자열에 URL이 명시된 경우에만 동일 host 링크로 변환한다.

V34는 기관별 JavaScript를 실행하지 않고 플랫폼 함수의 문자열·숫자 리터럴 인자, 링크 `data` 속성, 목록 URL query, 문서 hidden input만 허용된 URL 템플릿에 대입하는 `SAFE_TEMPLATE` 전략을 추가한다. 허용 placeholder는 `arg`, `attr`, `query`, `input`으로 제한하며 함수 인자 수와 동일 기관 host를 함께 검증한다. V35는 목록 추출률과 대표 상세 URL을 검증한 29곳에 14개 공통 플랫폼 프로필을 지정하여 누적 190곳을 `VERIFIED`로 관리한다. 신규 승격 출처는 모두 OFF 상태를 유지하며, 외부 전자민원 host로 이동하는 출처는 자동 승격하지 않는다.

V36~V38은 폐기된 URL을 현행 공식 목록으로 교체하고, 공통 게시판·셀 클릭형 새올 전자민원·대전 구청 통합 목록·검증된 JSON 응답을 정적 프로필로 보강한다. 대전 통합 목록의 상세 host는 검증된 5개 구청 host 고정 목록으로만 변환하며, 임의 host 입력은 허용하지 않는다.

V39는 브라우저가 공개 검색 폼을 제출해야 목록이 생성되는 성남시청 구조를 위해 `request_method_code`와 `request_form_json`을 추가한다. `request_method_code`는 `GET`, `POST_FORM`만 허용한다. `POST_FORM` 값은 공개 게시판의 문자열형 정적 검색 필드만 저장하며 secret, cookie, 인증정보, 사용자 개인정보를 저장하지 않는다. 수집기는 필드명, 필드 수, 값 길이와 전체 본문 크기를 제한한 뒤 UTF-8 URL 인코딩한다.

V40은 2026-07-14 전수 QA 결과를 정적 반영한다. 244곳 중 제목·등록일·안전한 상세 URL을 모두 확인한 224곳을 `VERIFIED/READY/OFF`로, 일부 행만 유효한 5곳을 `CHECK_REQUIRED/OFF`로 유지한다. 나머지 15곳은 접근 차단, 잘못된 기관 응답 헤더, timeout 또는 4xx/5xx로 로컬 환경에서 최종 확인하지 못했으며 파서 미지원 상태는 0곳이다. 이 migration은 어떤 출처도 자동으로 ON 처리하지 않는다.

V41~V45는 추가 공식 URL 교체, 반복 가능한 좁은 게시판 파서, 공주시 전자정부 게시판의 안전 상세 URL 템플릿을 반영한다. V46은 표준 Java HTTP 클라이언트에서 요청 헤더 또는 framing 오류가 재현되고 URLConnection 요청에서 HTTP 200이 확인된 기관에 한해 `LEGACY_BROWSER`를 추가한다. 이 요청 정책은 GET 전용이며 브라우저 호환 헤더와 기본값의 2배 제한시간을 사용한다. TLS 인증서 검증을 끄거나 redirect URL 검증을 우회하지 않는다.

V47~V54는 평택·천안·서천의 구형 게시판, 은평의 축약 열 새올 게시판, 강릉·순천의 현재 공식 HTTPS 목록, 강동의 느린 전자민원 응답을 각각 실사이트에서 재검증해 정적 프로필로 연결한다. 최종 DB 상태는 `VERIFIED 242`, `CHECK_REQUIRED 2`, `FAILED 0`이며 244곳 모두 `is_enabled=false`다. 중랑구는 최근 1년 이내 게시물이 없어 `STALE_SOURCE_CONTENT`, 금천구 지원사업 목록은 일부 행에 등록일이 없어 운영 재검토 대상으로 남긴다.

V55는 중랑·금천·성남·안양·속초·창원의 현재 공식 공고 화면과 검증된 수집 endpoint를 연결한다. 중랑·금천·창원은 기존 표준 목록 프로필을, 성남·안양·속초는 기존 안전한 새올 프로필을 재사용하며 별도 파서를 추가하지 않는다. 전수 QA에서 추가 확인된 강동·부산 남구의 간헐적 HTTPS 지연은 동일 공개 공고 HTTP endpoint와 느린 공공사이트용 `LEGACY_BROWSER` 정책으로 보정하고, 해운대·정읍·임실의 HTTP-to-HTTPS 전환은 공식 HTTPS URL과 브라우저형 요청 정책으로 보정한다. 사용자 바로가기 URL은 공식 HTTPS 화면을 유지하며 공개 수집 endpoint에는 개인정보나 secret을 포함하지 않는다. 적용 후 최종 DB 상태는 `VERIFIED 244`, `CHECK_REQUIRED 0`, `FAILED 0`이며 244곳 모두 `is_enabled=false`다. URL과 DOM 구조가 다시 변경되면 해당 출처만 후속 additive migration으로 보정한다.

V56은 출처 게시판 유형을 `LEGAL_NOTICE`, `SUPPORT_RECRUITMENT`, `GENERAL_NOTICE`, `PRESS_RELEASE`, `UNVERIFIED`로, 수집 정책을 `COLLECT_ALL`, `KEYWORD_FILTERED`, `EXCLUDED`로 분리한다. 244개 출처는 기초지자체 227, 시·도 15, 행정시 2이며 V56 적용 시점의 정적 의미 분류 결과는 일반 공지 208, 고시·공고 25, 지원·모집 10, 보도자료 1이다. 보도자료 출처는 OFF로 전환하고, 일반 공지는 DB seed의 포함·제외 키워드에 따라 `ACCEPTED`, `EXCLUDED`, `REVIEW_REQUIRED`로 판정한다. 키워드는 설명 가능한 판정 근거일 뿐 점수·추천 확률로 사용하지 않는다.

V57은 밀양시와 함양군이 일반 공지 URL을 사용하던 문제를 공식 고시·공고 화면으로 보정한다. 밀양시는 서버 렌더링된 고시·공고·채용 표에 기존 `SPRING_BBS`를 적용하고, 함양군은 공식 화면이 연결하는 새올 전자민원 endpoint에 기존 `SAFE_SAEOL_EMINWON`과 공개 폼 POST를 적용한다. 전수 QA와 운영 격리 시험에서 안양시 새올 endpoint timeout 및 공식 HTTPS 화면의 TLS 1.3 협상 종료가 재현되어, 공식 HTTPS 화면을 전용 `TLS12_BROWSER` 요청과 기존 `SPRING_BBS`로 직접 수집한다. 거제시는 페이지 번호가 없을 때 WAF가 HTTP 주소로 이동시키므로 공식 고시공고 URL에 `startPage=1`을 명시해 HTTPS 200 응답을 유지한다. 남동구는 403을 반환하던 일반 새소식 대신 제목·작성일·상세 URL을 제공하는 공식 고시공고 목록으로 교체한다. 속초는 공개 새올 고시공고를 수집하면서 일반 공지로 분류돼 있던 계약을 `LEGAL_NOTICE/COLLECT_ALL`로 바로잡고 기존 느린 사이트 전송 정책을 적용한다. TLS 인증서 검증과 URL 검증은 그대로 유지한다. 최종 분류는 일반 공지 203, 고시·공고 30, 지원·모집 10, 보도자료 1이다. migration은 여섯 출처를 자동 활성화하지 않고 OFF로 유지하며 기존 스냅샷·수집 실행·감사 이력을 삭제하거나 재분류하지 않는다.

V58은 운영 활성화 시 DNS 검증이 실패한 밀양시·함양군의 사용자 바로가기를 각 기관 대표 누리집의 공식 고시·공고 화면으로 고정한다. 밀양시는 `www.miryang.go.kr` 목록을 직접 수집하고, 함양군은 `www.hygn.go.kr` 공식 화면을 사용자에게 표시하면서 해당 화면이 연결한 `eminwon.hygn.go.kr` 공개 새올 endpoint만 내부 수집에 사용한다. 두 출처는 자동 활성화하지 않으며 기존 수집·감사 이력은 보존한다.

V59는 `SAFE_SAEOL_EMINWON_CELL`의 행 selector를 직접 자식 제목 셀 기준으로 좁혀 중첩 레이아웃 행을 수집 대상에서 제외한다. schema나 기존 원문·실행 이력은 변경하지 않으며 파서 프로필 설정만 additive migration으로 보정한다. 수집기는 과거 공고와 빈 레이아웃 행을 실패 건수에서 제외하고 실제 현재 후보 행의 필수값 누락만 `PARTIAL_FAILED`로 저장한다.

V61은 V57 적용 후에도 일반 공지로 남아 있던 203개 기관을 공식 고시·공고 메뉴 기준으로 전수 보정한다. 사용자 바로가기인 `notice_url`과 실제 수집 주소인 `collection_endpoint_url`을 분리하고, 새올 공개 목록은 개인정보나 인증값이 없는 hidden 필드만 `POST_FORM` 정적 seed로 저장한다. 링크 셀 대신 상위 행에 클릭 함수가 선언된 표는 설정된 함수명과 리터럴 인자만 해석하며 JavaScript를 실행하지 않는다. 적용 후 정적 분류는 고시·공고 233, 지원·모집 10, 보도자료 1이며 일반 공지 출처는 0이다. 기존 수집 원문, 실행 이력, 감사 로그는 삭제하거나 재분류하지 않는다.

V62는 2026-07-28 격리 전수 QA 결과를 정적으로 적용한다. V61 대상 203곳 중 제목·등록일·별도 상세 URL을 모두 추출한 190곳만 파서 프로필을 지정해 `READY/ON`으로 전환한다. 나머지 13곳은 관리자 화면에서 `PARTIAL_FIELDS`, `PARSER_FAILED`, `TRANSPORT_FAILED`로 구분할 수 있는 원본 오류 코드를 저장하고 OFF 상태를 유지한다. 전체 244곳 기준 QA 결과는 통과 230, 일부 필드 누락 5, 파서 미지원 6, 전송 실패 3이다. 강남구·대전 동구·용인시·서천군은 일반 공지 또는 채용 URL 대신 공식 고시·공고 메뉴와 실제 새올 수집 endpoint를 분리해 저장한다.

`local_government_notice_sources.parser_profile_code`는 시스템 소유 설정이다. 신규 출처는 `MANUAL_ONLY`로 등록하고, 일반 관리자 수정 요청에서는 기존 값을 보존한다. 실행 가능한 프로필 배정은 정적 migration과 격리 QA 결과로만 변경하며 운영자가 화면에서 직접 선택하지 않는다.

출처별 수집·의미 판정 중 예기치 않은 내부 예외는 `announcement_source_collection_source_results.error_code='PROCESSING_FAILED'`로 남긴다. 원문 예외나 stack trace는 이 테이블에 저장하지 않으며 실행 ID와 출처 관리코드만 서버 ERROR 로그의 추적 키로 사용한다.

지자체 수집은 제목, 등록일, 기관명, 원문 URL만 `source_completeness_code='MINIMAL'`로 저장한다. 본문·첨부·하이라이트와 매칭 조건 자동 저장은 수행하지 않는다. 정확한 교차 중복은 `DUPLICATE`, 유사 중복은 운영자 판단 전 `PENDING`으로 보존한다.

운영 공고 DRAFT 생성 직후 수집 원문은 `CONDITION_INPUT_REQUIRED`다. 대표 대상, 자격 조건, 진행 단계, 행동카드, 안내 문구를 입력·검수하고 운영 공고가 승인되어 정상 노출될 때만 `ACTIVATED`로 전환한다.

## 6. Enum / Status Code

| 코드 그룹 | 값 |
|---|---|
| `role_code` | `USER`, `PARTNER`, `OPERATOR`, `APPROVER`, `REVIEWER`, `ADMIN` |
| `user_status_code` | `ACTIVE`, `LOCKED`, `DISABLED`, `DELETED` |
| `consent_code` | `TERMS_OF_SERVICE`, `PRIVACY_POLICY`, `E_CERT`, `CREDIT_CHECK` |
| `partner_status_code` | `PENDING`, `ACTIVE`, `SUSPENDED`, `TERMINATED` |
| `relation_type_code` | `SPOUSE`, `CHILD`, `PARENT` |
| `business_type_code` | `SOLE_PROPRIETOR`, `CORPORATION`, `SIMPLIFIED_TAXPAYER`, `GENERAL_TAXPAYER`, `TAX_EXEMPT` |
| `company_stage_code` | `PRE_STARTUP`, `EARLY_STARTUP`, `OPERATING`, `SUSPENDED`, `CLOSURE_PLANNED`, `CLOSED`, `RESTART_PREPARING` |
| `verification_status_code` | `DRAFT`, `SUBMITTED`, `REVIEWING`, `VERIFIED`, `REJECTED`, `EXPIRED` |
| `document_source_type_code` | `USER_UPLOAD`, `E_CERT`, `PARTNER_CHECK`, `OPERATOR_CHECK` |
| `restriction_code` | `SAME_BUSINESS_SUSPECTED`, `SPOUSE_TRANSFER_SUSPECTED`, `FAMILY_BYPASS_SUSPECTED`, `CLOSED_REOPEN_SUSPECTED`, `POLICY_FUND_RESTRICTED`, `GUARANTEE_RESTRICTED`, `CREDIT_RECOVERY`, `PERSONAL_REHABILITATION`, `BANKRUPTCY_HISTORY`, `TAX_DELINQUENCY`, `OVERDUE_HISTORY`, `NEEDS_REVIEW` |
| `target_type_code` | `BUSINESS`, `PERSONAL`, `SPOUSE`, `CHILD`, `PARENT` |
| `approval_status_code` | `DRAFT`, `REQUESTED`, `APPROVED`, `REJECTED`, `CANCELED` |
| `auto_status_code` | `UPCOMING`, `OPEN`, `CLOSING_SOON`, `ENDED` |
| `manual_status_code` | `NORMAL`, `PAUSED`, `EARLY_CLOSED`, `SUSPENDED`, `BUDGET_EXHAUSTED`, `CLOSED`, `HIDDEN` |
| `application_method_code` | `ONLINE`, `VISIT`, `POST`, `EMAIL` |
| `reception_type_code` | `BUDGET_ENDS`, `FIRST_COME`, `ALWAYS_OPEN`, `PERIOD`, `EARLY_CLOSE_POSSIBLE` |
| `selection_method_code` | `FIRST_COME`, `REVIEW`, `LOTTERY`, `ELIGIBLE_PAYMENT`, `BUDGET_LIMIT` |
| `payment_method_code` | `CASH`, `VOUCHER`, `POINT`, `GOODS`, `REFUND`, `LOAN`, `GUARANTEE`, `INTEREST_SUPPORT`, `TAX_DEDUCTION` |
| `income_judgement_code` | `INCOME_CERT_ONLY`, `HEALTH_INSURANCE_ONLY`, `VAT_TAX_BASE_ONLY`, `ANY_ONE_DOCUMENT`, `INCOME_OR_HEALTH_INSURANCE`, `NO_LIMIT` |
| `comparator_code` | `GTE`, `LTE`, `GT`, `LT`, `EQ`, `BETWEEN` |
| `matching_status_code` | `MATCHED`, `NOT_MATCHED`, `REVIEW_REQUIRED`, `BLOCKED`, `PROGRESSED` |
| `condition_result_code` | `PASS`, `FAIL`, `SKIPPED`, `REVIEW_REQUIRED` |
| `progress_status_code` | `READY`, `IN_PROGRESS`, `WAITING_RESULT`, `APPROVED`, `REJECTED`, `SUPPLEMENT_REQUESTED`, `STOPPED`, `COMPLETED` |
| `step_status_code` | `LOCKED`, `READY`, `IN_PROGRESS`, `COMPLETED`, `SKIPPED`, `BLOCKED` |
| `result_code` | `APPROVED`, `REJECTED`, `SUPPLEMENT_REQUESTED`, `STOPPED` |
| `audit_result_code` | `SUCCESS`, `FAIL` |
| `stored_file_status_code` | `STORED`, `DELETED` |
| `document_submission_status_code` | `SUBMITTED`, `APPROVED`, `REJECTED` |
| `consultation_slot_status_code` | `OPEN`, `HELD`, `CLOSED`, `CANCELED` |
| `consultation_reservation_status_code` | `REQUESTED`, `ASSIGNED`, `CONFIRMED`, `CANCELED`, `COMPLETED`, `NO_SHOW` |
| `billing_cycle_code` | `ONE_TIME`, `MONTHLY`, `YEARLY` |
| `subscription_status_code` | `PENDING`, `ACTIVE`, `PAST_DUE`, `CANCELED`, `EXPIRED` |
| `billing_provider_code` | `MANUAL`, `TOSS`, `NICEPAY`, `KCP`, `STRIPE` |
| `payment_transaction_status_code` | `REQUESTED`, `APPROVED`, `FAILED`, `CANCELED`, `REFUNDED` |
| `refund_transaction_status_code` | `REQUESTED`, `APPROVED`, `FAILED` |
| `payment_provider_event_type_code` | `PAYMENT_APPROVED`, `PAYMENT_FAILED`, `PAYMENT_CANCELED`, `REFUND_APPROVED`, `REFUND_FAILED` |
| `notification_channel_code` | `IN_APP`, `EMAIL`, `SMS`, `KAKAO` |
| `notification_status_code` | `CREATED`, `SENT`, `FAILED`, `CANCELED` |
| `notification_delivery_status_code` | `REQUESTED`, `SUCCESS`, `FAIL`, `SKIPPED` |
| `notification_provider_code` | `INTERNAL`, `EMAIL`, `SMS`, `KAKAO`, `MANUAL` |
| `operation_task_type_code` | `DELAYED_PROGRESS`, `SUPPLEMENT_REQUEST`, `RECONTACT`, `PAYMENT_FAILED`, `CONSULTATION_PENDING`, `GENERAL` |
| `operation_task_status_code` | `OPEN`, `IN_PROGRESS`, `WAITING`, `DONE`, `CANCELED` |
| `operation_task_priority_code` | `LOW`, `NORMAL`, `HIGH`, `URGENT` |
| `operation_task_assignment_status_code` | `ASSIGNED`, `DONE`, `CANCELED` |
| `report_type_code` | `OPERATION_SUMMARY` |
| `report_format_code` | `CSV`, `EXCEL` |
| `report_export_status_code` | `REQUESTED`, `COMPLETED`, `FAILED` |
| `ai_assist_type_code` | `ANNOUNCEMENT_SUMMARY`, `DOCUMENT_DRAFT`, `OPERATION_MEMO_SUMMARY`, `USER_REPLY_DRAFT` |
| `ai_assist_resource_type` | `GENERAL`, `ANNOUNCEMENT`, `APPLICATION_PROGRESS`, `MATCHING_CASE`, `OPERATION_TASK`, `USER` |
| `ai_assist_request_status_code` | `REQUESTED`, `COMPLETED`, `FAILED` |
| `ai_assist_review_status_code` | `PENDING_REVIEW`, `ACCEPTED`, `DISCARDED` |
| `announcement_source_provider_code` | `BIZINFO`, `GOV24_PUBLIC_SERVICE`, `LOCAL_GOV_NOTICE` |
| `announcement_source_collection_request_type_code` | `BATCH`, `MANUAL` |
| `announcement_source_collection_request_status_code` | `APPROVAL_PENDING`, `APPROVED`, `REJECTED`, `CANCELED`, `EXPIRED` |
| `announcement_source_collection_run_status_code` | `QUEUED`, `RUNNING`, `COMPLETED`, `PARTIAL_FAILED`, `FAILED` |
| `announcement_source_run_item_status_code` | `COLLECTED`, `DUPLICATE`, `SKIPPED_ENDED`, `FAILED` |
| `announcement_source_review_status_code` | `COLLECTED`, `REVIEW_PENDING`, `CONDITION_INPUT_REQUIRED`, `REVIEW_COMPLETED`, `ACTIVATED`, `ARCHIVED`, `DUPLICATE`, `SKIPPED_ENDED` |
| `announcement_source_highlight_type_code` | `TARGET`, `SUPPORT_CONTENT`, `APPLICATION_PERIOD`, `APPLICATION_METHOD`, `EXCLUDED_TARGET`, `PREFERRED_CONDITION`, `BUSINESS_AGE_CONDITION`, `SALES_CONDITION`, `INDUSTRY_CONDITION`, `REGION_CONDITION`, `INCOME_CONDITION`, `ASSET_CONDITION`, `HEALTH_INSURANCE_CONDITION`, `REQUIRED_DOCUMENT`, `INQUIRY` |
| `announcement_source_duplicate_match_type_code` | `EXACT_DUPLICATE`, `SIMILAR` |
| `announcement_source_duplicate_decision_status_code` | `PENDING`, `CREATE_NEW_SELECTED`, `UPDATE_EXISTING_SELECTED`, `IGNORED` |

초기에는 `varchar`와 `CHECK` constraint를 사용한다. 코드명이 자주 바뀌는 영역만 별도 코드 테이블로 승격한다.

## 7. Local / Dev Seed와 운영 Migration 분리

운영 migration:

- 경로: `src/main/resources/db/migration`
- 포함: schema, FK, index, unique, check constraint, role/code seed
- 제외: 테스트 계정, 샘플 회원, 샘플 사업자, 샘플 공고, 샘플 파트너 검증, 운영 secret

local seed:

- 경로: `src/main/resources/db/seed/local`
- profile: `local`
- 포함 가능: 로컬 관리자, 로컬 사용자, 로컬 파트너, 로컬 검수자, 샘플 공고, 샘플 검증값
- 비밀번호는 로컬 전용 더미 해시만 허용한다.

dev seed:

- 경로: `src/main/resources/db/seed/dev`
- profile: `dev`
- 공유 개발환경 검증에 필요한 최소 데이터만 둔다.
- 운영 개인정보를 복제하지 않는다.

운영 초기 관리자 계정은 migration이 아니라 운영 bootstrap 절차로 생성하고 `password_reset_required = true`를 강제한다.

## 8. V1 Flyway Migration 범위

`V1__create_mvp_schema.sql`에 포함할 범위:

1. roles/users/auth tables
2. member/business/family profile tables
3. partner profile and verification tables
4. verification document and restriction tables
5. announcements, options, approvals, status histories
6. announcement condition tables
7. matching cases and result details
8. progress step, progress state, action, checklist, reminder logs
9. audit logs
10. role/code reference seed

`V1`에는 화면, Thymeleaf, API controller, matching algorithm implementation을 넣지 않는다. migration은 스키마 계약만 고정한다.

대시보드 전용 테이블은 `V1` 범위에 포함하지 않는다. `/api/v1/dashboard/me/...` 응답은 `partner_verifications`, `matching_cases`, `matching_result_details`, `application_progresses`, `application_step_states`, `application_step_checklists`를 기준으로 집계한다.

## 8.1 Additive Migration: 표준 서류 필드와 관리자 선택형 매칭

`V15__create_standard_document_fields.sql`은 기존 V1 계약을 깨지 않고 다음 구조를 추가한다.

- `standard_document_fields`: 공고 조건과 동적 입력에서 선택할 수 있는 표준 서류 필드 목록이다.
- `announcement_numeric_conditions.standard_field_id`, `announcement_option_conditions.standard_field_id`, `announcement_document_requirements.standard_field_id`, `announcement_input_requirements.standard_field_id`: 기존 저장 방식은 유지하면서 표준 필드와 연결할 수 있는 선택 FK다.
- `member_profiles`, `business_profiles`, `family_members`, `verification_family_values`에는 소득 여부, 소득 금액, 연매출 등 기본정보 비교에 필요한 선택 컬럼을 additive로 보강한다.
- 표준 서류 필드는 사업자등록증, 부가세 과세표준증명원, 면세사업자 수입금액증명원, 소득금액증명원, 국세완납증명서, 지방세완납증명서, 주민등록등본, 가족관계증명서, 건강보험료 납부확인서, 건강보험 자격확인서 기준으로 seed한다.

정책:

- 표준 서류 필드의 `required_default`는 기본 `false`다. 일반 사용자에게 서류 내용 입력을 기본 필수로 강제하지 않는다.

`V20__add_condition_eligible_standard_document_fields.sql`은 `standard_document_fields.is_condition_eligible`을 추가한다.

- `is_condition_eligible`은 과거 boolean 호환 필드로 유지한다.
- 자동 조건 저장 가능 여부의 상위 계약은 V21의 `condition_usage_code`를 사용한다.
- `matching_stage_code='BASIC'` 후보 계산은 `standard_field_id IS NULL`인 기본정보 조건만 사용한다.
- `matching_stage_code='FINAL'` 후보 계산은 `standard_field_id`가 연결된 조건에 대해 `member_document_input_values.standard_field_id` 값을 직접 비교한다.
- 이 필드는 추천도, 선정확률, 점수, 우선순위 계산에 사용하지 않는다.
- 자동 추천도, 선정확률, 점수, AI 자동판단 컬럼은 추가하지 않는다.
- 네이버 전자증명 API 자동 수집을 전제로 하는 저장 컬럼은 추가하지 않는다.

`V21__create_standard_code_catalogs.sql`은 외부 API 호출 없이 공고 조건 표준 코드를 DB seed로 관리하기 위한 구조를 추가한다.

- `standard_document_fields.condition_usage_code`: 표준 서류 항목의 조건 사용 상태다. 값은 `INPUT_ONLY`, `CONDITION_READY`, `STANDARDIZATION_REQUIRED`만 허용한다.
- `standard_code_groups`: KSIC, 사업자 유형, 과세 유형, 지역, 법정동, 건강보험 자격 구분 등 표준 코드 그룹이다.
- `standard_codes`: 코드 그룹별 실제 코드 목록이다. 운영 migration에는 MVP 대표 subset만 seed하고, 대량 전체 코드는 별도 운영 import 스크립트로 분리한다.
- `standard_field_code_groups`: 표준 서류 항목과 표준 코드 그룹의 연결 정보다. 사용 목적은 `CONDITION_VALUE`, `DISPLAY_OPTION`, `REFERENCE_MAPPING`으로 구분한다.

`condition_usage_code` 정책:

- `CONDITION_READY`: 공고 수치/선택 조건의 `standard_field_id`로 저장할 수 있고, 최종 매칭에서 자동 비교할 수 있다.
- `STANDARDIZATION_REQUIRED`: 화면에는 조건 후보로 보여주지만 자동 조건 저장은 차단한다. 업태/종목은 예외적으로 `announcement_industry_conditions.ksic_code`에 KSIC 코드로 저장한다.
- `INPUT_ONLY`: 사용자/운영자 입력 또는 확인 용도이며 공고 조건 저장에는 사용할 수 없다.

`V16__create_member_document_input_values.sql`은 사용자 기본정보 입력 화면의 서류별 선택 입력값 저장 구조를 추가한다.

- `member_document_input_values`: 회원 사용자가 입력한 표준 서류 필드별 값을 저장한다.
- `(user_id, standard_field_id)` unique로 한 사용자 기준 같은 서류 필드의 중복 입력을 차단한다.
- `value_text`, `value_number`, `value_date`, `value_boolean` 중 하나만 저장할 수 있도록 check constraint를 둔다.
- `standard_document_fields`에는 제공된 전자증명 항목 중 V15에 없던 사업장 주소, 업태, 종목, 사업자 정보, 종합소득금액, 완납 여부, 세대원 정보, 가족관계, 가입자 정보 등을 추가 seed한다.
- 이 구조는 사용자가 네이버 전자지갑 등에서 발급한 증명서를 회사에 전달하고, 필요한 값을 수동 입력하는 운영 흐름을 전제로 한다.

`V22__add_structured_address_fields.sql`은 주소 검색 결과 저장 구조를 추가한다.

- `member_profiles`: `postal_code`, `road_address`, `jibun_address`, `detail_address`, `sido_name`, `sigungu_name`, `eupmyeondong_name`, `legal_dong_code`, `road_name_code`, `building_management_no`, `address_source_code`.
- `business_profiles`: 같은 의미의 사업장 컬럼을 `workplace_` 접두어로 추가한다.
- `region_code`, `workplace_region_code`는 기존 화면 셀렉트와 기존 매칭 조건 호환을 위해 유지한다.
- API 승인키와 외부 API 응답 원문 전체는 DB에 저장하지 않는다.

## 9. Backend Gate 조건

- `V1__create_mvp_schema.sql`이 빈 PostgreSQL DB에 성공적으로 적용된다.
- FK, index, unique constraint, check constraint가 migration에 명시되어 있다.
- 운영 migration에 테스트 계정과 샘플 업무 데이터가 없다.
- local/dev seed는 profile별 Flyway location으로만 실행된다.
- 매칭 테이블에 추천도, 우선순위, 선정확률, 가점 컬럼이 없다.
- MyBatis XML 작성 시 `SELECT *`와 `${}`가 없다.
- 개인정보와 운영 감사 로그가 분리되어 있다.
- 공고 승인 상태가 `APPROVED`가 되기 전에는 매칭 기준으로 사용되지 않는다.
- 검증 ID가 있는 매칭은 파트너 검증값이 회원 입력값보다 매칭 기준에서 우선한다.
- 검증 ID가 없는 매칭은 운영자 수동 생성 또는 관리자 조건 후보 생성으로 생성되며, 동일 공고/회원 조합은 partial unique index로 중복을 차단한다.
- 진행 단계 완료 조건 충족 전 다음 단계 이동이 서버에서 차단된다.

## 10. Additive Migration: 공고 수집·분류 V2 (`V63`~`V71`)

2026-08-13 운영 DB에 V63~V68 적용을 확인했다. 초기 release는 `DRAFT` 1건, `ACTIVE` 0건이며 분류 V2와 상세본문 feature flag는 OFF다. 기존 V1~V62 migration은 수정하지 않고 다음 migration만 추가했다.

| migration | 계약 |
|---|---|
| `V63` | 지원대상·지원형태 카탈로그와 운영 공고 다중 배정 테이블 |
| `V64` | `DRAFT -> ACTIVE -> RETIRED` 생명주기를 갖는 규칙 release/group/rule/term 및 `row_version` |
| `V65` | 클라이언트 확정 정책을 담은 초기 `DRAFT` seed. 첨부 분석과 자동 활성화는 `false` |
| `V66` | 불변 원문 버전, 판정 evaluation, 일치 근거, 자동 태그, 관리자 확정 태그와 수집 실행의 규칙·검색계획 고정값 |
| `V67` | 운영 데이터와 격리 QA를 구분하는 `data_purpose_code` (`PRODUCTION`, `QA`) |
| `V68` | 기존 중복을 자동 정리하지 않는 사전검사와 `announcement_source_links.source_id` 단독 UNIQUE. 여러 원문이 같은 운영 공고에 연결되는 것은 허용 |
| `V69` | 기존 운영 원문의 재분류 미리보기·적용·일시중지·재개·원복 실행과 항목별 고정 원문·예상 판정·충돌·실패 이력 |
| `V70` | 제목 제외 공고의 원문 비저장 tombstone·규칙 FK, 기존 수집·재분류 run item 비식별화와 link 없는 제외 snapshot 원문 삭제 |
| `V71` | source 단위 비식별 정리와 FK 유지 비용을 제한하는 `announcement_source_collection_run_items.source_id` partial index |

핵심 불변조건:

- Flyway가 schema source of truth이며 초기 release는 운영에서 자동 활성화하지 않는다.
- 제목 자동 제외 공고는 `announcement_source_content_versions`를 만들지 않는다. SHA-256 identity, raw hash, 상태·사유·단계, rule·term FK와 발생 건수만 보존한다.
- 현재 판정은 `is_current=true` 한 건만 유지하되 과거 evaluation은 삭제하지 않는다.
- 자동 태그와 관리자가 확정한 태그를 분리하고, 재분류 시 기존 확정값은 `STALE`로 전환한다.
- 기존 `announcements.target_type_code`와 V1 단일 입력 계약은 유지한다. V1 저장은 대표 태그만 갱신하고 추가 태그를 임의 삭제하지 않으며, 복수 저장은 `/api/v2`가 담당한다.
- 기존 V1 첨부 행과 조회 응답은 호환을 위해 보존한다. TITLE/BODY 기본 분류는 첨부 URL·파일명을 입력에서 계속 제거한다. 이후 승인된 첨부 수집은 아래 V72/V73 별도 근거 계약을 사용하며 기존 attachment 행에 다시 섞지 않는다.
- 일반 애플리케이션 수집 write는 `data_purpose_code='PRODUCTION'`만 생성한다. QA 원문을 DB에 쓰는 내부 경로는 운영 격리 QA 승인 시 별도 확정하며, QA 정리는 명시적으로 `QA`인 행만 대상으로 한다.
- V56 판정 이력과 과거 snapshot을 migration에서 자동 재분류하지 않는다.
- 기존 원문 재분류는 `data_purpose_code='PRODUCTION'` 대상만 실행 이력에 고정한다. 미리보기는 source/evaluation을 변경하지 않으며, 적용은 고정한 원문 버전과 `classification_row_version`이 일치할 때만 새 evaluation을 append한다.
- 원복은 적용 evaluation을 삭제하지 않고 current만 해제하며 이전 current 판정과 snapshot 호환 projection을 복원한다. 단, V70에서 제목 제외 원문이 비식별 삭제된 항목은 `exclusion_id`로 감사 상태·hash만 보존하므로 해당 항목이 포함된 실행은 원복할 수 없다. 연결된 운영 공고의 상태는 적용·원복 모두 자동 변경하지 않는다.

## 11. Additive Migration: 첨부 근거와 worker 실행 (`V72`~`V73`)

V73의 추가 계약이다. 최초 2026-09-10에는 운영 V72와 로컬 구현을 구분했으며, 이후 schema·시험·운영 확인 시점은 문서 상단의 2026-09-16 기준을 따른다. 아래 계약 설명을 운영 worker 활성화 증거로 해석하지 않는다.

- V72는 정책, 배치, 작업, 불변 근거 묶음·파일·추출·종합 판정·입력·일치 근거·확정·태그를 기본 판정과 분리한다. 세부 DDL이 최종 계약이다.
- V73은 job에 nullable `execution_snapshot_json`, `download_budget_bytes`(최대 80 MiB), `reserved_download_bytes`를 추가한다. 이전 미고정 job의 실행 버전을 추측해 채우지 않는다.
- V73의 `announcement_attachment_collection_plans`는 목록 수집 run의 keyword release와 첨부 정책을 FROZEN/OFF/NO_POLICY로 불변 고정한다. job의 nullable `collection_run_id`와 `(run,policy,rule)` composite FK가 같은 실행의 정책을 검증한다. run 중 게시된 정책으로 대상을 암묵적으로 확장하지 않는다.
- `announcement_source_snapshots.attachment_intake_status_code`는 NOT_REQUESTED/QUEUED/PROFILE_REQUIRED/BASE_RECLASSIFICATION_REQUIRED/POLICY_BINDING_CHANGED/PROTECTED_LINK/RECHECK_NOT_DUE로 자동 예약·차단 원인을 구분한다. profile 미연결은 첨부 없음이나 성공이 아니다. 자동 재확인은 source마다 최소 24시간이며, 최근 일반 job이 있으면 기존 current/confirmation/version을 변경하지 않고 RECHECK_NOT_DUE만 기록한다. 명시적 재시도는 별도 한도 계약이다.
- 신규 ENFORCE binding은 같은 run에서 생성한 미연결 source만 허용하고 원문 저장 transaction에 포함한다. 이미 저장된 미적용 source는 ENFORCE 정책 아래 수집하더라도 preview만 생성한다. 신규 binding job에도 이전 pointer/policy/confirmation과 적용 후 version을 기록한다.
- 실행 snapshot은 시스템 profile code/hash, 종합 엔진 버전, 추출기 버전/config hash다. 예약 후 바꾸지 않으며 재시도 예산은 감소할 수 없다.
- V73의 `announcement_source_attachment_sets.discovery_warning_codes_json`은 최대 20개의 고정 발견 경고 코드다. 불변 manifest에 포함하며 외부 오류 메시지·URL·파일명 원문을 경고 코드에 넣지 않는다.
- `announcement_attachment_resource_leases`는 전체 다운로드 2, host별 1, 전체 추출 1 슬롯을 DB에서 공유한다. job/lease token과 만료 시각으로 늦은 worker의 갱신·해제를 차단한다.
- source의 `classification_row_version` 변경 시 진행 중 첨부 작업을 CONFLICT로 만료시키고 기존 첨부 current pointer·확정을 해제한다. 첨부 버전은 증가하고 정책/검수 필수 flag와 과거 근거는 보존한다. OFF만으로 검수 guard가 사라지지 않는다.
- 일반 첨부 재수집 예약은 version 증가와 함께 이전 첨부 current pointer/평가 current flag/confirmation을 해제한다. 새 작업 완료 전 이전 성공 판정을 현재로 반환하지 않으며, 기본 판정과 기본 확정 태그는 변경하지 않는다. 이전 이력은 삭제하지 않는다. 배치 미리보기는 이 일반 예약 경로로 current를 변경하면 안 된다.
- 근거 저장은 source→job 잠금과 마지막 lease CAS 아래 OPEN→SEALED로 처리한다. 성공 파일의 추출 실패도 근거이며, 발견 실패는 NO_FILES로 바꾸지 않는다. 봉인은 종합 판정 완료나 운영 공고 활성화를 뜻하지 않는다.
- 봉인된 근거의 종합 평가는 snapshot 조회→transaction 밖의 규칙 계산→source/job 최종 CAS로 저장한다. 파일별 입력(실패 포함), release에 속한 키워드 위치, 자동 다중 태그가 별도 행이다. 동일 작업 재완료는 멱등이고, 새 작업 세대의 set/file/extraction 선택 ID를 manifest에 포함하여 과거 입력을 혼합하지 않는다.
- 배치 job의 평가는 preview만 저장한다. 일반 job은 첨부 current pointer·attachment version만 갱신하며 기존 base 판정, 검수 필수 flag, 연결 정책, 운영 공고는 수정하지 않는다. COLLECT_ONLY effective 기준은 base로 유지한다.
- 작업에 예약된 bytes보다 큰 수신량을 저장할 수 없다. safe locator는 시스템 상대 경로·허용된 비기밀 식별자만 저장한다. 임시 fetch URL, 인증정보, 로컬 binary 경로는 저장 DTO에 없다.
- 이 migration은 정책 게시·ENFORCE·기존 데이터 재분류·운영 공고 활성화를 실행하지 않는다. 전체 상시 수집 연결은 진행 기록의 남은 Gate를 따른다.

### 11.1 관리자 확인·DRAFT 연결의 V73 추가 계약

- `announcement_source_attachment_confirmations.confirmed_source_version`, `confirmed_attachment_version`를 nullable integer로 추가한다. 두 값은 함께 null이거나 함께 0 이상이며 기존 확인의 버전을 추측해서 backfill하지 않는다. 새 API는 확인 직후의 원문/첨부 버전을 저장한다.
- 확인 행은 `is_current`만 바꿀 수 있다. 평가/메모/방법/버전/idempotency hash를 UPDATE로 수정할 수 없는 trigger를 둔다. 재확인은 새 행과 새 CONFIRMED 태그를 생성하고 이전 행을 STALE로 남긴다.
- 현재 확인 projection은 current evaluation/set hash뿐 아니라 원문 버전과 첨부 버전을 확인한다. 버전 없는 legacy 확인은 CURRENT로 표시하지 않는다. DRAFT 전환이 동일 확인으로 link를 만들며 첨부 버전을 1 증가시킨 경우에만 그 차이를 허용한다. 일반 재수집의 이전 확인은 승계하지 않는다.
- `announcement_source_links.attachment_confirmation_id`, `attachment_request_hash`를 nullable로 추가한다. 두 값은 함께 존재해야 하며 confirmation/source composite FK로 다른 source의 확인 연결을 거부한다. source UNIQUE와 기존 V1 입력은 보존한다.
- 첨부 확인에 연결된 link의 source/announcement/confirmation/request hash는 UPDATE로 바꿀 수 없다. 동일 전환 재시도는 최초 정규화 요청 hash를 비교하고 다른 요청은 409다.
- confirmation/전환 쓰기는 source 행 잠금과 기대 버전 CAS를 사용한다. 각 단계는 첨부 버전만 증가시키고 base/자동 판정/검수 guard를 변경하지 않는다. 카탈로그의 enabled 상태도 정렬된 FOR SHARE 잠금 아래 검증한다.
- service/Mapper의 PostgreSQL 동시성·멱등성·DRAFT·불변 이력 시험은 Linux35050057694의 job192건에서 실패·생략 없이 실행했다. 과거 Windows Code Integrity 차단과 구분하며, 운영 관리자 업무 E2E까지 검증한 것은 아니다.

### 11.2 역할 변경과 성공 파일 중간 저장의 V73 추가 계약

- `announcement_attachment_jobs.operation_code`는 COLLECT/ROLE_CHANGE/RETRY_FILES다. 역할 변경은 `reference_set_id`, `requested_by`가 필수이며 원래 set과 새 set은 같은 source/content/policy/profile의 서로 다른 SEALED set이어야 한다. ROLE_CHANGE job의 set 연결은 불변이고 예약 다운로드 bytes는 0으로 고정한다. RETRY_FILES의 선택 범위는 11.3을 따른다.
- `announcement_source_attachment_extractions.reused_from_extraction_id`는 같은 source의 원래 extraction을 가리킨다. DB trigger는 원래/새 파일 binary hash, 추출기 버전·config, 품질·텍스트·hash·block·페이지·소요시간·실제 추출 시각의 동일성을 확인한다. 역할 변경이 재다운로드/재추출처럼 기록되지 않는다.
- `announcement_attachment_file_checkpoints`는 `(job_id,stable_locator_hash)` PK 및 `(job_id,source_id)` composite FK의 내부 중간 근거다. 다른 job/generation에서 재사용하지 않으며 API/최종 첨부 set과 구분한다. UPDATE를 금지하고 유효 RUNNING COLLECT/RETRY_FILES job에만 INSERT한다. RETRY_FILES의 service 검증은 선택 locator·고정 역할과 일치하는 파일만 허용한다.
- checkpoint JSON은 검증된 안전 locator·성공 파일 metadata·COMPLETE_TEXT 추출/근거/최초 완료 epoch 시각으로 구성한다. 파일당 16 MiB, 정상 목록 10개 × 최대 3시도 중 목록 변경까지 30개 상한을 둔다. 바이너리·fetch URL·인증값·감사 metadata는 저장하지 않는다.
- source→job 잠금과 lease 확인 후 중간 저장한다. 최종 봉인/종료/충돌/취소 시 trigger와 봉인 transaction에서 중간 텍스트를 제거하며 source/job 삭제도 cascade된다. RETRY_WAIT/일시 중지는 재사용을 위해 유지한다. 공개 원문의 재확인 주기가 지난 새 세대는 중간 저장을 승계하지 않는다.
- 최종 extraction의 기존 `created_at`에는 추출 완료 epoch 시각을 timestamptz로 변환해 저장한다. 재사용 중간 근거의 시각을 봉인 시점으로 갱신하지 않는다. 완료 시각 없는 기존 내부 입력은 호환 기본값을 유지하며 운영 이력을 backfill하지 않는다.
- CurrentMapper의 실제 job 컬럼은 V72의 `set_id`다. 잘못 참조했던 `last_set_id`를 수정했으며 새 오프라인 XML/parameter 검증과 실제 PostgreSQL 회귀를 별도 Gate로 관리한다.
- 위 trigger·FK·timestamp/동시성 계약은 Linux35050057694의 migration·job 시험 근거를 따른다. 마지막 운영 schema V83 확인과 현재 운영 정책/worker 활성화 여부는 별도다.

### 11.3 실패 파일 선택 재시도의 V73 추가 계약

- `RETRY_FILES` job은 `reference_set_id`/`requested_by`를 필수로 갖는다. 예약 시 `set_id`는 null이며 최종 결과는 원래 reference와 다른 SEALED set이다. 같은 source/content/policy/profile을 DB trigger로 확인하며 한 번 연결된 최종 set을 교체하지 않는다.
- `announcement_attachment_retry_files`의 PK는 `(job_id,file_id)`다. `(job_id,source_id,reference_set_id)` → jobs와 `(file_id,reference_set_id,source_id)` → files의 composite FK로 다른 원문/집합 혼합을 차단한다.
- 선택 범위는 PENDING·미봉인 RETRY_FILES job에만 최대 10개 INSERT할 수 있다. 다운로드 FAILED/CANCELLED 또는 허용된 불완전 추출 품질만 선택할 수 있다. deferred constraint trigger는 transaction 완료 전에 한 개 이상 선택됐는지 확인한다.
- 선택 행 UPDATE 및 살아 있는 source/job 아래의 직접 DELETE를 금지한다. 승인된 상위 source/job 삭제의 FK cascade 정리는 허용한다. 서비스는 source 잠금 아래 현재 버전·정책·집합·진행 중 작업과 동일 멱등 키를 먼저 검증한다.
- 새 근거는 기존 전체 파일 수를 유지한다. 비선택 파일/extraction은 DB 복제와 `reused_from_extraction_id`로 원래 품질·실패·시각을 보존하고 선택 파일만 새 근거를 저장한다. MANUAL을 포함한 문서 역할/출처는 고정하며 파일 역할 변경 API와 혼합하지 않는다.
- 새 상세의 전체 locator 집합이 달라지거나 발견에 실패하면 선택 파일의 새 binary를 요청하지 않고 발견 미완료/실패를 저장한다. 실패 발견을 NO_FILES로 변환하거나 새 파일을 선택 범위에 추가하지 않는다.
- 새 선택 결과 bytes만 job의 누적 다운로드 예산과 비교한다. 이번 job 내부 중간 성공은 11.2의 checkpoint로 재사용할 수 있다. 일반 COLLECT 전체 저장 API로 RETRY_FILES 범위를 우회할 수 없다.
- 기본 수동 제한은 전체 COLLECT 수동 요청과 RETRY_FILES를 합산하여 source별 60초/최근 24시간 3회이며 같은 멱등 요청은 추가 예약으로 계산하지 않는다. 현재 값은 초기 구현 상한이다. 이 migration은 실제 재시도 예약·운영 정책 활성화·기존 데이터 변경을 실행하지 않는다.

### 11.4 불변 판정 이력의 조회 계약

- V73의 `ix_att_eval_source_history(source_id,evaluated_at DESC,id DESC)`는 source별 이력 pagination을 지원한다. 기존 current partial UNIQUE와 별개이며 V72 데이터/판정을 변경하지 않는다.
- 이력은 당시 base/set/policy/release와 자동 태그를 조회한다. 현재 카탈로그·정책·확정 태그로 과거 자동 결과를 덮어쓰지 않는다.
- 입력 조회는 `evaluation_inputs`의 exact file/extraction/set/source 관계를 따른다. 추출 없는 실패 입력은 LEFT JOIN으로 보존하고 최신 extraction 재선택을 하지 않는다.
- matches 목록과 count는 같은 evaluation/source/file 및 frozen release/group/rule/term JOIN을 공유한다. 전체 추출문·safe locator JSON·fetch URL·감사 메모를 읽지 않고 제한된 block 조회로 연결할 좌표만 제공한다.
- 현재 사용 여부는 동일 REPEATABLE_READ transaction의 현재 source projection을 기준으로 CURRENT_EFFECTIVE/CURRENT_PREVIEW/NOT_CURRENT를 구분한다. 이력 조회 자체는 판정·확인·버전·정책을 변경하지 않는다. 제목 제외/QA 원문은 과거 근거가 잔존해도 외부 조회 service에서 차단한다.

### 11.5 초기·전체 수동 수집 예약

- 기존 COLLECT operation에 nullable `requested_by`를 사용한다. 자동 예약은 null, 수동 전체 수집은 운영자 ID다. 기존 불변 실행 trigger가 actor/operation/request hash/실행 snapshot/예산을 예약 후 변경하지 못하게 한다.
- V73의 `ix_att_job_manual_network_window(source_id,created_at DESC)`는 `requested_by IS NOT NULL AND operation_code IN ('COLLECT','RETRY_FILES')` partial index다. 두 API는 같은 SQL scope로 요청 횟수를 합산하며 source 잠금 아래 재검증한다.
- read-only 조건 조회는 정책 공유 잠금을 사용하지 않는다. 예약은 source 행 잠금→기존 멱등 조회/버전 검증→동일 ACTIVE 규칙의 ACTIVE 정책 공유 잠금→profile/예산/한도 검증→job/버전/STALE/intake/감사를 같은 transaction에서 저장한다.
- 최초 source에는 현재 첨부 evaluation ID가 null이다. 이 null도 요청 기대값으로 비교한다. 요청된 전체 수집은 과거 set을 직접 수정하거나 선택 파일 scope를 만들지 않으며 일반 COLLECT worker를 그대로 사용한다.
- 미적용 기존 source는 ENFORCE 정책으로 수집해도 preview만 저장하고 검수 binding을 새로 만들지 않는다. 이미 binding이 있는 source만 그 의무/동일 정책을 보존한다. 정책 교체·기존 데이터 적용은 별도 승인된 경로의 계약이다.
- 최신 DB 테스트는 초기 read-only 조회, 미적용 ENFORCE preview, 공유 수동 한도, 같은 key 동시 예약, 조건 변경 충돌을 추가했으나 실제 Linux PostgreSQL 실행은 아직 남아 있다. V73은 운영 미적용 상태다.

### 11.6 키워드 규칙 교체 중 첨부 계획 보호

- 새 schema 변경 없이 `announcement_attachment_policies`와 keyword release 상태를 읽어 수집 실행 준비 단계에서 불일치를 거부한다. 일치 정책 조회는 ACTIVE keyword release를 요구한다. 불일치 ENFORCE 조회는 정책이 ACTIVE인지만 확인하고, keyword release가 다르거나 이미 퇴역한 경우도 포함한다. 과거 keyword release를 ACTIVE inner join으로 먼저 제거하면 교체 공백을 숨기므로 그렇게 조회하지 않는다.
- 후보 정책은 공유 잠금으로 조회하며 외부 요청은 transaction 종료 뒤에만 진행한다. 계획 작성 전 차단되면 collection plan/source/job/첨부 근거를 쓰지 않는다. 기존 NO_POLICY 계획은 변경하지 않고 재시작 차단 여부만 확인한다.
- PostgreSQL fixture는 퇴역 규칙의 ACTIVE ENFORCE, 일치 COLLECT_ONLY/OFF 우선, NO_POLICY 재개, context 조회 이후 규칙 퇴역을 검증하도록 추가했다. fixture는 테스트 소유 임시 DB에만 작성하며 운영 데이터와 연결하지 않는다. 실제 실행하지 않은 SQL/잠금 검증은 성공으로 보고하지 않는다.

### 11.7 정책 초안 멱등성·개정 계보

V73에 다음 additive 계약을 추가했다. 기존 V1~V72 파일은 수정하지 않는다.

- `announcement_attachment_policies`의 `creation_idempotency_key uuid UNIQUE`, `creation_request_hash varchar(64)`, `creation_operation_code varchar(20)`, `copied_from_policy_id uuid`(self FK). 기존 행은 모두 NULL로 보존한다.
- `ck_att_policy_creation`은 네 필드 전체 NULL인 기존 행 또는 key/hash/operation이 완성된 관리 초안만 허용한다. CREATE의 parent는 NULL, REVISION의 parent는 다른 정책 ID다. 요청 hash는 actor/operation/정규화 입력에 결합하며 응답에 노출하지 않는다.
- `ix_att_policy_parent`, `ix_att_policy_list(created_at DESC,id DESC)`를 추가한다. V72의 family/version UNIQUE와 rule별 ACTIVE UNIQUE를 보존한다.
- `tr_att_policy_revision_parent`는 개정 INSERT가 원본과 같은 family이고 더 큰 versionNo인지 확인한다. 원본 family/version은 별도 불변 trigger로 유지된다.
- `tr_att_policy_draft_identity`는 DRAFT도 id/code/versionNo/생성자/생성 시각/최초 요청/개정 parent를 바꾸지 못하게 한다. 관리 초안(key 존재)의 변경은 rowVersion을 정확히 1 증가시켜야 한다. V72 게시 불변 trigger는 그대로 유지한다.
- 서로 다른 생성·개정 경로의 동일 key를 transaction advisory lock namespace 73101로 직렬화한다. 개정 family는 namespace 73102로 직렬화한 뒤 최대 versionNo+1을 배정한다. 부모 조회 버전과 DRAFT 수정 CAS를 재검증한다. 일반 GET에는 FOR UPDATE/FOR SHARE를 사용하지 않는다.
- CREATE/REVISION INSERT는 SQL에서 DRAFT를 고정하고 policy_hash/published_at을 복사하지 않는다. 수정은 DRAFT·조회 버전 조건 아래 설정·system profile snapshot을 바꾸고 policy_hash=NULL과 rowVersion+1을 기록한다. 기존 source/job/확인·ACTIVE 정책을 수정하지 않는다.
- 새 초안/수정의 `settings_json`은 engineVersion/extractorVersion/extractorConfigHash(null)/maximumSourceBytes만 저장한다. 관리자 임의 JSON/URL/parser/실행 설정은 받지 않는다. 이후 역할 규칙 확장은11.31, runtime·QA 근거 검증과 게시 계약은11.22~11.28을 따른다. 전체 공식 Provider QA와 운영 게시 실행은 미완료다.
- 실제 PostgreSQL fixture 7건은 생성/수정/현재 상태 멱등 재조회·감사 분리, 동일 key 동시 생성, 다른 parent 동시 개정, 동시 CAS, DB 불변 trigger, 다른 actor/operation 키 재사용, 게시/퇴역 수정 거부와 개정을 다룬다. 최신 Linux 실행 전까지 컴파일/정적 검증만으로 SQL·잠금 성공을 선언하지 않는다.

### 11.8 정책 분류 검증 이력

V73에 `announcement_attachment_policy_checks`를 추가했다. 이 테이블은 `CLASSIFICATION_GOLDEN`만 기록하며 전체 QA/게시 가능 상태를 저장하지 않는다.

- 정책 ID/rowVersion/입력 hash, 규칙 ID/rowVersion/계산 snapshot hash/실제 RuleSet 내용 hash, check type, suite/엔진 버전, 결과 hash, 사례 수/ID JSON, 요청자, 멱등 키/요청 hash, 실행 시각을 저장한다. source/job이나 원문을 복사하지 않는다.
- 정책·규칙·user FK, key UNIQUE, SHA-256 형식, case_count(1~1000)/JSON 배열 길이 일치 CHECK, 정책별 이력/규칙/요청자 인덱스를 둔다. 정책 삭제는 검증 이력 FK가 보호한다.
- INSERT trigger는 현재 DRAFT 정책·정확한 정책/규칙 버전과 DRAFT/ACTIVE 규칙 연결만 허용한다. UPDATE는 항상 거부한다. 이후 정책/규칙 수정은 이력을 덮어쓰지 않고 조회에서 is_current=false가 된다.
- 성공 이력 저장 직전에 key advisory lock namespace 73103, 규칙 공유 잠금, 정책 행 잠금과 snapshot 대조를 수행한다. 같은 key의 동시 실행이 계산을 각각 수행해도 INSERT/감사 기록은 한 번만 남긴다.
- `AnnouncementSourceRuleReleaseService.selectRuleValidationDetails`는 read-only REPEATABLE_READ로 현재 행의 snapshot을 계산하며 저장된 게시 hash와 구분한다. 기존 v1 API와 게시 계약은 변경하지 않는다.
- 테스트 소유 PostgreSQL fixture에 실제 seed 규칙 분류/미게시 유지, 같은 key 동시 저장, 수정 후 STALE·이력 불변·잘못된 버전 거부 3건을 추가했다. 실제 Linux 실행 전까지 SQL/잠금 성공으로 계산하지 않는다.

### 11.9 비동기 정책 QA 실행·증거·공유 자원

V73의 additive 계약이며 V1~V72는 변경하지 않는다.

- `announcement_attachment_policy_validation_runs`: 정책·규칙 FK/rowVersion, snapshot hash/JSON, run 상태/rowVersion, actor FK, UUID 멱등 키 UNIQUE/요청 hash, lease token/만료, 고정 오류 코드, 생성/시작/종료 시각. 입력 JSON object 최대 2 MiB DB 제한(서비스 직렬화는 1.5 MB), hash 형식과 상태별 lease/시각 조합 CHECK를 둔다.
- 전역 PENDING/RUNNING/CANCEL_REQUESTED 1건 unique partial index, 정책별 이력·규칙·actor index. 예약/claim/취소는 advisory lock 73104로 짧게 직렬화한다. 정책 rowVersion과 실행 rowVersion은 별도 CAS다.
- `announcement_attachment_policy_validation_steps`: run FK + stepCode PK, PASSED/FAILED/MISSING, 서버 생성 metadata JSON object 최대 32 KiB, evidence hash/시각. 단계는 CLASSIFICATION_GOLDEN, INSTALLED_RUNTIME, PROVIDER_PROFILES, WORKER_DB_RECOVERY 4개로 제한한다.
- run trigger는 현재 DRAFT 정책/정확한 정책·규칙 버전으로 PENDING INSERT만 허용한다. 입력·요청 identity 불변, update rowVersion 정확히 +1, 상태 전이 제한, terminal UPDATE/모든 DELETE 금지. 취소는 기존 token/만료/시작 시각을 유지하며, 만료된 실행의 종료는 FAILED/LEASE_EXPIRED만 허용한다.
- step trigger는 RUNNING·유효 lease의 부모 행을 잠그고 자기 전역 추출 슬롯의 유효 소유권을 검사한다. UPDATE/DELETE·종료/취소 이후 INSERT는 거부한다. VERIFIED는 네 단계 PASSED와 현재 정책·규칙 버전이 모두 필요하다. 현재 worker는 실제 두 scope가 부족하여 INCOMPLETE를 기록하며 VERIFIED를 만들지 않는다.
- `announcement_attachment_resource_leases`에 policy_validation_id FK/token을 추가하고 기존 job owner 필드의 NOT NULL을 완화하되 `ck_att_resource_owner`로 정확히 한 종류의 owner만 허용한다. QA owner는 EXTRACTION/GLOBAL/slot 1만 사용할 수 있다. 정상 job이 만료 QA 슬롯을 얻을 때 QA 필드를 NULL로, QA가 만료 job 슬롯을 얻을 때 job 필드를 NULL로 바꾼다. 살아 있는 다른 owner는 덮어쓰지 않는다.
- RUNNING claim과 공유 슬롯 획득은 같은 transaction이며 슬롯 실패 시 claim 전체 rollback. 만료 8분, 매 파일 실행 전 run/resource 모두 잔여 40초 이상 확인. 근거/완료/슬롯 해제는 run/token 조건으로 늦은 소유자를 차단한다. 파일 실행 중 긴 DB transaction은 없다.
- snapshot의 활성 지자체 대상 목록은 최근 수집 FAILED/QA 보류를 이유로 누락하지 않는다. 입력에는 URL/목록 profile 설정 원문 대신 hash만 저장한다. 이력 목록은 입력 JSON을 읽지 않으며 GET의 현재성 값은 DB 정책·규칙 버전 비교만 의미한다.
- 테스트 소유 PostgreSQL fixture는 queue/공유 슬롯/claim rollback/불변성/취소/다른 token/목록 범위/동시 claim/만료 회수/현재 버전 완료를 다룬다. 직접 작성한 네 단계 proof 행은 DB 제약 fixture이며 실제 네 가지 QA 실행 성공 증거가 아니다. SQL·잠금 검증은 Linux35050057694의 실제 임시 DB 결과로 갱신됐다. 전체 공식 파일·운영 정책 QA 성공은 아니다.

### 11.10 기존 데이터 배치 고정 범위

V73의 additive 변경이다. V72의 batches/jobs를 사용하며 별도 원문 복사 테이블을 만들지 않는다.

- batches에 scope_item_count(1~1000, maximum_count 이하)·policy_snapshot_json(object/최대 512 KiB)을 추가한다. 기존 행은 모두 NULL로 보존하며 관리 API는 고정 metadata가 있는 행만 대상으로 한다. CANCELLED 상태를 additive CHECK에 포함하고 `(id,policy_id)` UNIQUE를 추가한다.
- jobs에는 frozen_provider_code를 추가하고 `(batch_id,policy_id)` composite FK로 배치와 정책을 일치시킨다. 이 필드는 기존 불변 실행 trigger에 포함한다. 기존 일반 jobs의 NULL은 유지한다.
- 범위는 jobs로 물질화하며 SCOPE_READY에서 HTTP/worker 실행은 없다. `scope_json`에는 필터·provider/제외 집계·선택/잔여 수·상한/정책 hash만 저장한다. source/content/base/current/confirmation ID는 source 삭제 cascade되는 jobs에만 저장하며 URL·본문은 복사하지 않는다.
- source를 UUID 순서로 잠근 뒤 정책/규칙 공유 잠금과 scopeHash를 재검증한다. 생성 key advisory lock namespace 73105를 사용한다. 불일치/이미 존재한 활성 source job으로 일부 항목만 예약하지 않는다. 검수/current/source 버전은 유지한다.
- 관리 배치 identity·scope·정책 snapshot·최초 요청 불변, rowVersion +1, 삭제 건수 감소 금지, CANCELLED 되돌림 금지. 관리 batch DELETE는 거부한다.
- job 삭제 trigger가 deleted_item_count와 batch rowVersion만 증가시킨다. deferred constraint trigger는 `고정 건수=남은 jobs+삭제 건수`, SCOPE_READY/CANCELLED batch와 같은 jobs 상태, provider/실행 snapshot 존재를 검사한다. 삭제 원문을 새 항목으로 채우거나 일부 jobs만 저장할 수 없다.
- 기존 active job UNIQUE에는 SCOPE_READY가 포함된다. 수집 전 취소는 jobs를 CANCELLED로 바꿔 예약을 해제하며 source 데이터·기존 검수·이력을 삭제하지 않는다. 수집 제어는 11.11을 따르며 적용/rollback은 후속 필수 작업이다.
- 테스트 소유 PostgreSQL에 고정 대상·신규 원문 미포함·claim 금지/검수 유지, 취소/예약 해제/삭제 금지, 원문 cascade 건수, 동일 키 동시 생성, 버전/불변/미완료 상태 차단 5건을 추가했다. 해당 임시 DB의 fixture 초기화는 TRUNCATE로 수행하며 운영 DB를 입력받지 않는다. 해당 SQL·동시성 검증은 Linux35050057694의 실제 임시 DB 결과를 따른다. 운영 배치 적용은 별도다.

### 11.11 고정 배치 수집 승인·중지·재개와 진행 집계

V73의 추가 계약이다. batch에 collection_started_at/collection_approval_hash를 추가하고 기존 approved_by와 묶는다. 관리 batch의 SCOPE_READY/CANCELLED에서는 모두 NULL, 실행 이후에는 모두 필수다. 최초 SCOPE_READY → COLLECTION_PENDING만 허용하고 승인 identity를 덮어쓰거나 SCOPE_READY로 되돌릴 수 없다. 최초 실행은 삭제 0·정확한 scope/전체 상한·정책 snapshot/고정 입력 검증 및 jobs PENDING 전환과 원자적이다.

jobs.frozen_locator_hash와 `attachment_source_locator_hash(uuid)`는 현재 provider/notice/URL/local-source/parser 연결의 정규화 hash를 고정한다. URL을 jobs에 복사하지 않는다. 관리 jobs에는 이 hash가 필수이며 실행/기존 previous-evaluation/confirmation/policy/review binding은 불변이다. `attachment_batch_job_input_unchanged(uuid)`는 매 HTTP/claim에서 지문·이전 binding·보호 연결을 확인한다. 변경된 입력의 claim은 CONFLICT/FROZEN_INPUT_CHANGED로 끝나고 version 불일치의 기존 SOURCE_VERSION_CHANGED와 구분한다.

시작/재개 잠금 순서는 source UUID → 정책/규칙 SHARE → batch UPDATE다. 수집 중지는 batch만 잠그며 COLLECTION_PAUSED로 새 claim/HTTP를 막는다. 진행 중 요청/정리는 끝날 수 있다. 재개는 고정 최초 상한/현재 삭제 수와 미완료 jobs를 재검증하고 시도·예산·lease·checkpoint·terminal 결과를 초기화하지 않는다. 기존 source current/검수/정책 binding/운영 공고는 수정하지 않는다.

worker enabled일 때 DB 전용 집계가 15초 간격·변경 가능한 최대 100 batch/SKIP LOCKED로 진행한다. 처음 100개의 단순 대기가 나머지 완료 집계를 막지 않는다. 모든 고정 항목이 terminal이고 삭제 0·각 SUCCEEDED/SEALED set/preview evaluation/hash일 때만 COLLECTED, 나머지는 COLLECTION_PARTIAL_FAILED다. 중지/적용/rollback 단계는 자동 전환하지 않는다. 삭제된 원문은 건수만 보존하고 새 원문으로 채우지 않는다.

실제 PostgreSQL용 테스트 6건(고정 수집/저장·평가 분리, 중지/재개 예산·승인 보존, 입력/승인 우회 거부, claim 충돌 종료, 중지 중 삭제 집계, 동시 시작 1회)을 추가했다. 이후 실제 PG 실행은 Linux35050057694에서 확인했다. 배치 preview/선택은11.12, 적용·원복·전체 분할 계약은 후속 절을 따른다. 승인된 운영 배치 실행은 미완료다.

### 11.12 봉인 결과의 불변 미리보기·명시적 선택

V73의 additive 계약이다. preview/선택을 현재 source 데이터와 분리하며 V1~V72를 수정하지 않는다.

- `announcement_attachment_batch_previews`: batch FK, PREVIEW_READY/PREVIEW_PARTIAL_FAILED, scope/input/preview hash, 확정 후 batch version, 고정 전체/남은/삭제/선택 가능/선택 수, actor FK·UUID 멱등 키 UNIQUE/요청·사유 hash, 생성 시각. 고정 수=남은+삭제, 선택≤선택 가능≤남은, 전체가 준비된 경우에만 PREVIEW_READY CHECK를 둔다.
- `announcement_attachment_batch_preview_items`: `(preview_id,job_id)` PK, preview/batch composite FK, job/batch composite FK ON DELETE CASCADE, readiness/eligible/selected, 개별 input hash, 근거 metadata JSON object 최대64KiB. source ID 목록/원문/URL을 상위 batch 또는 preview 요약에 복사하지 않는다. item은 source 삭제 cascade되는 job에 종속된다.
- batches에 nullable current_preview_id를 추가하고 `(current_preview_id,id)`를 preview `(id,batch_id)`에 deferred FK로 연결한다. legacy V72 preview_hash에는 직접 FK를 걸지 않아 과거 행을 보존한다. 관리 PREVIEW_READY 계열에는 pointer/hash가 필수다.
- BEFORE trigger는 PREVIEW_RUNNING transaction과 정확한 다음 batch version에서만 preview/item INSERT를 허용한다. 모든 preview UPDATE/DELETE, 독립 item UPDATE/DELETE, 이전 preview에 item 추가를 거부한다. job 삭제 cascade일 때만 해당 item metadata DELETE를 허용한다.
- deferred trigger는 생성 transaction의 전체 항목 수·선택 가능/선택 수·batch pointer/hash/status/version을 검사한다. 새 snapshot 없는 job 선택 변경은 거부하고 current preview의 선택과 남은 전체 jobs의 선택을 대조한다.
- 생성/선택은 멱등 키73106 → source UUID 잠금 → 정책/규칙 SHARE → batch 잠금 순서다. 수집된 정확한 SEALED set/evaluation/extraction metadata를 읽고 새 파일/HTTP/추출 job을 만들지 않는다. 선택 변경에도 새 불변 preview와 hash를 만들고 batch version은2 증가한다.
- 근거에는 AUTO(base/이전 첨부/제안)와 CONFIRMED 태그를 분리한다. 기존 source/current/confirmation/정책 binding은 바꾸지 않는다. 삭제 뒤 snapshot 당시 수는 유지하고 현재 남은 수·버전 차이로 오래된 상태를 표시하며 식별자를 복원하지 않는다.
- 단위/HTTP/XML/정적 migration 계약과 PG용6건(생성/선택·기존 current 보존, 입력 변화, cascade, 불변/우회 거부, 동시 같은 키, 실패 항목 선택 거부)을 추가했다. 실제 임시 PG 실행은 Linux35050057694에서 확인했다. preview 이력은 실제 적용/원복 완료가 아니다.

### 11.13 배치 적용 승인·항목 CAS·복구 근거

- V73에 `announcement_attachment_batch_application_actions`를 추가했다. START/PAUSE/RESUME, same-batch preview FK, expected_version·scope/selected/deleted 수, actor·UUID key/requestHash/reasonHash를 불변 이력으로 남긴다. (batch,expected_version) unique 및 batch별 START unique, actor/preview FK 인덱스를 둔다. 기존 데이터에는 승인이나 새 scope를 소급 생성하지 않는다.
- batch.application_approval_id는 최초 START를 가리키며 승인 preview/hash를 이후 교체하지 않는다. 승인 없는 관리 배치 적용 상태는 CHECK로 거부한다. 접수와 전체 선택 PENDING/상태 전이는 deferred action-complete 검증으로 같은 transaction에 묶인다. 중지/재개도 해당 버전 명령 이력이 필요하다.
- jobs.application_preview_id, applied_source_version/applied_input_hash, application_error_code/application_attempt_count/application_next_attempt_at를 추가했다. 기존 applied_evaluation_id/applied_attachment_version·previous binding/confirmation을 함께 사용한다. 승인된 적격·선택 item만 적용 가능하고 terminal 결과/적용 근거는 불변이다. 수집 실패 코드와 적용 실패 코드를 분리한다.
- source/규칙·정책/batch 잠금 후 itemInputHash/CAS를 재검증한다. source current 첨부 평가·정책·검수 요구를 갱신하고 attachment_row_version만 증가시킨다. 이전 confirmation/current evaluation은 STALE지만 원문/확정 태그는 보존한다. 새 종합 확인 없이 이전 확인으로 전환할 수 없다. V1 계약·base 판정·운영 공고는 변경하지 않는다.
- 각 항목 transaction이 실패하면 전체 source 변경이 rollback되며 별도 transaction에서 30초 backoff·최대3회 적용 실패를 기록한다. 전체 고정 scope 중 비선택/실패/삭제가 있으면 배치 전체 APPLIED로 표시하지 않는다. 같은 키/동시 worker/중지/삭제와 후속 DRAFT 보호의 PG 사례를 Linux35050057694에서 실행했으며 운영 적용·원복과 구분한다.
- 원복의 확인 유효성은11.14, 배치 승인·항목별 실행은11.15, 일반 작업 동기 복구는11.17에 정의했다. 실제 PostgreSQL·운영 검증이 남아 있으므로 적용 metadata나 로컬 테스트만으로 운영 원복 완료라고 판단하지 않는다.

### 11.14 원복 후 기존 검수 확인의 유효 버전

V73의 추가 계약이다. `announcement_attachment_confirmation_restorations`는 job/source/confirmation과 복구 후 source_version/attachment_version, 적용 지문, 복구 actor·멱등 키·요청 hash·시각을 보존한다. 원래 confirmation의 버전/검수 시각/메모/태그를 재작성하지 않는다. source/job·source/confirmation composite FK는 원문 삭제에 cascade되며, job UNIQUE·source/attachmentVersion UNIQUE 및 actor/lookup 인덱스가 있다. 이력의 UPDATE·단독 DELETE는 금지한다.

- INSERT는 현재 적용된 batch job, 아직 NOT_REQUESTED인 rollback, 정확한 적용 후 source/첨부 버전·정책/current evaluation 및 이전 확인 binding을 확인한다. 원문→job 잠금, 후속 current 확인·보호 link·활성 다른 job 차단을 수행한다.
- 이전 확인의 원래 버전 또는 **이전에 완료한** 복구 버전이 job의 적용 전 기대 버전과 일치해야 한다. 무버전/이미 만료된 확인을 유효하게 승격하지 않는다. 이전 평가의 같은 base/content/rule/정책/SEALED set hash도 확인한다.
- 복구 버전은 base 버전 유지·첨부 버전 `applied_attachment_version+1`이다. deferred constraint가 이전 evaluation/current pointer·정책/review binding·confirmation current·job ROLLED_BACK의 동일 transaction 완료를 요구한다. 적용 후 전체 입력 지문과 관리자 승인은 11.15의 서비스에서 별도 검증한다.
- `attachment_confirmation_restored_binding(sourceId,confirmationId)`는 완료된 원복 근거의 최대 버전만 반환한다. CurrentMapper와 검수 조회·DRAFT Service가 동일 근거를 사용하며, 원래 검수 응답 필드는 그대로 유지한다. 첨부 버전이 다시 바뀌면 이전 복구 근거만으로 전환할 수 없다.
- 기존 확인의 false→true 전환은 현재 source 버전과 일치하는 복구 근거가 있어야 한다. 새 확인 INSERT/기존 확인의 STALE 전환/원래 확인 응답 멱등성은 유지한다. `is_current=true`만 바꾸는 것은 원복이 아니다.
- 복구 근거 생성은 11.15의 승인된 batch worker로 연결했다. 일반 job의 예약·적용 근거는11.16, 동기 원복 승인·실행은11.17에서 추가했다. 실제 PostgreSQL 실행 없이 DDL/트리거 통과로 표시하지 않는다.

### 11.15 배치 원복의 불변 승인·대상·원자적 복구

V73의 추가 계약이다. `announcement_attachment_batch_rollback_actions`에 batch당 하나의 승인과 최초 scope/적용 대상/적격/삭제/base 재개/확인 복구/취소 대기 수, expectedVersion·previewHash·actor·UUID key·request/reason hash를 저장한다. `announcement_attachment_batch_rollback_items`는 승인/같은 batch/job composite FK와 항목별 전체 inputHash·적격 사유·영향 flag를 고정하며 source 삭제 시 job과 함께 cascade된다. 승인/대상 수정·단독 삭제·승인 후 대상 추가는 금지한다.

batch.rollback_approval_id와 jobs.rollback_action_id가 실행을 연결한다. 승인 없는 원복 상태·범위 축소·terminal 결과 초기화를 거부한다. deferred approval-complete 검증은 승인 집계/전체 대상 예약/batch 버전+1/기존 PENDING 적용 취소를 같은 transaction에 묶는다. 기존 application_approval/current preview와 적용 결과 APPLIED는 보존하며 원복 결과로 바꾸지 않는다.

jobs의 rollback_error_code/rollback_attempt_count(0~3)/rollback_next_attempt_at와 restored_source_version/restored_attachment_version/restored_confirmation_id를 추가했다. 항목 worker는 source→batch 잠금, 승인 inputHash와 적용 직후 전체 live 지문 재검증, source CAS, 이전 evaluation/current 및 필요 시 confirmation 복구를 하나의 transaction으로 수행한다. 확인 없는 source는 base 경로로 복구 가능하고, 이전 확인이 이미 무효였다면 current 확인으로 되살리지 않는다. source 버전은 유지하고 첨부 버전은 applied+1이며 DB trigger가 최종 pointer/정책/review/confirmation/버전을 검사한다.

원복 실패 후 최대3회/30초 backoff를 별도 transaction에 기록한다. 전체 scope 대비 일부 복원·취소·미적용·삭제는 부분 결과다. 일반 ENFORCE job에는 managed batch 원복을 적용하지 않는다. 중복 `uq_att_job_source` ADD 선언1건을 제거하고 정확히 한 번 선언되는지 정적 회귀를 추가했다. 실제 PostgreSQL 실행이 생략된 상태에서는 그 정적 확인이 migration 성공을 뜻하지 않는다.

### 11.16 일반 ENFORCE 작업의 예약 전·적용 후 복구 근거

V73의 추가 계약이다. 일반 작업은 배치와 달리 예약 시 이미 이전 current/confirmation을 STALE로 만들고 첨부 버전을 증가시킨다. 따라서 기존 `expected_attachment_version`을 예약 전 버전으로 간주해서는 안 된다.

| 시점 | 원문 버전 | 첨부 버전 | 기록 |
|---|---|---|---|
| 예약 직전 | S | A | reservation_source_version / reservation_attachment_version |
| 예약 완료 | S | A+1 | expected_source_version / expected_attachment_version |
| ENFORCE 판정 저장 | S | A+2 | applied_source_version / applied_attachment_version / applied_input_hash |
| 정상 적용 원복 | S 유지 | A+3 | 일반 원복 승인/복구 transaction; 11.17 |
| 실패 예약 복구 | S 유지 | A+2 | 실패로 종료된 A+1 예약만 복구; 적용 성공으로 변경하지 않음 |

- `capture_attachment_normal_reservation` BEFORE INSERT가 원문 잠금 아래 직전 pointer/policy/review/confirmation과 요청을 대조하고 예약 버전·출처 locator 지문·이전 evaluation의 current 여부·이전 confirmation binding의 유효 여부를 고정한다. `COLLECT`, `RETRY_FILES`, `ROLE_CHANGE` 모두 같은 Job Mapper INSERT를 사용한다. 이미 어긋난 확인을 유효하게 승격하지 않고 원래 확인의 버전 또는 완료된 복구 버전, 같은 base/content/rule/policy/SEALED set hash를 대조한다. 이 flag만으로 전체 검수 또는 원복 가능성을 보증하지 않는다.
- `attachment_normal_job_application_hash(jobId,evaluationId)`는 불변 작업 입력·예약 전 연결·적용 시점 current/버전·출처/기관 metadata 지문·base/evaluation/set/policy/rule hash·current 확인·보호 link·다른 활성 작업 존재를 묶는다. 본문·첨부 텍스트·URL·검수 메모는 이력에 복사하지 않는다. 정책 ACTIVE/OFF/퇴역과 실행 횟수는 지문에 넣지 않으며 OFF 전환을 원복으로 간주하지 않는다.
- `updateJobCompleted`가 동일 lease-fenced transaction에서 일반 적용의 원문 버전과 지문까지 저장한다. BEFORE UPDATE는 실제 current/source/policy/버전/SEALED 근거·출처 locator·후속 확인/link 부재와 완료 lease를 대조한다. PENDING 작업에 판정만 채우거나 COLLECT_ONLY preview를 APPLIED로 승격할 수 없다. PARTIAL_FAILED에서 검수 의무가 적용돼도 파일 성공으로 표현하지 않는다.
- 예약 근거·이전 binding·APPLIED 근거와 set/preview는 수정할 수 없다. V72 과거 행에는 근거를 추정해 소급 생성하지 않는다. 정수 첨부 버전에 예약+완료 두 단계의 여유가 없는 경우 신규 예약을 거부한다.
- 일반 원복은11.17의 별도 승인으로만 가능하다. batch 승인 ID나 `rollback_status_code=ROLLED_BACK` 직접 변경은 거부한다. 실제 PostgreSQL migration/trigger 실행은 환경 차단으로 미검증이다.

### 11.17 일반 작업 원복·실패 예약 해소의 원자적 승인

V73의 추가 계약이다. `announcement_attachment_normal_rollback_actions`는 job당 하나의 불변 승인·복구 영수증이다. job/source composite FK·source 삭제 cascade, (id,job) unique, actor/source 인덱스, UUID idempotency key/request/reason hash를 둔다. 원문 사유는 감사 metadata에 복사하지 않는다. `normal_rollback_action_id`는 batch의 rollback_action_id와 서로 다르며 batch job에 연결할 수 없다.

- 모드 `APPLIED`: SUCCEEDED/PARTIAL_FAILED로 종료됐고 적용 근거가 현재와 일치하는 일반 작업이다. 원래 application_status_code/APPLIED와 applied_* 이력을 유지한다.
- 모드 `FAILED_RESERVATION`: application_status_code=PENDING인 FAILED/CONFLICT/CANCELLED 일반 예약이다. 현재 pointer 없음·예약 후 버전 일치·후속 작업 없음이 필요하다. job 실패/미적용 이력을 성공이나 APPLIED로 바꾸지 않고 rollback 결과만 별도 기록한다. 진행 중/재시도 대기/과거 근거 없는 작업은 복구하지 않는다.
- reservation_context_hash는 예약 당시 base/content/rule/current와 출처 locator·기관 metadata의 지문이다. 실패 예약도 본문 버전 숫자가 그대로인 기관 정보 변경을 감지한다. 원복 가능성 함수 `attachment_normal_job_recovery_state`를 API preview와 DB 승인 INSERT에서 공통 사용한다. mode/원문·첨부 버전/job version·지문·현재 binding·이전 평가/확인 유효성·base 경로 재개/확인 복원 여부를 고정한다.
- ADMIN 확인, 멱등 키73109 → source → job 잠금 후 다시 조회하고 승인 INSERT·source CAS·이전 evaluation current·필요한 confirmation current·job ROLLED_BACK·감사 기록을 하나의 transaction으로 처리한다. 승인 당시 모든 효과와 previewHash가 맞아야 하며 후속 검수/link/다른 활성 job·입력 변경은 거부한다. 현재 ACTIVE/OFF 여부로 이전 보호를 자동 해제하지 않는다.
- deferred complete trigger가 source 버전 유지·첨부 버전+1·이전 pointer/policy/review·evaluation/확인·영수증 연결을 대조한다. 승인만 저장하거나 부분 복원으로 끝낼 수 없다. 실패 시 전체 transaction을 취소하고 같은 요청 재시도가 가능하다. 원복 뒤 옛 job 재시작·영수증 수정/단독 삭제·복구 버전 변경은 거부한다.
- 일반 확인 복구는 별도 batch restoration 행을 위조하지 않는다. 완료된 normal action도 `attachment_confirmation_restored_binding`의 공통 결과에 포함한다. 원래 confirmation ID/버전/시각/태그는 보존하며 Current/Review/DRAFT가 증가한 유효 버전을 사용한다. 이미 무효인 이전 확인은 STALE이고, 이전 상태가 미확인 검수 대기였다면 그 보호를 유지한다.
- 단위/HTTP/Mapper 정적 검증과 PG12건을 추가했다. 실제 Linux/PostgreSQL·API 운영 실행·원복 화면 검증은 아직 없으므로 운영 복구 완료가 아니다. 원문/파일 이력·운영 공고·사용자 신청을 삭제하거나 자동 활성화하지 않는다.

### 11.18 전체 후보 목록 고정·분할 소속 — V74

V74는 로컬 additive migration이며 V1~V73을 수정하지 않는다. `announcement_attachment_backfill_runs`에 전체 필터/정책 snapshot, scope/candidate 지문, 최초 후보 수(bigint), 분할 크기(1~1000)/수(bigint), 삭제 수/버전, actor/멱등 키/요청·사유 지문을 둔다. 생성 transaction ID는 DB 내부 guard용이며 API에 반환하지 않는다. 정책 변경·수집/적용 승인·job 생성은 하지 않는다.

`announcement_attachment_backfill_segments`의 PK는(run_id,segment_no), `announcement_attachment_backfill_items`의 PK는(run_id,source_id), UNIQUE는(run_id,ordinal)이다. 순번은 collected_at/source UUID 순이며 분할=(ordinal-1)/segmentSize+1이다. 전체 후보 materialize SQL에는 LIMIT가 없고 원문/URL을 이력에 복사하지 않는다. 입력 지문에는 원문·첨부 버전, base/content/rule·current/검수 binding, 출처 지문, 시각/마감일이 포함된다.

item의(base_evaluation_id,source_id,content_version_id,rule_release_id) composite FK는 기존 base 평가를 참조하며 ON DELETE CASCADE다. 원문/base 삭제 뒤 식별자 tombstone 없이 item을 제거하고 segment/run 삭제 수·run 버전만 증가한다. run/segment 최초 분모·필터·지문은 불변이다. 직접 item 삭제, 입력·분할 이동, 최초 transaction 이후 추가 삽입, 관리 이력 삭제와 직접 삭제 카운터 조작은 거부한다.

최초 run INSERT에 deferred constraint를 한 번 실행해 전체 item 수·후보 지문·순번 유일성/범위·분할별 최초 건수·분할 수를 대조한다. 부분 materialize를 commit할 수 없다. 이후 삭제 카운터는 cascade trigger 안에서만 증가하여 최초 분모를 유지한다. API 조회도 실제 item 합과 run/segment 수를 대조한다. candidateCount=remainingItemCount+deletedItemCount이고 전체 삭제는 처리 성공이 아니다.

source/base FK·run 목록·분할별 순번 인덱스를 둔다. 한 REPEATABLE READ transaction의 고정/분할/item 생성은 실패 시 전체 rollback한다. 실제 SQL·Flyway·원자성 검증은 `AnnouncementAttachmentBackfillIntegrationTest`와 최신 migration upgrade 시험 대상이다. 단위/HTTP/Mapper 정적 통과와 실제 PostgreSQL 실행은 구분한다. 고정 분할→batch 연결과 전체 상태 집계의 로컬 계약은11.19에 추가했다. 실제 전체 범위 실행·최종 결과 대조는 필수 잔여다.

### 11.19 고정 분할과 batch 일대일 연결 — V75

`announcement_attachment_backfill_segment_batches`는(run_id,segment_no) PK와 composite FK, batch_id UNIQUE/FK, UUID 멱등 키 UNIQUE를 갖는다. 예약 전 삭제 수·확인한 목록 버전·분할 지문·actor/요청·사유 지문·생성 시각을 불변 영수증으로 보관한다. 원문/URL이나 source ID 복사 목록을 만들지 않는다. V1~V74는 수정하지 않는다.

- INSERT guard는 고정 run/segment의 버전·삭제 수와 새 SCOPE_READY batch의 정책 snapshot·actor/key·scope_json의 backfillRunId/backfillSegmentNo를 확인한다. 이미 생성된 일반 batch를 임의로 연결하거나 실패/취소 연결을 교체·삭제하지 못한다.
- batch 최초 대상 수+예약 전 삭제 수=분할 최초 수, batch 삭제 수+예약 전 삭제 수=분할 삭제 수를 대조한다. item↔job의 source/content/base/rule/provider 소속을 양방향 확인한다. 일부를 빠뜨리거나 다른 원문으로 채운 동일 건수 배치도 거부한다.
- marked batch와 연결 영수증이 같은 transaction에 있어야 한다. batch/job/link의 deferred constraint가 cascade 완료 후 현재 잔여 소속과 건수를 검증한다. 먼저 job이 삭제되거나 먼저 ledger item이 삭제되는 중간 순서를 완료 상태로 판정하지 않는다.
- 삭제 카운터 갱신은 source→run→segment 순서를 유지하도록 V75에서 함수를 교체한다. V74의 최초 분모/입력 불변·삭제 수 증가 제약은 유지한다. 예약은 요청 키75001→기존 batch 키73105→source→정책→run→segment→신규 batch/job/link 순서다.
- `attachment_backfill_batch_input_unchanged`는 linked job의 고정 inventory 입력을 검사한다. 기존 `attachment_batch_job_input_unchanged`의 출처·이전 pointer/policy/review/confirmation·운영 link 보호를 모두 유지하면서 이 검사를 추가한다. 기존 claim/매 HTTP guard와 수집 시작·재개의 실행 입력 조회에 연결한다.
- 전체 요약은 고정 item LEFT JOIN link/job으로 잔여·미예약·누락을 구분한다. collection/application/rollback 각각의 전체 분모를 확인하고 source 삭제 수는 별도 유지한다. 새 status로 성공을 합성하거나 source·운영 공고·정책을 자동 변경하지 않는다.

최신 migration/실제 PG 검증은 V74→V75 upgrade와 `AnnouncementAttachmentBackfillIntegrationTest`의 1001건 분할 예약·전후 삭제·입력 변경·불변 연결·경합을 포함한다. 작성된 시험의 조건부 생략은 통과가 아니며 실제 승인 범위 적용과 운영 브라우저 검증은 별도 필수다.

### 11.20 정책 게시 전 영향 관측 — DDL 변경 없음

V72의 규칙별 ACTIVE unique 계약과 정책 binding/job, V73의 고정 collection plan/QA 이력을 읽는다. REPEATABLE READ/readOnly의 단일 관측에서 같은 규칙 ACTIVE0~1건·해당 규칙/전체 규칙 집계·최신 QA를 대조한다. source/job/plan을 별도 집계하여 join 증식을 방지하고 원문/job은PRODUCTION, plan은 원래 collection request의PRODUCTION만 포함한다.

현재 ACTIVE 규칙의 OFF 정책은 다른 규칙의 고정 작업까지 새 외부 요청을 막는다. 따라서 matchingRule과 allRules는 별도 포함 관계로 표시한다. 원문 바인딩/검수 요구/현재 첨부 pointer/운영 link, 수집 예약·RUNNING/적용 대기/원복 대기, FROZEN 계획 누적 이력을 구분한다. link는EXISTS, 상태 집계는FILTER를 사용하며 count에는LIMIT가 없다. 일부 집계를 전체로 반환하거나 QA를 운영 수에 섞지 않는다.

관측 지문은 source ID/개별 버전 전체의 scope 고정이 아니다. source membership 변경이 같은 집계 수로 남을 수 있어 게시 CAS/승인 토큰으로 사용할 수 없다. 최신 QA의 단계 상태·hash를 표시하되 실제 evidence/코드·설치·전체profile 재검증이나 게시 transaction을 실행하지 않는다. 상세 계약은 `announcement-attachment-policy-publication-impact-2026-09-12.md`와 API24.23을 따른다. 실제 PG 사례는 Linux35050057694에서 실행했다. 운영 규모 실행계획/시간 검증은 미완료다.

### 11.21 게시 준비 범위의 불변 원장 — V76

`announcement_attachment_policy_publication_scopes`는 정책·규칙 ID/버전/모드와 선택적 QA ID/hash, 봉인 상태·전체 수/hash, actor·멱등 키·사유/요청 지문, 생성/만료 시각을 보관한다. `announcement_attachment_policy_publication_scope_items`는 scopeId/type/entityId 복합 PK와 stateHash다. V1~V75는 이 증분에서 변경하지 않는다.

POLICY(선택 DRAFT·모든 ACTIVE/RETIRED), SOURCE(정책 binding PRODUCTION), JOB(수집/적용/배치 원복 대기 PRODUCTION), COLLECTION_PLAN(PRODUCTION FROZEN 누적), COLLECTOR(활성 지자체 시스템 설정)를 DB 함수로 모두 고정한다. 서로 다른 차원의 합계는 공고 수나 예상 HTTP 수가 아니다. entityId에 원문 FK를 걸지 않아 원문 삭제 이후에도 비식별 ID/hash 감사 이력이 남는다. 원문·첨부 텍스트·URL·개인정보는 복사하지 않는다.

OPEN→전체 INSERT→SEALED는 같은 생성 xid만 허용한다. root/항목은 이후 수정·삭제 불가다. deferred constraint가 commit 전 정책·규칙 버전/모드, 실제 count/hash와 양방향 EXCEPT를 검사한다. 누락·추가·동일 수 다른 ID/상태를 거부한다. scope hash는 정렬한 모든 항목과 정책/규칙/QA binding을 포함하며 관측 집계 hash와 구분한다. 유효시간 API10분/DB최대15분이다.

준비는 REPEATABLE READ 단일 snapshot이며 전역 게시 잠금을 대신하지 않는다. 최종 게시에서 최신 QA/설치/전체 profile·동시 쓰기·승인 범위를 재검증해야 한다. 정책 게시·기존 데이터 적용은 수행하지 않는다. 실제 PG 사례 및 V75→V76/빈 DB migration은 Linux35050057694에서 검증했으며 상세 계약은 `announcement-attachment-policy-publication-scope-2026-09-12.md`에 있다.

### 11.22 정책 게시 영수증과 원자적 교체 — V77

`announcement_attachment_policy_publications`에 게시 ID, scopeId UNIQUE/FK, policyId UNIQUE/FK, 게시 버전/hash·검증된 runtimeHash, 이전 정책 ID/교체 전 버전, QA ID/evidenceHash, actor/멱등 키/요청·사유 hash, 생성 xid·게시 시각을 불변 저장한다. 입력 원문·URL·비밀번호를 저장하지 않는다. V1~V76는 이 증분에서 변경하지 않는다.

INSERT trigger가 동일 관리자 준비·만료·DRAFT/ACTIVE 규칙·최신 VERIFIED QA/네 단계 PASSED metadata·양방향 전체 범위 차집합을 검사한다. 이 DB metadata 검사는 Java의 실제 근거 재검증을 대신하지 않는다. 이전 ACTIVE ID/버전과 새 게시 버전/hash/runtimeHash도 준비·QA에 일치해야 한다. 이후 UPDATE/DELETE는 거부한다.

서비스는18개 관련 테이블 EXCLUSIVE NOWAIT와 READ COMMITTED/15초 안에서 영수증 → 이전 ACTIVE 퇴역/버전+1 → DRAFT ACTIVE/버전+1 → 감사 metadata를 저장한다. policyHash는 승인된 QA snapshot hash이며, settings_json.extractorConfigHash에 QA 설치 지문만 고정한다. deferred constraint는 새 ACTIVE/이전 RETIRED·정확한 버전/hash/게시 시각·runtime 및 그 외 QA 설정 불변을 검사하여 부분 commit을 막는다. source/job/배치/운영 공고의 상태를 자동 변경하지 않는다.

09-12 코드·기관·독립 QA 지문 보강은 schema migration 없이 QA input JSON의 내부 schemaVersion5·installed.executionCodeHash·installed.workerDbQa·providerQaPlan과 지자체 target의 publicCode를 사용한다. workerDbQa는 독립 QA artifact/code/runtime 지문·네 suite별 수·정렬한 전체 case ID hash 목록이다. providerQaPlan은 기업마당/정부24/활성 지자체 전체의 기관·목록 parser·첨부 프로필 결합/미구현/불일치/중복 및 최소 검증 요구량이다. URL/설정 원문을 넣거나 실제 성공으로 표시하지 않는다. 누락/중복 기관 identity는 거부하고 같은 UUID의 기관 코드 변경도 과거 QA를 무효화한다. 전체 main class/resource 실제 바이트·독립 inventory 검증과 실제 QA 실행은 DB transaction 밖에서 수행한다. 게시 잠금 안에서는 정책/scope/최신 QA run/단계와 입력 snapshot이 검증 당시와 같음을 재확인한다. 과거 schema1~4 QA는 재작성하거나 새 코드의 성공 근거로 재사용하지 않는다. WORKER_DB_RECOVERY의 실제 Linux/PG 부모 연결·취소·정리는 Linux35050057694에서 확인했다. 전체 공식 Provider QA는 미완료다. 상세는 `announcement-attachment-policy-qa-bridge-2026-09-12.md` 및 `announcement-attachment-provider-qa-scope-2026-09-12.md`를 따른다.

정책별 게시 영수증은 하나이며 후속 개정은 새 정책을 사용한다. 잠금은 일반 SELECT를 허용하되 DML/row-lock 업무와 충돌하며 사용 중이면 게시가 즉시 실패한다. PG 게시 경합과 V76→V77/빈 DB 시험은 Linux35050057694에서 검증했다. 운영 규모 성능·전체 공식 QA·실제 운영 게시는 잔여다. 상세는 `announcement-attachment-policy-publication-2026-09-12.md`와 API24.26을 따른다.

### 11.23 정부24 첨부 저장 코드 정합성 — V78

원문·기존 Provider·제외 tombstone의 실제 코드는 `GOV24_PUBLIC_SERVICE`다. V78은 `announcement_attachment_jobs.frozen_provider_code`와 `announcement_attachment_backfill_items.provider_code`의 CHECK 허용 목록에 이 코드를 추가한다. 기존 `GOV24` 이력 값의 허용은 유지하며 데이터·FK·불변 trigger·기존 migration은 변경하지 않는다.

배치/전체 목록 필터의 API 별칭 `GOV24`는 Mapper 바인딩에서만 실제 코드로 변환한다. 저장되는 후보/항목/job, 정책 registry 및 QA 대상은 실제 코드를 유지한다. 원문 컬럼 변환이나 전체 데이터 UPDATE는 없다. 이전 요청/고정 이력은 재작성하지 않고 신규 미리보기부터 대상/지문을 재검증한다. V77→V78 및 빈 DB·정부24 코드의 전체 목록 materialize 시험은 Linux35050057694에서 실행했다. 정부24 외부 API/파일 수집 성공을 뜻하지 않는다. 상세는 API24.27 및 `announcement-attachment-gov24-provider-contract-2026-09-12.md`를 따른다.

### 11.24 Provider QA 분할 실행 원장 — V79

`announcement_attachment_provider_qa_runs`는 현재 DRAFT 정책/규칙 버전과 snapshot/catalog/전체 업무 코드/runtime 지문, 전체 요구 범위, 예상 항목 수와 합산 요청/byte 상한을 고정한다. `announcement_attachment_provider_qa_cases`는 순번·공고별 입력/profile 지문·예상 파일 수·상한과 소유 lease, 누적 사용량·안전한 결과 metadata를 저장한다. 제목/URL/파일 원문/추출문/기대 문구는 이 두 실행 원장에 저장하지 않는다.

- run BUILDING 생성→전체 case 입력→READY 봉인은 같은 transaction으로 완료해야 한다. 순번·개수·요청/byte 합계가 다르면 봉인할 수 없다. 단독 BUILDING commit, 항목 추가·삭제, 고정 입력 변경은 금지한다.
- 전체 활성 run은1개, RUNNING case도1개다. case lease는 최초 최대8분/부모24시간 유효기간 이내이며 연장·재claim하지 않는다. 만료는 FAILED/LEASE_EXPIRED로 남기고 늦은 응답을 거부한다. 재시도는 새 고정 계획이 필요하다.
- 요청/byte 예약은 부모 lock 아래 case/run에 원자적으로 누적하며 환급하지 않는다. 살아 있는 DOWNLOAD와 HOST 소유권이 필요하다. 기존 resource lease에 Provider case 소유자 두 컬럼을 추가하고 일반 job/정책 QA와 다운로드2·호스트1·추출1 슬롯을 공유한다. 만료 슬롯 인수 시 다른 종류의 이전 소유자 필드를 비운다.
- 원본 정리·입력/profile/runtime 지문·파일 분모·누적 사용량이 맞는 결과만 PASSED가 가능하다. 취소·현재 정책/규칙 변경·만료는 성공보다 우선한다. 모든 case가 끝나기 전 run을 종료하거나 분모를 줄일 수 없다.
- COMPLETED는 고정 case 전체의 기대 동작 일치다. 전체 수집원 QA, 정책 VERIFIED/게시, ENFORCE, 원문/운영 공고 갱신을 뜻하지 않는다. 전체 catalog/예약 API/정책 verifier 연계는 잔여 작업이다.

V1~V78은 변경하지 않는다. V78→V79/빈 DB migration과 실제 PostgreSQL 원장 사례는 Linux35050057694에서 실행했다. 상세 계약·검증은 `announcement-attachment-provider-qa-ledger-2026-09-12.md` 및 API24.28을 따른다.

### 11.25 Provider QA catalog snapshot — DDL 변경 없음

정책 QA input JSON의 내부 schema6는 전체 요구 목록과 고정 catalog 계획을 함께 저장한다. catalogVersion/catalogHash/scopeHash, 모든 target의 결합 상태·정상 공고/형식 기대 coverage와 모든 case의 준비 상태·입력 지문·기대 파일 수, 전체 분할 목록/상한을 포함한다. 원문 URL/제목/추출문/기대 문구와 실행용 Prepared.inputs는 저장하지 않는다. 기대 파일 수 미확인은 null이며, 한도 초과나 준비 실패를0개로 바꾸지 않는다.

2026-09-15 새 catalog의 COMPLETE_TEXT 입력에는 역할 규칙·역할/사유·텍스트/블록/전체 assessment 지문을 고정한다. V79 `evidence_json`의 파일 metadata에는 선택적 `roleAssessmentHash`만 추가한다. 기존32KiB 제한과 원장 불변·inputHash/codeHash 결합을 유지하며 기존 행/과거 누락 필드는 재작성하지 않는다. 새 역할 기대값 없는 완전 추출 입력은 실행 준비 불가다. 역할 근거 위치의 실제 값/원문은 QA 원장에 복제하지 않으며 DDL/migration 변경은 없다.

schema1~5 이력은 수정하지 않는다. 현재 catalog/규칙/runtime/전체 scope·준비 상태가 다르면 snapshotHash가 달라져 이전 QA를 재사용할 수 없다. V79 예약·서비스·검증기의 후속 구현/실행 상태는 장기 진행 기록을 따르며, catalog의 기대 coverage를 실제 QA PASSED로 저장하지 않는다. 공식 catalog는2026-09-16 충주 추가 후30참조/실행 기대값1이며 정상 기대 공고0이다. 정상3공고·전체 기관 분모와 실제 제공 형식/미확인 적용성을 구분한다. 참조 추가는 DDL·운영 데이터 변경이 아니다. 상세는 API24.29 및 `announcement-attachment-provider-qa-catalog-2026-09-12.md`를 따른다.

### 11.26 Provider QA 승인 분할 계획 — V80

2026-09-15 새 catalog schema2의 전체 formatCoverage와 대상별 formatApplicability를 기존 snapshot6/plan 지문에 포함한다. 각 대상에서 모든 기대 파일의 제공 형식·미관측·정상 다중 첨부 수를 분리하며 전체3형식·기관별 정상3건·정상 다중 첨부1건을 검증한다. OCR/부분 실패 형식을 분모에서 지우거나 미관측을 미지원으로 판단하지 않는다. schema1 catalog 파일과 null 신규 필드의 직렬화/hash 표현, V1~V83/운영 행은 보존한다. 신규 DDL은 없고 설치 코드/리소스 및 catalog/plan 변경으로 과거 QA 재사용을 차단한다. 이 metadata는 기대값이며 실제 PASSED 원장을 생성하지 않는다. API는24.30의 새 `/execution-plan/targets`를 따른다.

`announcement_attachment_provider_qa_run_plans`는 run에 1:1로 planHash, 분할 번호/전체 분할 수, 전체 catalog/실행 가능 case 수, 기대 coverage 여부, 여유 포함 시간 상한을 봉인한다. 기존 V79 run 컬럼·V1~V79 migration은 수정하지 않는다. 전체 분모>=실행 가능 분모>=선택 분할 case 수이며, 시간 상한은 모든 case의 maximum_seconds+60 합계와 정확히 같고23시간 이내여야 한다.

V80 이후 run INSERT의 deferred constraint가 같은 생성 xid의 계획/전체 case/READY 원자성을 검사한다. 계획은 생성 이후 수정·삭제할 수 없다. 과거 V79 이력은 변경하지 않으며 LEFT JOIN 조회에서 계획 정보가 null인 이력을 보존한다. 계획 없는 과거 활성 run은 새 실행 근거로 쓰지 않는다.

관리 Service가 현재 DRAFT 정책·규칙·snapshot/catalog/계획과 관리자의 정확한 분할/예산 확인을 대조하고, 짧은 queue→규칙/정책 잠금 아래 원장·계획·case·READY·감사 metadata를 함께 저장한다. 같은 actor/정책/입력/멱등 키는 OFF/간격 검사 전에 이력을 반환한다. 새 예약은 같은 정책에서60초 간격이며 전체 분할 실행을 하루3회로 축소하지 않는다.

기본 OFF 스케줄러는 승인된 분할 전체를 현재 catalog에서 재구성하고 전체 case 순서·입력/파일/상한을 대조한 후 한 case씩 실행한다. 변경·취소·만료와 원본/자원 정리를 기존 원장 계약에 연결하며 정책 VERIFIED/게시·source/job/운영 공고 활성화를 대신하지 않는다. 실제 PG 계획 사례·V79→V80/빈 DB 검증은 Linux35050057694에서 실행했다. 관리 API와 잔여 검증은 API24.30 및 `announcement-attachment-provider-qa-management-2026-09-12.md`를 따른다.

### 11.27 Provider QA 전체 실행 근거 조회 — DDL 변경 없음

새 내부 Evidence DAO/Mapper는 같은 정책/snapshot/catalog/승인 plan의 분할별 최신 시도를 전체 상태에서 선택한다. COMPLETED만 미리 필터링해 최신 실패를 건너뛰지 않는다. 각 run의 required_scope_json과 전체 case count/100개 페이지를 읽어 현재 전체 계획·순번·파일 분모와 대조한다. 응답 API에는 이 증거 JSON을 노출하지 않는다.

파일별 재검증기는 불변 case/run·현재 고정 입력과 실제 출력 metadata/hash/품질·요청/byte 합계·원본 정리·소유 시간의 일치를 요구한다. 전체 집계는 모든 분할·target·case/file 수와 정렬된 ID/digest를 보존한다. 실제 실행 순서와 분할 번호순을 구분하고, 정책 QA 이후 새로 완료된 증거로 과거 정책 QA를 소급 통과시키지 않는다.

실제 PostgreSQL2사례(최신 대기/실패 우선, case 전체 페이지/실제 시각/hash/미실행 보존)는 Linux35050057694에서 실행했다. 정책 단계/게시 연결과 잠금 확장은 아래11.28을 따른다. 상세는 API24.31 및 `announcement-attachment-provider-qa-evidence-2026-09-12.md`를 따른다.

### 11.28 Provider QA 정책 연결·게시 경합 잠금 — V81

V81은 `attachment_policy_publication_lock()`의 기존18테이블/순서를 유지하고 `announcement_attachment_provider_qa_runs`, `announcement_attachment_provider_qa_cases`, `announcement_attachment_provider_qa_run_plans`를 EXCLUSIVE NOWAIT 잠금에 추가한다. 일반 SELECT는 허용하며 DML/행 잠금과 충돌하면 즉시 거부한다. 기존 V1~V80/이력은 수정하지 않으며 정책·운영 데이터 쓰기는 없다.

정책 QA는 전체 Provider 근거와 세 나머지 단계가 모두 저장된 PASSED일 때만 VERIFIED를 기록한다. 게시 전 전체 재검증은 잠금 밖에서, 최신 run 소속·버전·상태 대조는 기존 게시 transaction 잠금 안에서 수행한다. 새 시도/실패/대기가 생기면 이전 성공으로 게시하지 않는다. V80→V81 및 새 DB 검증·읽기 허용/쓰기 차단/역방향 NOWAIT 경합 시험은 Linux35050057694에서 실행했다. 운영 정책 게시와 전체 Provider QA는 별도다.

### 11.29 3단계 자동 분석 상태 조회 — DDL 변경 없음

`AnnouncementAttachmentCurrentMapper`의 기존 현재 base/evaluation/SEALED set·확인 복구 binding·최신 일반 job projection에 `active_normal_job`을 추가한다. source별 일반 작업 전체의 SCOPE_READY/PENDING/RUNNING/RETRY_WAIT/PAUSED 존재 여부이며 batch_id가 있는 미리보기는 제외한다. 최신 종료 작업만 조회해 다른 활성 작업을 놓치지 않도록 기존 검수 쓰기 guard와 동일한 조건을 사용한다.

`AttachmentProcessingFlow`는 같은 REPEATABLE_READ 조회 결과에서 처리 흐름을 계산한다. source/판정/확인 이력·원본·운영 공고를 수정하거나 중복 상태를 저장하지 않는다. 현재 결합 누락·이전 근거·기술 실패·수동 확인·미리보기를 분리하며 새 migration은 필요하지 않다. v2의 additive `processingFlow` 계약은 API24.32를 따른다. 후속 대기열 필터는 아래11.30을 따른다.

### 11.30 최종 검증 대기열 검색 — DDL 변경 없음

현재 Mapper의 `ProcessingFlowStatus` CASE가 `AttachmentProcessingFlow`의9상태/우선순위를 SQL 검색으로 표현한다. `ActiveNormalJob`와 `AttachmentStale` fragment를 projection/필터에서 공유한다. 현재 source/base/rule/policy/SEALED set/복구된 confirmation 결합은 변경하지 않는다.

`processingFlowStatusCode`는 `#{processingFlowStatusCode}`로 바인딩하며 list/count의 `SearchWhere`를 공유한다. count의 분모는 페이지 내 행이 아닌 전체 검색 결과이고, 기존 제목 제외/QA 제외·provider·태그·기간 범위와 같은 transaction snapshot이다. 새 판정 상태를 DB에 중복 저장하거나 migration을 추가하지 않는다.

검증은 API 입력·Mapper 바인딩·UI 계약과 실제 임시 PostgreSQL9상태/29행·5개 페이지 크기의 SQL/Java projection 대조를 구분한다. 테스트 작성 자체는 SQL 실행 성공이 아니다. 실제 실행 상태는 장기 진행 기록을 따른다.

### 11.31 추출 텍스트 역할 근거 — V82

2026-09-15 양식 표식 보완은 `document-role-1.0.2`다. 기존 APPLICANT_FIELD/SIGNATURE_FIELD 코드와3개 FORM 근거 shape를 재사용하므로 추가 DDL은 없다. 규칙 버전/지문은 새 정책의 검증 대상으로만 기록하고, 기존1.0.1 정책·파일·불변 assessment·checkpoint를 업데이트하거나 새 규칙으로 재해석하지 않는다. 상세는 [양식 역할 증분](announcement-form-role-markers-2026-09-15.md)을 따른다.

`announcement_source_attachment_files`에 nullable `role_extraction_id uuid`, `role_assessment_json jsonb`를 추가하고 `role_origin_code`에 TEXT_RULE을 허용한다. 기존 UNKNOWN/PROFILE/MANUAL·봉인 이력·V1~V81은 변경하지 않는다. assessment가 없는 과거 행은 자동 판정 성공으로 채우지 않는다.

- 복합 FK `(role_extraction_id,id,set_id,source_id)`가 정확한 extraction/file/set/source를 묶는다. file→extraction 삽입 순서를 위해 DEFERRABLE INITIALLY DEFERRED다.
- assessment는 최대32KiB·정확한7필드(ruleVersion/rulesHash/textHash/blocksHash/roleCode/reasonCode/evidence)이며 evidence는 고정 규칙 코드와 blockIndex/startOffset/endOffset만 최대100개다. 원문은 기존 extraction에만 저장한다.
- commit 시 동일 정책의 역할 규칙 버전·SHA-256, 최신 COMPLETE_TEXT extraction, 실제 텍스트 지문, 전체 block 범위·중복·신뢰 좌표를 확인한다. block 지문은 UTF-8 메타데이터 Base64·코드포인트 정수·고정 구분자를 사용하는 `attachment-role-blocks-v1`이다. JSON 공백/키 순서에 의존하지 않는다.
- TEXT_RULE의 적용 역할은 assessment와 같아야 한다. UNKNOWN은 역할 미확정이며 성공 후보로 승격하지 않는다. MANUAL로 수정한 새 파일은 재사용 추출의 원래 assessment를 그대로 보존하고 새 extraction ID를 참조한다. OPEN 상태도 이미 기록한 근거/역할을 덮어쓰지 못한다.
- 새 정책 초안에만 `roleRuleVersion`/`roleRulesHash`를 서버가 고정한다. 기존 두 값 없는 정책·실행 snapshot은 자동 적용하지 않는다. 새 snapshot/checkpoint/manifest에 근거가 포함되며 과거 null 필드는 직렬화에서 생략해 기존 지문 형식을 유지한다. 규칙 불일치·오염된 checkpoint는 거절한다.
- 최초 수집 및 선택 재시도는 실제 추출을 다시 판정한다. 비선택 재사용과 관리자 역할 변경은 원문을 다시 요청하지 않고 기존 동일 근거를 복사한다. 자동 판정으로 MANUAL/PROFILE을 덮어쓰지 않는다.

검증 대상: 기존 checksum/빈 정책 유지, 실제 HWPX→worker→PG→v2, Unicode block 지문, 잘못된 버전/지문/좌표 거절, 재시작 checkpoint, 관리자 복제 근거 보존이다. 실행 결과는 장기 진행 기록을 따른다. migration 생성과 운영 적용은 별개이며 공식 파일 정확도·전체 Provider QA와 게시 승인은 남아 있다.

### 11.32 전체 분할 소속 검증의 집합 대조 — V83

도입 당시 1,001건 전체 분할 시험의 Linux 독립 실행 실패와 같은 SHA 주 시험82초를 관측했다. V75의 지연 trigger가 각 job 행에서 전체 분할에 대한 양방향 상관 EXISTS를 반복하는 처리 비용을 줄이는 것이 목적이다. 시간/행 수 제한이나 무결성 검사를 완화하는 변경은 아니다. 이후 Linux35050057694에서 전체 분할·동등성 시험과 독립 실행·정책 부모 연결을 통과했다. 과거 실패의 근본 원인이나 운영 성능 개선율을 이 결과만으로 단정하지 않는다.

- 기존 `check_attachment_backfill_segment_batch()` 함수만 additive V83에서 교체한다. source/content/base/release/provider의 다섯 값으로 정렬한 실제 전체 tuple 배열을 직접 비교한다. 해시 비교가 아니며 중복·NULL·누락·다른 소속·입력 순서의 의미를 보존한다. 각 분할은 기존1~1000건 제한을 유지한다.
- 기존 batch/receipt/job 지연 trigger, 삭제 전후 분모, 전체 job count, 표시된 batch의 필수 receipt, 기존 예외/SQLSTATE23514, source cascade 및 불변/잠금 규칙은 유지한다. 이력/데이터/기존 V1~V82를 수정하지 않는다.
- 검증은 이전 양방향 EXISTS와 새 배열 비교의 양성/음성·순서/중복/NULL/5개 tuple 변경 동등성, V82→V83 함수 교체와 지연 trigger 유지, 기존1,001건 전수 예약·경합·삭제·원복 시험이다. 최신 직접 확인한 Linux 시험 결과는 상단 근거 문서에 기록하며, API/UI 계약과 운영 승인 범위는 바뀌지 않는다.
