# ATT-051 연결 공고의 첨부 근거 갱신 계약

상태: 구현 진행 중. V87 목적·효과 차단, V88 불변 연결 snapshot, V89 경고 원장, V90 고정 연결 실행 검사를 추가했다. 경고 조회·명시 범위 조회·멱등 예약·전용 시작/중지/재개 API와 실패/만료 경고 저장을 구현했다. 실제 PostgreSQL에서 예약/실행/중지/재개 및 연결 변경 거부를 검증했다. 성공/부분 첨부 평가와 경고의 전체 연결, 관리자 UI, 실행/완료 시점 경합, 운영 E2E는 남아 있다. 운영 미반영이다. 아래 구현 차이 표는 최초 설계 조사 시점이며 최신 검증은 QA 추적표를 따른다.
기준 HEAD: `0e840600a67973a2bf6f4a371173af68922d0589`.
근거: `announcement-attachment-collection-design-2026-09-08.md` 12.2, `announcement-attachment-qa-plan-2026-09-08.md` ATT-051.

## 1. 목표와 불변 조건

이미 운영 공고와 연결된 원문도 운영자가 명시적으로 지정하면 새 첨부 근거를 수집하고 재검수 경고를 남길 수 있어야 한다. 연결된 운영 공고의 공개 상태·조건·지원 진행·신청 데이터는 변경하지 않는다.

- 기존 일반 배치·자동 intake·단건 전체 수집의 연결 원문 보호는 유지한다.
- 이 경로는 링크 삭제/교체, 공고 생성, DRAFT 재생성, 현재 판정 적용, 확인 무효화, 기존 업무의 진행 차단을 하지 않는다.
- 별도 경고는 새 근거 확인 필요를 뜻하며 기존 업무가 무효라는 뜻이 아니다. 사용자에게 자동 알림을 보내지 않는다.
- 제목 제외 원문은 복원하거나 다운로드하지 않는다. 정상 대상만 다른 후보로 자동 보충하지 않는다.
- 실패 파일과 발견 실패는 별도로 보존한다. 일부 성공을 전체 성공으로 표시하지 않는다.
- 운영 수집·정책·기존 데이터 반영은 이 설계로 자동 승인되지 않는다.

## 2. 실제 구현과의 차이

| 확인 파일 | 현재 동작 | 필요한 증분 |
|---|---|---|
| `AttachmentBatchRequests.Scope` | 기간/출처 기반 범위만 허용, unknown field 거부 | 별도 명시 source ID 범위 계약 |
| `AnnouncementAttachmentBatchMapper.xml` Eligible | 링크 존재 시 무조건 제외 | 일반 쿼리는 유지하고 별도 목적의 고정 대상 쿼리 추가 |
| `AnnouncementAttachmentBatchServiceImpl` | 범위/locator/정책/이전 binding 고정, `applyToSource=false` 작업 | 전체 link 집합을 추가 고정, 일반 적용·원복 대상과 구분 |
| V75 `attachment_batch_job_input_unchanged` | 매 요청 경계에서 링크 존재를 거부 | 명시 목적에만 전체 link 집합 일치 분기 허용 |
| `AnnouncementAttachmentEvaluationServiceImpl.saveDecision` | batch이면 preview만 저장; 일반 job이면 current/confirmation 변경 | 반드시 batch 경로 사용, 별도 경고를 같은 transaction으로 저장 |
| V68 `announcement_source_links` | unique(source_id), 원문당 최대 1개 연결 | 개수뿐 아니라 link ID와 announcement ID를 고정하여 같은 건수의 교체 감지 |

단건 일반 작업에 예외만 허용하면 current evaluation과 confirmation을 변경할 수 있으므로 사용하지 않는다. UI의 적용 버튼 숨김만으로 보호하지 않는다.

## 3. DB-first 계약

조사 시 최신 migration은 V86이었다. 새 V87 `V87__separate_linked_attachment_evidence_batch_purpose.sql`은 목적/일반 배치 기본값과 목적 불변·적용/원복 차단만 추가한다. 아래 나머지 DB 계약은 후속 구현이다. V1~V86은 수정하지 않는다.

### 3.1 작업 목적과 고정 연결

- batches에 `purpose_code NOT NULL DEFAULT 'STANDARD'` 추가: `STANDARD`, `LINKED_EVIDENCE_ONLY`만 허용. 기존 행/요청의 의미는 그대로다. 생성 후 변경 금지.
- V88의 별도 고정 연결 테이블은 job_id(PK), source_id, links_json을 저장한다. `(job_id, source_id)` 복합 FK로 작업 소속을 보장하고 JSON에는 linkId·announcementId 쌍을 저장한다. 현재 V68 제약상 연결은 최대 1개이며 배열 표현이 다중 연결을 허용한다는 뜻은 아니다. 기존 link가 삭제/교체되어도 고정 증거는 보존하며 live link 삭제 cascade는 사용하지 않는다.
- 고정 source가 삭제될 때의 개인정보/원문 정리 정책은 기존 source 삭제 계약을 따른다. 삭제 항목은 batch의 고정 분모와 삭제 집계에 남기되 원문 URL·텍스트를 별도 보관하지 않는다.
- 현재 live link의 FK는 연결 중 source 삭제를 거부한다. 첨부 기능은 이 보호를 완화하거나 link를 해제하지 않는다. 별도 업무에서 link가 해제된 뒤 source가 삭제되면 job→고정 snapshot cascade와 배치 삭제 건수 기록을 따른다. 운영 공고 자체는 삭제·수정하지 않는다.
- 고정 연결은 예약 transaction에서 전체 집합을 저장한 뒤 불변이다. 단순 count/hash만 저장하지 않으며 link 추가·삭제·같은 개수의 교체를 모두 비교한다.
- linked 목적 job은 `batch_id` 필수, operation은 기존 `COLLECT`, `application_status_code='NOT_REQUESTED'`, `rollback_status_code='NOT_REQUESTED'`, 선택 적용 false다. 다른 조합을 DB에서도 거부한다.
- 예약 및 경고 저장 시 source를 먼저 잠그고 같은 순서로 링크를 확인한다. link 생성·삭제의 경합이 source 잠금만으로 직렬화되는지 기존 쓰기 경로를 확인한다. 보장되지 않으면 additive trigger/명시적 잠금 프로토콜로 보강한다. 단순 사전 SELECT로 충분하다고 간주하지 않는다.

### 3.2 재검수 경고 이력

- V89 `announcement_attachment_linked_review_notices`: id, batch_id, job_id, source_id, set_id(nullable), evaluation_id(nullable), reason_code, created_at. 실제 job의 source/batch/set/preview와 완료 상태가 일치하고 고정 연결이 유지될 때만 삽입한다. worker 연결 전 자동 생성은 하지 않는다.
- UNIQUE(job_id): 완료 응답 유실·lease 재시도에서 경고를 중복 생성하지 않는다.
- FK는 source/set/evaluation의 실제 composite identity에 맞춘다. 다른 source의 set/evaluation 연결은 SQL에서 거부한다.
- 사유: 근거 수집 완료 후 확인 필요 / 부분 수집·추출 확인 필요 / 수집 실패. 구체적인 기존 실패 코드는 job/file 근거로 조회한다. 원문·계정정보·운영 공고 조건을 metadata로 복사하지 않는다.
- 새 근거와 경고는 append-only. 최신 경고 조회와 과거 이력을 분리하고 미완료 job은 진행 중으로 표시한다. 경고 삭제나 임의 확인 완료 기능은 본 증분에 추가하지 않는다.
- source의 base/current attachment pointer, policy binding, review-required flag, confirmation과 버전을 경고를 위해 변경하지 않는다. 현재 업무 판정과 별도 근거 버전을 화면/API에서 구분한다.

## 4. API 계약

기존 `/api/v1`과 현재 일반 `/api/v2/admin/announcement-attachment-batches` 요청의 의미는 유지한다. 기존 scope에 `includeLinked=true`를 추가하여 넓게 우회하지 않는다. 동일 wrapper/권한 규칙의 별도 v2 하위 자원을 추가한다.

제안 기본 경로: `/api/v2/admin/announcement-attachment-linked-evidence-batches`.

| 동작 | 계약 |
|---|---|
| POST `/scope-preview` | policyId, 중복 없는 sourceIds 1~1000개, maximumSourceBytes. DB 읽기 전용·외부 HTTP 0. 전체 링크와 버전/정책/locator를 서버에서 고정 지문으로 계산 |
| POST 기본 경로 | 위 입력 + expectedScopeHash + 사유 + 명시적 영향 확인, Idempotency-Key. ADMIN만 SCOPE_READY 예약 |
| GET `/{id}` 및 `/items` | 일반 wrapper/PageResponse. 목적·고정/현재 링크 대조·전체 분모·성공/부분/실패/삭제·경고 표시 |
| PUT `/{id}/collection` | 기존 수집 시작 수준의 버전·지문·고정 대상 수·최대 요청/bytes 명시 확인. 예약만으로 외부 요청하지 않음 |
| 중지/재개/수집 전 취소 | 기존 collection 상태 전이와 자원 한도 재사용. 임의 항목 추가·상한 변경 불가 |

- 읽기는 ADMIN/OPERATOR/APPROVER, 예약/실행은 ADMIN. 모든 변경은 서버 인증·CSRF·입력 검증을 사용한다.
- apply/rollback/confirmation/DRAFT API는 이 목적을 명시적으로 거부한다. 일반 API에 linked batch ID를 넣어도 거부한다.
- scope 누락/중복/미연결 source/제목 제외/진행 중 job/퇴역 정책/규칙 불일치는 항목별 준비 사유로 반환한다. 일부를 누락시켜 canReserve=true로 만들지 않는다.
- 오래된 범위는409: `예약 이후 운영 공고 연결이 변경됐습니다. 전체 연결과 수집 범위를 다시 확인하세요.`
- 같은 멱등 키·같은 입력은 같은 batch를 반환한다. 다른 actor/목적/ID/bytes/사유는409다.
- source ID는 승인된 범위의 식별자일 뿐 URL 입력이 아니다. profile/parser 선택은 계속 시스템 registry가 담당한다.

## 5. worker 경로와 오류 처리

1. 고정 source/base/정책/locator/전체 link 집합을 시작 시 재검증한다.
2. 기존 batch resource lease·worker·다운로드·격리 추출을 사용한다. 새 실행 엔진은 만들지 않는다.
3. 매 외부 요청 직전 DB 입력 fence는 STANDARD의 링크 없음 조건을 유지한다. LINKED_EVIDENCE_ONLY에만 고정 연결 전체 일치를 요구한다. OFF·정책·quota 검사를 우회하지 않는다.
4. set 봉인과 evaluation 저장은 기존 batch preview 의미를 유지한다. source current/confirmation 갱신 분기로 들어가지 않는다.
5. 마지막 transaction에서 연결·source 입력을 다시 확인하고 terminal job + 별도 경고를 저장한다. 연결이 달라지면 CONFLICT이며 이전 연결에 경고를 적용하지 않는다. 이미 확보한 근거의 보존/비노출은 기존 실패 근거 계약에 맞춘다.
6. 상세 발견 실패처럼 set이 없는 terminal 실패도 경고/작업 실패로 조회할 수 있어야 한다. 성공 callback에만 경고 생성을 두지 않는다.
7. 수집 시작 후 관리자 승인 취소/중지, lease 유실, 재시도, 부분 실패의 분모와 자원 정리는 기존 규칙을 유지한다.

## 6. 구현 체크리스트와 검증

- [x] 원 설계·QA 요구와 현재 보호 경계 조사.
- [x] 로컬 DB migration V87~V90: 목적/목적 불변·적용 금지, 고정 연결, 경고 소속 제약, 실행 fence 추가. 순차/빈 DB migration·기존 행/checksum 보존과 잘못된 snapshot/경고 쓰기 거부 검증. 운영 반영은 미완료.
- [x] scope/예약 Service/DAO/Mapper/DTO와 신규 v2 Controller, 전용 시작·중지·재개. 일반 scope regression 유지.
- [~] source/link 예약 잠금·매 요청 fence·실패/만료 경고 저장 검증. 합성 첨부의 성공/부분 결과·경고·배치 집계 통합14건 회귀 통과. 실제 외부 worker와 최종 완료 경합 검증은 남음.
- [x] 일반 적용 미리보기/apply/rollback 서비스의 명시적 목적 거부 및 실제 DB 검증. 연결 근거 미리보기로 review/DRAFT 호출 시 연결 보호·현재 판정 불일치 거부, 확인/공고 신규 생성0 검증. 운영 브라우저 검증은 별도.
- [ ] 현재 근거와 연결 공고 재검수 경고를 구분하는 관리자 조회/UI. UI 구현 전 frontend/UI-UX 스킬 적용.
- [ ] Linux 실제 PostgreSQL/worker 계약 및 브라우저 검증.
- [ ] 사용자가 지정·승인한 운영 링크 표본 1건부터 수집. 승인 전 운영 쓰기 없음.

필수 시험:

1. V68 원문당 단일 연결 제약을 유지한다. 연결 삭제·동일 건수의 link ID/공고 ID 교체를 예약·시작·HTTP 전·봉인 전에 각각 충돌 처리한다. 다른 원문의 연결은 해당 snapshot에 포함하지 않는다. V26만 보고 다중 연결 가능으로 판단했던 초기 설계를 V68과 실제 PostgreSQL 오류에 근거하여 정정했다.
2. 정상 PDF/HWP/HWPX·부분/손상/미지원 혼합·첨부 없음·발견 실패에서 파일 분모/오류 보존 및 경고 사유 구분.
3. 실제 DB에 연결된 공고의 상태·조건·신청/진행 행을 만들고 전후 모든 컬럼 snapshot을 비교. 변경0을 직접 assertion한다.
4. base/current evaluation, confirmation, policy binding, source versions의 전후 동일성. 경고와 set/evaluation만 새 이력으로 증가.
5. 일반 배치 연결 제외와 intake/단건 보호 회귀. linked batch를 일반 적용·원복·초안 API에 넣어 거부/쓰기0 검증.
6. 동일 키 재요청·동시 예약·lease 유실·worker 재시작·결과 저장 유실의 단일 job/경고 및 자원 정리.
7. 실제 역할별 권한/CSRF/다른 source 근거 접근 거부, 문자열 escape·민감 원문 로그 미복사.

성공 기준: 명시 연결 범위의 근거와 경고가 저장·조회되며 원래 운영 업무 데이터 불변이 전체 계층에서 입증된다.
실패 기준: 단순 연결 제외를 완료로 계산, guard 전역 해제, 명시 범위 축소/다른 후보 보충, source current/확인 무효화, 운영 공고·조건·신청 변경, 운영 승인 없이 실행.

## 7. 이번 증분의 경계

이 문서만으로 ATT-051은 완료되지 않는다. 기존 일반 배치·적용·검수 서비스95건 통과는 보호 기준선이며 신규 경로 통과가 아니다. PDF SRC-017546 단건 수집 승인과 이 기능의 운영 실행 승인은 서로 별개다. 기존 전체 goal 및 ATT-001~062 분모는 유지한다.
