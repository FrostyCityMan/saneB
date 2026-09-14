# 공고 첨부 기존 데이터 전체 분할 관리 계약

## 목적과 승인 경계

Gate 1·5, ATT-046~049의 전체 대상 대조를 위한 상위 관리 기록이다. 기존 단일 배치의 `maximumCount`는 1~1000건이며, 같은 필터 재실행은 다음 분할을 뜻하지 않는다. 전체 후보를 먼저 고정한 뒤 각 후보를 한 분할에만 배정한다. 첫 배치 성공을 전체 성공으로 표현하지 않는다.

전체 목표인 상시 worker, 전체 Provider, 정책 게시, 관리자 UI, 기존 데이터 실행, 운영 배포·브라우저 E2E는 그대로 유지한다. 이 문서는 그중 전체 범위 고정 계약이며 다른 Gate의 완료 근거가 아니다.

- 전체 목록 고정은 수집·적용 승인이 아니다. HTTP 요청, job 생성, source 변경, 정책 게시, ENFORCE, 운영 공고 변경은 없다.
- 운영 실행 시 전체 범위/건수와 분할별 요청 상한/영향/복구를 확인하고 별도 승인한다.
- 수집 결과·적용 결과·원복 결과와 목록 고정을 별도 상태로 표시한다. `INVENTORIED`는 처리 완료가 아니다.
- 제목 제외·기본 판정 부적격·연결 공고 보호는 기존 배치와 같은 배타 집계다. 준비 실패/진행 중 job을 조용히 건너뛰어 다른 대상을 넣지 않는다.

## DB-first

V1~V73 변경 없이 V74를 추가한다.

| 테이블 | 역할 |
|---|---|
| `announcement_attachment_backfill_runs` | 전체 필터·정책 snapshot·분류별 건수·후보 지문·최초 후보 수·분할 크기/수·요청 멱등성 |
| `announcement_attachment_backfill_segments` | 고정 분할 번호·최초 건수·삭제 건수 |
| `announcement_attachment_backfill_items` | source/content/base/rule 소유 관계, 최초 순번·입력 지문·분할 소속 |

- `(run_id, source_id)` unique. 순서는 `collected_at, source_id`, 분할 번호는 `(ordinal-1)/segmentSize+1`. 동률에도 중복·누락 없이 결정한다.
- 전체 후보 수는 bigint. 전체 조회/고정 SQL에는 LIMIT를 두지 않는다. 페이지 LIMIT는 이미 고정된 목록 조회에만 사용한다.
- 한 REPEATABLE READ transaction에서 집계 확인, run, segment, item을 저장한다. 오류·시간 초과·경합이면 전체 rollback한다. 일부만 저장한 성공 응답은 없다.
- SQL 전체 집계가 감당할 수 없는 범위는 timeout으로 실패한다. 첫 N건으로 축소하지 않으며 운영 용량 검증은 별도 필수다.
- 원문·제목·본문·URL을 이력에 복사하지 않는다. 입력 지문에는 기본 판정/버전/검수 binding, 기관·출처 연결 지문, 수집 시각·마감일이 포함된다.
- 원문 또는 고정 base/content 삭제는 FK cascade로 item을 제거한다. 최초 건수는 불변이고 삭제 건수만 증가한다. 삭제 대상 식별자 tombstone은 남기지 않는다.
- `최초 후보 수 = 남은 item 수 + 삭제 수 = 분할 최초 건수 합`을 deferred constraint로 확인한다. 최초 transaction 이후 item 추가·분할 이동·내용 변경·관리 이력 삭제를 거부한다.

## API

`/api/v2/admin/announcement-attachment-backfills`, ApiResponse/PageResponse, no-store.

| 메서드·경로 | 권한 | 의미 |
|---|---|---|
| POST `/scope-preview` | ADMIN/OPERATOR/APPROVER | 필터 전체 배타 집계·후보 지문·예상 분할 수. HTTP 0 |
| POST `/` | ADMIN | UUID Idempotency-Key, scope/expectedScopeHash/expectedCandidateCount/reason을 검증하고 전체 목록 고정 |
| GET `/`, `/{runId}` | 읽기 3역할 | 고정 전체 범위와 현재 잔여/삭제 건수. 실행 완료가 아님 |
| GET `/{runId}/segments` | 읽기 3역할 | 고정 분할 페이지 |
| GET `/{runId}/segments/{segmentNo}/items` | 읽기 3역할 | 해당 고정 소속과 현재 입력 일치 여부. 원문 없음 |

범위 입력: policyId, providerCodes(BIZINFO/GOV24/LOCAL_GOV_NOTICE), collectedFrom(포함), collectedBefore(미포함), 선택 deadlineFrom/Through, segmentSize(1~1000). `maximumCount`, sourceIds, URL, parser, 성공값 등 임의 필드는 거부한다. timezone은 UTC로 정규화한다. 같은 멱등 키의 actor/내용 변경은 409이며, 성공 후 같은 입력 재요청은 최초 run을 반환한다. 세션 CSRF를 검증한다.

진행 중인 동일 멱등 키는 advisory try-lock으로409를 반환하고 같은 키·입력 재확인을 안내한다. 대기 후 과거 snapshot으로 실행하지 않는다. 신규 저장 과정의 DB 경합/무결성 오류도 전체 rollback한다. page는1이상/size1~100, PageResponse 정수 페이지 범위 초과는 잘라내지 않고409다.

고정 정책은 현재 ACTIVE 규칙에 연결된 게시 COLLECT_ONLY/ENFORCE여야 한다. 목록 고정은 개별 profile 준비 완료를 보증하지 않는다. 분할 예약 직전에 정책·시스템 profile·최신 입력·작업 경합을 다시 검증해야 한다.

## 뒤따르는 필수 구현 — 범위 축소 금지

- [~] V75·고정 segment→batch 내부 예약/일대일 연결 구현. 원래 필터를 다시 실행하거나 삭제분을 채우지 않음. 실제 PostgreSQL·운영 검증은 남음.
- [~] 분할 예약/실행 전 입력 변경 차단, 삭제 전후 합계, 동일 예약 멱등성, 잠금 순서 구현·단위 검증. 실제 PG 경합은 남음.
- [~] 전체 수집·적용·원복/미예약·취소·삭제 집계 API 구현. 보호 제외는 frozenScope 최초 집계에 유지. 모든 분할의 실제 승인 범위 실행/최종 대조는 남음.
- [~] 관리자 UI에서 전체 목록 고정 → 분할 탐색 → 기존 배치 제어 연결 구현·Node/SSR 검증. 실제 DB/운영 브라우저, 정책 화면과 전체 이력은 별도 완료 필요.
- [ ] 실제 PostgreSQL/Flyway·Linux worker·운영 데이터·운영 브라우저에서 검증.

## 검증 기준

- 성공: 1001개 이상 후보의 전체 건수·분할 수·소속이 일치, 신규 도착 미편입, 삭제 후 최초 분모 보존, 다른 source/base 연결·부분 저장·추가 삽입 차단, 멱등성·권한·CSRF·입력 검증 통과.
- 실패: LIMIT를 전체 범위로 표현, 배치 필터 반복을 다음 분할로 간주, 준비 실패 숨김, 삭제를 성공으로 집계, HTTP/job/source 변경 발생, 단위/정적 검증을 실제 DB 성공으로 표현.
- 단위·HTTP·Mapper 계약 검증과 실제 PostgreSQL 검증을 구분한다. 환경상 생략된 시험은 미완료다.

## 2026-09-12 00:35 로컬 구현·검증 기록

- [x] 전체 scope/고정 run/분할·항목 탐색 DTO·Service·Controller·Mapper 구현, CSRF namespace 연결.
- [x] Service23·HTTP19, Mapper/migration 정적 검증 통과. 전체 root1392통과/184생략/실패0, 추출기25통과, Node69통과, bootJar/installDist 통과.
- [~] V74와 실제 PG7시험 작성, V73→V74 upgrade 검증·Linux 필수 suite 연결. 현재 PostgreSQL 시험은 전부 조건부 생략이며 실행 통과가 아니다.
- [ ] 고정 segment→batch 연결·전체 결과 집계·관리자 UI·실제 운영 적용.

실행 명령·JAR 지문·현재 blocker는 `announcement-attachment-end-to-end-progress-2026-09-09.md`의 2026-09-12 00:35 기록을 따른다. 브라우저는 목표에서 승인됐으나 실제 운영 환경 검증 선행조건이 충족되지 않아 이번 회차 미실행이다.

## V75 고정 분할 예약과 전체 집계

V1~V74 변경 없이 `announcement_attachment_backfill_segment_batches`를 추가한다. PK(run_id,segment_no), UNIQUE(batch_id)/멱등 키로 분할당 배치 하나를 고정한다. 예약 영수증은 변경·삭제하지 않는다. 수집·적용·원복은 연결된 기존 batch API를 사용하며 최초 예약은 SCOPE_READY, HTTP0이다.

| API suffix | 권한 | 의미 |
|---|---|---|
| GET `/{runId}/segments/{segmentNo}/reservation-preview` | 읽기3역할 | 고정 소속의 현재 입력·정책·시스템 profile·기존 job 준비 확인 |
| POST `/{runId}/segments/{segmentNo}/reservation` | 활성 ADMIN·CSRF·UUID Idempotency-Key | expectedRunVersion/expectedSegmentHash/expectedRemainingItemCount/expectedDeletedItemCount/reason 확인 후 일대일 batch 예약 |
| GET `/{runId}/summary` | 읽기3역할 | 최초 후보·잔여·삭제·예약/미예약·수집/적용/원복 각각의 전체 상태 집계 |

Preview readiness: READY, ALREADY_RESERVED, ALL_ITEMS_DELETED, INPUT_CHANGED, POLICY_CHANGED, BATCH_NOT_READY. READY만 예약할 수 있다. BATCH_NOT_READY의 개별 원인은 기존 batchPreview.items의 ACTIVE_JOB/BASE_RECLASSIFICATION_REQUIRED/PROFILE_REQUIRED 등을 따른다. 실패 항목을 빼고 새 후보를 넣지 않는다. ALREADY_RESERVED는 과거 배치 ID를 제공하며 정책 퇴역이나 원문 변경 뒤에도 기존 연결을 탐색할 수 있다.

예약 영수증은 최초 분할 수, 실제 예약 수, 예약 전 삭제 수, 확인한 inventory 버전/분할 지문과 batch ID, 조회 시 batch 상태를 구분한다. reservedAt은 영수증 기록 시각이며 worker 완료 시각이 아니다. 같은 actor/run/segment/key/payload 재요청은 현재 입력을 재예약하지 않고 최초 연결만 반환한다. 다른 키로 취소·실패한 연결을 교체하지 않는다.

- 요청 키75001→기존 batch 키73105→source UUID 순서→게시 정책→run→segment→신규 batch/job/연결 순서로 처리한다. 변경은 READ COMMITTED, 조회는 REPEATABLE READ다. 기존 batch 키도 source 전에 잠가 같은 UUID의 일반 예약과 역순 경합을 피한다.
- 내부 `FixedScope`는 HTTP 입력 DTO가 아니다. Controller는 sourceIds/URL/parser/batchId/force/승인 override를 받지 않는다. 원래 기간/최대 건수 query 대신 고정 ID 전체의 최신 보호/버전을 검사한다. 기존 일반 batch 지문/동작은 그대로다.
- 새 batch와 일대일 연결은 같은 transaction에 있어야 한다. marked batch의 연결 누락, 다른 분할 소속 job, 빠진 고정 item, 불일치한 정책 snapshot/삭제 분모는 deferred constraint로 거부한다.
- 최초 분할 수 = batch 최초 수 + 예약 전 삭제 수. 현재 분할 삭제 수 = 예약 전 삭제 수 + batch 삭제 수. cascade 뒤에도 실제 잔여 item/job의 소속과 건수가 일치해야 한다.
- 예약 전 삭제는 확인한 잔여만 예약하고 최초 분모를 보존한다. **예약 후 삭제는 기존 수집 시작 차단을 유지**한다. 수집 전 취소 후 기존 분할은 취소 이력으로 남으며 자동 재예약/대체하지 않는다. 재처리가 필요하면 잔여의 새 전체 범위를 명시적으로 고정·승인한다. 이 경우 원래 분할의 취소/삭제를 처리 성공으로 바꾸지 않는다.
- linked job의 원래 inventory 입력 지문을 수집 시작·재개 및 기존 claim/매 HTTP guard에서도 확인한다. 기관·출처·원문/첨부 버전·검수·보호 연결 변경을 덮어쓰지 않는다. 정상 batch와 기존 v1 계약은 유지한다.
- summary의 collectionCounts/applicationCounts/rollbackCounts는 서로 다른 차원이며 각각의 합은 remainingItemCount다. UNRESERVED는 미예약이고 MISSING_JOB은 무결성 오류409다. APPLIED와 ROLLED_BACK을 더해 두 번 성공으로 세지 않는다. 최초 candidateCount=remainingItemCount+deletedItemCount를 대조한다. unreservedInputChangedCount는 미예약 중 입력이 달라진 수이며 자동 제외가 아니다.

실제 PostgreSQL 시험은 1001건 전체 두 분할 예약, 신규 미편입, 예약 전후 삭제, 기관 변경 claim/시작 차단, 연결 없는 batch rollback, 불변 영수증/취소 집계, 동시 같은 분할 예약을 포함한다. 조건부 생략은 실제 DB 성공이 아니다. 관리자 분할 화면·실제 승인 범위 실행·운영 브라우저와 모든 다른 Gate는 계속 필수다.
