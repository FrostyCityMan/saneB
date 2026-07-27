# saneB Backend DB Model v1

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
| `announcement_source_links` | `source_snapshot_id`, `announcement_id`, `linked_by`, `linked_at` | PK `id`, FK `announcement_source_snapshots.id`, FK `announcements.id`, FK `users.id` | UQ `source_snapshot_id`, UQ `announcement_id` |

외부 공고 원문은 `announcement_source_snapshots`에 보존하고, 실제 매칭에 사용하는 운영 공고는 기존 `announcements`와 조건 테이블에 운영자가 별도로 입력한다. 하이라이트는 검수 참고용이며 `announcement_numeric_conditions`, `announcement_option_conditions`, `announcement_industry_conditions`에 자동 저장하지 않는다.

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
