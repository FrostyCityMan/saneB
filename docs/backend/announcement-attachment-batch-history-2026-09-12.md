# 첨부 배치 적용·원복 승인 이력

## 목적과 범위

Design Read: 운영 담당자가 다른 세션의 적용·원복 승인까지 탐색하고, 승인 당시 범위와 현재 결과를 혼동하지 않은 채 정확한 영수증으로 돌아간다.

- 기존 Thymeleaf/Bootstrap 5·검수 CSS·외부 JavaScript를 사용한다. 새 의존성·차트·모션은 없다.
- 사용자: 활성 ADMIN/OPERATOR/APPROVER. 내부 숙련 사용자·한국어·서울 시각·데스크톱과 작은 화면/키보드 사용을 가정한다.
- 과업: 배치 선택 → 승인 목록 → 페이지 이동 → 특정 승인 영수증/현재 처리 결과 확인. 목록 조회는 R0이나 이후 대량 적용·원복(R2)의 판단 근거이므로 잘못된 배치/분모/시점 표시를 허용하지 않는다.
- 이번 목록은 APPLICATION의 START/PAUSE/RESUME와 ROLLBACK의 START 승인이다. 수집 제어·미리보기·선택 변경 전체를 합친 일반 감사 로그가 아니다. 기존 단건 영수증 API를 대체하지 않는다.
- 외부 HTTP, 다운로드, 추출, worker 실행, 정책 게시, ENFORCE 전환, 기존 데이터 적용/원복을 조회로 시작하지 않는다. 운영 공고 자동 활성화도 없다.

## DB-first 계약

기존 로컬 V73의 `announcement_attachment_batch_application_actions`와 `announcement_attachment_batch_rollback_actions`를 UNION ALL로 조회한다. 이번 변경에는 DDL이나 기존 migration 수정이 없다. V73 자체의 실제 PostgreSQL 적용/검증은 별도 미완료 Gate다.

- 각 action의 승인 전 `expected_version`과 승인 당시 범위/선택/삭제/원복 영향을 그대로 사용한다. 작업·원문 테이블과 조인하지 않아 원문 삭제가 승인 기록을 지우거나 당시 분모를 바꾸지 않는다.
- action은 불변이고, 삽입 trigger가 배치를 잠근 뒤 현재 버전과 승인 전 버전의 일치를 검사한다. 승인 transaction의 배치 버전 증가 및 관련 근거는 deferred 제약으로 결합된다. 새 승인에 이전 버전을 끼워 넣는 방식은 허용하지 않는다.
- 첫 조회의 현재 배치 버전을 상한으로 고정하고 `expected_version < throughVersion`을 적용한다. 예를 들어 현재 버전20이면 승인 전 버전19까지 포함한다. 이후 버전21의 새 승인은 기존 상한20 목록에 들어오지 않는다.
- 정렬은 승인 전 버전 DESC, 종류 DESC, action UUID DESC다. 기록 시계의 역행보다 단조 증가 배치 버전을 우선한다. 기존 batch/version 제약·인덱스를 사용한다.
- Service의 REPEATABLE READ/readOnly transaction(timeout15초)에서 배치·전체 count·페이지를 함께 읽는다. 페이지 크기로 전체 count를 제한하지 않는다. 페이지 offset은 long으로 계산한다.
- 일부 row/합계/배치 소속/버전/종류별 필드가 모순되면409로 거부한다. 잘못된 일부 결과를 정상 전체 목록으로 포장하지 않는다.

## API

`GET /api/v2/admin/announcement-attachment-batches/{batchId}/action-history`

기존 `/api/v1` 및 기존 v2 변경 API/영수증 응답은 보존한다. Controller → Service → ServiceImpl → DAO → Mapper XML 계층이며 `ApiResponse<History>`와 no-store를 사용한다.

| 입력 | 계약 |
|---|---|
| batchId | UUID, 존재하지 않으면404 |
| page | 기본1, 1~2147483647 |
| size | 기본20, 1~100 |
| throughVersion | 생략하면 현재 배치 버전, 지정하면0~현재 버전. 현재보다 큰 값409 |

History: `batchId`, `throughVersion`, `currentBatchVersion`, `history`(PageResponse), `currentHttpRequests=0`.

| Entry 필드 | 의미 |
|---|---|
| actionId / batchId | 해당 승인과 배치 식별자 |
| actionKind / actionCode | APPLICATION + START/PAUSE/RESUME 또는 ROLLBACK + START |
| acceptedFromVersion / acceptedAt | 승인 전 버전 / 기록 시각. 완료 시각이나 DB commit 시각이 아님 |
| previewId | 적용 승인만 사용. 원복은null |
| scopeItemCount | 승인 당시 최초 고정 범위 |
| approvedTargetCount | 적용의 승인 선택 수 또는 원복의 승인 대상 수 |
| deletedCountAtAcceptance | 승인 당시 삭제 수. 현재 삭제 수가 아님 |
| approvedEligibleCount | 원복 당시 적격 수, 적용은null |
| approvedBaseReopenCount / approvedConfirmationRestoreCount | 원복 당시 기본 경로 재개/이전 확인 복구 수, 적용은null |
| cancelledPendingCount | 원복 당시 취소한 적용 대기 수, 적용은null |

목록에는 성공/실패의 현재 집계를 넣지 않는다. 원문/본문/첨부 텍스트/URL/actor/사유·사유 지문/멱등 키/request hash/실행 snapshot도 반환하지 않는다. 내부 감사 주체 기록은 기존 저장 계약에 유지한다.

현재 결과는 기존 GET `/{batchId}/application/actions/{actionId}` 또는 `/{batchId}/rollback/actions/{actionId}`로 읽는다. UI는 action ID/종류/배치/승인 전 버전/기록 시각/최초 범위/승인 수/preview 또는 원복 영향을 목록과 대조한 뒤 영수증을 표시한다. 조회 시점의 현재 삭제·성공·실패가 당시 수와 달라질 수 있다.

활성 ADMIN/OPERATOR/APPROVER만 조회하며 Service에서도 인증·활성 계정·비밀번호 변경 완료를 검사한다. 비로그인401, 외부/비활성 계정403, 잘못된 UUID·숫자·범위400, 없는 배치404, 미래 상한/모순된 결과409다. 자동 쓰기나 POST endpoint를 추가하지 않는다.

## 화면·상태·복구

- 기존 배치 화면의 `적용·원복 승인 전체 이력` 영역에서 조회한다. 처음에는 배치를 선택하라는 안내와 비활성 버튼을 표시한다. URL에 이력 기준이 없으면 자동으로 전체 이력 요청을 시작하지 않는다.
- 10개씩 페이지 표시하며 전체 수/페이지/고정 기준/현재 배치 버전을 함께 보여준다. 페이지 이동은 같은 상한을 유지하고 `최신 승인 목록 처음부터 조회`만 새 상한을 받는다.
- 비민감 `historyVersion`/`historyPage`와 batch/action 식별자는 URL에 보존한다. 새로고침 때 기준을 복원하되 인증은 서버가 다시 검증한다. 각 페이지 이동을 브라우저 방문 이력으로 누적하지 않고 기존 replaceState 방식을 유지한다.
- 영수증 버튼은 읽기 요청만 하고 검증된 결과 제목으로 포커스를 옮긴다. 표시값은 textContent, 구조는 기존 native heading/button/article/dl이다.
- 조회·페이지 이동은 입력 사유와 선택을 보존하되 이전 실행 동의를 해제한다. 결과 미확정 변경 요청이 있으면 이력 조회도 잠그고 동일 요청 재확인을 우선한다.
- 목록 요청 실패는 오류로 알리고 이전 행을 보존한다. 빈 목록은 승인 기록 없음이며 수집/적용 완료가 아니다. 실패·불일치 영수증을 새 성공 결과로 표시하지 않는다.
- 다른 배치를 선택하면 이전 승인 페이지와 영수증을 해제한다. 새 배치 조회가 실패해도 이전 배치 결과를 새 대상 결과처럼 남기지 않는다.
- SHOULD 예외: 사유/요청 키는 URL·브라우저 영구 저장소에 저장하지 않는다. 탭 종료 시 입력 손실 가능성은 기존 이탈 경고, 비민감 URL, 서버 승인 이력 탐색으로 보완한다. 탭 내 보존은 Node 시험, 실제 새로고침/세션 복구는 후속 운영 브라우저 시험으로 구분한다.

## 검증과 완료 기준

- [x] Service13건·HTTP12건·Mapper30건·배치 SSR8건 표적 실행: 2026-09-12 `BUILD SUCCESSFUL in31s`, 총63통과·생략/실패0.
- [x] Node 기존6파일94건 통과. 배치29건에는 이번 이력7건(고정 기준/당시·현재 수/중복·순서/읽기 역할/URL 복원/오류 보존/영수증 불일치/배치 이동)이 포함된다.
- [~] 실제 PostgreSQL3건 작성:25개 승인 페이지 탐색 중2개 추가, 원문 삭제 뒤 승인 분모 보존, 적용·원복 혼합 이력과 종료 후 영수증 대조. 기본 로컬 test에서는 조건부 생략이며 SQL/trigger 통과로 계산하지 않는다.
- [ ] 실제 PostgreSQL/Linux·운영 배포·역할별 브라우저/API/DB E2E.
- [ ] 실제320/360/375/768/1024/1440px, 확대·키보드·포커스·스크린리더·저연결 검증. Node DOM 포트/SSR 결과는 렌더링·접근성 완료 증거가 아니다.

성공 기준: 고정 기준의 모든 승인에 누락·중복 없이 접근하고, 삭제/새 승인 이후에도 당시 분모를 유지하며, 정확한 영수증의 현재 결과로 돌아갈 수 있다. 조회에 외부 요청/쓰기 부작용이 없어야 한다.

실패 기준: 페이지 일부를 전체로 표시, 새 승인으로 페이지 밀림, 다른 배치 결과 혼입, 당시 삭제 수를 현재 성공으로 변경, 영수증 불일치 무시, 조회로 수집·정책·적용 실행, 미실행 PG/브라우저를 통과로 보고.

전체 검증·산출물·남은 Gate의 최신 실행 기록은 `announcement-attachment-end-to-end-progress-2026-09-09.md`를 따른다. 이 기능의 로컬 진척은 정책 전체 QA/게시/UI·전체 profile·실파일·운영 Gate 완료가 아니다.
