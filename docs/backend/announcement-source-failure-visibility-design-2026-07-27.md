# 외부 공고 수집 실패 사유 관리자 조회 설계

## 1. 현재 단계와 Gate

- 단계: 구현 및 로컬 Backend Gate 완료
- 자동 수집 Gate: 운영 변경 전 일시중지
- 실패 사유 저장 Gate: 통과
- 관리자 조회 Gate: 로컬 구현 완료, 운영 배포 검증 대기
- 권한 Gate: 기존 `OPERATOR`, `APPROVER`, `ADMIN` 조회 권한을 유지한다.

현재 시스템은 URL별 수집 결과와 최근 오류를 DB에 저장하지만 관리자 화면은 신호등과 집계 건수만 표시한다. 따라서 운영자는 오류가 발생한 기관과 원인을 화면에서 바로 확인할 수 없다.

2026-07-27 운영 실행 `ASRUN-000009`에서는 다음 차이가 확인됐다.

- 완전 실패: 안양시청 1곳
- 부분 누락: 과천시청, 성주군청 2곳에서 총 4개 행
- 실행 집계 실패·제외: 총 5건

현재 `failedCount`는 완전 실패 기관 수만 집계하고, 부분 누락은 출처 행의 빨간 신호로만 표시한다. 집계와 행 신호의 의미가 일치하지 않으므로 함께 보정해야 한다.

## 2. 목표

관리자가 다음 질문에 한 화면에서 답할 수 있게 한다.

1. 어느 기관에서 문제가 발생했는가?
2. 전체 수집 실패인가, 일부 공고 누락인가?
3. HTTP 응답과 오류 분류는 무엇인가?
4. 마지막 성공과 마지막 실패는 언제인가?
5. 운영자가 다음에 수행할 조치는 무엇인가?
6. 같은 오류가 반복됐는가?

일반 사용자에게는 수집 주소, 파서, HTTP 상태, 내부 오류 코드를 노출하지 않는다.

## 3. 사용자 경험 설계

### 3.1 수집 현황 요약

`/app/admin/announcement-sources` 상단 신호등에 다음 값을 분리해 표시한다.

- 전체 사용 URL
- 정상 URL
- 완전 실패 기관
- 부분 누락 기관
- 설정 확인 필요 기관
- 검수 대기 공고

신호등 기준은 다음과 같다.

| 신호 | 기준 | 관리자 의미 |
|---|---|---|
| 빨강 | 완전 실패 기관이 1곳 이상 | 수집이 되지 않은 기관이 있으므로 즉시 확인 |
| 노랑 | 부분 누락, 설정 확인, 검수 대기가 1건 이상 | 수집은 됐지만 운영 확인 필요 |
| 초록 | 완전 실패, 부분 누락, 설정 확인, 검수 대기가 모두 0 | 수집과 후속 처리가 정상 |

기존 `failedCount`는 v1 호환을 위해 완전 실패 기관 수 의미를 유지한다. 신규 필드는 additive하게 추가한다.

### 3.2 지자체 URL 목록

목록에 `문제 유형` 열과 `실패 상세` 버튼을 추가한다.

- 정상: `정상`
- 부분 누락: `일부 공고 항목 누락`
- 완전 실패: `수집 실패`
- 설정 확인: `수집 설정 확인 필요`

빨강 또는 노랑 상태에서는 행 안에 최근 오류 요약을 한 줄로 표시한다. 원문 오류 메시지 전체는 모달에서 확인한다.

### 3.3 실패 상세 모달

`실패 상세` 버튼을 누르면 다음 내용을 표시한다.

- 관리코드
- 기관명
- 문제 수준: 완전 실패 또는 부분 누락
- 최근 수집 시각
- 최근 성공 시각
- HTTP 상태
- 오류 분류
- 한글 오류 제목
- 한글 상세 사유
- 운영자 권장 조치
- 공식 공고 페이지 바로가기
- 최근 수집 이력

기술 정보는 별도 접힘 영역에 배치한다.

- 내부 오류 코드
- 수집 방식
- 파서 프로필
- 수집 endpoint

내부 UUID와 stack trace는 표시하지 않는다.

### 3.4 수집 실행 이력

수집 실행 이력 표에 `상세 보기` 열을 추가한다. 상세 모달은 실행 요약과 URL별 결과를 함께 표시한다.

URL별 결과 항목은 다음과 같다.

- 출처 관리코드
- 기관명
- 결과
- 발견 공고 수
- 신규 공고 수
- 중복 공고 수
- 누락 또는 실패 수
- HTTP 상태
- 한글 실패 사유
- 시작 시각
- 종료 시각

정렬은 `완전 실패 -> 부분 누락 -> 나머지`, 기관명 오름차순으로 한다.

### 3.5 재수집 동선

실패 상세 모달에 `재수집 승인 요청` 버튼을 제공한다.

- `OPERATOR`, `ADMIN`만 버튼을 사용할 수 있다.
- 버튼은 기존 단일 URL 수집 승인 요청 API를 호출한다.
- 즉시 수집을 실행하지 않는다.
- `APPROVER`는 상세 조회와 승인만 수행한다.
- 기존 승인 절차를 우회하지 않는다.

## 4. 오류 분류 계약

DB에 저장된 `error_code`와 `error_message`를 원본으로 유지하고, 서비스 계층에서 관리자용 분류와 권장 조치를 파생한다.

| 오류 코드 | 관리자 표시 | 분류 | 권장 조치 |
|---|---|---|---|
| `RETRYABLE` | 기관 사이트 응답 지연 | 일시 장애 | 잠시 후 재수집 승인 요청. 반복 시 endpoint와 제한시간 점검 |
| `NETWORK_ERROR` | 기관 사이트 연결 실패 | 네트워크 | 기관 페이지 접속 여부와 서버 통신 경로 확인 |
| `COLLECTION_INTERRUPTED` | 수집 작업 중단 | 실행 환경 | 서버 로그와 배포·재시작 이력 확인 후 재수집 |
| `URL_VALIDATION_FAILED` | 수집 주소 검증 실패 | 주소 | 공식 URL과 허용된 프로토콜·호스트 확인 |
| `REDIRECT_URL_BLOCKED` | 이동 주소 차단 | 주소/보안 | 공식 이동 주소 확인 후 검증된 endpoint로 보정 |
| `TOO_MANY_REDIRECTS` | 주소 이동 반복 | 주소 | 최종 공식 공고 URL로 보정 |
| `REDIRECT_LOCATION_MISSING` | 이동 주소 누락 | 주소 | 기관 응답과 공식 공고 URL 확인 |
| `HTTP_ERROR` | 기관 사이트 응답 오류 | HTTP | HTTP 상태와 공식 페이지 상태 확인 |
| `ACCESS_BLOCKED` | 자동수집 접근 차단 | 접근 제한 | 요청 방식 검토 또는 수동 확인 대상으로 전환 |
| `UNSUPPORTED_CONTENT_TYPE` | 지원하지 않는 응답 형식 | 응답 형식 | HTML/JSON endpoint와 파서 프로필 확인 |
| `PARSER_NOT_CONFIGURED` | 수집 규칙 미지정 | 설정 | 검증된 파서 프로필 지정 |
| `JSON_PARSER_NOT_CONFIGURED` | JSON 수집 규칙 미지정 | 설정 | 검증된 JSON 파서 프로필 지정 |
| `LIST_SELECTOR_NOT_MATCHED` | 공고 목록 구조 변경 의심 | 파서 | 기관 페이지 구조와 목록 선택자 재검증 |
| `REQUIRED_FIELDS_MISSING` | 필수 공고 정보 추출 실패 | 파서 | 제목, 등록일, 원문 URL 추출 규칙 재검증 |
| `ITEM_FIELDS_MISSING` | 일부 공고 필수정보 누락 | 데이터 품질 | 누락 행을 확인하고 파서 보정 여부 판단 |
| `JSON_ITEM_FIELDS_MISSING` | 일부 JSON 공고 필수정보 누락 | 데이터 품질 | JSON 필드 매핑과 누락 행 확인 |
| `JSON_ITEMS_NOT_FOUND` | JSON 공고 목록 미발견 | 파서 | 목록 경로와 실제 응답 구조 재검증 |
| `JSON_REQUIRED_FIELDS_MISSING` | JSON 필수정보 추출 실패 | 파서 | 제목, 등록일, 원문 URL 필드 매핑 재검증 |
| `JSON_PARSE_ERROR` | JSON 응답 해석 실패 | 응답 형식 | 실제 응답과 JSON 형식 확인 |
| `DAEJEON_EMINWON_ITEMS_NOT_FOUND` | 대전 통합 공고 상세정보 미발견 | 파서 | 대전 통합 목록 링크 구조와 기관별 상세 주소 재검증 |
| `HEURISTIC_ITEMS_NOT_FOUND` | 공고 목록 자동 탐색 실패 | 파서 | 전용 또는 공통 파서 프로필 지정 |
| `PARSER_ERROR` | 공고 구조 해석 실패 | 파서 | 서버 로그와 기관 페이지 구조 확인 |

정의되지 않은 코드는 `분류되지 않은 수집 오류`로 표시하고, 저장된 한글 `error_message`를 함께 노출한다.

## 5. DB 설계

### 5.1 신규 테이블과 migration

신규 테이블과 migration은 필요하지 않다.

기존 `announcement_source_collection_source_results`가 실행별 URL 결과를 이미 저장한다.

- `run_id`
- `local_government_source_id`
- `result_status_code`
- `discovered_count`
- `new_count`
- `duplicate_count`
- `failed_count`
- `http_status`
- `error_code`
- `error_message`
- `started_at`
- `finished_at`

기존 `local_government_notice_sources`가 최근 상태를 이미 저장한다.

- `last_collected_at`
- `last_success_at`
- `last_http_status`
- `last_error_code`
- `last_error_message`

기존 unique/index로 실행별 조회와 출처별 이력 조회가 가능하다.

- `(run_id, local_government_source_id)` unique
- `(local_government_source_id, started_at DESC)` index

### 5.2 저장 금지

- stack trace
- 응답 본문 전체
- 운영 secret
- 외부 API key
- 개인정보 원문

## 6. API 계약

기존 `/api/v1` 경로는 유지하고 응답과 조회 조건만 additive하게 확장한다.

### 6.1 수집 실행 상세 확장

`GET /api/v1/admin/announcement-source-collections/runs/{runId}`

기존 `run`, `items`를 유지하고 `sourceResults`를 추가한다.

```json
{
  "success": true,
  "data": {
    "run": {},
    "items": [],
    "sourceResults": [
      {
        "sourcePublicCode": "LGS-000094",
        "institutionName": "안양시청",
        "resultStatusCode": "FAILED",
        "resultStatusLabel": "수집 실패",
        "issueLevelCode": "FULL_FAILURE",
        "discoveredCount": 0,
        "newCount": 0,
        "duplicateCount": 0,
        "failedCount": 1,
        "httpStatus": null,
        "errorCode": "RETRYABLE",
        "errorCategoryCode": "TEMPORARY",
        "errorTitle": "기관 사이트 응답 지연",
        "errorMessage": "기관 사이트 응답 시간이 초과되었습니다.",
        "recommendedAction": "잠시 후 재수집 승인 요청을 만들고, 반복되면 수집 주소와 제한시간을 확인하세요.",
        "startedAt": "2026-07-27T17:10:04+09:00",
        "finishedAt": "2026-07-27T17:10:20+09:00"
      }
    ]
  },
  "message": ""
}
```

### 6.2 출처 목록 조회 조건 확장

`GET /api/v1/admin/local-government-notice-sources`

선택 query parameter를 추가한다.

- `issueLevelCode=FULL_FAILURE`
- `issueLevelCode=PARTIAL_FAILURE`
- `issueLevelCode=WARNING`
- `issueLevelCode=NORMAL`

기존 query parameter와 응답 필드는 유지한다.

### 6.3 출처 응답 확장

`LocalGovernmentNoticeSourceResponse`에 다음 파생 필드를 추가한다.

- `issueLevelCode`
- `errorCategoryCode`
- `errorTitle`
- `recommendedAction`

기존 `lastErrorCode`, `lastErrorMessage`는 유지한다.

### 6.4 출처별 최근 수집 이력

다음 조회 API를 additive하게 추가한다.

`GET /api/v1/admin/local-government-notice-sources/{sourceId}/collection-results?page=1&size=20`

- 권한: `OPERATOR`, `APPROVER`, `ADMIN`
- 응답: `ApiResponse<PageResponse<LocalGovernmentNoticeCollectionResultResponse>>`
- 정렬: `started_at DESC`
- 화면에는 `sourcePublicCode`와 실행 `publicCode`만 표시한다.
- UUID는 API 내부 연결에만 사용하고 화면에 출력하지 않는다.

### 6.5 수집 현황 집계 확장

`GET /api/v1/admin/local-government-notice-sources/collection-summary`

기존 필드를 유지하고 다음 필드를 추가한다.

- `fullFailureCount`
- `partialFailureCount`
- `configurationWarningCount`
- `errorSourceCount`

호환 정책은 다음과 같다.

- 기존 `failedCount = fullFailureCount`
- `errorSourceCount = fullFailureCount + partialFailureCount`
- 빨강: `fullFailureCount > 0`
- 노랑: `partialFailureCount > 0` 또는 설정 확인/검수 대기 존재

## 7. 계층별 구현 책임

### Controller

- 기존 URL 매핑과 권한 선언을 유지한다.
- 조회 조건과 pagination만 전달한다.
- 오류 분류 로직을 작성하지 않는다.

### Service / ServiceImpl

- 결과 상태와 오류 코드를 관리자용 한글 표시로 변환한다.
- `FULL_FAILURE`, `PARTIAL_FAILURE`, `WARNING`, `NORMAL`을 판정한다.
- 권장 조치를 파생한다.
- DTO 조립을 담당한다.

### DAO / Mapper XML

- 실행별 URL 결과를 출처와 조인해 조회한다.
- 출처별 최근 실행 결과를 pagination 조회한다.
- 완전 실패와 부분 누락 집계를 분리한다.
- `SELECT *`, `${}`를 사용하지 않는다.

### Thymeleaf / JavaScript

- 모달 골격과 접근성 속성은 Thymeleaf에 둔다.
- API 응답은 `textContent`로만 렌더링한다.
- `th:utext`와 HTML 문자열 기반 오류 메시지 삽입을 사용하지 않는다.
- 상세 모달은 닫기, `Escape`, 초점 복귀를 지원한다.
- 360px에서는 항목을 세로로 표시하고 긴 URL과 메시지는 줄바꿈한다.

## 8. 권한과 보안

- 조회: `OPERATOR`, `APPROVER`, `ADMIN`
- 재수집 승인 요청 생성: `OPERATOR`, `ADMIN`
- 승인/반려: `APPROVER`, `ADMIN`
- 일반 `USER`, `PARTNER`: API 접근 시 403
- 일반 사용자 화면에는 관리자 수집 장애를 표시하지 않는다.
- URL은 기존 SSRF 검증을 통과한 값만 바로가기로 사용한다.
- 오류 메시지에 stack trace, secret, 응답 본문을 포함하지 않는다.

## 9. 구현 순서

1. `docs/backend/api-contract-v1.md`와 `db-model-v1.md`에 확정 계약을 반영한다.
2. URL별 결과 조회 VO, DTO, DAO 메서드를 추가한다.
3. Mapper XML에 실행별 상세, 출처별 이력, 분리 집계 SQL을 추가한다.
4. ServiceImpl에 오류 분류와 권장 조치 변환을 추가한다.
5. 기존 실행 상세 응답에 `sourceResults`를 추가한다.
6. 출처 목록의 문제 수준 필터와 파생 필드를 추가한다.
7. 관리자 화면에 요약 집계, 실패 상세, 실행 상세 모달을 추가한다.
8. 재수집 승인 요청 동선을 기존 승인 API와 연결한다.
9. 권한, mapper, controller, 브라우저 테스트를 보강한다.
10. 운영 배포 후 기존 실패 실행과 신규 시험 실행으로 검증한다.

## 10. 성공 기준

- `ASRUN-000009` 상세에서 안양시청 완전 실패 1곳을 확인할 수 있다.
- 과천시청과 성주군청의 부분 누락 4건을 별도로 확인할 수 있다.
- 집계 신호와 목록 행 신호의 의미가 일치한다.
- 관리자는 오류 코드가 아닌 한글 원인과 권장 조치를 먼저 본다.
- 출처별 최근 수집 이력에서 반복 오류 여부를 확인할 수 있다.
- 재수집은 승인 요청을 거치며 즉시 실행되지 않는다.
- 일반 사용자와 파트너는 실패 상세 API와 화면에 접근할 수 없다.
- 기존 `/api/v1` 응답 필드와 path가 유지된다.

## 11. 실패 기준

- 부분 누락을 완전 실패와 같은 숫자로만 표시한다.
- 집계는 정상인데 출처 행만 빨간색으로 표시된다.
- 관리자 화면에 내부 UUID 또는 stack trace가 노출된다.
- 실패 상세에서 재수집을 승인 없이 즉시 실행한다.
- 일반 사용자 화면에 수집 endpoint, 파서, HTTP 오류가 노출된다.
- 기존 v1 필드를 삭제하거나 의미를 변경한다.
- MyBatis에서 `SELECT *` 또는 `${}`를 사용한다.
- Thymeleaf에서 `th:utext`를 사용한다.

## 12. 검증 체크리스트

- [ ] 정상, 부분 누락, 완전 실패 fixture를 각각 준비한다.
- [ ] 실행 상세의 URL별 결과와 DB source result 행이 일치한다.
- [ ] 출처별 최근 이력이 최신순으로 pagination된다.
- [ ] `failedCount`, `fullFailureCount`, `partialFailureCount`가 중복 집계되지 않는다.
- [ ] 오류 코드별 한글 제목과 권장 조치가 표시된다.
- [ ] 정의되지 않은 오류 코드도 안전한 기본 문구로 표시된다.
- [ ] `USER`, `PARTNER` 접근이 403으로 차단된다.
- [ ] `OPERATOR`, `APPROVER`, `ADMIN` 조회 권한이 유지된다.
- [ ] 재수집 요청이 승인 대기로 생성된다.
- [ ] 360px, 768px, 1024px, 1440px에서 모달과 표를 확인한다.
- [ ] 키보드로 상세 열기, 닫기, 초점 복귀가 가능하다.
- [ ] 전체 테스트와 `bootJar`가 성공한다.
- [ ] PostgreSQL/Flyway Backend Gate가 성공한다.
- [ ] `SELECT *`, `${}`, `th:utext` 금지 패턴이 없다.

## 13. 보류 항목

- 일반 사용자 대상 수집 지연 공지
- Slack, 문자, 이메일 장애 알림 확대
- 오류 자동 복구 또는 파서 자동 생성
- stack trace와 응답 본문을 저장하는 진단 기능
- 장기 이력 보관 기간과 별도 아카이빙 정책

일반 사용자 공지는 실제 후보 데이터 신선도와 연결되는 별도 제품 정책이 필요하다. 현재 범위에서는 관리자 화면과 권한 API에만 실패 상세를 제공한다.

## 14. 최종 구현 계약

초기 설계의 `issueLevelCode` 단일 축은 접속·파싱·필드·의미 오류를
정확히 분리하지 못하므로 최종 구현에서는 `diagnosticReasonCode`를 사용한다.

| 진단 코드 | 의미 |
|---|---|
| `TRANSPORT_FAILED` | timeout, 네트워크, HTTP 응답 실패 |
| `PARSER_FAILED` | HTML/JSON 목록 구조 또는 파서 설정 실패 |
| `PARTIAL_FIELDS` | 제목·등록일·원문 URL 일부 또는 전체 누락 |
| `SEMANTIC_MISMATCH` | 보도자료, 미확인 출처, 게시판 유형 불일치 |
| `IRRELEVANT_CONTENT` | 일반 공지 정적 키워드 판정으로 제외된 게시물 |
| `PROCESSING_FAILED` | 출처 수집·정적 의미 판정 중 내부 처리 예외 |
| `UNCLASSIFIED_ERROR` | 정의되지 않은 원본 오류 코드가 남아 있어 추가 분류가 필요한 상태 |

`TRANSPORT_FAILED`는 상위 필터 계약을 유지하면서 원본 오류를 다음과 같이 세분화한다.

| 원본 오류 코드 | 관리자 의미 |
|---|---|
| `DNS_LOOKUP_FAILED` | 운영 서버가 기관 주소를 조회하지 못함 |
| `TLS_HANDSHAKE_FAILED` | 기관 사이트와 TLS 보안 연결 협상 실패 |
| `CONNECTION_REFUSED` | 기관 방화벽·접근 경로에서 연결 거부 |
| `CONNECTION_RESET` | 연결 후 기관 측에서 세션 중단 |
| `RETRYABLE` | 응답 제한시간 초과 또는 일시적 서버 오류 |
| `NETWORK_ERROR` | 위 유형으로 안전하게 분류되지 않은 네트워크 오류 |

stack trace와 원문 예외 메시지는 DB·API 응답에 저장하지 않는다. 관리자는 출처 화면에서 한글 진단 제목과 코드별 권장 조치를 확인하고, 필요한 경우 관리자 전용 앱 로그에서 실행 ID와 출처 관리코드로 ERROR 로그를 추적한다.

구현된 additive 계약은 다음과 같다.

- `GET /api/v1/admin/local-government-notice-sources`
  - `sourceBoardTypeCode`
  - `collectionPolicyCode`
  - `semanticallyVerified`
  - `diagnosticReasonCode`
- `GET /api/v1/admin/local-government-notice-sources/{sourceId}/collection-results`
- `GET /api/v1/admin/announcement-source-collections/runs/{runId}`의 `sourceResults[]`
- `GET /api/v1/admin/local-government-notice-sources/collection-summary`
  - `transportFailureCount`
  - `parserFailureCount`
  - `partialFieldsCount`
  - `semanticMismatchCount`
  - `irrelevantContentCount`
  - `unverifiedSourceCount`

원본 `error_code`와 `error_message`는 보존하고, 한글 제목과 권장 조치는
응답 조립 시 파생한다. 내부 UUID는 API 연결에만 사용하며 관리자 화면에는
관리코드와 실행코드를 우선 표시한다.
