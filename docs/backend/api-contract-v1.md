# saneB Backend API Contract v1

작성일: 2026-05-14

## 1. 기준

- 모든 API는 `/api/v1/...` 경로를 사용한다.
- 응답은 `ApiResponse<T>` wrapper를 사용한다.
- 목록 API는 `PageResponse<T>`를 사용한다.
- 인증과 권한 검증은 서버에서 수행한다.
- `/api/v1/**` 요청은 서버 rate limit 적용 대상이다.
- 서버 화면 form과 `/logout`은 CSRF 검증 대상이다.
- 브라우저 세션 쿠키가 포함된 `/api/v1/**` 변경 요청은 `XSRF-TOKEN` 쿠키와 같은 값을 `X-XSRF-TOKEN` header로 전송해야 한다. 단, `/api/v1/payment-webhooks/**`는 provider webhook 검증을 사용하므로 CSRF header 대상에서 제외한다.
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
  "reviewStatusLabel": "검수대기"
}
```

`GET /api/v1/admin/announcement-sources/{sourceId}`는 `attachments[]`, `highlights[]`, `duplicateCandidates[]`, `sourceDuplicates[]`를 포함한다. `duplicateCandidates[]`는 기존 활성 운영 공고와 비교한 결과다. `sourceDuplicates[]`는 기업마당·정부24·지자체 수집 원문 간 교차 중복 결과다. `matchTypeCode`는 `EXACT_DUPLICATE` 또는 `SIMILAR`이다.

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

지자체 URL 저장 요청은 기존 v1 필드에 선택값 `collectionEndpointUrl`, `requestProfileCode`를 추가한다. `collectionEndpointUrl`은 화면 URL과 실제 공개 데이터 endpoint가 다를 때만 사용하며 동일한 SSRF 검증을 거친다. `requestProfileCode`는 `DEFAULT`, `BROWSER_HTTP1`, `LEGACY_BROWSER`만 허용하고 생략하면 `DEFAULT`다. `LEGACY_BROWSER`는 실사이트 격리 QA에서 표준 Java HTTP 클라이언트와 다른 결과가 재현된 기관에만 적용하며, 브라우저 호환 헤더와 2배 제한시간을 사용하는 GET 전용 정책이다. 조회 응답에는 두 필드와 내부 요청 방식인 `requestMethodCode`가 함께 반환된다. `requestMethodCode='POST_FORM'`과 공개 폼 값은 검증된 운영 migration으로만 관리하며 관리자 저장 요청에서 임의 입력받지 않는다. JSON endpoint는 서버에 검증된 `GENERIC_JSON` 파서 프로필이 지정된 경우에만 처리한다.

파서 프로필은 서버 정적 seed로 관리한다. `SAFE_TEMPLATE` 프로필은 링크 함수 리터럴 인자 수와 `arg`, `attr`, `query`, `input` placeholder만 해석하며 JavaScript를 실행하지 않는다. 생성한 상세 URL은 같은 기관의 검증된 host일 때만 저장한다. 대전 통합 목록처럼 외부 전자민원 host로 이동하는 구조는 코드에 고정된 기관별 허용 목록만 사용한다. 2026-07-14 최종 전수 QA에서는 244개 중 242개 출처가 제목·등록일·상세 URL 검증을 통과했다. 중랑구는 최근 1년 이내 등록 공고가 없고, 금천구 지원사업 목록은 일부 행에 등록일이 없어 `CHECK_REQUIRED`로 유지한다. 모든 출처는 운영자가 개별 확인하기 전 OFF를 유지한다. 이 내부 프로필 확장은 기존 `/api/v1/admin/local-government-notice-parser-profiles`의 path와 응답 구조를 변경하지 않는다.

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `GET` | `/api/v1/admin/local-government-notice-sources` | `OPERATOR`, `APPROVER`, `ADMIN` | 지자체 URL 목록, pagination |
| `POST` | `/api/v1/admin/local-government-notice-sources` | `OPERATOR`, `ADMIN` | 지자체 URL OFF 상태 등록 |
| `GET` | `/api/v1/admin/local-government-notice-sources/{sourceId}` | `OPERATOR`, `APPROVER`, `ADMIN` | 지자체 URL 상세 |
| `PUT` | `/api/v1/admin/local-government-notice-sources/{sourceId}` | `OPERATOR`, `ADMIN` | 지자체 URL 수정 |
| `PATCH` | `/api/v1/admin/local-government-notice-sources/{sourceId}/enabled` | `OPERATOR`, `ADMIN` | 검증완료·파서 지정 URL ON/OFF |
| `DELETE` | `/api/v1/admin/local-government-notice-sources/{sourceId}` | `ADMIN` | 지자체 URL soft delete |
| `DELETE` | `/api/v1/admin/local-government-notice-sources/qa-artifacts` | `ADMIN` | 확인 문구 검증 후 지자체 QA 수집 원문·요청·실행 이력 삭제 |
| `POST` | `/api/v1/admin/local-government-notice-sources/{sourceId}/collection-requests` | `OPERATOR`, `ADMIN` | 단일 URL 수동 수집 승인 요청 |
| `GET` | `/api/v1/admin/local-government-notice-parser-profiles` | `OPERATOR`, `APPROVER`, `ADMIN` | 수집 파서 목록 |
| `GET` | `/api/v1/admin/local-government-notice-sources/collection-summary` | `OPERATOR`, `APPROVER`, `ADMIN` | 수집 신호등 집계 |
| `GET` | `/api/v1/admin/announcement-source-collection-schedules` | `OPERATOR`, `APPROVER`, `ADMIN` | 정기 수집 일정 목록 |
| `POST` | `/api/v1/admin/announcement-source-collection-schedules` | `OPERATOR`, `ADMIN` | 승인 대기 정기 일정 생성 |
| `PATCH` | `/api/v1/admin/announcement-source-collection-schedules/{scheduleId}/status` | `APPROVER`, `ADMIN` | 일정 승인·중지·반려·만료 |

신호등은 오류 URL이 있으면 `RED`, 신규 검수대기 또는 확인 필요 URL이 있으면 `YELLOW`, 오류와 미처리 항목이 없으면 `GREEN`이다. 자동 수집은 `APPROVED` 스케줄만 실행하며 `(schedule_id, scheduled_for)` unique key로 같은 예정시각의 중복 실행을 차단한다.

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

- 모든 endpoint가 `/api/v1/...`를 사용한다.
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
