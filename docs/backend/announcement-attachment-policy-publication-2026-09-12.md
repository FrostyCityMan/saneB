# 첨부 정책 게시 서버 경로 — V77

## 현재 상태 / Gate

Gate 3·4 부분 구현, 전체 Not ready다. 실제 게시용 transaction·영수증·POST/GET API와 QA 근거 재검증을 연결했다. **전체 Provider/worker DB QA 실행기와 그 근거 검증기는 미연결**이므로 현재 시스템은 정상적인 전체 QA VERIFIED→정책 게시 성공을 만들 수 없다. 이를 메타데이터나 대역 테스트로 우회하지 않는다. 후속 관리자 게시 UI와 로컬 합성 브라우저 흐름을 연결했지만 실제 PostgreSQL/Linux·운영 게시/브라우저 E2E는 필수 미완료다.

- [x] 기존 ACTIVE 퇴역 + DRAFT 게시 + 불변 영수증 + 감사 metadata의 원자적 코드 경로.
- [x] 현재 준비 범위/최신 QA/규칙·설치 지문과 분류 정답·합성 runtime fixture 재검증.
- [x] Service/HTTP/QA verifier/RuntimeGate/Mapper/Migration 표적 로컬 검증.
- [~] 실제 PG4사례·V76→V77 migration 시험 작성. 실행 환경에서 생략되면 통과가 아니다.
- [~] 게시 준비/실행/영수증 UI와 로컬 합성 브라우저 검증. 운영 역할·실제 서버/DB 게시 E2E는 남음.
- [ ] PROVIDER_PROFILES·WORKER_DB_RECOVERY 전체 실행기/근거 검증기, 운영 전체 검증.

이 파일은 기능 일부의 구현 기록이며 운영 정책 게시·ENFORCE·기존 데이터 승인서가 아니다. ATT-001~062와 전체 목표는 그대로 유지한다.

## 입력과 응답

경로: `/api/v2/admin/announcement-attachment-policies/{policyId}/publication`.

POST는 활성 ADMIN·비밀번호 변경 완료·CSRF·UUID `Idempotency-Key`가 필요하다. 입력은 아래만 허용한다.

- scopeId: V76에서 현재 관리자가 준비한 UUID.
- scopeHash: 고정된 전체 범위의 SHA-256. 관측용 observedImpactHash가 아니다.
- expectedVersion: 준비 당시 DRAFT의 rowVersion.
- acknowledgeNewCollectionBehavior=true: 해당 모드가 앞으로의 신규 수집 조건에 미치는 영향 확인.
- acknowledgeExistingJobsUnchanged=true: 기존 고정 작업은 원래 정책을 유지하고, 현재 ACTIVE 규칙의 OFF는 다른 규칙 작업의 새 외부 요청도 중지할 수 있음을 확인.
- acknowledgeNoBackfill=true: 기존 데이터 일괄 적용은 별도 범위 승인 배치임을 확인.
- reason: 공백 아닌 1~1000자. 원문은 저장하지 않고 hash만 감사 metadata에 남긴다.

QA 결과·passed·모드·규칙·프로필·정책 hash·임의 원문 ID 목록을 입력할 수 없다. 게시 준비와 실제 게시의 키는 서로 다른 작업 키다. 동일 게시 key/actor/정책/입력 재전송은 원래 영수증을 반환한다. 준비 만료나 이후 정책 퇴역이 있어도 재게시하지 않는다. 다른 입력의 key 재사용은409다.

POST201 및 GET200은 ApiResponse/no-store로 `publication`, `existingDataApplied=false`, `workerEnabledByRequest=false`, `currentHttpRequests=0`을 반환한다. 이0은 해당 API의 직접 요청 수이며, 게시 이후 별도 상시 scheduler가 새 정책 조건으로 실행할 미래 HTTP 수가 아니다.

publication 필드: publicationId/policyId/publishedPolicyVersion/policyHash/previousPolicyId/previousPolicyVersion/scopeId/scopeHash/qaRunId/modeCode/publishedAt. previousPolicyVersion은 **교체 직전 버전**이다. 같은 정책은 한 번만 게시하고 재개정은 새 policyId를 사용한다. 이후 퇴역해도 GET은 당시 불변 영수증을 반환한다. GET은 ADMIN/OPERATOR/APPROVER가 조회하는 정책별 단건이며 목록 페이지가 아니다.

401 인증,403 역할/계정,400 형식/영향 확인 누락,404 소속/영수증 없음,409 오래된 범위·불충분 QA·잠금 사용 중·멱등성/버전 충돌을 구분한다. DB/파일 예외 원문과 사유 원문은 반환하지 않는다.

## 최종 검증과 잠금

1. 요청/역할을 확인하고 기존 key가 있으면 저장된 영수증만 반환한다.
2. transaction 밖에서 설치된 Linux 격리 추출기 identity/suite 및 전체 애플리케이션 class/resource 지문을 읽는다. 짧은 REPEATABLE READ에서 현재 scope·정책·최신 QA run/단계·고정 입력을 준비한다. Windows fallback·추출·다운로드는 하지 않는다.
3. DB transaction 밖에서 현재 입력과 저장된 QA를 대조하고 네 단계의 typed 근거/지문을 재검증한다. 서버가 새로 읽은 ruleSet으로 AG-001~030 결과를 재계산한다. runtime은 현재12개 빌드 fixture의 input/text hash·품질·형식·문자/블록 수·원본 제거·결과 hash와 실행 시각 소속을 대조한다. 외부 파일을 추출하지 않는다. 이어 설치·애플리케이션 지문을 다시 읽어 처음과 같은지 확인한다.
4. READ COMMITTED/15초 transaction에서 `attachment_policy_publication_lock()`을 호출한다. 같은 요청의 선행 영수증을 확인하고 현재 scope 소속·해시·버전·만료·전체 membership, ACTIVE 규칙을 참조하는 DRAFT 정책, 최신 QA run/단계/입력 snapshot을 다시 읽는다. 정책·규칙·scope·run·단계 근거·snapshot hash/JSON이 잠금 밖에서 검증한 값과 같아야 한다. 준비 시점 RR의 오래된 결과만 재사용하지 않는다.
5. 파일 읽기와 QA 재실행은 쓰기 잠금 안에서 하지 않는다. 근거 검증 중 같은 key의 다른 실행이 먼저 게시한 경우에는 동일 actor/정책/요청 지문을 확인하고 원래 영수증을 반환한다. 새로운 key나 다른 내용의 성공으로 대체하지 않는다.
6. V77 영수증 INSERT → 같은 규칙의 이전 ACTIVE를 RETIRED/버전+1 → 새 DRAFT를 ACTIVE/버전+1 → 감사 metadata를 같은 transaction에 저장한다. 새 policyHash는 검증된 QA snapshot hash로 고정한다. 초안의 `extractorConfigHash=null`은 QA 입력에 이미 고정된 installed.runtimeHash로만 해소한다. 모드·규칙·profile·그 외 설정은 바꾸지 않으며, DB도 QA settings와 runtime 필드 외 나머지 설정이 같음을 검사한다. 사용자가 임의 hash를 제출할 수 없다.
7. deferred constraint는 영수증만 있거나 교체 일부만 완료된 commit을 거부한다. 기존 source/job의 판정·pointer·검수·정책 binding, 배치, 운영 공고 상태는 자동 변경하지 않는다.

잠금은 정책·QA·준비/게시 원장·키워드 규칙/그룹/규칙어/용어·원문/link/job·수집 계획/run/request·지자체 source/profile 총18개 테이블의 **EXCLUSIVE NOWAIT**다. 일반 SELECT는 허용하지만 다른 DML과 SELECT FOR UPDATE/SHARE는 해당 짧은 게시 구간에서 차단된다. 기존 writer/row-lock 업무가 있으면 게시가 즉시409로 실패하며 자동 대기·반복하지 않는다. 트랜잭션은15초 제한이다. 파일 지문과 QA CPU 검증은 잠금 전에 끝내고 잠금 안에서는 DB 현재성을 대조한다. 외부 다운로드/추출은 하지 않는다. 실제 규모의 지연·경합 성능은 PG/운영 Gate에서 검증해야 한다. 초기 저빈도 정책 게시의 명확한 전역 경계이며, 고빈도 실행으로 바꾸려면 전체 writer 경계를 다시 설계해야 한다.

## QA 증거의 구현 경계

`AttachmentPolicyPublicationQaVerifier`는 run의 VERIFIED 문자열만 보지 않는다. 고정 입력, 네 단계의 run 소속/시각/중복·누락, 현재 정답/fixture, 저장 evidence hash를 검사한다. JSONB 저장 과정의 속성 순서 변화는 typed record로 복구해서 hash를 계산하므로 변조로 오인하지 않는다. 모르는 필드는 거부한다.

현재 두 추가 단계에는 실제 verifier bean이 없다. `AttachmentPolicyAdditionalQaEvidenceVerifier`는 후속 전체 출처/worker QA 실행기와 짝지을 내부 계약이다. 임의 JSON·성공 count·과거 CLI 결과를 받아 hash만 반환하는 구현을 추가해서는 안 된다. 실제 전체 대상/사례 catalog, 실행 전후 입력/설치 지문, 누락/skip/실패·정리/복구 결과를 검증한 뒤에만 근거 지문을 반환해야 한다. 구현 전에는 명확한409로 게시를 거부한다.

V73 QA worker는 여전히 이 두 단계를 MISSING/INCOMPLETE로 기록한다. 이를 VERIFIED로 바꾸지 않았다. QA 입력 schemaVersion2는 전체 애플리케이션 class/resource를 검증한 코드 지문을 포함하므로 이전 입력 hash를 재사용할 수 없고 새 전체 QA가 필요하다. 상세는 `announcement-attachment-code-fingerprint-2026-09-12.md`다.

## 검증과 남은 작업

- 표적 테스트는 Service·HTTP·근거 변조/누락·JSONB 속성 순서·runtime 저장 결과·Mapper/DDL 계약이다. positive Service/HTTP는 대역이며 실제 정책 게시 성공이 아니다.
- PG4사례: 원자적 정책 교체와 기존 작업 보존, 영수증만/부분 교체 commit 거부, 오래된 scope 거부, 기존 writer 앞 NOWAIT 실패(SQLSTATE55P03). DB용 VERIFIED metadata fixture는 SQL 제약 시험용이며 Java verifier를 통과하는 전체 QA 결과가 아니다.
- V1~V76는 이 증분에서 수정하지 않는다. V77 자체에는 운영 정책 게시/worker 활성화/기존 데이터 DML이 없다.
- 실행 UI는 후속 `announcement-attachment-policy-publication-ui-2026-09-12.md`에서 연결했다. 미해결 QA 차단·범위/효과/복구 확인과 요청 유실 동일 키 재확인을 로컬 합성 브라우저로 검증했다. 전역 쓰기 경계·실제 Linux/DB/운영 게시를 실행한 증거는 아니다.
- 최종 실행 명령·결과·환경·산출물 hash는 `announcement-attachment-end-to-end-progress-2026-09-09.md`에 기록한다.
