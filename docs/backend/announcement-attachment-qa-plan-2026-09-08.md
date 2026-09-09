# 공고 첨부파일 수집·추출 QA 계획

- 작성일: 2026-09-08
- 상태: 테스트 설계. 아래 시나리오는 아직 실행하지 않았다.
- 기준 설계: [첨부파일 상세 설계](announcement-attachment-collection-design-2026-09-08.md)
- 목적: 링크 발견, 실제 파일 확보, 텍스트 추출, 판정, 검수·전환을 각각 증명한다.

## 1. 검증 Gate

| Gate | 상태 | 필요한 증거 |
|---|---|---|
| 문서 자체 검토 | [x] | 참조 경로·DB/API/상태 일관성·Git 변경 범위. 구현 검증은 별도 |
| 로컬 fixture 단위 테스트 | [ ] | 파일별 기대 텍스트·실패·locator·프로세스 종료 |
| DB/API 통합 | [ ] | 빈 DB/V71 upgrade, FK·CAS·동시성·403·409 |
| 사이트별 격리 QA | [ ] | 실제 공고와 첨부 응답을 확인한 비식별 결과 |
| 운영 COLLECT_ONLY | [ ] | 제한 대상 처리량·오류·자원·기존 판정 미변경 |
| 신규 ENFORCE | [ ] | 종합 UI·전환 guard·현재 근거·모드 OFF 대응 |
| 기존 데이터 적용 | [ ] | 고정 대상·완료 preview·충돌 처리·조건부 원복 |

## 2. fixture 구성

원칙: 재배포 가능한 합성 문서를 우선 사용한다. 실제 파일 fixture는 공개·비민감 여부와 재배포 가능 범위를 확인하고 필요하면 내용 대신 비식별 검증 결과만 보관한다. 운영에서 실제 받은 문서·개인정보를 무심코 Git에 추가하지 않는다. fixture 원문에 실제 계정·비밀번호·API key를 넣지 않는다.

| 집합 | 최소 구성 |
|---|---|
| PDF | 일반 한글, 영문 약어, 표, 다단, 긴 문서, 공백, 스캔, 텍스트+스캔 혼합, 암호·추출 제한, 손상, 한도 초과 |
| HWP | 5.x 일반 문단, 표 셀, 각주·머리말, 그림 포함, 배포용 문서, 암호, 구형/손상, 압축 record 제한 |
| HWPX | 문단·표, section 여러 개, XML 확장 요소, 이미지, XXE, ZIP bomb·경로 이탈, 손상 |
| HTTP | 직접 GET, 무확장 download, redirect, 404 HTML, login HTML, 429, timeout, TLS/DNS 실패, 압축 초과 |
| 분류 | NOTICE/GUIDE/FORM/REFERENCE/UNKNOWN, 파일명만 B, 첨부 A/B, 다중 파일, 본문 부재, 상충 문서 |

HWP 배포용 문서는 선택 라이브러리 버전에서 실제 지원 여부를 확인한다. 지원하지 않으면 `UNSUPPORTED`로 안전하게 완료해야 하며, 성공 지원 범위에 넣지 않는다. 스캔 문서의 글자를 추출하지 못하는 것은 1차 기능의 알려진 범위지만 이를 COMPLETE_TEXT로 표시하면 실패다.

예상 manifest 필드:

```text
caseId, fixtureRelativePath, fixtureSha256, formatCode,
expectedDownloadStatus, expectedQualityCode, expectedErrorCode,
expectedPhrases[], expectedLocators[], expectedDecision,
expectedNetworkRequestCount, expectedPersistedOriginalCount,
redistributionStatus, note
```

## 3. 필수 자동 검증 시나리오

모든 행은 현재 `[ ]`이며 구현 시 실제 테스트 클래스·결과를 연결한다.

| ID | 입력/사건 | 기대 결과 |
|---|---|---|
| ATT-001 | 제목 B + 정상 PDF 링크 | 상세/첨부 HTTP 0, source/file/job 원문성 행 0, tombstone만 기록 |
| ATT-002 | 제목 조합 미충족 + 첨부 정상 조합 | 첨부 요청 0, 제목 제외 복구 금지 |
| ATT-003 | 제목 A + 정상 본문·첨부 | 수집·근거 확보, 종합 REVIEW_REQUIRED |
| ATT-004 | 정상 제목·본문 + 첨부 파일명에만 수출 | 파일명으로 B 판정하지 않음 |
| ATT-005 | 정상 제목 + NOTICE 첨부 본문 B | REVIEW_REQUIRED, EXCLUDED 금지 |
| ATT-006 | 정상 제목 + 첨부 A/B 동시 | REVIEW_REQUIRED, B를 주 근거로 하되 A도 보존 |
| ATT-007 | 본문 없음 + 완전한 NOTICE 안에 대상/지원 조합 | 조건 충족 시 종합 ACCEPTED, base는 BODY_UNAVAILABLE 그대로 |
| ATT-008 | 본문 실패 + 독립 API의 유효 첨부 descriptor | 첨부 처리 계속, 출처·상태 분리 |
| ATT-009 | 파일 A에 대상만, 파일 B에 지원형태만 | 파일 간 AND 금지, REVIEW_REQUIRED |
| ATT-010 | 신청 양식의 예시 문구에 A/B | FORM 참고 근거, 자동 제외/자동 후보 생성 금지 |
| ATT-011 | 파일 역할 UNKNOWN | 역할 검수 필요, 자동 ACCEPTED 금지 |
| ATT-012 | 첨부 영역 정상 파싱·파일 0개 | NO_FILES, 본문 판정 유지 |
| ATT-013 | 첨부 영역 selector 실패 | DISCOVERY_FAILED, NO_FILES로 위장 금지 |
| ATT-014 | 파일 3개 중 NOTICE 1개 실패 | PARTIAL_FAILED/REVIEW_REQUIRED, 성공 파일 조회 가능 |
| ATT-015 | 본문 안의 지원 제외 업종·첨부 상충 내용 | 검수 근거, 부정문 의미 추측·자동 제외 금지 |
| ATT-016 | 텍스트+스캔 혼합 PDF | PARTIAL_TEXT/OCR_REQUIRED, COMPLETE_TEXT 금지 |
| ATT-017 | 정상 한글 PDF·HWP·HWPX | 핵심 문구·표 셀·locator 검증, 지원 형식별 기대값 일치 |
| ATT-018 | HWP 파일에 실제 페이지 정보 없음 | section/paragraph/cell locator, 추정 페이지 번호 금지 |
| ATT-019 | 암호·손상·0byte 문서 | ENCRYPTED/FAILED/EMPTY_TEXT 등 고정 상태, 실패 원문 로그 없음 |
| ATT-020 | URL 확장자 PDF, 응답은 로그인 HTML | 형식 오류/접근 제한, PDF 추출 실행 금지 |
| ATT-021 | 확장자 없는 정상 binary | header+signature 검증 후 형식별 처리 |
| ATT-022 | 다른 기관 file host redirect | allowlist에 있으면 재검증, 없으면 BLOCKED |
| ATT-023 | 사설 IP/IPv6/metadata endpoint/DNS 변경 | 외부 요청 차단, 검증한 IP로만 실제 연결 |
| ATT-024 | 20 MiB 초과 또는 스트리밍 중 예산 초과 | 제한 상태, 즉시 스트림 종료, 부분파일 정리 |
| ATT-025 | HTTP gzip/HWP record/HWPX ZIP 압축 폭탄 | 각 해제 byte·entry·비율 제한, 웹 JVM 생존 |
| ATT-026 | XML 외부 entity·ZIP 경로 이탈·문서 내 URL | 읽기/외부 호출/경로 이탈 0 |
| ATT-027 | extractor timeout·OOM·비정상 종료 | 프로세스 트리 종료, 안전한 실패, 임시파일 정리 |
| ATT-028 | 스크립트·매크로·embedded 파일 포함 | 실행/재귀 다운로드 0 |
| ATT-029 | job 동일 key·동일 요청 2회 | 동일 작업 응답, UNIQUE로 중복 결과 방지 |
| ATT-030 | 동일 key·다른 요청 | 409, 원래 작업 변경 없음 |
| ATT-031 | 두 worker 동시 claim/lease 만료 후 늦은 응답 | 한 worker만 CAS 확정, 늦은 결과 현재화 금지 |
| ATT-032 | 같은 URL의 파일 교체 | 새 binary hash·set·evaluation, 과거 입력 보존 |
| ATT-033 | 목록 본문 hash 불변·첨부만 변경 | 중복 조기 반환으로 갱신 job 누락하지 않음 |
| ATT-034 | 추출 중 source content/검수/version 변경 | CONFLICT, 기존 결과·검수 덮어쓰기 없음 |
| ATT-035 | 원문 삭제 후 worker 완료 | source 재생성 없음, FK/CAS 실패·임시 자원 정리 |
| ATT-036 | source 다른 file/evaluation/extraction ID 요청 | 404, 내용 노출 없음 |
| ATT-037 | 다른 source 또는 다른 rule release의 근거 insert | DB composite FK 거부 |
| ATT-038 | 제목 제외 source 삭제 cascade | 새 하위 데이터 포함 원문 제거, worker 잔여물 미노출 |
| ATT-039 | 현재 종합 판정 뒤 문서 역할 수정 | 새 set/evaluation, 이전 confirmation STALE |
| ATT-040 | 완료 전 또는 오래된 confirmation으로 전환 | 409, 운영 공고 0건 생성 |
| ATT-041 | ENFORCE source를 기존 v1/v2 전환으로 요청 | 기존 오류 wrapper로 409, 새 경로 안내 |
| ATT-042 | 첨부 요구 없는 기존 source 전환 | 기존 계약·동작 그대로 |
| ATT-043 | 정상 종합 confirmation으로 동일 DRAFT 요청 | DRAFT 1개, 기존 source link UNIQUE 유지 |
| ATT-044 | WORKER OFF 또는 정책 변경 | 새 외부 요청 중지, 이미 요구된 첨부 검수 guard 유지 |
| ATT-045 | ADMIN/OPERATOR/APPROVER/USER별 API·CSRF | 읽기/쓰기 권한 일치, CSRF 없는 변경 거부 |
| ATT-046 | backfill 범위 생성 후 새 source 등장 | 고정 대상 외 수집·적용 0 |
| ATT-047 | batch scope preview 및 classification preview | 첫 preview 네트워크 0, 두 번째도 저장된 파일만 사용 |
| ATT-048 | preview 뒤 원문/첨부/규칙/정책 변경 | 적용 CONFLICT, 자동 새 preview 후 적용 금지 |
| ATT-049 | 적용 후 추가 검수/전환 뒤 rollback | 충돌 항목 보호, 덮어쓰기 금지 |
| ATT-050 | 적용 후 변경 없는 rollback | pointer/binding 조건부 복원, version 증가, 이력 보존 |
| ATT-051 | 이미 연결된 운영 공고를 명시 범위에 포함 | 원문 재검수 경고만, 운영 상태·조건·신청 데이터 미변경 |
| ATT-052 | 로그/응답/HTML/임시 경로에 악성·민감 문구 | HTML escape·마스킹/고정 오류, 원문/secret 감사 복사 0 |
| ATT-053 | 빈 DB와 V71 DB에 신규 migration 적용 | schema·기존 데이터·checksum 정상, 기존 첨부 CHECK false 유지 |
| ATT-054 | OFF 및 COLLECT_ONLY 모드로 기존 Golden 실행 | 기존 제목·본문 입력/결과·hash·첨부 제거 테스트 유지 |
| ATT-055 | 같은 판정의 두 confirmation·전환 동시 실행 | source lock·version으로 현재 확정 1개, DRAFT 최대 1개 |
| ATT-056 | 같은 파일의 다른 문단 또는 불명확한 PDF 표에서 대상/지원 분산 | evidenceScopeId 밖의 자동 AND 금지, 검수 |
| ATT-057 | 아직 OPEN인 set의 ENFORCE source | pending projection·decisionId null, 봉인 판정인 것처럼 표시 금지 |
| ATT-058 | source는 같지만 다른 content/set/policy/base를 조합 | composite FK 거부 |
| ATT-059 | 추출 subprocess의 환경/네트워크/파일 접근 | DB secret 미상속, 네트워크·다른 파일 접근 차단 |
| ATT-060 | 두 worker 실행 및 관리자 정책 한도 상향 | 전역 semaphore·bytes 예산과 배포 hard cap 준수 |
| ATT-061 | base evaluation과 첨부 정책의 keyword release 불일치 | BASE_RECLASSIFICATION_REQUIRED, 제목 판정을 건너뛰는 첨부 요청·적용 0 |
| ATT-062 | COLLECT_ONLY에서 본문 부족을 첨부가 보완 | preview만 ACCEPTED, effective/기존 source·검수는 base 유지 |

## 4. 출처별 실제 QA 등록표

메시지의 ‘약 15개 모듈’을 검증된 수로 취급하지 않는다. 실제 프로필 목록을 추출해 다음 행을 채운다. 프로필 하나가 여러 기관을 처리하면 각 변형을 별도 검증한다.

| provider/profile | 목록·본문 가용성 | 첨부 발견 | bytes 다운로드 | PDF | HWP | HWPX | 미지원 이유 |
|---|---|---|---|---|---|---|---|
| BIZINFO / 실제 프로필 확인 필요 | [ ] | [ ] | [ ] | [ ] | [ ] | [ ] | API key/공식 응답/파일 host 확인 필요 |
| GOV24_PUBLIC_SERVICE / 실제 프로필 확인 필요 | [ ] | [ ] | [ ] | [ ] | [ ] | [ ] | API 가용성/첨부 제공 방식 확인 필요 |
| LOCAL_GOV_NOTICE / 프로필별 행 추가 | [ ] | [ ] | [ ] | [ ] | [ ] | [ ] | 정적 parser QA와 별개로 첨부 QA 필요 |

각 프로필은 직접 링크·redirect·무확장 다운로드·빈 첨부 중 해당되는 변형을 포함한다. 대표 정상 공고 최소 3건과 해당 형식별 1건 이상을 확보하되, 발견하지 못한 형식은 ‘검증 제외/미확인’이지 성공이 아니다. 실제 QA 대상이 적으면 그 수와 이유를 기록한다. 모두 정상인 쉬운 파일만 골라 전체 지원으로 표현하지 않는다.

검증 결과는 `profileCode`, `profileHash`, provider, 확인 일자, safe case ID, HTTP 상태, 수신 byte 수, binary hash, 추출 상태, 글자 수, 소요시간, 실패 코드로 기록한다. 개인정보 원문·서명 URL·토큰을 결과표에 넣지 않는다.

## 5. 운영 검증 지표

- 발견 성공률: FOUND/NO_FILES인 source 수 / 발견 시도 source 수. 원래 미지원·QA 보류는 별도 건수.
- 다운로드 성공률: 검증된 파일 bytes 확보 건수 / 허용 대상 파일 시도 수. 제한 초과를 분모에서 숨기지 않는다.
- 완전 추출률: COMPLETE_TEXT 파일 수 / 추출 시도 파일 수. OCR_REQUIRED·PARTIAL_TEXT·UNSUPPORTED를 따로 표시.
- 종합 판정 변화: 같은 source의 base→effective 상태 분포와 관리자 재검수 건수.
- 서버 영향: 처리 중/대기 job, 최고 대기 시간, timeout/OOM, 임시 공간, worker CPU/메모리, 실제 네트워크 bytes.
- 불변조건: 제목 제외 요청 0, 개인정보·secret 로그 0, 자동 운영 활성화 0, 다른 source 근거 혼입 0.

라이브 페이지에서 링크가 열리는 것은 서버 다운로드의 증거가 아니다. 다운로드 성공은 텍스트 추출 성공과 다르며 추출 성공은 조건 해석·지원 대상 확정과 다르다.

## 6. 관리자 UX 수용 기준

- [ ] 미수집·확인된 없음·실패·부분 완료·OCR 필요가 구분된다.
- [ ] 문서별 역할·근거 위치·갱신 시각과 재시도 가능 여부를 확인할 수 있다.
- [ ] source 목록·집계·상세의 종합 상태 기준이 같다.
- [ ] 키보드만으로 파일 선택 → 원문 근거 확인 → 역할 변경 → 검수 확정 흐름이 가능하다.
- [ ] 충돌 시 작성한 입력을 보존하고 최신 근거 확인 후 다시 제출할 수 있다.
- [ ] 320px stress/360px 기준 및 200% 확대에서 주요 행동·오류 설명이 잘리지 않는다.
- [ ] 색상 외 텍스트 상태, focus-visible, 명시적 label, 필요한 상태 알림이 있다.
- [ ] 브라우저 QA는 사용자의 명시적 지시가 있을 때만 실행하고, 없으면 미실행으로 보고한다.

## 7. 구현 후 실행할 검증 명령

아래는 예정 명령이며 이번 문서 작업에서 실행하지 않았다. 새 테스트 클래스 이름은 구현 시 실제 이름에 맞춘다.

```powershell
.\gradlew.bat test --tests '*AnnouncementAttachment*' --tests '*DocumentTextExtractor*'
.\gradlew.bat test --tests '*AnnouncementSource*' --tests '*MigrationContractTest'
.\gradlew.bat test bootJar
.\gradlew.bat flywayIntegrationTest
```

추출 CLI용 산출물/테스트 task는 구현 시 build.gradle에 명시하고 위 검증에 추가한다. PostgreSQL 테스트에는 격리 DB와 적절한 환경변수가 필요하다. stopped Docker/DB나 브라우저를 문서 검증을 위해 시작하지 않는다. 임시 자원은 해당 작업이 만든 것만 정리한다.

## 8. 설계 작업 검증 기록

- 코드·migration·화면 구현 테스트: 미실행, 설계만 수행.
- 실제 첨부 확보·추출/운영 QA: 미실행.
- 문서 구조·상대 링크·Git diff 점검: 통과. ATT-001~ATT-062는 실행 예정 시나리오이며 통과한 테스트 수가 아니다.
