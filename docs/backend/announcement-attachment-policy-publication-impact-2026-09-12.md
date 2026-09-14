# 첨부 정책 게시 전 영향 조회

## 범위와 설계 결정

상세 설계의 게시 전 영향 확인을 위한 **읽기 전용 API**다. 정책 게시/QA 전체 검증/기존 데이터 적용 API가 아니며 이 조회의 성공을 게시 승인으로 사용하지 않는다. 전체 정책 수명주기 구현 중 한 단계이고 publication transaction·정책 UI·실제 QA/운영 Gate는 계속 필수다.

현재 worker의 `ExternalExecutionConditions`를 기준으로, ACTIVE 키워드 규칙에 연결된 ACTIVE/OFF 정책은 **다른 규칙의 고정 작업을 포함해** 새 외부 요청을 막는다. 기존 정책이 RETIRED가 되어도 고정 작업은 원래 정책을 참조한다. 따라서 해당 규칙 범위만 보고 OFF 영향을 계산하지 않는다.

- `matchingRule`: 대상 정책의 ruleReleaseId에 연결된 모든 정책(현재·퇴역 포함)의 범위.
- `allRules`: 전체 규칙 범위. matchingRule을 포함하며 두 범위의 수를 합산하지 않는다.
- 출처와 작업은 PRODUCTION만, 수집 계획은 원래 수집 request의 PRODUCTION만 포함한다. QA를 운영 건수에 섞지 않는다.
- 기존 제목 제외·실패 상태라도 남아 있는 정책 binding은 임의로 숨기지 않는다. 원문/제목/URL은 반환하지 않고 보존 영향의 집계만 제공한다.

## DB-first 조회 계약

V72 정책/작업과 V73 수집 계획/QA 이력을 사용한다. migration 추가·수정은 없다.

1. REPEATABLE READ/readOnly(timeout15초)의 한 transaction에서 정책, 같은 규칙의 ACTIVE 정책, 두 범위 집계, 최신 QA와 단계를 읽는다. SELECT FOR UPDATE/SHARE, 감사 INSERT, lease/worker 변경을 하지 않는다.
2. ACTIVE 정책은 V72의 규칙별 unique index 계약에 맞게0~1건이다. 전체 count와 반환 수가 다르거나 중복 ACTIVE면409다. 다른 규칙의 정책을 같은 규칙의 교체 대상으로 반환하지 않는다.
3. source/job/plan을 각각 집계한 뒤 한 행으로 합친다. 원문에 여러 job/link가 있다고 source 수가 증식하지 않는다. source link는 EXISTS로 세며 source/job/plan 사이에 page/LIMIT를 적용하지 않는다.
4. 모든 수는0이상의 long이며 API에서 정확한 정수로 취급할 수 있는 범위까지 허용한다. 부분 범위가 전체보다 크거나 subset 건수가 분모보다 크면409다. NULL·미집계를0으로 꾸미지 않는다.

| Counts 필드 | 정확한 의미 |
|---|---|
| boundSourceCount | attachment_policy_id가 있는 PRODUCTION 원문 수 |
| reviewRequiredSourceCount | 그중 is_attachment_review_required인 원문 수. 유효 확인 완료 수가 아님 |
| effectiveAttachmentSourceCount | 그중 current_attachment_evaluation_id가 있는 원문 수. 유효 후보/최종 선정 수가 아님 |
| linkedSourceCount | 그중 운영 공고 link가 하나 이상 있는 원문 수 |
| frozenCollectionJobCount | COLLECT/RETRY_FILES의 SCOPE_READY/PENDING/RUNNING/RETRY_WAIT/PAUSED 작업 수 |
| runningCollectionJobCount | 그중 RUNNING 작업 수. 실제 전송 중 HTTP 요청 수가 아님 |
| applicationPendingJobCount | application_status_code=PENDING 작업 수 |
| rollbackPendingJobCount | rollback_action_id가 있고 rollback_status_code=NOT_REQUESTED인 작업 수 |
| frozenCollectionPlanCount | PRODUCTION request에 속한 FROZEN 계획 **누적 이력 수**. 현재 실행 run 수가 아님 |

source 관련 세부 수는 서로 겹칠 수 있다. 작업의 수집/적용/원복 집계도 별도 차원이므로 더해서 전체 원문 수나 요청량으로 사용하지 않는다. 향후 HTTP 총량·신규 대상 수는 시간 구간/명시적 범위가 없는 이 GET에서 추정하지 않는다.

## API와 관측 지문

`GET /api/v2/admin/announcement-attachment-policies/{policyId}/publication-impact`

활성 ADMIN/OPERATOR/APPROVER·비밀번호 변경 완료 계정이 조회한다. 기존 `/api/v1`과 v2 초안/QA 계약을 유지하며 ApiResponse/no-store를 사용한다. Controller → Service → ServiceImpl → DAO → Mapper XML 계층이다. POST 게시 기능은 이 경로에 없으며 CSRF가 있어도405다.

응답:

- `policy`: 조회 대상의 기존 Summary.
- `activePolicyForRule`: 같은 규칙의 현재 ACTIVE Summary, 없으면null. 없음을 OFF 정책으로 합성하지 않는다.
- `matchingRule`, `allRules`: 위 Counts.
- `maximumSourceBytes`: 초안 설정의 공고별 상한(1~83886080바이트). 전체 다운로드 예측치가 아니다.
- `wouldStopNewExternalRequests`: 조회 당시 대상 규칙이 ACTIVE이고 제안 모드가 OFF일 때true. **그 상태로 게시한다는 가정**의 신규 외부 요청 중지 영향이며 실제 게시/즉시 HTTP 취소가 아니다.
- `wouldLiftGlobalOffStop`: 위 ACTIVE 규칙의 현재 정책이 OFF이고 제안 모드가 COLLECT_ONLY/ENFORCE일 때true. 전역 OFF 중지 조건의 해제 영향일 뿐 worker flag·profile·예산·버전 검증을 우회하거나 모든 작업을 재개한다는 뜻이 아니다.
- `latestQa`: 최신 run 식별자/상태/rowVersion/정책·규칙 버전/inputVersionsCurrent/snapshotHash/단계코드·상태·증거hash/종료시각. 없으면null.
- `blockingReasonCodes`: 현재 확인 가능한 차단 사유. 아래 최종 재검증 사유는 항상 포함한다.
- `requiresPublicationRevalidation=true`, `currentHttpRequests=0`.
- `observedImpactHash`, `observedAt`: 관측 내용의 SHA-256과 관측 시각.

지문은 정책/현재 ACTIVE/집계/QA metadata/차단 사유/설정·profile 지문을 포함하고 조회 시각은 제외한다. 같은 내용은 같은 지문이며 건수·정책·QA 변경은 지문을 바꾼다. **source ID/개별 버전 전체를 고정한 scope hash는 아니다. 같은 건수의 대상 교체는 반영되지 않을 수 있으며, 승인 키나 게시 CAS 조건으로 그대로 사용할 수 없다.** 게시 구현은 실제 QA snapshot/현재 런타임과 승인한 정확한 대상/영향을 transaction에서 다시 검증해야 한다.

목록에는 원문·본문·첨부·URL·actor·사유·멱등 키·요청 hash·QA evidence JSON·input snapshot을 넣지 않는다. QA metadata의 단계별 상태만 반환하며 증거 내용의 정확성/최신 runtime은 이 조회에서 새로 검증하지 않는다.

## QA 상태와 차단 사유

| 코드 | 의미 |
|---|---|
| POLICY_NOT_DRAFT | 초안이 아니므로 새 게시 대상으로 사용할 수 없음 |
| KEYWORD_RULE_NOT_ACTIVE | 참조 키워드 규칙이 현재 ACTIVE가 아님. 자동 게시하지 않음 |
| QA_NOT_REQUESTED | 해당 정책의 QA 실행 이력 없음 |
| QA_NOT_VERIFIED | 최신 QA가 VERIFIED가 아님. 예전 통과 이력을 자동 선택하지 않음 |
| QA_INPUT_VERSIONS_CHANGED | 최신 QA와 현재 정책/규칙 DB 버전이 일치하지 않음 |
| QA_REQUIRED_STEPS_NOT_PASSED | 필수4단계 중 누락/실패/MISSING 존재. 저장되지 않은 단계는NOT_RUN |
| PUBLICATION_REVALIDATION_REQUIRED | 조회는 최종 검증/동의/게시 transaction이 아니므로 반드시 다시 검증 필요 |

VERIFIED·PASSED metadata가4개 있어도 `canPublish=true`나 게시 승인 토큰을 생성하지 않는다. 단계 소속/중복/코드/지문 형식이 모순되면409로 조회를 거부한다. inputVersionsCurrent도 DB 버전의 현재성일 뿐 설치/전체 수집원/실파일/worker 검증의 현재성이 아니다.

비로그인401, 권한·계정 상태403, 잘못된UUID/누락ID400, 없는 정책404, 저장 계약 모순409다. 보안/DB 예외 원문이나 입력 원문을 응답하지 않는다.

## 검증·남은 작업

- [x] 표적 Service16·HTTP10·Mapper31건, 총57건 로컬 통과(29초). 읽기3역할/금지역할/직접 Service, 전체·규칙 범위/OFF 영향, 고정 관측 지문, QA 누락/STALE/모든PASSED도 재검증, 소속/집계 오류, POST 거부를 확인했다.
- [~] 실제 PostgreSQL3건 작성: 빈 초안 조회와 audit/정책 불변, PRODUCTION/QA 원문·작업 분리 및 상태 불변, 퇴역 정책 FROZEN 이력과 QA request 제외·ACTIVE 교체 후 지문. 기본 테스트에서는 조건부 생략이며 실행 성공을 뜻하지 않는다.
- [ ] 실제 PG/Linux 검증, 최종 QA 증거/코드·설치·전체 profile 재검증, 승인 범위 게시 transaction, 관리자 UI, 운영 배포/역할별 브라우저 E2E.

성공 기준: 정책 교체/중지 전에 실제 관측 분모와 기존 보호 대상·고정 작업을 확인하고, 범위·시점·QA 근거의 한계를 명확히 드러낸다. 실패 기준: 다른 규칙 작업에 대한 OFF 영향을 누락, QA/운영 혼합, 누적 계획을 현재 요청으로 표시, 일부 결과/최신 실패를 숨김, 조회를 게시 승인 또는 실행으로 처리.

장기 목표의 publication과 실제 QA 요구는 그대로 유지한다. 구현과 이 문서는 운영 게시·ENFORCE·기존 데이터 적용 승인을 대신하지 않는다. 최종 실행 증거는 장기 진행 문서에 기록한다.

09-12 후속으로 V76과 `publication-scopes` 준비 API에서 정확한 ID/상태 지문 전체 고정을 추가했다. 이 GET의 observedImpactHash 의미는 바꾸지 않는다. 고정 준비 역시 게시 승인이 아니며 최종 transaction/UI와 전체 QA는 남는다. 상세는 `announcement-attachment-policy-publication-scope-2026-09-12.md`를 따른다.
