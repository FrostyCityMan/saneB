# 공고 수집 분류·다중 태깅 상세 설계

- 작성일: 2026-08-11
- 상태: 로컬 구현·검증 완료, 운영 적용 보류
- 설계 기준선: `master`, `d0b3eb3`, 당시 최신 migration `V62`
- 로컬 구현 기준: additive migration `V63`~`V68` (운영 DB 미적용)

## 1. 현재 단계와 Gate

- [x] 클라이언트 지원대상 명칭과 수집 조합 정책 확정
- [x] 그룹 A·B 정책과 그룹 B 우선순위 확정
- [x] 기관명 및 광범위 단독 제외어 처리 확정
- [x] 첨부파일 신규 수집·저장·다운로드·추출·분류 완전 제외 확정
- [x] 현재 Flyway·Controller·ServiceImpl·Mapper·화면·테스트 기준선 확인
- [x] DB·API·판정 엔진 상세 설계 초안 작성
- [x] 초기 키워드 release DRAFT seed와 Golden QA 검증
- [x] 구현 승인
- [x] migration·코드·화면 구현
- [x] 로컬 테스트·`bootJar`·PostgreSQL Flyway 최종 검증
- [ ] 격리 QA 및 운영 적용

이 문서는 설계 계약과 로컬 구현 결과를 함께 기록한다. DB의 최종 source of truth는 기존 migration을 수정하지 않고 추가한 `V63`~`V68`이며, 이 상태 표시는 운영 DB 적용이나 규칙 활성화를 뜻하지 않는다.

## 2. Design Read

- 주요 사용자: 공고 수집을 운영하는 `OPERATOR`, 검수 상태를 확인하는 `APPROVER`, 키워드 정책을 관리하는 `ADMIN`
- 사용자 목표: 필요한 지원 공고는 놓치지 않으면서 무관한 공고를 설명 가능한 규칙으로 분리한다.
- 빈번한 과업: 검수대기 확인, 자동 제외 근거 확인, 키워드·유의어 관리, 운영 공고 DRAFT 전환
- 실패 비용: 높음. 잘못된 자동 제외는 지원 공고 누락을 만들고, 잘못된 통과는 운영 검수 비용과 사용자 혼선을 만든다.
- 위험 등급: `R2`. 규칙 release 활성화는 이후 대량 수집 결과에 영향을 주므로 미리보기, 명시적 확인, 버전, 감사 이력이 필요하다.
- 입력 환경: 관리자용 데스크톱 우선, 360px 모바일에서도 조회·상태 확인 가능
- 접근성·언어: 한국어 우선, WCAG 2.2 AA, 키보드 조작과 색상 외 상태 라벨 제공

한 줄 설계 방향은 다음과 같다.

> 평면 키워드 필터를 버전형 규칙 release와 구조화된 판정 근거로 교체하고, 자동화 결과는 관리자 승인 전 운영 공고가 되지 않게 한다.

## 3. 목표와 비목표

### 3.1 목표

1. 기업마당·정부24·지자체 수집 결과에 동일한 분류 엔진을 적용한다.
2. `지원대상 키워드 AND 지원유형 키워드`를 기본 후보 조건으로 사용한다.
3. 제목과 본문을 분리 판정하고, 적용 위치와 일치 규칙을 보존한다.
4. 그룹 B 제목 일치를 그룹 A보다 우선해 자동 제외한다.
5. 자동 제외 원문도 DB에 보존해 근거를 조회할 수 있게 한다.
6. 지원대상과 지원유형을 복수 태깅한다.
7. 관리자가 키워드·유의어를 관리하되 활성 규칙을 직접 덮어쓰지 않게 한다.
8. 기존 `/api/v1`, 단일 `target_type_code`, V56 데이터를 깨지 않는다.

### 3.2 비목표

- PDF·HWP·HWPX·이미지 첨부파일 다운로드 또는 내용 분석
- OCR
- 추천점수, 선정확률, 자동 적합도 점수
- AI에 의한 자동 승인·탈락
- 수집 공고의 자동 운영 활성화
- 수집 키워드로 운영 공고의 실제 매칭 조건 자동 생성
- 관리자 화면에서 지자체 파서를 선택·변경하는 기능
- 기존 V56 migration 수정 또는 기존 이력 삭제

기존 V1 이력의 첨부 metadata 조회 계약은 호환을 위해 유지한다. 신규 수집에서는 첨부 URL·파일명 필드를 provider 원문에서 제거하고 attachment 행을 만들지 않으며, 상세 HTML에서도 첨부 링크와 표시명을 본문 추출 전에 제거한다.

## 4. 확정 제품 정책

초기 키워드의 사용·보류·제외 범위는 `docs/backend/announcement-classification-keyword-seed-v1-2026-08-11.md`를 따른다.

### 4.1 지원대상

| 코드 | 화면 표시 | 의미 |
|---|---|---|
| `BUSINESS` | 사업자 | 사업체·사업활동·대표자 자격이 지원조건 |
| `PERSONAL` | 본인(개인) | 사업자 본인의 개인 자격 또는 일반 개인 자격 |
| `SPOUSE` | 배우자 | 배우자 관계·상태가 지원조건 |
| `CHILD` | 자녀 | 자녀·아동·학생 자격이 지원조건 |
| `PARENT` | 부모 | 부모·고령·부양 자격이 지원조건 |

한 공고에 여러 코드를 지정할 수 있다. `가구 전체`는 여섯 번째 최상위 대상이 아니라 보조 조건으로 취급하며, 운영 공고 전환 시 관리자가 실제 관계 대상을 확정한다.

### 4.2 초기 지원유형

| 코드 | 화면 표시 | 대표어 |
|---|---|---|
| `GENERAL_SUPPORT` | 일반 지원 | 지원사업, 지원 |
| `GRANT_SUBSIDY` | 지원금·보조금 | 지원금, 보조금, 장려금, 보상금 |
| `POLICY_FINANCE` | 정책자금·융자 | 정책자금, 정책금융, 융자, 대출, 자금지원 |
| `GUARANTEE` | 보증 | 보증, 특례보증, 신용보증, 보증료 지원 |
| `INTEREST_SUPPORT` | 이자지원 | 이자지원, 이차보전, 이자차액보전 |
| `VOUCHER_BENEFIT` | 바우처·이용권 | 바우처, 쿠폰, 포인트 |
| `REFUND_REDUCTION` | 환급·감면 | 환급, 감면, 면제, 할인 |

`신청`, `접수`, `모집`, `선정`, `제출서류`는 공고 문맥이며 지원유형을 충족시키지 않는다.

### 4.3 그룹 A·B

- 그룹 A: 제조·기술·제품, R&D·연구개발, 특허·지식재산·인증
- 그룹 B: 수출·해외진출, 투자·스타트업·벤처, 조달·혁신제품, ESG·친환경 기술, 행정 잡공고
- 기관명은 발행기관·주관기관 metadata이며 그룹 B가 아니다.
- `채용공고`, `입찰공고`, `고시`, `의원`은 단독 자동 제외어가 아니다.

우선순위는 고정한다.

```text
제목 그룹 B > 그룹 A > 대상+지원유형 조합
```

그룹 B가 제목에서 발견되면 그룹 A 포함 여부와 관계없이 `EXCLUDED`다. 그룹 B가 본문에서만 발견되면 자동 제외하지 않고 `REVIEW_REQUIRED`다.

## 5. 현재 구현과 차이

| 영역 | 현재 구현 | 상세 설계 요구 |
|---|---|---|
| 운영 공고 대상 | `announcements.target_type_code` 단일 값 | 대표 대상은 유지하고 복수 연결 추가 |
| 입력 화면 | 대상 5개 라디오 | 대표 대상 라디오 + 복수 대상 체크박스 |
| 키워드 DB | V56 `INCLUDE/EXCLUDE` | 대상·지원유형·A·B·문맥·강도·유의어·위치 |
| 판정 방식 | 제목 소문자 `contains()` | 정규화 문구·독립 토큰, 제목·본문 분리 |
| 제공자 적용 | 지자체만 필터, 다른 제공자는 `PROVIDER_TRUSTED` | 모든 제공자 공통 엔진 |
| 충돌 정책 | INCLUDE+EXCLUDE → 검수 | 제목 B가 A보다 우선해 제외 |
| 자동 제외 저장 | snapshot 저장 전 반환 | snapshot·decision·match·run item 모두 보존 |
| 본문 | 기업마당·정부24 일부 제공, 지자체 `null` | 본문 없음은 검수, 지자체 상세본문 Gate 분리 |
| 판정 근거 | 쉼표 문자열 | 규칙·유의어·위치·release를 관계형 저장 |
| 운영 전환 | JS가 `BUSINESS`로 고정 요청 | 관리자가 대표·복수 대상과 지원유형 확인 |
| 발견 검색 | 제공자별 호출 기준이 다르고 공통 조합 계획 없음 | 대표 검색어 조합과 수집 후 공통 판정을 분리 |

주요 현행 근거는 다음과 같다.

- 단일 대상: `V1__create_mvp_schema.sql`의 `announcements.target_type_code`
- 수집 원문: `V26__create_announcement_source_collection.sql`
- 평면 규칙: `V56__classify_local_government_notice_semantics.sql`
- 제목 부분일치: `AnnouncementSourceSemanticFilter`
- 제외 조기 반환: `AnnouncementSourceServiceImpl.handleProviderItem`
- 지자체 본문 미저장: `LocalGovernmentNoticeCollector`
- 단일 대상 화면: `templates/app/announcement-input.html`

기존 `COLLECT_ALL`은 앞으로 “게시판 행을 모두 발견한다”는 수집 범위만 의미한다. 개별 공고가 분류 엔진을 우회한다는 의미로 사용하지 않는다.

## 6. 판정 엔진 계약

### 6.0 발견과 판정의 분리

`대상 키워드 AND 지원유형 키워드`는 두 계층에서 사용한다.

1. **발견 검색**: 검색 API를 제공하는 외부 제공자에는 대표적인 강한 대상어와 지원유형어의 조합을 보낸다.
2. **공통 판정**: 발견된 모든 원문을 같은 ACTIVE release로 다시 판정한다.

모든 유의어의 곱집합을 외부 API에 호출하지 않는다. term의 `is_discovery_term=true`인 최소 대표어만 검색 계획에 포함하고, 나머지 유의어는 수집 후 분류에만 사용한다. 검색 기능이 없는 지자체 게시판은 `COLLECT_ALL`로 행을 발견하되 개별 원문의 공통 판정을 우회하지 않는다.

같은 원문이 여러 검색 조합에서 발견되면 provider notice ID 또는 정규화 URL로 본문 요청 전에 병합한다. 수집 run에는 적용한 release ID와 검색 계획 hash를 보존한다.

### 6.1 규칙 역할

| `group_kind_code` | 목적 | 제목 action | 본문 action |
|---|---|---|---|
| `AUTO_EXCLUDE_B` | 그룹 B | `EXCLUDED` | `REVIEW_REQUIRED` |
| `REVIEW_A` | 그룹 A | `REVIEW_REQUIRED` | `REVIEW_REQUIRED` |
| `TARGET` | 지원대상 태그 | `TAG` | `TAG` |
| `SUPPORT_TYPE` | 지원유형 태그 | `TAG` | `TAG` |
| `CONTEXT` | 공고 문맥 보조 | `CONTEXT_ONLY` | `CONTEXT_ONLY` |
| `PROTECTED_METADATA` | 기관명 등 보호 구간 | `MASK_ONLY` | `MASK_ONLY` |

관리자는 키워드를 그룹에 연결하지만 action과 우선순위를 임의 변경하지 못한다. 제품 정책 변경은 additive migration과 별도 승인 대상이다.

판정 순서는 다음과 같다.

1. provider의 `agency_name`, source에 연결된 기관 별칭, 제목 선두 기관 표기를 보호 metadata 구간으로 식별
2. 보호 구간에 완전히 포함된 A·B·대상·지원유형 부분일치를 판정에서 제외
3. 남은 제목 구간에서 그룹 B → 그룹 A → 대상+지원유형 순으로 판정
4. 본문도 같은 보호 구간 정책으로 판정

예를 들어 `중소벤처기업진흥공단 소상공인 경영안정자금`의 기관명 구간 안에 있는 `벤처`는 B2 근거가 아니다. 기관명 밖에 별도로 `벤처 지원`이 있으면 해당 B2 근거는 정상 적용한다. 보호 metadata match도 근거에는 남기되 action을 적용하지 않는다.

### 6.2 강도

- `STRONG`: 해당 역할을 직접 나타내는 문구
- `SUPPLEMENTARY`: 다른 강한 근거와 함께 사용할 보조 문구

숫자 점수를 부여하지 않는다.

제목과 본문의 대상+지원유형 조합은 다음 규칙으로 판단한다.

| 대상 근거 | 지원유형 근거 | 조합 결과 |
|---|---|---|
| `STRONG` | `STRONG` | 충족 |
| `STRONG` | `SUPPLEMENTARY` | 충족 |
| `SUPPLEMENTARY` | `STRONG` | 충족 |
| `SUPPLEMENTARY` | `SUPPLEMENTARY` | 미충족 |

예를 들어 `소상공인 + 지원`은 제목 후보가 될 수 있지만 `주민 + 지원`은 자동 후보가 아니다.

### 6.3 일치 방식

관리자가 임의 정규식을 입력하게 하지 않는다.

| 코드 | 용도 |
|---|---|
| `NORMALIZED_PHRASE` | Unicode·대소문자·연속 공백을 정규화한 문구 포함 |
| `TOKEN` | `IP`, `IR`, `PoC`, `TIPS`처럼 독립 경계가 필요한 약어 |
| `EXACT_TITLE` | 제목 전체가 지정 문구와 같은 제한적 행정 공고 |

띄어쓰기 변형은 유의어로 관리한다. 예: `물품구매`와 `물품 구매`.

정규화 순서는 Unicode `NFKC`, 영문 `Locale.ROOT` 소문자화, 줄바꿈·연속 공백 축약, 일반 구분 기호의 단어 경계 보존 순서다. 띄어쓰기를 전부 제거하거나 형태소·오탈자를 추정하지 않는다. 원문 강조를 위해 정규화 문자와 원문 문자 위치의 대응표를 유지하고, offset은 원문 Unicode code point 기준 0부터 시작하는 반개방 구간 `[start, end)`로 저장한다.

### 6.4 제목 1차 판정

| 조건 | 최종/중간 결과 | 사유 코드 |
|---|---|---|
| 그룹 B 제목 일치 | `EXCLUDED` | `TITLE_GROUP_B_MATCHED` |
| B 없음, 그룹 A 제목 일치 | 본문 확인 후에도 최소 `REVIEW_REQUIRED` | `TITLE_GROUP_A_MATCHED` |
| A·B 없음, 대상+지원유형 조합 충족 | 본문 2차 진행 | `TITLE_COMBINATION_MATCHED` |
| A·B 없음, 조합 미충족 | `EXCLUDED` | `TITLE_COMBINATION_NOT_MATCHED` |

제목 B가 확인되면 본문 내용으로 자동 복구하지 않는다. 규칙 변경 후 재분류만 가능하다.

### 6.5 본문 2차 판정

| 조건 | 결과 | 사유 코드 |
|---|---|---|
| 본문 없음 | `REVIEW_REQUIRED` | `BODY_UNAVAILABLE` |
| 본문 수집 실패 | `REVIEW_REQUIRED` | `BODY_FETCH_FAILED` |
| 그룹 B가 본문에서만 일치 | `REVIEW_REQUIRED` | `BODY_GROUP_B_MATCHED` |
| 그룹 A 일치 | `REVIEW_REQUIRED` | `BODY_GROUP_A_MATCHED` |
| 대상+지원유형 조합 재확인 | `ACCEPTED` | `TARGET_SUPPORT_CONFIRMED` |
| 조합 재확인 실패 | `REVIEW_REQUIRED` | `BODY_COMBINATION_NOT_CONFIRMED` |

기업마당·정부24 API가 제공하는 지원내용·요약은 `body_text`로 인정한다. 지자체는 현재 목록 수집만으로 `body_text`가 없으므로 다음 중 하나가 적용된다.

1. 상세 페이지 본문 수집 기능 적용 전: 모든 제목 후보를 `BODY_UNAVAILABLE/REVIEW_REQUIRED`로 이관
2. 상세 페이지 본문 수집 기능 적용 후: 공식 상세 URL의 보이는 본문 텍스트만 추출해 2차 판정

지자체 상세 본문 수집은 첨부파일 분석과 별개다. 스크립트·스타일·외부 하위 리소스를 실행하거나 가져오지 않고, 기존 URL 검증·동일 기관 host 제한을 유지해야 한다.

2026-08-14 로컬 구현은 각 요청과 redirect 직전에 DNS를 다시 검증하고, 검증을 통과한 공개 IP 목록만 HTTP 연결 계층의 고정 DNS 결과로 사용한다. URI의 원래 기관 host는 TLS hostname 검증에 유지한다. HTML은 첨부 링크·표시명과 실행 불가 요소를 먼저 제거한 뒤 `main`, `[role=main]`, `article` 순으로 의미 본문 영역을 선택하고, 해당 영역이 없을 때만 `body`를 사용한다. 이 구현은 운영 feature flag를 변경하지 않으며 실제 기관별 canary 검증 전에는 활성화 완료로 보지 않는다.

본문의 출처와 가용 상태를 혼동하지 않도록 다음 코드를 함께 저장한다.

| 필드 | 코드 |
|---|---|
| `body_source_code` | `PROVIDER_FULL_TEXT`, `PROVIDER_SUMMARY`, `DETAIL_PAGE_TEXT`, `NONE` |
| `body_availability_code` | `AVAILABLE`, `UNAVAILABLE`, `FETCH_FAILED`, `UNSUPPORTED` |

공식 API 요약도 2차 판정 텍스트로 사용할 수 있지만 제목 반복뿐이거나 빈 값이면 `UNAVAILABLE`로 처리한다. 요약에서 조합이 재확인되지 않았다는 이유만으로 자동 제외하지 않고 `BODY_COMBINATION_NOT_CONFIRMED/REVIEW_REQUIRED`로 이관한다.

### 6.6 최종 상태와 검수 상태

| 판정 | `semantic_status_code` | `review_status_code` | run item |
|---|---|---|---|
| 제목 B 또는 조합 미충족 | `EXCLUDED` | `ARCHIVED` | `EXCLUDED`, `source_id` 포함 |
| 본문 B·A·미확인 | `REVIEW_REQUIRED` | `REVIEW_PENDING` | `COLLECTED` |
| 제목·본문 조합 확인 | `ACCEPTED` | `REVIEW_PENDING` | `COLLECTED` |
| 중복 | 기존 의미 상태 보존 | `DUPLICATE` | `DUPLICATE` |

`REVIEW_REQUIRED` 원문은 `REVIEW_COMPLETED` 전 운영 공고로 전환하지 못한다. `EXCLUDED`는 운영 공고 전환을 계속 차단한다.

### 6.7 수집기 상태와 원문 분류 상태 분리

지자체 V62의 정적 parser QA 보류와 실제 최신 수집 실패는 원문 키워드 분류가 아니다.

- `QA_BLOCKED`: source 설정·parser 검증 상태. 원문 판정을 실행하지 못한 상태
- `LATEST_RUN_FAILURE`: 최근 실제 수집 실행 실패
- `ACCEPTED`, `REVIEW_REQUIRED`, `EXCLUDED`: 수집에 성공한 개별 원문의 분류 상태

source 상태가 `QA_BLOCKED`면 키워드 분류기가 이를 `EXCLUDED`로 바꾸지 않는다. 화면에서도 `수집 출처 상태`와 `수집 원문 분류`를 별도 영역·필터로 표시한다. 브라우저 바로가기 성공도 서버 수집 성공이나 본문 확보 성공으로 간주하지 않는다.

## 7. DB 상세 설계

구현 결과 migration 번호는 `V63`~`V68`로 확정했다. `V68`은 한 수집 원문이 두 운영 공고로 중복 연결되지 않도록 `source_id` 단독 UNIQUE를 추가한다. 여러 provider 원문이 하나의 운영 공고를 가리키는 것은 허용하므로 `announcement_id`는 index만 유지한다.

### 7.1 V63 구현: 분류 카탈로그와 운영 공고 다중 태깅

#### `announcement_target_categories`

- `id uuid` PK
- `category_code varchar(30)` UNIQUE
- `category_name varchar(100)`
- `is_enabled boolean`
- `sort_order integer`
- `created_at`, `updated_at`

5개 대상 코드를 seed한다.

#### `announcement_support_types`

- `id uuid` PK
- `support_type_code varchar(40)` UNIQUE
- `support_type_name varchar(100)`
- `is_enabled boolean`
- `sort_order integer`
- `created_at`, `updated_at`

7개 초기 지원유형을 seed한다.

#### `announcement_target_category_assignments`

- `id uuid` PK
- `announcement_id uuid` FK
- `target_category_id uuid` FK
- `is_primary boolean`
- `assignment_source_code varchar(20)`: `MANUAL`, `SOURCE_CONFIRMED`, `LEGACY_BACKFILL`
- `assigned_by uuid` nullable FK
- `assigned_at timestamptz`
- UNIQUE `(announcement_id, target_category_id)`
- 공고별 `is_primary=true` partial UNIQUE

#### `announcement_support_type_assignments`

- `id uuid` PK
- `announcement_id uuid` FK
- `support_type_id uuid` FK
- `assignment_source_code varchar(20)`
- `assigned_by uuid` nullable FK
- `assigned_at timestamptz`
- UNIQUE `(announcement_id, support_type_id)`

`announcements.target_type_code`는 삭제하거나 nullable로 바꾸지 않는다. 기존 값을 대표 대상 assignment로 backfill하고, 신규 코드는 한 transaction에서 기존 컬럼과 연결 테이블을 dual-write한다.

### 7.2 V64 구현: 버전형 키워드 규칙

#### `announcement_source_classification_rule_releases`

- `id uuid` PK
- `release_code varchar(40)` UNIQUE
- `version_no integer` UNIQUE
- `row_version integer`
- `release_status_code varchar(20)`: `DRAFT`, `ACTIVE`, `RETIRED`
- `rule_snapshot_hash varchar(64)`
- `combination_operator_code varchar(10)`
- `body_unavailable_action_code varchar(30)`
- `attachment_analysis_enabled boolean`
- `auto_activation_enabled boolean`
- `change_note varchar(1000)`
- `created_by`, `created_at`
- `activated_by`, `activated_at`
- `retired_at`
- ACTIVE release partial UNIQUE

정책값은 다음으로 고정한다.

```text
combination_operator_code = AND
body_unavailable_action_code = REVIEW_REQUIRED
attachment_analysis_enabled = false
auto_activation_enabled = false
```

#### `announcement_source_classification_rule_groups`

- `id uuid` PK
- `release_id uuid` FK
- `group_code varchar(80)`
- `group_name varchar(150)`
- `group_kind_code varchar(30)`
- `target_category_id uuid` nullable FK
- `support_type_id uuid` nullable FK
- `title_action_code varchar(30)`
- `body_action_code varchar(30)`
- `sort_order integer`
- `is_enabled boolean`
- UNIQUE `(release_id, group_code)`

#### `announcement_source_classification_keyword_rules`

- `id uuid` PK
- `group_id uuid` FK
- `rule_code varchar(100)`
- `strength_code varchar(30)`
- `is_enabled boolean`
- `sort_order integer`
- `row_version integer`
- `created_by`, `created_at`, `updated_by`, `updated_at`
- UNIQUE `(group_id, rule_code)`
- term의 composite FK를 위한 UNIQUE `(id, group_id)`

#### `announcement_source_classification_keyword_terms`

- `id uuid` PK
- `keyword_rule_id uuid`
- `group_id uuid`
- `term_type_code varchar(20)`: `CANONICAL`, `SYNONYM`
- `term_text varchar(200)`
- `normalized_term_text varchar(200)`
- `match_mode_code varchar(30)`
- `is_discovery_term boolean`
- `discovery_order integer` nullable
- `is_classification_term boolean`
- `is_enabled boolean`
- `created_by`, `created_at`, `updated_by`, `updated_at`
- composite FK `(keyword_rule_id, group_id)` → rule `(id, group_id)`
- UNIQUE `(group_id, normalized_term_text, match_mode_code)`
- 규칙별 `term_type_code='CANONICAL'` partial UNIQUE

대표어와 유의어를 한 term 테이블에 두어 같은 그룹 안의 숨은 중복도 DB에서 차단한다. 같은 문구가 A와 B에 의도적으로 존재할 수 있으므로 전역 term UNIQUE는 두지 않는다.

`PROTECTED_METADATA`의 장문 공식 기관명은 `NORMALIZED_PHRASE`로 사용할 수 있다. `기보`, `중진공`, `TIPA`, `KIAT`, `KEIT`, `KOTRA` 같은 약칭은 단순 본문 부분일치로 보호하지 않고, provider의 `agency_name` 또는 source 기관 mapping과 일치하거나 제목 선두의 독립 표기일 때만 보호한다.

ACTIVE release는 직접 수정하지 않는다. 관리자는 ACTIVE를 복제한 DRAFT에서 편집하고 미리보기 후 새 release를 활성화한다.

`version_no`는 공개되는 규칙 버전 번호이고 `row_version`은 동시 편집 방지용이다. `rule_snapshot_hash`는 그룹·규칙·term을 코드 순으로 정렬한 canonical JSON의 SHA-256으로 활성화 직전에 계산한다. 표시 순서 변경도 hash에 포함하고, 처리자·시각 같은 가변 metadata와 secret은 포함하지 않는다.

### 7.3 V65 구현: 초기 키워드 release

- 대상 5종 규칙
- 지원유형 7종 규칙
- 공고 문맥 규칙
- 기관명 보호 metadata 규칙
- 그룹 A 3종
- 그룹 B 5종
- 영문 약어 token과 한글 유의어

정확한 초기 상태는 `announcement-classification-keyword-seed-v1-2026-08-11.md`를 따른다. migration은 이를 ACTIVE가 아닌 DRAFT로 적재한다. `DRAFT_DISABLED`, `NOT_A_RULE`은 판정에 참여하지 않는다. `METADATA_ONLY`는 보호 구간 식별에만 사용하며 태그·검수·제외 action을 만들지 않는다.

기관명 그룹은 자동 제외 seed에서 제거하고 `PROTECTED_METADATA`로만 적재한다. `채용공고`, `입찰공고`, `고시`, `의원`과 기존 V56의 단독 `입찰`, `용역`은 신규 자동 제외 seed로 이관하지 않는다.

기존 `announcement_source_semantic_keyword_rules`를 자동 변환하지 않는다. 기존 행은 rollback 호환과 과거 근거를 위해 그대로 유지한다.

### 7.4 V66 구현: 판정 이력과 수집 원문 태그

#### `announcement_source_content_versions`

- `id uuid` PK
- `source_id uuid` FK
- `raw_hash varchar(64)`
- `title varchar(500)`
- `body_text text`
- `body_source_code varchar(30)`
- `body_availability_code varchar(30)`
- `source_url text`
- `raw_payload_json jsonb`
- `collected_at timestamptz`
- UNIQUE `(source_id, raw_hash)`

기존 `announcement_source_snapshots`는 `/api/v1`과 목록 성능을 위한 current projection으로 유지한다. 같은 provider 원문의 hash가 바뀔 때만 content version을 append해 과거 판정의 원문 근거를 보존한다. 첨부 내용은 content version에도 저장하지 않는다.

#### `announcement_source_classification_evaluations`

- `id uuid` PK
- `source_id uuid` FK
- `content_version_id uuid` FK
- `run_id uuid` nullable FK
- `rule_release_id uuid` FK
- `engine_version varchar(40)`
- `body_source_code varchar(30)`
- `body_availability_code varchar(30)`
- `title_stage_code varchar(40)`
- `body_stage_code varchar(40)`
- `decision_status_code varchar(30)`
- `reason_code varchar(80)`
- `is_current boolean`
- `evaluated_at timestamptz`
- source별 current partial UNIQUE
- 인덱스 `(is_current, decision_status_code, evaluated_at DESC)`

#### `announcement_source_classification_matches`

- `id uuid` PK
- `evaluation_id uuid` FK
- `keyword_rule_id uuid` FK
- `keyword_term_id uuid` FK
- `match_location_code varchar(20)`: `TITLE`, `BODY`
- `matched_text varchar(500)`
- `start_offset`, `end_offset`
- `applied_action_code varchar(30)`
- offset CHECK

#### `announcement_source_classification_target_matches`

- `id uuid` PK
- `evaluation_id uuid` FK
- `target_category_id uuid` FK
- `created_at timestamptz`
- UNIQUE `(evaluation_id, target_category_id)`

#### `announcement_source_classification_support_matches`

- `id uuid` PK
- `evaluation_id uuid` FK
- `support_type_id uuid` FK
- `created_at timestamptz`
- UNIQUE `(evaluation_id, support_type_id)`

자동 판정 태그를 evaluation에 귀속시켜 재분류 전후 이력을 모두 보존한다.

#### `announcement_source_confirmed_target_categories`

- `id uuid` PK
- `source_id uuid` FK
- `target_category_id uuid` FK
- `based_on_evaluation_id uuid` FK
- `confirmed_by uuid` FK
- `confirmed_at timestamptz`
- UNIQUE `(source_id, target_category_id)`

#### `announcement_source_confirmed_support_types`

- `id uuid` PK
- `source_id uuid` FK
- `support_type_id uuid` FK
- `based_on_evaluation_id uuid` FK
- `confirmed_by uuid` FK
- `confirmed_at timestamptz`
- UNIQUE `(source_id, support_type_id)`

수동 확정 태그는 자동 match와 분리한다. 새 evaluation이 current가 되면 이전 `based_on_evaluation_id`의 수동 확정값은 삭제하지 않지만 `STALE`로 표시하고 운영 공고 전환 전 재확인을 요구한다. 변경 전후는 기존 `announcement_source_review_histories`와 `audit_logs`에 원문 없이 기록한다.

#### 기존 테이블 additive 컬럼

- `announcement_source_collection_runs.rule_release_id uuid` FK
- `announcement_source_collection_runs.search_plan_hash varchar(64)`
- `announcement_source_collection_runs.search_plan_json jsonb`
- `announcement_source_snapshots.classification_row_version integer NOT NULL DEFAULT 0`

`search_plan_json`에는 실제 검색어·제공자·페이지 범위만 저장하고 인증 헤더, API key, token은 저장하지 않는다. `classification_row_version`은 검수·태그 확정의 낙관적 잠금에 사용한다.

기존 `semantic_status_code`, `semantic_reason_code`, `semantic_matched_keywords`는 `/api/v1` 호환용 current projection으로 유지한다. 새 evaluation·match가 판정 근거 source of truth다.

### 7.5 V67 구현: QA·운영 원문 목적 분리

현재 지자체 QA 정리는 연결되지 않은 snapshot을 물리 삭제할 수 있다. 자동 제외 근거 보존과 충돌하지 않게 다음 값을 추가한다.

```text
data_purpose_code IN ('PRODUCTION', 'QA')
DEFAULT 'PRODUCTION'
```

QA 정리는 `data_purpose_code='QA'`만 대상으로 제한한다. 기존 행을 임의로 QA로 추정하지 않는다.

현재 일반 애플리케이션 수집 write는 DB 기본값에 따라 `PRODUCTION`만 생성한다. 운영 DB에 QA 원문을 기록하는 내부 QA writer는 이번 로컬 구현 범위에 포함하지 않았으며, 격리 QA 실행을 승인할 때 별도 내부 전용 경로와 생성·정리 절차를 먼저 확정한다. QA 목적값을 관리자 일반 입력으로 받지 않는다.

### 7.6 기존 데이터 이관

| 기존 데이터 | 처리 |
|---|---|
| 운영 공고 `target_type_code` | primary assignment로 결정적 backfill |
| 기존 지원유형 | 추측하지 않고 비워 둠 |
| 기존 snapshot 판정 | `LEGACY_V56` evaluation 요약만 생성 가능 |
| 기존 snapshot 원문 | 현재 raw hash로 content version 1건 backfill |
| 기존 쉼표 키워드 | 위치·그룹이 없으므로 match 행으로 변환하지 않음 |
| 기존 자동 제외 run item | snapshot이 없어 복구 불가, 재수집 외 추정 금지 |
| 기존 연결 운영 공고 | 재분류로 자동 비활성화하지 않음 |

## 8. 공통 서비스 계층 설계

분류기를 `localgov` provider 내부에서 제거하고 공통 `announcementsource` 도메인으로 이동한다.

```text
AnnouncementSourceController
→ AnnouncementSourceService
→ AnnouncementSourceServiceImpl
   → AnnouncementSourceSearchPlanService
      → provider별 검색 지원 여부와 조합 계획
   → ProviderClient
   → ProviderContentClient
      → 공식 상세 본문 확보, 첨부 미수집
   → AnnouncementSourceClassificationService
      → AnnouncementSourceClassificationServiceImpl
      → AnnouncementSourceClassificationDao
      → AnnouncementSourceClassificationMapper.xml
   → AnnouncementSourcePersistenceService
      → AnnouncementSourceDao
      → AnnouncementSourceMapper.xml
→ PostgreSQL
```

Provider는 외부 응답을 공통 item으로 변환하는 일만 담당한다. 분류 엔진은 실행 시작 시 ACTIVE release를 한 번 읽고 해당 실행 전체에서 같은 release ID를 사용한다.

### 8.1 권장 클래스 경계

| 계층 | 권장 객체 | 책임 |
|---|---|---|
| Controller | `AnnouncementSourceRuleReleaseController` | 규칙 버전·키워드 API DTO 변환 |
| Controller | `AnnouncementSourceClassificationController` | 판정 근거·재분류·수동 확정 API |
| Service | `AnnouncementSourceRuleReleaseService` | 규칙 버전 외부 계약 |
| ServiceImpl | `AnnouncementSourceRuleReleaseServiceImpl` | DRAFT 편집·검증·활성화 transaction |
| Service | `AnnouncementSourceClassificationService` | 제목·본문 판정 외부 계약 |
| ServiceImpl | `AnnouncementSourceClassificationServiceImpl` | 정규화·우선순위·근거 생성 |
| ServiceImpl | `AnnouncementSourcePersistenceServiceImpl` | source별 짧은 저장 transaction |
| DAO | `AnnouncementSourceClassificationDao` | 규칙·evaluation·match SQL 호출 |
| Mapper XML | `AnnouncementSourceClassificationMapper.xml` | PostgreSQL 명시 컬럼 SQL |
| support | `AnnouncementSourceTextNormalizer` | 원문 보존형 NFKC·offset mapping |
| support | `AnnouncementSourceSearchPlanBuilder` | discovery term 조합과 실행 계획 hash |
| provider | `ProviderContentClient` 구현체 | 공식 상세 HTML 본문 확보 |

Controller에 판정 로직을 두지 않고 ServiceImpl에서 직접 SQL을 작성하지 않는다. Mapper는 명시 컬럼, `#{}` binding, SQL 주석, resultMap을 사용한다.

### 8.2 주요 메서드 계약

프로젝트 명명 규칙에 맞춰 다음 접두사를 사용한다.

```text
selectRuleReleaseList()
selectKeywordRuleList()
selectClassificationDetails()
insertDraftRuleRelease()
saveKeywordRule()
updateKeywordRule()
updateKeywordRuleStatus()
deleteDraftKeywordRule()
saveRuleReleasePublication()
saveClassificationEvaluation()
saveConfirmedClassification()
saveReclassification()
```

검색 DTO는 `AnnouncementSourceRuleSearchCondition`, 규칙 쓰기는 `AnnouncementSourceKeywordRuleSaveRequest`, 판정 조회는 `AnnouncementSourceClassificationDetailsResponse`, 수동 확정은 `AnnouncementSourceConfirmedClassificationSaveRequest`로 분리한다. 목록 응답은 `PageResponse`, 단건·쓰기는 `ApiResponse`를 유지한다.

`ProviderContentClient`는 사용자가 입력한 임의 URL을 받지 않는다. 시스템에 등록된 공식 source와 해당 원문의 검증된 상세 URL만 처리한다. redirect는 같은 허용 host 범위에서 제한하고, private·loopback·link-local 주소, 비 HTML 응답, 최대 크기·시간 초과를 차단한다. JavaScript를 실행하지 않고 첨부 링크를 따라가지 않는다.

지자체 상세 본문 수집의 초기 안전 기본값은 다음과 같이 제안한다. 값은 환경설정으로 외부화하되 관리자 화면에서 parser나 제한을 바꾸게 하지 않는다.

| 설정 | 제안 기본값 |
|---|---|
| 기능 flag | OFF로 배포 후 격리 QA에서 활성화 |
| connect timeout | 3초 |
| read timeout | 7초 |
| redirect | 최대 3회, 허용된 동일 기관 host 범위 |
| 응답 크기 | 압축 해제 후 최대 2 MiB |
| Content-Type | `text/html`만 허용, UTF-8·EUC-KR·MS949 등 명시 charset을 안전하게 decode |
| 동시성 | source host별 최대 2건 |
| 재시도 | timeout·5xx에 1회, 무한 재시도 금지 |

차단·timeout·본문 추출 실패는 수집 run 전체 실패가 아니라 해당 원문의 `BODY_FETCH_FAILED/REVIEW_REQUIRED`로 기록한다.
문자셋은 HTTP header, HTML `meta charset`, UTF-8 fallback 순으로 해석하며 복구 불가능한 decode 오류도 같은 검수 경로로 보낸다.

현재 수집 method는 외부 HTTP 호출과 DB 처리를 하나의 큰 transaction에서 수행한다. 구현 시 orchestration은 transaction 밖에 두고, 각 원문 저장·판정·근거 기록을 별도 Bean의 짧은 transaction으로 분리한다. 같은 클래스 내부 호출로 `REQUIRES_NEW`를 우회하지 않는다.

## 9. 원문 저장 순서

현재의 `EXCLUDED` 조기 반환을 제거한다.

```text
ACTIVE release와 provider 검색 계획 고정
→ Provider item 발견
→ provider ID·정규화 URL 기준 실행 내 병합
→ 제목 정규화와 제목 단계 사전 판정
→ 제목 단계가 본문 확인 대상이면 공식 상세 본문 확보
→ 동일 provider의 기존 원문 확인
→ snapshot 저장 또는 안전한 갱신
→ raw hash가 새 값이면 content version append
→ 고정한 ACTIVE release로 최종 판정
→ evaluation·match·source 태그 저장
→ snapshot 호환 semantic projection 갱신
→ run item에 source_id와 판정 사유 저장
→ 교차 provider 중복 확인
→ 검수 큐 반영
```

제목 단계 사전 판정은 본문 요청 여부를 결정하기 위한 메모리 내 결과다. 최종 근거는 snapshot과 같은 짧은 item transaction에서 다시 검증·저장한다. 제목 B나 제목 조합 미충족 건도 본문 요청만 생략할 뿐 snapshot 저장은 생략하지 않는다.

본문 확인 대상은 `TITLE_COMBINATION_MATCHED`와 `TITLE_GROUP_A_MATCHED`다. 그룹 A는 최종 상태가 검수로 고정되더라도 본문 근거와 복수 태그를 확보하기 위해 본문을 확인한다.

자동 제외도 최소한 다음을 남긴다.

- 원문 제목·본문·URL
- provider 원문 JSON과 hash 및 content version
- 규칙 release
- 제목·본문 단계 결과
- 일치 규칙·유의어·위치
- 최종 상태와 사유
- 실행 ID와 source ID

감사 로그에는 원문 전체를 복사하지 않고 evaluation ID, release, 상태, 처리자 등 비식별 metadata만 기록한다.

## 10. 상태 전이와 동시성

### 10.1 전환 제한

- `EXCLUDED`: 운영 공고 전환 불가
- `REVIEW_REQUIRED`: `REVIEW_COMPLETED` 전 전환 불가
- `ACCEPTED`: 관리자 확인 후 DRAFT 전환 가능
- 모든 DRAFT는 기존 승인 절차를 통과해야 활성화 가능

### 10.2 낙관적 잠금

- 키워드 수정 요청은 `expectedVersion`을 필수로 받는다.
- 검수·분류 수정은 `expectedClassificationDecisionId`와 `expectedVersion`을 함께 받는다.
- 불일치는 `409 CONFLICT`로 거절하고 최신값과 재시도 경로를 제공한다.
- 수집 중 새 release가 활성화돼도 이미 시작된 run은 처음 고정한 release를 사용한다.
- 같은 provider 원문이 동시에 발견되면 unique constraint와 `INSERT ... ON CONFLICT` 후 기존 source 재조회로 한 source에 수렴한다.
- 새 current evaluation을 만들 때 source 행을 잠그고 기존 current 해제와 신규 current insert를 같은 transaction에서 수행한다.
- release 활성화 transaction은 현재 ACTIVE와 대상 DRAFT를 잠그고 partial UNIQUE로 동시 활성화를 최종 차단한다.

### 10.3 재분류

- 규칙 변경은 신규 수집부터 적용한다.
- 기존 원문은 자동 재분류하지 않는다.
- 관리자가 dry-run 대상·예상 상태 변경 건수를 확인한 뒤 실행한다.
- 새 evaluation을 append하고 이전 evaluation은 current만 해제한다.
- 연결된 운영 공고가 새 판정에서 제외돼도 운영 공고를 자동 숨기지 않고 운영 확인 작업만 생성한다.

## 11. API 계약

모든 응답은 `ApiResponse`, 목록은 `PageResponse`를 유지한다.

### 11.1 신규 `/api/v1` 규칙 관리 API

새 리소스이므로 기존 v1 의미를 변경하지 않는다.

| Method | Endpoint | 권한 | 용도 |
|---|---|---|---|
| `GET` | `/api/v1/admin/announcement-source-rule-releases` | `OPERATOR`, `APPROVER`, `ADMIN` | release 목록 |
| `POST` | `/api/v1/admin/announcement-source-rule-releases` | `ADMIN` | ACTIVE 복제 DRAFT 생성 |
| `GET` | `/api/v1/admin/announcement-source-rule-releases/{releaseId}/keyword-rules` | 내부 3역할 | 키워드 목록 |
| `POST` | `/api/v1/admin/announcement-source-rule-releases/{releaseId}/keyword-rules` | `ADMIN` | 키워드·유의어 등록 |
| `PUT` | `/api/v1/admin/announcement-source-rule-releases/{releaseId}/keyword-rules/{ruleId}` | `ADMIN` | DRAFT 규칙 수정 |
| `PATCH` | `/api/v1/admin/announcement-source-rule-releases/{releaseId}/keyword-rules/{ruleId}/status` | `ADMIN` | 사용 중지·재사용 |
| `DELETE` | `/api/v1/admin/announcement-source-rule-releases/{releaseId}/keyword-rules/{ruleId}` | `ADMIN` | 미게시 DRAFT 규칙 제거 |
| `POST` | `/api/v1/admin/announcement-source-rule-releases/{releaseId}/preview` | `ADMIN` | 제목·본문 미리보기 |
| `POST` | `/api/v1/admin/announcement-source-rule-releases/{releaseId}/publication` | `ADMIN` | release 활성화 |
| `GET` | `/api/v1/admin/announcement-sources/{sourceId}/classification` | 내부 3역할 | 구조화 판정 근거 |
| `POST` | `/api/v1/admin/announcement-sources/{sourceId}/reclassifications` | `ADMIN` | 명시적 재분류 |
| `PUT` | `/api/v1/admin/announcement-sources/{sourceId}/confirmed-classification` | `OPERATOR`, `ADMIN` | 검수 완료 태그 확정 |

규칙 쓰기 DTO의 핵심 필드는 다음과 같다.

```json
{
  "ruleGroupCode": "TARGET_BUSINESS",
  "canonicalKeyword": "소상공인",
  "synonyms": ["소상공인 사업자"],
  "strengthCode": "STRONG",
  "matchModeCode": "NORMALIZED_PHRASE",
  "sortOrder": 10,
  "expectedVersion": 3,
  "changeReason": "클라이언트 확정 키워드 반영"
}
```

물리 `DELETE`는 한 번도 게시되지 않은 DRAFT의 행에만 허용한다. 과거 ACTIVE release나 판정 근거가 참조하는 규칙은 `PATCH .../status`로 사용 중지하며 이력을 남긴다.

release 활성화 요청은 `expectedVersion`, 변경 사유, 정답 세트 실행 ID를 받는다. 응답에는 이전·신규 release, 추가·수정·사용중지 규칙 수, 예상 판정 변경 수를 반환한다. 활성화 transaction 안에서 기존 ACTIVE를 `RETIRED`로 바꾸고 새 release 하나만 ACTIVE로 만든다.

수집 원문 태그 확정 요청은 자동 판정 자체를 덮어쓰지 않고 별도 수동 확정값을 만든다.

```json
{
  "expectedClassificationDecisionId": "uuid",
  "expectedVersion": 4,
  "targetCategoryCodes": ["BUSINESS", "PERSONAL"],
  "supportTypeCodes": ["POLICY_FINANCE"],
  "reviewNote": "공고 본문 자격조건 확인"
}
```

새 자동 evaluation이 생기면 기존 확정값은 `STALE`이 되며 재확정 전 운영 공고로 전환할 수 없다.

### 11.2 수집 원문 v1 additive 조회

기존 목록·상세에 다음 필드를 additive하게 제공할 수 있다.

```json
{
  "classification": {
    "decisionId": "uuid",
    "ruleReleaseCode": "ASCR-000001",
    "semanticStatusCode": "REVIEW_REQUIRED",
    "reasonCode": "BODY_GROUP_B_MATCHED",
    "titleStageCode": "COMBINATION_MATCHED",
    "bodyStageCode": "GROUP_B_MATCHED",
    "bodySourceCode": "PROVIDER_SUMMARY",
    "bodyAvailabilityCode": "AVAILABLE",
    "targetCategoryCodes": ["BUSINESS", "PERSONAL"],
    "supportTypeCodes": ["POLICY_FINANCE"],
    "confirmedClassificationStatusCode": "STALE",
    "version": 4,
    "matches": [
      {
        "ruleGroupCode": "AUTO_EXCLUDE_B_EXPORT",
        "canonicalKeyword": "수출",
        "matchedTerm": "수출",
        "locationCode": "BODY",
        "startOffset": 42,
        "endOffset": 44,
        "appliedActionCode": "REVIEW_REQUIRED"
      }
    ]
  }
}
```

기존 V1 첨부 이력은 응답 호환을 위해 읽을 수 있다. 신규 V2 수집은 첨부 metadata를 생성하지 않으며 `classification.matches[].locationCode`는 `TITLE`, `BODY`만 허용한다.

규칙 목록 조회는 `page`, `size`, `releaseId`, `groupKindCode`, `groupCode`, `strengthCode`, `matchModeCode`, `enabled`, `keyword` 조건을 받는 `PageResponse`다. 수집 원문 목록에는 `semanticStatusCode`, `reviewStatusCode`, `targetCategoryCode`, `supportTypeCode`, `matchedGroupCode`, `matchLocationCode`, `ruleReleaseId` 필터를 additive하게 제공한다.

### 11.3 운영 공고 다중 태깅 `/api/v2`

현재 v1 쓰기 요청은 단일 `targetTypeCode`만 받는다. 이를 배열 의미로 바꾸지 않고 신규 v2 쓰기 계약을 만든다.

```http
POST /api/v2/admin/announcement-sources/{sourceId}/announcements
```

```json
{
  "primaryTargetCategoryCode": "BUSINESS",
  "targetCategoryCodes": ["BUSINESS", "PERSONAL"],
  "supportTypeCodes": ["POLICY_FINANCE"],
  "incomeJudgementCode": "VAT_TAX_BASE_ONLY",
  "expectedClassificationDecisionId": "uuid",
  "expectedVersion": 4
}
```

검증 조건:

- 대표 대상은 전체 대상 목록에 반드시 포함
- 대상 코드는 중복 없이 1~5개
- 지원유형은 중복 없이 1개 이상
- 현재 decision과 `expectedClassificationDecisionId` 일치
- 현재 `classification_row_version`과 `expectedVersion` 일치
- 수동 확정값의 `basedOnEvaluationId`가 현재 decision과 일치
- `EXCLUDED` 전환 금지
- `REVIEW_REQUIRED`는 검수 완료 후에만 전환

운영 공고 직접 생성·수정도 신규 `/api/v2/announcements` 계약에서 같은 배열을 사용한다. 기존 v1은 단일 대상만 저장하고 연결 테이블에도 동일한 한 건을 dual-write한다.

명시적 경로는 다음과 같다.

- `POST /api/v2/announcements`
- `PUT /api/v2/announcements/{announcementId}`
- `POST /api/v2/admin/announcement-sources/{sourceId}/announcements`

기존 `/api/v1` 쓰기 요청과 응답 필드의 의미는 변경하지 않는다.

### 11.4 API 오류 코드

- `ANNOUNCEMENT_SOURCE_RULE_RELEASE_NOT_ACTIVE`
- `ANNOUNCEMENT_SOURCE_RULE_RELEASE_NOT_DRAFT`
- `ANNOUNCEMENT_SOURCE_RULE_DUPLICATE`
- `ANNOUNCEMENT_SOURCE_RULE_INVALID`
- `ANNOUNCEMENT_SOURCE_VERSION_CONFLICT`
- `ANNOUNCEMENT_SOURCE_CLASSIFICATION_REQUIRED`
- `ANNOUNCEMENT_SOURCE_NOT_CONVERTIBLE`
- `ANNOUNCEMENT_SOURCE_CATEGORY_INVALID`

본문 미확보와 A/B 일치는 HTTP 오류가 아니라 저장되는 판정 사유다.

## 12. 관리자 화면 설계

기존 Thymeleaf·Bootstrap 5·공통 layout/fragment를 사용하며 새 UI·상태 관리 라이브러리를 추가하지 않는다. 이 화면은 정밀한 내부 업무도구이므로 장식보다 상태·근거·오류 회복을 우선한다.

### 12.1 키워드 관리

권장 route는 `/app/admin/announcement-keywords`다.

주 행동은 `새 DRAFT release 편집 후 활성화`다. ACTIVE release를 직접 편집하지 않는다.

화면에서는 내부 용어 `release` 대신 `규칙 버전`을 사용한다. 상태는 `초안(DRAFT)`, `적용 중(ACTIVE)`, `이전 버전(RETIRED)`으로 표시한다.

구성:

1. 현재 ACTIVE release, 적용 시각, 키워드 수, 정책 요약
2. 그룹·세부 그룹·강도·일치 방식·상태 필터
3. 대표 키워드, 유의어, 제목/본문 action, 수정자·수정일 표
4. DRAFT 편집 패널
5. 제목·본문 판정 미리보기
6. release 변경 전후 diff와 정답 세트 결과
7. 활성화 영향 요약과 확인
8. 감사 이력

활성화는 R2 변경으로 취급한다.

1. DRAFT 저장
2. 제목·본문 미리보기
3. 정답 세트 실행
4. 현재 ACTIVE 대비 추가·수정·사용중지 규칙과 예상 상태 변경 건수 확인
5. `새 규칙 버전 적용` 명시적 확인
6. 적용된 release 코드·시각·처리자 확인

정답 세트 실패, version 충돌, 예상 변경 건수 조회 실패 중 하나라도 있으면 활성화 버튼을 비활성화하고 이유와 복구 경로를 표시한다. 활성화 실패 시 DRAFT 입력은 보존한다.

`삭제`는 다음처럼 처리한다.

- 아직 한 번도 활성화되지 않은 DRAFT 행: DRAFT에서 제거 가능
- ACTIVE 또는 판정 근거로 사용된 행: 물리 삭제 금지, `사용 중지`

사용 중지에는 변경 사유를 요구한다. 정규식 입력, action 변경, parser 선택은 제공하지 않는다.

### 12.2 수집 공고 검수

기존 화면의 기본 필터 `REVIEW_PENDING + ACCEPTED`는 그룹 A 검수 건을 숨길 수 있다. 기본 보기를 `조치 필요`로 바꾼다.

탭:

- 조치 필요
- 유효 후보
- 자동 제외
- 전체

필터:

- 제공자
- 지원대상
- 지원유형
- 그룹 A/B
- 일치 위치
- release

상세에는 다음을 표시한다.

- 최종 판정과 한글 사유
- 제목 판정과 본문 판정
- 대상·지원유형 태그
- A/B 일치와 적용 우선순위
- 일치 키워드·유의어·위치
- release 버전과 판정 시각
- `첨부 내용은 수집 판정에 사용하지 않습니다` 안내

분류 상태와 운영 처리 상태를 한 배지로 합치지 않는다.

| 영역 | 코드 | 화면 표시 |
|---|---|---|
| 분류 상태 | `ACCEPTED` | 유효 후보 |
| 분류 상태 | `REVIEW_REQUIRED` | 관리자 검수 필요 |
| 분류 상태 | `EXCLUDED` | 자동 제외, 원문 보관 |
| 처리 상태 | `REVIEW_PENDING` | 검수 대기 |
| 처리 상태 | `REVIEW_COMPLETED` | 검수 완료 |
| 확정 태그 | `STALE` | 새 판정 발생, 태그 재확인 필요 |

대표적인 판정 사유는 코드가 아니라 다음 한글 문구를 우선 표시하고 기술 코드는 상세 정보에서만 제공한다.

| 사유 코드 | 한글 표시 |
|---|---|
| `TITLE_GROUP_B_MATCHED` | 제목에 자동 제외 문구가 있습니다. |
| `TITLE_GROUP_A_MATCHED` | 제목에 관리자 검수 문구가 있습니다. |
| `TITLE_COMBINATION_NOT_MATCHED` | 제목에서 지원대상과 지원유형 조합을 확인하지 못했습니다. |
| `BODY_UNAVAILABLE` | 확인할 본문이 없어 관리자가 검수해야 합니다. |
| `BODY_FETCH_FAILED` | 본문을 가져오지 못해 관리자가 검수해야 합니다. |
| `BODY_GROUP_B_MATCHED` | 본문에 자동 제외 검토 문구가 있어 관리자가 확인해야 합니다. |
| `BODY_GROUP_A_MATCHED` | 본문에 관리자 검수 문구가 있습니다. |
| `BODY_COMBINATION_NOT_CONFIRMED` | 본문에서 지원대상과 지원유형 조합을 다시 확인하지 못했습니다. |
| `TARGET_SUPPORT_CONFIRMED` | 제목과 본문에서 지원대상과 지원유형을 확인했습니다. |

자동 제외 원문도 별도 탭에서 조회할 수 있지만 운영 공고 전환 버튼은 비활성화하고 사유를 표시한다.

### 12.3 운영 공고 입력

기존 단일 라디오를 바로 제거하지 않는다.

- 대표 대상: 기존 라디오 1개
- 지원대상 태그: 5개 체크박스
- 지원유형: 복수 체크박스
- 대표 대상은 체크된 지원대상에 자동 포함
- `개인 지원` 문구를 `본인(개인)`으로 통일

복수 태그는 분류와 검색 정보일 뿐 매칭 성공이나 최종 진행 공고를 의미하지 않는다.

### 12.4 화면 상태와 접근성

- 로딩: 현재 release와 목록 영역별 상태 표시
- 빈 상태: 필터 결과 없음과 등록 키워드 없음 구분
- 저장 오류: 입력값 보존, 상단 요약과 필드 오류 연결
- 버전 충돌: 최신값과 차이를 보여주고 새로고침·재적용 선택
- 활성화 영향 조회 실패: 기존 ACTIVE가 계속 적용됨을 명시하고 재시도 제공
- 사용 중지 성공: 상태 배지와 감사 이력 즉시 갱신
- 권한 없음: 수정 버튼을 숨기기만 하지 않고 읽기 전용 사유 제공
- 상태는 색상 외 텍스트와 아이콘으로 표현
- 표는 키보드로 접근하고 모바일에서는 핵심 열 우선 또는 상세 행 사용
- 모달 사용 시 Escape, 포커스 트랩, 닫은 뒤 호출 버튼 복귀
- 320px 리플로우, 200%·400% 확대, `focus-visible` 검증

브라우저 QA는 사용자가 명시적으로 요청한 구현 검증 단계에서만 수행한다.

## 13. 권한

| 기능 | ADMIN | OPERATOR | APPROVER |
|---|---:|---:|---:|
| 규칙·release 조회 | 가능 | 가능 | 가능 |
| DRAFT 생성·키워드 수정 | 가능 | 불가 | 불가 |
| release 활성화 | 가능 | 불가 | 불가 |
| 판정 근거 조회 | 가능 | 가능 | 가능 |
| 수집 원문 검수 상태 변경 | 가능 | 가능 | 기존 계약 유지 |
| 수집 원문 태그 수동 확정 | 가능 | 가능 | 불가 |
| 운영 공고 DRAFT 전환 | 가능 | 가능 | 불가 |
| 자동 제외 직접 우회 | 불가 | 불가 | 불가 |

자동 제외를 복구하려면 규칙 release를 수정하고 재분류해야 한다. UI에서 강제 통과 버튼을 제공하지 않는다.

## 14. QA와 테스트

### 14.1 정답 세트 필드

```text
caseId, providerCode, title, bodyText,
expectedStatusCode, expectedReasonCode,
expectedTargetCategoryCodes, expectedSupportTypeCodes,
expectedGroupACodes, expectedGroupBCodes,
expectedMatchLocations, note
```

### 14.2 필수 정책 표본

| ID | 조건 | 기대 결과 |
|---|---|---|
| QA-01 | 소상공인 정책자금, 본문 재확인 | `ACCEPTED`, BUSINESS, POLICY_FINANCE |
| QA-02 | 주민 지원 | `EXCLUDED/TITLE_COMBINATION_NOT_MATCHED`, 약한 조합 통과 금지 |
| QA-03 | 스마트공장 지원사업 | `REVIEW_REQUIRED`, A1 |
| QA-04 | 기술창업 지원사업 | `EXCLUDED`, A1+B2 중 B 우선 |
| QA-05 | 소상공인 수출바우처 | `EXCLUDED`, 제목 B1 |
| QA-06 | 제목 정상, 본문에서만 수출 | `REVIEW_REQUIRED` |
| QA-07 | 중진공 소상공인 경영안정자금, 본문 재확인 | `ACCEPTED`, BUSINESS, POLICY_FINANCE, 중진공은 보호 metadata |
| QA-08 | 소상공인 채용지원금 | `채용공고` 단독 규칙 오탐 없음 |
| QA-09 | 소상공인 대상 입찰 참여 지원 | `입찰공고` 단독 규칙 오탐 없음 |
| QA-10 | 지원사업 고시 | `고시`로 제외하지 않음 |
| QA-11 | 소상공인 병의원 경영지원금 | `의원` 오탐 없음 |
| QA-12 | 공무원 채용 | `EXCLUDED`, B6 |
| QA-13 | 공사 입찰 | `EXCLUDED`, B6 |
| QA-14 | TIPS·팁스 | 두 표현 모두 B2 |
| QA-15 | 제목 B, 본문 A | `EXCLUDED` |
| QA-16 | 제목 A, 본문에서만 B | `REVIEW_REQUIRED` |
| QA-17 | 제목 조합 충족, 본문 없음 | `REVIEW_REQUIRED/BODY_UNAVAILABLE` |
| QA-18 | 제목·본문 정상, 첨부명만 수출자료 | 첨부 무시 |
| QA-19 | 청년 소상공인 배우자 취업지원금 | BUSINESS+PERSONAL+SPOUSE |
| QA-20 | 같은 원문을 세 provider로 입력 | 동일 판정 |

### 14.3 테스트 계층

| 계층 | 필수 검증 |
|---|---|
| 분류기 단위 | AND, B>A, 제목/본문 차이, 강약, token, 유의어, 첨부 미사용 |
| 정규화 단위 | NFKC, 대소문자, 공백·구분기호, 원문 code point offset 대응 |
| 검색 계획 | discovery term만 사용, 조합별 호출, 실행 내 중복 병합, release 고정 |
| 규칙 Service | DRAFT 편집, 활성화, 사용중지, 중복, version 충돌, audit |
| Mapper/PostgreSQL | FK·UNIQUE·partial UNIQUE, evaluation append, 과거 태그 보존, 자동 제외 snapshot 보존 |
| Controller MockMvc | wrapper, pagination, `@Valid`, 역할별 403, 409 충돌 |
| View smoke | 본인(개인), 복수 선택, 정책 안내, 사용중지, 이력, `th:utext` 미사용 |
| 수집 Service | 세 provider 공통 판정, source_id 보존, 본문 미확인 검수 |
| 전환 회귀 | 대표 대상 포함, v1 단일 요청 유지, v2 배열 검증 |
| 상태 회귀 | EXCLUDED 차단, REVIEW_REQUIRED 검수 완료 Gate, 자동 활성화 금지 |
| Migration | 계약 정적 테스트와 실제 PostgreSQL Flyway 적용 |

### 14.4 측정·운영 지표

출시 후 단일 `수집 건수`만 성공 지표로 사용하지 않는다.

| 지표 | 목적 |
|---|---|
| provider별 제목 발견·본문 확보·본문 실패 건수 | 발견과 본문 수집 문제 분리 |
| 판정 사유별 `ACCEPTED/REVIEW_REQUIRED/EXCLUDED` 건수 | 규칙 변경 영향 추적 |
| 자동 제외 표본의 관리자 복구율 | 과도한 제외 위험 탐지 |
| 유효 후보 표본의 관리자 반려율 | 과도한 통과 위험 탐지 |
| 검수 대기 시간과 장기 미처리 건수 | 운영 부담 확인 |
| 규칙 버전별 정답 세트 통과율 | release 품질 Gate |
| version 충돌·활성화 실패 건수 | 동시 편집·운영 안정성 확인 |

분석·감사 이벤트는 `ruleReleaseId`, `providerCode`, 상태·사유 코드, 건수, 처리 시간만 기록한다. 원문 제목·본문, 실제 키워드 일치 문장, 개인정보, 인증정보는 분석 이벤트에 넣지 않는다.

권장 이벤트:

```text
announcement_rule_draft_saved
announcement_rule_preview_executed
announcement_rule_golden_set_executed
announcement_rule_release_published
announcement_classification_review_completed
announcement_conversion_blocked
```

키워드 목록과 수집 원문 목록은 서버 pagination을 사용하고 기본 20건, 최대 100건으로 제한한다. 구현 시 실제 사용자 75번째 백분위 기준 LCP 2.5초, INP 200ms, CLS 0.1 목표를 유지하되 브라우저 측정은 사용자가 명시적으로 요청한 QA 단계에서 수행한다.

### 14.5 핵심 수용 시나리오

```gherkin
Given ADMIN이 현재 ACTIVE를 복제한 DRAFT를 편집했고 정답 세트가 모두 통과했을 때
When 영향 요약을 확인하고 "새 규칙 버전 적용"을 실행하면
Then 한 개의 release만 ACTIVE가 된다
And 이후 시작한 run만 새 release를 사용한다
And 처리자·시각·변경 사유가 감사 이력에 남는다
```

```gherkin
Given 수집 원문의 수동 확정 태그가 이전 evaluation을 기준으로 저장되어 있을 때
When 새 evaluation이 current가 되면
Then 기존 확정 태그는 STALE로 표시된다
And 재확정 전 운영 공고 전환은 409로 차단된다
And 입력한 태그 이력은 삭제되지 않는다
```

```gherkin
Given 제목에 그룹 B와 그룹 A가 함께 있고 그룹 B가 기관명 보호 구간 밖에 있을 때
When 공통 분류기가 제목을 판정하면
Then 결과는 EXCLUDED다
And 원문 snapshot, content version, release, 일치 위치가 저장된다
And 운영 공고 전환은 제공되지 않는다
```

## 15. 구현·배포·롤백 순서

### 15.1 구현 증분

1. **DB 기반**: V63 카탈로그·운영 공고 다중 태그와 v1 dual-write
2. **규칙 기반**: V64 규칙 버전 schema, V65 초기 DRAFT, ADMIN 관리 API·화면
3. **판정 근거**: V66 content version·evaluation·match·확정 태그와 공통 분류기
4. **v2 전환**: 운영 공고 생성·수정·수집 원문 전환의 다중 태그 계약
5. **검수 화면**: 조치 필요·유효 후보·자동 제외 분리와 구조화 근거
6. **지자체 본문**: 별도 feature flag와 격리 QA로 상세 HTML 수집
7. **운영 분리**: V67 QA·PRODUCTION 목적 제한과 정리 쿼리 보호

각 증분은 관련 migration 계약 테스트, Service·Mapper·MockMvc 테스트, `bootJar`가 통과한 뒤 다음 단계로 이동한다. 6단계를 뒤로 미루면 지자체 제목 후보는 안전하게 `BODY_UNAVAILABLE/REVIEW_REQUIRED`로 남는다.

### 15.2 배포·롤백

1. V63~V68 additive schema 적용
2. 새 Mapper·Service dual-write 배포, 신규 엔진 feature flag OFF
3. DRAFT 초기 release와 정답 세트 적재
4. 세 provider 표본 dry-run
5. 상태별 건수와 오탐 검토
6. ADMIN이 release 활성화
7. feature flag ON
8. 신규 수집부터 적용
9. 기존 원문 재분류는 별도 관리자 실행

롤백은 migration 삭제가 아니다.

- 애플리케이션 feature flag를 OFF로 전환
- 이전 ACTIVE release를 다시 활성화
- 필요하면 새 evaluation을 생성
- V56 테이블과 기존 semantic 컬럼은 유지
- 단일 `target_type_code`와 `/api/v1`은 계속 유지

## 16. 구현 완료 기준

- 모든 provider가 같은 ACTIVE release로 판정된다.
- 검색 지원 provider는 DRAFT에서 승인된 discovery term 조합만 사용하고 실행 계획을 보존한다.
- 대상+지원유형 AND와 강약 조합이 정답 세트와 일치한다.
- 제목 A+B는 항상 B 우선 자동 제외된다.
- 본문 B는 자동 제외가 아니라 검수로 이동한다.
- 기관명과 확정 제거 단독어로 인한 오탐이 없다.
- 자동 제외 원문의 snapshot·raw·decision·match를 조회할 수 있다.
- 모든 판정에 release와 제목/본문 위치별 근거가 있다.
- 지원대상·지원유형 다중 태그가 저장되고 대표 대상이 일관된다.
- 기존 v1 단일 요청과 응답이 회귀하지 않는다.
- ADMIN 외 규칙 쓰기는 403이다.
- 첨부 다운로드·추출·OCR 코드가 추가되지 않는다.
- 수집 공고는 관리자 승인 전 운영 활성화되지 않는다.
- 전체 테스트, `bootJar`, Flyway PostgreSQL 통합 테스트가 통과한다.
- 브라우저 QA는 사용자가 명시적으로 요청한 경우에만 수행하고, 미실행 시 통과로 보고하지 않는다.

## 17. 운영 schema 적용 후 남은 활성화 Gate

2026-08-13 운영 DB에 V63~V68을 적용했다. 초기 release는 `DRAFT` 1건이며 `ACTIVE`는 0건이고, 분류 V2와 상세본문 feature flag는 모두 OFF다.

1. migration 복구 절차와 격리 QA 전용 write·cleanup 절차 확정
2. DRAFT 규칙의 dry-run 대상과 예상 변경 건수 검토
3. 로컬 연결 IP 고정 구현의 staging 검증과 운영 네트워크 private-range egress 차단 확인
4. ADMIN의 초기 release 활성화 승인과 제한된 신규 수집 canary 승인
5. 기존 원문 재분류는 별도 실행 승인 전 금지

운영 migration은 적용됐지만, 이 Gate가 통과되기 전에는 규칙 활성화, feature flag ON, 제한 수집 canary, 기존 원문 재분류를 실행하지 않는다.

## 18. 2026-08-12 로컬 검증 결과

- 전체 Gradle 테스트: 537건, 실패 0, 오류 0, 조건부 skip 17
- 공고·분류·보안·migration 대상군: 278건, 실패 0, 오류 0, 조건부 skip 2
- `bootJar`: 성공
- PostgreSQL 16 빈 DB Flyway V1→V68 및 규칙 게시 생명주기 통합 테스트: 2건 통과
- 관리자 JavaScript 3개 `node --check`: 통과, 상주 Node.js 프로세스 없음
- Mapper XML 파싱, `git diff --check`, `SELECT *`·MyBatis `${}`·`th:utext` 금지 패턴 검사: 통과
- 브라우저 QA: 사용자 실행 지시가 없어 정책상 미실행
- 운영 DB·외부 provider·규칙 활성화·재분류·배포: 2026-08-12 로컬 검증 당시 미실행. 이후 운영 적용 결과는 19절에 기록한다.

## 19. 2026-08-13 운영 적용 결과

- GitHub Actions 수동 배포 `31683175372`, 대상 commit `0ed85b3`: 테스트, `bootJar`, S3 업로드, CodeDeploy 모두 성공
- V68을 막던 동일·미사용 후속 source link 1행을 승인된 트랜잭션으로 분리하고 `audit_logs`에 복구 metadata를 기록했다. 공고 본체와 원문·매칭·진행 데이터는 삭제하지 않았다.
- Flyway 최고 성공 버전 `68`, V68 성공 이력 1건, `announcement_source_links.source_id` UNIQUE 제약 존재, 중복 source 0건을 확인했다.
- 초기 규칙 release는 `DRAFT` 1건, `ACTIVE` 0건이다.
- `saneb.service`는 active이고 내부 `/actuator/health`는 `UP`이다.
- `SANEB_ANNOUNCEMENT_SOURCE_CLASSIFICATION_V2_ENABLED=false`, `SANEB_LOCAL_GOV_NOTICE_DETAIL_BODY_ENABLED=false`를 확인했다.
- 규칙 활성화, feature flag ON, 외부 provider canary, 기존 원문 재분류, 브라우저 QA는 실행하지 않았다.
