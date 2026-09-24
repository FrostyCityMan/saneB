# saneB Backend API Contract v1

> 2026-09-24 후속 구간 규칙1.0.3 명시 선택: 정책 생성/초안 수정의 `segmentRuleVersion` 및 구간 GET의 `analysisVersion`은 정확한 `segment-role-1.0.0`/`segment-role-1.0.2`/`segment-role-1.0.3`을 허용한다.3의 서버 지문은 `8b9fdd872f3eb9890146d6e360408204ff285f4b23977e07693834aceec66d43`이다. 생략 생성·독립 POST·기본 GET은0, 생략 수정은 현재 정책 버전 유지다. 진단1·빈 값·임의 버전·직접 hash 입력은 계속 거부한다. 기존 snapshot과 평가 FK·버전별 저장 이력은 변경하지 않으며, 판정 결합 조회는 정확한 저장 버전/지문을 재현 검증한다. 화면의 명시3 선택은 초안 저장만 수행하고 운영 게시·기존 데이터 적용을 실행하지 않는다. v1과 Flyway 변경 없음. 아래0/2 설명은 선행 증분 이력이며 현재 허용 목록은 이 항목을 따른다.

> 2026-09-24 정책 초안 구간 규칙 선택: `POST /api/v2/admin/announcement-attachment-policies`와 `PUT .../{policyId}`에 선택적 `segmentRuleVersion`을 추가했다. 정확한 `segment-role-1.0.0`/`segment-role-1.0.2`만 허용하며 지문은 서버가 고정한다. 생략/null은 생성 시 기존1.0.0 기본값, 수정 시 현재 버전 유지다. 빈 값·진단1.0.1·알 수 없는 값은400이며 `segmentRulesHash` 직접 입력은 거부한다. 수정은 활성 ADMIN+CSRF+DRAFT+expectedVersion CAS를 그대로 사용한다. 파일 단위 엔진은 이 필드로 전환할 수 없고 새 구간 정책 초안이 필요하다. 생성 멱등 키는 명시한 버전을 포함해 결합하며 과거 생략 요청의 hash와 재요청 동작을 유지한다. 일반 수정의 정책 row_version 증가/정책 hash 초기화로 이전 QA·게시 준비 근거를 재사용할 수 없으며 재검증이 필요하다. 초안 저장은 정책 게시·기존 작업 변경·기존 데이터 적용·수집 실행이 아니다. 관리자 화면에서 버전 선택·변경 전후 확인·저장 응답 지문 대조를 제공한다. 실제 운영 적용은 별도 Gate다.

> 2026-09-24 판정 결합 구간 조회: `GET /api/v2/admin/announcement-sources/{sourceId}/attachment-extractions/{extractionId}/segment-analysis/evaluations/{evaluationId}`를 추가했다. 선택한 판정의 입력 FK와 정책 버전에 결합된 분석만 조회한다. `ApiResponse.data`는 `evaluationId`, `policyId`, `evaluationCurrent`, `evaluatedFileRoleCode`, `segmentAnalysis`다. 마지막 필드는 기존 구간 응답 구조이며, `applicationMode=SHADOW`는 조회가 판정을 변경하지 않는다는 의미다. `evaluationCurrent`는 평가 이력의 현재 표시이며 ENFORCE·최종 검수·공개 여부가 아니다. 판정 당시 파일 역할과 조회 시점 파일 역할은 별도 필드다. ADMIN/OPERATOR/APPROVER 조회, 성공·업무 오류 no-store, 원문/QA/제목 제외 보호를 유지한다. 결합 없음은404, 결합·저장 분석 재현 불일치는409이며 독립 분석이나 다른 버전으로 대체하지 않는다. 기존 GET/POST와 v1은 보존하고 신규 migration은 없다. 상세 화면은 현재/미리보기 집합 또는 선택한 분류 이력의 ID를 이 경로에 전달하며, 기존1.0.0 독립 분석과 구분한다. 운영 반영·브라우저 확인은 별도 검증이다.

> 2026-09-24 구간 버전 고정 연결: 구간 GET API에 선택적 `analysisVersion=segment-role-1.0.2`를 추가했다. 생략하면 기존 `segment-role-1.0.0`만 조회한다. 저장 이력이 없으면 `NOT_ANALYZED`이며 다른 버전으로 대체하거나 분석을 생성하지 않는다. 지원 버전은 정확한 `1.0.0`/`1.0.2`이고 진단용 `1.0.1`·알 수 없는 값·빈 값은 구체적인 400 오류다. 권한·no-store·wrapper는 유지한다. POST 수동 SHADOW 분석과 정책 신규 생성 기본값은 여전히 `1.0.0`이다. 이번 연결은 명시적으로 고정한 `1.0.2` worker·정책 검증의 서버 기반이며, 관리자 신규 버전 선택 UI·운영 게시·재분류 완료를 뜻하지 않는다.

> 2026-09-22 구간 분석 API 추가: `GET/POST /api/v2/admin/announcement-sources/{sourceId}/attachment-extractions/{extractionId}/segment-analysis`. GET은 미분석/저장 결과만 조회하고 POST는 저장된 추출 원문으로 버전별 멱등 분석을 생성한다. 응답은 `ApiResponse`, `applicationMode=SHADOW`; 기존 파일 역할·종합 판정·ACTIVE 상태는 변경하지 않는다. GET은 ADMIN/OPERATOR/APPROVER, POST는 ADMIN/OPERATOR 및 기존 CSRF 검증을 적용한다. 원문·인증정보는 분석 JSON/감사 metadata에 넣지 않는다. [필드·정합성·미완료 범위](announcement-attachment-segment-analysis-design-2026-09-22.md).

> 후속 worker 연결: 정책 조회 Configuration에 선택적 `segmentRuleVersion/segmentRulesHash`를 추가하고 미설정 시 기존 JSON을 유지한다. SHADOW는 위 API 호출의 비적용 동작을 뜻하며, 동일 분석이 별도 worker 평가에 참조됐는지 여부를 뜻하지 않는다. 신규 엔진 정책 게시 QA·관리자 구간 표시 연결은 아직 완료되지 않았다. 운영 정책 활성화나 기존 데이터 재처리를 수행하지 않는다.

> 엔진별 정책 분류 검증: 기존 `attachment-1.0.0`은 AG30건과 기존 결과 형식을 유지한다. 신규 생성 초안은 구간 버전·hash에 고정된 `attachment-segment-1.0.0`을 사용하며 AG30+SG22 총52건을 검사한다. 기존 정책의 일반 편집은 엔진 계열을 유지하고 개정은 기존 설정을 복사한다. 구 엔진 결과를 신규 엔진 게시 근거로 재사용하지 않는다. 전체 QA 입력 생성·수집원 QA 계획 조회는 신규 엔진도 지원하지만 실제 구간 기대값/전체 실행 근거가 부족하면 통과·게시할 수 없다. 초안 생성·분류 검사 성공은 운영 활성화가 아니다.

> 첨부 V2 확장: [공고 첨부파일 수집·추출 API 설계](announcement-attachment-collection-design-2026-09-08.md)를 바탕으로 24절에 로컬 구현 계약을 기록한다. 기존 v1 제목·본문 조회 계약을 유지하며 첨부 적용 원문의 전환/검수 조건을 서버에서 확인한다. 로컬 코드·테스트 진척은 운영 반영이나 전체 E2E 완료를 뜻하지 않는다. 최신 실행 증거와 잔여 Gate는 [진행 기록](announcement-attachment-end-to-end-progress-2026-09-09.md)을 따른다.

작성일: 2026-05-14

## 1. 기준

- 기존 계약은 `/api/v1/...` 경로를 유지한다. 배열 의미처럼 기존 계약을 깨는 변경만 명시적으로 `/api/v2/...` 신규 경로를 사용한다.
- 응답은 `ApiResponse<T>` wrapper를 사용한다.
- 목록 API는 `PageResponse<T>`를 사용한다.
- 인증과 권한 검증은 서버에서 수행한다.
- `/api/v1/**`, `/api/v2/**` 요청은 서버 rate limit 적용 대상이다.
- 서버 화면 form과 `/logout`은 CSRF 검증 대상이다.
- 브라우저 세션 쿠키가 포함된 `/api/v1/**`, `/api/v2/**` 변경 요청은 `XSRF-TOKEN` 쿠키와 같은 값을 `X-XSRF-TOKEN` header로 전송해야 한다. 단, `/api/v1/payment-webhooks/**`는 provider webhook 검증을 사용하므로 CSRF header 대상에서 제외한다.
- Controller는 URL 매핑, 요청/응답, DTO 변환만 담당한다.
- 비즈니스 로직은 ServiceImpl에 둔다.
- SQL은 DAO와 Mapper XML을 통해서만 실행한다.
- MyBatis XML에서 `SELECT *`와 `${}`는 금지한다.
- MVP에서는 AI 자동판단, 추천도, 우선순위, 선정확률, 가점 계산을 제공하지 않는다.
- API의 `...Id` 필드는 서버 내부 PK/FK용 UUID다. 화면 표시, 운영자 검색, 수기 입력에는 `...Code` 필드를 우선 사용한다. `userCode`, `announcementCode`, `matchingCaseCode`, `progressCode`, `verificationCode`, `reservationCode`는 사람이 읽을 수 있는 고유 문자열이며 내부 UUID를 대체해 화면에 노출하는 업무 식별자다.

## 2. 공통 Response Wrapper

### 2.1 성공 응답

```json
{
  "success": true,
  "data": {},
  "message": ""
}
```

### 2.2 목록 응답

```json
{
  "success": true,
  "data": {
    "items": [],
    "page": 1,
    "size": 20,
    "totalCount": 0,
    "totalPages": 0
  },
  "message": ""
}
```

### 2.3 오류 응답

```json
{
  "success": false,
  "data": {
    "errorCode": "VALIDATION_FAILED",
    "fieldErrors": [
      {
        "field": "loginId",
        "message": "loginId is required"
      }
    ]
  },
  "message": "요청 값이 올바르지 않습니다."
}
```

사용자 메시지는 `message`에 둔다. 시스템 예외, SQL, secret, 개인정보 원문은 응답에 포함하지 않는다.

## 3. Pagination 규칙

| 항목 | 규칙 |
|---|---|
| `page` | 1부터 시작 |
| `size` | 기본 20, 최대 100 |
| `sort` | 서버가 허용한 whitelist 필드만 사용 |
| 응답 | `items`, `page`, `size`, `totalCount`, `totalPages` |

잘못된 paging 값은 `INVALID_PAGE_REQUEST`로 처리한다.

## 4. 인증 / 권한 계약

### 4.1 Role

- `USER`
- `PARTNER`
- `OPERATOR`
- `APPROVER`
- `ADMIN`

다중 역할 사용자의 기본 진입 우선순위:

1. `ADMIN`
2. `APPROVER`
3. `OPERATOR`
4. `PARTNER`
5. `USER`

`primaryRole`은 권한 판단과 메뉴 노출 우선순위에만 사용한다.

MVP v1에서 `defaultRoute`는 역할별 기본 진입점을 반환한다. Frontend는 로그인 또는 회원가입 직후 이 route로 이동하고, 역할별 메뉴와 접근 제어는 `roles`, `primaryRole`, 서버 권한 응답을 기준으로 처리한다.

| `primaryRole` | `defaultRoute` |
|---|---|
| `ADMIN` | `/app/admin/dashboard` |
| `APPROVER` | `/app/approver/reviews` |
| `OPERATOR` | `/app/operator/dashboard` |
| `PARTNER` | `/app/partner/verifications` |
| `USER` | `/app/dashboard` |

`passwordResetRequired = true`이면 역할과 무관하게 `defaultRoute = /password`를 반환한다.

### 4.2 Auth Endpoints

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `POST` | `/api/v1/auth/login` | anonymous | 로그인 |
| `POST` | `/api/v1/auth/signup` | anonymous | 회원가입 후 세션 생성 |
| `POST` | `/api/v1/auth/logout` | authenticated | 로그아웃 |
| `GET` | `/api/v1/auth/me` | authenticated | 현재 사용자/권한/defaultRoute 조회 |
| `PATCH` | `/api/v1/auth/password` | authenticated | 비밀번호 변경 |

#### LoginRequest

```json
{
  "loginId": "admin",
  "password": "password"
}
```

#### LoginResponse

```json
{
  "userId": "uuid",
  "loginId": "admin",
  "name": "관리자",
  "roles": ["ADMIN"],
  "primaryRole": "ADMIN",
  "defaultRoute": "/app/admin/dashboard",
  "passwordResetRequired": false
}
```

#### SignupRequest

```json
{
  "loginId": "user01",
  "password": "new-password",
  "passwordConfirm": "new-password",
  "name": "사용자",
  "phone": "010-0000-0000",
  "email": "user01@example.com",
  "termsAgreed": true,
  "privacyAgreed": true
}
```

회원가입은 `users`에 `ACTIVE`, `password_reset_required=false`로 저장하고 `user_roles`에 `USER` 역할을 부여한다. 가입 성공 시 세션을 생성하고 `LoginResponse`와 동일한 응답을 반환한다. 이용약관과 개인정보 처리방침 동의는 현재 유효한 `consent_versions` 기준으로 `user_consents`에 저장한다.

### Pre-signup Candidate Preview

회원가입 전 임시 후보 확인은 개인정보를 저장하지 않고 승인·진행 중 공고를 기준으로 후보 수와 예상 지원금 범위만 반환한다. 대표자명, 출생연도, 사업 시작일, 사업장 지역, 업종, 가족 간단 정보를 받을 수 있으나 저장하지 않는다. 추천도, 선정확률, 우선순위, 가점은 계산하지 않는다.

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `POST` | `/api/v1/pre-signup/candidate-preview` | public | 회원가입 전 임시 후보 수와 지원금 범위 확인 |

#### CandidatePreviewRequest

```json
{
  "representativeName": "홍길동",
  "birthYear": 1988,
  "regionCode": "SEOUL",
  "ksicCode": "47911",
  "annualRevenue": 120000000,
  "openingDate": "2023-01-10",
  "hasSpouse": true,
  "hasChild": true,
  "hasParent": true,
  "families": [
    {
      "relationTypeCode": "CHILD",
      "birthYear": 2018,
      "schoolAgeStatusCode": "ELEMENTARY",
      "cohabiting": null
    },
    {
      "relationTypeCode": "PARENT",
      "birthYear": 1955,
      "schoolAgeStatusCode": null,
      "cohabiting": true
    }
  ]
}
```

#### CandidatePreviewResponse

```json
{
  "possibleCandidateCount": 3,
  "minSupportAmount": 1000000,
  "maxSupportAmount": 10000000,
  "criteriaNotice": "회원가입 전 임시 확인 결과입니다. 실제 신청 가능 여부는 가입 후 공고별 입력값과 서버 검증 기준으로 확정됩니다."
}
```

#### AuthMeResponse

```json
{
  "userId": "uuid",
  "loginId": "admin",
  "name": "관리자",
  "roles": ["ADMIN"],
  "primaryRole": "ADMIN",
  "defaultRoute": "/app/admin/dashboard",
  "passwordResetRequired": false,
  "profile": {
    "memberProfileId": "uuid",
    "businessProfileId": "uuid",
    "partnerProfileId": null
  }
}
```

AuthMeResponse 필드 계약:

| 필드 | 타입 | nullable | Frontend 사용 기준 |
|---|---|---:|---|
| `userId` | `uuid` | true | 실제 DB 인증 연동 전 skeleton에서는 `null` 가능 |
| `loginId` | `string` | false | 화면 표시용 계정 ID |
| `name` | `string` | false | 사용자명 표시 |
| `roles` | `string[]` | false | 메뉴/권한 표시 기준, 값은 `USER`, `PARTNER`, `OPERATOR`, `APPROVER`, `ADMIN` |
| `primaryRole` | `string` | false | 다중 역할 사용자 대표 역할 |
| `defaultRoute` | `string` | false | 역할별 기본 진입 route, 비밀번호 변경 필요 시 `/password` |
| `passwordResetRequired` | `boolean` | false | `true`이면 비밀번호 변경 흐름 우선 |
| `profile.memberProfileId` | `uuid` | true | 회원 기본 프로필 존재 여부 |
| `profile.businessProfileId` | `uuid` | true | 사업자 프로필 존재 여부 |
| `profile.partnerProfileId` | `uuid` | true | 파트너 프로필 존재 여부 |

#### PasswordChangeRequest

```json
{
  "currentPassword": "old-password",
  "newPassword": "new-password"
}
```

`newPassword`는 8~16자로 입력해야 한다. 검증 실패 시 `fieldErrors[].message`에 사용자가 바로 이해할 수 있는 한국어 안내 문구를 내려준다.

## 4.1 Admin User Management API

관리자 전용 회원관리 계약이다. DB는 기존 `users`, `roles`, `user_roles`를 사용하며 신규 테이블은 추가하지 않는다.

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `GET` | `/api/v1/admin/users` | `ADMIN` | 회원 목록 조회 |
| `GET` | `/api/v1/admin/users/roles` | `ADMIN` | 권한 선택 목록 조회 |
| `PATCH` | `/api/v1/admin/users/{userId}/status` | `ADMIN` | 계정 상태 변경 |
| `PUT` | `/api/v1/admin/users/{userId}/roles` | `ADMIN` | 회원 권한 전체 저장 |

목록 query:

| 필드 | 설명 |
|---|---|
| `keyword` | 아이디, 이름, 휴대폰, 이메일 검색 |
| `statusCode` | `ACTIVE`, `LOCKED`, `DISABLED`, `DELETED` |
| `roleCode` | `USER`, `PARTNER`, `OPERATOR`, `APPROVER`, `ADMIN` |
| `page`, `size` | 기본 pagination |

#### AdminUserStatusUpdateRequest

```json
{
  "statusCode": "ACTIVE"
}
```

#### AdminUserRolesUpdateRequest

```json
{
  "roleCodes": ["USER", "OPERATOR"]
}
```

정책:

- `ADMIN`만 접근할 수 있다.
- 물리적 회원 삭제와 비밀번호 강제 초기화는 이 계약에 포함하지 않는다.
- `DELETED`는 DB 행 삭제가 아니라 로그인 차단용 상태 코드다.
- 현재 로그인한 관리자는 자기 계정을 `LOCKED`, `DISABLED`, `DELETED`로 바꿀 수 없다.
- 현재 로그인한 관리자는 자기 계정의 `ADMIN` 권한을 제거할 수 없다.
- 변경 이력은 `audit_logs`에 비식별 metadata로 기록한다.

## 4.2 Admin Member Basic Info API

관리자 또는 운영자가 회원을 조회한 뒤 해당 회원의 기본정보와 서류별 선택 입력값을 대신 저장하는 계약이다. DB는 기존 `member_profiles`, `business_profiles`, `family_members`, `member_document_input_values`를 사용한다.

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `GET` | `/api/v1/admin/member-basic-info/{userId}` | `OPERATOR`, `ADMIN` | 회원 기본정보·서류별 입력값 통합 조회 |
| `PUT` | `/api/v1/admin/member-basic-info/{userId}` | `OPERATOR`, `ADMIN` | 회원 기본정보·서류별 입력값 통합 저장 |

요청/응답 구조는 `/api/v1/member/basic-info`와 동일하다.

정책:

- `OPERATOR`, `ADMIN`만 접근할 수 있다.
- `userId`는 저장 대상 회원이다.
- 저장 시 `member_document_input_values.user_id`에는 대상 회원 ID를 저장하고, `submitted_by`에는 저장한 관리자 ID를 기록한다.
- 서류 기반 값은 모두 선택 입력이다. 누락 시 일부 매칭 또는 입증에서 불리할 수 있다는 안내만 제공하고 저장 자체를 막지 않는다.
- 운영 secret, 외부 API key, 개인정보 원문을 감사 로그 metadata에 저장하지 않는다.

## 4.3 Address Search API

행정안전부 도로명주소 검색 API를 서버에서 대신 호출하는 내부 API다. 브라우저는 외부 도메인이나 승인키를 직접 알 수 없으며, 화면은 `/api/v1/addresses/road`만 호출한다.

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `GET` | `/api/v1/addresses/road` | authenticated | 도로명주소 검색 결과 조회 |

Request query:

| 필드 | 필수 | 설명 |
|---|---|---|
| `keyword` | Y | 도로명, 건물명, 지번 검색어. 두 글자 이상 |
| `page` | N | 기본값 `1` |
| `size` | N | 기본값 `10`, 최대 `20` |
| `firstSort` | N | `none`, `road`, `location` |
| `includeHistory` | N | 변동 주소 포함 여부. 기본값 `false` |

Response:

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "postalCode": "30112",
        "roadAddress": "세종특별자치시 도움6로 42",
        "roadAddressPart1": "세종특별자치시 도움6로 42",
        "roadAddressPart2": "",
        "jibunAddress": "세종특별자치시 어진동 572",
        "sidoName": "세종특별자치시",
        "sigunguName": "",
        "eupmyeondongName": "어진동",
        "legalDongCode": "3611010300",
        "roadNameCode": "361103258001",
        "buildingManagementNo": "3611010300105720000000001",
        "buildingName": "행정안전부",
        "apartment": false
      }
    ],
    "page": 1,
    "size": 10,
    "totalCount": 1,
    "totalPages": 1
  },
  "message": ""
}
```

환경변수:

- `JUSO_API_ENABLED`: 주소 검색 API 사용 여부.
- `JUSO_API_BASE_URL`: 기본값 `https://business.juso.go.kr/addrlink/addrLinkApi.do`.
- `JUSO_API_KEY`: 행정안전부 도로명주소 API 승인키. 운영 secret이며 코드, 문서, 브라우저에 실제 값을 기록하지 않는다.
- `JUSO_API_TIMEOUT_MILLIS`: 외부 API timeout.

정책:

- 주소 검색 API는 인증 사용자만 호출한다.
- 외부 API 승인키는 서버 환경변수로만 주입한다.
- 회원/사업자 저장 API에는 검색 결과의 구조화 필드만 저장하고 외부 API 원문 전체는 저장하지 않는다.
- `regionCode`, `workplaceRegionCode`는 기존 시도 단위 매칭 코드를 유지하며, `legalDongCode` 등 상세 주소 식별자는 보조 비교 데이터로 저장한다.

## 5. Member / Business / Family API

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `GET` | `/api/v1/members/me/profile` | `USER` | 내 회원 기본 프로필 조회 |
| `PUT` | `/api/v1/members/me/profile` | `USER` | 내 회원 기본 프로필 저장 |
| `GET` | `/api/v1/members/me/business-profile` | `USER` | 내 사업자 프로필 조회 |
| `PUT` | `/api/v1/members/me/business-profile` | `USER` | 내 사업자 프로필 저장 |
| `GET` | `/api/v1/members/me/family-members` | `USER` | 내 가족 구성원 목록 |
| `POST` | `/api/v1/members/me/family-members` | `USER` | 가족 구성원 추가 |
| `PUT` | `/api/v1/members/me/family-members/{familyMemberId}` | `USER` | 가족 구성원 수정 |
| `DELETE` | `/api/v1/members/me/family-members/{familyMemberId}` | `USER` | 가족 구성원 삭제 |
| `GET` | `/api/v1/member/basic-info` | `USER` | 내 기본정보 통합 조회 |
| `PUT` | `/api/v1/member/basic-info` | `USER` | 내 기본정보 통합 저장 |
| `GET` | `/api/v1/admin/member-basic-info/{userId}` | `OPERATOR`, `ADMIN` | 관리자/운영자용 회원 기본정보 통합 조회 |
| `PUT` | `/api/v1/admin/member-basic-info/{userId}` | `OPERATOR`, `ADMIN` | 관리자/운영자용 회원 기본정보 통합 저장 |

`/api/v1/member/basic-info`는 사용자 첫 행동 화면에서 사용하는 통합 계약이다. 기존 `/api/v1/members/me/...` 세분화 계약은 v1 문서상 유지하지만, 현재 화면 구현은 통합 계약을 사용한다. 서류 기반 세부 값은 선택 입력이며, 누락 시 일부 매칭 또는 입증에서 불리할 수 있다는 안내만 제공한다.

#### MemberProfileSaveRequest

```json
{
  "birthYear": 1988,
  "address": "서울특별시 ...",
  "regionCode": "11000",
  "householder": true,
  "householdMember": false,
  "healthInsuranceBasisCode": "EMPLOYEE",
  "hasIncome": true
}
```

#### BusinessProfileSaveRequest

```json
{
  "representativeName": "홍길동",
  "businessRegistrationNo": "1234567890",
  "businessName": "사내비상점",
  "workplaceAddress": "서울특별시 ...",
  "workplaceRegionCode": "11000",
  "openingDate": "2022-01-01",
  "industryName": "도소매업",
  "businessCategory": "도매 및 소매업",
  "businessItem": "전자상거래",
  "ksicCode": "47911",
  "businessTypeCode": "SOLE_PROPRIETOR",
  "companyStageCode": "OPERATING"
}
```

#### FamilyMemberSaveRequest

```json
{
  "relationTypeCode": "CHILD",
  "birthYear": 2018,
  "address": "서울특별시 ...",
  "schoolAgeStatusCode": "PRESCHOOL",
  "enrollmentStatusCode": null,
  "cohabiting": true,
  "supported": true,
  "hasIncome": false
}
```

#### MemberBasicInfoSaveRequest

```json
{
  "birthYear": 1988,
  "regionCode": "SEOUL",
  "postalCode": "04524",
  "roadAddress": "서울특별시 중구 세종대로 110",
  "jibunAddress": "서울특별시 중구 태평로1가 31",
  "detailAddress": "101호",
  "sidoName": "서울특별시",
  "sigunguName": "중구",
  "eupmyeondongName": "태평로1가",
  "legalDongCode": "1114010300",
  "roadNameCode": "111403005001",
  "buildingManagementNo": "1114010300100310000000001",
  "addressSourceCode": "JUSO_API",
  "hasIncome": true,
  "incomePresenceCode": "HAS_INCOME",
  "incomeAmount": 30000000,
  "healthInsuranceBasisCode": "WORKPLACE",
  "business": {
    "representativeName": "홍길동",
    "businessRegistrationNo": "123-45-67890",
    "businessName": "사내비상점",
    "workplaceRegionCode": "SEOUL",
    "workplacePostalCode": "04524",
    "workplaceRoadAddress": "서울특별시 중구 세종대로 110",
    "workplaceJibunAddress": "서울특별시 중구 태평로1가 31",
    "workplaceDetailAddress": "2층",
    "workplaceSidoName": "서울특별시",
    "workplaceSigunguName": "중구",
    "workplaceEupmyeondongName": "태평로1가",
    "workplaceLegalDongCode": "1114010300",
    "workplaceRoadNameCode": "111403005001",
    "workplaceBuildingManagementNo": "1114010300100310000000001",
    "workplaceAddressSourceCode": "JUSO_API",
    "openingDate": "2022-01-01",
    "ksicCode": "47911",
    "businessTypeCode": "SOLE_PROPRIETOR",
    "companyStageCode": "OPERATING",
    "annualRevenue": 120000000,
    "annualRevenueYear": 2025,
    "employeeCount": 5,
    "regularEmployeeCount": 3,
    "plannedHireCount": 1,
    "niceCreditScore": 750,
    "kcbCreditScore": 720,
    "hasExistingLoan": false,
    "hasPolicyFundUsage": false,
    "hasGuaranteeUsage": false
  },
  "families": [
    {
      "relationTypeCode": "CHILD",
      "birthYear": 2018,
      "schoolAgeStatusCode": "ELEMENTARY",
      "cohabiting": true,
      "hasIncome": false,
      "incomePresenceCode": "NONE",
      "incomeAmount": null
    }
  ],
  "interviewResponses": [
    {
      "questionCode": "SAME_BUSINESS_IN_PROGRESS",
      "answerCode": "NO",
      "note": null
    },
    {
      "questionCode": "OTHER_RESTRICTION",
      "answerCode": "UNKNOWN",
      "note": "확인 예정"
    }
  ],
  "documentInputs": [
    {
      "documentTypeCode": "BUSINESS_REGISTRATION",
      "fields": [
        {
          "standardFieldId": "uuid",
          "valueText": "서울특별시 중구",
          "valueNumber": null,
          "valueDate": null,
          "valueBoolean": null
        }
      ]
    }
  ]
}
```

통합 기본정보 저장 정책:

- `business`는 선택 객체다. 빠른 기본정보 입력에서는 대표자명, 사업 시작일, 사업장 지역, 업종만 저장할 수 있다. 사업자등록번호와 상호명은 알 수 있을 때 입력한다.
- 주소 검색으로 선택한 구조화 주소값은 선택 입력이다. `addressSourceCode`, `workplaceAddressSourceCode`는 `JUSO_API`, `MANUAL`만 허용한다.
- `regionCode`, `workplaceRegionCode`는 기존 시도 단위 조건 비교용 코드이며, `legalDongCode`, `workplaceLegalDongCode`는 향후 시군구/읍면동 조건 비교용 보조 식별자다.
- `families`는 배우자, 자녀, 부모 1단계만 허용한다.
- `incomePresenceCode`는 `UNKNOWN`, `NONE`, `HAS_INCOME`만 허용한다.
- 사업자 선택 입력값에는 `employeeCount`, `regularEmployeeCount`, `plannedHireCount`, `niceCreditScore`, `kcbCreditScore`, `hasExistingLoan`을 포함할 수 있다. 신용 점수는 NICE와 KCB를 분리해 저장하며 0~1000 범위만 허용한다. NICE/KCB와 기대출 여부는 외부 API 자동조회가 아니라 사용자 또는 운영자가 직접 입력하는 수동 값이다.
- 가족 선택 입력값에는 `schoolAgeStatusCode`, `enrollmentStatusCode`, `cohabiting`, `supported`, `incomePresenceCode`, `incomeAmount`를 포함할 수 있다. 자녀 학령/재학, 부모 동거/부양, 배우자 소득, 가구합산 소득 조건은 저장된 가족 목록 기준으로 매칭한다.
- `interviewResponses`는 간단 인터뷰 선택 응답이다. 허용 `questionCode`는 `SAME_BUSINESS_IN_PROGRESS`, `DUPLICATE_SUPPORT_USAGE`, `BUSINESS_ACTUALLY_OPERATING`, `OTHER_RESTRICTION`이며, 허용 `answerCode`는 `YES`, `NO`, `UNKNOWN`이다. 정책자금 이용 여부와 보증기관 이용 여부는 기존 `business.hasPolicyFundUsage`, `business.hasGuaranteeUsage`를 재사용한다.
- 지원 품목, 제외 품목, 지원 용도처럼 자동 조건보다 담당자 확인이 필요한 항목은 공고 동적 입력 항목으로 설정한다.
- `documentInputs`는 사용자 기본정보 입력 하단의 서류별 선택 입력값이다. `standardFieldId`는 `standard_document_fields.id`를 참조한다.
- `documentInputs`가 요청에 포함되면 기존 서류 입력값을 전체 교체 저장한다. 빈 배열은 서류 입력값 전체 삭제를 의미하며, 필드가 없으면 기존 값을 유지한다.
- 서류 기반 값은 모두 선택 입력이다. 누락 시 일부 매칭 또는 입증에서 불리할 수 있다는 안내만 제공하고, 기본정보 저장 자체를 막지 않는다.
- 저장된 개인, 사업자, 가족 기본정보와 서류별 선택 입력값은 관리자 조건 매칭 후보 생성과 입증 확인의 비교 자료로 사용할 수 있다.
- 개인정보 원문을 감사 로그 metadata에 저장하지 않는다.

#### MemberBasicInfoResponse.documentInputs

```json
[
  {
    "documentTypeCode": "BUSINESS_REGISTRATION",
    "documentTypeLabel": "사업자등록증",
    "selected": true,
    "fields": [
      {
        "standardFieldId": "uuid",
        "fieldKey": "WORKPLACE_ADDRESS",
        "fieldLabel": "사업장 주소",
        "fieldTypeCode": "TEXT",
        "scopeCode": "BUSINESS",
        "required": false,
        "sortOrder": 45,
        "helpText": "사업자등록증에 표시된 사업장 주소입니다.",
        "valueText": "서울특별시 중구",
        "valueNumber": null,
        "valueDate": null,
        "valueBoolean": null
      }
    ]
  }
]
```

#### MemberProfileResponse

```json
{
  "memberProfileId": "uuid",
  "userId": "uuid",
  "birthYear": 1988,
  "address": "서울특별시 ...",
  "regionCode": "11000",
  "householder": true,
  "householdMember": false,
  "healthInsuranceBasisCode": "EMPLOYEE",
  "hasIncome": true
}
```

#### BusinessProfileResponse

```json
{
  "businessProfileId": "uuid",
  "userId": "uuid",
  "representativeName": "홍길동",
  "businessRegistrationNo": "1234567890",
  "businessName": "사내비상점",
  "workplaceAddress": "서울특별시 ...",
  "workplaceRegionCode": "11000",
  "openingDate": "2022-01-01",
  "industryName": "도소매업",
  "businessCategory": "도매 및 소매업",
  "businessItem": "전자상거래",
  "ksicCode": "47911",
  "businessTypeCode": "SOLE_PROPRIETOR",
  "companyStageCode": "OPERATING",
  "employeeCount": 5,
  "regularEmployeeCount": 3,
  "plannedHireCount": 1,
  "niceCreditScore": 750,
  "kcbCreditScore": 720
}
```

#### FamilyMemberResponse

```json
{
  "familyMemberId": "uuid",
  "userId": "uuid",
  "relationTypeCode": "CHILD",
  "birthYear": 2018,
  "address": "서울특별시 ...",
  "schoolAgeStatusCode": "PRESCHOOL",
  "enrollmentStatusCode": "ENROLLED",
  "cohabiting": true,
  "supported": true,
  "hasIncome": false,
  "incomePresenceCode": "NONE",
  "incomeAmount": null
}
```

Member / Business / Family API skeleton 착수 기준:

- V1 DB migration에 `member_profiles`, `business_profiles`, `family_members`가 이미 포함되어 있으므로 DB 계약 변경 없이 Controller/Service/ServiceImpl/DAO/Mapper XML skeleton 착수가 가능하다.
- 저장 API는 `@Valid` request DTO와 서버 검증을 전제로 한다.
- 조회 API는 `ApiResponse<T>` 또는 `ApiResponse<PageResponse<T>>`로 감싼다.
- Mapper XML 작성 시 명시 컬럼만 사용하고 `SELECT *`, `${}`는 사용하지 않는다.
- 가족 구성원 목록은 최초 skeleton에서 pagination 없이 전체 목록을 반환해도 된다. 가족 구성원 수가 커지는 요구가 생기면 v1 내에서 `PageResponse` 적용 여부를 별도 합의한다.

## 6. Partner Verification API

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `GET` | `/api/v1/partner-verifications` | `PARTNER`, `OPERATOR`, `APPROVER`, `ADMIN` | 검증 목록 |
| `POST` | `/api/v1/partner-verifications` | `PARTNER`, `OPERATOR` | 검증 생성 |
| `GET` | `/api/v1/partner-verifications/{verificationId}` | `PARTNER`, `OPERATOR`, `APPROVER`, `ADMIN` | 검증 상세 |
| `PUT` | `/api/v1/partner-verifications/{verificationId}/member-values` | `PARTNER`, `OPERATOR` | 회원 검증값 저장 |
| `PUT` | `/api/v1/partner-verifications/{verificationId}/business-values` | `PARTNER`, `OPERATOR` | 사업/금융 검증값 저장 |
| `PUT` | `/api/v1/partner-verifications/{verificationId}/family-values` | `PARTNER`, `OPERATOR` | 가족 검증값 저장 |
| `PUT` | `/api/v1/partner-verifications/{verificationId}/documents` | `PARTNER`, `OPERATOR` | 검증 서류 체크 저장 |
| `PUT` | `/api/v1/partner-verifications/{verificationId}/restriction-flags` | `PARTNER`, `OPERATOR` | 제한 플래그 저장 |
| `PATCH` | `/api/v1/partner-verifications/{verificationId}/status` | `PARTNER`, `OPERATOR`, `APPROVER`, `ADMIN` | 검증 상태 변경 |

#### VerificationCreateRequest

```json
{
  "memberUserId": "uuid",
  "businessProfileId": "uuid"
}
```

#### VerificationBusinessValuesSaveRequest

```json
{
  "annualRevenue": 120000000,
  "employeeCount": 5,
  "regularEmployeeCount": 3,
  "taxStatusCode": "PAID",
  "niceCreditScore": 820,
  "kcbCreditScore": 805,
  "hasExistingLoan": true,
  "hasPolicyFundUsage": false,
  "hasGuaranteeUsage": false,
  "financialCheckedOn": "2026-05-14"
}
```

#### VerificationDocumentsSaveRequest

```json
{
  "documents": [
    {
      "documentTypeCode": "BUSINESS_REGISTRATION",
      "sourceTypeCode": "E_CERT",
      "checked": true,
      "note": ""
    }
  ]
}
```

#### VerificationStatusUpdateRequest

```json
{
  "statusCode": "VERIFIED",
  "reviewNote": "필수 서류와 검증값 확인 완료"
}
```

## 7. Announcement API

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `GET` | `/api/v1/announcements` | authenticated | 공고 목록 |
| `POST` | `/api/v1/announcements` | `OPERATOR` | 공고 생성 |
| `GET` | `/api/v1/announcements/{announcementId}` | authenticated | 공고 상세 |
| `PUT` | `/api/v1/announcements/{announcementId}` | `OPERATOR` | 공고 기본 정보 수정 |
| `PUT` | `/api/v1/announcements/{announcementId}/conditions` | `OPERATOR` | 공고 조건 저장 |
| `GET` | `/api/v1/announcements/{announcementId}/input-requirements` | authenticated | 공고별 추가 입력 항목 조회 |
| `PUT` | `/api/v1/announcements/{announcementId}/input-requirements` | `OPERATOR` | 공고별 추가 입력 항목 저장 |
| `PUT` | `/api/v1/announcements/{announcementId}/steps` | `OPERATOR` | 진행 단계 저장 |
| `POST` | `/api/v1/announcements/{announcementId}/approval-requests` | `OPERATOR`, `ADMIN` | 승인 요청 |
| `PATCH` | `/api/v1/announcements/{announcementId}/approval` | `APPROVER`, `ADMIN` | 승인/반려/취소 |
| `PATCH` | `/api/v1/announcements/{announcementId}/manual-status` | `OPERATOR`, `APPROVER` | 수동 상태 변경 |

#### Announcement 상태 응답 필드

`GET /api/v1/announcements`와 `GET /api/v1/announcements/{announcementId}`는 기존 필드를 유지하며 다음 상태 필드를 추가로 제공한다.

| 필드 | 설명 |
|---|---|
| `manualStatusCode` | 관리자가 직접 설정한 수동 상태. 기존 필드 유지 |
| `automaticStatusCode` | 신청 시작일/마감일 기준 계산 상태. `UPCOMING`, `OPEN`, `CLOSING_SOON`, `ENDED` |
| `automaticStatusLabel` | 자동 상태 한글명. `모집예정`, `접수중`, `마감임박`, `종료` |
| `effectiveStatusCode` | 실제 화면 노출 상태. 수동 상태가 `NORMAL`이 아니면 수동 상태, `NORMAL`이면 자동 상태 |
| `effectiveStatusLabel` | 실제 화면 노출 상태 한글명 |
| `receptionTypeCode` | 목록 응답에서 접수 성격 배지 표시에 사용하는 선택값. 예: `BUDGET_EXHAUSTION`, `FIRST_COME`, `ALWAYS_OPEN`, `PERIOD`, `EARLY_CLOSE_POSSIBLE` |

자동 상태는 DB 저장 컬럼이 아니며 조회 시 계산한다. 수동 상태가 `NORMAL`이 아닌 공고는 수동 상태가 자동 상태보다 우선한다.

#### AnnouncementSaveRequest

```json
{
  "targetTypeCode": "BUSINESS",
  "title": "소상공인 지원사업",
  "agencyName": "서울시",
  "summary": "MVP 필수조건 기반 공고",
  "applicationStartDate": "2026-06-01",
  "applicationEndDate": "2026-06-30",
  "incomeJudgementCode": "VAT_TAX_BASE_ONLY",
  "minAmount": 1000000,
  "maxAmount": 5000000,
  "options": [
    {
      "optionGroupCode": "APPLICATION_METHOD",
      "optionCode": "ONLINE"
    }
  ]
}
```

#### AnnouncementConditionsSaveRequest

```json
{
  "industryConditions": [
    {
      "conditionTypeCode": "INCLUDE",
      "ksicCode": "47911"
    }
  ],
  "numericConditions": [
    {
      "standardFieldId": null,
      "conditionScopeCode": "BUSINESS",
      "conditionKey": "ANNUAL_REVENUE",
      "comparatorCode": "LTE",
      "valueNumber": 300000000,
      "minNumber": null,
      "maxNumber": null,
      "unitCode": "KRW"
    }
  ],
  "optionConditions": [
    {
      "standardFieldId": null,
      "conditionScopeCode": "BUSINESS",
      "conditionKey": "BUSINESS_TYPE",
      "optionCode": "SOLE_PROPRIETOR",
      "optionText": null
    }
  ],
  "documentRequirements": [
    {
      "standardFieldId": null,
      "documentTypeCode": "BUSINESS_REGISTRATION",
      "required": true,
      "sortOrder": 1
    }
  ]
}
```

`standardFieldId`는 선택값이다. 값이 없으면 기존처럼 기본정보 기반 조건으로 저장한다. 값이 있으면 `standard_document_fields.id`를 참조한다. 수치/선택 조건에서는 `conditionUsageCode=CONDITION_READY`인 항목만 자동 조건으로 저장할 수 있다. `STANDARDIZATION_REQUIRED` 항목은 화면에 조건 후보로 노출하되 자동 조건 저장은 차단한다. 업태/종목은 `announcement_industry_conditions.ksic_code`에 KSIC 코드로 저장한다. 필요 서류와 동적 입력 항목은 `INPUT_ONLY`, `STANDARDIZATION_REQUIRED` 항목도 요청 입력으로 사용할 수 있다.

매칭 단계 정책:

- `BASIC` 후보는 사용자 기본정보 기준의 넓은 후보이므로 `standardFieldId`가 연결된 서류 조건을 계산하지 않는다.
- `FINAL` 후보는 사용자 기본정보와 `member_document_input_values`에 저장된 서류별 선택 입력값을 함께 사용한다.
- 서류 조건은 추천도, 선정확률, 점수, 우선순위 계산으로 확장하지 않는다.

#### AnnouncementStepsSaveRequest

공고 진행 단계는 공고별로 N개를 저장할 수 있다. 각 단계는 N개의 버튼과 N개의 필요 서류를 가진다. 저장 시 기존 단계, 버튼, 단계 서류를 전체 교체한다.

완료 조건 코드:

| 코드 | 의미 | 서버 이동 조건 |
|---|---|---|
| `BUTTON_CLICK` | 버튼 선택 | 등록된 단계 버튼 선택 |
| `ALL_REQUIRED_DOCUMENTS_CHECKED` | 필수 서류 전체 확인 | 현재 단계 필수 서류 체크 완료 |
| `REQUIRED_INPUTS_SAVED` | 필수 입력값 저장 | 신청 진행 필수 동적 입력값 저장 완료 |
| `RECEIPT_SAVED` | 접수 정보 저장 | 접수번호와 접수일 저장 완료 |
| `RESULT_SAVED` | 최종 결과 저장 | 최종 결과와 결과일 저장 완료 |

버튼 행동 코드:

| 코드 | 의미 |
|---|---|
| `MOVE_NEXT` | 다음 단계로 이동 |
| `COMPLETE_STEP` | 현재 단계 완료 처리 |
| `STOP_PROGRESS` | 진행 중단 처리 |

```json
{
  "steps": [
    {
      "stepOrder": 1,
      "stepName": "안내 발송",
      "guideMessage": "현재 사업 정보 기준으로 진행 가능한 항목이 확인되었습니다.",
      "actionGuide": "진행 의사를 선택하세요.",
      "completionConditionCode": "BUTTON_CLICK",
      "nextConditionCode": "WANTS_TO_PROGRESS",
      "buttons": [
        {
          "buttonCode": "WANTS_TO_PROGRESS",
          "buttonLabel": "진행 원함",
          "buttonActionCode": "MOVE_NEXT",
          "sortOrder": 1
        },
        {
          "buttonCode": "NOT_INTERESTED",
          "buttonLabel": "관심없음",
          "buttonActionCode": "STOP_PROGRESS",
          "sortOrder": 2
        }
      ],
      "documents": []
    },
    {
      "stepOrder": 2,
      "stepName": "서류 안내",
      "guideMessage": "진행에 필요한 서류를 준비하고 체크리스트를 확인합니다.",
      "actionGuide": "필수 서류가 모두 준비되면 서류 준비 완료를 선택하세요.",
      "completionConditionCode": "ALL_REQUIRED_DOCUMENTS_CHECKED",
      "nextConditionCode": "REQUIRED_DOCUMENTS_CHECKED",
      "buttons": [
        {
          "buttonCode": "DOCUMENTS_READY",
          "buttonLabel": "서류 준비 완료",
          "buttonActionCode": "MOVE_NEXT",
          "sortOrder": 1
        }
      ],
      "documents": [
        {
          "documentTypeCode": "BUSINESS_REGISTRATION",
          "required": true,
          "sortOrder": 1
        }
      ]
    }
  ]
}
```

#### AnnouncementInputRequirementsSaveRequest

```json
{
  "requirements": [
    {
      "standardFieldId": "uuid",
      "fieldKey": "OPENING_DATE",
      "fieldLabel": "개업일",
      "fieldTypeCode": "DATE",
      "scopeCode": "BUSINESS",
      "required": false,
      "sensitive": false,
      "sortOrder": 1,
      "helpText": "사업자등록증에 표시된 개업일을 입력합니다.",
      "options": []
    }
  ]
}
```

`standardFieldId`는 선택값이다. 표준 항목을 선택하면 `fieldKey`, `fieldTypeCode`, `scopeCode`는 표준 항목과 일치해야 한다. 동적 입력 항목은 사용자에게 추가로 값을 받기 위한 구조이며, `conditionUsageCode`가 `INPUT_ONLY` 또는 `STANDARDIZATION_REQUIRED`인 항목도 입력 요청용으로 저장할 수 있다.

## 7.1 Standard Code API

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `GET` | `/api/v1/standard-code-groups` | authenticated | 표준 코드 그룹 목록 |
| `GET` | `/api/v1/standard-codes` | authenticated | 표준 코드 그룹별 코드 검색 |

#### StandardCodeResponse

```json
{
  "standardCodeId": "uuid",
  "groupCode": "KSIC_11",
  "groupName": "한국표준산업분류 제11차",
  "code": "56111",
  "codeName": "한식 일반 음식점업",
  "parentCode": "56",
  "levelNo": 5,
  "sortOrder": 56111,
  "active": true
}
```

`GET /api/v1/standard-codes?groupCode=KSIC_11&keyword=음식&page=1&size=20`은 `ApiResponse<PageResponse<StandardCodeResponse>>`로 응답한다. 운영 중 외부 표준 코드 API를 실시간 호출하지 않으며, DB seed에 적재된 코드만 조회한다.

## 7.2 Admin Announcement Source Collection API

외부 공고 수집은 원문 보관과 운영 공고 입력을 분리한다. 수집 원문과 하이라이트는 운영자 검수 참고용이며, 매칭 조건으로 자동 저장하지 않는다. 배치와 관리자 버튼 실행은 모두 먼저 수집 요청을 만들고 승인자가 승인한 뒤 실행한다.

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `POST` | `/api/v1/admin/announcement-source-collections/requests` | `OPERATOR`, `ADMIN` | 수동 수집 요청 생성 |
| `GET` | `/api/v1/admin/announcement-source-collections/requests` | `OPERATOR`, `APPROVER`, `ADMIN` | 수집 요청 목록 |
| `GET` | `/api/v1/admin/announcement-source-collections/requests/{requestId}` | `OPERATOR`, `APPROVER`, `ADMIN` | 수집 요청 상세 |
| `PATCH` | `/api/v1/admin/announcement-source-collections/requests/{requestId}/approval` | `APPROVER`, `ADMIN` | 수집 요청 승인/반려/취소 |
| `POST` | `/api/v1/admin/announcement-source-collections/requests/{requestId}/runs` | `OPERATOR`, `ADMIN` | 승인된 요청 실행 |
| `GET` | `/api/v1/admin/announcement-source-collections/runs` | `OPERATOR`, `APPROVER`, `ADMIN` | 수집 실행 이력 |
| `GET` | `/api/v1/admin/announcement-source-collections/runs/{runId}` | `OPERATOR`, `APPROVER`, `ADMIN` | 수집 실행 상세 |
| `GET` | `/api/v1/admin/announcement-sources` | `OPERATOR`, `APPROVER`, `ADMIN` | 수집 원문 목록 |
| `GET` | `/api/v1/admin/announcement-sources/{sourceId}` | `OPERATOR`, `APPROVER`, `ADMIN` | 수집 원문 상세, 첨부, 하이라이트 조회 |
| `PATCH` | `/api/v1/admin/announcement-sources/{sourceId}/review-status` | `OPERATOR`, `APPROVER`, `ADMIN` | 검수 상태 변경 |
| `PATCH` | `/api/v1/admin/announcement-sources/{sourceId}/duplicate-candidates/{candidateId}/decision` | `OPERATOR`, `ADMIN` | 중복/유사 공고 후보 검수 결정 |
| `PATCH` | `/api/v1/admin/announcement-sources/{sourceId}/source-duplicates/{duplicateId}/decision` | `OPERATOR`, `ADMIN` | 교차 제공자 유사 원문 검수 결정 |
| `POST` | `/api/v1/admin/announcement-sources/{sourceId}/announcements` | `OPERATOR`, `ADMIN` | 수집 원문을 운영 공고 초안으로 연결 |

#### AnnouncementSourceCollectionRequestCreateRequest

```json
{
  "providerCode": "BIZINFO",
  "searchKeyword": "소상공인",
  "searchRegionCode": "서울",
  "searchCategoryCode": "01",
  "maxCount": 100,
  "requestNote": "서울 소상공인 모집 중 공고 확인"
}
```

#### AnnouncementSourceCollectionApprovalRequest

```json
{
  "requestStatusCode": "APPROVED",
  "approvalNote": "검색 조건 확인 후 실행 승인"
}
```

#### AnnouncementSourceSummaryResponse

```json
{
  "sourceId": "uuid",
  "sourceCode": "SRC-000001",
  "providerCode": "BIZINFO",
  "providerLabel": "기업마당",
  "providerNoticeId": "20260601001",
  "title": "소상공인 지원사업",
  "agencyName": "중소벤처기업부",
  "applicationStartDate": "2026-06-01",
  "applicationEndDate": "2026-06-30",
  "reviewStatusCode": "REVIEW_PENDING",
  "reviewStatusLabel": "검수대기",
  "semanticStatusCode": "ACCEPTED",
  "semanticReasonCode": "INCLUDE_KEYWORD_MATCHED",
  "semanticMatchedKeywords": "지원, 모집",
  "postedAt": "2026-05-28T09:00:00+09:00",
  "collectedAt": "2026-05-28T09:05:00+09:00"
}
```

수집 공고 검수 화면은 `reviewStatusCode=REVIEW_PENDING&semanticStatusCode=ACCEPTED`를 기본 조회 조건으로 사용하고 목록에는 `postedAt` 원문 등록일을 표시한다. `semanticStatusCode`는 `ACCEPTED`, `REVIEW_REQUIRED`, `EXCLUDED`다. V70 이후 제목 단계 `EXCLUDED` 공고는 source snapshot을 생성하지 않으므로 원문 목록·상세 API의 조회 대상이 아니다. run 응답에는 제외 건수와 사유만 남고 해당 항목의 `sourceId`, provider 공고번호, URL, 일치 원문은 `null`이다. `GET /api/v1/admin/announcement-sources/{sourceId}`는 저장된 비제외 source에 대해 `attachments[]`, `highlights[]`, `duplicateCandidates[]`, `sourceDuplicates[]`와 의미 판정 필드를 포함한다. `duplicateCandidates[]`는 기존 활성 운영 공고와 비교한 결과다. `sourceDuplicates[]`는 기업마당·정부24·지자체 수집 원문 간 교차 중복 결과다. `matchTypeCode`는 `EXACT_DUPLICATE` 또는 `SIMILAR`이다.

#### AnnouncementSourceDuplicateDecisionRequest

```json
{
  "decisionActionCode": "UPDATE_EXISTING",
  "targetTypeCode": "BUSINESS",
  "incomeJudgementCode": "VAT_TAX_BASE_ONLY",
  "decisionNote": "기존 운영 공고를 원문 기준으로 업데이트"
}
```

`decisionActionCode` 값은 `CREATE_NEW`, `UPDATE_EXISTING`, `IGNORE`이다. `CREATE_NEW`는 해당 후보를 검수 완료 처리하고 신규 운영 공고 DRAFT 생성을 허용한다. `UPDATE_EXISTING`은 후보의 기존 운영 공고 기본정보를 수집 원문 기준으로 갱신하고 수집 원문을 활성 전환한다. `IGNORE`는 후보를 무시 처리한다. 보류 중인 중복/유사 후보가 있으면 `POST /api/v1/admin/announcement-sources/{sourceId}/announcements`는 실패한다.

수집 실행은 신청 마감일이 현재일보다 과거인 공고를 `SKIPPED_ENDED`로 처리한다. 운영자가 검수 후 전환한 운영 공고는 `approval_status_code='DRAFT'`로 생성되며, 원문은 `CONDITION_INPUT_REQUIRED`로 유지한다. 조건과 진행 단계는 기존 공고 입력 화면에서 직접 입력한다. 외부 provider 인증키와 endpoint secret은 환경변수로만 주입한다.

### 7.3 Local Government Notice Source API

모든 URL은 등록·수정·redirect 시 서버에서 SSRF 검증한다. `http`, `https`만 허용하며 loopback, 사설망, link-local, AWS metadata, multicast, 인증정보 포함 URL, 비표준 포트를 차단한다.

지자체 URL 저장 요청은 기존 v1 필드에 선택값 `collectionEndpointUrl`, `requestProfileCode`, `sourceBoardTypeCode`, `collectionPolicyCode`, `semanticallyVerified`, `semanticVerificationNote`를 추가한다. 기존 클라이언트가 의미 검증 필드를 보내지 않으면 신규 등록은 `UNVERIFIED/EXCLUDED`, 수정은 기존 값을 유지한다. `collectionEndpointUrl`은 화면 URL과 실제 공개 데이터 endpoint가 다를 때만 사용하며 동일한 SSRF 검증을 거친다. `requestProfileCode`는 `DEFAULT`, `BROWSER_HTTP1`, `LEGACY_BROWSER`, `TLS12_BROWSER`, `SESSION_BROWSER`를 허용하고 생략하면 `DEFAULT`다. `LEGACY_BROWSER`는 실사이트 격리 QA에서 표준 Java HTTP 클라이언트와 다른 결과가 재현된 기관에만 적용하며 브라우저 호환 헤더와 2배 제한시간을 사용하는 GET 전용 정책이다. `TLS12_BROWSER`는 TLS 1.3 협상을 종료하고 TLS 1.2에서만 정상 응답하는 공식 HTTPS 게시판에 한정한다. 인증서 검증과 URL 검증은 그대로 유지한다. 조회 응답에는 두 필드와 내부 요청 방식인 `requestMethodCode`가 함께 반환된다. `requestMethodCode='POST_FORM'`과 공개 폼 값은 검증된 운영 migration으로만 관리하며 관리자 저장 요청에서 임의 입력받지 않는다. JSON endpoint는 서버에 검증된 `GENERIC_JSON` 파서 프로필이 지정된 경우에만 처리한다.

출처 ON 조건은 URL·파서 검증 완료, 의미 검증 완료, 게시판 유형이 `UNVERIFIED`가 아님, 수집 정책이 `EXCLUDED`가 아님을 모두 충족해야 한다. `PRESS_RELEASE`는 반드시 `EXCLUDED`로 저장하며 ON 전환을 차단한다. 일반 공지는 `KEYWORD_FILTERED`를 사용하고 정적 포함·제외 키워드의 일치 이유를 원문과 실행 항목에 기록한다.

파서 프로필은 서버 정적 seed와 격리 QA 결과로 관리한다. 관리자 화면은 `parserProfileCode` 선택 항목을 노출하지 않으며, 신규 출처는 `MANUAL_ONLY`, 기존 출처 수정은 서버에 저장된 파서 배정을 보존한다. 기존 v1 저장 요청의 `parserProfileCode` 필드는 호환성상 유지하지만 일반 클라이언트 입력으로 파서 배정을 변경하지 않는다. `SAFE_TEMPLATE` 프로필은 링크 함수 리터럴 인자 수와 `arg`, `attr`, `query`, `input` placeholder만 해석하며 JavaScript를 실행하지 않는다. 생성한 상세 URL은 같은 기관의 검증된 host일 때만 저장한다. 대전 통합 목록처럼 외부 전자민원 host로 이동하는 구조는 코드에 고정된 기관별 허용 목록만 사용한다. 2026-07-22 복구 QA에서는 중랑·금천·성남·안양·속초·창원의 현재 공식 화면과 공개 수집 endpoint를 보정하고, 강동·부산 남구의 간헐적 HTTPS 지연은 기존 `LEGACY_BROWSER` 정책과 공개 수집 endpoint로, 해운대·정읍·임실의 HTTPS 전환은 기존 브라우저형 요청 정책으로 보강했다. 사용자 바로가기는 공식 HTTPS 화면을 유지하고 TLS 호환 또는 응답 지연이 있는 공개 공고 목록만 수집 endpoint로 분리한다. 244개 출처는 시스템 QA가 완료되기 전 모두 OFF를 유지하며, 기존 `/api/v1/admin/local-government-notice-parser-profiles`의 path와 응답 구조는 내부 진단 호환을 위해 변경하지 않는다.

2026-07-27 V57 보정에서는 밀양시와 함양군의 일반 공지 URL을 공식 고시·공고 URL로 교체한다. 밀양시는 `SPRING_BBS` GET, 함양군은 `SAFE_SAEOL_EMINWON` 공개 폼 POST를 사용하며 새로운 파서 유형을 추가하지 않는다. 같은 전수 QA와 운영 격리 시험에서 안양시 새올 endpoint timeout 및 공식 HTTPS 화면의 TLS 1.3 협상 종료가 재현되어, 공식 HTTPS 화면을 전용 `TLS12_BROWSER` 요청과 기존 `SPRING_BBS`로 직접 수집한다. 거제시는 `startPage=1`을 명시한 공식 고시공고 HTTPS 목록으로 보정하고, 남동구는 제목·작성일·상세 URL을 제공하는 공식 고시공고 목록으로 교체한다. 속초는 고시공고 의미 분류와 느린 endpoint 요청 정책을 함께 바로잡는다. 여섯 출처의 기존 스냅샷과 실행 이력은 보존되고, 운영자가 ON으로 승인한 이후 수집되는 항목부터 확정된 의미 정책을 적용한다.

2026-07-27 V58은 밀양시·함양군의 사용자 바로가기 URL을 각 기관 대표 누리집의 공식 고시·공고 화면으로 고정한다. 화면 URL과 공개 수집 endpoint가 다른 함양군은 기존 `collectionEndpointUrl` 분리 계약을 사용하며, 관리자와 사용자는 외부 새올 처리 주소가 아니라 함양군 대표 누리집 화면으로 이동한다.

2026-07-27 V59는 새올 전자민원 셀 클릭형 목록에서 중첩 레이아웃 행을 공고 행으로 중복 인식하지 않도록 직접 자식 제목 셀을 가진 행만 선택한다. 수집기는 최근 1년 범위를 벗어난 과거 행과 빈 레이아웃 행을 필드 누락으로 계산하지 않으며, 당일 신규 공고가 등록일 대신 `시:분:초`만 제공하면 서울 기준 오늘 날짜로 해석한다. 실제 후보 행의 제목·등록일·원문 URL 누락만 `PARTIAL_FIELDS`로 남긴다.

2026-07-28 V61~V62는 일반 공지로 분류됐던 203개 기관의 사용자 바로가기를 공식 고시·공고 메뉴로 보정하고, 실제 목록이 별도 새올 endpoint에 있는 경우 `collectionEndpointUrl`과 공개 `POST_FORM` 설정을 분리한다. 격리 전수 QA에서 제목·등록일·별도 상세 URL을 모두 확인한 190개 출처만 활성화하며, 미통과 13개 출처는 OFF 상태와 원본 오류 코드를 유지한다. 강남구·대전 동구·용인시·서천군의 잘못된 공지·채용 URL도 공식 고시·공고 경로로 교체했다. 이 변경은 기존 `/api/v1/admin/local-government-notice-sources`의 path와 응답 필드를 변경하지 않으며, 관리자는 기존 `diagnosticReasonCode` 필터와 원문 바로가기로 보류 원인을 확인한다.

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `GET` | `/api/v1/admin/local-government-notice-sources` | `OPERATOR`, `APPROVER`, `ADMIN` | 지자체 URL 목록, pagination |
| `POST` | `/api/v1/admin/local-government-notice-sources` | `OPERATOR`, `ADMIN` | 지자체 URL OFF 상태 등록 |
| `GET` | `/api/v1/admin/local-government-notice-sources/{sourceId}` | `OPERATOR`, `APPROVER`, `ADMIN` | 지자체 URL 상세 |
| `PUT` | `/api/v1/admin/local-government-notice-sources/{sourceId}` | `OPERATOR`, `ADMIN` | 지자체 URL 수정 |
| `PATCH` | `/api/v1/admin/local-government-notice-sources/{sourceId}/enabled` | `OPERATOR`, `ADMIN` | 검증완료·자동수집 준비 완료 URL ON/OFF |
| `DELETE` | `/api/v1/admin/local-government-notice-sources/{sourceId}` | `ADMIN` | 지자체 URL soft delete |
| `DELETE` | `/api/v1/admin/local-government-notice-sources/qa-artifacts` | `ADMIN` | 확인 문구 검증 후 지자체 QA 수집 원문·요청·실행 이력 삭제 |
| `POST` | `/api/v1/admin/local-government-notice-sources/{sourceId}/collection-requests` | `OPERATOR`, `ADMIN` | 단일 URL 수동 수집 승인 요청 |
| `GET` | `/api/v1/admin/local-government-notice-sources/{sourceId}/collection-results` | `OPERATOR`, `APPROVER`, `ADMIN` | 출처별 최근 수집 결과와 한글 진단, pagination |
| `GET` | `/api/v1/admin/local-government-notice-parser-profiles` | `OPERATOR`, `APPROVER`, `ADMIN` | 수집 파서 목록 |
| `GET` | `/api/v1/admin/local-government-notice-sources/collection-summary` | `OPERATOR`, `APPROVER`, `ADMIN` | 수집 신호등 집계 |
| `GET` | `/api/v1/admin/announcement-source-collection-schedules` | `OPERATOR`, `APPROVER`, `ADMIN` | 정기 수집 일정 목록 |
| `POST` | `/api/v1/admin/announcement-source-collection-schedules` | `OPERATOR`, `ADMIN` | 승인 대기 정기 일정 생성 |
| `PATCH` | `/api/v1/admin/announcement-source-collection-schedules/{scheduleId}/status` | `APPROVER`, `ADMIN` | 일정 승인·중지·반려·만료 |

출처 목록의 선택 query parameter는 `sourceBoardTypeCode`, `collectionPolicyCode`, `semanticallyVerified`, `diagnosticReasonCode`다. `diagnosticReasonCode`는 `TRANSPORT_FAILED`, `PARSER_FAILED`, `PARTIAL_FIELDS`, `SEMANTIC_MISMATCH`, `IRRELEVANT_CONTENT`, `PROCESSING_FAILED`, `UNCLASSIFIED_ERROR`를 지원한다. `PROCESSING_FAILED`는 수집·의미 판정 중 예기치 않은 내부 예외가 발생한 출처를 실행 결과에서 누락하지 않기 위한 관리자 진단값이다. `UNCLASSIFIED_ERROR`는 원본 오류 코드를 유지하면서 미정의 오류를 접속 실패로 오분류하지 않기 위한 안전한 관리자 진단값이다. 수집 실행 상세 응답은 기존 `run`, `items`를 유지하고 URL별 `sourceResults[]`를 추가한다.

`TRANSPORT_FAILED`의 원본 `errorCode`는 기존 `RETRYABLE`, `NETWORK_ERROR`, `HTTP_ERROR`와 함께 `DNS_LOOKUP_FAILED`, `TLS_HANDSHAKE_FAILED`, `CONNECTION_REFUSED`, `CONNECTION_RESET`을 지원한다. 응답에는 stack trace나 기관 응답 본문을 넣지 않고 각 코드에 대응하는 한글 진단 제목과 운영 조치만 반환한다. 네트워크 오류와 제한시간 초과는 최대 3회 재시도하며 기본 제한시간은 15초다.

출처 처리 중 내부 예외가 발생하면 URL별 결과는 `resultStatusCode=FAILED`, `errorCode=PROCESSING_FAILED`로 저장한다. 사용자 화면에는 노출하지 않으며 관리자 화면에는 안전한 안내만 반환한다. 원문 예외는 DB나 API 응답에 저장하지 않고 서버 ERROR 로그에서 실행 ID와 출처 관리코드로 추적한다.

신호등은 오류 URL이 있으면 `RED`, 신규 검수대기 또는 확인 필요 URL이 있으면 `YELLOW`, 오류와 미처리 항목이 없으면 `GREEN`이다. 자동 수집은 `APPROVED` 스케줄만 실행하며 `(schedule_id, scheduled_for)` unique key로 같은 예정시각의 중복 실행을 차단한다.

운영 배포에서는 지자체 승인 일정 실행기와 API 제공자 배치 승인요청 생성기를 활성화한다. API 배치는 기업마당 또는 정부24의 URL과 인증키가 모두 설정된 제공자만 승인요청을 생성하며 인증키 값은 화면과 응답에 노출하지 않는다.

QA 산출물 삭제 요청의 `confirmationText`는 `DELETE_LOCAL_GOVERNMENT_QA_DATA`와 정확히 일치해야 한다. `LOCAL_GOV_NOTICE` 원문이 운영 공고와 연결돼 있으면 전체 삭제를 차단한다. 삭제 범위는 지자체 수집 원문, 요청, 실행, URL별 결과와 스케줄 실행 이력이며 URL 관리 정보, 파서 검증 결과, 운영 공고, 감사 로그는 유지한다.

## 8. Matching API

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `GET` | `/api/v1/standard-document-fields` | authenticated | 공고 조건/입력항목용 표준 서류 필드 목록 |
| `POST` | `/api/v1/matching/cases` | `OPERATOR`, `ADMIN` | 호환용 수동 최종 매칭 생성 |
| `POST` | `/api/v1/matching/cases/candidates` | `OPERATOR`, `ADMIN` | 회원 기본정보 기준 조건 매칭 후보 재계산 |
| `POST` | `/api/v1/matching/cases/final-recalculate` | `OPERATOR`, `ADMIN` | 상담/서류별 선택 입력 이후 최종 매칭 재계산 |
| `GET` | `/api/v1/matching/cases/basic-candidates` | `USER` | 내 기본정보 기준 후보 목록 |
| `GET` | `/api/v1/matching/cases/final` | `OPERATOR`, `APPROVER`, `REVIEWER`, `ADMIN` | 관리자 최종 매칭 후보 목록 |
| `GET` | `/api/v1/matching/cases/member-lookups` | `OPERATOR`, `APPROVER`, `ADMIN` | 매칭 생성용 회원 조회 |
| `GET` | `/api/v1/matching/cases` | `USER`, `PARTNER`, `OPERATOR`, `APPROVER`, `ADMIN` | 매칭 케이스 목록 |
| `GET` | `/api/v1/matching/cases/{matchingCaseId}` | `USER`, `PARTNER`, `OPERATOR`, `APPROVER`, `ADMIN` | 매칭 케이스 상세 |
| `GET` | `/api/v1/matching/cases/{matchingCaseId}/results` | `USER`, `PARTNER`, `OPERATOR`, `APPROVER`, `ADMIN` | 조건별 매칭 결과 |
| `PATCH` | `/api/v1/matching/cases/{matchingCaseId}/status` | `OPERATOR`, `APPROVER`, `ADMIN` | 매칭 상태 수동 변경 |

#### StandardDocumentFieldResponse

```json
[
  {
    "standardFieldId": "uuid",
    "documentTypeCode": "BUSINESS_REGISTRATION",
    "fieldKey": "OPENING_DATE",
    "fieldLabel": "개업일",
    "fieldTypeCode": "DATE",
    "scopeCode": "BUSINESS",
    "requiredDefault": false,
    "conditionEligible": true,
    "conditionUsageCode": "CONDITION_READY",
    "sortOrder": 20,
    "helpText": "사업자등록증에 표시된 개업일입니다."
  }
]
```

`conditionEligible`은 기존 화면 호환을 위해 유지한다. 신규 화면과 서버 검증은 `conditionUsageCode`를 기준으로 한다. `CONDITION_READY`는 자동 조건 저장 가능, `STANDARDIZATION_REQUIRED`는 표준 코드 매핑 후 별도 구조로 처리 필요, `INPUT_ONLY`는 입력/확인 전용이다.

#### MatchingCaseCreateRequest

```json
{
  "announcementId": "uuid",
  "memberUserId": "uuid",
  "verificationId": null
}
```

`verificationId`는 선택값이다. 현재 운영 기준에서는 검증 없이 수동 매칭을 생성할 수 있으며, 이 경우 `matching_cases.verification_id`는 `null`로 저장한다. 검증 ID를 전달한 경우에는 기존처럼 검증 완료, current, matching block 여부를 서버에서 확인한다.

이 endpoint는 기존 v1 호환을 위한 수동 보정용이다. 기본 운영 흐름은 사람이 매칭 케이스를 직접 만드는 방식이 아니라, 사용자 기본정보 저장 시 `BASIC` 후보를 자동 갱신하고 상담/서류별 선택 입력 이후 관리자가 `FINAL` 후보를 재계산한 뒤 진행할 공고를 선택하는 방식이다. 수동 생성 매칭은 `matching_stage_code=FINAL`, `matching_basis_code=PARTNER_INPUT`으로 저장한다.

공고와 회원의 매칭은 다대다 관계로 처리한다. `matching_cases` 한 행은 특정 공고와 특정 회원 사이의 후보 관계를 나타내며, 회원 1명은 여러 공고 후보를 가질 수 있고 공고 1건은 여러 회원 후보를 가질 수 있다. API는 동일 공고-회원 조합의 중복 후보 생성을 막되, 공고 또는 회원을 단일 매칭으로 제한하지 않는다.

#### MatchingCandidateGenerateRequest

```json
{
  "memberUserId": "uuid"
}
```

승인되고 수동 상태가 정상인 공고를 대상으로 사용자의 저장된 기본정보와 공고 조건을 비교한다. 조건이 맞으면 `matching_stage_code=BASIC`, `matching_basis_code=BASIC_INFO` 후보를 생성한다. 회원이 기본정보를 저장하면 서버가 자동으로 기본 후보를 갱신하며, 이 endpoint는 운영자/관리자 재계산용이다.

일반 사용자는 `GET /api/v1/matching/cases/basic-candidates` 응답을 사용하는 `/app/matching/basic-candidates` 화면에서 현재 매칭 공고를 확인한다. 이 화면은 내부 UUID를 노출하지 않고 공고 코드와 매칭 코드 같은 공개 코드만 표시한다. 사용자-facing 명칭은 현재 매칭 공고이지만 내부 의미는 `matching_stage_code=BASIC`인 기본정보 기준 후보이며, 최종 확정 공고가 아니다. 현재 매칭 공고 확인 후 구독 결제와 상담 요청으로 이어진다.

#### MatchingFinalRecalculateRequest

```json
{
  "memberUserId": "uuid"
}
```

구독 이후 상담 요청과 서류별 선택 입력이 진행된 회원을 대상으로 최종 매칭 후보를 재계산한다. 서류별 선택 입력값이 있는 조건은 해당 값을 우선 사용하고, 없으면 기본정보를 사용한다. 조건에 부합하지 않는 기존 최종 후보는 `NOT_MATCHED`로 전환되어 관리자 기본 최종 목록에 노출되지 않는다.

#### MatchingCandidateGenerateResponse

```json
{
  "memberUserId": "uuid",
  "createdCount": 1,
  "skippedCount": 0,
  "candidates": []
}
```

#### MatchingCaseResponse

```json
{
  "matchingCaseId": "uuid",
  "matchingCaseCode": "MCH-000001",
  "announcementId": "uuid",
  "announcementCode": "ANN-000001",
  "memberUserId": "uuid",
  "memberUserCode": "USR-000001",
  "verificationId": "uuid",
  "verificationCode": "VRF-000001",
  "statusCode": "MATCHED",
  "matchingStageCode": "FINAL",
  "matchingBasisCode": "DOCUMENT_INPUT",
  "announcementTitle": "공고명",
  "agencyName": "기관명",
  "targetTypeCode": "BUSINESS",
  "minAmount": 1000000,
  "maxAmount": 5000000,
  "applicationStartDate": "2026-06-01",
  "applicationEndDate": "2026-06-30",
  "memberLoginId": "user01",
  "memberName": "사용자",
  "progressCreated": false,
  "matchedAt": "2026-05-14T10:00:00+09:00"
}
```

`matchingStageCode=BASIC`은 사용자 기본정보 기준의 넓은 후보이고, `matchingStageCode=FINAL`은 상담/서류별 선택 입력 이후 관리자가 진행 공고를 선택하기 위한 최종 후보다. 매칭 응답에는 추천도, 우선순위, 선정확률, 가점 값을 포함하지 않는다.

## 9. Application Progress API

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `POST` | `/api/v1/application-progresses` | `OPERATOR`, `ADMIN` | 최종 매칭 케이스에서 진행 시작 |
| `GET` | `/api/v1/application-progresses` | `USER`, `PARTNER`, `OPERATOR`, `APPROVER`, `ADMIN` | 진행 목록 |
| `GET` | `/api/v1/application-progresses/{progressId}` | `USER`, `PARTNER`, `OPERATOR`, `APPROVER`, `ADMIN` | 진행 상세 |
| `PATCH` | `/api/v1/application-progresses/{progressId}/steps/{stepId}/action` | `USER`, `PARTNER`, `OPERATOR` | 단계 행동 처리 |
| `PUT` | `/api/v1/application-progresses/{progressId}/steps/{stepId}/documents` | `USER`, `PARTNER`, `OPERATOR` | 단계 체크리스트 저장 |
| `PATCH` | `/api/v1/application-progresses/{progressId}/receipt` | `PARTNER`, `OPERATOR` | 접수번호/접수일 저장 |
| `PATCH` | `/api/v1/application-progresses/{progressId}/result` | `PARTNER`, `OPERATOR` | 최종 결과 저장 |

#### ApplicationProgressStartRequest

```json
{
  "matchingCaseId": "uuid"
}
```

신청 진행 생성은 관리자 또는 운영자만 수행한다. 사용자는 기본 후보를 확인하고 구독/상담/서류별 선택 입력 흐름을 진행하지만, 최종 진행 공고 선택은 관리자 화면에서 수행한다. `matching_cases.verification_id`가 `null`이어도 `matching_stage_code=FINAL`, `statusCode=MATCHED`이고 공고 진행 단계가 있으면 신청 진행을 시작할 수 있다.

#### ApplicationProgressDetailsResponse

`GET /api/v1/application-progresses/{progressId}`와 진행 처리 API 응답은 진행 상태, 체크리스트, 현재 공고 단계에 등록된 행동 버튼 목록을 함께 반환한다.

```json
{
  "progressId": "uuid",
  "progressCode": "APP-000001",
  "matchingCaseId": "uuid",
  "matchingCaseCode": "MCH-000001",
  "announcementId": "uuid",
  "announcementCode": "ANN-000001",
  "memberUserId": "uuid",
  "memberUserCode": "USR-000001",
  "currentStepId": "uuid",
  "statusCode": "IN_PROGRESS",
  "stepStates": [
    {
      "stepId": "uuid",
      "stepOrder": 1,
      "stepName": "진행 의사 확인",
      "statusCode": "READY"
    }
  ],
  "checklists": [
    {
      "stepDocumentId": "uuid",
      "stepId": "uuid",
      "documentTypeCode": "BUSINESS_REGISTRATION",
      "required": true,
      "checked": false
    }
  ],
  "stepButtons": [
    {
      "stepId": "uuid",
      "buttonCode": "WANTS_TO_PROGRESS",
      "buttonLabel": "진행 원함",
      "buttonActionCode": "MOVE_NEXT",
      "nextStepId": "uuid",
      "sortOrder": 1
    }
  ]
}
```

화면은 `stepButtons` 중 현재 `currentStepId`와 같은 `stepId`의 버튼만 사용자에게 노출한다. 사용자는 `buttonCode`를 직접 입력하지 않고, 화면은 `buttonLabel`을 버튼 문구로 표시한 뒤 숨은 값으로 `buttonCode`를 서버에 전송한다.

#### ProgressActionRequest

```json
{
  "buttonCode": "WANTS_TO_PROGRESS",
  "input": {}
}
```

#### ProgressChecklistSaveRequest

```json
{
  "documents": [
    {
      "stepDocumentId": "uuid",
      "checked": true
    }
  ]
}
```

#### ProgressReceiptSaveRequest

```json
{
  "receiptNo": "A-2026-0001",
  "receiptDate": "2026-06-15"
}
```

#### ProgressResultSaveRequest

```json
{
  "resultCode": "APPROVED",
  "resultNote": "승인 완료",
  "resultDate": "2026-07-01",
  "receivedAmount": 7000000
}
```

완료 조건을 충족하지 않은 단계 이동은 `PROGRESS_CONDITION_NOT_MET`로 거절한다.
`receivedAmount`는 최종 결과가 `APPROVED`인 경우에만 저장한다. Dashboard의 `totalReceivedAmount`는 `application_progresses.received_amount`가 있고 승인 결과인 진행 건만 합산한다.

## 10. Dashboard API

대시보드 API는 읽기 전용 집계 계약이다. Frontend는 이 응답을 기준으로 화면을 표시하며, 브라우저에서 매칭/우선순위/선정확률/가점 계산을 수행하지 않는다.

MVP v1 DB 집계 기준:

- 진행 가능한 후보는 `matching_cases.matching_stage_code = BASIC` 기준으로 집계한다. `MATCHED`, `REVIEW_REQUIRED`, `PROGRESSED`만 후보로 포함하며, 해당 데이터가 없으면 empty state를 반환한다.
- 사용자 화면의 핵심 후보 분류는 사업자/개인/가족 기준의 `targetCandidateCounts`를 우선 사용한다. 기존 `candidateCounts`는 v1 호환 필드로 유지하되 사용자 대시보드의 주요 분류로 강조하지 않는다.
- 금액 범위는 후보 `matching_cases`에 연결된 `announcements.min_amount`, `announcements.max_amount`의 최소/최대값만 사용한다. `application_progresses.received_amount`와 혼합하지 않는다.
- 현재 해야 할 행동은 `application_step_states.status_code IN (READY, IN_PROGRESS)`인 단계 1건을 우선 반환한다. 진행 단계가 없으면 기본정보 입력, 현재 매칭 공고 확인, 상담 요청, 최종 매칭 대기, 관리자 진행 시작 대기 순서로 1개의 행동만 반환한다. 구독 결제는 `/app/matching/basic-candidates` 화면에서 후보 확인 후 진입한다. 일반 사용자가 직접 신청 진행을 생성하는 route는 반환하지 않는다.
- 진행/승인/수령 금액은 `application_progresses` 기준으로 집계한다. 누적 수령 금액은 `received_amount`가 있고 `status_code IN (APPROVED, COMPLETED)` 또는 `result_code = APPROVED`인 행만 합산한다.
- 개인정보 원문은 대시보드 집계에 포함하지 않고, 사용자 식별자와 진행/공고 운영 데이터만 조인한다.
- 사용자 대시보드 화면은 현재 해야 할 행동, 진행 가능 현황, 누적 현황 3개 영역만 표시한다. 검증/전자증명/재검증/최근 상태 영역은 사용자 화면의 핵심 영역으로 사용하지 않는다.

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `GET` | `/api/v1/dashboard/me/summary` | `USER` | 사용자 대시보드 상단 요약 |
| `GET` | `/api/v1/dashboard/me/current-action` | `USER` | 현재 해야 할 행동 1개 |
| `GET` | `/api/v1/dashboard/me/progress-summary` | `USER` | 진행/결과 누적 요약 |
| `GET` | `/api/v1/dashboard/me/reverification-status` | `USER` | 재검증 필요 여부 |
| `GET` | `/api/v1/admin/dashboard/summary` | `ADMIN` | 관리자 대시보드 운영 집계 |
| `GET` | `/api/v1/operator/dashboard/summary` | `OPERATOR`, `ADMIN` | 운영자 업무 홈 집계 |
| `GET` | `/api/v1/approver/reviews/summary` | `APPROVER`, `ADMIN` | 승인자 큐 집계 |

#### DashboardSummaryResponse

```json
{
  "serviceStatusCode": "BASIC_INFO_REQUIRED",
  "candidateCounts": {
    "policyFund": 3,
    "supportFund": 2,
    "subsidy": 0
  },
  "targetCandidateCounts": {
    "business": 3,
    "personal": 1,
    "family": 1
  },
  "finalMatchedCount": 0,
  "supportAmountRange": {
    "minAmount": 30000000,
    "maxAmount": 70000000,
    "basisCode": "ANNOUNCEMENT_AMOUNT_RANGE"
  },
  "verificationStatusCode": "DRAFT",
  "noticeMessage": "저장된 기본정보 기준으로 진행 가능한 공고가 아직 없습니다."
}
```

`supportAmountRange`는 확정 수령액이 아니라 공고에 등록된 지원금액 범위의 참고 표시다.

DashboardSummaryResponse 필드 계약:

| 필드 | 타입 | nullable | Frontend 사용 기준 |
|---|---|---:|---|
| `serviceStatusCode` | `string` | false | 대시보드 전체 상태 배지 |
| `candidateCounts.policyFund` | `number` | false | v1 호환용 정책자금 후보 건수 |
| `candidateCounts.supportFund` | `number` | false | v1 호환용 지원금 후보 건수 |
| `candidateCounts.subsidy` | `number` | false | v1 호환용 보조금 후보 건수 |
| `targetCandidateCounts.business` | `number` | false | 사용자 화면 우선 표시: 사업자 기준 후보 건수 |
| `targetCandidateCounts.personal` | `number` | false | 사용자 화면 우선 표시: 개인 기준 후보 건수 |
| `targetCandidateCounts.family` | `number` | false | 사용자 화면 우선 표시: 가족 기준 후보 건수 |
| `finalMatchedCount` | `number` | false | 최종 매칭 확정 건수 |
| `supportAmountRange.minAmount` | `number` | true | 공고 기준 최소 지원금액 |
| `supportAmountRange.maxAmount` | `number` | true | 공고 기준 최대 지원금액 |
| `supportAmountRange.basisCode` | `string` | false | 금액 표시 근거 코드 |
| `verificationStatusCode` | `string` | false | 파트너 검증 상태 |
| `noticeMessage` | `string` | true | 사용자 안내 문구 |

#### DashboardCurrentActionResponse

```json
{
  "actionCode": "BASIC_MATCHING_REVIEW_REQUIRED",
  "title": "현재 매칭 공고를 확인해 주세요.",
  "description": "저장한 기본정보와 맞는 공고를 확인한 뒤 구독과 상담을 진행합니다.",
  "primaryButtonLabel": "현재 매칭 공고 보기",
  "route": "/app/matching/basic-candidates",
  "dueDate": null,
  "displayOrder": 10
}
```

`displayOrder`는 화면 표시용 현재 행동 정렬값이다. 매칭 추천도나 선정 우선순위가 아니다.

DashboardCurrentActionResponse 필드 계약:

| 필드 | 타입 | nullable | Frontend 사용 기준 |
|---|---|---:|---|
| `actionCode` | `string` | false | 현재 행동 코드 |
| `title` | `string` | false | 행동 카드 제목 |
| `description` | `string` | true | 행동 설명 |
| `primaryButtonLabel` | `string` | true | 주 버튼 라벨 |
| `route` | `string` | true | 버튼 이동 route |
| `dueDate` | `date` | true | 마감일, `yyyy-MM-dd` |
| `displayOrder` | `number` | false | 화면 정렬용 값 |

#### DashboardProgressSummaryResponse

```json
{
  "inProgressCount": 2,
  "waitingResultCount": 1,
  "approvedCount": 1,
  "supplementRequestedCount": 0,
  "stoppedCount": 0,
  "totalReceivedAmount": 5000000
}
```

`totalReceivedAmount`는 사용자가 직접 입력했거나 파트너/운영자가 결과로 확인한 금액만 합산한다. 공고 기준 지원금액 범위와 합산하지 않는다.

DashboardProgressSummaryResponse 필드 계약:

| 필드 | 타입 | nullable | Frontend 사용 기준 |
|---|---|---:|---|
| `inProgressCount` | `number` | false | 진행 중 건수 |
| `waitingResultCount` | `number` | false | 결과 대기 건수 |
| `approvedCount` | `number` | false | 승인 완료 건수 |
| `supplementRequestedCount` | `number` | false | 보완 요청 건수 |
| `stoppedCount` | `number` | false | 중단 건수 |
| `totalReceivedAmount` | `number` | false | 확정 수령 금액 합계 |

#### OperatorDashboardSummaryResponse

```json
{
  "announcementWork": {
    "draftCount": 3,
    "requestedCount": 2,
    "openAnnouncementCount": 8,
    "pausedAnnouncementCount": 1,
    "closedAnnouncementCount": 6
  },
  "matchingWork": {
    "matchedCount": 12,
    "reviewRequiredCount": 2,
    "blockedCount": 0,
    "progressedCount": 7
  },
  "applicationProgressWork": {
    "readyCount": 1,
    "inProgressCount": 2,
    "waitingResultCount": 1,
    "approvedCount": 2,
    "supplementRequestedCount": 0,
    "stoppedCount": 0,
    "totalReceivedAmount": 5000000
  }
}
```

OperatorDashboardSummaryResponse는 운영자 업무 현황을 표시하기 위한 읽기 전용 집계다. 공고 입력, 매칭 관리, 신청 진행 업무의 처리 대기 건수를 보여주며, 매칭 추천도, 선정확률, 우선순위, 가점 계산을 포함하지 않는다.

#### ApproverReviewSummaryResponse

```json
{
  "announcementReview": {
    "requestedCount": 4,
    "rejectedCount": 1,
    "approvedCount": 9
  },
  "verificationReview": {
    "submittedCount": 5,
    "reviewingCount": 2,
    "verifiedCount": 8,
    "rejectedCount": 1
  },
  "matchingReview": {
    "reviewRequiredCount": 3,
    "blockedCount": 1,
    "progressedCount": 7
  },
  "progressReview": {
    "waitingResultCount": 6,
    "approvedCount": 4,
    "supplementRequestedCount": 2,
    "stoppedCount": 1
  }
}
```

ApproverReviewSummaryResponse는 승인자 업무 현황을 표시하기 위한 읽기 전용 집계다. 공고 승인 요청, 검증 검토, 매칭 확인, 최종 결과 대기 건수를 보여주며, 개인정보 원문과 추천도, 선정확률, 우선순위, 가점 계산을 포함하지 않는다.

#### DashboardReverificationStatusResponse

```json
{
  "required": true,
  "lastVerifiedAt": "2025-11-14T10:00:00+09:00",
  "reasonCode": "VERIFICATION_EXPIRED",
  "requiredItems": [
    "BUSINESS_STATUS",
    "TAX_STATUS",
    "FINANCIAL_STATUS"
  ]
}
```

재검증 기준일과 만료 정책은 Backend에서 관리한다.

DashboardReverificationStatusResponse 필드 계약:

| 필드 | 타입 | nullable | Frontend 사용 기준 |
|---|---|---:|---|
| `required` | `boolean` | false | 재검증 필요 여부 |
| `lastVerifiedAt` | `datetime` | true | 마지막 검증 일시, offset 포함 |
| `reasonCode` | `string` | true | 재검증 사유 코드 |
| `requiredItems` | `string[]` | false | 재검증 필요 항목 코드 목록 |

#### AdminDashboardSummaryResponse

```json
{
  "userSummary": {
    "totalUserCount": 12,
    "activeUserCount": 10,
    "userRoleCount": 8,
    "partnerRoleCount": 2,
    "operatorRoleCount": 1,
    "approverRoleCount": 1,
    "adminRoleCount": 1
  },
  "announcementSummary": {
    "totalAnnouncementCount": 20,
    "draftCount": 3,
    "requestedCount": 2,
    "approvedCount": 12,
    "rejectedCount": 1,
    "openAnnouncementCount": 8,
    "pausedAnnouncementCount": 1,
    "closedAnnouncementCount": 6
  },
  "verificationSummary": {
    "totalVerificationCount": 15,
    "reviewQueueCount": 4,
    "verifiedCount": 9,
    "rejectedCount": 1,
    "statusCounts": [
      {
        "statusCode": "SUBMITTED",
        "count": 2
      }
    ]
  },
  "matchingSummary": {
    "totalMatchingCaseCount": 30,
    "matchedCount": 12,
    "reviewRequiredCount": 3,
    "blockedCount": 1,
    "progressedCount": 8,
    "statusCounts": [
      {
        "statusCode": "MATCHED",
        "count": 12
      }
    ]
  },
  "applicationProgressSummary": {
    "totalProgressCount": 8,
    "activeProgressCount": 3,
    "waitingResultCount": 1,
    "approvedCount": 2,
    "supplementRequestedCount": 0,
    "stoppedCount": 0,
    "completedCount": 1,
    "totalReceivedAmount": 5000000,
    "statusCounts": [
      {
        "statusCode": "IN_PROGRESS",
        "count": 2
      }
    ]
  },
  "auditSummary": {
    "totalAuditCount": 100,
    "failAuditCount": 4,
    "recentFailAuditCount": 1
  }
}
```

관리자 대시보드는 별도 저장 테이블을 만들지 않고 현재 V1 테이블을 읽기 전용으로 집계한다. 개인정보 원문, secret, 추천도, 우선순위, 선정확률, 가점 계산은 응답에 포함하지 않는다.

## 11. Frontend Enum / Status 표시값

Frontend는 아래 표시값을 1차 착수 기준으로 사용한다. 목록에 없는 코드는 코드 원문을 fallback으로 표시하고, Backend 계약 확정 없이 추천도, 우선순위, 선정확률, 가점 의미를 추가하지 않는다.

| 코드 그룹 | 코드 | 표시값 |
|---|---|---|
| `role_code` | `USER` | 일반 사용자 |
| `role_code` | `PARTNER` | 파트너 |
| `role_code` | `OPERATOR` | 운영자 |
| `role_code` | `APPROVER` | 승인자 |
| `role_code` | `REVIEWER` | 검수자 |
| `role_code` | `ADMIN` | 관리자 |
| `user_status_code` | `ACTIVE` | 정상 |
| `user_status_code` | `LOCKED` | 잠김 |
| `user_status_code` | `DISABLED` | 비활성 |
| `user_status_code` | `DELETED` | 삭제 |
| `verification_status_code` | `DRAFT` | 검증 전 |
| `verification_status_code` | `SUBMITTED` | 제출 완료 |
| `verification_status_code` | `REVIEWING` | 검토 중 |
| `verification_status_code` | `VERIFIED` | 검증 완료 |
| `verification_status_code` | `REJECTED` | 반려 |
| `verification_status_code` | `EXPIRED` | 만료 |
| `matching_status_code` | `MATCHED` | 조건 일치 |
| `matching_status_code` | `NOT_MATCHED` | 조건 불일치 |
| `matching_status_code` | `REVIEW_REQUIRED` | 검토 필요 |
| `matching_status_code` | `BLOCKED` | 제한 확인 |
| `matching_status_code` | `PROGRESSED` | 진행 전환 |
| `progress_status_code` | `READY` | 준비 |
| `progress_status_code` | `IN_PROGRESS` | 진행 중 |
| `progress_status_code` | `WAITING_RESULT` | 결과 대기 |
| `progress_status_code` | `APPROVED` | 승인 |
| `progress_status_code` | `REJECTED` | 탈락 |
| `progress_status_code` | `SUPPLEMENT_REQUESTED` | 보완 요청 |
| `progress_status_code` | `STOPPED` | 중단 |
| `progress_status_code` | `COMPLETED` | 완료 |
| `business_type_code` | `SOLE_PROPRIETOR` | 개인사업자 |
| `business_type_code` | `CORPORATION` | 법인사업자 |
| `business_type_code` | `SIMPLIFIED_TAXPAYER` | 간이과세자 |
| `business_type_code` | `GENERAL_TAXPAYER` | 일반과세자 |
| `business_type_code` | `TAX_EXEMPT` | 면세사업자 |
| `company_stage_code` | `PRE_STARTUP` | 창업 전 |
| `company_stage_code` | `EARLY_STARTUP` | 초기 창업 |
| `company_stage_code` | `OPERATING` | 운영 중 |
| `company_stage_code` | `SUSPENDED` | 휴업 |
| `company_stage_code` | `CLOSURE_PLANNED` | 폐업 예정 |
| `company_stage_code` | `CLOSED` | 폐업 |
| `company_stage_code` | `RESTART_PREPARING` | 재개 준비 |
| `relation_type_code` | `SPOUSE` | 배우자 |
| `relation_type_code` | `CHILD` | 자녀 |
| `relation_type_code` | `PARENT` | 부모 |
| `service_status_code` | `VERIFICATION_REQUIRED` | 검증 필요 |
| `service_status_code` | `MATCHING_READY` | 매칭 준비 |
| `service_status_code` | `IN_PROGRESS` | 진행 중 |
| `service_status_code` | `WAITING_RESULT` | 결과 대기 |
| `service_status_code` | `COMPLETED` | 완료 |
| `dashboard_action_code` | `MEMBER_PROFILE_REQUIRED` | 회원 정보 입력 필요 |
| `dashboard_action_code` | `BUSINESS_PROFILE_REQUIRED` | 사업자 정보 입력 필요 |
| `dashboard_action_code` | `FAMILY_PROFILE_REQUIRED` | 가족 정보 확인 필요 |
| `dashboard_action_code` | `VERIFICATION_DOCUMENT_REQUIRED` | 서류 확인 필요 |
| `dashboard_action_code` | `PROGRESS_ACTION_REQUIRED` | 진행 단계 확인 필요 |
| `dashboard_action_code` | `NONE` | 할 일 없음 |
| `reverification_reason_code` | `VERIFICATION_EXPIRED` | 검증 만료 |
| `reverification_reason_code` | `BUSINESS_STATUS_CHANGED` | 사업 상태 변경 |
| `reverification_reason_code` | `TAX_STATUS_REQUIRED` | 세금 상태 확인 필요 |
| `reverification_reason_code` | `FINANCIAL_STATUS_REQUIRED` | 금융 상태 확인 필요 |

## 12. Consent API

동의 이력은 운영 감사 로그와 분리해 `user_consents`에 저장한다. 개인정보 원문이나 외부 API 응답 원문은 동의 이력에 저장하지 않는다.

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `GET` | `/api/v1/consents/current` | anonymous | 현재 유효한 동의 항목 목록 |
| `GET` | `/api/v1/users/me/consents` | authenticated | 내 동의 이력 목록 |
| `POST` | `/api/v1/users/me/consents` | authenticated | 내 동의 이력 저장 |

#### CurrentConsentResponse

```json
{
  "consentVersionId": "uuid",
  "consentCode": "PRIVACY_POLICY",
  "consentName": "개인정보 처리방침",
  "versionNo": 1,
  "required": true,
  "effectiveFrom": "2026-06-08T10:00:00+09:00"
}
```

#### ConsentSaveRequest

```json
{
  "consentCode": "E_CERT",
  "consented": true
}
```

`consentCode`는 `TERMS_OF_SERVICE`, `PRIVACY_POLICY`, `E_CERT`, `CREDIT_CHECK`만 허용한다. MVP에서는 `consented=true` 저장만 허용한다.

#### UserConsentResponse

```json
{
  "userConsentId": "uuid",
  "consentVersionId": "uuid",
  "consentCode": "E_CERT",
  "consentName": "전자증명 이용 동의",
  "versionNo": 1,
  "consented": true,
  "consentedAt": "2026-06-08T10:00:00+09:00"
}
```

회원가입 성공 시 `TERMS_OF_SERVICE`, `PRIVACY_POLICY` 2건은 자동 저장한다. 전자증명과 신용조회 동의는 사용자가 해당 기능을 실제로 진행할 때 별도 저장한다.

## 13. File / Document Submission API

파일 원문은 `STORAGE_ROOT` 하위의 비공개 저장소에 저장하고, DB에는 파일 메타데이터와 서류 제출 이력만 저장한다. 감사 로그 metadata에는 원본 파일명, 파일 내용, 개인정보 원문을 저장하지 않는다.

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `POST` | `/api/v1/files` | authenticated | multipart 파일 업로드 |
| `GET` | `/api/v1/files/{fileId}` | authenticated | 파일 메타데이터 조회 |
| `POST` | `/api/v1/document-submissions` | `USER`, `PARTNER`, `OPERATOR`, `ADMIN` | 검증 건 또는 신청 진행 건에 파일 제출 |
| `GET` | `/api/v1/document-submissions` | `USER`, `PARTNER`, `OPERATOR`, `APPROVER`, `ADMIN` | 서류 제출 이력 조회 |
| `PATCH` | `/api/v1/document-submissions/{submissionId}/review` | `PARTNER`, `OPERATOR`, `APPROVER`, `ADMIN` | 서류 승인/반려 검토 |

#### StoredFileResponse

```json
{
  "fileId": "uuid",
  "ownerUserId": "uuid",
  "originalFilename": "business.pdf",
  "contentType": "application/pdf",
  "fileSize": 1024,
  "checksumSha256": "sha256-hex",
  "statusCode": "STORED",
  "createdAt": "2026-06-08T10:00:00+09:00"
}
```

#### DocumentSubmissionCreateRequest

```json
{
  "fileId": "uuid",
  "resourceTypeCode": "APPLICATION_PROGRESS",
  "resourceId": "uuid",
  "documentTypeCode": "BUSINESS_REGISTRATION"
}
```

`resourceTypeCode`는 `PARTNER_VERIFICATION`, `APPLICATION_PROGRESS`만 허용한다. 일반 사용자는 본인 검증 건 또는 본인 신청 진행 건에만 제출할 수 있다.

#### DocumentSubmissionReviewRequest

```json
{
  "statusCode": "APPROVED",
  "reviewNote": "확인 완료"
}
```

검토 `statusCode`는 `APPROVED`, `REJECTED`만 허용한다.

#### DocumentSubmissionResponse

```json
{
  "submissionId": "uuid",
  "fileId": "uuid",
  "originalFilename": "business.pdf",
  "contentType": "application/pdf",
  "fileSize": 1024,
  "resourceTypeCode": "APPLICATION_PROGRESS",
  "resourceId": "uuid",
  "documentTypeCode": "BUSINESS_REGISTRATION",
  "statusCode": "SUBMITTED",
  "submittedBy": "uuid",
  "submittedAt": "2026-06-08T10:00:00+09:00",
  "reviewedBy": null,
  "reviewedAt": null,
  "reviewNote": null
}
```

## 14. Consultation API

상담 예약은 파트너가 등록한 가능 시간과 사용자의 예약 요청을 분리해 저장한다. 중복 예약은 같은 slot에 대한 active 예약 partial unique index로 차단한다.

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `GET` | `/api/v1/consultation-slots` | `USER`, `PARTNER`, `OPERATOR`, `REVIEWER`, `ADMIN` | 상담 가능 시간 조회 |
| `POST` | `/api/v1/consultation-slots` | `PARTNER`, `OPERATOR`, `ADMIN` | 상담 가능 시간 등록 |
| `PATCH` | `/api/v1/consultation-slots/{slotId}/status` | `PARTNER`, `OPERATOR`, `ADMIN` | 상담 가능 시간 상태 변경 |
| `GET` | `/api/v1/consultation-reservations` | `USER`, `PARTNER`, `OPERATOR`, `REVIEWER`, `ADMIN` | 상담 예약 목록 조회 |
| `POST` | `/api/v1/consultation-reservations` | `USER`, `OPERATOR`, `ADMIN` | 상담 예약 요청 |
| `PATCH` | `/api/v1/consultation-reservations/{reservationId}/status` | `USER`, `PARTNER`, `OPERATOR`, `ADMIN` | 상담 예약 확정/취소/완료 처리 |

#### ConsultationSlotCreateRequest

```json
{
  "partnerUserId": "uuid",
  "startAt": "2026-06-20T10:00:00+09:00",
  "endAt": "2026-06-20T10:30:00+09:00",
  "note": "오전 상담"
}
```

파트너는 본인 slot만 등록할 수 있다. 운영자와 관리자는 `partnerUserId`를 지정할 수 있다.

#### ConsultationReservationCreateRequest

```json
{
  "slotId": null,
  "memberUserId": null,
  "memberUserCode": "USR-000001",
  "partnerUserId": null,
  "partnerUserCode": null,
  "progressId": null,
  "verificationId": null,
  "requestNote": "전화 상담 희망"
}
```

일반 사용자는 본인 상담 요청만 생성할 수 있다. `slotId` 없이 접수할 수 있으며, 운영자와 관리자는 `memberUserId` 또는 `memberUserCode`, `partnerUserId` 또는 `partnerUserCode`, `slotId`를 지정해 수기 접수할 수 있다. 화면에서는 `memberUserCode`, `partnerUserCode`를 우선 사용한다. 사용자가 생성한 요청의 담당자 배정은 운영자 또는 관리자가 수행한다.

#### ConsultationReservationStatusUpdateRequest

```json
{
  "statusCode": "ASSIGNED",
  "partnerUserId": "uuid",
  "partnerUserCode": "USR-000002",
  "slotId": null,
  "note": "확정"
}
```

예약 상태 흐름은 `REQUESTED -> ASSIGNED|CONFIRMED|CANCELED`, `ASSIGNED -> CONFIRMED|CANCELED`, `CONFIRMED -> COMPLETED|NO_SHOW|CANCELED`만 허용한다. `ASSIGNED`, `CONFIRMED` 처리에는 담당자 `partnerUserId` 또는 `partnerUserCode`가 필요하다. 화면에서는 `partnerUserCode`를 우선 사용한다. 일반 사용자는 본인 예약 취소만 가능하다.

## 15. Subscription / Payment API

PG사는 TossPayments로 결정한다. API와 DB는 `providerCode='TOSS'`를 허용하는 provider 중립 계약을 유지하고, 운영 secret은 `PAYMENT_WEBHOOK_SECRET` 및 TossPayments 운영 key 환경변수로만 주입한다. 결제사 webhook payload 원문은 저장하지 않고 비식별 이벤트 metadata만 저장한다.

MVP 1차는 월 단순 구독 구조다. 복잡한 할인, 사용량 과금, 자동 청구 retry, billing key 저장, TossPayments 승인 API 실연동은 Toss 상점 계약, client key/secret key, webhook URL, 결제 성공/실패 redirect URL, 자동결제 여부가 확정된 뒤 별도 Payment Hardening Gate에서 연결한다.

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `GET` | `/api/v1/subscription-plans` | authenticated | 요금제 목록 |
| `POST` | `/api/v1/subscription-plans` | `OPERATOR`, `ADMIN` | 요금제 등록 |
| `PATCH` | `/api/v1/subscription-plans/{planId}/status` | `OPERATOR`, `ADMIN` | 요금제 활성/비활성 변경 |
| `GET` | `/api/v1/subscriptions` | `USER`, `OPERATOR`, `ADMIN` | 구독 목록 |
| `POST` | `/api/v1/subscriptions` | `USER`, `OPERATOR`, `ADMIN` | 구독 생성 |
| `PATCH` | `/api/v1/subscriptions/{subscriptionId}/cancel` | `USER`, `OPERATOR`, `ADMIN` | 구독 취소 |
| `GET` | `/api/v1/payments` | `USER`, `OPERATOR`, `ADMIN` | 결제 거래 목록 |
| `POST` | `/api/v1/payments` | `USER`, `OPERATOR`, `ADMIN` | 결제 요청 거래 생성 |
| `PATCH` | `/api/v1/payments/{paymentId}/status` | `OPERATOR`, `ADMIN` | 결제 승인/실패/취소 기록 |
| `POST` | `/api/v1/mock-payments/monthly-subscription` | `USER` | 월 구독 mock 결제 처리 |
| `GET` | `/api/v1/refunds` | `USER`, `OPERATOR`, `ADMIN` | 환불 거래 목록 |
| `POST` | `/api/v1/refunds` | `USER`, `OPERATOR`, `ADMIN` | 환불 요청 생성 |
| `PATCH` | `/api/v1/refunds/{refundId}/status` | `OPERATOR`, `ADMIN` | 환불 승인/실패 기록 |
| `POST` | `/api/v1/payment-webhooks/{providerCode}` | webhook secret | 결제사 이벤트 수신 |

관리자 화면은 결제를 진행하지 않는다. 운영자와 관리자는 `/app/billing/plans`에서 월 구독으로 받을 금액만 등록하거나 활성/비활성 처리한다. 사용자 결제 화면은 `/app/billing/mock`이며 일반 사용자에게만 제공한다.

#### SubscriptionPlanCreateRequest

```json
{
  "planCode": "BASIC",
  "planName": "기본 요금제",
  "billingCycleCode": "MONTHLY",
  "priceAmount": 99000,
  "currencyCode": "KRW",
  "active": true,
  "sortOrder": 10,
  "description": "기본 이용권"
}
```

#### UserSubscriptionCreateRequest

```json
{
  "userId": null,
  "planId": "uuid"
}
```

일반 사용자는 본인 구독만 생성할 수 있다. 운영자와 관리자는 `userId`를 지정할 수 있다. 무료 요금제는 즉시 `ACTIVE`, 유료 요금제는 결제 전 `PENDING`으로 생성한다.

#### PaymentCreateRequest

```json
{
  "subscriptionId": "uuid",
  "providerCode": "MANUAL",
  "amount": 99000,
  "currencyCode": "KRW"
}
```

`amount`와 `currencyCode`는 요금제 금액/통화와 일치해야 한다. `providerCode`는 `MANUAL`, `TOSS`, `NICEPAY`, `KCP`, `STRIPE`만 허용한다.

#### MockMonthlyPaymentRequest

```json
{
  "planId": "uuid",
  "simulateFailure": false
}
```

이번 MVP Goal의 월 구독 검증용 계약이다. TossPayments 실제 승인 API를 호출하지 않고 `payment_transactions.provider_code='MANUAL'`로 결제 거래를 기록한다. 성공 시 구독 상태는 `ACTIVE`, 실패 시 `PAST_DUE`로 저장한다. 이미 활성화된 구독이 있으면 중복 결제를 차단한다.

#### PaymentStatusUpdateRequest

```json
{
  "statusCode": "APPROVED",
  "providerPaymentKey": "provider-key",
  "failureCode": null,
  "failureMessage": null
}
```

결제 상태 흐름은 `REQUESTED -> APPROVED|FAILED|CANCELED`만 허용한다. `APPROVED`가 되면 해당 구독은 `ACTIVE`가 된다.

#### RefundCreateRequest

```json
{
  "paymentId": "uuid",
  "refundAmount": 99000,
  "reason": "취소 요청"
}
```

승인된 결제만 환불 요청이 가능하다. 이미 승인된 환불 금액과 신규 환불 금액의 합계는 결제 금액을 초과할 수 없다.

#### PaymentProviderEventRequest

```json
{
  "eventId": "provider-event-id",
  "paymentId": "uuid",
  "refundId": null,
  "merchantUid": "SANEB-20260608101010-12345678",
  "providerPaymentKey": "provider-payment-key",
  "providerRefundKey": null,
  "eventTypeCode": "PAYMENT_APPROVED",
  "amount": 99000,
  "currencyCode": "KRW",
  "failureCode": null,
  "failureMessage": null
}
```

webhook 요청은 `X-SANEB-WEBHOOK-SECRET` header가 `PAYMENT_WEBHOOK_SECRET` 환경변수와 일치할 때만 처리한다. `PAYMENT_WEBHOOK_SECRET`이 비어 있으면 webhook 처리는 거부된다.

## 16. Notification / Operation Task API

외부 이메일/SMS/카카오 실제 발송 provider는 아직 고정하지 않는다. MVP 이후 확장 구간에서도 알림 메시지와 발송 이력은 먼저 DB에 남기고, provider payload 원문과 secret은 저장하지 않는다.

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `GET` | `/api/v1/notifications/me` | authenticated | 내 알림 목록 |
| `PATCH` | `/api/v1/notifications/{notificationId}/read` | authenticated | 내 알림 읽음 처리 |
| `POST` | `/api/v1/admin/notifications/send` | `OPERATOR`, `ADMIN` | 운영 알림 생성/발송 기록 |
| `GET` | `/api/v1/operation-tasks` | `OPERATOR`, `ADMIN` | 운영 업무 큐 목록 |
| `POST` | `/api/v1/operation-tasks` | `OPERATOR`, `ADMIN` | 운영 업무 생성 |
| `PATCH` | `/api/v1/operation-tasks/{taskId}/status` | `OPERATOR`, `ADMIN` | 운영 업무 상태 변경 |
| `POST` | `/api/v1/operation-tasks/{taskId}/comments` | `OPERATOR`, `ADMIN` | 운영 업무 댓글 등록 |
| `POST` | `/api/v1/operation-tasks/{taskId}/assignments` | `OPERATOR`, `ADMIN` | 운영 업무 담당자 배정 |

사용자 화면은 `/app/notifications`에서 내 알림을 조회하고 읽음 처리한다. 상단 알림 배지는 `/api/v1/notifications/me?unreadOnly=true`의 실제 미확인 건수로 표시한다. 운영자와 관리자는 `/app/operation-tasks`에서 운영 업무 큐를 조회하고 상태를 처리한다.

#### NotificationSendRequest

```json
{
  "recipientUserId": "uuid",
  "templateCode": null,
  "channelCode": "IN_APP",
  "title": "보완 요청",
  "body": "서류 보완이 필요합니다.",
  "resourceType": "APPLICATION_PROGRESS",
  "resourceId": "uuid"
}
```

`IN_APP`은 즉시 `SENT`로 저장하고 delivery log는 `SUCCESS`로 남긴다. `EMAIL`, `SMS`, `KAKAO`는 provider가 설정되기 전까지 message는 `CREATED`, delivery log는 `SKIPPED`로 남긴다.

#### OperationTaskCreateRequest

```json
{
  "taskTypeCode": "SUPPLEMENT_REQUEST",
  "priorityCode": "HIGH",
  "title": "보완 요청 확인",
  "description": "사용자 보완 요청 확인",
  "resourceType": "APPLICATION_PROGRESS",
  "resourceId": "uuid",
  "dueAt": "2026-06-20T10:00:00+09:00",
  "assigneeUserIds": ["uuid"]
}
```

운영 업무 상태는 `OPEN -> IN_PROGRESS|WAITING|DONE|CANCELED`, `IN_PROGRESS|WAITING -> IN_PROGRESS|WAITING|DONE|CANCELED`만 허용한다. `DONE`, `CANCELED` 이후 상태 변경은 차단한다.

### Progress Inactivity Monitor

장기 미진행 분류는 별도 수동 API가 아니라 서버 스케줄러가 기존 `notification_messages`, `progress_reminder_logs`, `operation_tasks`에 기록한다.

| 기준 | 처리 |
|---|---|
| 24시간 미진행 | `FIRST_REMINDER` 인앱 알림 |
| 48시간 미진행 | `RE_GUIDE` 인앱 알림 |
| 공고 마감 2일 전 | `DEADLINE_D_MINUS_2` 인앱 알림. 상시 접수 또는 마감일 미입력 공고는 제외 |
| 7일 이상 미진행 | `LONG_STALLED` 인앱 알림 및 `DELAYED_PROGRESS` 운영 업무 |
| 14일 이상 미진행 | `TM_RECONTACT` 인앱 알림 및 `RECONTACT` 운영 업무 |
| 신규 가능 항목 발생 | `BASIC` 매칭 후보가 새로 생성될 때 사용자에게 인앱 알림 |
| 6개월 정보 미갱신 | 회원 기본정보, 사업자 정보, 가족 정보 기준 최근 수정일이 6개월을 넘으면 인앱 재확인 알림 |

사용자가 단계 이동, 체크리스트 저장, 동적 입력 저장 등 행동을 완료하면 `application_progresses.updated_at`이 갱신되며 이후 리마인드 판단에서 제외된다. 스케줄러는 `SANEB_INACTIVITY_REMINDER_ENABLED`, `SANEB_INACTIVITY_REMINDER_FIXED_DELAY_MS`, `SANEB_INACTIVITY_REMINDER_INITIAL_DELAY_MS`, `SANEB_INACTIVITY_REMINDER_BATCH_SIZE` 환경변수로 조정한다.

## 17. Admin Report API

관리자 리포트는 운영 상태 요약과 요약 내보내기만 제공한다. 내보내기 결과는 `ApiResponse` wrapper 안에 텍스트 content로 반환하며, 개인정보 원문은 포함하지 않는다.

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `GET` | `/api/v1/admin/reports/summary` | `ADMIN` | 운영 요약 리포트 |
| `POST` | `/api/v1/admin/reports/exports` | `ADMIN` | 리포트 내보내기 생성 |
| `GET` | `/api/v1/admin/reports/exports/{exportId}` | `ADMIN` | 리포트 내보내기 상세 |
| `GET` | `/api/v1/admin/reports/exports/{exportId}/download` | `ADMIN` | 리포트 내보내기 content 조회 |

#### ReportExportCreateRequest

```json
{
  "reportTypeCode": "OPERATION_SUMMARY",
  "formatCode": "CSV"
}
```

`formatCode`는 `CSV`, `EXCEL`을 허용한다. 현재 `EXCEL`은 브라우저에서 열 수 있는 tab-separated content로 반환한다.

## 18. Admin App Log API

앱 로그 관리는 관리자 전용 읽기 기능이다. 로그 파일 경로는 `SANEB_APP_LOG_PATH` 환경변수로 지정하며, 기본값은 배포 스크립트의 `/home/ubuntu/app/app.log`와 맞춘다. 로그 삭제, 원문 다운로드, 임의 경로 조회는 제공하지 않는다.

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `GET` | `/api/v1/admin/app-logs` | `ADMIN` | 최근 앱 로그 조회 |

목록 query:

| 필드 | 설명 |
|---|---|
| `levelCode` | 선택. `INFO`, `WARN`, `ERROR`, `DEBUG` 중 하나 |
| `keyword` | 선택. 최근 로그 안에서 대소문자 구분 없이 검색 |
| `lines` | 선택. 기본 120, 최대 500 |

응답:

```json
{
  "logPath": "/home/ubuntu/app/app.log",
  "available": true,
  "fileSizeBytes": 1024,
  "lastModifiedAt": "2026-06-08T10:00:00+09:00",
  "requestedLines": 120,
  "returnedLines": 1,
  "levelCode": "ERROR",
  "keyword": "payment",
  "message": "최근 로그를 조회했습니다.",
  "lines": [
    {
      "sequenceNo": 1,
      "content": "2026-06-08 ERROR sample"
    }
  ]
}
```

`password`, `token`, `secret`, `apiKey`, `authorization` 형태의 값은 화면과 API 응답에서 마스킹한다. 그래도 로그는 운영 민감정보가 포함될 수 있으므로 `ADMIN` 외에는 접근할 수 없다.

## 19. AI Assist API

AI 보조는 운영자 업무 초안 생성에만 사용한다. 입력 원문은 DB와 감사 로그에 저장하지 않고 `input_hash_sha256`, `input_length`, provider/model/status metadata만 저장한다. 기본 provider는 외부 호출이 없는 `LOCAL_SAFE`이며, 외부 provider를 붙이는 경우에도 운영 secret은 환경변수로만 주입한다.

금지:

- AI 자동 승인
- AI 자동 탈락
- 개인정보 원문 외부 전송
- 선정확률, 우선순위, 추천도, 가점 자동 계산

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `POST` | `/api/v1/ai-assist/requests` | `OPERATOR`, `ADMIN` | AI 보조 초안 생성 요청 |
| `GET` | `/api/v1/ai-assist/requests` | `OPERATOR`, `ADMIN` | AI 보조 요청 목록 |
| `GET` | `/api/v1/ai-assist/requests/{requestId}` | `OPERATOR`, `ADMIN` | AI 보조 요청 상세 |
| `PATCH` | `/api/v1/ai-assist/results/{resultId}/review` | `OPERATOR`, `ADMIN` | AI 보조 결과 검토 상태 변경 |

#### AiAssistCreateRequest

```json
{
  "assistTypeCode": "ANNOUNCEMENT_SUMMARY",
  "resourceType": "ANNOUNCEMENT",
  "resourceId": "uuid",
  "inputText": "공고 원문 또는 운영 메모",
  "operatorNote": "초안 작성 참고 메모"
}
```

`inputText`는 provider 호출 또는 local 초안 생성에만 사용하고 DB에는 저장하지 않는다. 저장값은 SHA-256 hash와 글자 수뿐이다.

#### AiAssistResponse

```json
{
  "requestId": "uuid",
  "resultId": "uuid",
  "assistTypeCode": "ANNOUNCEMENT_SUMMARY",
  "resourceType": "ANNOUNCEMENT",
  "resourceId": "uuid",
  "requestStatusCode": "COMPLETED",
  "providerCode": "LOCAL_SAFE",
  "modelCode": "RULE_TEMPLATE_V1",
  "reviewStatusCode": "PENDING_REVIEW",
  "resultText": "운영자 검토용 초안",
  "requestedBy": "uuid",
  "createdAt": "2026-06-08T10:00:00+09:00",
  "completedAt": "2026-06-08T10:00:01+09:00"
}
```

`resultText`는 운영자 검토 전 사용자에게 확정 안내로 표시하지 않는다.

#### AiAssistReviewRequest

```json
{
  "reviewStatusCode": "ACCEPTED"
}
```

허용값:

| 코드 그룹 | 값 |
|---|---|
| `ai_assist_type_code` | `ANNOUNCEMENT_SUMMARY`, `DOCUMENT_DRAFT`, `OPERATION_MEMO_SUMMARY`, `USER_REPLY_DRAFT` |
| `ai_assist_resource_type` | `GENERAL`, `ANNOUNCEMENT`, `APPLICATION_PROGRESS`, `MATCHING_CASE`, `OPERATION_TASK`, `USER` |
| `ai_assist_request_status_code` | `REQUESTED`, `COMPLETED`, `FAILED` |
| `ai_assist_review_status_code` | `PENDING_REVIEW`, `ACCEPTED`, `DISCARDED` |

## 20. Audit API

운영 감사 로그는 기본적으로 내부 조회용이다.

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `GET` | `/api/v1/audit-logs` | `ADMIN`, `APPROVER` | 감사 로그 목록 |
| `GET` | `/api/v1/audit-logs/{auditLogId}` | `ADMIN`, `APPROVER` | 감사 로그 상세 |

감사 로그 응답은 개인정보 원문과 secret을 포함하지 않는다.

목록 query:

| 필드 | 설명 |
|---|---|
| `keyword` | 작업, 대상, 대상 번호, 작업자 검색 |
| `actionCode` | 작업 종류 정확히 일치 검색 |
| `resourceType` | `USER`, `PARTNER_VERIFICATION`, `MATCHING_CASE`, `APPLICATION_PROGRESS`, `DOCUMENT_SUBMISSION`, `CONSULTATION_RESERVATION`, `SUBSCRIPTION`, `PAYMENT_TRANSACTION`, `REFUND_TRANSACTION`, `NOTIFICATION_MESSAGE`, `OPERATION_TASK`, `REPORT_EXPORT` |
| `resultCode` | `SUCCESS`, `FAIL` |
| `page` | 1부터 시작 |
| `size` | 1~100 |

#### AuditLogSummaryResponse

```json
{
  "auditLogId": "uuid",
  "actorUserId": "uuid",
  "actorDisplayName": "관리자 (admin01)",
  "actionCode": "USER_ROLES_UPDATE",
  "actionLabel": "권한 변경",
  "resourceType": "USER",
  "resourceLabel": "회원",
  "resourceId": "uuid",
  "resultCode": "SUCCESS",
  "resultLabel": "성공",
  "createdAt": "2026-06-08T10:00:00+09:00"
}
```

#### AuditLogDetailsResponse

```json
{
  "auditLogId": "uuid",
  "actorUserId": "uuid",
  "actorDisplayName": "관리자 (admin01)",
  "actionCode": "USER_ROLES_UPDATE",
  "actionLabel": "권한 변경",
  "resourceType": "USER",
  "resourceLabel": "회원",
  "resourceId": "uuid",
  "resultCode": "SUCCESS",
  "resultLabel": "성공",
  "ipAddress": "127.0.0.1",
  "userAgent": "browser",
  "metadataJson": "{\"changedCount\":\"1\"}",
  "createdAt": "2026-06-08T10:00:00+09:00"
}
```

## 21. ErrorCode 초안

| errorCode | HTTP | 설명 |
|---|---:|---|
| `AUTH_REQUIRED` | 401 | 인증 필요 |
| `AUTH_INVALID_CREDENTIALS` | 401 | 로그인 실패 |
| `AUTH_FORBIDDEN` | 403 | 권한 없음 |
| `AUTH_PASSWORD_RESET_REQUIRED` | 403 | 비밀번호 변경 필요 |
| `CSRF_TOKEN_INVALID` | 403 | 브라우저 세션 API 요청의 CSRF header 불일치 |
| `VALIDATION_FAILED` | 400 | 요청 검증 실패 |
| `INVALID_PAGE_REQUEST` | 400 | paging 값 오류 |
| `INVALID_STATUS_TRANSITION` | 400 | 허용되지 않는 상태 변경 |
| `RESOURCE_NOT_FOUND` | 404 | 리소스 없음 |
| `DUPLICATE_LOGIN_ID` | 409 | loginId 중복 |
| `DUPLICATE_PHONE` | 409 | 휴대폰 번호 중복 |
| `DUPLICATE_BUSINESS_REGISTRATION_NO` | 409 | 사업자번호 중복 |
| `ANNOUNCEMENT_NOT_APPROVED` | 409 | 승인되지 않은 공고 |
| `VERIFICATION_NOT_VERIFIED` | 409 | 검증 ID가 포함된 매칭 요청에서 검증 미완료 |
| `MATCHING_BLOCKED` | 409 | 제한 플래그로 매칭 차단 |
| `PROGRESS_STEP_LOCKED` | 409 | 잠긴 단계 접근 |
| `PROGRESS_CONDITION_NOT_MET` | 409 | 단계 완료 조건 미충족 |
| `RATE_LIMIT_EXCEEDED` | 429 | 짧은 시간 내 과도한 API 요청 |
| `DB_CONSTRAINT_VIOLATION` | 409 | DB 제약 위반 |
| `INTERNAL_ERROR` | 500 | 서버 오류 |

## 22. Backend Gate

- 기존 endpoint는 `/api/v1/...`를 유지하고, 기존 의미를 깨는 신규 계약만 명시적으로 `/api/v2/...`를 사용한다.
- 모든 응답이 `ApiResponse`를 사용한다.
- 목록 응답은 `PageResponse`를 사용한다.
- Controller는 DTO 변환과 route 처리만 담당한다.
- ServiceImpl에서 transaction과 업무 규칙을 처리한다.
- DAO는 Mapper XML 호출만 수행한다.
- Mapper XML에는 명시 컬럼 목록을 작성한다.
- Mapper XML에는 `${}`를 사용하지 않는다.
- 권한은 서버에서 검증한다.
- 공고 승인 전 매칭 생성은 차단한다.
- 검증 ID가 포함된 매칭 요청은 검증 완료 전 생성이 차단된다.
- 매칭 응답에 추천도, 우선순위, 선정확률, 가점 값을 포함하지 않는다.
- AI 보조 응답은 운영자 검토용 초안이며 자동 승인, 자동 탈락, 추천 계산으로 사용하지 않는다.

## 23. 공고 수집·분류 V2 증분 계약

이 절은 2026-08-12 로컬 구현 계약이다. 운영 DB 반영, 초기 규칙 활성화, 기존 원문 재분류 실행 여부를 의미하지 않는다.

### 23.1 기존 V1 호환과 V2 다중 태그

- 기존 공고 상세·목록 응답에는 `targetCategoryCodes`, `supportTypeCodes`를 additive 필드로 제공한다.
- `POST|PUT /api/v2/announcements`는 `targetCategoryCodes[]`, `supportTypeCodes[]`를 직접 저장한다.
- `POST /api/v2/admin/announcement-sources/{sourceId}/announcements`는 확정된 다중 태그로 운영 공고 `DRAFT`만 생성한다. 외부 원문을 자동 활성화하지 않는다.
- 기존 V1 저장 의미는 유지하며 대표 지원대상 한 건과 추가 다중 태그를 호환 처리한다.
- 수집 원문 전환은 source 행을 잠근 뒤 기존 link를 먼저 확인한다. 이미 연결된 source의 동일 재요청은 기존 결과를 멱등 반환하고, 한 source를 다른 운영 공고로 중복 연결하는 요청은 쓰기 전에 409로 차단한다. 여러 source가 같은 운영 공고를 가리키는 것은 허용한다.

### 23.2 분류 조회·확정·재분류

| method | endpoint | 역할 | 설명 |
|---|---|---|---|
| `GET` | `/api/v1/admin/announcement-sources/{sourceId}/classification` | `ADMIN`, `OPERATOR`, `APPROVER` | 현재 판정, 근거, 자동·확정 다중 태그 조회 |
| `PUT` | `/api/v1/admin/announcement-sources/{sourceId}/confirmed-classification` | `ADMIN`, `OPERATOR` | 관리자 확정 태그 저장. 판정 ID와 `expectedVersion` 검증 |
| `POST` | `/api/v1/admin/announcement-sources/{sourceId}/reclassifications` | `ADMIN` | 지정한 `ACTIVE` release와 불변 원문 버전으로 단건 재분류 |

재분류는 새 evaluation을 append하고 기존 운영 공고의 승인·활성 상태를 자동 변경하지 않는다. 연결된 운영 공고가 있는 원문이 새 규칙에서 제외되면 운영 확인 task만 생성한다. `changeReason` 원문은 감사 metadata에 저장하지 않고 해시만 남긴다. V70에서 제목 자동 제외 원문이 비식별 삭제된 재분류 항목은 상태·판정 hash·tombstone 참조만 유지하며, 해당 항목이 포함된 실행의 원복 요청은 `409 ANNOUNCEMENT_SOURCE_NOT_CONVERTIBLE`로 거절한다.

기존 원문 일괄 재분류는 `/api/v1/admin/announcement-source-reclassification-runs`를 사용한다.

| method | endpoint | 역할 | 설명 |
|---|---|---|---|
| `POST` | `/previews` | `ADMIN` | ACTIVE release, provider, 수집일, 연결 공고 포함 여부, 최대 건수로 변경 없는 미리보기 생성 |
| `GET` | `` | `ADMIN`, `OPERATOR`, `APPROVER` | 최근 실행 20건과 상태·판정 분포·충돌·실패 집계 조회 |
| `GET` | `/{runId}` | 내부 3역할 | 실행 단건 진행 상태 조회 |
| `POST` | `/{runId}/application` | `ADMIN` | `PREVIEW_COMPLETED` 실행 적용 시작 |
| `POST` | `/{runId}/pause` | `ADMIN` | 적용 배치 일시중지 |
| `POST` | `/{runId}/resume` | `ADMIN` | 일시중지 실행 재개 |
| `POST` | `/{runId}/rollback` | `ADMIN` | 적용된 항목만 append-only 이력을 보존하며 원복 |

미리보기는 항목별 content version, 규칙 snapshot hash, 예상 판정 hash, 분류 version을 고정한다. 적용 시 하나라도 달라지면 해당 항목만 `APPLY_CONFLICT`로 남기며 다른 항목은 계속 처리한다. 적용 확인 문구는 `기존 원문 재분류 적용`, 원복 확인 문구는 `기존 원문 재분류 원복`이다. 원문 사유는 저장하지 않고 SHA-256만 실행 행에 보존한다.

수집 원문 목록은 기존 pagination을 유지하면서 `targetCategoryCode`, `supportTypeCode`, `matchedGroupCode`, `matchedGroupKindCode`, `matchLocationCode`, `ruleReleaseId` 필터와 다중 태그 배열을 additive로 제공한다.

### 23.3 규칙 release 관리

기준 prefix는 `/api/v1/admin/announcement-source-rule-releases`다.

| method | suffix | 설명 |
|---|---|---|
| `GET`, `POST` | `` | release 목록·초안 복제 생성 |
| `GET`, `POST` | `/{releaseId}/keyword-rules` | 규칙 목록·추가 |
| `PUT`, `DELETE` | `/{releaseId}/keyword-rules/{ruleId}` | 초안 규칙 수정·삭제 |
| `PATCH` | `/{releaseId}/keyword-rules/{ruleId}/status` | 사용·중지 상태 변경 |
| `POST` | `/{releaseId}/preview` | 제목·본문만 사용하는 판정 미리보기 |
| `POST` | `/{releaseId}/golden-set-runs` | 서버 QA-01~20 실행 및 `goldenSetRunId` 발급 |
| `POST` | `/{releaseId}/publication` | 같은 초안/version의 서버 QA ID 확인 후 단일 transaction 게시 |

`ACTIVE` release는 직접 수정할 수 없다. 게시 시 기존 `ACTIVE`는 `RETIRED`, 대상 `DRAFT`는 `ACTIVE`가 되며, `row_version` 충돌은 409다. 초기 V65 release는 `DRAFT`이므로 migration 적용만으로 수집 결과에 영향을 주지 않는다.

### 23.4 수집 실행 안전 계약

- `saneb.announcement-source.classification-v2.enabled` 기본값은 `false`다.
- `saneb.announcement-source.classification-v2.provider-codes`로 신규 수집 분류 canary 채널을 제한한다. 전역 flag가 `true`여도 allowlist 밖 provider는 V1 수집 경로를 유지한다.
- 외부 검색 지원 provider는 고정된 release의 지원대상×지원형태 검색계획을 사용하고, 지자체는 발견 후 같은 공통 엔진으로 판정한다.
- 제목의 그룹 B는 자동 제외, 제목의 그룹 A는 관리자 검수, 본문의 그룹 B는 관리자 검수로 처리한다.
- 상세본문 기능은 별도 flag 기본 `false`이며 공식 등록 host 일치, URL·DNS 검증, redirect·시간·크기·동시성 제한을 통과한 HTML만 입력으로 사용한다.
- 2026-09-14 로컬 본문 정제: 태백25·횡성65·영월17의 정확한 공식 BBS 상세 경로는 실측한 단일 게시판 표·제목 표식·내용 셀을 요구한다. 누락/중복은 내부 `BODY_SELECTOR_CHANGED`, 빈 내용은 `BODY_TEXT_EMPTY`로 실패하며, 기존 `BodyAvailabilityCode.FETCH_FAILED` 계약으로 전달한다. 페이지 전체나 첨부 파일명을 본문으로 대체하지 않는다. 다른 게시판의 기존 정제와 v1 응답·DB enum은 변경하지 않으며 본문 실패 뒤의 첨부 분석·최종 검증 흐름을 유지한다.
- 신규 수집은 provider 응답의 첨부 URL·파일명 필드를 원문 저장 전에 제거하고 attachment 행을 생성하지 않는다. 지자체 상세 HTML에서도 첨부 링크와 표시명을 본문 추출 전에 제거한다.
- 기존 TITLE/BODY 분류 경로는 PDF·HWP 등을 다운로드·추출·분류하지 않는다. 이후 승인된 첨부 처리는 별도 V72/V73 작업자·근거·V2 계약을 사용하며, V1 첨부 이력은 호환 조회만 유지한다.
- 관리자 검수·유효 후보는 원문 버전과 일치 규칙 근거를 저장한다. 제목 자동 제외는 원문 없이 SHA-256 identity, 사유·단계, rule·term FK와 run 건수만 저장한다.
- `data_purpose_code`가 명시적으로 `QA`인 원문·요청만 QA 정리 대상이다. 일반 애플리케이션 수집은 `PRODUCTION`만 생성하며, DB에 쓰는 격리 QA 경로는 운영 QA 승인 시 별도 확정한다.
- 상세본문 기능을 운영에서 켜기 전 DNS 재바인딩을 포함한 private-range egress 차단 또는 연결 IP 고정 검증을 완료한다.

## 24. 첨부 근거 V2 조회

2026-09-16 검증 구분: 아래는 구현된 확장 계약이며 최초2026-09-10 설명 이후 정책 게시·배치·Provider QA API가 추가됐다. `85c7f65` Linux35050057694의 HTTP 단위 계약, 실제 임시 PostgreSQL job192건·migration17건·worker12건과 운영 업무 E2E를 구분한다. 후속 `9fe892c` Linux35051444985에서도 전체 계약 및 제천 실파일3개→worker/임시 DB/API를 검증했으며 UNKNOWN 검수 상태를 유지했다. [계약 검증 근거](announcement-attachment-contract-evidence-2026-09-16.md)를 따르며, API 구현이나 시험 fixture의 정책 활성화를 실제 운영 게시·ENFORCE로 표현하지 않는다.

기존 TITLE/BODY 및 `/api/v1` 응답에 첨부를 혼합하지 않는다. 아래 별도 조회는 `ADMIN`, `OPERATOR`, `APPROVER`만 허용하며 `USER`, `PARTNER`, `REVIEWER`는 거부한다. 운영 배포 완료 기록이 아니다.

prefix: `/api/v2/admin/announcement-sources/{sourceId}`

| method | suffix | 응답 |
|---|---|---|
| GET | `/attachment-sets` | 발견/봉인 상태, `warningCodes` 고정 경고 배열, 건수, hash, 생성·발견·봉인 시각 |
| GET | `/attachment-sets/{setId}/files` | 파일 역할, 다운로드 결과, 최신 추출 ID/품질/글자 수/오류 |
| GET | `/attachment-extractions/{extractionId}/blocks` | code point 위치·locator·범위 신뢰도와 제한된 본문 구간 |

- 모든 목록은 `ApiResponse<PageResponse<...>>`다. `page` 기본 1, 범위 1~1000000. 집합/파일 `size` 기본 20, 최대 100. block `size` 기본 10, 최대 20.
- block 추가 파라미터: `textOffset`(각 block 내부 code point 위치, 기본 0, 최대 1000000), `textLimit`(기본 2000, 최대 4000). 큰 문단은 block 페이지를 고정하고 `textOffset`을 이동해 이어 읽는다. `startOffset/endOffset`은 전체 block 경계, `textStartOffset/textEndOffset`은 이번 응답 구간, `hasMoreText`는 남은 구간 유무다.
- 집합·파일·block 응답은 모두 `Cache-Control: no-store`다. 전체 원본 binary 다운로드 API가 아니며 fetch URL, 로컬 경로, lease token, safe locator JSON을 반환하지 않는다.
- 발견 `warningCodes`는 원문 없는 고정 코드다. `ATTACHMENT_DETAIL_UNAVAILABLE`(상세 확인 실패), `ATTACHMENT_SELECTOR_CHANGED`(첨부 영역 변경), `ATTACHMENT_DOWNLOAD_FORM_CHANGED`(다운로드 폼 변경), `ATTACHMENT_LINK_UNRESOLVED`(링크 확인 실패), `ATTACHMENT_FILE_LIMIT`(파일 수 한도)와 정의된 전송 실패 코드를 구분한다. 폼 변경은 FAILED/발견 미완료로 저장하며 NO_FILES로 표시하지 않는다. 폼 원문 인자는 반환하지 않는다.
- 제목 통과 기본 판정이 있는 PRODUCTION 원문만 조회한다. 원문 행이 잔존하더라도 제목 제외·QA·기본/제목 판정 미완료이면 집합·파일·텍스트를 반환하지 않는다. 다른 source에 속한 set/extraction ID는 404 wrapper다. 잘못된 페이지·본문 한도는 구체적인 한국어 400 wrapper를 반환한다.
- 신규 첨부 변경 namespace(`attachment-*`, 첨부 policies/batches)는 session CSRF 검증을 요구한다. 기존 V1 및 기존 V2 변경 경로의 CSRF 계약은 이번 단계에서 바꾸지 않았다. 누락/만료 시 403과 새로고침 안내를 반환한다.
- 첨부 필수 source는 기존 V1/V2 전환·기본 분류 확정·재분류·롤백으로 우회할 수 없다. `ANNOUNCEMENT_SOURCE_NOT_CONVERTIBLE` 409를 반환한다. 기존 link가 이미 있는 전환 재요청은 쓰기 없이 기존 link를 반환한다.
- 새 수집의 내부 자동 예약, 종합 조회·확인·DRAFT 전환, 역할 변경/작업 조회, 실패 파일 재시도 및24.7의 초기/전체 수동 수집 API를 구현했다. 정책 게시·배치·전체 분할·Provider QA 관리 API와 관리자 화면도 후속 절에 구현 계약이 있다. 남은 것은 전체 공식 표본/정책 QA, 승인된 운영 적용, 최신 버전 관리자 업무 E2E이며 CSRF 경로 보호나 로컬 시험만으로 운영 완료를 주장하지 않는다.

### 24.1 현재 분류 목록·상세

동일한 READ 역할, `ApiResponse`/목록 `PageResponse`, `Cache-Control: no-store`를 사용한다.

| method | endpoint | 응답 |
|---|---|---|
| GET | `/api/v2/admin/announcement-sources` | source 요약 및 base/effective/preview, 첨부 진행 상태·버전·확인 상태 |
| GET | `/api/v2/admin/announcement-sources/{sourceId}` | `source`(목록과 같은 요약), `content`(본문·문의·신청 방식·공개 원문 URL·완전성) |
| GET | `/api/v2/admin/announcement-sources/{sourceId}/attachment-classification` | 본문 없는 현재 요약; pending은 판정 ID가 없는 진행 projection |

- 목록 필터: `providerCode`, `effectiveStatusCode`(ACCEPTED/REVIEW_REQUIRED), `jobStatusCode`, `targetCategoryCode`, `supportTypeCode`, `keyword`(100자 이하 제목/기관명 리터럴 부분 검색), `collectedFrom`, `collectedTo`(서울 날짜, 양끝 포함), `page`(1~1000000), `size`(기본 20/최대 100).
- 목록과 count는 같은 SQL 범위/상태/태그 식을 사용하고 하나의 읽기 전용 REPEATABLE_READ snapshot에서 읽는다. 상세도 같은 판정 join을 사용한다.
- PRODUCTION만 반환한다. TITLE 제외·기본 제외 원문은 목록에 포함하지 않고 단건은 404다. 기존 V1 계약이나 tombstone을 원문으로 복원하지 않는다.
- 미적용 source의 `effectiveClassification`은 base이며, 첨부 결과는 `previewClassification`으로만 제공한다. ENFORCE source는 유효한 현재 첨부 판정 또는 `REVIEW_REQUIRED/ATTACHMENT_PENDING` 등 진행 사유를 사용한다. 모드 OFF만으로 base에 복귀하지 않는다.
- 새 세대 예약 시 이전 첨부 current pointer/confirmation을 같은 transaction에서 해제한다. 진행 중 effective `decisionId`, `setId`, `setHash`, `inputHash`는 null이며 이전 첨부 태그를 승계하지 않는다. 과거 근거는 별도 이력 조회로 보존한다.
- 현재 판정은 source/base/content/release, 정책 binding, SEALED set을 검증한다. 현재 confirmation이 있으면 그 확인의 태그, 없으면 해당 판정의 AUTO 태그를 사용한다. 목록 태그 필터에도 같은 기준을 사용한다.
- `attachmentSummary`: `jobId`, `jobStatusCode`, 고정 `errorCode`, `intakeStatusCode`, `isStale`, `discoveryStatusCode`, `isDiscoveryComplete`, `totalCount`, `processedCount`. 미발견 상태의 건수는 null이다. `processedCount`는 성공 파일 수가 아니며 실패 근거를 포함한다.
- `intakeStatusCode=RECHECK_NOT_DUE`는 24시간 자동 재확인 간격이 지나지 않았다는 예약 결과다. 기존 유효한 판정/확인/버전은 유지한다. `jobStatusCode` 및 발견 완료 상태와 동일한 의미가 아니다. 최근 일반 작업은 source의 단조 증가 `expected_attachment_version` 순으로 선택하며 batch preview로 대체하지 않는다.
- `confirmationStatusCode`는 `NONE/CURRENT/STALE`다. 자동 ACCEPTED나 SUCCEEDED는 관리자 검수 확정·자격 확정·운영 활성화를 뜻하지 않는다.
- 실행 snapshot/lease/다운로드 locator/원본 payload를 반환하지 않는다. API·서비스 단위 검증은 통과했으나 새 목록 SQL의 실제 PostgreSQL 회귀는 로컬 Code Integrity 차단으로 Linux 환경에서 추가 검증해야 한다.

### 24.2 첨부 검수 확인과 DRAFT 전환

2026-09-10 로컬 추가. 기본 경로는 `/api/v2/admin/announcement-sources/{sourceId}/attachment-classification`이다. 기존 V1/V2 전환 guard를 제거하지 않는다.

| method | suffix | 역할 / 계약 |
|---|---|---|
| GET | `/review-context` | ADMIN/OPERATOR/APPROVER. 현재 버전, 판정 상태·사유, `manualSourceCheckRequired`, `requiredAcknowledgementCodes` |
| POST | `/confirmations` | ADMIN/OPERATOR. CSRF와 UUID `Idempotency-Key` 필수. 특정 현재 판정의 관리자 확인과 확정 태그 저장 |
| POST | `/announcements` | ADMIN/OPERATOR. CSRF 필수. 현재 확인의 저장된 태그로 DRAFT만 생성 |

모든 응답은 `ApiResponse` 및 `Cache-Control: no-store`를 사용한다. service도 활성 운영 계정·권한·비밀번호 변경 완료를 확인한다. 읽기 전용 APPROVER는 두 POST를 호출할 수 없다.

2026-09-11 additive 조회 필드:

- `confirmedClassification`: 현재 source/evaluation/set hash 및 유효 sourceVersion/attachmentVersion에 일치하는 확인이 있고 공고에 아직 연결되지 않았을 때만 제공한다. `confirmation`은 원래 검수 시점·버전을 보존하는 기존 확인 응답이고 `targetCategoryCodes`/`supportTypeCodes`는 해당 확인에 저장된 정규화 분류다. 불일치/확인 없음/이미 연결이면 null이다. 브라우저 새로고침 후 자동 후보 태그를 수동 확정값으로 오인하지 않도록 분리한다.
- `confirmedClassification.binding`: `{restorationId, confirmationId, sourceId, sourceVersion, attachmentVersion}`. 최초 확인이면 `restorationId=null`이고 버전은 원래 confirmation과 같다. 완료된 원복 근거가 있으면 해당 복구 ID와 증가한 유효 첨부 버전을 사용한다. 원래 confirmation ID·confirmedAt·버전·태그를 덮어쓰지 않는다. 검수 화면과 DRAFT 전환은 이 유효 버전 및 현재 판정/set hash를 함께 검증한다. 복구 근거가 다른 원문/확인, 잘못된 버전 또는 미완료 상태이면 허용하지 않는다. 이 additive 조회 필드가 원복 실행 API의 구현 완료를 의미하지 않는다.
- `linkedAnnouncement`: 기존 공고 연결의 `announcementId`, `announcementCode`; 연결이 없으면 null. 현재 승인·활성 상태를 뜻하지 않는다. 확인 메모·actor·멱등키·request hash를 반환하지 않는다.
- 읽기는 기존 REPEATABLE_READ 조회 트랜잭션 안에서 수행하며 확인/태그/원문 버전/공고를 수정하지 않는다.
- 전용 화면 `/app/admin/collected-announcements/{sourceId}/attachments`는 동일 조회 역할과 no-store를 적용한다. 화면은 요약과 review-context의 버전 일치 후에만 쓰기를 열며 서버 검증을 대체하지 않는다.
- 이 화면의 별도 첨부 복구 영역은 아래 24장의 기존 전체 수집·실패 파일 재시도·역할 변경·작업 상태 API를 사용한다. 복구용 현재 파일 집합과 과거 근거 탐색은 분리하며, 작업 접수와 처리 성공은 구분한다. 정책 게시/ENFORCE/기존 데이터 일괄 적용을 이 단건 화면에서 실행하지 않는다.

검수·전환 요청의 `version` 객체는 다음 필드를 모두 포함한다. ID는 UUID, 버전은 0 이상 정수, set hash는 SHA-256 소문자 64자리다.

```text
version.expectedBaseDecisionId
version.expectedAttachmentDecisionId
version.expectedSourceVersion
version.expectedAttachmentVersion
version.expectedSetHash
```

확인 요청의 나머지 필드:

- `targetCategoryCodes`: 중복 없는 1~5개 지원대상. BUSINESS/PERSONAL/SPOUSE/CHILD/PARENT. PERSONAL의 화면명은 본인(개인)이다.
- `supportTypeCodes`: 중복 없는 1~7개 지원형태. GENERAL_SUPPORT/GRANT_SUBSIDY/POLICY_FINANCE/GUARANTEE/INTEREST_SUPPORT/VOUCHER_BENEFIT/REFUND_REDUCTION.
- `reviewMethodCode`: EXTRACTED_TEXT 또는 MANUAL_SOURCE_CHECK.
- `acknowledgedErrorCodes`: review-context가 반환한 현재 필수 사유의 정확한 집합. 누락·추가·중복을 허용하지 않는다. 최대 100개, 코드별 80자.
- `reviewNote`: 직접 확인한 내용과 사유 1~1000자. 확인 테이블에만 저장한다. 응답/감사에는 메모 원문을 반환하지 않고 감사에는 hash를 기록한다.

확인은 PRODUCTION·제목 통과·첨부 검수 binding·현재 SEALED 판정에 한정된다. COLLECT_ONLY preview를 확인 API로 ENFORCE 전환할 수 없다. 현재 일반 작업이 진행 중이면 확인할 수 없다. batch preview는 기존 확인을 차단하는 일반 작업으로 세지 않는다. 다른 source의 판정·확인 식별자는 404, 오래된 버전·set hash·연결 변경은 409다.

발견 실패, 다운로드 실패, COMPLETE_TEXT 아닌 품질, 미확인 역할, 불확실한 문맥 또는 경고가 있으면 전체 원문을 직접 확인하는 MANUAL_SOURCE_CHECK와 모든 해당 사유 확인을 요구한다. 첫 정상 첨부로 뒤의 실패를 숨기지 않는다. A/B 검수 사유는 자동 판정을 ACCEPTED로 바꾸지 않고 별도 확인으로 처리한다. OFF/퇴역 정책만으로 이미 적용된 확인 의무가 없어지거나 현재 근거의 수동 검수가 차단되지는 않는다.

구간 엔진(`attachment-segment-1.0.0`)에서는 현재 evaluation input에 결합되고 정책 버전·hash 및 전체 원문/block 재현 검증을 통과한 RESOLVED 구간을 사용한다. 파일 전체 자동 역할이 UNKNOWN이어도 구간이 모두 확정되었다면 그 이유만으로 MANUAL_SOURCE_CHECK를 중복 요구하지 않는다. 수동/프로필 고정 역할, UNKNOWN 구간, 부분·실패 파일, A/B·부정 문맥·경고는 기존대로 별도 확인한다. 미결합 SHADOW 분석·다른 추출·변조/누락 근거는 요구 해제에 사용할 수 없으며 409로 최신 처리를 안내한다. 최종 confirmation은 여전히 필수이고 DRAFT 외 자동 활성화는 없다. 기존 파일 엔진과 `/api/v1`은 변경하지 않는다.

확인 응답은 source/confirmation/evaluation ID, set hash, 확인 직후 `sourceVersion`/`attachmentVersion`, 방법, `isCurrent`, 시각이다. 확인은 첨부 버전을 1 증가시키며 이전 확인을 STALE로 남긴다. 기본 판정과 첨부 자동 판정·실패 상태는 변경하지 않는다. 같은 Idempotency-Key와 동일한 정규화 요청/actor는 최초 확인을 반환하며 새 확인이나 태그를 만들지 않는다. 다른 원문·actor·요청으로 재사용하면 409다. 이후 STALE가 된 확인 재조회는 `isCurrent=false`로 반환될 수 있다.

전환 요청은 `version`, `expectedConfirmationId`, 현재 확정 대상에 포함된 `primaryTargetCategoryCode`, 선택 `incomeJudgementCode`다. 소득 판단 방식 생략 시 기존 V2와 같은 VAT_TAX_BASE_ONLY를 사용하며 이 값은 자격 판정 완료를 뜻하지 않는다. 확인 이후 최신 버전은 review-context에서 다시 읽는다.

- 확인 ID·판정·set hash·현재 유효 확인 버전·활성 카탈로그를 재검증한다. 완료된 원복 이후에도 과거 confirmation의 원래 버전으로 요청할 수 없으며 최신 review-context.version을 사용해야 한다. 요청 배열 대신 DB의 해당 CONFIRMED 태그를 복사한다.
- 미검수 중복/유사 후보가 있으면 409다. 기존 공고에 연결된 source의 새 확인·다른 전환 요청도 409로 보호한다.
- source 잠금→현재 조건 검증→DRAFT/다중 배정→source UNIQUE link→버전 증가/감사 전체가 하나의 transaction이다. 생성 직후 승인 상태가 DRAFT가 아니면 rollback한다. 승인 요청·활성화는 하지 않는다.
- 같은 source의 최초 전환과 같은 확인/정규화 요청 hash는 기존 link를 반환한다. 다른 요청이나 첨부 전환 메타데이터가 없는 legacy link를 새 전환 성공으로 반환하지 않는다. 최초 요청 hash에는 actor를 포함하지 않아 다른 허용 운영자가 동일 전환을 재시도해도 중복 공고를 만들지 않는다.
- 연결 후 확인 자체는 같은 근거의 CURRENT로 유지하되, 새 확인으로 기존 DRAFT 태그를 덮어쓰지 않는다. 내용·역할·판정 변경 흐름은 기존 확인을 STALE로 처리해야 한다.

오류 코드: `ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT`(409), `ANNOUNCEMENT_ATTACHMENT_NOT_READY`(409), `ANNOUNCEMENT_ATTACHMENT_REVIEW_REQUIRED`(409), `ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN`(403), `RESOURCE_NOT_FOUND`(404), `VALIDATION_FAILED`(400). 중복 후보는 기존 `ANNOUNCEMENT_SOURCE_NOT_CONVERTIBLE`(409)을 사용한다. UI는 409에서 입력을 보존하고 원인/다시 확인할 버전을 안내해야 한다. 해당 UI와 최신 PostgreSQL 실행·운영 E2E는 아직 완료되지 않았다.

### 24.3 첨부 역할 변경과 비동기 작업 조회

2026-09-10 로컬 구현. prefix는 `/api/v2/admin/announcement-sources/{sourceId}`다.

| method | suffix | 역할 / 계약 |
|---|---|---|
| PUT | `/attachment-roles` | ADMIN/OPERATOR, CSRF·UUID Idempotency-Key 필수. 새 SEALED 근거와 재평가 작업을 생성하고 202 반환 |
| GET | `/attachment-jobs/{jobId}` | ADMIN/OPERATOR/APPROVER. 해당 원문에 속한 작업 상태 조회 |

- 요청: 24.2와 같은 `version`, `expectedSetId`, `fileRoles`(현재 첨부 전체 1~10개를 중복 없이 `{fileId, documentRoleCode}`로 제출), `reason`(1~1000자). 역할은 NOTICE/GUIDE/FORM/REFERENCE/UNKNOWN이며, 하나 이상 실제로 변경해야 한다. 일부 파일 생략·잘못된 역할·동일 역할만 제출하면 400이다. 다른 source의 파일/set/job은 404다.
- 원문→작업 잠금 아래 현재 base/content/첨부 판정·버전·set hash·고정 정책/엔진/추출 버전을 확인한다. 아직 일반 작업이 진행 중이거나 미봉인 상태이면 409다. 이미 운영 공고에 연결된 source는 보호하여 변경하지 않는다.
- 새 set/file/extraction ID로 복사하며 원래 역할/성공·실패/텍스트 근거를 수정하지 않는다. 바뀐 역할만 MANUAL로 기록한다. 실제 추출 시각과 결과는 보존하고 파일 조회에 `reusedFromExtractionId`를 추가하여 원래 extraction을 식별한다.
- `operationCode=ROLE_CHANGE`의 작업은 기존 고정 입력으로 비동기 평가만 실행하며 다운로드/추출을 하지 않는다. 예약 시 current 판정과 확인을 STALE로 만들고 첨부 버전을 1 증가시킨다. 재평가 완료 후 최신 분류와 review-context를 다시 읽어야 한다.
- OFF 모드는 새 HTTP를 중지한다. 기존 봉인 근거의 역할 재평가까지 막지 않으며 검수 의무를 해제하지 않는다. COLLECT_ONLY의 역할 변경은 preview만 갱신하고 ENFORCE를 암묵적으로 활성화하지 않는다.
- 같은 key·actor·정규화 요청은 최초 작업을 반환한다. 다른 actor/원문/입력으로 key를 재사용하면 409다. 잘못된 역할 입력은 `ANNOUNCEMENT_ATTACHMENT_ROLE_INVALID`(400), 나머지 버전·권한 오류는 24.2와 같은 wrapper다.
- 응답 `ApiResponse<AttachmentJobResponse>`: `sourceId`, `jobId`, `setId`, `operationCode`, `jobStatusCode`, `sourceVersionAtReservation`, `attachmentVersionAtReservation`, `generation`, `errorCode`. 예약 버전은 작업 완료 이후의 현재 버전이 아니다. lease token·execution snapshot·내부 request hash를 반환하지 않으며 `Cache-Control: no-store`다.
- 이 API의 로컬 서비스·HTTP 검증과 실제 PostgreSQL·운영 브라우저 검증은 별도다. 수동 실패 재시도는 24.5를 따르며 관리자 화면은 아직 구현하지 않았다.

### 24.4 내부 자동 재시도의 성공 파일 중간 저장

- 같은 COLLECT 또는 RETRY_FILES job의 일시 네트워크 재시도에서 완전 추출 성공 파일을 중간 저장해 재사용한다. 발견 HTML은 다시 확인하여 안정 locator에 맞는 새 요청을 만들지만, 성공 파일의 binary/추출은 반복하지 않는다. 실패/부분 추출/OCR_REQUIRED를 완료 checkpoint로 승격하지 않는다. RETRY_FILES는 최초 선택 파일과 고정 역할에 해당하는 근거만 중간 저장한다.
- checkpoint는 사용자 조회 API나 최종 set/현재 판정에 포함하지 않는다. 모든 첨부의 최종 결과를 봉인한 후에만 기존 API로 제공한다. 새 job/generation은 같은 URL이어도 binary를 다시 확인한다.
- 중간 저장 원문은 내부 접근 제한 DB 테이블에만 두며 원본 binary는 저장 전에 삭제한다. 최초 추출 완료 시각을 최종 근거에 보존한다. 봉인/종료/충돌/취소 또는 source 삭제 시 임시 중복 텍스트를 정리한다.
- 이는 작업 내부의 자동 재시도 보강이다. 아래 수동 재시도와 구분하며, 관리자 화면의 완료 증거는 아니다.

### 24.5 봉인된 부분 실패 집합의 수동 파일 재시도

2026-09-10 로컬 구현. `POST /api/v2/admin/announcement-sources/{sourceId}/attachment-jobs`는 **실패 파일 선택 재시도만** 받는다. 초기 첨부 발견/전체 재수집은 기존 요청 의미를 보존하기 위해 24.7의 별도 `/attachment-jobs/collection`으로 요청한다.

- ADMIN/OPERATOR, CSRF, UUID `Idempotency-Key`를 요구한다. 성공은 202 `ApiResponse<AttachmentJobResponse>`, `Cache-Control: no-store`다. 작업 조회는 24.3의 GET을 사용한다.
- 요청: 24.2와 같은 `version`, `expectedSetId`, 중복 없는 `fileIds` 1~10개, `maximumDownloadBytes` 1~83,886,080 byte, `reason` 1~1000자. 요청에는 URL·provider/profile 선택·추출기 실행 옵션이 없다. reason 원문은 응답·감사에 노출하지 않고 감사에는 hash를 기록한다.
- 대상은 제목을 통과한 PRODUCTION 원문의 현재 SEALED·FOUND·발견 완료 집합이다. 전체 발견/처리/저장 파일 건수가 일치해야 한다. 발견 실패나 빈 집합은 이 경로로 성공 처리하지 않으며 전체 재수집이 필요하다.
- 다운로드 FAILED/CANCELLED 또는 다운로드 SUCCEEDED이면서 PARTIAL_TEXT/CORRUPT/LIMIT_EXCEEDED/TIMEOUT/FAILED/ISOLATION_UNAVAILABLE인 파일만 선택할 수 있다. COMPLETE_TEXT·BLOCKED·OCR_REQUIRED·ENCRYPTED·UNSUPPORTED는 자동 재요청 대상이 아니며 해당 수동 확인 안내를 따른다. 형식/권한/정책 변경을 재시도로 우회하지 않는다.
- 현재 기본/첨부 판정·source/attachment 버전·set hash·source/content/policy 연결을 확인한다. 동일 ACTIVE 기본 규칙의 게시 ACTIVE 첨부 정책과 설치된 profile/엔진/추출기 hash가 필요하다. OFF/퇴역 정책 또는 설정 불일치는 새 네트워크 예약을 차단한다.
- 연결된 운영 공고 및 실행 중 일반 작업은 보호한다. 다른 source의 파일/집합은 404, 오래된 버전·연결·정책은 409다. 409 응답은 선택 입력을 유지하고 최신 근거를 다시 확인하도록 안내한다.
- 새 `RETRY_FILES` generation과 불변 선택 범위를 같은 transaction에서 예약한다. 예약 즉시 첨부 버전 +1, 기존 첨부 현재 판정/확인 STALE를 적용한다. 동일 key·actor·정규화 요청은 최초 작업을 반환하고 횟수 제한을 다시 소비하지 않는다. 다른 요청에 같은 key를 쓰면 409다.
- 초기 구현 제한은 전체 수동 수집과 실패 파일 재시도를 합산하여 원문별 60초 간격, 최근 24시간 최대 3회다. 이는 초기 안전 상한이며 고객이 확정한 수치로 표현하지 않는다. 자동 수집·무HTTP 역할 변경은 이 수동 요청 횟수에 합산하지 않는다. 초과 시 `ANNOUNCEMENT_ATTACHMENT_RETRY_RATE_LIMITED`(429)와 제한 조건을 반환한다.
- worker는 상세를 다시 확인하되 원래 전체 안정 locator 집합이 동일할 때만 선택 파일을 다운로드한다. 발견 실패/집합 변경 시 새 파일로 범위를 넓히지 않고 binary 요청 없이 선택 실패 근거를 보존한다. `DISCOVERY_FAILED`/`DISCOVERY_CHANGED` 및 발견 미완료 상태로 검수 사유를 남긴다.
- 새 set에는 선택 파일의 새 결과와 선택하지 않은 **모든** 원래 파일의 성공/실패 근거를 함께 넣는다. 수동 역할과 역할 출처, 기존 추출의 실제 시각·provenance를 보존하고 과거 set은 수정하지 않는다. 실패 파일 일부가 성공해도 다른 실패를 숨기지 않는다.
- 선택 파일에서 이번 job이 새로 받은 bytes만 누적 예산에 합산한다. 보존 근거의 과거 bytes는 재요청으로 세지 않는다. 감사의 요청량 상한은 총 3시도 × (상세 1 + 선택 파일 수) × GET 최대 4 hop이며 POST redirect는 허용하지 않는다. 중간 성공 재사용으로 실제 요청량은 이보다 적을 수 있다.
- 기존 첨부 검수 binding만 유지하고 COLLECT_ONLY를 암묵적으로 ENFORCE로 바꾸지 않는다. base 판정·연결된 공고·활성 상태는 변경하지 않는다. 재시도 완료 뒤 최신 판정과 검수 기준을 다시 조회해야 한다.
- 입력 오류는 `ANNOUNCEMENT_ATTACHMENT_RETRY_INVALID`(400) 또는 `VALIDATION_FAILED`(400), 나머지 권한/404/409는 기존 wrapper를 유지한다. 최신 PostgreSQL 무결성·동시성 및 운영·브라우저 검증은 별도 미완료 Gate다.

### 24.6 종합 판정 이력·당시 입력·키워드 근거

2026-09-10 로컬 구현. prefix는 `/api/v2/admin/announcement-sources/{sourceId}/attachment-classification`이며 READ는 ADMIN/OPERATOR/APPROVER다. 모든 응답은 `ApiResponse` wrapper와 `Cache-Control: no-store`를 유지한다.

| method | suffix | 계약 |
|---|---|---|
| GET | `/history` | 불변 판정 이력 페이지. evaluatedAt 내림차순, 동일 시각은 evaluation ID 내림차순 |
| GET | `/{evaluationId}` | 당시 종합 판정 요약·자동 다중 태그·입력/일치 건수·현재 원문/첨부 버전 |
| GET | `/{evaluationId}/inputs` | 당시 평가에 사용한 모든 파일의 ID·역할·입력/다운로드/추출 상태·오류·실제 추출 시각·재사용 참조 |
| GET | `/{evaluationId}/matches` | 당시 release/group/rule/term과 적용 action, file/extraction ID, block/offset. 선택 `fileId` 필터 |

- 목록은 `PageResponse`, `page` 기본 1/1~1000000, `size` 기본 20/1~100이다. 목록/count는 동일한 source/evaluation/file 범위와 REPEATABLE_READ snapshot을 사용한다.
- 요약은 evaluation/source/base/set/policy/ruleRelease ID, 엔진 버전, input/decision hash, 자동 semanticStatus/reason/warnings, 평가 시각과 `usageCode`를 반환한다.
- `CURRENT_EFFECTIVE`는 현재 projection이 같은 첨부 판정을 가리키고 검수 binding이 있는 경우다. `CURRENT_PREVIEW`는 같은 현재 포인터여도 COLLECT_ONLY/미적용 source의 미리보기다. `NOT_CURRENT`는 과거 판정 또는 아직 현재로 적용되지 않은 배치 미리보기 등을 포함한다. 단순 DB `is_current`만으로 effective 판정으로 올리지 않는다.
- 상세의 `autoTargetCategoryCodes`/`autoSupportTypeCodes`는 **당시 AUTO 태그만** 반환한다. CONFIRMED 태그는 기존 현재 조회·검수 기준 API에서 확인한다. 현재 카탈로그 비활성이나 규칙 퇴역을 이유로 과거 근거를 숨기지 않는다.
- 상세의 `currentSourceVersion`/`currentAttachmentVersion`은 조회 시점의 값이다. 과거 평가의 확인/수정 허가를 뜻하지 않으며 쓰기에는 최신 review-context의 전체 기대 버전이 필요하다.
- 입력 목록은 평가 입력의 exact extraction ID를 사용한다. 최신 attempt나 새 set의 추출 결과로 바꾸지 않으며 extraction 없는 다운로드 실패도 목록/count에서 빠뜨리지 않는다.
- matches는 keyword term과 근거 위치만 반환하며 전체 추출 텍스트·원문 URL·binary·lease·검수 메모를 조회/반환하지 않는다. 원문 구간은 해당 extraction의 제한된 block API로 별도 조회한다. `startOffset/endOffset`은 전체 추출문 기준 code point의 반개구간이며 `blockIndex`는 저장된 block의 index다. 화면에서는 문자열을 escape해서 표시해야 하며 현재 브라우저 검증 완료를 뜻하지 않는다.
- `fileId`는 같은 원문이더라도 해당 evaluation의 입력 파일이어야 한다. 잘못된 source/evaluation/file 연결은 404이며 필터 결과 0건 성공으로 숨기지 않는다. 해당 입력에 실제 일치가 없는 경우만 빈 페이지다.
- 제목 제외·QA·현재 제목 판정이 없는 원문은 과거 판정이 남아 있어도 이 API로 조회할 수 없다. 조회는 source/current/confirmation/정책을 수정하거나 네트워크 다운로드를 실행하지 않는다. pending이면 과거 이력은 읽을 수 있어도 현재 사용 상태로 표시하지 않는다.
- 로컬 서비스/HTTP/XML binding 검증과 실제 PostgreSQL·운영 화면 검증은 구분한다. 신규 PG 테스트는 환경 차단 때문에 아직 실행 통과로 계산하지 않는다.

### 24.7 초기·전체 첨부 수집 조건 조회와 예약

2026-09-10 로컬 구현. 기존 실패 파일 선택 재시도의 요청 구조를 보존하기 위해 별도 collection action을 추가한다. 상세 설계 9.2의 제안된 단건 수집 기능을 같은 job 리소스의 하위 경로로 구체화한 것이며 기존 `/api/v1` 의미를 변경하지 않는다.

prefix: `/api/v2/admin/announcement-sources/{sourceId}`

| method | suffix | 역할 / 계약 |
|---|---|---|
| GET | `/attachment-collection-context` | ADMIN/OPERATOR/APPROVER. 현재 수집 조건과 영향/상한 조회. 작업 생성·첨부 발견·다운로드 없음 |
| POST | `/attachment-jobs/collection` | ADMIN/OPERATOR. CSRF·UUID Idempotency-Key 필수. 새 전체 COLLECT job 예약, 202 |

모든 응답은 `ApiResponse`와 `Cache-Control: no-store`다. 예약 후에는 기존 GET `/attachment-jobs/{jobId}`로 처리 상태를 조회한다. 202는 처리 완료/다운로드 성공이 아니다.

조건 조회 응답:

- `version`: `expectedBaseDecisionId`, nullable `expectedAttachmentDecisionId`, `expectedSourceVersion`, `expectedAttachmentVersion`. 초기 수집·실패 후 현재 첨부 판정이 없으면 null을 그대로 반환하며 예약에서도 null을 조회 당시 값으로 비교한다.
- `policyId`, `policyHash`, `executionHash`, `modeCode`: 같은 ACTIVE 기본 규칙의 게시 ACTIVE 정책과 서버가 선택한 정확한 시스템 profile/엔진/추출 설정의 지문이다. 호출자가 parser/profile/URL을 선택하는 필드는 없다.
- `maximumDownloadBytes`: 정책 상한(최대 83,886,080 byte). `maximumFileCount=10`, `maximumAttempts=3`, `maximumHttpRequests=132`다. HTTP 상한은 총 3시도 × (상세 1 + 파일 최대 10) × 각 처리의 전체 HTTP 최대 4회다. 중간 페이지/게재기간 조회/최종 POST도 GET redirect와 이 상한을 공유한다. POST redirect는 차단하며 실제 요청 수는 더 적을 수 있다.
- `isAttachmentReviewRequired`, `effectCode`: 기존 검수 binding이 있으면 `PRESERVE_ENFORCE_AND_STALE_CONFIRMATION`, 없으면 정책 mode가 ENFORCE여도 `COLLECT_PREVIEW_ONLY`다. 조건 조회는 실제 격리 추출기가 정상 실행됐다는 증거가 아니며 worker가 외부 요청 전 설치된 런타임 hash/격리를 다시 확인한다.

예약 요청은 조건 조회의 `version`, `expectedPolicyId`(조회 policyId), `expectedPolicyHash`(조회 policyHash), `expectedExecutionHash`(조회 executionHash), 1바이트 이상·조회 상한 이하 `maximumDownloadBytes`, `reason` 1~1000자를 제출한다. 조건 조회 후 정책·profile·기본/첨부 판정·버전이 달라졌으면 409이며 입력을 보존하고 새 조건을 확인해야 한다.

- 제목 통과 PRODUCTION 원문만 대상이다. 연결된 운영 공고, 진행 중 첨부 작업, worker 비활성, OFF/퇴역/ACTIVE 규칙 불일치, 기존 검수 정책과 다른 정책, profile 없음/중복/불일치, 잘못된 실행/예산 설정은 예약하지 않는다. 각 경우에 원인과 후속 행동이 포함된 한국어 오류를 반환한다.
- 조건 조회는 REPEATABLE_READ·read-only이며 정책에 FOR SHARE를 실행하지 않는다. 실제 예약에서만 source 잠금 뒤 정책/규칙 공유 잠금을 잡고 멱등·버전·영향도를 다시 검증한다. 네트워크/디스크 추출 작업은 이 transaction에 포함하지 않는다.
- 새 job은 `operationCode=COLLECT`, `requested_by=운영자`로 자동 수집과 구분한다. 새 세대/미봉인 set에서 전체 첨부를 재발견·처리한다. 기존 일부 실패 집합이 없어도 예약할 수 있어 최초 수집, 발견 실패 후 재발견, NO_FILES 뒤 재확인에 사용한다.
- 전체 재수집은 선택 재시도와 달리 새 발견 목록과 profile 역할로 **모든** 파일 근거를 새로 만든다. 이전 수동 역할/추출/실패는 과거 set/이력으로 보존하며 새 set에 암묵적으로 복사하지 않는다. 이전 확인은 STALE이므로 새 전체 근거로 재검수해야 한다. 성공 파일·수동 역할을 고정해 실패 파일만 처리하려면 24.5를 사용한다.
- 예약은 첨부 버전 +1, 현재 첨부 판정/확인 STALE, intake QUEUED를 원자적으로 기록한다. 기존 검수 binding·정책·base 판정·운영 공고 상태는 바꾸지 않는다. 기존 미적용 source에 새 ENFORCE binding을 만들지 않는다.
- 같은 source·actor·정규화 요청의 같은 Idempotency-Key는 최초 작업을 반환한다. 그 뒤 버전/worker 상태가 바뀌어도 새 작업·요청 횟수를 만들지 않는다. 다른 원문/운영자/입력에 키를 재사용하면 409다.
- 전체 수집과 24.5 선택 재시도는 source별 60초/최근 24시간 3회 초기 한도를 공유한다. 한도 초과는 `ANNOUNCEMENT_ATTACHMENT_COLLECTION_RATE_LIMITED`(429), 입력 오류는 `ANNOUNCEMENT_ATTACHMENT_COLLECTION_INVALID` 또는 `VALIDATION_FAILED`(400), 잘못된 원문은 404, 상태/정책 충돌은 기존 첨부 409 wrapper다.
- 감사에는 job/policy ID·요청량 상한·사유 hash만 저장한다. 사유 원문/URL/본문/첨부 내용은 넣지 않는다. 단건 예약은 운영 정책 게시·ENFORCE 활성화·기존 데이터 일괄 적용 승인을 대신하지 않는다.
- 관리 화면과 실제 PostgreSQL/운영 worker/브라우저 검증은 별도 미완료 Gate다.

### 24.8 수집 시작 시 키워드 규칙·첨부 정책 불일치

- worker 연동이 활성인 수집 실행은 기존 목록 Provider 호출 **전**에 첨부 계획을 확정한다. 현재 ACTIVE 키워드 규칙에 맞는 ACTIVE 첨부 정책이 없고, 다른 규칙 또는 이미 퇴역한 규칙에 연결된 ACTIVE ENFORCE 정책이 남아 있으면 실행을 차단한다. 키워드 분류 context가 없는 경우에도 해당 ENFORCE 의도를 암묵적으로 OFF로 해석하지 않는다.
- 내부 service 오류는 `ANNOUNCEMENT_ATTACHMENT_RULE_POLICY_MISMATCH`/409다. 기존 수집 실행 API는 예외를 수집 결과로 기록하는 v1 계약을 유지하므로 HTTP 409로 바꾸지 않는다. 해당 run은 `FAILED`, `totalCount=0`, `failedCount=1`이며 고정된 한국어 `errorMessage`로 현재 ACTIVE 키워드 규칙의 첨부 정책 검증·게시 후 새 실행이 필요함을 안내한다. 실패 1은 외부 공고 1건 실패가 아니라 실행 준비 실패다.
- 차단된 run은 목록·상세·첨부 HTTP 요청과 source/job/첨부 판정 저장을 하지 않는다. 요청/run 실패 metadata만 남기며 예외 원문·URL·인증정보를 응답 또는 해당 차단 로그에 복사하지 않는다.
- 일치하는 ACTIVE 정책이 있으면 그 정책의 OFF/COLLECT_ONLY/ENFORCE를 따른다. 이미 저장된 FROZEN/OFF run은 이후 정책 변경을 중간에 채택하지 않는다. 기존 NO_POLICY run은 덮어쓰지 않지만 재개 시 미일치 ENFORCE가 발견되면 차단한다. 최초 설치처럼 ENFORCE 정책 자체가 없으면 기존 NO_POLICY 동작을 유지한다.
- 이 방어는 정책 게시, 기존 source 재분류, 검수 의무 원복, ENFORCE 활성화를 수행하지 않는다. worker 연동 비활성 시의 기존 동작도 변경하지 않는다. 신규 정책의 검증·게시 및 운영 설정 변경은 별도 경로/승인 대상이다.
- service/SQL binding/수집 orchestration 회귀를 추가했고, 규칙 퇴역 경합을 포함한 실제 PostgreSQL 검증은 별도 Linux Gate로 관리한다.

### 24.9 첨부 정책 초안·개정 관리

상세 설계의 정책 관리 중 **초안 생성/조회/수정/개정** 계약이다. 초안 저장은 QA 통과, 정책 게시 또는 ENFORCE 적용이 아니다. 이후 추가된 `/validation`, `/publication`, 관리자 화면과 배치 계약은 후속 절을 따른다. 전체 공식 QA와 승인된 운영 적용·업무 E2E는 별도 미완료다.

prefix: `/api/v2/admin/announcement-attachment-policies`. 모든 정상 응답은 `ApiResponse`, 목록 data는 `PageResponse`, 응답 캐시는 `no-store`다.

| method | suffix | 권한 / 계약 |
|---|---|---|
| GET | 빈 경로 | ADMIN/OPERATOR/APPROVER. `status`(DRAFT/ACTIVE/RETIRED), `ruleReleaseId` 선택 필터. page 기본 1, size 기본 20·최대 100. 생성 시각 내림차순/id 내림차순 |
| GET | `/{policyId}` | 동일 READ 3역할. 설정과 시스템 profile binding, 개정 원본, 편집/검증 필요 여부 |
| POST | 빈 경로 | ADMIN만. CSRF·UUID Idempotency-Key 필수. versionNo=1 DRAFT 생성, 201 |
| PUT | `/{policyId}` | ADMIN만. CSRF·expectedVersion 필수. DRAFT만 CAS 수정, 200 |
| POST | `/{policyId}/revisions` | ADMIN만. CSRF·UUID Idempotency-Key·expectedVersion 필수. 새 DRAFT 개정, 201 |

직접 service 호출도 활성 계정·비밀번호 변경 완료·권한을 다시 확인한다. OPERATOR는 첨부 재시도 권한이 있어도 정책 변경은 할 수 없다.

입력:

- 생성: `ruleReleaseId`, `modeCode`(OFF/COLLECT_ONLY/ENFORCE), `maximumSourceBytes`(1~83,886,080 byte), `reason`(공백 아닌 1~1000자).
- 수정: 생성 필드와 `expectedVersion`(조회 rowVersion, 0~2,147,483,646). 시스템 엔진/추출기 버전·profile binding을 다시 고정하고 런타임 설정 지문과 게시 hash를 미검증 상태로 초기화한다.
- 개정: 원본의 `expectedVersion`(0 이상), `reason`. 코드 family를 유지하고 현재 최대 versionNo+1을 배정한다. DRAFT/ACTIVE/RETIRED 원본을 허용하며 설정/profile snapshot을 복사하되 게시 hash/시각과 QA 성공은 복사하지 않는다. 퇴역 규칙을 복사한 초안은 현재 DRAFT/ACTIVE 규칙으로 수정 후 별도 검증해야 한다.
- 정의하지 않은 필드는 모두 400으로 거부한다. URL·parser·profile 목록·임의 settings JSON·실행 명령·extractorConfigHash·게시/QA 성공값을 요청에서 지정할 수 없다. 오류 응답에 그 입력 원문을 복사하지 않는다.

응답:

- `policy`: 식별자/code/versionNo/rowVersion, 정책 상태/mode, 연결 규칙 ID/현재 상태, policyHash, 생성/게시 시각.
- `configuration`: engineVersion, extractorVersion, extractorConfigHash, maximumSourceBytes. 생성/수정 직후 extractorConfigHash=null은 **실제 설치 Linux 런타임 검증 전**이라는 뜻이다. 복사된 기존 hash가 있어도 QA 통과를 뜻하지 않는다.
- `systemProfileBindings`: 서버 registry의 providerCode/profileCode/profileHash. 관리자 선택 입력이 아니며 등록된 일부 profile만 존재하는 현재 범위를 전체 지원으로 표시하면 안 된다.
- 2026-09-12 시스템 registry는 기업마당·대전 서구·새올 GET4기관·화천 POST·부산광역시·서울 강북구의 9개 프로필이다. 일반 첨부 영역의 파일명으로 역할을 확정하지 않으며 `UNKNOWN`으로 발견하고 관리자가 확정한 역할을 유지한다. 강북의 고정 3단계 요청은 게재기간을 확인하고, 부산의 실측 한글 헤더 호환은 해당 프로필에만 적용한다. 공통 고정 호출·다운로드 flow/gateway·타입 검사 코드도 지문에 포함하며 profile 목록/실행 hash 변경 후 초안 저장·전체 QA가 필요하다. 기존 정책/데이터는 자동 갱신하지 않는다. 상세 계약과 실제 다운로드 검증 범위는 `announcement-attachment-saeol-get-profiles-2026-09-12.md`, `announcement-attachment-hwacheon-post-profile-2026-09-12.md`, `announcement-attachment-legal-board-profiles-2026-09-12.md`를 따른다. 전체 profile/격리 추출·DB QA 완료는 아니다.
- `copiedFromPolicyId`, `isEditable`, `isDraftValidationRequired`, updatedAt. isEditable은 현재 ADMIN이 DRAFT를 조회할 때만 true다. DRAFT는 역할과 관계없이 검증 필요 상태다. runtime command/path, 감사 사유 원문, 생성 요청 hash/멱등 키는 반환하지 않는다.

동시성/불변성:

- 같은 actor·정규화 요청·operation·key는 같은 정책 ID를 반환한다. 이후 편집됐으면 그 정책의 **현재 상태**를 반환하며 최초 응답 snapshot을 재현하는 계약은 아니다. 다른 actor/입력/생성-개정 operation에 같은 key를 사용하면 409다.
- 생성 요청 key 잠금, 정책 family 잠금, 변경 대상 행 잠금과 rowVersion CAS를 분리한다. 다른 과거 버전에서 동시에 개정해도 family versionNo는 중복되지 않는다. GET은 행 잠금을 하지 않는 REPEATABLE_READ 조회다.
- 게시/퇴역 행의 수정은 `ANNOUNCEMENT_ATTACHMENT_POLICY_NOT_DRAFT`/409다. 조회 버전·멱등성·시스템 등록 충돌은 `ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT`/409이며 입력을 보존하고 최신 상태를 확인하도록 안내한다. 규칙/정책 미존재는 404, 입력 위반은 400이다.
- 새 초안에 연결할 규칙은 DRAFT/ACTIVE만 허용한다. 키워드 규칙을 자동 게시하지 않는다. source/job/검수 binding·기존 ACTIVE 정책과 운영 공고를 변경하지 않고 네트워크/추출을 실행하지 않는다.
- 감사 로그는 actor/정책 ID/action/버전·설정 hash/사유 hash만 기록하며 사유 원문·설정 JSON·본문은 복사하지 않는다.

검증은 서비스 단위, 실제 Spring HTTP 권한/CSRF/요청 파싱, XML binding과 PostgreSQL 통합 fixture로 구분한다. 실제 PostgreSQL 실행 및 정책 검증·게시 경로가 남아 있으므로 정책 수명주기 전체 완료로 표현하지 않는다.

### 24.10 정책 분류 정답 세트 실행·이력

prefix: `/api/v2/admin/announcement-attachment-policies/{policyId}/classification-checks`.

- GET: ADMIN/OPERATOR/APPROVER, page 기본 1/size 기본 20·최대 100. 생성 시각/id 내림차순 `PageResponse`를 `ApiResponse`로 감싼다.
- POST: ADMIN만. CSRF·UUID Idempotency-Key 필수. `{expectedVersion, reason}`만 받는다. reason은 공백 아닌 1~1000자다. 성공한 분류 이력 201, `Cache-Control: no-store`. 클라이언트 `passed`, 사례 수, 규칙·파서·실행 지문·URL은 400으로 거부한다.
- 응답은 checkId, policyId/policyVersion/policySnapshotHash, ruleReleaseId/ruleVersion/ruleSnapshotHash/ruleContentHash, `checkTypeCode=CLASSIFICATION_GOLDEN`, suiteVersion/engineVersion/resultHash/caseCount/caseIds, isCurrent, createdAt이다. 본문·일치 원문·사유·actor·멱등 키를 반환하지 않는다.
- 서버가 현재 DB 규칙으로 AG-001~030을 실행한다. 입력 읽기와 최종 저장만 짧은 transaction을 사용하며 분류 실행은 transaction 밖이다. 저장 직전 정책·규칙 버전/내용 hash를 다시 확인한다.
- 게시 ACTIVE 규칙의 저장 hash와 현재 내용으로 계산한 hash가 다르면 무결성 오류 409다. 새 실행은 DRAFT 정책과 DRAFT/ACTIVE 규칙만 허용한다. 키워드 규칙 자동 게시 없음.
- 분류 실패는 `ANNOUNCEMENT_ATTACHMENT_POLICY_QA_FAILED`/409, 변경 충돌은 `ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT`/409, 게시·퇴역 정책 새 실행은 `ANNOUNCEMENT_ATTACHMENT_POLICY_NOT_DRAFT`/409다. case ID와 고정 한국어 원인만 안내하고 외부 예외/입력 원문은 복사하지 않는다.
- 같은 actor·정책·정규화 요청/key는 같은 이력을 반환한다. 이력 이후 수정됐어도 과거 결과를 덮어쓰지 않고 isCurrent=false를 반환한다. 다른 actor/정책/입력의 키 재사용은 409다. GET도 현재 입력 버전 일치 여부를 다시 계산한다.
- 이 분류 성공은 전체 `/validation` 또는 `/publication` 성공이 아니다. isCurrent는 정책·규칙 입력 버전의 일치일 뿐, runtime/profile/운영 검증 여부가 아니다. policyHash·extractorConfigHash·DRAFT 상태·rowVersion·기존 source/job/검수 binding은 바꾸지 않는다.

전체 QA·게시의 남은 요구는 [정책 검증·게시 실행 계약](announcement-attachment-policy-validation-2026-09-11.md)을 따른다.

### 24.11 비동기 정책 QA 예약·취소·단계 이력

prefix: `/api/v2/admin/announcement-attachment-policies/{policyId}/validation-runs`.

| Method/path | 권한·응답 | 입력 |
|---|---|---|
| GET prefix | ADMIN/OPERATOR/APPROVER, 200 `ApiResponse<PageResponse>` | page 기본 1, size 기본 20·최대 100 |
| GET `/{runId}` | 동일 읽기 권한, 200 `ApiResponse` | 정책과 실행 ID 범위 일치 필수 |
| POST prefix | 활성 ADMIN, 202 예약(완료 아님), `ApiResponse` | CSRF·UUID Idempotency-Key, `{expectedVersion, reason}` |
| PUT `/{runId}/cancellation` | 활성 ADMIN, 200 `ApiResponse` | CSRF, `{expectedVersion, reason}` |

- `expectedVersion`은 예약 시 **정책 rowVersion**, 취소 시 **run rowVersion**이다. 0 이상이며 reason은 공백 아닌 1~1000자다. 미정의 필드·URL·임의 파일 경로·profile·실행 성공값/hash를 모두 400으로 거부한다. 응답은 `no-store`다.
- 응답: runId, policyId/policyVersion, ruleReleaseId/ruleVersion, snapshotHash, statusCode/rowVersion, inputVersionsCurrent, errorCode, createdAt/startedAt/completedAt, steps. 각 단계는 stepCode/statusCode/evidence/evidenceHash를 반환한다. 서버 생성 case ID/품질·hash·정리 결과만 포함하며 원문 snapshot/설치 경로/URL/사유·actor/key/token은 반환하지 않는다.
- `inputVersionsCurrent`는 정책·규칙의 현재 DB 버전 일치 여부일 뿐 전체 QA, 실제 설치 환경, 현재 profile, 게시 가능 상태를 뜻하지 않는다.
- 상태: PENDING/RUNNING/CANCEL_REQUESTED/CANCELLED/INCOMPLETE/FAILED/CONFLICT/VERIFIED. 단계: CLASSIFICATION_GOLDEN/INSTALLED_RUNTIME/PROVIDER_PROFILES/WORKER_DB_RECOVERY. 미실행 단계는 NOT_RUN, 저장 단계는 PASSED/FAILED/MISSING이다.
- 현재 서버는 분류30건·고정 합성 runtime12건과 독립 Linux worker DB 계약 실행 경로를 제공한다. WORKER_DB_RECOVERY는 실제 자식의 전체 suite/case·설치 지문·취소/정리 검증 후 PASSED 또는 FAILED로 저장한다. PROVIDER_PROFILES는 MISSING이므로 VERIFIED 생성은 아직 불가하다. 정책 게시 API는 후속 절에 구현됐으나 전체 QA 조건을 우회하지 않는다. 과거 분류 이력/클라이언트 제출 결과를 전체 성공으로 수용하지 않는다.
- 신규 예약은 `SANEB_ANNOUNCEMENT_ATTACHMENT_POLICY_QA_ENABLED=true`와 실제 Linux 설치 identity가 필요하다. 기본값 false. 전역 대기/실행 1개, 같은 정책 60초 간격·최근 24시간 최대 3회(실패·취소 포함). 현재 외부 공고 HTTP 요청 0회이며 수집 job/운영 공고/정책 모드 쓰기 없음.
- 같은 actor·정책·정규화 입력/key는 같은 실행을 반환한다. worker 비활성화·입력 버전 변경 후에도 기존 이력은 재실행하지 않는다. 다른 actor/정책/입력의 같은 key는 409다.
- DRAFT 정책·DRAFT/ACTIVE 규칙을 고정하고 claim은 추출 슬롯 확보와 같은 transaction이다. 실행 lease 8분, 매 파일 시작 시 잔여 40초 확인, parser 실행은 DB transaction 밖이다. PENDING 취소는 즉시 CANCELLED, RUNNING 취소는 CANCEL_REQUESTED 후 현재 파일 정리 뒤 CANCELLED다. 만료는 FAILED/LEASE_EXPIRED이며 새 예약으로 재검증한다.
- 정책/실행 미존재·다른 정책의 run은 404, 권한 부족은 403, 입력은 400. 구버전·한도·비활성 worker·변경된 입력은 `ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT`/409, 설치 identity 부재는 `ANNOUNCEMENT_ATTACHMENT_POLICY_QA_FAILED`/409다. 비동기 실행 실패는 HTTP 예약 성공과 구분하여 조회의 상태/오류/단계 증거로 확인한다.

실제 PostgreSQL trigger/lease 동시성·Linux runtime·모든 운영 profile·정책 UI·게시·운영 브라우저 검증은 필수 미완료이며 별도 Gate를 따른다.

### 24.12 기존 데이터 배치 범위 미리보기·고정·취소

prefix: `/api/v2/admin/announcement-attachment-batches`.

| Method/path | 권한·응답 | 동작 |
|---|---|---|
| POST `/scope-preview` | ADMIN/OPERATOR/APPROVER, CSRF, 200 | 필터 JSON의 순수 DB 조회, HTTP/다운로드/쓰기 0 |
| POST prefix | ADMIN, CSRF·UUID Idempotency-Key, 201 | `{scope,expectedScopeHash,reason}` → SCOPE_READY batch/jobs 고정 |
| GET prefix | 읽기 3역할, 200 | page 기본 1/size 기본 20·최대 100 |
| GET `/{batchId}` | 읽기 3역할, 200 | 고정 범위와 현재 jobs/삭제 건수 |
| GET `/{batchId}/items` | 읽기 3역할, 200 | 고정된 남은 항목 pagination, 필터 재실행 없음 |
| PUT `/{batchId}/scope-cancellation` | ADMIN, CSRF, 200 | `{expectedVersion,reason}` → 수집 전 SCOPE_READY만 CANCELLED |

모든 응답은 ApiResponse, 목록은 PageResponse를 포함하며 no-store다. scope 필드는 policyId, providerCodes, collectedFrom/collectedBefore(시작 포함·끝 제외 ISO offset 시각), 선택 deadlineFrom/deadlineThrough(양끝 포함), maximumCount(1~1000)다. provider는 BIZINFO/GOV24/LOCAL_GOV_NOTICE만, 중복 불가다. 최초 운영 실행은 설계대로 100건 이하부터 승인받는다.

범위 응답은 전체 provider/제외 사유별 counts, candidateCount/selectedCount/remainingCount, 선택 IDs와 readinessCode, scopeHash, 정책·규칙 ID/hash, 최대 bytes/HTTP와 currentHttpRequests=0을 구분한다. 선택한 source가 준비 불가여도 다음 후보로 대체하지 않고 canReserve=false다. 파일 개수나 최종 판정 변경 건수를 추정 성공값으로 반환하지 않는다. 미등록 출처 PROFILE_REQUIRED, 규칙 불일치 BASE_RECLASSIFICATION_REQUIRED, 진행 작업 ACTIVE_JOB, 버전 상한 VERSION_LIMIT는 항목별 사유다.

고정 순서는 수집 시각 오름차순·UUID며 maximumCount 초과분은 이번 배치가 아니다. QA/삭제 원문은 조회하지 않고, 제목/기본 판정 부적격·연결된 운영 공고는 건수로 구분하여 제외한다. API에 sourceIds·임의 URL/파서/profile·연결 공고 포함·성공값·요청량 변경을 추가 입력하면 400이다. reason은 공백 아닌 1~1000자, scope hash는 64자리 SHA-256이다.

예약은 source/current/검수/운영 공고를 변경하거나 worker를 실행하지 않는다. SCOPE_READY는 같은 원문의 다른 수집 예약과 충돌할 수 있다. 같은 actor·필터·지문·사유/key는 최초 batch 현재 상태를 반환하고 재실행하지 않는다. 다른 입력/key 재사용, 범위/정책/출처/버전 변화, 준비 불가, 구버전 취소는 409다. 미존재 404/권한 403/인증 401/잘못된 입력 400 wrapper를 유지한다.

상세는 batchId/policyId/statusCode/scopeHash/rowVersion, itemCount/remainingItemCount/deletedItemCount, jobCounts, frozenScope, createdAt이다. 원문 제목/URL/사유·요청자/멱등 키/실행 설정 원문은 반환하지 않는다. 원문 삭제 시 ID를 되살리지 않고 삭제 건수를 보존하며 새 source를 자동 보충하지 않는다.

수집 시작/중지/재개는 아래 24.13을 따른다. 배치 분류 preview 확정·적용·적용 pause/resume·rollback·배치 화면은 미완료다. 수집 전 취소는 적용 후 원복을 뜻하지 않는다. 전체 범위와 후속 조건은 [배치 실행 계약](announcement-attachment-batch-execution-2026-09-11.md)을 따른다.

### 24.13 고정 배치 수집 시작·중지·재개

prefix는 24.12와 같다. 모두 PUT/활성 ADMIN/CSRF/200 ApiResponse/no-store다. 기존 v1과 범위 POST의 멱등 계약은 변경하지 않는다.

| 경로 | 요청 | 효과 |
|---|---|---|
| `/{batchId}/collection` | Collection 확인 입력 | SCOPE_READY → COLLECTION_PENDING; 고정 jobs만 PENDING |
| `/{batchId}/collection-pause` | `{expectedVersion,reason}` | 수집 대기/진행 → COLLECTION_PAUSED |
| `/{batchId}/collection-resume` | Collection 확인 입력 | COLLECTION_PAUSED → COLLECTING; job 예산·시도·terminal 유지 |

Collection: `{expectedVersion,expectedScopeHash,expectedItemCount,expectedDeletedItemCount,expectedMaximumDownloadBytes,expectedMaximumHttpRequests,reason}`. scopeHash 64자리, itemCount 1~1000, 버전/삭제 건수 0 이상, 최대 bytes/HTTP 1 이상, 사유 공백 아닌 1~1000자다. 모든 값은 조회된 고정 범위/최초 상한과 정확히 일치해야 한다. 임의 cap·대상/URL/profile·성공값 입력은 허용하지 않는다.

상태/버전/범위·삭제 건수/상한 불일치, 퇴역·변경된 정책/규칙, 변경된 고정 source/base/첨부/current/검수/정책 binding/출처 지문은 409다. 최초 시작 전에 삭제된 항목은 예약 취소·새 범위 고정이 필요하다. 재개 시 terminal 항목은 재시도하지 않으며 미완료 항목만 재검증한다. 응답 손실/409 때 GET으로 상태를 확인하며 동일 과거 버전 요청은 중복 시작하지 않는다.

중지는 새 claim/HTTP를 막지만 이미 전송 중인 요청/정리는 끝날 수 있다. worker 설정/정책 게시/현재 판정·확인·운영 공고를 변경하지 않는다. 저장된 승인 지문은 최초 실행만 기록하고 재개 시 덮어쓰지 않는다. job의 FROZEN_INPUT_CHANGED는 버전 외 출처/이전 검수/보호 연결 변경으로 인한 종료다.

worker 활성 시 DB 집계는 COLLECTION_PENDING → COLLECTING → COLLECTED/COLLECTION_PARTIAL_FAILED를 반영한다. 고정 건수 전체가 terminal이고 삭제 0·모든 SUCCEEDED/SEALED set/preview evaluation/hash를 만족해야 COLLECTED다. 중지 상태 자동 해제/적용/확인/활성화는 없다. GET jobCounts/삭제 수와 batch 상태의 주기 차이를 구분한다. 실제 PG·Linux·운영 브라우저 검증은 미완료다.

### 24.14 봉인 결과 미리보기·선택 이력

prefix: `/api/v2/admin/announcement-attachment-batches/{batchId}/classification-preview`. 기존 v1 계약은 변경하지 않는다.

| Method/path | 권한·응답 | 입력/효과 |
|---|---|---|
| POST prefix | ADMIN·CSRF·UUID Idempotency-Key, 201 | `{expectedVersion,expectedScopeHash,reason}` → 새 미리보기, 선택 0건 |
| PUT `/selection` | ADMIN·CSRF·UUID Idempotency-Key, 200 | `{expectedVersion,expectedPreviewHash,selectedJobIds,reason}` → 새 선택 snapshot |
| GET prefix | ADMIN/OPERATOR/APPROVER, 200 | 정확한 current preview pointer/hash 조회 |
| GET `/{previewId}` | 읽기 3역할, 200 | 같은 batch의 과거 snapshot 및 현재 유효 여부 |
| GET `/{previewId}/items` | 읽기 3역할, 200 | snapshot 항목 pagination, page1/size20 기본·size최대100 |

모든 응답 ApiResponse/no-store, 목록 PageResponse다. 사유는 공백 아닌1~1000자, 버전0~2147483645, 지문은 소문자16진수64자리다. selectedJobIds는 중복 없는 UUID 최대1000개, 빈 배열은 전체 선택 해제이며 null은400이다. sourceIds/URL/파일/profile/정책 변경/성공값 등 미정의 필드는400이다. 없는 batch/다른 batch의 preview는404, 권한403/인증401이다.

생성은 수집 종료 또는 PREVIEW_READY 계열에서만 가능하다. 선택은 현재 preview와 같은 batchVersion/previewHash/inputHash여야 하며 입력 변경·구버전·퇴역/변경 정책·READY 아닌 항목 또는 다른 batch 선택은409다. 동일 actor/배치/정규화 입력/키 재요청은 원래 이력을 반환하고 현재 선택을 되돌리지 않는다. 다른 입력의 키 재사용은409다. 새 미리보기 생성은 선택을 자동 승계하지 않고0건으로 만든다.

요약: previewId, batchId, statusCode, scopeHash/inputHash/previewHash, snapshotBatchVersion/currentBatchVersion, itemCount, snapshotRemainingItemCount/snapshotDeletedItemCount, availableItemCount/currentDeletedItemCount, eligibleItemCount/selectedItemCount, currentPreview/inputsCurrent, currentHttpRequests=0, createdAt. statusCode와 eligible/selected 수는 저장 당시 값이다. inputsCurrent=false인 과거 PREVIEW_READY를 현재 적용 가능 상태로 해석하지 않는다.

항목: jobId/sourceId/providerCode, readinessCode, eligible/selected, evidence. evidence는 base/이전 첨부/제안 판정, 기본 검수 상태·기존 확인 유무, 규칙 ID/version/hash, 발견·파일·실패·추출·역할 metadata, exact extraction ID, base AUTO/이전 AUTO/CONFIRMED/제안 AUTO 태그와 추가·제거 차이를 분리한다. 원문/첨부 텍스트·URL·파일 표시명·사유/actor/key·실행 입력 JSON은 반환하지 않는다.

READY는 성공적으로 수집된 완전한 SEALED set/evaluation 근거와 현재 고정 입력이 일치함을 뜻한다. 제안 REVIEW_REQUIRED도 포함될 수 있으나 최종 승인이나 관리자 확인 완료가 아니다. 기타 readinessCode는 COLLECTION_NOT_SUCCESSFUL/EVIDENCE_INCOMPLETE/PROTECTED_LINK/ACTIVE_JOB/SOURCE_CHANGED다. 삭제 건수까지 전체 분모를 유지하고 한 항목이라도 준비 불가면 PREVIEW_PARTIAL_FAILED다.

이 작업은 저장된 근거만 읽고 새 HTTP/다운로드/추출을 실행하지 않는다. 한 transaction에서 PREVIEW_RUNNING을 거쳐 새 전체 snapshot/선택/current pointer/hash를 저장하며 batch version은2 증가한다. 기존 current 평가·확인·정책 binding·운영 공고는 변경하지 않는다. 적용과 원복은 아래 별도 API다. 관리자 배치 화면·PG/운영 E2E는 미완료다.

### 24.15 배치 적용 승인·중지·재개·결과 조회

기준 경로 `/api/v2/admin/announcement-attachment-batches/{batchId}/application`. 기존 v1 변경 없이 ApiResponse/PageResponse와 no-store를 사용한다.

- POST 기본/`pause`/`resume`: 활성 ADMIN, CSRF, UUID Idempotency-Key. expectedVersion/expectedPreviewId/expectedPreviewHash/expectedItemCount/expectedSelectedCount/expectedDeletedCount/acknowledgeReviewReset=true/reason을 엄격히 검증한다. 202는 접수이며 적용 완료가 아니다. 다른 키 입력·버전·범위·정책 충돌409, 유효하지 않은 입력400, 권한403, 미인증401이다.
- GET `/actions/{actionId}`: ADMIN/OPERATOR/APPROVER, 동일 batch의 영수증+현재 집계. 다른 배치/없는 내역404. 최초 actionId/previewId/actionCode/acceptedFromVersion/acceptedAt, 승인 scopeItemCount/approvedSelectedCount와 현재 currentStatusCode/currentVersion/remainingItemCount/deletedItemCount/selectedRemainingCount/pendingCount/appliedCount/conflictCount/failedCount/currentHttpRequests=0을 분리한다.
- GET `/items?page=1&size=20`: 동일 읽기 역할, size1~100. 남은 전체 job의 jobId/sourceId/collectionStatusCode/selected/applicationStatusCode/applicationErrorCode/applicationAttemptCount/nextAttemptAt/appliedEvaluationId/appliedSourceVersion/appliedAttachmentVersion/rollbackStatusCode를 반환한다. 삭제 원문이나 URL·제목·본문·검수 메모를 반환하지 않는다.
- START는 현재 고정 ACTIVE ENFORCE policy/rule과 전체 미리보기 지문/선택 수를 source·정책·batch 잠금 아래 대조하고 선택 성공 항목만 PENDING으로 만든다. COLLECT_ONLY/새 정책으로 자동 승격하지 않는다. 같은 키 재요청은 최초 영수증만 반환하며 현재 상태를 재시작하지 않는다.
- 항목별 worker는 exact item 해시와 source CAS 후 이전 confirmation을 STALE로 만들고 첨부 current/binding/검수 요구 및 첨부 버전을 갱신한다. 운영 공고/DRAFT 자동 생성·수집 HTTP는 없다. 전체 고정 범위의 미선택/실패/삭제를 숨기지 않는다. 중지는 다음 항목부터 효력이 있고 완료 항목의 원복이 아니다.
- 현재 적용 코드/로컬 검증과 실제 운영 적용은 별개다. 조건부 batch rollback은 24.16에 추가했으며 전체 배치 UI, 최신 PostgreSQL/운영 E2E는 미완료다. 상세 전이·실패 코드·backoff는 [배치 실행 계약](announcement-attachment-batch-execution-2026-09-11.md)을 따른다.

### 24.16 고정 배치 원복 영향·승인·결과

prefix: `/api/v2/admin/announcement-attachment-batches/{batchId}/rollback`. 기존 v1은 변경하지 않는다. ApiResponse/PageResponse·no-store를 유지하며 쓰기에는 ADMIN·CSRF·UUID Idempotency-Key가 필요하다. Service도 활성 계정·권한·비밀번호 변경 완료를 검사한다.

- GET `/preview`: ADMIN/OPERATOR/APPROVER. APPLIED/APPLY_PARTIAL_FAILED/APPLY_PAUSED에서만 미리본다. 현재 version/statusCode/previewHash, 최초 scopeCount와 남은/삭제 수, 적용 완료분 전체 targetCount·eligibleCount·conflictCount, baseReopenCount·confirmationRestoreCount·staleConfirmationCount·cancelPendingCount 및 currentHttpRequests=0이다. HTTP/DB 쓰기 없이 현재 입력으로 계산하며, 일부 적격 항목만 전체 대상으로 축소하지 않는다.
- GET `/items?page=1&size=20`: 같은 조회 역할, size1~100. 남은 전체 job의 jobId/sourceId/providerCode/applicationStatusCode/rollbackStatusCode/readinessCode, target/eligible/baseReopens/confirmationRestores/staleConfirmationRemains, errorCode/attemptCount/nextAttemptAt를 반환한다. 완료 뒤의 READY 여부는 새로운 원복 허가가 아니며 과거 승인 집계는 영수증에서 조회한다.
- POST prefix: `{expectedVersion,expectedPreviewHash,expectedScopeCount,expectedTargetCount,expectedDeletedCount,expectedBaseReopenCount,expectedConfirmationRestoreCount,expectedCancelPendingCount,acknowledgeBindingRestoration:true,reason}`. 정확한 현재 미리보기와 1~1000 scope/대상, 0~1000 영향 수, 사유1~1000자를 검증한다. force/직접 source 목록·정책 변경·임의 결과 필드는 거부한다. 적격0이면409다. 수집 완료나 원복 성공이 아니라 **202 접수 영수증**을 반환한다.
- GET `/actions/{actionId}`: 같은 조회 역할. 최초 actionId/batchId/acceptedFromVersion/acceptedAt 및 scopeCount/approvedTargetCount/approvedEligibleCount/approvedBaseReopenCount/approvedConfirmationRestoreCount/cancelledPendingCount를 보존하고, 현재 statusCode/currentVersion/remainingTargetCount/deletedCount/pendingCount/rolledBackCount/conflictCount/failedCount/currentHttpRequests=0을 별도 제공한다. 교차 batch 식별자는404다.

승인은 source UUID 순서 잠금→batch 잠금 후 전체 해시/범위를 다시 확인한다. 적용 완료분 전부를 고정하고 초기 충돌 항목도 이력에 남긴다. APPLY_PAUSED의 남은 적용 PENDING은 `CONFLICT/APPLICATION_CANCELLED_BY_ROLLBACK`으로 종료하며 재개하지 않는다. 같은 멱등 키·actor·payload는 최초 영수증만 반환한다.

원복 worker는 별도 `SANEB_ANNOUNCEMENT_ATTACHMENT_ROLLBACK_ENABLED=true`에서 실행된다(기본 false). 다운로드 worker나 ACTIVE 정책 상태에 종속되지 않지만 승인된 ROLLING_BACK 항목만 DB에서 처리한다. 이후 current/버전/전체 입력 지문/후속 검수·역할·link·활성 작업 변경은 충돌로 끝난다. 이전 pointer·정책/review binding을 복원하고 첨부 버전을 +1한다. 이전에 유효했던 확인만 복구 근거를 기록하여 재사용하며, 이미 무효였던 확인은 STALE로 유지한다. 운영 공고·원문·첨부 이력 삭제/자동 활성화·다운로드·재분류는 없다.

예상치 못한 실패는 항목 transaction 전체 rollback 뒤 별도 실패 횟수를 저장한다. 30초 간격 최대3회, 마지막은 FAILED/ROLLBACK_TRANSACTION_FAILED다. 전체 최초 scope가 모두 원복되고 삭제0일 때만 ROLLED_BACK이다. 미적용/취소/실패/삭제가 있으면 ROLLBACK_PARTIAL_FAILED이며 승인 대상의 부분 결과와 구분한다. 일반 job 원복은24.17로 분리했다. 전체 화면·실제 DB/운영 E2E는 후속 필수 Gate다.

### 24.17 일반 작업 원복 영향·동기 승인·영수증

prefix: `/api/v2/admin/announcement-sources/{sourceId}/attachment-jobs/{jobId}/rollback`. 기존 v1 응답/의미는 변경하지 않는다. ApiResponse/no-store를 유지한다. source/job/batch 소유 범위를 서버에서 검증하며 batch 작업은 이 API로 복구하지 않는다.

| 요청 | 권한·응답 | 의미 |
|---|---|---|
| GET `/preview` | 활성 ADMIN/OPERATOR/APPROVER, 200 | 원문 한 건의 복구 모드·현재 버전·영향·지문·차단 사유 조회 |
| POST prefix | 활성 ADMIN·CSRF·UUID Idempotency-Key, 200 | 명시적 영향 승인과 원복을 동일 transaction에서 완료한 영수증 |
| GET `/actions/{actionId}` | 활성 ADMIN/OPERATOR/APPROVER, 200 | 동일 source/job/action의 불변 영수증 |

Preview 필드: sourceId/jobId, modeCode(`APPLIED`/`FAILED_RESERVATION`/`UNAVAILABLE`), jobStatusCode/applicationStatusCode/rollbackStatusCode, readinessCode, sourceVersion/attachmentVersion/previewHash, baseReopens/confirmationRestores/staleConfirmationRemains, targetCount=1/currentHttpRequests=0. 원문·파일·검수 메모·actor/key·내부 execution JSON은 반환하지 않는다.

readinessCode: READY, CURRENT_BINDING_CHANGED, PREVIOUS_BINDING_INVALID, JOB_NOT_TERMINAL, RECOVERY_EVIDENCE_MISSING, ALREADY_RECOVERED. READY도 자동 승인 또는 파일 처리 성공이 아니다. 완료된 일반 적용과 실패로 종료된 검수 예약만 대상이며 진행 중/재시도 대기/과거 근거 없는 작업은 차단한다.

POST body는 `{expectedSourceVersion,expectedAttachmentVersion,expectedPreviewHash,expectedBaseReopen,expectedConfirmationRestore,acknowledgeBindingRestoration:true,reason}`이다. 원문 버전0이상, 첨부 버전0~2147483646, 지문 소문자16진수64자리, effect boolean 두 값 필수, 사유1~1000자다. force/임의 source 목록·정책 변경·결과 override는400으로 거부한다. 비로그인401/권한403/다른 source·job·action404/기존 입력·버전·효과 변경 및 안전 조건 불충족409다. Service에서도 활성 계정·비밀번호 변경 완료·ADMIN을 검사한다.

원복은 source→job 잠금 뒤 preview를 재검증한다. 후속 검수·DRAFT link·다른 활성 job·본문/출처/기관 입력 변경을 덮어쓰지 않는다. 원래 실패와 APPLIED 근거를 보존하고 이전 pointer/policy/review/확인 연결을 새 첨부 버전에서 복구한다. 이전 확인이 이미 무효라면 STALE로 유지한다. 이전 상태도 검수 대기였다면 그 의무까지 제거하지 않는다. 다운로드/추출/원문 삭제/운영 공고 자동 활성화는 없다.

Receipt: actionId/sourceId/jobId/modeCode/statusCode=ROLLED_BACK, restoredSourceVersion/restoredAttachmentVersion, baseReopened/confirmationRestored, targetCount=1/currentHttpRequests=0, recordedAt. recordedAt은 영수증 기록 시각이며 이후 source의 현재 상태나 DB commit 시각을 뜻하지 않는다. 같은 actor/source/job/key/payload의 재요청은 최초 영수증만 반환한다. 새로운 키로 재복구하거나 다른 actor/payload로 키를 재사용할 수 없다. 200은 transaction 완료 응답이며 batch의202 대기 응답과 구분한다. 응답 유실/5xx는 성공을 추정하지 말고 같은 키·본문으로 재조회/재요청한다.

일반 작업 탐색: GET `/api/v2/admin/announcement-sources/{sourceId}/attachment-recovery-jobs?page=1&size=10`을 추가한다. 활성 ADMIN/OPERATOR/APPROVER만 조회하며 ApiResponse<PageResponse<JobSummary>>/no-store다. page는1~1000000, size는1~100이다. 목록과 count 모두 같은 PRODUCTION·비제외 원문의 batch_id IS NULL 작업만 대상으로 하고 created_at DESC,id DESC로 정렬한다. 실패·미적용·이미 복구된 작업을 숨기지 않는다. 존재하지 않거나 QA/제외 원문은404, 유효 원문의 일반 작업0건은 정상 빈 페이지다.

JobSummary 필드: sourceId/jobId/operationCode/jobStatusCode/applicationStatusCode/rollbackStatusCode/actionId(nullable)/createdAt. actionId는 해당 job의 normal_rollback_action_id이며 새로고침 뒤에도 기존 영수증을 찾는 용도다. 목록은 READY 판정이나 원복 승인이 아니다. readiness와 현재 영향은 개별 preview를 조회한다. raw 내용·URL·사유/사유 지문·멱등 키·actor·execution snapshot은 목록에 포함하지 않는다. 조회로 정책/worker/source를 변경하지 않는다.

일반 원복 UI는 기존 `/app/admin/collected-announcements/{sourceId}/attachments`에 별도 영역으로 연결했다. ADMIN만 승인하며 영향 조회와 동의는 매번 분리한다. 200이라도 영수증 식별자·모드·버전·영향이 승인과 다르면 성공으로 표시하지 않는다. 응답 유실 후 재시도의401/403/409도 최초 요청이 실패했다는 증거가 아니므로 동일 키/본문을 보존하고 새 변경을 잠근다. 상세 UI 계약은 `announcement-attachment-recovery-ui-2026-09-11.md`를 따른다.

코드·로컬 검증과 실제 운영 원복은 별개다. 운영 실행, 최신 PostgreSQL migration/원자성, 배치 화면의 실제 검증 및 역할별 실제 브라우저 E2E는 아직 필수 미완료다.

### 24.18 기존 데이터 첨부 배치 관리자 화면

`/app/admin/announcement-attachment-batches`는 ADMIN/OPERATOR/APPROVER용 no-store SSR 화면이다. ADMIN만 변경 요청을 하며 Service/API의 기존 활성 계정·역할·CSRF 검증을 그대로 따른다. `수집 공고 검수 → 첨부 배치 작업`으로 진입한다. 기존 v1/v2 JSON 계약을 바꾸거나 새 정책 게시/worker 설정 변경 API를 추가하지 않는다.

- 목록/정책은 페이지 조회, 범위는 read-only `scope-preview` POST로 조회한다. 배치 API의 정부24 입력 코드는 `GOV24`이며 단건 UI의 별도 표기 코드를 혼용하지 않는다. 관리자에게 URL·파서·profile 선택을 요구하지 않는다.
- 범위의 전체 후보/선택/잔여, 보호 제외·준비 불가 사유와 HTTP/bytes 상한을 표시한다. 범위 고정 예약은 HTTP0이며 수집 시작과 별도로 동의한다. 한 배치는 전체 기존 데이터 처리 결과가 아니다.
- collection/start·pause·resume는 기존 버전 CAS를 사용한다. 응답 유실 뒤 같은 요청 재확인 또는 명시적 최신 상태 조회/새 기준 동의로 복구한다. 새 기준 사용은 최초 요청 성공 판정이 아니다.
- classification-preview의 전체 페이지(100개씩, 최대1000개)를 읽고 마지막에 현재 미리보기의 ID/hash/버전/입력을 재검증한다. 화면 페이지는10개씩 보여주되 다른 페이지의 선택을 보존한다. 미저장 선택이 있으면 다른 배치 이동/새 예약을 막고 저장 또는 명시적 선택 복원을 요구한다.
- 저장된 선택·현재 preview만 적용 승인에 사용한다. COLLECT_ONLY를 ENFORCE로 자동 승격하지 않는다. 입력/범위/선택 변경은 실행 동의를 해제한다. 적용 후 이전 확인 STALE·재검수 필요를 안내한다.
- 원복은 적용 완료분 전체의 적격/충돌·기본 경로 재개·이전 확인 복구·남은 적용 대기 취소를 표시하고 승인한다. ROLLING_BACK/종료 후에는 새 원복 preview를 요청하지 않고 항목/기존 영수증을 읽는다.
- 멱등 변경의 응답 유실/5xx/잘못된 영수증 후에는 원래 key/body만 재요청한다. 이어진401/403/409를 최초 요청 실패로 간주해 새 키를 만들지 않는다. 202는 접수이며 실제 적용·원복 상태/건수와 구분한다. 접수 후 목록 갱신이 실패해도 배치·접수 URL과 확인한 응답을 보존한다.
- 사유·키·본문은 탭 메모리만 사용하고 이탈 경고를 제공한다. 비민감 배치/페이지/action ID만 URL로 복원한다. 모든 문자열은 textContent로 렌더링한다. 본문·파일 근거는 기존 단건 검수 화면으로 연결한다.

상세 설계/검증 범위는 `announcement-attachment-batch-ui-2026-09-11.md`다. Node 모의 API 흐름은 서버 DB transaction 또는 실제 브라우저 검증이 아니다. 정책 관리 화면·1000건 초과 전체 분할 ledger·실제 Linux/PG·운영 승인 범위 적용·역할별 브라우저 E2E는 별도 필수 잔여다.

### 24.19 기존 데이터 전체 후보 고정·분할 목록

`/api/v2/admin/announcement-attachment-backfills`에 전체 목록 고정 API를 추가한다. 기존 배치 API의 `maximumCount`와 의미가 다르며 기존 v1/v2 계약은 변경하지 않는다. 원문/제목/본문/URL·멱등 키·actor·원문 사유는 응답하지 않는다. ApiResponse/PageResponse와 no-store를 유지한다.

| 메서드/경로 | 권한 | 결과 |
|---|---|---|
| POST `/scope-preview` | 활성 ADMIN/OPERATOR/APPROVER·CSRF | 전체 배타 집계, candidateCount/candidateHash/scopeHash, 예상 segmentCount, previewHttpRequests=0, canInventory |
| POST prefix | 활성 ADMIN·CSRF·UUID Idempotency-Key | 201, 전체 고정 목록. 수집/적용 승인이 아니며 job을 생성하지 않음 |
| GET prefix, `/{runId}` | 읽기 3역할 | 전체 목록 페이지/상세, statusCode=INVENTORIED |
| GET `/{runId}/segments` | 읽기 3역할 | 최초 분할 번호/건수, 현재 잔여/삭제 건수 |
| GET `/{runId}/segments/{segmentNo}/items` | 읽기 3역할 | 고정 source/content/base/rule 식별자·순번·입력 지문·currentInputMatches, 현재 필터를 다시 실행하지 않음 |

Scope: `{policyId,providerCodes,collectedFrom,collectedBefore,deadlineFrom?,deadlineThrough?,segmentSize}`. 지원 출처는 BIZINFO/GOV24/LOCAL_GOV_NOTICE 중 중복 없는 1~3개이며 수집 시작 포함/종료 미포함, 마감일 양끝 포함, UTC 정규화다. segmentSize 1~1000은 한 분할 크기이며 전체 후보 상한이 아니다. 전체 집계/고정 SQL에는 LIMIT가 없다.

목록 고정 body는 `{scope,expectedScopeHash,expectedCandidateCount,reason}`. 지문은 소문자 16진수64자리, 확인한 전체 후보 수1이상, 사유1~1000자다. maximumCount/sourceIds/URL/profile/성공값 등 정의하지 않은 필드는400이다. 같은 actor/key/내용은 최초 run을 재조회하며 다른 actor/내용 또는 진행 중 동일 요청은409다. 처리 중이면 입력을 바꾸지 말고 같은 키로 재확인한다. 대기 후 오래된 REPEATABLE READ snapshot을 사용하지 않도록 advisory try-lock을 쓴다.

응답은 candidateCount(불변 최초 분모), remainingItemCount, deletedItemCount, segmentSize, segmentCount, rowVersion, frozenScope, createdAt을 구분한다. `INVENTORIED`는 처리 완료가 아니다. 원문 삭제 후 빈 분할도 유지하고 잔여0을 수집/적용 성공으로 표현하지 않는다. 입력 변경은 고정 항목을 숨기는 대신 currentInputMatches=false로 표시한다.

page1이상/size1~100. 고정 전체 목록의 후보 합·분할 합·잔여·삭제가 어긋나면409이며 일부 건수로 성공 응답하지 않는다. 기존 PageResponse 정수 페이지 범위를 초과하면 명시적409로 거부한다. scope 고정 transaction의 시간 상한은30초이며 timeout/경합은 전체 rollback이지 첫 N건 저장이 아니다.

V74와 `announcement-attachment-backfill-ledger-2026-09-12.md`를 따른다. 고정 분할→기존 batch 예약 연결과 전체 집계의 로컬 구현은 아래24.20, 관리자 분할 UI는24.21에 추가했다. **실제 PostgreSQL/운영 실행·최종 결과 대조와 브라우저 검증은 필수 미완료**다. 목록 고정만으로 전체 기존 데이터 처리를 완료했다고 보고하지 않는다.

### 24.20 고정 분할 예약·전체 결과 집계 — V75

24.19의 고정 run/segment를 기존 batch 실행 경로와 연결하는 API다. 원래 필터를 재실행하거나 sourceIds를 HTTP 요청으로 받지 않는다. 기존 일반 배치의 지문/수집 승인/삭제 차단 규칙은 유지한다. 모든 응답은 ApiResponse/no-store다.

| 메서드·경로 | 권한 | 응답/의미 |
|---|---|---|
| GET `/api/v2/admin/announcement-attachment-backfills/{runId}/segments/{segmentNo}/reservation-preview` | 활성 읽기3역할 | 최초/잔여/삭제 건수, runVersion/segmentHash/readinessCode/canReserve, 기존 batchId 또는 고정 소속의 batchPreview |
| POST 같은 prefix `/reservation` | 활성 ADMIN·CSRF·UUID Idempotency-Key | 201, 최초 분할과 새 SCOPE_READY batch의 일대일 예약 영수증. 수집 시작 아님 |
| GET `/api/v2/admin/announcement-attachment-backfills/{runId}/summary` | 활성 읽기3역할 | 전체 candidateCount/remainingItemCount/deletedItemCount, segmentCount/reservedSegmentCount, reservedRemainingItemCount/unreservedItemCount/unreservedInputChangedCount, collectionCounts/applicationCounts/rollbackCounts |

예약 body는 `{expectedRunVersion,expectedSegmentHash,expectedRemainingItemCount,expectedDeletedItemCount,reason}`다. 목록 버전0이상, 지문 소문자16진수64자리, 잔여1~1000/삭제0~999, 사유1~1000자다. sourceIds/URL/profile/batchId/force/승인 또는 성공값 override는400이다. 입력·정책·고정 소속·준비 상태 변경, 이미 연결된 분할, 다른 actor/payload의 키 재사용은409, 다른/없는 run·분할은404다.

readinessCode는 READY, ALREADY_RESERVED, ALL_ITEMS_DELETED, INPUT_CHANGED, POLICY_CHANGED, BATCH_NOT_READY다. READY만 예약 가능하며 BATCH_NOT_READY는 기존 batchPreview.items의 구체적인 사유를 확인한다. 이미 예약된 분할은 정책 퇴역 뒤에도 과거 batchId를 반환하고 자동 교체하지 않는다.

영수증 필드: runId/segmentNo/batchId, inventoryVersionAtReservation, originalItemCount/reservedItemCount/deletedBeforeReservation, segmentHash/currentBatchStatusCode/reservedAt. 최초 분모와 예약 전 삭제 수를 보존한다. reservedAt은 기록 시각이며 완료 시각이 아니다. 동일 키·입력 재요청은 최초 연결을 반환하며 현재 원문을 다시 예약하지 않는다. 응답 유실 시 입력을 보존하고 같은 키/본문으로 재확인한다.

분할 최초 수=예약 배치 최초 수+예약 전 삭제 수, 분할 현재 삭제 수=예약 전 삭제 수+배치 삭제 수다. 예약 이후 삭제된 batch의 최초 수집 시작은 기존처럼 거부한다. 취소/실패 배치는 연결을 보존하며 다른 키로 덮어쓰지 않는다. 재처리가 필요하면 남은 대상을 새 전체 범위로 명시적으로 고정·승인하고 원래 결과는 취소/실패/삭제로 대조한다.

summary는 전체 잔여 목록을 기준으로 세 차원을 독립 집계한다. 각 map의 합은 remainingItemCount이고 UNRESERVED는 미예약이다. 삭제는 별도 deletedItemCount이며 성공으로 가산하지 않는다. APPLIED 이력과 ROLLED_BACK 결과를 합산하지 않는다. 배치가 연결됐는데 job이 누락되거나 전체 분모가 다르면409이며 부분 집계를 성공으로 반환하지 않는다. summary에 전체 처리 완료를 뜻하는 boolean/status는 없다.

V75/상세 ledger 문서가 로컬 계약의 기준이다. 실제 PostgreSQL 경합/삭제/1001건 예약, 운영 승인 범위 실행·최종 대조와 역할별 브라우저 E2E는 필수 잔여다.

### 24.21 전체 목록·분할 관리 화면 연결

SSR `/app/admin/announcement-attachment-backfills`는24.19~24.20을 소비한다. ADMIN/OPERATOR/APPROVER 조회·no-store, ADMIN만 목록 고정/예약을 제공한다. 화면에서 정책 게시·ENFORCE 전환·수집 시작·판정 적용·원복을 자동 호출하지 않는다.

전체 후보 수와 분할 크기를 구분하고 삭제/미예약/입력 변경 및 수집·적용·원복 각각의 집계를 표시한다. 선택 분할 예약 후에는 반환된 batchId의 기존 배치 화면으로 이동해 수집/적용/복구를 별도 승인한다. batch의 고정 scope에 backfillRunId/backfillSegmentNo가 있을 때만 원래 전체 목록으로 돌아가는 링크를 표시한다.

목록/분할/항목 페이지와 비민감 ID를 URL로 탐색하며, 미저장 사유/요청 키는 탭 메모리에만 보관한다. 응답 유실 후 같은 키·본문 재확인, 후속 조회 실패 뒤 영수증 보존, 서버 건수·지문 대조와 초기 비활성/동의 해제를 적용한다. 상세 상태·검증·예외는 `announcement-attachment-backfill-ui-2026-09-12.md`를 따른다. Node 모의 API/SSR 시험을 실제 DB·운영 브라우저 완료로 계산하지 않는다.

### 24.22 배치 적용·원복 승인 전체 이력

GET `/api/v2/admin/announcement-attachment-batches/{batchId}/action-history`는 활성 ADMIN/OPERATOR/APPROVER 전용 읽기 API다. ApiResponse<History>/no-store, page기본1(1~2147483647), size기본20(1~100), throughVersion생략시 현재 배치 버전이다. 상한은0~현재 버전이고 미래 버전409, 잘못된 UUID/숫자/범위400, 없는 배치404, 비로그인401/권한403이다.

History는 batchId/throughVersion/currentBatchVersion/history(PageResponse)/currentHttpRequests=0이다. Entry는 actionId/batchId/actionKind/actionCode/acceptedFromVersion/previewId/scopeItemCount/approvedTargetCount/deletedCountAtAcceptance/approvedEligibleCount/approvedBaseReopenCount/approvedConfirmationRestoreCount/cancelledPendingCount/acceptedAt을 반환한다. APPLICATION의 START/PAUSE/RESUME는 previewId가 있고 원복 영향4필드는null이다. ROLLBACK의 START는 previewId=null이며 당시 적격/기본 경로 재개/이전 확인 복구/대기 취소 수를 반환한다. 승인 시각은 기록 시각이지 완료 시각이 아니다.

기존 V73의 두 불변 승인 테이블만 읽고 원문/job과 조인하지 않는다. 적용 approvedTargetCount는 당시 선택 수, 원복은 당시 대상 수다. 삭제 후에도 당시 수를 보존하며 현재 성공/실패/삭제 수는 기존 개별 application/rollback 영수증 GET으로 읽는다. actor/사유·지문/멱등 키/원문/URL/실행 snapshot은 목록에 없다.

REPEATABLE READ/readOnly에서 배치·count·페이지를 조회하고 승인 전 버전 `< throughVersion`을 적용한다. 정렬은 acceptedFromVersion DESC/actionKind DESC/actionId DESC다. 다음 페이지에 첫 응답 throughVersion을 전달하면 새 승인으로 기존 페이지가 밀리지 않는다. 최신 목록으로 갱신하려면 상한 없이1페이지를 다시 조회한다. 버전/소속/종류별 필드/합계가 모순되면409로 거부한다.

기존 배치 화면에 전체 승인 탐색과 정확한 영수증 연결을 추가했다. 사유/선택 보존, 상한·페이지 URL 복원, 배치 이동 시 이전 결과 해제, 유실된 변경 요청 중 조회 잠금을 유지한다. 일반 감사 로그 전체나 수집 제어 이력이 아니며 조회로 정책/worker/판정·운영 공고를 변경하지 않는다. 상세 계약과 실제 검증 경계는 `announcement-attachment-batch-history-2026-09-12.md`를 따른다.

### 24.23 첨부 정책 게시 전 영향 관측

GET `/api/v2/admin/announcement-attachment-policies/{policyId}/publication-impact`: 활성 ADMIN/OPERATOR/APPROVER의 읽기 전용 ApiResponse/no-store다. 정책 ID는UUID, 비로그인401/권한403/잘못된ID400/없는정책404/정책·QA·집계 모순409다. 이 경로의 POST는405이며 게시·QA 실행·원문 변경을 하지 않는다.

응답은 policy(Summary), activePolicyForRule(nullable Summary), matchingRule/allRules(Counts), maximumSourceBytes, wouldStopNewExternalRequests/wouldLiftGlobalOffStop, latestQa(nullable), blockingReasonCodes, requiresPublicationRevalidation=true, observedImpactHash/observedAt/currentHttpRequests=0이다. 두 범위는 포함 관계이며 합산하지 않는다. Counts의 source/job은 PRODUCTION, 계획은 PRODUCTION 수집 request만 포함한다.

Counts: boundSourceCount/reviewRequiredSourceCount/effectiveAttachmentSourceCount/linkedSourceCount, frozenCollectionJobCount/runningCollectionJobCount, applicationPendingJobCount/rollbackPendingJobCount/frozenCollectionPlanCount. 수집 job은 COLLECT/RETRY_FILES의 SCOPE_READY/PENDING/RUNNING/RETRY_WAIT/PAUSED다. RUNNING은 전송 중 HTTP 수가 아니며 FROZEN 계획은 누적 이력이다. 검수/적용/원복 차원도 중복 합산하지 않는다.

현재 ACTIVE 규칙의 OFF 정책은 다른 규칙의 고정 작업까지 새 외부 요청을 막으므로 matchingRule과 allRules를 분리한다. 두 would 필드는 그 상태로 게시한다고 가정한 중지/중지조건 해제 영향이며 실행·worker 활성화·profile 허용을 뜻하지 않는다. 공고별 bytes 상한을 전체 다운로드 예측으로 곱하지 않는다.

latestQa는 runId/statusCode/rowVersion/policyVersion/ruleVersion/inputVersionsCurrent/snapshotHash/steps(stepCode/statusCode/evidenceHash)/completedAt만 반환한다. source ID/원문/URL/actor/사유/key/QA evidence·input JSON은 없다. 최신 실패·미완료를 숨기고 과거 성공을 선택하지 않는다. 단계가 없으면NOT_RUN, 모순·중복이면409다.

차단 코드: POLICY_NOT_DRAFT, KEYWORD_RULE_NOT_ACTIVE, QA_NOT_REQUESTED, QA_NOT_VERIFIED, QA_INPUT_VERSIONS_CHANGED, QA_REQUIRED_STEPS_NOT_PASSED, PUBLICATION_REVALIDATION_REQUIRED. 마지막은 항상 포함하며4단계 PASSED metadata도 최종 게시 검증·동의를 대신하지 않는다. canPublish/승인 토큰은 반환하지 않는다.

observedImpactHash는 시각을 제외한 관측 metadata의 지문이다. source ID/개별 버전 전체를 고정하지 않으므로 같은 건수의 대상 교체는 감지하지 못할 수 있다. 정확한 게시 scope hash/CAS로 사용할 수 없으며 최종 QA/런타임·승인 대상/영향 재검증은 후속 게시 transaction의 필수 조건이다. 상세 집계·검증 계약은 `announcement-attachment-policy-publication-impact-2026-09-12.md`를 따른다.

### 24.24 첨부 정책 초안·QA 관리 화면

`/app/admin/announcement-attachment-policies`는 ADMIN/OPERATOR/APPROVER용 Thymeleaf/no-store 화면이다. 기존 policies·validation-runs·publication-impact API와 v1 키워드 규칙 목록 GET을 사용하며 기존 응답/DB 계약을 변경하지 않는다. ADMIN만 초안 생성/수정/개정·QA 예약/취소를 준비하고 실행할 수 있다. 권한·CSRF·버전·사유 검증은 기존 서버 API가 최종 책임진다.

상태 필터/페이지/정책/QA ID는 URL로 탐색한다. 초안 저장은 운영 모드 전환이 아니다. 정책 게시 준비·실행·영수증은 후속 게시 계약과 UI에 연결됐으나 전체 실제 QA 검증 전에는 게시할 수 없다. QA 단계 PASSED/MISSING/NOT_RUN/FAILED와 실행 INCOMPLETE/VERIFIED를 구별하고 전체 검증 부족·게시 재검증을 명시한다. 관측 범위/원문·작업·누적 계획의 집계는 더하지 않는다.

생성/개정/QA 예약 응답 유실은 원래 키·본문으로 재확인한다. 수정/취소는 expectedVersion 기반이며 유실 후409를 최초 실패로 단정하지 않는다. 정확한 현재 정책/실행을 별도로 조회하고 동의 후 새 검토 기준으로만 채택할 수 있다. 과거 요청의 성공 여부를 확정하지 않으며 입력을 보존한다. 확인한 응답은 후속 조회 실패로 지우지 않는다. 상세 상태·합성 브라우저/실제 운영 검증 경계는 `announcement-attachment-policy-ui-2026-09-12.md`를 따른다.

### 24.25 첨부 정책 게시 준비 범위 — V76

`/api/v2/admin/announcement-attachment-policies/{policyId}/publication-scopes`에 POST 준비와 GET 이력, `/{scopeId}` 상세, `/{scopeId}/items` 항목 페이지를 추가한다. POST는 활성 ADMIN·비밀번호 변경 완료·CSRF·UUID Idempotency-Key, 읽기는 활성 ADMIN/OPERATOR/APPROVER다. ApiResponse/PageResponse와 no-store를 유지한다.

준비 입력은 expectedVersion(0 이상), reason(공백 아닌 1~1000자)만 허용한다. sourceIds/maximumCount/scopeHash/observedImpactHash/QA 성공 등 추가 입력은400이다. DB에서 전체 POLICY/SOURCE/JOB/COLLECTION_PLAN/COLLECTOR의 ID와 상태 hash를 고정한다. 항목 페이지 size1~100은 조회용이며 전체 고정 범위를 줄이지 않는다.

POST201/GET상세의 data는 scope(Summary), isExpired, isScopeCurrent, isApproval=false, requiresPublicationRevalidation=true, currentHttpRequests=0이다. Summary는 scopeId/policyId/policyVersion/ruleReleaseId/ruleVersion/modeCode/qaRunId/qaSnapshotHash/itemCount/scopeHash/createdAt/expiresAt이다. 항목은 entityTypeCode/entityId/stateHash만 반환한다. actor·요청 키·사유/요청 hash·원문·URL은 공개 DTO에 없다.

10분 유효기간이며 만료 후에도 원래 이력은 보존한다. 동일 key·actor·정책·입력은 원래 scope를 반환하고 새로운 범위로 자동 재고정하지 않는다. 다른 입력의 key 재사용409, 소속/미존재404, 버전/저장 계약 모순409다. 현재성은 조회 snapshot의 DB 범위 일치만 의미하며 QA/설치/전체 profile 최신성이나 게시 승인이 아니다. itemCount는 여러 종류 원장의 행 수이지 공고 수/HTTP 수가 아니다.

준비 원장은 정책/worker/원문/운영 공고를 변경하지 않는다. 후속 실제 게시 서버 경로는24.26이며 전체 QA와 게시 UI/운영 검증은 미완료다. 준비 계약은 `announcement-attachment-policy-publication-scope-2026-09-12.md`를 따른다.

### 24.26 첨부 정책 게시·불변 영수증 — V77

POST `/api/v2/admin/announcement-attachment-policies/{policyId}/publication`: 활성 ADMIN·비밀번호 변경 완료·CSRF·UUID Idempotency-Key가 필요하다. 입력은 scopeId, scopeHash, expectedVersion, acknowledgeNewCollectionBehavior=true, acknowledgeExistingJobsUnchanged=true, acknowledgeNoBackfill=true, reason(1~1000자)다. 임의 QA 성공/설정/모드/hash를 입력하지 않는다. scope는 현재 관리자가 준비한 원장이며 동의 항목은 신규 수집 조건·기존 고정 작업·별도 기존 데이터 승인 배치를 구분한다.

설치·전체 애플리케이션 코드 지문 읽기 → 짧은 읽기 transaction에서 현재 입력/QA 준비 → transaction 밖 네 단계 근거 검증·설치/코드 지문 재확인 → READ COMMITTED/15초의 관련 테이블 EXCLUSIVE NOWAIT(V77 기존18개와 V81 Provider 원장/항목/계획3개) → 현재 범위/정책/최신 QA/단계/입력과 검증 당시 값의 일치 재확인 → 영수증/이전 ACTIVE 퇴역/새 정책 ACTIVE/감사 원자적 저장 순서다. 파일 읽기/QA CPU 검증은 쓰기 잠금 안에서 하지 않는다. 일반 SELECT는 허용하며 진행 중인 writer나 row-lock 업무가 있으면409로 반환한다. 자동 대기/재게시하지 않는다. 잠금 밖 검증 중 같은 요청이 먼저 게시됐다면 입력 지문을 확인하고 원래 영수증을 반환한다.

게시할 policyHash는 검증된 QA snapshot hash다. 초안 설정의 null extractorConfigHash는 QA의 installed.runtimeHash로만 고정한다. 그 외 설정·모드·규칙·profile은 변경하지 않는다. 원문/기존 job/운영 공고를 일괄 변경하지 않고 worker flag도 켜지 않는다.

POST201/GET200의 data는 publication(영수증), existingDataApplied=false, workerEnabledByRequest=false, currentHttpRequests=0이다. 영수증 필드는 publicationId/policyId/publishedPolicyVersion/policyHash/previousPolicyId/previousPolicyVersion(교체 직전)/scopeId/scopeHash/qaRunId/modeCode/publishedAt이다. GET 같은 경로는 ADMIN/OPERATOR/APPROVER의 정책별 단건 이력이며 이후 퇴역해도 당시 결과를 반환한다. 동일 게시 키/actor/정책/입력은 준비 만료 이후에도 같은 영수증이며, 다른 입력 재사용은409다. ApiResponse/no-store를 유지한다.

WORKER_DB_RECOVERY 실행·근거 검증기는 연결됐고 Linux35050057694에서 실제 부모 연결·취소·정리를 확인했다. PROVIDER_PROFILES 실행·근거 검증기도24.30~24.31 및 정책 validation 경로에 연결됐으나 전체 공식 표본·고정 기대값·실행 원장이 부족하여 전체 QA→게시 성공은 미완료다. 네 단계 PASSED metadata나 과거 CLI를 성공으로 변환하지 않는다. 분류 정답을 현재 규칙으로 재계산하고 설치 runtime 결과는 현재12개 fixture와 소속 시각/지문/정리 여부를 대조한다. 관리자 게시 준비·실행·영수증 UI와 실제 PG 계약 시험은 존재하며 승인된 운영 게시·업무 E2E가 남는다. 서버 계약은 `announcement-attachment-policy-publication-2026-09-12.md`, 화면 계약과 합성 검증 경계는 `announcement-attachment-policy-publication-ui-2026-09-12.md`를 따른다.

09-12 정책 QA 내부 snapshot schema5: 수집원 publicCode·전체 코드 지문·독립 QA artifact/전체 suite/case/추출기 지문과 전체 Provider 요구 목록(providerQaPlan)을 고정한다. 설치 경로는 `SANEB_ANNOUNCEMENT_ATTACHMENT_CONTRACT_QA_ROOT`, 기본 `/opt/saneb/attachment-contract-qa`다. 성공 JSON 업로드 기능은 없다. 기존 schema1~4 이력은 재작성하지 않으며 현재 입력과 다른 과거 QA는 게시 근거로 재사용하지 않는다. 실행은 기존8분 lease 안에서60초 저장/정리 여유를 남기고 취소/슬롯을 반복 확인한다. 상세는 `announcement-attachment-policy-qa-bridge-2026-09-12.md`다.

### 전체 Provider QA 요구 목록 조회 — 2026-09-12

GET `/api/v2/admin/announcement-attachment-policies/{policyId}/provider-qa-plan?page=1&size=20`: 활성 ADMIN/OPERATOR/APPROVER 전용 읽기이며 no-store·ApiResponse를 사용한다. data는 planHash, isPolicyManifestCurrent, summary, targets(PageResponse)다. size1~100이며 runtime/QA worker 활성화 없이 현재 DB 수집원과 시스템 프로필 결합을 조회한다. 외부 HTTP/QA 예약/정책 변경은0이다.

기업마당·정부24·모든 활성 미삭제 지자체를 포함한다. SYSTEM_BINDING_MATCHED/PROFILE_MISSING/LIST_PARSER_MISMATCH/PROFILE_AMBIGUOUS를 구분하며 결합된 기관도 실제 QA 통과로 표시하지 않는다. targets의 sourceId는 `local_government_notice_sources.id`이며 수집된 `announcement_sources.id`가 아니다. URL·설정 JSON 원문·비밀값은 반환하지 않는다. 최소 정상 공고 수와 PDF/HWP/HWPX는 요구량이며 발견·추출 성공 수가 아니다. isExecutionPlanComplete/isQaPassed/isSingleLeaseCoverageGuaranteed는 false, currentHttpRequests는0이다. planHash는 요구 목록만의 지문이며 전체 정책 snapshotHash/실파일 현재성을 대신하지 않는다. 상세는 `announcement-attachment-provider-qa-scope-2026-09-12.md`다.

### 24.27 첨부 정부24 출처 코드 경계 — V78

2026-09-15 [공식 목록 API 계약 보완](announcement-gov24-official-list-contract-2026-09-15.md)은 외부 `api.odcloud.kr` 어댑터의 페이지/검색 규격을 정합화한다. 공식 목록에서 지원하지 않는 지역·내부 카테고리·신청기간은 구체적인 오류로 거부하며, 새 정부24 자동 배치 요청의 `startDate/endDate`는 null이다. 이미 승인된 요청은 변경하지 않는다. 기존 중계 API·Controller/DTO·출처 별칭·첨부 profile 미지원 계약은 유지한다. 실제 API/첨부/운영 성공은 별도 검증 대상이다.

기존 `/api/v1` 및 단건 첨부 API의 실제 출처는 `GOV24_PUBLIC_SERVICE`다. 배치·전체 목록의 `providerCodes` 필터는 기존 계약인 `GOV24` 별칭을 유지한다. 이는 서로 다른 수집 채널이 아니다.

| 위치 | 정부24 코드 |
|---|---|
| 배치/전체 목록 입력·정규화된 scope·범위 counts | GOV24 |
| 원문·후보 items·고정 항목·작업 providerCode | GOV24_PUBLIC_SERVICE |
| 시스템 profile 바인딩·QA 대상 snapshot | GOV24_PUBLIC_SERVICE |

서버는 Mapper 필터 바인딩에서 별칭을 실제 코드로 변환하며, 범위 counts의 출력만 별칭으로 맞춘다. 고정 분할 소속 비교도 이 경계를 따른다. 원문 ID·내용·Provider 자체는 변환하지 않는다. 기존 요청의 정규화/멱등 키 지문 형식과 최초 결과 재조회는 유지한다. 수정 전 정부24 0건 미리보기는 최신 실제 후보 지문으로 다시 확인해야 하며, 이미 고정된 이력에 대상을 추가하지 않는다.

시스템 첨부 profile이 없는 정부24 원문은 0건으로 숨기지 않고 `PROFILE_REQUIRED` 후보로 반환하며 예약을 차단한다. 관리자 화면은 실제 코드와 과거 이력 별칭을 모두 `정부24`로 표시하되 profile 입력을 허용하거나 준비 성공을 추정하지 않는다. V78은 두 첨부 CHECK 제약에 실제 코드를 추가하고 이전 이력은 수정하지 않는다. 정부24 실제 API/첨부 지원 및 운영 성공은 별도 미완료다. 상세는 `announcement-attachment-gov24-provider-contract-2026-09-12.md`를 따른다.

### 24.28 Provider QA 내부 실행 계약 — HTTP 예약 API 미제공

V79 원장과 `AnnouncementAttachmentProviderQaExecutionService.saveCase`는 시스템 소유 고정 입력을 실행하는 내부 계약이다. 기존 `/api/v1`과 공개 v2 계약은 변경하지 않는다. 새 Controller/예약·조회 API/scheduler는 아직 연결하지 않았으며 관리자 URL·파일·성공 JSON 업로드를 허용하지 않는다.

- 기본 설정 `SANEB_ANNOUNCEMENT_ATTACHMENT_PROVIDER_QA_ENABLED=false`. OFF에서는 원장 조회·코드 지문 계산·HTTP·추출을 실행하지 않는다. ON만으로 작업이 예약되거나 정책이 활성화되지 않는다.
- PENDING 항목의 입력/profile/runtime/전체 코드 지문과 DB 정책/규칙 버전을 대조하고, 짧은 transaction에서 부모→항목 순서로 소유권을 획득한다. 기존 RUNNING/terminal 항목은 다시 실행하지 않는다.
- 실제 HTTP/격리 추출은 transaction 밖에서 수행한다. 실행 중 소유권 확인, 공유 자원 임대/반환, 요청·byte 예약만 짧은 transaction을 사용한다. 전역 슬롯 부족·호스트 충돌은 무제한 재시도로 숨기지 않는다.
- 원본 정리 후 실제 결과와 DB 누적 예산을 재대조한다. 취소/입력 변경/lease 유실이면 성공으로 확정하지 않는다. 자원 반환은 해당 case/소유 token/permit만 대상으로 한다. 응답 유실 후 같은 항목을 자동 재실행하지 않는다.
- 결과는 고정 공고 단위 검증이며 `isPolicyQaPassed=false`다. 전체 공개 표본 catalog·분할 예약/취소·조회와 전체 정책 QA 연계는 별도 필수 작업이다. API나 운영 UI에서 사용할 최종 실행 기능이 완료됐다는 뜻이 아니다.

내부 Service35건·실행기42건·Mapper/DDL 표적 검증은 통과했다. 실제 PostgreSQL·Linux·운영 실행은 미검증이다. DB11.24 및 `announcement-attachment-provider-qa-ledger-2026-09-12.md`에 범위와 잔여 검증을 기록한다.

### 24.29 Provider QA catalog 계획 — 내부 snapshot6

고정 classpath catalog와 현재 전체 Provider 요구 범위·규칙/runtime을 대조해 전체 target/case 상태와 실행 가능 입력·분할 상한을 계산한다. 이 단계에 새 공개 HTTP 예약 API는 없으며 기존 v1/v2 응답 shape는 변경하지 않는다. 관리자 URL/파일/정답 업로드를 받지 않는다.

기존 정책 QA 예약의 서버 snapshot schema6에는 `providerQaCatalog`의 catalogVersion/catalogHash/scopeHash, 전체 target/case metadata와 분할별 case code·요청/byte/시간 상한을 추가한다. 원문 URL·제목·기대 문구·Prepared.inputs는 포함하지 않는다. catalog 또는 준비 상태가 바뀌면 기존 snapshot과 달라 이전 QA를 재사용할 수 없다. 실제 Provider 전체 QA는 계속 MISSING이다.

REFERENCE_ONLY·TARGET_OUTSIDE_SCOPE·TARGET_BINDING_UNAVAILABLE·PROFILE_CHANGED·SOURCE_BINDING_INVALID·OBSERVATION_EXPIRED·EXPECTATION_INVALID·DUPLICATE_DETAIL·TITLE_EXPECTATION_CHANGED를 실행 가능한 EXPECTED_INPUT_READY와 구분한다. 후자도 실제 실행/추출 성공은 아니다. 정상3공고와 PDF/HWP/HWPX 기대값 coverage, 전체 scope/분할은 보존하며 일부 표본으로 전체 준비/성공을 표시하지 않는다.

공식 참조는2026-09-15 원주·제천 추가 후15건/실행 기대값0이다. 참조 확장은 API shape 또는 운영 QA 성공을 뜻하지 않는다. 실제 표본/전체 정책 검증의 최신 경계는 `announcement-attachment-provider-qa-catalog-2026-09-12.md`와 장기 진행 기록을 따른다.

### 24.30 Provider QA 계획·예약·조회·취소 — V80

새 기준 경로는 `/api/v2/admin/announcement-attachment-policies/{policyId}/provider-qa-runs`다. 기존 v1/구조적 provider-qa-plan/정책 validation-runs 계약은 보존한다. 모든 응답은 ApiResponse이며 목록은 PageResponse(items/page/size/totalCount/totalPages), no-store다. 페이지는1 이상/크기1~100이다.

| 메서드·하위 경로 | 역할 | 요청·결과 |
|---|---|---|
| GET `/execution-plan` | ADMIN/OPERATOR/APPROVER | page/size. 현재 policyVersion/snapshotHash/catalogHash/planHash, 전체 대상/catalog/실행 가능 수·기대 coverage와 분할 페이지. 설치 검증 불가409. 쓰기/HTTP 없음 |
| GET `/execution-plan/targets` | ADMIN/OPERATOR/APPROVER | page/size. 동일 snapshot/catalog/plan 지문, 전체 기대 coverage·isQaPassed=false, formatCoverage 및 PageResponse<TargetPlan>. 기관별 정상3건/정상 다중 첨부 수·기대 제공/미관측/누락 형식을 구분. 설치 검증 불가409. 쓰기/HTTP/예약 없음 |
| POST 기본 경로 | ADMIN | UUID Idempotency-Key, expectedVersion/expectedSnapshotHash/expectedCatalogHash/expectedPlanHash, segmentNo/expectedCaseCount/maximumRequests/maximumBytes/maximumSecondsIncludingMargin, acknowledgeScope/acknowledgeNetworkBudget/acknowledgeIncompleteCoverage, reason. 정확한 계획 대조 후202/원장 metadata |
| GET 기본 경로·`/{runId}`·`/{runId}/cases` | ADMIN/OPERATOR/APPROVER | 정책 소속 검사와 실행/항목 페이지. OFF에서도 조회 가능. 실제 DB 버전 현재성은 코드/운영 실행 성공을 뜻하지 않음 |
| PUT `/{runId}/cancellation` | ADMIN | expectedVersion/reason, CSRF. READY/RUNNING만 취소. 미실행 항목은 취소로 남기고 실행 중 소유자는 스스로 정리. OFF에서도 허용 |

모든 관리 계정은 활성·필수 비밀번호 변경 완료 상태여야 한다. 임의 URL·파일 경로·source/profile/실행 옵션·성공 결과 입력은400, 다른 입력의 같은 멱등 키·현재성/정확한 예산 불일치는409다. scope/network 확인은true 필수이고 전체 기대값이 미완료이면 별도 인지 확인도true여야 한다. 이 확인은 일부 QA 분할 승인이지 전체 정책 게시/ENFORCE/기존 데이터 적용 승인이 아니다.

원장/항목 응답에는 idempotency key·lease token·actor·requestHash·원문/추출문·증거 JSON을 노출하지 않는다. 과거 계획 없는 이력의 plan 필드는null이다. `isQaPassed`는 항상false이며 COMPLETED는 해당 분할 기대 동작 일치일 뿐이다. 새 예약이 OFF거나 실행 가능 분할이 없으면 isReservationEnabled=false다.

서버의 기본 OFF 스케줄러는 현재 설치·전체 계획·저장된 분할/항목을 재대조하고 기존 실제 ExecutionService에 한 공고씩 연결한다. 전체 verifier·격리 Linux/PG 계약과 예약/취소 관리 UI는 후속 구현·검증됐으나 전체 공식 기대값·운영 실행은 잔여다. 상세는 DB11.26 및 `announcement-attachment-provider-qa-management-2026-09-12.md`를 따른다.

2026-09-15 catalog schema2 적용성은 `FIXED_SAMPLE_FORMATS_V2`다. 전체 필수 PDF/HWP/HWPX와 전체 누락 형식을 formatCoverage로, 대상별 기대 제공 형식·미관측 형식·정상 다중 첨부 수를 targets.items[].formatApplicability로 조회한다. `EXPECTATIONS_UNKNOWN`은 유효한 기대 파일 부재, `FIXED_SAMPLE_EXPECTATIONS`는 고정 기대 표본의 범위이며 실제 제공 여부/QA 통과 판정이 아니다. 미관측을 미지원/N/A로 표현하지 않는다. 발견된 OCR/부분 등 실패 형식은 해당 대상의 누락 분모에 남는다. 기존 schema1/구조적 provider-qa-plan/execution-plan/v1 shape는 변경하지 않으며 현재 참조15/실행 기대값0이다. 상세는 `announcement-provider-format-applicability-v2-2026-09-15.md`를 따른다.

2026-09-15 관리자 읽기 전용 화면 `/app/admin/announcement-attachment-provider-coverage?policyId={UUID}&page={n}`을 추가했다. 정책 상세에서 진입하며24.30의 `/execution-plan/targets`만 size20으로 GET한다. 정책 ID·버전·지문·형식·정확한 페이지 분모를 대조하고 페이지 간 계획 변경을 차단한다. 실제 QA/예약/게시/기존 데이터 적용을 실행하지 않으며 기대 범위를 통과 근거로 표시하지 않는다. 상세는 `announcement-provider-coverage-ui-2026-09-15.md`다.

2026-09-15 예약·이력·취소 화면 `/app/admin/announcement-attachment-provider-qa?policyId={UUID}`를 별도로 연결했다. 선택 실행과 페이지는 runId/planPage/runPage/casePage로 보존한다. ADMIN은 현재 서버 계획의 분할·전체 코드·요청/바이트/시간 예산 및 미완료 범위를 별도 확인한 뒤 기존 POST/PUT을 사용한다. OPERATOR/APPROVER는 조회만 가능하다. 예약 응답 유실은 같은 Idempotency-Key·본문으로 재확인하고 이후401/403/409도 기존 미확정을 해제하지 않는다. 취소는 실행 rowVersion과 사유를 사용하며 CANCEL_REQUESTED/취소 종료를 구분한다. 계획409/OFF와 이력/취소를 분리하고 게시·ENFORCE·기존 데이터 적용 API는 호출하지 않는다. DB/API shape 변경은 없으며 합성/운영 검증 경계는 `announcement-provider-qa-ui-2026-09-15.md`를 따른다.

### 24.31 전체 Provider QA 근거 검증 — 내부 계약

기존 HTTP API·v1/v2 응답은 변경하지 않는다. 관리자 URL·파일·성공 JSON 제출 API도 추가하지 않는다. 내부 `AttachmentProviderQaEvidenceGate`는 현재 snapshot6/전체 catalog와 DB 분할별 최신 시도·모든 case의 파일별 근거를 대조해 PASSED/MISSING/FAILED/CANCELLED 및 고정 사유 코드를 반환한다. 여기서 PASSED는 해당 전체 Provider 실행 근거의 검증 결과이며 정책 전체 VERIFIED/게시 완료가 아니다.

2026-09-15 내부 파일 기대값에 선택적 `roleExpectation`(규칙 버전/지문, 역할/사유, textHash/blocksHash/assessmentHash), 실행 결과에 선택적 `roleAssessmentHash`를 추가한다. 기존 누락 필드의 직렬화/hash는 보존하지만 새 catalog의 COMPLETE_TEXT는 역할 기대값 필수다. 실제 추출의 역할·사유·텍스트·전체 위치 근거가 모두 일치해야 하며 저장된 결과도 같은 고정 입력/실제 textHash에 재결합한다. UNKNOWN 음성 사례와 양식/참고자료만 있는 공고는 정상3공고 요구량을 채우지 않는다. 원장 조회 HTTP 응답에 내부 기대값·증거 JSON·원문을 추가하지 않으며 공개 계약 변경과 DDL은 없다.

2026-09-22 구간 엔진은 내부 실행 입력에 `engineVersion`, COMPLETE_TEXT 파일에 `segmentExpectation`(analysisVersion/rulesHash/textHash/blocksHash/analysisHash/statusCode/순서 고정 roleCodes)을 요구한다. 실제 추출 결과의 전체 구간 위치·역할·근거 hash를 대조하고 결과에는 `segmentAnalysisHash`만 추가한다. 설정·catalog·입력·저장 결과·정책 게시 재검증이 같은 엔진에 결합된다. 기존 엔진의 누락 필드는 직렬화하지 않아 기존 JSON/hash를 보존한다. 새 엔진의 정상 표본은 모든 파일 COMPLETE_TEXT/모든 구간 RESOLVED이며 NOTICE/GUIDE가 하나 이상 있어야 한다. 파일 전체 UNKNOWN이어도 NOTICE/FORM 구간이 모두 확인되면 정상 표본에 포함할 수 있으나 UNKNOWN 구간·부분/실패 파일·양식만 있는 공고는 포함하지 않는다. 정상 표본은 정책 QA 수량의 의미이며 사용자 노출·최종 승인·자동 활성화가 아니다. 기대값은 검토 없이 관측 결과에서 자동 작성하지 않는다.

전체 기대값 부재·분할 누락·최신 시도 미완료는 MISSING, 지문/순번/분모/시각/파일·예산 불일치 또는 DB 조회 실패는 FAILED, 소유 실행 중단은 CANCELLED다. 실패 원문/SQL/URL/파일 텍스트를 사유에 복사하지 않는다. 실제 실패/부분 품질을 정상 문서로 바꾸지 않으며 미실행 파일을 분모에서 제외하지 않는다.

2026-09-14 정책 QA worker/게시 추가 검증기를 이 집계에 연결했다. PROVIDER_PROFILES의 실제 판정이 PASSED이고 나머지 세 단계도 저장된 PASSED일 때 VERIFIED, 누락 시 INCOMPLETE, 근거 불일치 시 FAILED, 취소 시 CANCELLED다. 기존 HTTP shape/v1은 보존한다. 정책 QA 완료는 자동 게시·ENFORCE·기존 데이터 적용을 뜻하지 않는다.

게시 API는 잠금 밖 전체 재검증 후 V81 게시 잠금 안에서 최신 Provider 분할 시도도 재확인한다. 변경되면409와 재확인 안내를 반환하며 정책·영수증 쓰기를 하지 않는다. 같은 키로 이미 완료한 게시 요청의 재조회는 보존한다. 실제 Linux/PG/전체 Provider 실행, 형식 적용성 계약·표본 기대값/관리자 QA 화면은 필수 잔여다. DB11.27~11.28 및 `announcement-attachment-provider-qa-evidence-2026-09-12.md`를 따른다.

### 24.32 3단계 자동 분석 흐름 — v2 additive 조회

`GET /api/v2/admin/announcement-sources`의 items, `GET /{sourceId}`의 source, `GET /{sourceId}/attachment-classification`의 Summary에 `processingFlow`를 추가한다. 기존 ApiResponse/PageResponse·no-store·조회 역할·v1·base/effective/preview·판정 코드·태그·필터 의미는 보존한다. 자동 분석 완료와 관리자 확인은 별개다.

```json
{"processingFlow":{"statusCode":"AUTOMATIC_PROCESSING","isAutomaticAnalysisComplete":false,"isFinalReviewAvailable":false}}
```

- 상태 코드는 NOT_APPLIED, CLASSIFICATION_PENDING, CONFIGURATION_REQUIRED, AUTOMATIC_PROCESSING, EVIDENCE_STALE, TECHNICAL_EXCEPTION, READY_FOR_FINAL_REVIEW, FINAL_REVIEW_EXCEPTION, FINAL_REVIEW_CONFIRMED다. 의미·진행 계획은 [3단계 설계](announcement-three-stage-filtering-workflow-2026-09-14.md)를 따른다.
- 정상 완료는 현재 SEALED 평가/파일 수/발견 완료·정상 상태/알려진 판정 이유/종료 작업으로 확인한다. UNKNOWN 문서 역할·A/B·문맥 쟁점은 분석 종료 후에도 최종 예외 검증 대상이다. 추출 실패·부분/스캔·누락은 기술 미완료다. 수동 원문 확인이 저장되어도 자동 분석 완료로 바꾸지 않는다.
- `isFinalReviewAvailable`은 입력 안내용 조회 힌트이며 권한·전환 승인이 아니다. 일반 작업의 SCOPE_READY/PENDING/RUNNING/RETRY_WAIT/PAUSED가 하나라도 있으면 false다. 배치 미리보기는 기존 현재 판정 검수를 막지 않는다. 기존 source lock/CAS/currentness/필수 확인/서버 권한 검증은 쓰기 시 재실행한다.
- 기술 예외라도 현재 봉인 근거가 있고 활성 일반 작업이 없으면 기존 명시적 수동 원문 확인 절차를 요청할 수 있다. 근거 결합·작업/판정 상태가 미확인이면 잠근다. 확인 완료를 ACTIVE·최종 선정·자동 처리 성공으로 해석하지 않는다.
- 상세 화면은 대기/진행 상태에서 review-context를 요청하지 않으며 판정 이력을 중간 근거로 표시한다. 목록 처리 흐름 필터는 아래24.33을 따른다. 기존 effectiveStatusCode의 REVIEW_REQUIRED 건수에는 대기/예외도 포함되므로 최종 검증 대기 건수로 쓰면 안 된다.

이 단계에는 새로운 DDL·운영 적용·외부 수집 실행이 없다. 단위/HTTP/Node 결과와 실제 PostgreSQL/운영 브라우저 결과는 진행 기록에서 구분한다.

### 24.33 3단계 처리 상태별 검증 대기열 — v2 additive 필터

`GET /api/v2/admin/announcement-sources`에 선택 query `processingFlowStatusCode`를 추가한다. 허용값은24.32의9개 코드이며 공백은 필터 없음, 미지 값은 허용 목록이 포함된400이다. 기존 provider/effectiveStatus/job/태그/검색어/기간/page/size 조건과 AND 결합한다. 미지 값을 무시하거나 전체 조회로 바꾸지 않는다.

예: `?processingFlowStatusCode=READY_FOR_FINAL_REVIEW&providerCode=BIZINFO&page=1&size=20`.

- 기본 API 호출은 필터 없음으로 기존 목록 의미를 보존한다. Java의 기존10인자 조회 조건 생성자도 유지한다.
- ApiResponse<PageResponse<Summary>>·no-store·ADMIN/OPERATOR/APPROVER 읽기 권한은 변경하지 않는다. totalCount/totalPages는 같은 REPEATABLE_READ와 같은 SQL CASE·SearchWhere로 LIMIT 이전 전체 검색 범위를 계산한다.
- 현재 평가/봉인 set/정책/확인 복구 결합과 전체 활성 일반 작업 검사를 그대로 사용한다. 준비 완료 조건에는 대기·이전 근거·기술 미완료가 포함되지 않는다. 관리자 확인 완료는 자동 분석 완전성 boolean과 별개다.
- `/app/admin/announcement-attachment-queue`는 해당 API의 읽기 전용 화면이며 기본 선택은 최종 검증 대기다. 전체/9개 상태·수집처·제목/기관명 검색과 페이지를 URL에 유지한다. 기존 v1 제목·본문 목록을 대체하거나 기존 집계 의미를 변경하지 않는다.
- 화면은 이전 비동기 응답/오류가 새 필터를 덮어쓰지 못하게 하고 서버 건수·페이지·행별 상태가 모순되면 오류로 분리한다. 401/403/시간 초과/계약 불일치를0건으로 표시하지 않는다. 원문 body/첨부 text를 목록에서 요청하지 않으며 조회만으로 재수집·분류·확인·DRAFT·정책 적용을 실행하지 않는다.

DDL/migration·v1·운영 쓰기 없음. 실제 PostgreSQL/운영 브라우저 검증 여부는 진행 기록을 최종 근거로 한다.

### 24.34 텍스트 역할 규칙·고정 근거 — v2 additive

기존 v1은 변경하지 않는다. v2 `GET /admin/announcement-sources/{sourceId}/attachment-sets/{setId}/files`의 FileSummary에 nullable `roleExtractionId`, `roleAssessment`를 추가한다. 전체 실제 경로는 `/api/v2/admin/...`이며 기존 ApiResponse/PageResponse·no-store·읽기 권한·페이지 계약을 유지한다.

- `roleOriginCode`: 기존 UNKNOWN/PROFILE/MANUAL에 TEXT_RULE을 추가한다. MANUAL은 현재 관리자 지정값이며 자동 제안의 역할과 다를 수 있다.
- `roleAssessment`: `{ruleVersion,rulesHash,textHash,blocksHash,roleCode,reasonCode,evidence:[{ruleCode,blockIndex,startOffset,endOffset}]}`. 원문/파일 URL/파일 경로는 없다. 위치는 추출 전체의 Unicode 코드포인트·0부터·끝 제외다. 기존 blocks 조회에 `blockIndex+1` 페이지를 전달해 동일 추출 근거를 확인한다.
- 과거/불완전/미적용 파일의 null 근거를 성공으로 보충하지 않는다. TEXT_RULE 결과는 정확한 COMPLETE_TEXT 추출과 고정 규칙에 연결된다. 관리자 수정 후에도 자동 제안은 보존하되 현재 역할과 구분한다. 잘못된 필드/지문/위치 구조는 임의 JSON으로 반환하지 않는다.
- 정책 Details.configuration에 선택 `roleRuleVersion`, `roleRulesHash`를 쌍으로 추가한다. 새 초안 생성/수정 시 서버의 `document-role-1.0.2`와 고정 규칙 지문을 저장한다. 양식의 콜론 없는 입력 표제/괄호형 서명 보완이며 [규칙 증분](announcement-form-role-markers-2026-09-15.md)을 따른다. 기존1.0.1·값이 없는 정책·복사 초안은 자동으로 새 역할 규칙을 적용하지 않으며, 수정·QA·게시 절차가 필요하다. 요청 body에 임의 역할 정규식이나 규칙 지문을 받지 않는다.
- worker는 현재 고정 규칙의 UNKNOWN/UNKNOWN·완전 추출 파일에만 적용한다. 실패 파일의 재시도와 checkpoint도 같은 버전·근거를 검증한다. MANUAL/PROFILE은 덮어쓰지 않는다. 역할이 UNKNOWN이면 미확정 사유를 그대로 남긴다.
- 관리자 상세는 역할 출처/현재 역할/자동 제안/사유/버전, 펼침 영역의 지문·근거 문단 링크를 한글로 표시한다. 기존 native details·텍스트 렌더링·문단 강조를 재사용한다. 연결 불일치는 오류로 표시하고 강조하지 않는다. 정책 화면은 규칙 미연결과 새 규칙 연결을 구분하고, 조회만으로 게시/재처리를 실행하지 않는다.

UI 적용 기준: ADMIN/OPERATOR/APPROVER가 근거를 읽는 R0 조회이며, 기존 관리자 역할 변경/게시의 권한·확인·사유·CAS는 그대로다. 실제 검수 비용은 잘못된 역할을 확정할 때 높으므로 미확정/오류/과거 제안을 명시한다. 작은 화면의 기존 메타데이터 줄바꿈·키보드 native controls를 유지하고 새 라이브러리·모션은 추가하지 않는다. 수용 기준은 같은 추출의 근거 조회, 수동 역할 보존, legacy null 표시, 자동 승인 오해 방지다. Node 계약 검증과 실제 운영 브라우저 결과는 구분한다.
