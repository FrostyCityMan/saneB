# saneB Backend API Contract v1

작성일: 2026-05-14

## 1. 기준

- 모든 API는 `/api/v1/...` 경로를 사용한다.
- 응답은 `ApiResponse<T>` wrapper를 사용한다.
- 목록 API는 `PageResponse<T>`를 사용한다.
- 인증과 권한 검증은 서버에서 수행한다.
- Controller는 URL 매핑, 요청/응답, DTO 변환만 담당한다.
- 비즈니스 로직은 ServiceImpl에 둔다.
- SQL은 DAO와 Mapper XML을 통해서만 실행한다.
- MyBatis XML에서 `SELECT *`와 `${}`는 금지한다.
- MVP에서는 AI 자동판단, 추천도, 우선순위, 선정확률, 가점 계산을 제공하지 않는다.

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
  "companyStageCode": "OPERATING"
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
  "enrollmentStatusCode": null,
  "cohabiting": true,
  "supported": true,
  "hasIncome": false
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
| `PUT` | `/api/v1/announcements/{announcementId}/steps` | `OPERATOR` | 진행 단계 저장 |
| `POST` | `/api/v1/announcements/{announcementId}/approval-requests` | `OPERATOR`, `ADMIN` | 승인 요청 |
| `PATCH` | `/api/v1/announcements/{announcementId}/approval` | `APPROVER`, `ADMIN` | 승인/반려/취소 |
| `PATCH` | `/api/v1/announcements/{announcementId}/manual-status` | `OPERATOR`, `APPROVER` | 수동 상태 변경 |

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
      "conditionScopeCode": "BUSINESS",
      "conditionKey": "BUSINESS_TYPE",
      "optionCode": "SOLE_PROPRIETOR",
      "optionText": null
    }
  ],
  "documentRequirements": [
    {
      "documentTypeCode": "BUSINESS_REGISTRATION",
      "required": true,
      "sortOrder": 1
    }
  ]
}
```

#### AnnouncementStepsSaveRequest

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
        }
      ],
      "documents": []
    }
  ]
}
```

## 8. Matching API

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `POST` | `/api/v1/matching/cases` | `OPERATOR`, `ADMIN` | 매칭 케이스 생성/재계산 |
| `GET` | `/api/v1/matching/cases/member-lookups` | `OPERATOR`, `APPROVER`, `ADMIN` | 매칭 생성용 회원 조회 |
| `GET` | `/api/v1/matching/cases` | `USER`, `PARTNER`, `OPERATOR`, `APPROVER`, `ADMIN` | 매칭 케이스 목록 |
| `GET` | `/api/v1/matching/cases/{matchingCaseId}` | `USER`, `PARTNER`, `OPERATOR`, `APPROVER`, `ADMIN` | 매칭 케이스 상세 |
| `GET` | `/api/v1/matching/cases/{matchingCaseId}/results` | `USER`, `PARTNER`, `OPERATOR`, `APPROVER`, `ADMIN` | 조건별 매칭 결과 |
| `PATCH` | `/api/v1/matching/cases/{matchingCaseId}/status` | `OPERATOR`, `APPROVER`, `ADMIN` | 매칭 상태 수동 변경 |

#### MatchingCaseCreateRequest

```json
{
  "announcementId": "uuid",
  "memberUserId": "uuid",
  "verificationId": null
}
```

`verificationId`는 선택값이다. 현재 운영 기준에서는 검증 없이 수동 매칭을 생성할 수 있으며, 이 경우 `matching_cases.verification_id`는 `null`로 저장한다. 검증 ID를 전달한 경우에는 기존처럼 검증 완료, current, matching block 여부를 서버에서 확인한다.

#### MatchingCaseResponse

```json
{
  "matchingCaseId": "uuid",
  "announcementId": "uuid",
  "memberUserId": "uuid",
  "verificationId": "uuid",
  "statusCode": "MATCHED",
  "matchedAt": "2026-05-14T10:00:00+09:00"
}
```

매칭 응답에는 추천도, 우선순위, 선정확률, 가점 값을 포함하지 않는다.

## 9. Application Progress API

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| `POST` | `/api/v1/application-progresses` | `USER`, `OPERATOR`, `ADMIN` | 매칭 케이스에서 진행 시작 |
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

일반 사용자는 본인 `matchingCaseId`만 진행 시작할 수 있다. 운영자와 관리자는 기존처럼 다른 회원의 매칭 케이스를 진행 시작할 수 있다. `matching_cases.verification_id`가 `null`인 매칭 케이스도 `statusCode = MATCHED`이고 공고 진행 단계가 있으면 신청 진행을 시작할 수 있다.

#### ApplicationProgressDetailsResponse

`GET /api/v1/application-progresses/{progressId}`와 진행 처리 API 응답은 진행 상태, 체크리스트, 현재 공고 단계에 등록된 행동 버튼 목록을 함께 반환한다.

```json
{
  "progressId": "uuid",
  "matchingCaseId": "uuid",
  "announcementId": "uuid",
  "memberUserId": "uuid",
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

- 진행 가능한 후보는 `matching_cases` 기준으로 집계한다. `MATCHED`, `REVIEW_REQUIRED`, `PROGRESSED`만 후보로 포함하며, 해당 데이터가 없으면 empty state를 반환한다.
- 후보 유형은 V1의 별도 유형 컬럼을 추가하지 않고 `announcement_options.option_group_code = PAYMENT_METHOD` 기준으로 분류한다. `LOAN`, `GUARANTEE`, `INTEREST_SUPPORT`는 `policyFund`, `VOUCHER`, `POINT`, `GOODS`, `TAX_DEDUCTION`은 `supportFund`, `CASH`, `REFUND`는 `subsidy`로 집계한다.
- 금액 범위는 후보 `matching_cases`에 연결된 `announcements.min_amount`, `announcements.max_amount`의 최소/최대값만 사용한다. `application_progresses.received_amount`와 혼합하지 않는다.
- 현재 해야 할 행동은 `application_step_states.status_code IN (READY, IN_PROGRESS)`인 단계 1건을 우선 반환한다. 없으면 파트너 검증 상태를 기준으로 `VERIFICATION_DOCUMENT_REQUIRED` 또는 `NONE`을 반환한다.
- 진행/승인/수령 금액은 `application_progresses` 기준으로 집계한다. 누적 수령 금액은 `received_amount`가 있고 `status_code IN (APPROVED, COMPLETED)` 또는 `result_code = APPROVED`인 행만 합산한다.
- 개인정보 원문은 대시보드 집계에 포함하지 않고, 사용자 식별자와 진행/공고 운영 데이터만 조인한다.

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
  "serviceStatusCode": "VERIFICATION_REQUIRED",
  "candidateCounts": {
    "policyFund": 3,
    "supportFund": 2,
    "subsidy": 0
  },
  "finalMatchedCount": 0,
  "supportAmountRange": {
    "minAmount": 30000000,
    "maxAmount": 70000000,
    "basisCode": "ANNOUNCEMENT_AMOUNT_RANGE"
  },
  "verificationStatusCode": "DRAFT",
  "noticeMessage": "전자증명 검증 전 참고 결과입니다."
}
```

`supportAmountRange`는 확정 수령액이 아니라 공고에 등록된 지원금액 범위의 참고 표시다.

DashboardSummaryResponse 필드 계약:

| 필드 | 타입 | nullable | Frontend 사용 기준 |
|---|---|---:|---|
| `serviceStatusCode` | `string` | false | 대시보드 전체 상태 배지 |
| `candidateCounts.policyFund` | `number` | false | 정책자금 후보 건수 |
| `candidateCounts.supportFund` | `number` | false | 지원금 후보 건수 |
| `candidateCounts.subsidy` | `number` | false | 보조금 후보 건수 |
| `finalMatchedCount` | `number` | false | 최종 매칭 확정 건수 |
| `supportAmountRange.minAmount` | `number` | true | 공고 기준 최소 지원금액 |
| `supportAmountRange.maxAmount` | `number` | true | 공고 기준 최대 지원금액 |
| `supportAmountRange.basisCode` | `string` | false | 금액 표시 근거 코드 |
| `verificationStatusCode` | `string` | false | 파트너 검증 상태 |
| `noticeMessage` | `string` | true | 사용자 안내 문구 |

#### DashboardCurrentActionResponse

```json
{
  "actionCode": "VERIFICATION_DOCUMENT_REQUIRED",
  "title": "사업자등록증 확인이 필요합니다.",
  "description": "파트너 검증을 위해 필수 서류 확인을 완료해 주세요.",
  "primaryButtonLabel": "서류 확인하기",
  "route": "/app/member/verifications/current",
  "dueDate": null,
  "displayOrder": 5
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

## 13. Audit API

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
| `resourceType` | `USER`, `PARTNER_VERIFICATION`, `MATCHING_CASE`, `APPLICATION_PROGRESS` |
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

## 14. ErrorCode 초안

| errorCode | HTTP | 설명 |
|---|---:|---|
| `AUTH_REQUIRED` | 401 | 인증 필요 |
| `AUTH_INVALID_CREDENTIALS` | 401 | 로그인 실패 |
| `AUTH_FORBIDDEN` | 403 | 권한 없음 |
| `AUTH_PASSWORD_RESET_REQUIRED` | 403 | 비밀번호 변경 필요 |
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
| `DB_CONSTRAINT_VIOLATION` | 409 | DB 제약 위반 |
| `INTERNAL_ERROR` | 500 | 서버 오류 |

## 14. Backend Gate

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
