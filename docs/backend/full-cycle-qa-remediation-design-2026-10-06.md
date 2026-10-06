# 사내비 관리자→사용자 전체 사이클 QA 상세 수정 계획

- 작성일: 2026-10-06 KST
- 단계: 설계. 코드 구현·운영 데이터 보정·배포는 미실행.
- 저장소 기준: `codex/attachment-three-stage-linux-qa`, `48f2b3505842232d6301bbb529691f2036b5c888`
- Flyway 기준: V91 `add_source_body_refresh_previews`. 기존 migration은 수정하지 않는다.
- QA 근거: `output/qa/full-cycle-20261006/qa-results.md`의 **로그인 후 실행 결과 추가 기록** 및 같은 폴더의 최종 DOCX. 앞부분 중간 기록을 최종 상태로 사용하지 않는다.
- 근거 공고: SRC-017843 → ANN-000070 → APP-000003. 이 번호는 결함 재현 근거이며 운영 일괄 변경 대상 목록이 아니다.

## 1. 목표·범위·설계 기준

관리자가 확보된 수집 자료를 검수해 비공개 초안을 생성하고, 명시적 승인 후 사용자 후보·신청·결과까지 일관되게 연결되도록 한다. 사용자가 마지막 단계 행동을 저장해도 이미 확정한 결과를 잃지 않고, 숨김 공고는 현재 후보에서 즉시 사라져야 한다.

Design read: 기존 Thymeleaf·Bootstrap 화면과 공통 스타일을 유지한다. 기술 이력과 정책 용어를 늘리지 않고, 관리자에게는 자료 확인→검수→초안 만들기, 사용자에게는 현재 후보→신청 진행→결과 이력만 명확하게 제공한다.

위험 우선순위는 데이터 무결성·권한 → 과업 성공·오류 회복 → 일관성 → 효율 → 시각 표현이다. 일반 입력은 R1, 승인·결과·전환 및 운영 보정은 R2로 관리한다. 관리자 최종 검증, 외부 공고 자동 활성화 금지, mock 결제, 수동 서류 입력을 보존한다.

### 이번 설계 범위

- [x] QA 기록과 현재 서비스·Mapper·화면 코드를 대조한다.
- [x] 원인이 확인된 결함과 추가 재현이 필요한 현상을 구분한다.
- [x] 수정 단위·DB/API 영향·회귀 테스트·완료 Gate를 정의한다.
- [ ] 다음 지시 후 로컬 구현과 테스트를 수행한다.
- [ ] 별도 승인 후 커밋·푸시·배포 및 운영 브라우저 재검증을 수행한다.
- [ ] 운영 데이터 보정이 필요한 경우 별도 대상·변경 전후값을 제시하고 승인을 받는다.

이번 설계만으로 크롤러 개선, 전체 지역 재수집, 첨부 정책 활성화, 재분류, 실제 결제, 외부 기관 접수 또는 개인정보 수정 권한을 추론하지 않는다.

## 2. 발견 사항과 원인 확정 수준

| ID | 현상 | 코드 근거·확정 수준 | 조치 우선순위 |
|---|---|---|---|
| F01 | 관리자 승인 후 사용자 마지막 행동에서 APPROVED→WAITING_RESULT | `ApplicationProgressServiceImpl.updateProgressStepAction`의 마지막 단계 분기가 결과와 무관하게 WAITING_RESULT 저장. 원인 확정. 접수 저장 Mapper도 같은 상태를 강제하므로 함께 보완. | 1: 결과 무결성 |
| F02 | 숨김·NOT_MATCHED 공고가 사용자 후보와 대시보드에 잔존 | 사용자 BASIC 조회에 상태·공고 공개 조건이 없고 대시보드 후보 집계도 공개 조건 누락. 원인 확정. | 2: 후보 노출 |
| F03 | 첨부 상세에서 전환 차단, 수집 목록에서는 초안 생성 | 첨부 검수 필수 여부에 따라 첨부 엄격 경로와 기본 분류 경로가 의도적으로 분리됨. 화면의 작업 가능 안내가 이 분기를 반영하지 못함. 정책 우회라고 단정하지 않는다. | 3: 전환 흐름 |
| F04 | 전환 직후 생성된 초안이 아니라 빈 공고 입력 폼 표시 | 목록 JS는 announcementCode 전달, 입력 JS는 announcementId만 처리. 원인 확정. | 3: 전환 흐름 |
| F05 | 본인 신청·육아휴직 언급으로 개인·자녀 태그 제안 | 실제 과분류 재현 확인. 정확한 규칙·문맥 발생 원인은 추가 fixture 분석 필요. | 5: 분류 정확도 |
| F06 | 사용하지 않은 기본 선택 조건 행이 저장을 막고 마지막 행 삭제 불가 | 빈 기본 행과 직렬화·삭제 UI를 개선해야 함. 서버 전체 조건 저장 검증은 유지. | 4: 입력 |
| F07 | 직원 수 조건 0을 모호한 메시지로 거부 | 조건 DTO는 수치 기준값 `> 0` 계약. 0 허용으로 정책을 변경하지 않고 구체적 오류를 제공. | 4: 입력 |
| F08 | 유효 동적 입력도 첫 저장은 실패, 재조회 후 성공 | 현상 확인. 최초 요청 payload·응답을 확보하지 못해 원인 미확정. 비동기 로딩/재렌더링은 검증할 가설. | 4: 입력 |
| F09 | 단계 시간과 다른 화면 시간의 9시간 차이 | 단계 ViewController는 OffsetDateTime을 지역 변환 없이 format. 서버 응답 offset과 함께 재현해 KST 표시를 검증해야 함. | 4: 표시 |
| F10 | 승인 1건인데 ‘누적 진행 건수’ 0건 | dashboard.html은 progressMetrics[0], Controller는 해당 값에 inProgressCount 사용. 라벨·집계 의미 불일치 확인. | 4: 집계 표현 |

ASSIGNED→COMPLETED 직행 거부, 필수 체크리스트·접수·결과 미입력 시 진행 차단은 QA에서 확인한 정상 정책이다. 이를 우회하지 않고 다음 행동 안내만 개선한다.

## 3. W1 — 신청 결과 상태 보존과 동시성

### 3.1 상태를 분리해서 취급한다

단계 완료는 `application_step_states`, 확정 결과는 `result_code`·결과 일자·금액, 현재 업무 상태는 진행 status로 관리한다. 마지막 단계가 끝났다는 이유로 결과를 지우거나 모든 진행을 COMPLETED로 바꾸지 않는다.

| 이벤트 | 현재 결과 | 저장 후 원칙 |
|---|---|---|
| 일반 단계 완료, 다음 단계 있음 | 없음 | 기존 정책대로 다음 단계 READY, 진행 IN_PROGRESS |
| 일반 단계 완료, 마지막 단계 | 없음 | 단계 COMPLETED, current step 없음, 진행 WAITING_RESULT |
| 일반 단계 완료 또는 접수 정보 수정 | APPROVED / REJECTED / SUPPLEMENT_REQUESTED / STOPPED | 유효한 기존 결과 상태·일자·금액을 유지하고 해당 단계/접수 필드만 변경 |
| 관리자 결과 저장·정정 | 허용된 결과 | 기존 권한·입력 검증 후 상태와 결과를 한 transaction에서 일치시킴 |
| 명시적 중단 행동 | 별도 정책 | 기존 중단 정책과 감사 이력을 유지. 일반 완료와 구분하고 결과가 있는 경우의 허용 전이를 명시적으로 테스트 |
| 상태·결과가 이미 모순된 행 | 불일치 | 일반 저장으로 임의 보정하지 않음. 명시적 충돌 메시지 및 대상 검토로 분리 |

결과 선입력 후 사용자가 마지막 행동을 완료하는 현재 정상 업무를 차단하지 않는다. SUPPLEMENT_REQUESTED 상태의 재개 역시 별도 기존 전이 정책을 따르고, 단순 접수 수정/일반 완료를 결과 변경 명령으로 취급하지 않는다.

### 3.2 구현 방향

1. ServiceImpl에 일반 행동 상태 결정과 결과 변경 정책을 공통화한다. Controller/DAO에 업무 분기를 두지 않는다.
2. `updateProgressStepAction`, `updateProgressReceipt`, `updateProgressResult` 및 관련 상태 변경 메서드를 전수 점검한다. 접수 Mapper의 WAITING_RESULT 상수도 제거하고 서비스가 검증한 상태를 binding한다.
3. 진행 부모 행을 `SELECT ... FOR UPDATE`로 먼저 잠근 다음 최신 결과를 읽고 권한·단계·입력을 검증한다. 관련 mutation의 lock 순서를 진행→단계/체크리스트로 통일한다. 잠금 도입 전 실제 호출 관계와 교착 위험을 확인한다.
4. 행동 로그·단계·현재 단계·진행 상태·감사 로그를 같은 transaction으로 묶고 영향 행 수를 확인한다. 실패하면 부분 변경을 남기지 않는다.
5. 중복 클릭은 기존 409/단계 잠금 계약을 유지한다. 화면은 버튼을 비활성화하고 충돌 시 최신 상태를 재조회한다. 같은 행동 로그를 두 번 생성하지 않는다.

### 3.3 테스트

- 결과 4종 각각에 대해 마지막 행동 후 상태·결과·일자·금액 보존.
- 결과 없음→WAITING_RESULT 및 다음 단계 READY 전이.
- 승인 금액 0원과 양수 보존, 비승인 금액 입력 거부.
- 결과 저장과 마지막 행동, 결과 저장과 접수 수정의 양방향 동시 실행. 실제 PostgreSQL 잠금/transaction으로 검증.
- 중복 클릭, 권한 없는 사용자, 다른 소유자의 진행 접근, 로그 저장 실패 rollback.
- 관리자 결과 정정 및 명시적 중단 정책 회귀. 상태 모순 fixture를 정상 경로로 위장하지 않음.

## 4. W2 — 현재 후보와 신청 이력 분리

### 4.1 사용자 현재 후보 조회 조건

후보 목록·대시보드 카드·후보 건수·후보 금액에 같은 조건을 적용한다.

- 본인 소유 BASIC 매칭.
- 현재 후보로 허용된 매칭 상태만 포함. 기존 대시보드의 MATCHED·REVIEW_REQUIRED·PROGRESSED 범위를 출발점으로 유지하고 NOT_MATCHED 등 비후보 상태는 제외한다. PROGRESSED를 신규 신청 가능 후보로 오인시키지 않고 기존 진행으로 연결한다. 이를 목록에서 완전히 제거하는 정책 변경은 별도 결정한다.
- 공고 승인 APPROVED, 수동 상태 NORMAL.
- 기존 후보 생성과 동일하게 시작일이 없거나 사업 기준일 이하, 종료일이 없거나 사업 기준일 이상. 종료일 당일 포함, 시작 전 공고는 현재 생성 정책대로 제외한다.
- 페이지 total과 실제 items는 SQL의 동일 WHERE를 사용한다. pagination 후 JavaScript/Java에서 숨김 행을 제거하지 않는다.

공고 숨김 직후 새 조회에서 제외되어야 하며 사용자 기본정보 재저장·재매칭·다음 배치 실행에 의존하지 않는다. 후보 생성 쿼리와 노출 쿼리가 사용하는 사업 기준일은 Asia/Seoul로 일치시킨다. DB CURRENT_DATE의 timezone 의존은 현재 설정을 확인하고, 가능하면 Clock 기반 LocalDate를 서비스에서 산출해 DAO에 binding한다.

### 4.2 이력·계약 보존

- 관리자 매칭 전체 이력/일반 검색의 의미는 바꾸지 않는다. 전용 사용자 현재 후보 DAO·조회 조건으로 분리한다.
- 숨김·기간 종료 후에도 기존 신청 진행·접수·결과는 소유자 이력 화면에서 보존한다. 새 신청 가능 여부와 이미 신청한 이력을 혼동하지 않는다.
- 공개 상태가 바뀐 공고를 최종 선택/신청 생성하는 mutation도 서버에서 재검증한다. 목록을 먼저 열어 둔 뒤 숨김 처리된 경우 stale 화면에서 신청이 시작되지 않아야 한다.
- 공개 복구 시 현재 매칭·조건 버전의 유효성을 확인한다. 무효한 옛 후보를 무조건 복원하지 않는다.
- 과거 알림·감사·신청 행을 삭제하거나 일괄 NOT_MATCHED로 변경하지 않는다.
- 응답 필드와 wrapper는 유지한다. 현재 후보 전용 V1 응답의 잘못된 노출을 정정하되 일반 이력 API의 필터 의미를 바꾸지 않는다. 기존 계약 문구와 다르게 의미가 바뀌면 구현 전에 V2 필요성을 보고한다.

### 4.3 테스트

숨김/승인 취소/기간 종료/시작 전/NOT_MATCHED/타 사용자/동일 공고의 BASIC·FINAL fixture, 빈 날짜, KST 자정 및 종료일 경계, 첫 페이지와 다음 페이지 total 일치, 목록·카드·집계 금액 일치, 완료 신청 이력 보존, 조회 후 숨김→신청 쓰기 거부를 검증한다.

## 5. W3 — 검수와 초안 전환을 하나의 작업 흐름으로 연결

### 5.1 서버가 제공하는 작업 가능 정보

기존 `/app/admin/collected-announcements/{sourceId}/attachments`의 간결한 검수 화면을 기본 작업 공간으로 유지한다. `tools=true` 기술 상세는 보조 화면으로 남긴다. 수집 목록과 상세는 동일 서버 판정의 작업 가능 정보를 사용한다.

| 내부 모드(설계용) | 판정 기준 | 관리자 안내·행동 |
|---|---|---|
| BASE_REVIEW | 해당 source의 attachmentReviewRequired=false이며 기존 기본 분류 경로가 허용됨 | ‘본문 기준 검수 가능 · 첨부 판정은 적용되지 않음’. 확보된 본문·분류를 확인하고 기존 기본 검수/초안 경로 사용 |
| ATTACHMENT_REVIEW | 해당 source의 attachmentReviewRequired=true | 최신 첨부 판정·정책·sealed set·hash·버전·필수 확인 항목을 충족한 엄격 경로 사용 |
| BLOCKED | 제외/보관/중복/필수 자료 실패/진행 중/판정 만료 등 해당 경로의 차단 조건 | 정확한 이유와 허용된 다음 행동만 표시. 정상 후보나 전환 가능으로 표현하지 않음 |
| ALREADY_LINKED | 기존 전환 공고 존재 | 기존 초안/공고 열기. 중복 생성 버튼 없음 |

이 내부 모드명을 기존 enum에 즉시 추가한다는 뜻은 아니다. 구현 시 기존 V2 projection에 작업 가능 여부·허용 경로·차단 사유·연결 공고 ID를 additive하게 제공하거나 별도 V2 조회 계약을 정의한다. V1 enum 의미와 응답 구조는 유지한다.

### 5.2 보존할 안전장치

- COLLECT_ONLY를 ENFORCE로 변경하거나 미적용 첨부 판정을 자동 적용해 UI 오류를 해결하지 않는다.
- source별 첨부 검수 필수 플래그를 기준으로 한다. 현재 전역 모드가 변경됐다고 기존 필수 검수 기록을 기본 경로로 우회하지 않는다.
- BASE_REVIEW도 기존 분류 확정 버전·권한·중복·본문 확보 정책을 검사한다. 본문 수집 실패는 일반 정상 검수 목록과 기술 오류 목록을 구분한다.
- ATTACHMENT_REVIEW의 source/decision/attachment 버전, set hash, SEALED 상태, 최신성, 중복 방어는 완화하지 않는다. 검수 화면 열람 후 자료가 변경되면 409와 재조회 안내를 제공한다.
- 원문 직접 확인 예외는 이미 허용된 정책 안에서만 제공한다. ‘직접 확인’을 클릭했다고 기술 실패를 성공으로 바꾸지 않는다.
- 자료→근거 확인→대상·형태 조정→검수 사유→비공개 초안 생성 순서. 최종 승인 없이 사용자에게 활성화하지 않는다.
- 단순화는 표현의 변경이다. 서버 검증이나 감사 근거를 제거하지 않는다.

### 5.3 초안 자동 불러오기

1. 모든 전환 성공 redirect를 반환된 UUID `announcementId`로 통일한다. 공고 코드는 화면 식별용으로 유지한다.
2. 전환 결과에 UUID가 없으면 성공 후 빈 신규 폼으로 이동하지 않고 오류·기존 공고 조회 경로를 제공한다.
3. 기존 announcementCode 링크는 권한이 검증된 정확한 코드 조회로 해석한다. 목록 첫 페이지에서만 찾는 방식은 금지한다. 기존 서버 조회 사용 가능성을 먼저 확인하고 없으면 호환 resolver를 설계한다.
4. ID와 code가 동시에 있고 다르면 명시적 오류. 대상 조회 실패·권한 오류에서도 신규 생성 폼으로 조용히 전환하지 않는다.
5. 상세·조건·필요 서류·단계·동적 항목 로딩을 완료하기 전에 수정/저장 버튼을 열지 않는다. 오래된 조회 응답이 새 선택 폼을 덮어쓰지 않게 요청 순서를 보호한다.

## 6. W4 — 입력·오류·시간·집계 표현

### 조건 행

- 선택 조건 0행을 허용하는 UI로 정리한다. 마지막 미사용 행 삭제가 가능해야 한다.
- 화면이 만든 초기 기본 행에서 사용자 입력이 전혀 없는 경우에만 저장 대상에서 제외한다. 부분 입력 행은 누락 필드를 구체적으로 표시한다.
- 숫자 0, boolean false 등을 일반 falsy 필터로 삭제하지 않는다. 기존 저장 행 삭제는 명시적 사용자 의도로 처리한다.
- 수치 조건의 `> 0` 계약은 유지하고 ‘직원 수 조건 기준값은 0보다 커야 합니다’처럼 조건명·필드·제약을 전달한다. 0 포함 정책 변경이 필요하면 별도 DB/API 검토를 받는다.
- 합성 결과 금액 0원과 조건 기준값 0의 서로 다른 정책을 혼동하지 않는다.

### 동적 입력

- 먼저 유효 입력 첫 저장 실패를 지연 응답·재렌더링·필수 항목 변경 fixture로 재현한다. 원인이 확인되기 전 특정 race가 원인이라고 확정하지 않는다.
- 조회 중/저장 중/성공/실패/충돌 상태를 분리한다. 필수 정의와 기존 값 확보 전 입력·저장 차단, 요청 epoch 보호, 저장 payload 고정, 실패 시 작성값 보존을 적용한다.
- 서버는 필수 항목의 표시명과 입력 방법을 한국어로 안내한다. 개인정보 값·payload 원문을 로그에 남기지 않는다.
- 관련 API가 이미 버전 충돌을 지원하는지 확인하고 필요 시 V2 additive 필드를 설계한다. V1 필수값 판정을 임의 완화하지 않는다.

### 시간과 누적 현황

- OffsetDateTime 표시 전에 동일 instant를 Asia/Seoul로 변환한다. 저장된 시간을 +9시간 덧셈으로 보정하지 않는다. LocalDate는 날짜 그대로 유지한다.
- 단계·체크리스트·서류 검증 화면의 같은 formatter 사용 지점을 점검하고 공유 가능 범위를 정한다.
- 현재 ‘누적 진행 건수’는 inProgressCount이므로 우선 ‘현재 진행 건수’로 정확히 표기한다. 진짜 전체 누적 건수가 필요하면 새 집계 필드와 포함 상태를 별도 계약으로 정의한다. 기존 inProgressCount 의미를 바꾸지 않는다.
- 승인/결과 대기 집계는 W1의 저장 상태 정합성 해결 후 검증한다. 화면에서 result_code만 참고해 저장 오류를 감추지 않는다.
- 정상 상담 전이와 접수 권한 제한에는 ‘상담 확정 후 완료 처리’, ‘운영자 접수 정보 입력 후 진행’처럼 실제 다음 행동을 안내한다.

## 7. W5 — 태그 과분류: 별도 규칙 검증 단위

‘본인 신청’은 신청 주체를 말할 뿐 개인 자격 지원대상 확정 근거가 아니며, 육아휴직 근로자 언급도 자녀가 수혜대상이라는 뜻은 아니다.

1. SRC-017843의 제목·본문·첨부 중 실제 태그 근거와 규칙 ID·적용 버전을 추적한다. 자료 전체를 문서에 복제하지 않는다.
2. 수혜대상·사업주 요건·직원 요건·신청 대리/절차 문맥을 분리한다. 현재 구간별 근거 모델을 재사용하고 단어 삭제만으로 해결하지 않는다.
3. 대상·지원형태 조합과 승인된 A/B 우선순위를 유지한다. 다중 태그·관리자 수정·최종 확인은 유지한다. 근거 부족이면 미확정으로 표시하고 자동 확정하지 않는다.
4. 사업주 사회보험료, 개인 복지, 자녀 직접 지원, 가족 대리 신청, 본인 신청 안내, 육아휴직 근로자 지원을 positive/negative fixture로 비교한다.
5. 규칙/seed 변경이 필요하면 기존 게시 버전 수정이 아니라 새 초안 버전으로 만든다. 로컬 비교 보고 후 운영 게시·기존 공고 재분류는 별도 승인한다.

W1~W4 UI/업무 흐름 수정 배포와 규칙 활성화를 묶지 않는다. 규칙 검증이 끝나지 않았으면 개선된 검수 화면에서도 관리자 태그 확인을 유지하고 W5를 미완료로 보고한다.

## 8. DB·API 및 변경 파일 계획

기본안은 **새 테이블·컬럼 없이** 기존 진행/결과/매칭/공고/검수 구조를 사용한다. row lock·조회 predicate·transaction은 migration을 요구하지 않는다. 대량 후보 조회에서 실행 계획으로 병목이 입증된 경우에만 additive index migration을 검토한다. 번호는 구현 당시 latest+1을 다시 확인한다. 현재 V91까지의 파일은 변경하지 않는다.

| 작업 | 주요 변경 후보 | 계약 영향 |
|---|---|---|
| W1 | ApplicationProgressServiceImpl.java, ApplicationProgressDao.java, ApplicationProgressMapper.xml | 기존 상태/응답/권한 유지. 일반 행동의 결과 회귀 정정 |
| W2 | MatchingServiceImpl.java, MatchingDao.java, MatchingMapper.xml, DashboardServiceImpl.java, DashboardDao.java, DashboardMapper.xml | 사용자 현재 후보 쿼리와 집계 일치. 관리자·신청 이력 보존 |
| W3 | AnnouncementSourceServiceImpl.java, AnnouncementAttachmentReviewServiceImpl.java, AttachmentLegacyPathGuard.java, 관련 V2 DTO/Controller | V1 유지, 작업 가능 projection은 V2 additive. 엄격 경로 guard 유지 |
| W3 UI | saneb-collected-announcements.js, saneb-announcement-conversion.js, saneb-announcement-input.js, announcement-conversion-review.html, collected-announcements.html | 현재 attachments 작업 공간 유지, UUID 이동 및 조회 오류 처리 |
| W4 | AnnouncementConditionsSaveRequest.java, GlobalExceptionHandler 관련 검증 처리, saneb-announcement-input.js | HTTP 상태·wrapper·수치 제약 유지. 오류 정보 구체화 |
| W4 동적 | DynamicAnnouncementInputServiceImpl.java, saneb-application-progress-input.js, 관련 DTO/Mapper | 재현 후 최소 수정. 필수 입력·권한 보존 |
| W4 표시 | ApplicationProgressViewController.java, PartnerVerificationViewController.java, DashboardViewController.java, dashboard.html | KST 표시와 집계 라벨 정정. 기존 지표 의미 유지 |
| W5 | 분류기·구간 근거 처리·키워드 초안/fixture | 원인 분석 후 파일 확정. 운영 규칙·재분류 자동 실행 없음 |

새 메서드는 select/insert/update/delete/save 명명과 Controller→Service→ServiceImpl→DAO→Mapper XML을 지킨다. SQL은 명시 컬럼·`#{}` binding·주석을 사용한다. Thymeleaf는 th:text, 기존 디자인 시스템과 의존성을 사용한다.

문서는 구현 결과에 맞춰 db-model-v1.md, api-contract-v1.md 및 기존 V2 계약 문서, full-development-roadmap-v2.md에 필요한 부분만 업데이트한다. 완료되지 않은 W5나 운영 검증을 완료로 표시하지 않는다.

## 9. 기존 데이터 정합성 점검·보정 계획 — 자동 실행 금지

1. 읽기 전용으로 결과 상태 불일치, 숨김 후보 노출 대상, 전환 중복, 잘못 연결된 공고를 건수·비식별 ID로 조사한다. 실제 운영 조회는 승인 범위에서 수행한다.
2. 각 행의 마지막 관리자 결과 저장과 이후 사용자 행동 로그를 비교한다. 명시적 중단·정정·보완 재개를 일반 회귀와 구분한다.
3. 변경 대상 ID, 변경 전후 상태, 근거, 제외 사유, 영향받는 집계를 preview로 제시한다. `status=result_code` 일괄 UPDATE는 금지한다.
4. 별도 승인 후 좁은 transaction/CAS 조건으로 확정된 대상만 수정한다. 수행 당시 버전/상태가 preview와 다르면 건너뛰고 보고한다. 감사 metadata에는 비식별 변경 정보만 남긴다.
5. rollback은 대상 ID와 이전 상태에 한정하고 이후 합법적 변경이 있으면 자동 역수정하지 않는다. 개인정보 원문·비밀번호·토큰을 preview나 로그에 기록하지 않는다.

숨김 후보 문제는 우선 조회 수정으로 해결한다. 매칭/신청 이력 삭제나 전 지역 재매칭을 선행하지 않는다. ANN-000070은 QA 이력임을 유지하고, 이미 관리자 재저장으로 복구한 APP-000003을 다시 보정한다고 가정하지 않는다.

## 10. 검증 계획과 예정 명령

아래 명령은 **향후 구현 검증 계획**이며 이번 설계에서 실행한 제품 테스트가 아니다. Java 21·Node 경로와 로컬 테스트 DB 격리를 먼저 확인한다. 운영 DB 연결값으로 통합 테스트를 실행하지 않는다.

```powershell
.\gradlew.bat test --tests '*ApplicationProgressServiceImplTest' --tests '*ApplicationProgressSmokeIntegrationTest' --tests '*MatchingSmokeIntegrationTest' --tests '*DashboardServiceImplTest' --tests '*AnnouncementSourceServiceImplTest' --tests '*AnnouncementAttachmentReviewServiceTest' --tests '*DynamicAnnouncementInputSmokeIntegrationTest' --no-daemon
.\gradlew.bat test --tests '*MigrationContractTest' --tests '*FlywayMigrationIntegrationTest' --no-daemon
.\gradlew.bat test bootJar --no-daemon
node --test scripts/qa/attachment-conversion.test.mjs scripts/qa/attachment-review-ui.test.mjs scripts/qa/collected-navigation.test.mjs
```

기존 suite에 필요한 서비스·View·Mapper 테스트와 Node DOM/지연 응답 테스트를 추가한다. 동시성·pagination·DB timezone 검증은 mock 통과로 대체하지 않는다. Docker/임시 PostgreSQL을 사용할 수 없으면 로컬 DB Gate를 미검증으로 남기고 승인된 격리 환경을 별도 요청한다. 생성한 Node/Gradle/임시 DB 자원만 종료하며 사용자 브라우저·서버는 임의 종료하지 않는다.

### 향후 명시적 승인 후 운영 브라우저 회귀 시나리오

1. 관리자 수집 목록과 상세에서 같은 작업 가능 여부·이유 확인. BASE/필수 첨부/판정 만료/기존 전환 공고 각각 확인.
2. 실제 확보 자료→태그 수정→검수 사유→초안 생성. 자동 활성화 없음, 중복 생성 없음.
3. 즉시 생성된 공고가 입력 화면에 불러와짐. 실패 시 빈 신규 폼으로 저장되지 않음.
4. 선택 조건 없이 수치 조건만 저장, 빈 기본 행 삭제, 0 오류의 정확한 안내, 저장값 재조회.
5. 조건·서류·단계·동적 입력 설정→승인 요청→관리자 수동 승인.
6. 사용자 기본정보 후보 확인→mock 구독→상담→수기 배정→수동 서류 값→정밀 후보→관리자 선택.
7. 동적 입력 첫 유효 저장, 필수 항목·체크리스트 차단, 접수 입력, 관리자 결과 저장, 사용자 마지막 행동.
8. 새 조회에서 결과 상태·승인 건수·결과 대기·합성 금액·KST 시간 일치. 관리자 재저장으로 복구하는 우회 없이 통과.
9. 공고 숨김→사용자 후보/카드/건수/금액 즉시 제외, 완료 진행/결과 이력 보존. stale 후보 화면에서 새 신청 차단.
10. 오류 복구·중복 클릭·권한·키보드·loading/empty/error/success/disabled 상태 및 375/768/1024/1440 반응형. 기존 모바일 360px 구조도 유지.

실제 카드 승인·외부 기관 접수·실제 수령액 변경은 포함하지 않는다. 기존 수집 자료로 QA했다는 사실을 신규 크롤링 성공으로 표현하지 않는다.

## 11. 단계·Gate·성공/실패 기준

| Gate | 상태 | 통과 기준 | 실패 또는 중단 기준 |
|---|---|---|---|
| G0 설계 | [x] | 근거·원인 확정 수준·변경 범위·정책 불변식·검증 계획 기록 | 미확정 원인을 확정으로 표현, 기존 정책 우회를 설계에 포함 |
| G1 결과 무결성 | [ ] | W1 단위·실DB 동시성·권한·rollback 통과 | 마지막 행동/접수 수정이 결과 회귀, 중복 로그·부분 저장 |
| G2 후보 노출 | [ ] | W2 목록·count·카드·금액 동일 조건, 이력 보존, stale 신청 차단 | 재매칭해야 숨김 제거, pagination total 불일치, 이력 삭제 |
| G3 검수→초안 | [ ] | W3 모드별 동일 안내·서버 guard·UUID 로딩·중복 방어 통과 | 정책 강제 활성화, 첨부 hash 검증 우회, 빈 폼 저장 |
| G4 입력·분류 | [ ] | W4 입력 첫 저장/오류/시간/라벨 회귀 통과, W5 fixture·변경 규칙 검증 | 동적 입력 원인 미해결, 수치 계약 무단 완화, 과분류를 관리자 수작업만으로 완료 처리 |
| G5 로컬 전체 회귀 | [ ] | 관련 suite·전체 test·bootJar·migration 계약, 변경 문서 대조 | DB 테스트 생략을 성공 처리, 기존 migration 수정, V1 의미 파괴 |
| G6 운영 전체 사이클 | [ ] | 별도 배포·QA 승인, 동일 배포 버전 확인, 관리자→사용자 1사이클과 숨김/이력까지 증거 확보 | 로컬 성공만으로 운영 완료 선언, 관리자 재저장 우회 필요, 미검증 항목을 통과 처리 |

G0 이후 6개 Gate가 남아 있다. 이는 구현률이 아니라 검증 단계 수다. 구현 공수나 완료율은 아직 산출하지 않는다. W5를 후속 릴리스로 분리할 수 있으나 이 경우 전체 수정 완료는 선언하지 않고 규칙 검증 미완료를 명시한다.

최종 보고는 변경 파일·SHA·실행 명령·실제 통과/실패/생략 수·DB/배포 버전·브라우저 증거·남은 위험을 구분한다. 규칙 게시·재분류·기존 데이터 보정·배포 승인은 각각 실제 영향을 명시해서 받는다.

## 12. 이번 설계의 확인 결과

- [x] 루트 AGENTS.md와 적용 skill·필수 기준을 확인했다.
- [x] QA 최종 추가 기록, 상태 변경 서비스/Mapper, 후보·집계 조회, 전환/초안 JS, 검증 DTO, 시간 표시·집계 라벨을 대조했다.
- [x] Git branch/HEAD 및 기존 미추적 파일을 확인하고 보존했다.
- [x] Node 읽기 전용 문서 검사 12/12 통과: 12개 절, 7개 Gate, 10개 발견 사항, QA·V91 근거 경로, 상태/정책/계약 불변식 및 미실행 명시를 확인했다. Node는 종료 코드 0으로 종료했다. 해당 결과는 제품 테스트와 구분한다.
- [ ] 제품 코드·migration·운영 데이터 변경: 미실행.
- [ ] 제품 test/bootJar·AWS/GitHub·운영 조회: 이번 설계에서는 미실행.
- [ ] 브라우저 검증: 현재 요청이 설계이므로 사용자 정책에 따라 미실행.

이 문서는 `long-goal-operating-protocol`의 Gate/실패 기준, `ui-ux-operating-principles`의 데이터 무결성·오류 회복 우선순위, `frontend-design-core`의 기존 시스템 재사용 기준을 반영했다. 새 UI 라이브러리나 기술스택 변경은 제안하지 않는다.
