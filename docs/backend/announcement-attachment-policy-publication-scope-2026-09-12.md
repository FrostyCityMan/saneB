# 첨부 정책 게시 준비 범위 원장 — V76

## 현재 단계와 성공 기준

Gate 1·4 부분 구현이다. 기존 `publication-impact`의 집계 관측과 실제 게시 사이에서 **정확한 현재 DB 대상 ID와 상태 지문을 고정하는 준비 API**를 추가한다. 이 준비 API는 정책 게시/승인/전체 QA/worker 활성화를 수행하지 않는다. 후속 V77 게시 서버 경로는 `announcement-attachment-policy-publication-2026-09-12.md`에 있으며 전체 QA·실제 게시 UI/운영 검증은 필수 미완료다.

- [x] V76·Controller → Service → ServiceImpl → DAO → Mapper XML·DTO 구현.
- [x] 권한·CSRF·멱등 키·입력 제한·불변 범위 조회의 로컬 표적 테스트.
- [~] 실제 PostgreSQL 6사례와 V75→V76/빈 DB migration 검증 작성. 환경 조건 생략은 통과가 아니다.
- [~] V77 게시 transaction/영수증·현재 분류/설치 근거 검증을 연결했다. 전체 profile/worker QA 근거·게시 UI·PG/운영 E2E는 남는다.

성공 기준은 일부 목록이나 같은 건수의 다른 ID를 전체 승인 범위로 오인하지 않도록 완전한 준비 원장을 저장하는 것이다. 원장 저장 성공을 정책 게시 완료로 반환하거나 최신 QA/런타임 검증을 생략하면 실패다. 원래 ATT-001~062 및 운영까지의 목표는 축소하지 않는다.

## DB 계약

`V76__add_attachment_policy_publication_scope.sql`만 추가한다. V1~V75는 이 증분에서 변경하지 않는다.

| 원장 | 저장 내용 |
|---|---|
| announcement_attachment_policy_publication_scopes | 정책·규칙 ID/버전·모드, 선택적인 QA ID/지문, 전체 항목 수·scopeHash, actor·멱등 키·요청/사유 hash, 생성/만료 시각 |
| announcement_attachment_policy_publication_scope_items | scopeId + entityTypeCode + entityId 복합 PK, stateHash |

범위 구성은 `attachment_policy_publication_members`의 다음 전체 집합이다. 페이지/LIMIT/클라이언트 ID 입력으로 고정 대상을 줄이지 않는다.

1. POLICY: 선택 DRAFT와 모든 ACTIVE/RETIRED 정책. 정책 설정·프로필·모드·버전과 연결 규칙 상태 지문.
2. SOURCE: 첨부 정책 binding이 있는 모든 PRODUCTION 원문. 분류/첨부 버전·정책/검수/current pointer·정렬된 link ID 지문.
3. JOB: PRODUCTION의 수집 예약/실행/재시도/중지(COLLECT/RETRY_FILES), 적용 대기 또는 승인된 배치 원복 대기 작업. 일반 원복은 동기 처리이므로 별도 pending 단계가 없다. 적용 PENDING 등 보수적인 잔여 상태도 임의로 생략하지 않는다.
4. COLLECTION_PLAN: PRODUCTION 수집 request의 FROZEN 계획 누적 이력. 현재 실행 중인 요청 수와 같지 않다.
5. COLLECTOR: 활성·미삭제 지자체 수집원 ID와 시스템 파서 설정의 지문. 주소/설정 원문은 해시 계산에만 사용한다.

정책·원문·작업은 서로 다른 차원이다. `itemCount`는 원장 행 수이며 공고 수/HTTP 수/예상 bytes가 아니다. 이 원장은 미래 신규 공고의 무제한 실행 승인이나 실제 전체 Provider 지원 증거가 아니다. 기업마당·정부24 런타임 설정/설치 identity 등은 최종 QA/게시 단계에서 별도로 다시 확인해야 한다.

scopeHash는 DB가 정의한 JSONB 정렬(type C collation, UUID)과 SHA-256으로 계산한다. root의 정책/규칙 버전·모드·QA binding 및 모든 type/ID/stateHash를 포함한다. 관측 시각·요청 키는 포함하지 않는다. 기존 observedImpactHash는 재사용하지 않는다.

OPEN 생성 → 전체 항목 INSERT → SEALED 전환은 같은 DB transaction/xid에서만 허용한다. deferred constraint는 봉인·건수·hash·정책/규칙 버전을 다시 검사하고 원장과 현재 전체 집합의 **양방향 EXCEPT**가 비어 있어야 commit한다. 열린 원장, 누락/추가·동일 건수 교체·잘못된 상태 지문은 rollback한다. 봉인 후 원장/항목 변경·삭제·추가는 금지한다.

항목 entityId는 원문/작업 삭제로 없어지지 않는 비식별 감사 metadata다. 원문 본문·첨부 텍스트·URL·actor 이름·인증 값·사유 원문을 복사하지 않는다. root의 정책/QA/actor FK는 유지한다. 원문 삭제 후 해당 항목이 현재 집합에서 사라지면 준비 현재성은 false가 된다.

## API

기본 경로: `/api/v2/admin/announcement-attachment-policies/{policyId}/publication-scopes`. ApiResponse/PageResponse·no-store를 사용하며 v1은 변경하지 않는다.

| 요청 | 역할·효과 |
|---|---|
| POST / | 활성 ADMIN·비밀번호 변경 완료·CSRF·UUID Idempotency-Key 필요. `{expectedVersion: 0, reason: "검토 사유"}`만 입력. 준비 원장/감사 metadata 저장, 201 |
| GET /?page=1&size=20 | ADMIN/OPERATOR/APPROVER. 정책별 봉인 이력 페이지 |
| GET /{scopeId} | 동일 읽기 역할. 고정 summary + 현재성/만료 여부 |
| GET /{scopeId}/items?page=1&size=20 | 동일 읽기 역할. 고정 type/ID/stateHash 페이지·최초 총수 |

페이지는 1 이상, size 1~100, offset은 int 범위다. Summary는 scopeId/policyId/policyVersion/ruleReleaseId/ruleVersion/modeCode/qaRunId/qaSnapshotHash/itemCount/scopeHash/createdAt/expiresAt다. actor/멱등 키/사유·요청 hash는 공개 DTO에서 제외한다.

Details의 `isApproval=false`, `requiresPublicationRevalidation=true`, `currentHttpRequests=0`은 이 API의 효과다. 실행 중인 전체 시스템 HTTP 수가 아니다. `isScopeCurrent`는 조회 snapshot의 DB 범위/정책 버전이 그대로이며 만료 전인지만 뜻한다. QA 전체 통과·증거 진위·설치·profile 최신성이나 게시 가능성을 뜻하지 않는다.

QA는 해당 정책의 가장 최신 run이 VERIFIED이며 정책/규칙 버전이 일치할 때만 ID/hash를 참고 연결한다. 이전 성공으로 최신 실패를 가리지 않는다. 연결은 승인 증거 재검증이 아니며 QA가 없어도 준비는 가능하다.

준비 유효시간은 10분(DB 최대 15분)이다. 만료된 원장도 이력에서 조회한다. 동일 key·actor·정책·입력 재전송은 만료/범위 변경과 무관하게 원래 원장을 반환한다. 다른 입력/actor/정책의 key 재사용은409다. 같은 key의 동시 INSERT 패자는 실패 transaction을 rollback한 뒤 새 read transaction에서 승자를 조회한다. 자동 새 키나 자동 재고정은 없다.

준비 쓰기는 REPEATABLE READ/20초, 요청 재조회와 GET은 REPEATABLE READ/readOnly/15초다. 401 인증/403 역할·계정/400 입력/404 소속·미존재/409 버전·멱등·저장 모순을 구분하고 SQL 예외 원문은 반환하지 않는다.

## 반드시 남겨둔 게시 경계

이 transaction은 전역 쓰기를 막는 게시 잠금이 아니다. 다른 transaction이 직후 상태를 바꿀 수 있다. 실제 게시 구현은 정확한 승인 scopeHash·현재 정책/규칙/설치·전체 QA 증거를 올바른 잠금 순서에서 다시 대조하고, 해당 규칙의 기존 ACTIVE 퇴역/새 정책 ACTIVE·감사·멱등 영수증을 원자적으로 기록해야 한다. 기존 원문/첨부 판정·검수·운영 공고는 자동 변경하지 않는다. ENFORCE 기존 데이터는 별도의 범위 승인 배치를 사용한다.

이번 증분은 UI를 변경하지 않는다. 기존 화면의 ‘실제 게시 미제공’ 안내를 유지하며 범위 준비 버튼을 게시 버튼으로 대체하지 않는다. 최종 전체 검증 결과와 환경 차단은 장기 진행 기록에 기록한다.
