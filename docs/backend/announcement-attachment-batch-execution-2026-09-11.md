# 기존 공고 첨부 배치 실행 계약

현재 Gate 5는 **부분 구현 / Not ready**다. 범위 미리보기·고정 예약·수집 전 취소·수집 시작/중지/재개·worker 집계·봉인 결과 preview/명시적 선택 이력과 조회 API, 적용 승인/중지/재개·항목별 CAS 처리·결과 조회 및 조건부 batch 원복 승인/작업자를 구현했다. 배치 UI·운영 전체 분할 집계·일반 자동 ENFORCE 원복은 필수 미완료다. 실제 PostgreSQL/Linux/운영 실행은 아직 검증되지 않았다.

## 범위와 완료 기준

- 전체 목표는 승인된 기존 데이터 전체를 성공/검수/실패/충돌/보호 제외로 설명하는 것이다. 100건 또는 1,000건 배치 하나의 성공으로 전체 완료를 선언하지 않는다.
- 최초 운영 배치는 상세 설계대로 최대 100건부터 제시한다. API 한도는 1~1,000건이며 요청자가 maximumCount를 명시해야 한다. 상한을 넘는 잔여 후보는 remainingCount로 별도 표시한다.
- 배치 생성은 외부 요청/정책 적용 승인이 아니다. 운영 수집 시작·적용 전 대상 ID/건수, 최대 HTTP/bytes, 현재 보호 항목과 복구 방법을 제시하고 해당 실행 범위를 승인받는다.
- 일반 자동/수동 첨부 수집 및 기존 검수 경로는 유지한다. 배치 예약은 기존 source의 버전·current 판정·확인·정책 binding·운영 공고를 수정하지 않는다. 같은 원문의 다른 첨부 수집은 활성 예약 UNIQUE 때문에 충돌할 수 있으며 수집 전 취소로 해제할 수 있다. 기존 현재 판정의 검수는 batch job을 일반 수집 job으로 취급하여 막지 않는다.

## 범위 미리보기 — HTTP 0회

`POST /api/v2/admin/announcement-attachment-batches/scope-preview`는 JSON 필터를 받는 read-only REPEATABLE_READ 조회다. ADMIN/OPERATOR/APPROVER, CSRF가 필요하다. 이 POST는 DB 쓰기·job 생성·원본 다운로드·parser 실행이 없다.

| 필드 | 계약 |
|---|---|
| policyId | 현재 ACTIVE 규칙의 게시 COLLECT_ONLY 또는 ENFORCE 정책 |
| providerCodes | BIZINFO/GOV24/LOCAL_GOV_NOTICE 중 중복 없는 1~3개. 서버에서 정렬 |
| collectedFrom/collectedBefore | ISO offset 시각, 시작 포함·종료 제외. 시작 < 종료이며 UTC로 정규화 |
| deadlineFrom/deadlineThrough | 선택적 마감일 범위, 양끝 포함. 역전 금지. 지정 시 마감일 NULL 행은 해당 범위에 포함하지 않음 |
| maximumCount | 1~1,000. 초기 운영 실행은 100건 이하부터 승인 요청 |

QA 데이터·이미 삭제된 원문은 조회 대상이 아니다. 남아 있는 PRODUCTION 범위를 provider별로 TITLE_OR_BASE_NOT_ELIGIBLE, LINKED_PROTECTED, CANDIDATE의 상호 배타적 건수로 집계한다. 제목/본문/URL을 목록 SQL로 가져오지 않는다. V70 삭제 원문의 URL을 복원하거나 역조회하지 않는다.

후보는 수집 시각 오름차순·UUID 순서로 maximumCount까지만 선택한다. 전체 candidateCount, selectedCount, remainingCount를 구분하며 나머지가 자동 처리된다고 표시하지 않는다. 선택 후 규칙 불일치/진행 작업/미지원 출처가 발견돼도 다음 후보로 바꿔 채우지 않는다. 해당 ID와 준비 불가 사유를 유지하고 전체 예약을 차단한다.

준비 사유는 READY, BASE_RECLASSIFICATION_REQUIRED, ACTIVE_JOB, VERSION_LIMIT, PROFILE_REQUIRED다. ACTIVE 정책의 엔진·추출기 버전/설치 지문/최대 80 MiB 설정과 서버 등록 profile 연결을 검증한다. 출처 연결은 URI 구성 검증만 하고 HTTP는 호출하지 않는다. 관리자가 URL·parser·profile·sourceIds·includeLinked·성공값·HTTP 상한을 임의 지정할 수 없다.

scopeHash는 정규화 필터, 정책 입력, 전체 집계, 선택된 source/content/base/기존 current·확인/binding/버전, 실행 profile·설정과 저장된 출처 연결의 지문에 묶인다. 지문 계산을 위해 읽은 URL은 응답·scope metadata에 저장하지 않는다. 실제 파일 수·판정 변경 건수는 아직 모르므로 제공하지 않는다. 응답의 요청량은 선택 건수 × 파일 최대 10개/최대 3시도/리다이렉트 제한에 따른 **132 HTTP 및 정책별 최대 bytes 상한**이다. 실제 이번 preview 요청량은 0이다.

## 범위 고정·조회·취소

1. ADMIN이 `{scope, expectedScopeHash, reason}`과 UUID Idempotency-Key로 POST한다. 같은 actor·정규화 입력·키는 같은 배치를 반환하고 조건을 재실행하지 않는다. 다른 actor/입력/키 재사용은 409다.
2. 멱등 키 → 선택 source UUID 순서 잠금 → 정책/규칙 공유 잠금 뒤 범위를 다시 계산한다. 대상·집계·정책·기존 확인·출처 연결·버전 차이가 있으면 409이며 일부만 예약하지 않는다.
3. 한 transaction에서 BACKFILL/SCOPE_READY batch와 정확한 source별 SCOPE_READY jobs를 생성한다. scope JSON에는 필터·집계·상한/정책 hash만 저장하며 source ID 목록을 복사하지 않는다. source identity는 삭제 cascade되는 jobs에만 둔다. 사유는 hash만 감사 metadata에 남긴다.
4. SCOPE_READY는 worker claim/HTTP 대상이 아니다. 각 job은 고정 base/content/규칙·정책, 기존 source/첨부 버전, 이전 binding/current/confirmation, profile/추출기 설정, 요청 지문과 예산을 갖는다. 원문·기존 검수·판정은 변경하지 않는다.
5. GET 목록/상세/items는 고정된 jobs와 삭제 건수를 읽는다. 최초 필터를 다시 실행해 새 source를 포함하지 않는다. 목록과 items는 pagination/no-store를 제공한다.
6. 수집 전 취소는 PUT `/{batchId}/scope-cancellation`에 `{expectedVersion, reason}`을 받는다. ADMIN·CSRF·현재 batch rowVersion·SCOPE_READY를 확인하여 batch와 남은 jobs를 CANCELLED로 바꾼다. 이력 삭제나 이전 source 상태 복원을 하지 않는다. 이미 실행 중인 배치를 이 endpoint로 취소/원복할 수 없다.

## DB 무결성과 개인정보 정리

V73(운영 미적용)에 scope_item_count/policy_snapshot_json, batch-policy composite binding, jobs.frozen_provider_code, 목록/범위 인덱스와 trigger를 추가했다. V1~V72는 수정하지 않았다.

- 고정 범위/정책 snapshot/최초 요청/생성 identity는 불변이다. 관리 batch DELETE와 취소 상태 되돌리기는 거부한다.
- 고정 itemCount = 현재 job 수 + deletedItemCount를 지연 constraint trigger로 확인한다. 최초 저장에서 jobs 일부 누락 또는 범위 밖 추가로 이 합계를 맞추지 못하면 transaction 전체가 실패한다.
- SCOPE_READY batch에는 SCOPE_READY jobs만, CANCELLED batch에는 CANCELLED jobs만 남아야 한다. 예약만 한 batch의 job을 직접 PENDING으로 바꾸어 수집을 시작할 수 없다.
- 원문/content/base 삭제로 jobs가 cascade되면 삭제 건수와 batch rowVersion만 증가시킨다. source ID/URL/텍스트를 tombstone에 복사하지 않으며 새 원문으로 보충하지 않는다.
- job의 policy는 batch의 policy와 composite FK로 일치해야 한다. frozen provider와 기존 job 실행 입력도 수정할 수 없다.

## 명시적 수집 시작·중지·재개

세 endpoint는 ADMIN·CSRF·현재 rowVersion·no-store/ApiResponse를 요구한다. POST 범위 예약의 Idempotency-Key와 달리 실행 제어는 상태와 버전 CAS다. 응답을 잃으면 GET 상세를 확인한다. 같은 이전 버전의 요청을 반복해도 실행을 중복 생성하지 않으며 409를 반환한다.

| PUT 경로 | 요청 | 전이 |
|---|---|---|
| `/{batchId}/collection` | 아래 수집 확인 입력 | SCOPE_READY → COLLECTION_PENDING, 같은 transaction에서 고정 jobs만 PENDING |
| `/{batchId}/collection-pause` | expectedVersion, reason | COLLECTION_PENDING/COLLECTING → COLLECTION_PAUSED |
| `/{batchId}/collection-resume` | 아래 수집 확인 입력 | COLLECTION_PAUSED → COLLECTING; 기존 jobs·예산·시도·완료 상태 보존 |

수집 확인 입력은 expectedVersion, expectedScopeHash, expectedItemCount, expectedDeletedItemCount, expectedMaximumDownloadBytes, expectedMaximumHttpRequests, reason이다. 클라이언트가 cap을 설정하는 것이 아니라 상세 frozenScope의 **최초 전체 상한**과 대상/삭제 건수를 확인한다. 재개 시 남은 예산을 새 예산처럼 추가하지 않는다. 미정의 필드·임의 source/profile/URL은 거부한다.

시작/재개는 고정 jobs의 source UUID 순서 잠금 → 게시 정책/규칙 공유 잠금 → batch 잠금 후 검증한다. 최초 필터를 재실행하지 않는다. 정책 전체 snapshot, 현재 base/content/규칙, source/첨부 버전, 이전 current/검수/정책 binding, 등록 profile/추출기 설정, 출처 연결 지문 및 source별 bytes가 다르면 전체 제어 요청을 409로 거부한다. 진행 이후 임의로 보호 공고를 포함하거나 다른 원문으로 채우지 않는다.

- 최초 시작 전에 삭제된 항목이 있으면 시작하지 않는다. 범위 예약 취소 후 남은 범위를 새로 고정해야 한다.
- 최초 시작은 collection_started_at, approved_by, collection_approval_hash로 승인 identity를 저장하고 변경을 금지한다. 이는 실제 외부 요청/성공 기록이 아니다. API가 worker 설정을 켜거나 정책을 게시하지 않는다.
- 중지는 새 claim/HTTP를 batch gate에서 차단한다. 이미 전송 중인 요청의 즉시 중단은 보장하지 않는다. 현재 파일/임시 자원 정리와 봉인 결과 저장은 끝날 수 있다. 작업 예산·시도·checkpoint는 지우거나 환급하지 않는다.
- 재개는 현재 버전과 삭제 건수를 새로 확인한다. 완료/실패/충돌/취소된 jobs는 재시도하지 않고 미완료 jobs만 고정 입력을 재검증한다. 중지 중 모든 대상이 삭제됐다면 재개 후 PARTIAL_FAILED 집계로 종료하며 성공 0건을 성공 배치로 표현하지 않는다.
- `frozen_locator_hash`는 PostgreSQL 정규화 지문이며 원문 URL을 job에 복사하지 않는다. 매 HTTP 직전 출처/이전 검수 binding/보호 연결을 다시 확인한다. claim에서 이 입력이 바뀌면 CONFLICT/FROZEN_INPUT_CHANGED로 끝내 무한 재대기를 막는다. source/base 버전 변경은 기존 SOURCE_VERSION_CHANGED와 구분한다.
- 각 시작/중지/재개 감사에는 동작·batch ID·사유 hash만 기록한다. 사유·URL·원문을 감사 metadata에 남기지 않는다.

## worker 진행 집계

기존 worker enabled 설정이 true일 때 15초 간격의 DB 전용 scheduler가 상태 변경 가능한 배치를 최대 100개 SKIP LOCKED로 집계한다. 대기 중인 100개가 뒤의 완료 배치 집계를 막지 않도록 변경 후보만 선택한다. 별도 파일/HTTP·parser·자동 apply를 실행하지 않는다.

1. 실제 시도가 있는 COLLECTION_PENDING은 COLLECTING이 된다.
2. 고정 itemCount = 남은 jobs + deletedItemCount이고 모든 남은 job이 terminal일 때만 수집 종료 상태가 된다.
3. 전체 항목이 SUCCEEDED이고 각각 SEALED set·preview evaluation/hash를 가진 경우만 COLLECTED다. 실패/충돌/취소/삭제 또는 불완전 근거가 하나라도 있으면 COLLECTION_PARTIAL_FAILED다.
4. COLLECTION_PAUSED는 집계가 자동 해제하지 않는다. GET은 실제 jobCounts와 삭제 수를 항상 별도로 반환하며 batch 상태는 scheduler 주기까지 지연될 수 있다.
5. COLLECTED는 저장된 수집 근거가 완성됐다는 뜻이며 배치 preview 승인·현재 판정 적용·관리자 확인·DRAFT/활성화 완료가 아니다. 기존 평가 service의 batch 경로는 current와 검수/정책 binding을 수정하지 않는다.

## 후속 구현의 필수 Gate

- [~] 범위 조회/고정/취소: 코드·단위/HTTP/XML 검증 구현. 최신 PostgreSQL 동시성·cascade·deferred trigger 실행은 차단 상태다.
- [~] 승인된 수집 시작: 고정 입력 재검증·CAS 전환·동시성/거부 테스트 구현. 실제 PG와 운영 승인/실행 검증 필요.
- [~] 고정 집합의 수집·봉인·진행/실패/삭제 집계: 기존 worker와 연결하고 저장 분리 테스트를 추가했다. 최신 PG 및 실제 전체 파일 확인 필요.
- [~] 분류 preview는 저장된 SEALED 근거 metadata와 정확한 evaluation inputs만 사용한다. 추가 HTTP 0·고정 버전/지문·성공 항목 명시적 선택·불변 이력 API/테스트를 구현했다. 실제 PG/운영 검증은 남아 있다.
- [ ] 적용은 승인된 성공 항목만 CAS로 수행하고 실패/충돌을 전체 집계에 남긴다. 후속 검수·DRAFT 연결·신청/운영 공고는 보호한다.
- [~] 수집/적용 pause/resume와 적용 이후 변경 없는 batch pointer/binding 조건부 rollback을 구현했다. 실제 DB/UI/운영 검증은 남아 있다.
- [ ] 1,000건 초과 전체 승인 범위의 분할/중복 방지/진행 ledger 및 최종 건수 대조. 현재 remainingCount 표시는 이 전체 처리의 구현 완료가 아니다.
- [ ] 관리자 화면, 최신 PostgreSQL/Linux·실제 모든 Provider 파일·운영 배포·역할별 운영 브라우저 E2E.

현재 운영 대응은 범위 재조회 → 준비 사유 해결 → 고정 예약 → 정확한 실행 영향 승인 → 수집 시작 → 전체 결과 대조다. 이 문서 회차에서는 운영 실행하지 않았다. 예약이 불필요하면 수집 전 취소로 해제하고 시작 후에는 수집 중지를 사용한다. 새 배치/범위 승인 없이 직접 SQL로 상태를 넘기거나 기존 활성 공고를 수정하지 않는다.

## 봉인 분류 미리보기와 명시적 선택

prefix: `/api/v2/admin/announcement-attachment-batches/{batchId}/classification-preview`.

| 요청 | 권한 | 계약 |
|---|---|---|
| POST prefix | ADMIN·CSRF·UUID Idempotency-Key | `{expectedVersion,expectedScopeHash,reason}` → 201, 새 미리보기; 최초 선택은 0건 |
| PUT `/selection` | ADMIN·CSRF·UUID Idempotency-Key | `{expectedVersion,expectedPreviewHash,selectedJobIds,reason}` → 200, 새 선택 이력; 빈 목록은 전체 해제 |
| GET prefix | ADMIN/OPERATOR/APPROVER | batch의 정확한 current_preview_id/hash가 가리키는 요약 |
| GET `/{previewId}` | 읽기 3역할 | 지정 배치의 과거 미리보기와 현재 유효 여부 |
| GET `/{previewId}/items` | 읽기 3역할 | 그 당시 근거/선택의 페이지 조회, size 1~100 |

모두 ApiResponse/PageResponse·no-store다. 생성/선택은 수집 종료 또는 PREVIEW_READY/PREVIEW_PARTIAL_FAILED에서만 가능하다. source UUID 잠금 → 정책/규칙 SHARE → batch 잠금 후 현재 ACTIVE 정책/규칙이 수집 snapshot과 동일한지 검증한다. 새 규칙·정책으로 자동 재분류하지 않는다. 멱등 키는 namespace73106으로 직렬화하며 동일 actor/배치/정규화 입력은 원래 이력을 반환한다. 과거 요청을 재전송해 현재 선택을 되돌리지 않는다. 다른 입력의 키 재사용은 409다.

미리보기는 worker가 이미 봉인·평가한 exact set/evaluation/입력을 읽는다. 목록 필터 재실행·파일 다운로드·추출기·추가 분류 job을 생성하지 않는다. 최대 1000개 고정 jobs와 source별 최대 10개 파일의 계약을 검증하며, 초과 검출용 11개 metadata 조회는 불완전 근거로 처리한다. fileCount는 조회 metadata 수이며 manifest의 discoveredCount/processedCount와 대조한다. 미봉인 set의 파일 metadata는 읽지 않으며 실패 job만 준비 불가 항목으로 남긴다. 본문/추출 텍스트/URL/파일 표시명은 미리보기로 반환하거나 이력에 저장하지 않는다. 출처 변경은 지문으로만 비교하고 상세 텍스트는 기존 제한된 evidence 조회 경로를 사용한다.

항목에는 기본 판정/기존 첨부 판정/제안 판정, 기본 검수 상태·기존 첨부 확인 유무, 규칙 ID/version/hash, 발견 상태/실제 파일·입력 수, 실패 다운로드/불완전 추출/역할 미확인 수와 파일별 상태·정확한 extraction ID를 담는다. 태그는 **base 자동 / 이전 첨부 자동 / 관리자 CONFIRMED / 제안 AUTO**를 분리하고 각각의 추가/제거 코드 차이를 보여준다. 이는 변경 예상 비교이며 현재 수동 확정 태그를 덮어쓰는 작업이 아니다.

선택 가능한 READY는 작업 처리 성공(SUCCEEDED), 완전한 SEALED 근거·동일 평가 입력, 현재 source/base/첨부 버전·이전 검수/정책/출처 연결 일치, 보호 연결/다른 활성 job 부재를 뜻한다. 제안 판정이 REVIEW_REQUIRED여도 수집·근거 처리가 성공한 항목일 수 있다. READY는 최종 승인·선정 결과·관리자 확인 완료가 아니다.

준비 불가 항목은 COLLECTION_NOT_SUCCESSFUL, EVIDENCE_INCOMPLETE, PROTECTED_LINK, ACTIVE_JOB, SOURCE_CHANGED와 저장된 실패 코드로 구분한다. 실패·충돌·삭제를 숨겨 성공 항목으로 전체 수를 대체하지 않는다. eligibleItemCount가 최초 고정 itemCount와 같을 때만 PREVIEW_READY이며, 나머지는 PREVIEW_PARTIAL_FAILED다. 최초 선택은 모두 false이고 READY 외 항목·다른 배치·중복 ID 선택은 전체 409/400이다.

선택 변경은 기존 snapshot의 batchVersion/previewHash와 현재 전체 inputHash가 같아야 한다. source·근거·검수·규칙·태그·정책이 달라지면 409다. 사용자가 새 미리보기를 명시적으로 생성해야 하며, 입력이 바뀐 항목을 자동 제외하여 기존 선택을 유지하지 않는다. 새 미리보기 생성은 선택을 0으로 초기화한다. 이미 수집 기준과 충돌한 source는 새 preview에서도 SOURCE_CHANGED로 남고, 새 입력 수집/범위 절차가 필요할 수 있다.

한 transaction에서 PREVIEW_RUNNING → PREVIEW_READY/PREVIEW_PARTIAL_FAILED를 처리하고 batchVersion은 2 증가한다. 새로운 append-only preview와 전체 항목/선택을 저장한 뒤 current pointer/hash를 함께 변경한다. 중간 상태는 커밋하지 않는다. 현재 source/current 평가/검수/정책 binding/운영 공고에는 쓰지 않는다.

요약의 itemCount·snapshotRemainingItemCount·snapshotDeletedItemCount·eligibleItemCount·selectedItemCount는 **저장 당시 snapshot 수**다. availableItemCount/currentDeletedItemCount는 지금 남은 수다. currentPreview는 현재 pointer 여부, inputsCurrent는 pointer·batchVersion·개별 근거/정책/전체 지문이 여전히 일치하는지다. 삭제/후속 변경 후 statusCode가 과거 PREVIEW_READY여도 inputsCurrent=false이면 현재 적용 근거로 사용할 수 없다. currentHttpRequests는 항상0이다.

V73에 preview/preview_items append-only 이력, batch.current_preview_id nullable FK, exact job/batch composite FK와 전체 건수·선택 일치 deferred trigger를 추가했다. V72의 기존 preview_hash 값에는 새 FK를 직접 걸지 않아 legacy row를 보존한다. 원문/job 삭제 시 해당 item metadata만 cascade되며 ID나 URL을 복원하지 않는다. 요약 snapshot은 남겨 삭제로 인한 오래된 상태를 설명한다. 독립적인 이력 DELETE/UPDATE·이전 preview에 item 추가·새 snapshot 없는 job 선택 변경은 DB에서 거부한다.

미리보기 자체는 적용 승인 근거이며 current 판정을 변경하지 않는다. 아래 적용 API/worker를 추가했지만 정책 게시·ENFORCE·운영 선택/일괄 적용을 실행한 증거는 아니다. 조건부 원복은 필수 후속 Gate다.

## 명시적 적용 승인·중지·재개와 항목별 CAS

`/api/v2/admin/announcement-attachment-batches/{batchId}/application` 하위 API다.

| 메서드·경로 | 권한 | 결과 |
|---|---|---|
| POST 기본 경로 | ADMIN·CSRF·UUID Idempotency-Key | START 접수, 선택 항목만 PENDING, 202 |
| POST `/pause` | 동일 | APPLYING → APPLY_PAUSED 접수, 202 |
| POST `/resume` | 동일 | APPLY_PAUSED → APPLYING 접수, 202 |
| GET `/actions/{actionId}` | ADMIN/OPERATOR/APPROVER | 동일 배치 접수 영수증과 현재 집계 |
| GET `/items?page=1&size=20` | 동일 읽기 역할 | 전체 남은 작업의 수집/적용 상태·실패 코드·재시도/적용 버전, size 최대100 |

변경 요청은 expectedVersion, expectedPreviewId, expectedPreviewHash, expectedItemCount, expectedSelectedCount, expectedDeletedCount, acknowledgeReviewReset=true, reason(1~1000자)다. 임의 source/job 목록·URL·정책 교체·force·성공 여부 입력은 허용하지 않는다. 전체/선택은1~1000, 선택은 전체 이하, 삭제는0~전체다. 변경 요청을 START/PAUSE/RESUME별 불변 이력으로 남기며 동일 키/동일 요청은 최초 영수증과 현재 상태만 반환한다. 다른 요청의 키 재사용은409다. 사유 원문은 기록하지 않고 해시만 저장한다.

START는 고정 source UUID → 정책/규칙 SHARE → batch 잠금 후 현재 미리보기 버전·전체 입력 해시·선택 수를 다시 검증한다. **고정 정책과 현재 ACTIVE ENFORCE 정책·규칙이 정확히 같아야 한다.** COLLECT_ONLY 수집 결과를 승인 버튼으로 ENFORCE로 승격하거나 정책을 교체하지 않는다. COLLECT_ONLY/정책 변경 결과는 기존 근거로 보존하며 새 ENFORCE 범위·수집·미리보기 승인 절차가 필요하다. 이 API는 정책 게시 API가 아니다.

접수는 완료가 아니다. response에는 actionId/batchId/previewId/actionCode, acceptedFromVersion/acceptedAt, currentStatusCode/currentVersion, scopeItemCount/approvedSelectedCount, 현재 remaining/deleted/selectedRemaining/pending/applied/conflict/failed 수와 currentHttpRequests=0을 분리한다. 과거 접수 이력의 승인 건수는 원문 삭제 후에도 당시 건수로 유지한다. 항목 목록은 실제 남은 job만 반환하고 삭제 ID를 복원하지 않는다. collectionStatusCode=SUCCEEDED여도 applicationStatusCode=CONFLICT/FAILED일 수 있다.

worker enabled일 때 기본 1초 간격으로 승인된 APPLYING 배치의 대기 항목 하나를 처리한다. 후보 조회 뒤 source → 정책/규칙 → batch 잠금으로 다시 조회한다. 다른 worker가 먼저 완료했거나 중지됐으면 쓰지 않는다. 하나의 20초 제한 transaction에서 다음을 처리한다.

1. 최초 승인 preview의 exact itemInputHash와 현재 source/base/set/evaluation/extraction/이전 확인·binding metadata를 대조한다. 공통 지문 직렬화 계약을 미리보기와 적용에 함께 사용한다.
2. 현재 ACTIVE 정책/규칙 변경은 APPLICATION_POLICY_CHANGED, source·근거·보호 link·다른 활성 job 변경은 APPLICATION_INPUT_CHANGED, 나머지 metadata 지문 변화는 APPLICATION_PREVIEW_CHANGED로 terminal CONFLICT 처리한다. source 최종 CAS가0이면 APPLICATION_SOURCE_CAS_CONFLICT다. 새 미리보기나 다른 source로 자동 대체하지 않는다.
3. source current_attachment_evaluation_id/attachment_policy_id를 해당 job으로 변경하고 is_attachment_review_required=true, attachment_row_version을1 증가시킨다. classification_row_version과 base 판정은 유지한다. 이전 첨부 confirmation/current evaluation은 STALE로 전환하며 과거 CONFIRMED 태그/검수 기록은 삭제하지 않는다. 새 종합 확인 전 기존 확인으로 DRAFT 전환할 수 없다.
4. job의 applied_evaluation_id/applied_source_version/applied_attachment_version/applied_input_hash를 남기고 APPLIED로 종료한다. 적용 후 지문은 조건부 복구의 비교 근거이며 복구 성공 여부가 아니다. 이 과정은 운영 공고·지원 진행·첨부 원문을 수정하지 않고 HTTP·재다운로드·재추출·재분류도 하지 않는다.
5. 남은 PENDING이 없고 최초 전체 scope_item_count가 전부 APPLIED·삭제0일 때만 배치는 APPLIED다. 비선택·실패·충돌·삭제가 있으면 APPLY_PARTIAL_FAILED다. 선택 부분 성공을 전체 성공으로 부풀리지 않는다.

중지는 다음 항목부터 적용을 막는다. 현재 source transaction이 먼저 잠금을 보유했다면 그 항목 완료 뒤 중지 응답이 돌아올 수 있다. 중지/재개는 완료/충돌/실패를 되돌리거나 선택·누적 수집 bytes/HTTP·재시도 횟수를 초기화하지 않는다. 재개 후 정책/입력이 바뀐 대기 항목은 다시 CAS하여 충돌로 종료한다.

예기치 않은 transaction 실패는 현재 항목 전체가 rollback된 뒤 별도 짧은 transaction에서 application_attempt_count만 증가시키고 30초 후 재시도한다. 최대3회 실패하면 FAILED/APPLICATION_TRANSACTION_FAILED로 종료하며 수집 job.error_code와 구분한다. DB 예외 원문을 audit/log에 복사하지 않는다. 원문 삭제로 대기가 사라진 배치는 DB 전용 집계가 부분 종료하며 APPLY_PAUSED를 자동 해제하지 않는다.

V73에 불변 application_actions, batch.application_approval_id, job.application_preview_id/적용 후 버전·지문/실패·backoff를 추가했다. 승인과 상태/전체 선택 대기 생성은 deferred complete trigger로 같은 transaction을 요구한다. 승인 preview 교체, 승인 없는 선택 항목 적용, terminal 결과 초기화, 이전/적용 근거 변조를 거부한다. 기존 legacy batch/job은 관리 scope/provider가 없는 상태를 추측해 새 계약으로 채우지 않는다.

### 남은 원복 Gate

배치 원복 API·작업자는 아래 승인·실행 증분으로 연결했다. 실제 PostgreSQL·역할별 UI/E2E가 아직 검증되지 않아 출시 판정은 Not ready다. 일반 자동 ENFORCE 원복은 별도 필수 미완료다.

### 이전 확인의 복구 유효성 기반

V73 미적용 증분에 `announcement_attachment_confirmation_restorations`를 추가했다. 원래 confirmation의 ID·confirmedAt·confirmed_source_version·confirmed_attachment_version·작성자·태그는 그대로 두고, 완료된 원복이 연결하는 새 유효 버전만 별도 기록한다. 이전 확인이 적용 당시 이미 만료/무버전이었다면 원복으로 유효한 확인으로 승격하지 않는다. 복구 근거는 원문/작업/확인의 composite FK, 작업당 한 건, 원문·첨부 버전 UNIQUE, 멱등 키와 지문, 불변 이력으로 제한한다. 원문 삭제 시 함께 cascade되며 원문이 남아 있는 상태의 이력 단독 삭제는 허용하지 않는다.

INSERT 전 source→job 잠금을 잡고 현재 적용 판정·정책·두 버전·보호 link/새 검수/활성 작업 부재와 이전 유효 확인 버전을 검사한다. deferred 검증은 source pointer/정책/검수 binding·이전 evaluation current·이전 confirmation current·job ROLLED_BACK을 같은 transaction에서 완료하도록 강제한다. `is_current=true`만으로 이전 확인을 부활시키는 UPDATE는 거부한다. 이 테이블은 승인 이력의 대체물이 아니며, 아래 고정 승인과 항목 worker를 통해 생성한다. 이번 작업은 운영 원복을 실행하지 않았다.

`attachment_confirmation_restored_binding(sourceId,confirmationId)`는 완료된 원복 중 가장 큰 유효 버전만 제공한다. CurrentMapper의 목록/상세와 ReviewService의 검수 조회/DRAFT 전환이 이 동일한 근거를 사용한다. 최초 확인은 원래 버전과 일치해야 하며, 복구 후에는 증가한 버전으로만 전환할 수 있다. DRAFT 생성 직후 link로 인한 첨부 버전 +1 예외는 그대로 유지한다.

화면 설계 판단: 운영자의 과업은 저장된 검수를 재확인한 뒤 DRAFT로 전환하는 것이며, 잘못된 유효성 표시는 수동 검수 없이 전환을 허용할 수 있어 위험도 R2다. 기존 Thymeleaf·Bootstrap·native 폼과 입력 보존/오류/disabled 동작을 유지한다. 원래 검수 날짜와 "원복으로 이전 확인이 복구됨, 새로 검수한 기록은 아님"을 구분하여 안내한다. 새 의존성·레이아웃·모션·자동 전환은 추가하지 않았다. 검증은 서비스/Mapper/Node 계약과 DB용 회귀이며 실제 브라우저·접근성 수동 검증은 필수 미완료다.

남은 작업:

- [~] 전체 원복 영향 미리보기/승인과 항목 worker를 구현했다. 실제 PostgreSQL 및 배치 UI 검증은 필요하다.
- [~] ADMIN·CSRF·멱등 승인, 전체 appliedInputHash/CAS·조건부 복구·실패3회/30초 backoff·분모 보존 집계를 구현했다. 다운로드와 별도 opt-in 설정은 아직 운영에서 켜지 않았다.
- [~] batch의 확인 없음/이미 무효인 확인·base 복구를 구현했다. 일반 자동 ENFORCE job의 적용 후 근거 계약/원복은 미완료다.
- [ ] 실제 PostgreSQL/Linux 회귀, 전체 batch UI, 운영 역할별 브라우저 E2E.

### 배치 원복 승인·실행 증분

API는 api-contract-v1.md 24.16, DB는 db-model-v1.md 11.15를 따른다. APPLIED/APPLY_PARTIAL_FAILED/APPLY_PAUSED의 적용 완료분 **전체**가 원복 대상이다. 현재 적격0이면 실행하지 않는다. 초기 충돌도 대상 이력에 남기며, 잘못된 항목이 이후 우연히 적격이 되어도 자동 복구하지 않는다. 승인 뒤 다시 원문/current/버전·역할·검수·보호 link·활성 작업이 바뀌면 항목별 CONFLICT로 보존한다.

source UUID 순서→batch 잠금으로 전체 영향/해시를 재검증하고 불변 승인·대상·ROLLING_BACK·job 대기·남은 적용 PENDING 취소를 원자적으로 저장한다. 적용 중지에서 넘어온 PENDING은 APPLICATION_CANCELLED_BY_ROLLBACK으로 종료하며 기존 적용 API로 재개할 수 없다. 202 영수증은 접수일 뿐 복구 성공이 아니다.

worker는 `SANEB_ANNOUNCEMENT_ATTACHMENT_ROLLBACK_ENABLED` 별도 설정(기본 false)으로 시작하며 다운로드 worker OFF와 독립적이다. 한 tick당 한 항목, source→batch 잠금 뒤 같은 work를 재조회하고 승인한 계획 지문·적용 후 live 지문을 검사한다. source CAS·이전 evaluation/current·필요한 원래 확인 복구·job 복구 버전을 같은 transaction으로 완료한다. 오류 시 원문/DB 예외 원문을 로그나 audit에 복사하지 않으며 최대3회/30초 backoff 뒤 FAILED로 남긴다.

원문이 삭제되면 대상 ID도 cascade되고 최초 승인 집계/전체 scope는 남는다. 최초 전체 scope가 모두 원복되고 삭제0일 때만 ROLLED_BACK이다. 미적용·취소·실패·삭제가 포함되면 ROLLBACK_PARTIAL_FAILED다. 승인 대상 성공률만으로 전체 완료를 주장하지 않는다. 정책 게시/ENFORCE/운영 공고 삭제·자동 생성·원문 삭제·첨부 HTTP는 이 경로에 없다.
