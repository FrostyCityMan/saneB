# 첨부 수집 ATT-001~062 구현·검증 추적표

## 2026-10-05 최신 앱 코드 Linux 통합 검증 완료

앱 코드 `5f0e274ea88e6c700a8fc671d098df6807e784ca`의 run37261775271이 completed/success로 종료됐다. artifact11325571457을 `build/ci-evidence-37261775271`에 내려받아 XML을 대조했다. 일반 test(앱·추출기 합산)4,922건 중4,521통과/401조건부 생략/실패·오류0이다. 별도 QA20·작업229·migration/Backfill18·정책DB2·runtime7·worker12·Flyway5건은 모두 실패·오류·생략0이다.

Flyway5건에서 정부24 본문 버전 저장 사례와 `gov24CollectionRunRefreshesSnapshotAndRetainsPreviousContentVersion`이 실제 실행됐음을 확인했다. 독립 보고서 SYNTHETIC_WORKER_DB_CONTRACTS_V2는259발견/259통과/실패0/생략0/미실행0이며 ATTACHMENT_CONTRACT_QA_CLEANUP/POLICY_DB_QA_CLEANUP 모두 SUCCEEDED다. 일반 test의 생략을 통과로 합산하지 않으며, 독립 시험259건은 별도 task 사례와 중복되므로 총 고유 사례 수로 더하지 않는다.

문서 전용 후속 HEAD `96f576d`는 검증 앱 SHA와 main 코드가 동일하다. 최신 Linux 코드 검증 하위 Gate는 통과했지만 실제 정부24 API/첨부 profile·최신 코드 운영 설치·검수/DRAFT·기존 데이터 적용·운영 E2E는 남는다. 정확한 앱 SHA 설치·재시작 승인을 요청했으며 정책/기능 활성화 승인을 대신하지 않는다.

## 2026-10-05 정부24 상세 저장 증분 Linux 증거

`4b482c3c6ba234d3c2be1240797ffa58be9de290`의 run37260946511이 completed/success로 종료됐다. artifact11324493047을 `build/ci-evidence-37260946511`에 내려받아 XML을 직접 대조했다. 일반 test(앱·추출기 합산)4,921건/조건부 생략400건/실패·오류0, 별도 QA20·작업229·migration/Backfill18·정책DB2·runtime7·worker12·Flyway4건은 실패·오류·생략0이다.

Flyway XML에는 `gov24DetailContentVersionsKeepSummaryProvenanceAndDeduplicateIdenticalBody`의 실제 실행이 포함된다. 독립 보고서 SYNTHETIC_WORKER_DB_CONTRACTS_V2는 discovered259/passed259/failed0/skipped0/notRun0이며 두 정리 표식 ATTACHMENT_CONTRACT_QA_CLEANUP/POLICY_DB_QA_CLEANUP은 SUCCEEDED다. 이 결과는 합성 제공자·임시 DB 검증이며 실제 정부24 API/첨부·운영 E2E 증거가 아니다.

후속 공통 중복 SQL null 매개변수 수정과 수집 service 통합 사례를 포함한 최신 `5f0e274`의 run37261775271은 현재 실행 중이다. 위 선행 결과를 최신 SHA의 전체 통과로 대체하지 않는다. 새 run 종료 후 Flyway5건과 신규 수집 service 사례의 실제 실행을 별도로 대조해야 한다.

## 2026-10-05 설치 증거와 잔여 운영 Gate 정합성 정정

운영 설치 증거는 `docs/deployment/diagnostic-iam-preflight-2026-10-05.md`의 11:56 KST 진단을 기준으로 한다. `00d16d9` 배포37256870718/CodeDeploy d-A8GG4SJ6L 성공, DB V90·migration 실패0·설치/배포 JAR 지문 일치·worker/source batch true를 확인한 이력이다. 현재 시각의 운영 상태를 다시 조회한 결과는 아니다.

12:00 KST 브라우저 조회 증거는 `docs/deployment/attachment-operator-acceptance-2026-10-02.md`를 따른다. 정책 검증 범위 조회 및 기존 HWPX 저장 근거 조회 성공은 신규 다운로드·연결 공고 처리·검수/DRAFT 성공을 뜻하지 않는다. ATT-051/059의 오래된 배포 중/worker 비활성 설명만 정정하며 완료 상태를 올리지 않는다.

정부24 상세 본문 후속 코드 `5f0e274`는 위 설치에 포함되지 않는다. 해당 코드의 Linux 실행37261775271은 결과 확인 전이며, 정부24 첨부 profile·실제 API·운영 활성화는 별도 미완료다. 상세 구현·로컬 검증은 `announcement-attachment-gov24-gap-audit-2026-10-05.md`에 기록한다.

## 2026-10-03 최신 QA 도구 커밋의 Linux 재검증 완료

`00d16d9cecd01159006a316c5cc7be3c30fd64f6` / run37087157343은 completed/success다. artifact11261158314 XML 대조 결과: QA20, 작업229, migration/Backfill18, 정책DB2, runtime7, worker12, Flyway3 모두 실패/오류/생략0. 일반 test4891건은 실패/오류0·조건부 생략399건이며 일부 DB 시험은 별도 task에서 실행됐다. 실사이트 선택 실행과 운영 E2E까지 통과했다고 해석하지 않는다.

독립 실행 보고서 SYNTHETIC_WORKER_DB_CONTRACTS_V2는 discovered259/passed259/failed0/skipped0/notRun0이다. ATTACHMENT_CONTRACT_QA_CLEANUP 및 POLICY_DB_QA_CLEANUP 모두 SUCCEEDED. 로컬 증거 사본: `build/ci-evidence-37087157343-a8a74611b1f24cd48350ba68a34138f7`.

읽기 전용 운영 metadata 재확인: 서울 runningInstances1·SSM Online·DB available·삭제보호/암호화 true·health UP. remoteCommands0/writes0. 최신 성공 배포 이력은371b1ac이나 현재 설치 SHA/DB migration을 직접 조회한 증거가 아니므로 구분한다. V87~V90 설치·재시작 승인과 배포 전 진행 중 작업/복구 기준 확인이 남는다. 정책 게시·ENFORCE·신규 수집 예약·기존 데이터 적용은 실행하지 않았다.

## 2026-10-03 연결 근거 예약 로컬 브라우저 검증

- 후속 브라우저 검증: 동일 합성 배치를 예약→수집 시작→중지→재개하여 SCOPE_READY/0→COLLECTION_PENDING/1→COLLECTION_PAUSED/2→COLLECTING/3을 확인했다. 각 승인 확인 체크는 초기화되고 최초132회·24MiB 예산은 유지된다. 실제 worker가 없는 합성 상태 전환이며 실파일 다운로드 성공을 의미하지 않는다.
- 조회 전용 페이지는 동일 배치 조회가 가능하고 수집 제어 대신 관리자 요청 안내가 표시된다. 두 탭의 console error/warn 0건. 모바일 override375×812에서 실제 document clientWidth/scrollWidth는360/360으로 가로 넘침이 없었다. 모든 반응형 구간·모든 폼을 검증했다는 의미는 아니다. `linked-resumed.jpg`, `linked-readonly-mobile.jpg`를 같은 build 증거 폴더에 저장했다. viewport 복원·생성 탭 종료·서버 PID40256 종료 완료.
- 테스트 서버의 재개 응답을 실제 클라이언트 계약인 COLLECTING으로 정정했다. 이 정정은 합성 서버에만 적용되며 제품 코드에는 변경이 없다.

- 실제 Thymeleaf 배치 화면을 `SANEB_ATTACHMENT_BATCH_VIEW_EXPORT=true`일 때만 내보내는 테스트 경로와 `scripts/qa/attachment-linked-batch-fixture-server.mjs`를 추가했다. localhost 임의 포트의 단일 탭 합성 API이며 운영 DB·인증·외부 수집에 연결하지 않는다. 운영 보안·worker 검증 대체물이 아니다.
- 실행: `:test --tests '*AnnouncementAttachmentBatchViewControllerSmokeTest' --no-daemon --max-workers=1` 36초 성공, XML 8건/실패0/오류0/생략0. `node --test scripts/qa/attachment-batch-ui.test.mjs` 37건 통과, `node --check scripts/qa/attachment-linked-batch-fixture-server.mjs` 성공.
- 실제 인앱 브라우저에서 합성 UUID 1건·24MiB 상한 입력 → 준비 상태 조회 → 영향 확인 → 사유·확인 체크 → 예약 응답과 선택 배치 조회까지 검증했다. `SCOPE_READY`·1건·132요청 상한, 연결 근거 전용 표시 및 별도 수집 시작 버튼을 확인했다. 예약만으로 수집하지 않았다. 해당 탭 console error/warn 0건.
- 증거: `build/attachment-batch-ui-qa/linked-reserved.jpg` 전체 화면. 실제 운영 E2E, 모바일 viewport, 수집 시작/일시정지/재개 브라우저 상호작용, 응답 유실·조회 역할 브라우저 시나리오는 이번 실행에서 미검증이다. 테스트 전용 서버 PID25964와 생성 탭은 종료했다.
- 재실행: 위 환경변수로 SSR 테스트 수행 후 `node scripts/qa/attachment-linked-batch-fixture-server.mjs`; 출력된 loopback URL만 사용한다. 이 서버의 역할/메모리 상태는 프로세스 단위이므로 다중 사용자 검증에는 사용하지 않는다. 종료하면 합성 예약은 소멸한다.

## 2026-10-03 수정 SHA Linux 통합 검증 통과

`68cd3a61962e2c4824e0f831572264b84ff205b7`의 GitHub Actions run37085618683 completed/success를 확인했다. artifact11259989027의 XML 직접 대조: QA 산출물20, 작업229, migration/Backfill18, 정책 DB QA2, 격리 runtime7, worker12, Flyway3 모두 실패/오류/생략0이다. 일반 test 디렉터리(앱/추출기 합산)는4891건 중399건 조건부 생략이며 별도 task에서 실행한 DB/worker 시험도 이 생략 목록에 포함된다. 모든 실사이트 QA까지 통과했다고 확대하지 않는다.

독립 산출물 실행 로그의 SYNTHETIC_WORKER_DB_CONTRACTS_V2는 discovered259/passed259/failed0/skipped0/notRun0, ATTACHMENT_CONTRACT_QA_CLEANUP 및 POLICY_DB_QA_CLEANUP 모두 SUCCEEDED다. 운영 파일 수집·운영 DB·브라우저 E2E 증거가 아니라 Linux 격리 합성 검증이다. 원 보고서는 GitHub artifact에 보관되어 있으며 로컬 검토 사본은 build/ci-evidence-37085618683-383987d22282401f87c1dd4da7a71440이다.

운영 코드·V87~V90 반영은 조건부 승인 요청 상태이며 아직 실행하지 않았다. 배포 전 진행 중 작업·현재 버전·이전 JAR/추출기 및 DB 복구 기준 확인이 남는다. 정책 게시/ENFORCE/기존 데이터 적용은 별도다. 검증 SHA를 유지하기 위해 이 결과 기록만으로 추가 CI를 발생시키지 않는다.

## 2026-10-03 68cd3a6 전체 DB 회귀

코드 HEAD `68cd3a61962e2c4824e0f831572264b84ff205b7`에서 `attachmentJobIntegrationTest attachmentMigrationTest --no-daemon --max-workers=1`을 실행했다. 동일 세션20169를 유지하여13분43초 정상 종료를 확인했다. 새 XML 기준 작업229·Backfill14·migration4, 총247건 실패/오류/생략0이다. 기존 migration diff는 운영 기준371b1ac 대비 V87~V90 추가4개뿐이며 기존 파일 수정0이다. 로컬 임시 PostgreSQL 검증은 운영 DB 반영 증거가 아니다.

장시간 무출력 구간에서도 재시작하지 않았다. 중간 JVM 진단은 Windows attach 접근 거부로 미실행이었으나 이후 작업→migration 진행과 정상 종료를 확인했다. 수정 전 Linux 실패를 로컬 통과로 덮지 않으며 수정 SHA run37085618683의 최종 결과는 별도 확인한다. 운영 health UP 조회는 기존 설치의 기동 증거뿐이다. 운영 코드·정책·데이터 변경은 실행하지 않았다.

## 2026-10-03 Linux QA 산출물의 정적 시험 목록 계약 복구

선행 `2a643c7` Linux run37084709367은 `AttachmentContractQaPackageTest.fixedSuiteInventoryCannotOmitDynamicallyRegisteredTestCases`에서 실패했다. 새 linked 통합 시험의 ParameterizedTest 두 개가 독립 QA 산출물의 사전 시험 목록 고정 규칙과 충돌한 원인이다. 검사/기대값을 약화하지 않고 동일5사례(연결 ID 교체·공고 ID 교체·삭제, 정상·부분 완료)를 명시적 @Test5개와 공통 private helper로 변환했다. 총 linked 사례17개는 유지한다. 최신 `bf6a564` run37085319530은 조회 당시 in_progress이며 아직 수정 전 코드다.

정정 후 `attachmentContractQaTest attachmentJobIntegrationTest --tests '*linked*' --tests '*fixedSuiteInventoryCannotOmitDynamicallyRegisteredTestCases' --no-daemon --max-workers=1`1분49초 성공. QA runner10·산출물6·정책 DB QA runner4·실제 PostgreSQL linked17, 총37건 실패/오류/생략0이다. QA 산출물 재생성·시험은 로컬에서 통과했으며 수정 SHA의 Linux 전체 실행은 별도 확인해야 한다.

관리자 배치/전체 범위/검수/복구/정책/큐/구간/운영/Provider QA/Provider coverage 및 CSRF Node 회귀283건 실패/생략0을 확인했다. 현재 서울 SSO Inventory 읽기 전용 조회는 계정/역할 pin 일치, 실행 EC2 1대·SSM Online·DB available·삭제 방지/암호화 true였다. 원격 명령0·운영 쓰기0이며 임시 CA 제거를 확인했다. 이는 앱 버전/health/worker/E2E 검증이 아니다. V87~V90 운영 migration 및 복구 검증은 여전히 미완료다.

## 2026-10-03 연결 근거 수집의 신청·진행 하위 데이터 보존

ATT-051 성공/부분 완료 통합 fixture에 수치 조건·공고 진행 단계·매칭·신청 진행·단계 상태를 실제 격리 PostgreSQL 행으로 추가했다. 예약→시작→합성 첨부 봉인→평가/경고→일반 적용 및 검수/DRAFT 오용 거부 이후 다섯 테이블의 대상 행 전체 JSON snapshot이 전후 동일함을 검증한다. source/announcements 전체 행 불변 검증도 유지한다. 기존 공고 조건·신청 데이터의 빈 테이블만 비교하는 시험이 아니다.

`attachmentJobIntegrationTest --tests '*linked*' --no-daemon --max-workers=1`1분27초 성공,17건 실패/오류/생략0. 정상/부분 파일 근거는 합성이며 실제 외부 파일·운영 DB·동시 최종 완료 경합 성공으로 확대하지 않는다. migration/운영 코드 변경은 없다. 임시 PostgreSQL과 Gradle 시험은 종료됐다.

조회 당시 UI HEAD `6c8df1f`의 Linux run37085079615는 pending, 선행 `2a643c7`의 run37084709367은 단위·HTTP·임시 DB·migration 단계 in_progress였다. 두 상태 모두 Linux 통과나 운영 배포 증거가 아니다.

## 2026-10-03 연결 공고 전용 예약 폼 연결

Design Read: 기존 배치 화면의 plain-panel·접힘 보조 영역·승인 폼·정책 목록·오류 복구를 재사용한다. 내부 운영자의 명시 UUID/공고별 bytes 입력→전체 준비 조회→영향 확인→ADMIN 예약→기존 배치 상세 흐름이다. 예약은 외부 HTTP0이며 수집 시작은 별도 R2 확인이다. 사유·미체크 확인, 로딩 잠금, 조회 실패 시 예약 권한 초기화/입력 보존, 입력 변경 시 동의 해제, 읽기 전용 역할 예약 차단을 유지한다. 새 의존성·스타일·모션은 없다.

`node --test scripts/qa/attachment-batch-ui.test.mjs`37건 실패/생략0, UI 구문 검사 및 diff 검사 통과. 모의 DOM에서 실제 이벤트 처리로 조회→명시 예약→반환 배치 이동/자동 수집0·오류 후 입력 보존을 검증했다. `:test --tests '*AnnouncementAttachmentBatchViewControllerSmokeTest' bootJar --no-daemon --max-workers=1`40초 성공, SSR/권한8건 실패/오류/생략0. 처음 `test`로 실행해 하위 추출기에도 화면 필터가 전파되어 no tests found로 실패했고 루트 `:test`로 정정했다. 최종 코드의 bootJar를 재생성했다.

원문 ID 입력/준비 사유/예약 기능은 연결했으나 실제 브라우저·보조기술·모바일 반응형·운영 예약/worker 통합은 아직 검증하지 않았다. UI 전체 완료 또는 ATT-051 완료로 계산하지 않는다. 운영 정책·수집·데이터 변경 없음. 이번 Node/Gradle 실행은 종료됐다.

## 2026-10-03 연결 원문 예약 클라이언트 계약

기존 배치 core에 전용 원문 UUID 목록·공고별 bytes 정규화, scope-preview 응답 검증, 근거 전용 확인을 포함한 멱등 예약 명령과 영수증 검증을 추가했다. 모든 요청 ID가 정확히 한 번 응답에 포함되는지, 정책·전체 예산·132회/공고 상한·준비 상태·연결 지문이 일치하는지 확인한다. 부적격 항목을 제외하여 일부 예약하지 않는다. 응답 유실 시 동일 키/본문 재요청을 유지한다. 서버 DTO/서비스의 기존 계약을 사용하며 API/DB 변경은 없다.

`node --test scripts/qa/attachment-batch-ui.test.mjs`34건 실패/생략0, core `node --check` 통과. 원문 누락/대체/중복·예산 불일치·동의 없음·미저장 선택·다른 목적 영수증 거부와 동일 요청 재시도를 확인했다. 이번 단계는 화면 연결에 사용할 계약 함수이며 실제 예약 폼·UI 이벤트 연결은 아직 미완료다. 브라우저/운영 예약 성공으로 계산하지 않는다. Node 종료, 운영 변경 없음.

## 2026-10-03 연결 공고 근거 전용 배치 제어 UI

Design Read: 기존 Thymeleaf/Bootstrap 배치 작업 화면과 한글 영향 확인·사유·확인 체크 절차를 재사용한다. 관리자 목표는 고정된 연결 원문의 첨부만 수집하고 기존 공고와 현재 판정을 보존하는 것이다. 외부 수집 실행은 R2, 결과 조회는 R0로 구분한다. 새 의존성·스타일·모션은 없다.

목록/상세에 작업 목적을 표시하고 LINKED_EVIDENCE_ONLY의 시작·중지·재개를 전용 API로 보낸다. 예약 취소는 기존 공통 API를 유지한다. 일반 판정 미리보기·선택·적용·원복 버튼과 명령 생성을 차단하며 공고별 새 근거와 재검수 경고 조회를 안내한다. 기존 권한, 버전·예산 확인, 불확실한 응답의 재조회/재시도 처리를 유지한다.

검증: `node --test scripts/qa/attachment-batch-ui.test.mjs`32건 실패/생략0, `node --check src/main/resources/static/js/saneb-attachment-batches.js` 및 `git diff --check` 통과. 기존 실행 세션53309의 `bootJar --no-daemon --max-workers=1` 종료를 확인했으며16초 성공이다. 전용 명령 경로·동일 출처 요청·예산 보존, 금지 목적 거부와 모의 DOM의 적용 버튼 제외를 검증했다. Node 실행은 종료됐고 새 상주 자원을 만들지 않았다.

한계: 실제 브라우저·키보드·반응형·운영 API 연동은 미검증이다. 전용 원문 선택/범위 조회/예약 UI는 아직 미완료이며 이번 변경은 예약된 배치의 제어를 제공한다. 운영 배포·재수집·정책 변경은 실행하지 않았다. ATT-001~062 전체 완료나 운영 수집 완료를 뜻하지 않는다.

## 2026-10-03 관리자 연결 공고 재검수 경고 조회 UI

Design Read: 기존 Thymeleaf/Bootstrap 검수 화면·plain-panel/attachment-evidence-item 및 공통 CSS를 재사용하는 R0 읽기 전용 보조 영역이다. 내부 운영자·승인자의 경고→저장 첨부 근거 확인을 지원한다. 새 라이브러리·모션·자동 수집·현재 판정 변경은 없다. 정상/부분/실패 및 현재 연결 변경 여부를 한글로 구분하고, 실패 집합 없음·빈 이력·조회 오류 재시도·페이지 이동을 제공한다. 기존 epoch/panel 응답 차단으로 최신 기준 조회 뒤 오래된 경고 응답을 버린다.

검증: `node --test scripts/qa/attachment-review-ui.test.mjs`29건 및 `node --check src/main/resources/static/js/saneb-announcement-attachment-review.js` 통과. `:test --tests '*AnnouncementAttachmentControllerSmokeTest' --tests '*AnnouncementAttachmentReadServiceTest' bootJar --no-daemon --max-workers=1`43초,26건 및 bootJar 통과. `:test --tests '*AnnouncementAttachmentViewControllerSmokeTest' --no-daemon --max-workers=1`38초,9건 통과. 모든 실행의 실패/오류/생략0. SSR/정적 계약 검증은 실제 브라우저 상호작용·반응형·스크린리더·운영 데이터 검증을 대신하지 않는다. 이번 증분은 미배포이며 브라우저 검증은 후속 Gate다. 전용 배치 예약/실행 UI도 별도 미완료다.

## 2026-10-03 봉인 후 연결 변경과 늦은 평가

실제 PostgreSQL에서 합성 첨부 집합 봉인 뒤 link ID 교체·announcement ID 교체·link 삭제를 각각 확정한 후 평가 서비스를 호출했다. 세 경우 모두 Optional.empty, job CONFLICT/FROZEN_INPUT_CHANGED, 재호출도 empty, 경고0을 검증했다. 확보한 set ID와 source/기존 운영 공고 전체 행은 유지된다. 실행 상태는 시험 fixture로 준비했으며 외부 HTTP/실제 파일 추출과 동시 transaction 교착 검증은 아니다.

`attachmentJobIntegrationTest --tests '*linkedLateEvaluation*' --no-daemon --max-workers=1` 52초 성공,3건 실패/오류/생략0. 기존 완료 후처리 코드가 작동하여 운영 코드 변경 없음. UI·운영 E2E는 미완료다.

## 2026-10-03 연결 근거 미리보기의 검수·DRAFT 오용 거부

실제 PostgreSQL의 성공/부분 완료 연결 근거 판정 ID와 실제 집합 hash를 검수 확인·DRAFT 전환 서비스에 전달했다. 연결 유지 시 기존 연결 보호 오류로 거부되고, 시험 transaction에서만 연결을 해제해도 현재 판정 불일치로 거부된다. 연결 해제는 rollback하며 확인0·연결1·전체 공고 수 증가0·source/운영 공고 행 불변을 확인했다. 기존 보호 코드가 작동하여 운영 코드 변경은 없다.

`attachmentJobIntegrationTest --tests '*linkedCompletedEvidence*' --no-daemon --max-workers=1` 재실행52초 성공,2건 실패/오류/생략0. 첫 실행은 이전 매개변수 시험의 공고가 보존된 상태에서 전체 공고1건을 기대한 assertion 오류로 실패했고, 실행 전후 전체 건수 비교로 수정했다. 브라우저·운영 검수 E2E 및 실제 파일 다운로드 성공 증거는 아니다.

## 2026-10-03 연결 전용 배치의 일반 적용 경로 차단

일반 적용 미리보기·적용·원복 서비스의 배치 조회에 DB 목적 검사를 추가했다. LINKED_EVIDENCE_ONLY는 DB 상태 제약 오류에 도달하기 전에409 업무 오류로 거부하며 첨부 근거/경고 조회를 안내한다. 일반 STANDARD 의미와 V87 DB 제약은 유지한다.

`:test --tests '*AnnouncementAttachmentBatchPreviewServiceTest' --tests '*AnnouncementAttachmentBatchApplicationServiceTest' --tests '*AnnouncementAttachmentBatchRollbackServiceTest' attachmentJobIntegrationTest --tests '*linkedCompletedEvidence*' bootJar --no-daemon --max-workers=1` 1분2초 성공. 서비스16/23/20 및 실제 PostgreSQL2, 총61건 실패/오류/생략0, bootJar 성공. 실제 DB는 성공/부분 완료 linked 배치를 일반 미리보기 생성·적용 START·원복 미리보기에 넣어 명시적 목적 오류와 배치/source/운영 공고 불변을 확인한다. 검수 확인·DRAFT API 직접 오용과 운영 E2E는 이 시험 범위가 아니다.

## 2026-10-03 연결 근거 성공·부분 결과와 경고 통합

실제 PostgreSQL에서 전용 범위 조회→예약→시작→claim→합성 첨부 집합 봉인→평가→경고 조회→배치 최종 집계를 검증했다. 정상1파일은 SUCCEEDED/EVIDENCE_READY/COLLECTED, 정상1+다운로드 실패1은 PARTIAL_FAILED/EVIDENCE_PARTIAL/COLLECTION_PARTIAL_FAILED다. 부분 실패에서도 입력2개와 NETWORK_TIMEOUT 근거가 남는다. 평가 재호출은 같은 ID이며 경고1개만 존재한다. source 및 연결 announcements 전체 행이 전후 동일하고 평가 current=false다.

`attachmentJobIntegrationTest --tests '*linkedCompletedEvidence*' --no-daemon --max-workers=1` 41초 성공 후, 배치 집계를 추가하여 `attachmentJobIntegrationTest --tests '*linked*' --no-daemon --max-workers=1` 1분15초 성공. 최종 결과14건·실패/오류/생략0. 이 시험은 합성 추출 근거이며 외부 HTTP·실제 PDF/HWP/HWPX 추출·신청/진행 하위 행·최종 저장 경합·관리자 UI·운영 E2E는 증명하지 않는다. 운영 변경 없음.

## 2026-10-03 연결 근거 수집 시작·중지·재개

V90은 일반 배치의 연결 제외를 유지하면서 LINKED_EVIDENCE_ONLY에만 V88 불변 연결 비교를 적용한다. 전용 PUT collection/collection-pause/collection-resume은 ADMIN·CSRF·현재 버전 확인이 필요하다. 일반/전용 목적 혼용을 거부하고, 시작·재개 시 source/link/정책 잠금 뒤 고정 지문·예산·연결을 재검증한다. 운영에는 반영하지 않았다.

`:test --tests '*AnnouncementAttachmentBatchServiceTest' --tests '*AnnouncementAttachmentBatchControllerSmokeTest' attachmentJobIntegrationTest --tests '*linkedScopePreviewRetains*' --tests '*linkedExecutionFence*' --tests '*linkedEvidencePurposeCannot*' bootJar --no-daemon --max-workers=1`은 1분34초 성공. 서비스31·MockMvc30·실제 PostgreSQL3, 총64건 실패/오류/생략0이며 bootJar 성공이다. 예약→전용 시작→claim→중지 시 외부 실행 차단→재개→연결 ID 교체 후 재개 거부, 원문 전체 컬럼 불변을 검증했다. 실제 외부 HTTP·성공/부분 첨부 평가와 경고의 전체 연결·관리자 UI·운영 E2E는 미검증이다. 이전 기록의 실행 API 미구현 상태는 이 증분으로 갱신한다.

## 2026-10-03 연결 근거 멱등 예약

별도 POST 기본 경로에 ADMIN/CSRF/Idempotency-Key/명시 확인을 적용했다. 서비스는 정렬한 source 잠금→link 잠금→정책 잠금 뒤 조회 지문과 전체 준비 상태를 재확인하고, LINKED_EVIDENCE_ONLY 배치·SCOPE_READY jobs·V88 snapshot을 한 transaction으로 기록한다. source/current/검수/운영 공고를 변경하지 않는다. 예약은 worker claim 대상이 아니다.

검증 명령 `:test --tests '*AnnouncementAttachmentBatchControllerSmokeTest' --tests '*AnnouncementAttachmentBatchServiceTest' --tests '*AttachmentLinkedBatchRequestsTest' attachmentJobIntegrationTest --tests '*linkedScopePreview*' bootJar --no-daemon --max-workers=1` 1분1초 성공. MockMvc29/일반 서비스31/입력3/실제 DB1, 총64건 실패·오류·생략0 및 bootJar 성공이다. 실제 DB는 stale 연결 지문 거부·같은 키 동일 배치·다른 사유 충돌·새 키 중복 활성 작업 거부·원문 전체 컬럼 불변·snapshot1개·worker 미실행을 검증했다. 동시 예약 경합·전용 실행 API·성공/부분 첨부 worker·관리자 UI·운영은 후속 검증이다.

## 2026-10-03 연결 근거 명시 범위 조회

별도 `POST /api/v2/admin/announcement-attachment-linked-evidence-batches/scope-preview`를 추가했다. 서버에서 입력/활성 정책/정책 상한을 재검증하고 요청 전체 ID를 유지하며 부적격·미연결 원문을 대체하지 않는다. 현재 연결 identity, 원문/첨부 버전·기존 확인·정책/profile/출처가 지문에 포함된다. 일반 Eligible은 변경하지 않았고 DB 읽기 외 동작은 없다.

`:test --tests '*AnnouncementAttachmentBatchControllerSmokeTest' --tests '*AnnouncementAttachmentBatchServiceTest' attachmentJobIntegrationTest --tests '*linkedScopePreview*' bootJar --no-daemon --max-workers=1` 54초 성공. 일반/신규 MockMvc·서비스59건, 실제 DB1건, 실패/오류/생략0 및 JAR 생성 성공이다. DB는 연결 변경 지문 차이·일반 배치 연결 제외·누락ID 포함 전체분모·job/batch 생성0을 확인했다. 첫 권한 시험은 빈 본문400을403으로 기대한 시험 오류로 유효 본문으로 수정했고, 다음 CSRF 시험은 새 namespace 보호 누락을 발견하여 SecurityConfig의 CSRF 예외 제외 목록에 새 경로를 추가해 해결했다. 예약/실행 API·전체 성공 worker·관리자 UI·운영 검증은 아직 남는다.

## 2026-10-03 worker terminal 경고 저장 연결 — 전체 회귀 통과

전체 회귀 종료: 같은 세션을 재시작하지 않고 완료를 확인했다. `attachmentJobIntegrationTest --no-daemon --max-workers=1`은10분39초 성공, JUnit222건/612.252초·실패/오류/생략0이다. 공통 작업 서비스의 기존 예약·lease·예산·검수·배치 회귀 결과이며 아직 열지 않은 연결 원문 외부 수집의 성공 증거는 아니다.

후속 `:test --tests '*AttachmentLinkedBatchRequestsTest' bootJar --no-daemon --max-workers=1`은24초 성공, 입력 계약3건·실패/오류/생략0 및 JAR 생성 성공이다. 새 DTO는 정책·명시 원문1~1000개·중복/빈 ID 거부·1~80MiB bytes·지문·명시 확인·사유를 검증하고 임의 입력을 거부하며 식별자/사유 toString 노출을 제한한다. 아직 Controller/예약 서비스에 연결하지 않은 입력 계약이며 API 구현 완료가 아니다. 이 DTO는 전체222건 실행 시작 뒤 추가했으므로 별도3건 결과로만 검증한다.

성공/부분 완료 평가 저장, 직접 실패 처리, 최대100건 lease 만료 회수에서 공통 후처리를 같은 transaction으로 호출하도록 구현했다. 연결 근거 목적의 terminal job만 경고를 기록하고, source/link 잠금 뒤 연결 불일치이면 CONFLICT/FROZEN_INPUT_CHANGED로 분리한다. 일반 목적·재시도 대기는 경고를 생성하지 않는다. 기존 입력 fence는 아직 연결 원문의 실제 실행을 허용하지 않으며 전용 예약/실행 API는 미구현이다.

`attachmentJobIntegrationTest --tests '*linkedNotice*' --tests '*linkedSnapshot*' --no-daemon --max-workers=1` 52초 성공,8건·실패/오류/생략0. 새2건은 직접 실패 자동 기록/중복 콜백 무쓰기/연결 변경 충돌, lease 소진 회수의 자동 실패 경고를 검증한다. 성공·부분 완료의 실제 worker 경고와 전체 회귀 통과를 의미하지 않는다. 처음 지정한 단위 시험 클래스는 존재하지 않아 No tests found로 종료됐으며 통과 수에 포함하지 않는다. 공통 JobService 변경 영향 확인을 위해 전체 `attachmentJobIntegrationTest --no-daemon --max-workers=1`을 실행 중이다. 결과 확정 전 커밋·운영 반영하지 않는다.

## 2026-10-03 연결 재검수 경고 조회 API

`GET /api/v2/admin/announcement-sources/{sourceId}/attachment-linked-review-notices`를 기존 Controller→ReadService→ServiceImpl→EvidenceDao→Mapper 경로에 추가했다. 내부 조회3역할·원문 적격성·페이지1~1000000/크기1~100·ApiResponse/PageResponse·no-store를 유지한다. 경고 이력과 조회 시 연결 일치 여부를 구분하고 원문/URL/lease는 반환하지 않는다. v1 변경 없음, 읽기만 수행한다.

`:test --tests '*AnnouncementAttachmentReadServiceTest' --tests '*AnnouncementAttachmentControllerSmokeTest' attachmentJobIntegrationTest --tests '*linkedNotice*' --no-daemon --max-workers=1`은59초 성공: 서비스12/MockMvc14/실제 PostgreSQL·MyBatis1건, 총27건 실패·오류·생략0이다. DB 시험은 저장된 실패 경고의 DTO/페이지/다른 원문 미노출을 확인했다. `bootJar --no-daemon --max-workers=1`은13초 성공이다. 전용 예약 API·worker 자동 기록·관리자 UI·운영 브라우저 검증은 미완료이며 조회 API 통과를 그 증거로 사용하지 않는다.

## 2026-10-03 V89 연결 공고 재검수 경고 원장

V89에 작업당 1개 불변 경고 원장을 추가했다. source/job·set/evaluation 복합 FK와 실제 terminal job의 source/batch/set/preview/상태 비교, 고정 연결 재검증, source/link 잠금으로 다른 근거의 삽입을 제한한다. 사유는 EVIDENCE_READY/EVIDENCE_PARTIAL/COLLECTION_FAILED이며 원문/개인정보 metadata는 저장하지 않는다. 기존 source pointer/운영 공고/확인은 수정하지 않는다. worker 자동 기록·API/UI·terminal 경고 누락 강제는 후속 구현이며 ATT-051 완료가 아니다.

실행: `:test --tests '*MigrationContractTest' attachmentJobIntegrationTest --tests '*linkedNotice*' --tests '*linkedSnapshot*' attachmentMigrationTest --tests '*freshSchemaAndV71UpgradePreservePriorChecksums' --no-daemon --max-workers=1` 1분21초 성공. 정적99건/작업DB6건/마이그레이션1건 실패·오류·생략0. 신규 시험은 진행 중 job 경고 거부, 첨부 set 없는 FAILED 경고 저장, 중복/수정/단독 삭제 거부를 검증했다. V71→89/빈 DB 적용·기존 업무행/checksum 보존도 통과했다. 최초 V89 구문 오류는 미커밋 V89의 CASE 괄호 수정으로 해결했으며 기존 migration은 변경하지 않았다. 성공/부분실패 실제 worker 결과, 경고 FK 악성 교차 소속·완료 경합·UI·운영은 아직 미검증이다.

## 2026-10-03 V88 연결 snapshot 기반 검증

경합/삭제 후속 검증: 같은 표적 명령은46초 성공,7건·실패/오류/생략0이다. 새 경합 시험은 snapshot 예약 transaction 내부에서 별도 연결의 link ID UPDATE가 lock_timeout=300ms·SQLSTATE55P03으로 차단되고, commit 이후 같은 변경이 성공하며 비교 함수가 false로 바뀌는 것을 검증했다. 삭제 시험은 기존 live link FK가 source 삭제를 거부함을 확인한 뒤, 합성 link를 별도로 해제하고 source 삭제 시 job/snapshot 정리·고정 분모1 유지·삭제 집계1·운영 공고 전체 컬럼 불변을 검증했다. 최초 삭제 시험의 FK 실패를 보호 완화로 해결하지 않았다. 예약 직렬화 증거이며 worker HTTP 전/완료 저장 경합 검증은 아니다. 이전 V87 SHA579e48f의 Linux37037830571은 success, V88 SHA4702f0f의37039105876은 cancelled로 확인되어 후자를 통과로 계산하지 않는다. SHA68ec436의37039782126은 조회 시 in_progress였다.

후속 실제 PostgreSQL 표적 시험 `attachmentJobIntegrationTest --tests '*linkedSnapshot*' --tests '*linkedEvidencePurpose*' --no-daemon --max-workers=1`은41초 성공,5건·실패/오류/생략0이다. 신규3건은 정상 snapshot 예약 commit, 임의 수정/삭제 거부, link ID 교체 후 비교 false와 고정 이력 보존, snapshot 누락 시 commit 거부·배치/job 원복, 위조 link ID 삽입 거부·예약 원복을 검증한다. 기존 V87 목적 보호2건도 함께 통과했다. 처음 시험 준비의 job 단독 취소는 기존 배치 상태 일관성 제약에서 거부되어 기존 취소 서비스로 수정했다. 제약/trigger를 비활성화하지 않았다. 동시성·source 삭제 cascade·경고 원장·worker 및 운영 검증은 여전히 남는다.

V88은 작업/source 복합 FK, 예약 시 연결 ID·공고 ID snapshot 저장, snapshot 수정/단독 삭제 방지, 예약 transaction 종료 시 snapshot 존재 확인, 현재 연결과의 비교 함수를 추가한다. 기존 외부 요청 fence는 변경하지 않으며 경고 원장·전용 API·worker 연결은 남아 있다.

첫 실제 DB 시험은 `uq_announcement_source_links_source` 위반으로 실패했다. V26만 근거로 같은 원문에 복수 연결을 허용한다고 판단했던 설계를 V68의 원문당 단일 연결 계약에 맞춰 정정했다. 제약을 완화하지 않고 시험을 연결 삭제·대상 교체·동일 대상의 link ID 교체로 수정했다.

재실행 명령 `:test --tests '*MigrationContractTest' attachmentMigrationTest --tests '*freshSchemaAndV71UpgradePreservePriorChecksums' --no-daemon --max-workers=1`은50초 성공이다. 정적 계약98건 및 실제 PostgreSQL migration1건이 실패/오류/생략 없이 통과했다. V71→88 순차 적용·빈 DB validate·기존 행/checksum 보존과 snapshot 비교를 확인했다. snapshot 삽입/변경/삭제 거부 및 동시성 전체 계약을 입증한 것은 아니며 해당 시험은 후속으로 남긴다. 운영 DB·정책·worker 변경 없음.

## 2026-10-03 ATT-051 기능 누락 확인

후속 실제 PostgreSQL 표적 시험 `attachmentJobIntegrationTest --tests '*linkedEvidencePurpose*' --no-daemon --max-workers=1`은34초 성공,2건·실패/오류/생략0이다. STANDARD 배치 목적 변경은 목적 불변 trigger, 고정 범위 없는 LINKED_EVIDENCE_ONLY 생성은 `ck_att_linked_batch_scope`에서 각각 SQLSTATE23514로 거부됐고 쓰기 실패 후 상태 유지·기존 STANDARD 생성 성공을 확인했다. 신규 경로의 정상 수집이나 job 적용/원복 효과 차단 전체 시험은 아니다. 같은 작업 suite 전체 회귀는 별도 실행 중이며 이 표적 결과로 전체 통과를 선언하지 않는다.

전체 회귀 종료: `attachmentJobIntegrationTest --no-daemon --max-workers=1`은10분27초 성공, 전용 XML214건/600.049초·실패/오류/생략0이다. 실행 중 thread 관측은 loopback PostgreSQL 연결 응답 대기를 보였으나 시험 메서드가 계속 바뀌었고 실행을 재시작하지 않았다. 이 관측만으로 지연의 근본 원인을 확정하지 않는다. 종료 후 시험 JVM/Gradle PID 종료를 확인했다. V87 기존 DB/작업 계약 회귀 증거이며 미구현 ATT-051 전체 연결·경고/API/worker와 운영 검증은 여전히 남는다.

구현 계약은 [연결 공고 첨부 근거 갱신 설계](announcement-attachment-linked-evidence-design-2026-10-03.md)에 정리했다. V87에는 STANDARD 기본값과 LINKED_EVIDENCE_ONLY 목적 불변·적용/원복 차단만 추가했다. 전체 링크 집합 고정·경고 원장·API·worker 연결은 미구현이다. 기존 외부 요청 fence는 완화하지 않아 연결 원문 수집은 아직 불가능하다.

V87 정적 migration 계약97건 통과·실패/생략0, 21초 성공이다. `AnnouncementAttachmentMigrationTest`4건은 로컬 환경 조건으로 전부 생략됐다. V86→87 순차 upgrade/기존 데이터 보존 및 새 제약·trigger 조회 assertion을 추가했으나 실제 PostgreSQL 실행 성공은 아니다. 기존 V1~V86 파일 변경 없음, 운영 DB/정책/worker 변경 없음. 다음은 고정 연결·경고 원장과 SQL 동작 시험이다.

후속 전용 실행으로 로컬 DB 차단을 재검증했다. Docker 엔진은 없지만 프로젝트 embedded PostgreSQL은 현재 실행 가능하다. `attachmentMigrationTest --tests '*AnnouncementAttachmentMigrationTest.freshSchemaAndV71UpgradePreservePriorChecksums' --no-daemon --max-workers=1` 46초 성공, 전용 XML1건/33.192초·실패/오류/생략0이다. V71→87 순차 적용, 빈 DB validate, 기존 업무 snapshot/checksum 보존, V87 기본값·제약3개·trigger2개 및 기존 연결 거부 fence 보존을 확인했다. 일반 test의4건 생략과 별개이며 V87 잘못된 쓰기 직접 거부·연결 근거 수집 전체 기능 통과로 확대하지 않는다. 임시 DB는 시험의 try-with-resources로 종료됐다.

이 항목은 운영 검증만 남은 상태가 아니다. 원 설계 `announcement-attachment-collection-design-2026-09-08.md` 503행과 QA 계획 ATT-051은 연결된 원문을 별도 명시 범위에 포함하여 첨부 근거·재검수 경고만 갱신하는 경로를 요구한다. 현재 `AttachmentBatchRequests.Scope`에는 이를 지정할 입력이 없고, `AnnouncementAttachmentBatchMapper.xml`의 `Eligible`은 연결 원문을 무조건 제외한다. 범위 집계의 `LINKED_PROTECTED`는 제외 건수이지 해당 경로의 구현 증거가 아니다. 단건 Collection/Intake와 batch preview/application도 연결 원문을 차단한다.

- [x] 기존 기본 제외·운영 공고 보호 코드 확인.
- [ ] 별도 명시 범위의 연결 원문 수집·근거 저장·재검수 경고 경로 구현.
- [ ] 연결된 공고의 상태·조건·신청 데이터를 전후 비교하는 실제 PostgreSQL 시험.
- [ ] 승인된 운영 표본의 경고 표시·기존 업무 데이터 불변 검증.

현재 기본 보호 회귀: `:test --tests '*AnnouncementAttachmentBatchServiceTest' --tests '*AnnouncementAttachmentBatchApplicationServiceTest' --tests '*AnnouncementAttachmentReviewServiceTest' --no-daemon --max-workers=1` 42초 성공. JUnit 각각31/23/41건, 총95통과·실패/오류/생략0이다. 서비스 대역 시험이므로 누락된 명시 포함 경로, 실제 PostgreSQL 및 운영 E2E의 성공 증거로 사용하지 않는다.

다음 구현은 기존 보호 조건을 전역으로 제거하지 않는다. 먼저 additive DB 계약으로 별도 작업 목적·고정 source/link identity·근거 갱신 경고를 정의하고, 명시적 범위와 승인 지문에 결합한다. 기존 v1/일반 배치 기본값·DRAFT/적용/원복 차단은 유지한다. 별도 경로의 worker는 첨부 근거만 저장하고 운영 공고·조건·신청·연결을 변경하지 않아야 하며, 예약 이후 link 변경은 충돌로 남겨야 한다. 기존 계약의 의미 변경이 필요한 경우 신규 버전 경계를 먼저 명시한다. 이 문단은 설계 착수용 누락 기록이며 DB/API 구현 완료나 운영 실행 승인이 아니다.

## 2026-10-03 호출 중단·격리 자원 정리 검증

`ce76b2db9bb9f276e77dd0ae5a0cac80a1047190`의 [Linux37031100334](https://github.com/FrostyCityMan/saneB/actions/runs/37031100334)가 success다. artifact11238805743의 전용 runtime XML7건은 실패/오류/생략0이며 신규 `interruptedCallerKillsObservedChildAndRecoversWithoutRetainingOriginals`가0.390초 통과했다. 실제 격리 자식 JVM의 고유 표식을 관측한 후 호출 스레드를 중단하여 CANCELLED 반환·중단 신호 보존·자식 종료·소유 원본 및 임시 폴더 정리·다음 추출 복구를 검증한다. 기존 실제30초 timeout 시험도30.238초 통과했다.

ATT-027/059의 합성 입력 Linux launcher 증거를 보강한다. 웹 UI의 취소 기능이나 운영 서버의 강제 종료·OS OOM killer·모든 악성 문서 검증을 추가한 것은 아니다. 일반 test task에서 같은 클래스7건은 조건부 생략됐고, 별도 필수 runtime task에서 실제 실행된 결과를 사용한다. 보고서 판정기는 최소7건을 요구하며 이전6건만 있으면 실패한다. 운영 main 코드·DB·정책은 변경하지 않았으며 전체 ATT 완료 상태는 올리지 않는다.

## 2026-10-03 양평 개정2 단건 운영 근거

승인된 SRC-017679 재수집 작업 `e7796154-4de8-47c8-9875-97598358ac58`이 처리 완료됐다. 새 집합 `846733ce-2bc4-369a-aac1-1b3a3eb1204f`의 HWPX1개·11,398자·505문단·12구간을 운영 화면에서 조회하고 기존 집합 보존과 기본 판정 불변을 확인했다. 정책 `7df4c0a2-0d6f-402b-bb63-6ce39f2fef9c`에 연결된 신규 미리보기다. 아래 최신 정책 신규 작업 승인 대기/미실행은 이 실행 이전 기록이다.

- ATT-011/017/036/062: UNKNOWN 역할 표시, HWPX 저장 문단1→2·연결 구간 조회, 미리보기와 기본 판정 분리의 최신 운영 표본을 추가한다. 다른 원문 ID 접근 거부, 모든 형식, 후속 관리자 확정까지 증명한 것은 아니다.
- ATT-059: 운영 worker 비활성이라는 과거 행은 현재 상태가 아니다. 실제 작업 성공을 추가하되 격리 내부·프로세스 트리·자원 고갈 경계의 새 직접 관측은 아니다.
- 수집 성공만으로 ATT 전체 행을 완료로 승격하지 않는다. 본문 부족→ACCEPTED 실표본, 역할별 실제 세션, ENFORCE·검수→DRAFT·배치 운영 검증은 남는다. 자세한 한도·식별자·증거 경계는 [운영 테스트 안내](../deployment/attachment-operator-acceptance-2026-10-02.md)의 00:51 기록을 따른다.

## 2026-10-03 최신 Linux·운영 증거 대조

아래 날짜별 증분과 행의 과거 실행 대기는 당시 이력이다. 현재 코드 기준 `371b1acda13deed4740c5f3377cc0b298d4cee09`의 [Linux37005549445](https://github.com/FrostyCityMan/saneB/actions/runs/37005549445) success와 보관 XML을 다시 확인했다. job212·worker12·migration4·전체 분할14·Flyway3은 실패/생략0이다. 이는 새 테스트 실행이 아니라 동일 SHA의 실행 결과 재대조다. 최종 운영 경합·실파일·역할별 브라우저 요구는 남으므로 완료 수를 올리지 않는다.

| 요구 | 최신 확인 증거 | 남은 검증 |
|---|---|---|
| ATT-044 | job `att040OffImmediatelyBlocksExternalBytesAndResourcesButNotSealedRecovery`, `offDoesNotPreventRoleReevaluationOfFrozenSealedEvidenceOrClearGuard` 통과. 운영 COLLECT_ONLY 개정2 게시·개정1 퇴역 영수증 확인 | 운영 OFF 전환·실제 작업 중 경계와 봉인 근거 복구 |
| ATT-045 | 운영 관리자 로그인·동의·새 개정 저장·QA 예약·게시 성공 | OPERATOR/APPROVER/USER 실제 세션 및 CSRF 음성 경로. 관리자 성공만으로 전체 권한 검증 완료 아님 |
| ATT-053 | migration `freshSchemaAndV71UpgradePreservePriorChecksums` 및 Flyway3 통과. 현재 테스트는 V86 단계까지 포함 | 09-22 완료 근거의 범위는 유지. 최신 운영 V86 직접 대조는 운영 기준선의 10-02 증거를 참조하며 이번 턴에는 서버 직접 조회하지 않음 |
| ATT-055 | job `concurrentSameDraftRequestCreatesOneAnnouncement`, `concurrentConfirmationsAllowOneWriterAndReturnConflictForStaleVersion` 통과 | 실제 운영 두 세션 검수·DRAFT 경합 |
| ATT-061 | job `att061PolicyProfileAndRuleMustMatchWithoutUpdatingSourceOnFailure`, `att061FrozenRunKeepsPublishedPolicyAfterRetirementAndOffPreventsClaim` 통과. 개정2의 새 QA·동일 규칙 정책 교체 확인 | 운영 release 불일치 음성 사례와 기존 고정 작업의 실제 경계 |
| ATT-062 | job `currentProjectionSeparatesCollectOnlyPreviewFromListFiltersAndCount`, `collectOnlyHistoryIsPreviewAndHiddenSourceCannotExposeStoredEvidence`; worker `collectOnlyStoresPreviewWithoutBindingReviewOrChangingBaseOrCreatingAnnouncement` 통과 | 기존 양평 표본은 미리보기와 기본 판정 분리 확인. 본문 부족을 첨부가 보완해 ACCEPTED가 되는 요구의 실제 표본 및 최신 정책 신규 작업은 미검증 |

운영 개정2 `7df4c0a2-0d6f-402b-bb63-6ce39f2fef9c`, QA `733f9065-d4af-4065-9271-97b4451d1e8e`, 게시 영수증 `88e89d51-9a58-424c-be1a-03ba63c97846`의 상세는 [운영 기준선](../deployment/attachment-runtime-baseline-2026-09-15.md)을 따른다. COLLECT_ONLY의 4단계 통과와 결합224/전체225는 STRICT 전체 실파일 통과가 아니다. 새 정책 단건 재수집은 영향 범위 승인 대기이며 실행하지 않았다. ENFORCE·기존 데이터 적용·최종 검수·DRAFT 및 전체 ATT 완료를 선언하지 않는다.

## 2026-09-22 ATT-053 완료 근거

`272dafd527d7f37236b8728bd764a48d1a66be81`의 [Linux35693601984](https://github.com/FrostyCityMan/saneB/actions/runs/35693601984)가 success다. 보완한 `freshSchemaAndV71UpgradePreservePriorChecksums`는4.036초 통과/생략0이다. V71 합성 사용자·본문 확보/미확보 원문2건·분류2건·숨김 DRAFT·연결1건과 규칙 seed를 포함한 기존10테이블의 모든 기존 컬럼/행 지문이 V83 upgrade 후 동일하다. V71/upgrade/fresh DB의 attachment_analysis_enabled=true INSERT/UPDATE는 정확한 CHECK와 SQLSTATE23514로 거부됐다. 빈 DB validate 및 기존 순차 schema/checksum 검증도 유지했다.

운영의 동일 migration80개 checksum 대조 근거와 결합해 ATT-053만 [x]로 전환한다. 현재 추적표는 완료1/부분61이며 전체 Gate 완료는 아니다. 합성 대표 데이터의 보존을 운영 전체 업무 데이터/모든 schema drift 감사로 확대하지 않는다. 정책/worker/기존 데이터 변경·옥천 QA·운영 브라우저 E2E는 실행하지 않았다. 아래 checksum 증분의 미완료 표기는 보완 이전 기록이다.

## 2026-09-22 운영 migration checksum 전수 대조

승인 배포 `9a1bb45`의 설치 JAR와 운영 Flyway 이력을 읽기 전용으로 비교했다. 최신 버전은 V83이며 실제 migration은 V2/V3/V5가 없는 **80개**다. 이력80/파일80, 누락·추가·checksum 불일치·잘못된 항목 모두0, READ ONLY/ROLLBACK/쓰기0이다. SSM `638ec0bb-b863-4bd0-a2b7-a87eb589f3c6` Success와 설치 JAR hash `12985f8cd4d710f24da85d2f82c9556d719264aef5f43ac1a57239687bc9c2bc`를 결합했다. 비교기는 실제 Flyway10.20.1 계산기로 로컬80파일·인코딩5경계 사례를 대조하고 정상/오류 판정8사례를 검증했다.

동일 SHA Linux35681482535의 `freshSchemaAndV71UpgradePreservePriorChecksums` 통과와 순차 V72~83/빈 DB 적용 근거도 재확인했다. 다만 해당 시험은 V71 업무 데이터의 대표 snapshot 보존을 직접 대조하지 않는다. 기존 false 값 assertion을 CHECK 제약의 실제 거부 시험으로 확대하지 않는다. ATT-053은 checksum 차단만 해소하고, 기존 업무 데이터 보존 및 CHECK false 유지의 직접 시험 근거를 보완할 때까지 부분 완료다. 정책/worker 활성화·옥천 QA·운영 업무 E2E를 실행하지 않았다.

## 2026-09-22 ATT-059 실제 격리 경계 시험 보강

`AttachmentRuntimeGateIntegrationTest.productionLauncherBlocksParentEnvironmentHostFilesAndHostNetwork`를 추가했다. 실제 운영 비밀·외부 사이트 대신 공개 합성 부모 환경값, 테스트 소유의 호스트 파일, 테스트 부모의 loopback listener를 만들고 production `IsolatedAttachmentExtractor.selectExtraction`으로 합성 probe JAR를 실행한다. 환경값 비전달·호스트 파일 비노출·호스트 loopback 연결 차단, 입력/추출기 라이브러리 읽기 전용과 전용 임시 파일 쓰기·JRE 읽기를 직접 assertion한다. 기존12개 실제 parser fixture 시험은 유지한다.

이 probe는 parser 동작을 대신 검증하지 않으며 실행 경계를 관찰하는 테스트 전용 클래스다. 단위시험에서 Java21 컴파일·단일 class JAR 구성을 확인했다. 로컬 표적22건=21통과/Windows의 Linux 전용1건 생략/실패0·23초 성공이다. 이 로컬 작성/컴파일 성공과 아래 실제 Linux 결과를 구분하며 ATT-059 전체 완료로 계산하지 않는다. OOM/프로세스 트리·OS crash와 최종 설치 환경의 격리는 별도 잔여다. 운영 main 코드·정책·DB·catalog 기대값은 변경하지 않는다.

후속 실제 결과: `f496d2e` [Linux35631537040](https://github.com/FrostyCityMan/saneB/actions/runs/35631537040) success, runtime XML2건 실패/생략0이다. `productionLauncherBlocksParentEnvironmentHostFilesAndHostNetwork`1.391초 통과, 기존12fixture 검증5.024초 통과를 개별 testcase에서 확인했다. 합성 canary의 실제 launcher 환경/파일/네트워크·read-only 경계 증거이며 최종 운영 설치, 임의 악성 문서 전체, OOM/프로세스 트리 복구까지 완료한 것은 아니다.

## 2026-09-22 서울 태백 재관측·고정 비교 근거

[서울 임시 QA](announcement-seoul-temporary-bbs-qa-2026-09-21.md)에서 최신 BBS 코드의 BODY431자와 전체HWPX2파일(2041/1994자,62/117블록)을 관측하고, 새 사전 기대값을 production CaseExecutor로 별도 재다운로드·재추출해 비교했다. 두 실행 각 JUnit1/1·실패/생략0, 전체 파일/역할 지문 일치·원본/임시 자원 정리·운영 DB쓰기0이다. UNKNOWN/MIXED_DOCUMENT_ROLES와 FORM·REVIEW_REQUIRED를 유지해 ATT-011의 해당 표본 분류 근거, ATT-017의 HWPX 해당 표본 추출/locator 근거를 보강한다. 실제 UI 표시/후속 행동·나머지 모든 profile/형식 요구는 남는다.

합계8요청/2,658,726bytes가 승인44요청/80MiB 이내인 점은 예산 사용 근거이며 ATT-024의 초과 경계 시험이나 ATT-060의 다중 worker 동시성/정책 게시를 대체하지 않는다. 관측 성공을 제목 제외·실패/재시도·운영 DB/API/브라우저 검증으로 확장하지 않으며 기존62행 상태/분모를 유지한다.

## 2026-09-16 검증 기준 갱신

아래 날짜별 증분은 당시 기록이다. 과거의 “최신 PG 미실행/환경 차단”은 현재의 Linux 실행 여부를 뜻하지 않는다. `85c7f65` Linux35050057694의 보관 XML에서 migration3·전체 분할14·job192·worker12·Flyway3·runtime1·정책 부모2건의 실패·생략0을 확인했다. [DB/API 계약 근거](announcement-attachment-contract-evidence-2026-09-16.md)에 실제 testcase와 증명 범위를 연결했다. ATT037/053/058의 DB 실행 차단 표기는 갱신하되 전체 운영 요구 통과로 승격하지 않는다.

실제 공식 파일·운영 설정/데이터·인증 브라우저는 별도 근거가 필요하다. 표의 나머지 “최신 PG” 요구는 위 실행된 해당 assertion으로 충족한 부분과 운영/실파일 잔여를 구분해 읽는다. 후속 `9fe892c` Linux35051444985도 전체 계약 success이며 제천 별도3/3을 확인했다. 제목 음성1건 요청0, 양성2건 전체HWPX3개 완전 추출·worker/DB/API 대조·UNKNOWN 검수 유지·원본/lease 정리/운영쓰기0이다. ATT002/011/017/036의 해당 표본 근거이며 전체62행·Gate0~8과 정상 표본·전체 대상 분모를 줄이지 않는다.

- 09-14 17시 3단계 대기열: P1 처리 흐름 projection/상세 안내와 P2 v2 선택 필터·공통 SQL count/page·읽기 전용 화면을 구현했다. FLOW004~010 및 ATT043~045의 부분 구현/로컬 회귀 근거이며 기존62행의 실제 통과 상태를 변경하지 않는다. root2059통과/239생략·Node152·독립 QA14·bootJar 성공, 실제 PG 대기열 시험은 initdb/Code Integrity 초기화 실패1로 assertion 미실행이다. Linux 독립213건은INVENTORY_ONLY/통과0. P3/P4·실제 Provider/Linux/운영 E2E와 전체 Gate 최종 통과0을 유지한다. 상세는09-14 3단계 설계/최신 진행 기록/API24.33/DB11.30이다.

- 09-12 11:15 Provider QA 관리 연결: V80 승인 계획/분할 원자 봉인, 정확한 범위·예산 확인/멱등 예약, 역할별 계획·원장·항목 페이지/취소, 기본 OFF 스케줄러의 현재 입력/전체 항목 대조·단일 case 실행을 연결했다. ATT-029~034/037/044/045/052/058/060/061의 부분 구현 근거다. Service34/HTTP23/스케줄러2와 전체 root1919통과234생략·추출기25(기존 결과 재사용)·QA14·Node130통과. 실제 PG3사례/V80 migration은 미실행이며 독립208건도 발견만 했고 통과0이다. 공식 참조9/기대값0, 전체 catalog/UI/verifier·Linux/운영 E2E 잔여와62행/Gate 최종 통과0을 유지한다. 상세는 `announcement-attachment-provider-qa-management-2026-09-12.md`와 최신 진행 기록을 따른다.

- 09-12 10:40 시스템 catalog/계획: 참조 표본과 실행 기대값·전체 target/case 분모·정상3공고/형식별 기대 coverage·중복 상세/제목 판정·유효기간·분할 상한을 구현해 정책 snapshot6에 연결했다. ATT-001/002/012~014/029/034/044/052/060/061의 부분 계획 근거다. 공식 참조9/실행 기대값0이며 실제 Provider 성공으로 바꾸지 않는다. catalog27건 포함 root1858통과231생략·extractor25·QA14·Node130통과, 독립205건은 실행0이다. 실제 기대값/전체 catalog·예약 API/정책 verifier·Linux/PG·운영 E2E가 남으며 기존62행과 Gate 최종 통과0을 유지한다. 상세는 `announcement-attachment-provider-qa-catalog-2026-09-12.md`와 최신 진행 기록을 따른다.

- 09-12 10:15 Provider QA 원장: V79의 불변 전체 요구 범위/run/case와 내부 ExecutionService를 연결했다. 기존 job/정책 QA와 전역 자원을 공유하고 소유권/만료/취소·원자적 누적 예산·정리 후 결과 지문/건수 대조를 구현했다. ATT-029~034/037/044/052/058/060/061의 부분 구현 근거다. 내부 Service35/기존 실행기42와 로컬 전체 root1831통과231생략·extractor25·QA14·Node130통과. 새 PG14사례를 포함한 독립205건은 발견만 했고 통과0이다. 전체 catalog/예약 API/정책 verifier·실제 Linux/PG/Provider/운영 브라우저는 미완료이며 PROVIDER_PROFILES MISSING/전체 Gate 최종 통과0과 기존62행을 유지한다. 상세는 `announcement-attachment-provider-qa-ledger-2026-09-12.md`와 최신 진행 기록을 따른다.

- 09-12 고정 공고 실행 단위: 제목 제외 요청0·전체 파일 목록/형식/binary 지문 검증·기존 flow/transport/signature/Linux extractor 호출·부분 실패/취소 분모·공유 자원 callback·420초/요청/byte 제한·원본 정리·안전한 결과 metadata를 구현했다. ATT-001/002/012~014/017/020~024/027/032/044/052/060의 부분 실행 단위 근거다. HTTP/격리 추출은 대역으로 검증했으며 실제 Provider/DB 원장/운영 증거가 아니다. PROVIDER_PROFILES MISSING/전체 Gate 최종 통과0을 유지한다. 최종 실행 단위42건·root1794통과217생략·extractor25·QA14·Node130통과이며 구체적 실행/캐시/미실행 경계는 최신 진행 기록을 따른다.

- 09-12 09:13 전체 Provider 요구 범위: 시스템12개 프로필에 기관/목록 parser 결합을 선언하고 모든 대상의 미구현·불일치·중복·단순 결합을 읽기 전용 provider-qa-plan API와 snapshot schema5에 연결했다. ATT-044/045/052/060/061의 부분 범위·권한·입력 고정 근거다. 전체 Provider 실파일/분할 원장은 미구현이며 이 목록/최소 요구량은 실제 성공이 아니다. 표적145·최종 root1752통과217생략·extractor25·QA14·Node130통과, 독립191건은 실행0이다. 기존62행과 운영/실파일 Gate를 유지하며 상세는 `announcement-attachment-provider-qa-scope-2026-09-12.md`를 따른다.

- 09-12 정책 DB QA 연결: WORKER_DB_RECOVERY를 부모 소유 namespace/임시 PG 실행·취소/정리·전체 사례/지문 검증과 게시 verifier에 연결했다. snapshot schema4는 독립 QA artifact/전체 suite/case를 고정한다. JSONB 필드 순서와 증거 저장 시각도 검증한다. ATT-029~031/034/037/044/045/053/055/058/060/061의 부분 구현 근거이며 실제 Linux 연결2사례·독립191건·전체 Provider·운영 E2E는 미완료다. 표적72/QA14 및 첫 전체 root1738통과217생략·extractor25·Node130통과, 최종 후속 결과는 최신 진행 기록을 따른다. 기존62개 조건과 Gate0~8을 축소하지 않는다.
- 08:57 최종 후속 회귀는3분13초 성공, root1740통과/217생략·extractor25·QA14·Node130통과다. JSONB/기록 시각 회귀와 시간 의존적인 Node fixture 수정을 포함한다. 독립191건은 INVENTORY_ONLY·통과0이며 부모 Linux2사례도 아직 미실행이다. 실제 운영·전체 Provider 증거로 대체하지 않는다.

- 09-12 BBS 증분: 태백·횡성·영월 고정 profile3개를 추가해 registry12개다. 기관별 exact URL/파일 영역·세션 비전달·빈 영역/누락 구분·10개 상한·UNKNOWN 역할·비지원 파일 보존과 제한된 MIME/파일명 헤더 호환을 구현했다. 각 기관3건/총9공고14파일(PDF2/HWPX12)1,499,273byte·23HTTP·원본 정리를 실제로 확인했다. ATT-004/011~014/020~024/028/044/060의 부분 근거이며 HWP 실파일·빈/비지원 혼합 실사이트·Linux/PG·전체 Provider/정책 QA·운영 E2E는 미완료다. MIME/헤더 초기 실패와 최종 회귀는 `announcement-attachment-standard-bbs-profiles-2026-09-12.md` 및 최신 진행 기록을 따른다. 기존62행의 완료 조건을 축소하지 않는다.
- 08:12 최종 회귀는 root1715통과/217생략·extractor25·QA13·Node130통과다. 실사이트22건 중 강북1건 BRIDGE_GET HTTP400으로 전체 명령은 실패했고, 동일 법정7표본 재검증은24초 통과했다. 원인 미확정/간헐 실패를 보존하며 전체 실사이트/운영 Gate 완료로 표시하지 않는다. 새 BBS9건은 최종 코드로 통과했다.

- 09-12 07:36 정부24 계약 증분: 실제 출처 `GOV24_PUBLIC_SERVICE`와 배치 필터 별칭 `GOV24`의 경계를 수정하고 V78에서 첨부 작업/전체 목록 CHECK를 확장했다. 정책 registry·QA 대상·UI 응답/한글 표기, 고정 분할 소속·멱등 재요청·미지원 profile 차단 회귀를 포함한다. root1684통과/216생략·extractor25·QA13·Node130통과. ATT-053 및 전체 Provider/기존 데이터 범위 계약의 부분 근거이며 실제 정부24 수집·PG·운영 검증은 미완료다. 새 PG1사례·V78 upgrade를 포함한 독립 목록191건은 발견만 했고 통과0이다. 상세는 `announcement-attachment-gov24-provider-contract-2026-09-12.md`를 따른다.

- 09-12 07:08 코드 지문/게시 경계 증분: 수동 코드 목록을 전체 main class/resource 1438파일과 실제 바이트 대조로 교체했다. 잠금 밖 설치/QA 검증 후 게시 잠금 안에서 최신 DB 근거를 재대조하며 변경·동일 요청 경합을 검사한다. root1678통과/215생략·extractor25·QA13·Node129통과다. ATT-034/044/045/061의 부분 코드·로컬 회귀 근거이며 실제 PG·추가 Provider/worker QA 및 운영 게시/E2E는 미완료다. 상세는 `announcement-attachment-code-fingerprint-2026-09-12.md`를 따른다.

- 09-12 06:48 Gate6 증분: 실제 worker/격리 추출/DB/검수/DRAFT 연결8사례를 작성하고 전용 task·독립 산출물·CI 필수 XML에 연결했다. HTTP는 고정 합성 입력이다. root1658통과/215생략·extractor25·Node129·QA12통과이며, worker8은 Linux/PG 미실행이다. 독립 목록190건 발견/passed0은 전체 통과가 아니다. 상세 범위와 마지막 조건부 테스트 정리 assertion 보강 후 재컴파일/패키징 증거는 최신 진행 기록과 `announcement-attachment-contract-runtime-2026-09-12.md`를 따른다.

- 09-12 06:20 Gate6 검증 경로 증분: 독립 Linux DB QA 산출물/실행 판정/CI 연결 및 정책 Node 테스트 누락을 보완했다. root1656통과/207생략·extractor25·Node129·별도 실행기/패키징11통과. 고정 suite 목록182건(job168/migration2/backfill12)은 INVENTORY_ONLY/실행0이며 실제 SQL·worker·운영 성공이 아니다. ATT-029~031/037/053/055/058/060의 실제 DB 증거를 확보할 실행 경로를 추가한 것으로 모든 행의 잔여 Gate를 유지한다. 상세는 `announcement-attachment-contract-runtime-2026-09-12.md`와 최신 진행 기록을 따른다.

기준: `announcement-attachment-qa-plan-2026-09-08.md`. 2026-09-11 현재 작업 트리의 연결표이며 전체 완료 증명서가 아니다.
원래 테스트 메서드의 일부 ATT 번호는 QA 문서 의미와 다르므로 **메서드 이름의 번호가 아니라 검증한 조건**으로 연결했다.

## 상태와 증거 범위

- 09-12 05:54 부산·강북 후속: root1862건 중1655통과/207생략, extractor25·Node129통과. 신규프로필14/고정3단계flow9/worker2/header2 및 form10/11 경계를 검증했다. ATT-004/011~014/020~024/028/044/060의 부분 근거다. 실사이트13사례 재실행 통과, 새7사례18파일(PDF2/HWPX16)1,698,868byte·비지원1개 미다운로드/원본 정리다. 전체 강제 실행 중 강북 HTTP4001회로 명령이 실패했고 동일표본29초 재검증은 통과했다. 원인은 미확정으로 보존한다. Linux/PG·전체 Provider/정책 QA·운영/브라우저는 여전히 미완료이며 `announcement-attachment-legal-board-profiles-2026-09-12.md`와 최신 진행 기록에 경계를 적었다.

- 09-12 05시 최종 Node 회귀는7파일129건 통과(정책34 포함), 실패/생략0이다. 아래 V77 UI의 마지막 모드3종·고정 항목 페이지/분모 검증까지 포함하며 실제 서버/DB/운영 증거는 아니다.

- 09-12 05:11 V77 게시 UI 후속: root1834건 중1628통과/206생략, extractor25, 정책 Node34통과. V77 서버/API/QA verifier 회귀와 준비·개별 영향 확인·요청/영수증 결합 UI를 포함한다. 실제 로컬 합성 브라우저 정상 게시/응답 유실 동일 key/body/만료/조회 전용·INCOMPLETE 차단, 키보드 확인·제출,320~1440px 가로 넘침0을 확인했다. ATT-044/045/052/053·정책 수명주기의 부분 근거이며 전체 요구 완료는 아니다. 전용 PG는 initdb 초기화3실패로 SQL 미도달, 후속 job task 미실행이다. 전체 Provider/worker QA 실행·검증기와 실제 Linux/DB·운영 E2E는 남는다. 상세는 `announcement-attachment-policy-publication-ui-2026-09-12.md`와 최신 진행 기록을 따른다.

- 09-12 04:17 게시 준비 범위 V76/API 회귀: root1788건 중1586통과/202생략, extractor25·Node117통과. Service19/HTTP21, Mapper/Migration 정적 계약과 PG6사례 작성을 추가했다. 전체 ID/상태 고정과 권한/CSRF·사유 비노출은 ATT-045/052/053·정책 수명주기의 부분 근거다. job PG165/전체 분할PG12/migration2는 생략이다. 실제 정책 게시·전체 QA·Linux/DB·운영 E2E를 통과한 결과가 아니다. 상세는 `announcement-attachment-policy-publication-scope-2026-09-12.md`와 최신 장기 진행 기록을 따른다.

- 09-12 03:41 전체 회귀: root1740건 중 **1544통과/196조건부 생략**, extractor25·Node117통과, 별도 공식 다운로드6사례/10파일통과. 화천 POST·공통 고정 호출 선형 해석·폼 변경 실패 근거 저장/한글 표시를 포함한다. job PG159/전체 분할PG12/migration2는 생략이며 새 parameterized PG 입력별 실행을 의미하지 않는다. 상세는 `announcement-attachment-hwacheon-post-profile-2026-09-12.md`다. 실파일 텍스트 추출·전체 Provider·운영 worker/DB/브라우저 증거는 아니다.

- 09-11 18:37 최신 회귀: root1517건 중 **1340통과/177조건부 생략**, extractor25·Node48통과, 실패/오류0. 일반 작업 목록/영향/ADMIN 원복 승인/영수증 UI를 연결하고 Node mount/계약16건, Service5/HTTP7/Mapper1건을 추가했다. SSR9건도 권한·잠긴 승인·필수 동의·영수증 제목을 검증했다. 일반 이력 PG1건을 추가한 최신 job PG153건은 전부 생략이다. 최종 전체 rerun/build/installDist는3분56초에 성공했다. 실제 SQL/trigger·전체 배치 UI·운영/렌더링 브라우저 성공 증거는 아니다.
- 09-11 18:09 최신 회귀: root1503건 중 **1327통과/176조건부 생략**, extractor25·Node32통과, 실패/오류0. 일반 원복 Service21/HTTP15와 Mapper26/MigrationContract84가 통과했다. 일반 APPLIED 및 실패 예약의 영향 조회·ADMIN 승인·원자적 복구·멱등 응답·원래 실패 보존을 구현하고 PG12건을 추가했다. 최신 job PG152건은 모두 생략이다. 최종 전체 rerun/build/installDist는3분23초에 성공했지만 실제 SQL/trigger·관리자 복구 UI·운영/브라우저 성공 증거는 아니다.
- 09-11 17:46 최신 회귀: root1453건 중 **1289통과/164조건부 생략**, extractor25·Node32통과, 실패/오류0. 일반 작업 예약 전 버전/확인 binding 고정과 적용 후 지문 저장, 근거 변경 방지의 migration 정적2건·Mapper1건을 추가했다. PG8건을 추가해 job PG140건은 모두 생략이다. 최종 코드로 전체 rerun/build/installDist가3분21초에 성공했지만 실제 SQL/trigger·일반 원복 승인/실행·운영 및 브라우저 성공 증거는 아니다.
- 09-11 17:24 최신 회귀: root1442건 중 **1286통과/156조건부 생략**, extractor25·Node32통과, 실패/오류0. BatchRollbackService20/HTTP16/Scheduler2, Mapper/MigrationContract와 승인·전체 입력 CAS·이전 binding 복구·중복/충돌·무효 확인 보호·분모 보존을 검증했다. job PG132건은 모두 생략이다. 기존 확인 복원 PG fixture도 실제 승인/worker 경로에 연결했고 PG8건을 추가했지만 실제 DB/운영 성공 증거는 아니다. 첫 전체 실행은 512 MiB 힙 압박을 확인해 소유 worker를 종료했으며, 컨텍스트 캐시4 제한 후 전체 회귀가 3분32초에 성공했다. 테스트/검증 조건은 축소하지 않았다.
- 09-11 16:52 최신 회귀: root1394건 중 **1246통과/148조건부 생략**, extractor25·Node32통과, 실패/오류0. ReviewService41/MapperBinding23과 이전 확인의 불변 원본·복구 유효 버전 분리, 잘못된/오래된 복구 근거 차단, 최신 버전 DRAFT 조건을 검증했다. 새 PG6건을 포함한 job PG124건은 모두 생략이다. DB 복구 근거 생성은 테스트 fixture에만 있으며 전체 원복 승인 API/worker·배치 UI·운영 E2E 완료를 뜻하지 않는다.
- 09-11 16:23 최신 회귀: root1376건 중 **1234통과/142조건부 생략**, extractor25·Node30통과, 실패/오류0. BatchApplicationService23/HTTP18/Scheduler2와 승인 접수·중지/재개·항목별 CAS·불변 승인·실패/backoff·수집/적용 결과 분리를 포함한다. 추가 PG8건을 포함한 job PG118건은 생략되어 실제 SQL/trigger·운영 성공을 뜻하지 않는다. 조건부 rollback/검수 복원 계약·배치 UI·전체 Provider/QA/E2E는 미완료다.
- 09-11 15:46 회귀: root1322건 중 **1188통과/134조건부 생략**, extractor25·Node30통과, 실패/오류0. BatchPreviewService16/HTTP18과 SEALED 근거·불변 이력·명시적 선택·0HTTP·입력 변경 충돌·삭제 수 구분의 당시 증거다. 최신 변경과 실제 운영 검증은 별도다.
- 09-11 15:09 회귀: root 1279건 중 **1151통과/128조건부 생략**, extractor25·Node30통과, 실패/오류0. BatchService26/BatchController24/BatchClaim5와 수집 승인·중지/재개·전체 집계·고정 입력 충돌의 당시 증거다. 최신 변경의 검증은 위 실행과 구분한다.

- [x] 해당 요구의 전체 필수 계층을 직접 검증. ATT-053의 기록된 완료 근거만 해당하며, 나머지 요구와 전체 운영/브라우저 Gate 완료를 뜻하지 않는다.
- [~] 코드·부분 로컬 테스트는 있으나 실제 파일/DB/운영/API/UI 중 필수 증거가 남음.
- [ ] 구현 또는 직접 assertion을 추가해야 함.
- [!] 해당 DB 검증이 현재 환경에서 차단. Windows Code Integrity 3077의 libpq.dll 로드 거부, Docker 시작 timeout. 기존 통과 기록을 최신 변경의 증거로 대체하지 않는다.
- 10:23 root 703건(1실패)은 caller fixture 수정 후 해소. 이후 root 726건(659통과/67생략) 성공은 **24시간 간격 수정 전** 증거다.
- 24시간 간격 추가 후 Current/Intake/MigrationContract 선택 검증 78건 통과. 최종 전체 회귀 결과는 진행 기록에서 별도 갱신한다.
- 공개 파일 4개 CLI의 과거 기록은 실제 worker/DB/API/운영 ACTIVE의 성공이 아니다.
- 09-11 10:47 로컬 회귀: root 992건 중 898통과/94조건부 생략, extractor 13통과, 실패/오류 0. 규칙/정책 불일치 준비 차단과 workflow 구조 검증을 포함한다. Node 보고서 판정 9건도 통과했다. 배포·운영·브라우저 또는 생략 PG 성공 증거가 아니다.
- 09-11 11:31 최신 로컬 회귀: root 1053건 중 **952통과/101조건부 생략**, extractor 13통과, 실패/오류 0. 정책 service 26/HTTP 25건, Mapper/identity migration 정적 검증을 포함한다. 초안 API 통과는 정책 검증·게시나 DB 실제 실행 성공이 아니다.
- 09-11 11:59 최신 로컬 회귀: root 1104건 중 **1000통과/104조건부 생략**, extractor 13통과, 실패/오류 0. GoldenGate 10/CheckService 18/CheckController 18건과 추가 Mapper/MigrationContract를 포함한다. 서버 분류 사례 AG-001~030은 실파일·전체 QA/게시·운영 검증을 대신하지 않는다.
- 09-11 14:08 최신 로컬 회귀: root 1207건 중 **1090통과/117조건부 생략**, extractor 25통과, Node 30통과, 실패/오류 0. 정책 QA 예약/취소·snapshot·부분 증거 INCOMPLETE·lease guard를 포함한다. 실제 Linux/PG/운영 E2E 완료 증거는 아니다.
- 09-11 14:35 최신 로컬 회귀: root 1246건 중 **1124통과/122조건부 생략**, extractor25/Node30 통과·실패0. 배치 범위 service14/HTTP17과 Mapper/MigrationContract를 포함한다. 현재 수집 시작·분류 preview·적용·실행 중지/원복·전체 처리 UI는 미완료다.
- PG 접두 테스트는 `attachmentJobIntegrationTest`의 이전 40건 통과 기록과 현재 추가·변경한 153건을 구분한다. 최신153건은 Linux 실행이 필요하다. XML/parameter 오프라인 검증을 SQL/trigger 통과로 대체하지 않는다. 수동 Linux workflow와 skip/오래된 보고서 차단기를 추가했지만 원격 실행은 아직 없다.

## 파일 키

2026-09-12 화천 POST 증분: `discovery/HwacheonPostAttachmentDiscoveryProfileTest` 13건, `AttachmentDownloadInvocationTest` 4건이 exact source·POST4필드·불투명 값 비저장·긴 입력/표현식 거부·빈 영역/부분/한도·등록을 검증한다. 실제 다운로드 task는 GET4사례+화천2사례/총10파일이다. `AnnouncementAttachmentDiscoveryEvidenceTest` 6건과 worker26건에 폼 실패 전달/저장 대역을 포함하고, `AnnouncementAttachmentJobIntegrationTest.att013DiscoveryWarningIsBoundToImmutableManifestAndReadApi`에 새 코드 PG 입력을 추가했다(미실행). Node UI 회귀는 실패 사유 한글 표시·NO_FILES와 구분을 검증한다. ATT-013의 발견 실패를 저장 단계에서 잃지 않도록 연결했지만 전체 요구 완료로 판정하지 않는다.

2026-09-12 새올 GET 증분: `discovery/SaeolGetAttachmentDiscoveryProfileTest`는 4기관 구조·출처/URL/인코딩·NO_FILES/FAILED·한도·UNKNOWN 역할·등록 계약을, `SaeolAttachmentProfileLiveQaTest`는 실제 공식 상세4/첨부7개 다운로드·signature·임시 정리를 검증한다. ATT-004/011~014/020~024/028의 **부분 증거**이며 실파일 텍스트 추출/전체 profile/worker DB 성공은 아니다. 기존 기업마당/대전 서구의 파일명 기반 역할 추정도 제거했고 MANUAL 역할 재시도 보존 회귀를 유지했다. 상세는 `announcement-attachment-saeol-get-profiles-2026-09-12.md`를 따른다.

2026-09-11 추가: `AttachmentRuntimeGate`와 12개 빌드 합성 입력 AR-001~012, parser/CLI fixture 단위 테스트 및 `AttachmentRuntimeGateIntegrationTest`를 연결했다. 품질·한글/이모지·정확한 근거 좌표·원본 정리·전후 지문을 검사한다. **실제 Linux 실행은 미확인**이며 root 테스트에서의 조건부 생략이나 mock 실행은 ATT 완료 증거가 아니다. 상세 범위는 `announcement-attachment-policy-validation-2026-09-11.md`를 따른다.

`src/test/java/com/saneb/domain/announcementattachment/` 아래:

- E: `classification/AnnouncementAttachmentClassificationEngineTest.java`
- W: `worker/AnnouncementAttachmentWorkerServiceTest.java`
- F: `worker/AttachmentFileTypeValidatorTest.java`
- B/S: `discovery/BizInfoAttachmentDiscoveryProfileTest.java`, `discovery/SeoguSaeolAttachmentDiscoveryProfileTest.java`
- T/I: `extraction/AttachmentTemporaryStorageTest.java`, `extraction/IsolatedAttachmentExtractorTest.java`
- Runtime: `extraction/AttachmentRuntimeGateTest.java`, `extraction/AttachmentRuntimeGateIntegrationTest.java`; 12개 입력의 parser/CLI 검증은 `attachment-extractor/src/test/java/com/saneb/extractor/AttachmentRuntimeFixtureTest.java`
- PolicyValidation: `service/impl/AnnouncementAttachmentPolicyValidationServiceTest.java`, `service/impl/AttachmentPolicyValidationSnapshotFactoryTest.java`, `controller/AnnouncementAttachmentPolicyValidationControllerSmokeTest.java`. ATT-029/030/031/045/052/060/061에 연결되는 예약 멱등성·권한·변경/취소·원문 비노출·입력 고정/공유 슬롯 계약의 부분 증거다. 대응 PG 12건은 미실행이며 전체 policy QA/게시 검증은 아니다.
- HTTP: `controller/AnnouncementAttachmentControllerSmokeTest.java`
- CurrentController/CurrentService/IntakeServiceTest: 같은 패키지의 `controller/AnnouncementAttachmentCurrentControllerSmokeTest.java`, `service/AnnouncementAttachmentCurrentServiceTest.java`, `service/AnnouncementAttachmentIntakeServiceTest.java`
- PG: `src/test/java/com/saneb/db/AnnouncementAttachmentJobIntegrationTest.java`
- WorkerDB: `src/test/java/com/saneb/db/AnnouncementAttachmentWorkerIntegrationTest.java` — 실제 worker/DB/추출 8사례, HTTP만 합성. Linux 미실행이다. `AttachmentWorkerFixtureContractTest` 2건은 그 합성 입력·실패 계약만 로컬 검사한다.
- D: `src/test/java/com/saneb/domain/announcementsource/provider/content/AttachmentDownloadBoundaryTest.java`
- X: `attachment-extractor/src/test/java/com/saneb/extractor/AttachmentExtractorTest.java`
- SourceServiceImplTest: `src/test/java/com/saneb/domain/announcementsource/service/impl/AnnouncementSourceServiceImplTest.java`
- RoleService/RoleController: `service/AnnouncementAttachmentRoleServiceTest.java`, `controller/AnnouncementAttachmentRoleControllerSmokeTest.java`
- Checkpoint: `service/AnnouncementAttachmentCheckpointServiceTest.java`; mapper binding은 `src/test/java/com/saneb/db/AnnouncementAttachmentMapperBindingTest.java`
- RetryService/RetryEvidence/RetryController: `service/AnnouncementAttachmentRetryServiceTest.java`, `service/AnnouncementAttachmentRetryEvidenceTest.java`, `controller/AnnouncementAttachmentRetryControllerSmokeTest.java`
- HistoryService/HistoryController/ReadService: `service/AnnouncementAttachmentHistoryServiceTest.java`, `controller/AnnouncementAttachmentHistoryControllerSmokeTest.java`, `service/AnnouncementAttachmentReadServiceTest.java`
- CollectionService/CollectionController: `service/AnnouncementAttachmentCollectionServiceTest.java`, `controller/AnnouncementAttachmentCollectionControllerSmokeTest.java`

구현은 `src/main/java/com/saneb/domain/announcementattachment/{classification,discovery,worker,extraction,service,dao,controller}`,
`src/main/resources/mapper/announcementattachment`, V72/V73 migration과 연결된다. 기존 경로 guard는 announcementsource service 구현에 있다.

## 62개 요구 추적

| ID | 요구 | 현재 구현/테스트 근거 | 상태 | 남은 필수 증거 |
|---|---|---|---|---|
| ATT-001 | 제목 B: 요청/원문 0 | E.excludedOrUnmatchedTitleCannotEnterAttachmentEngine; PG.att001And002…; W.att001… | [~] | 실제 목록→상세/첨부 요청 0, 사용자 비노출 E2E |
| ATT-002 | 제목 조합 미충족 복구 금지 | E.excludedOrUnmatchedTitleCannotEnterAttachmentEngine; PG.att001And002… | [~] | 실제 worker 요청 0 |
| ATT-003 | 제목 A는 첨부 정상이어도 검수 | 분류 엔진 정책, PolicyGoldenGate AG-019 제목 A+정상 NOTICE 직접 검증 | [~] | 최신 실제 DB·브라우저 회귀 |
| ATT-004 | 파일명 B는 분류 입력 아님 | B.discoversAllFiveFilesAndKeepsFilenameOutOfClassificationInputs | [~] | 실제 filename-only B 표본의 DB 판정 |
| ATT-005 | NOTICE 첨부 B는 검수 | E.attachmentGroupBRequiresReviewNeverTitleExclusion | [~] | 운영 ACTIVE 근거·검수 표시 |
| ATT-006 | 첨부 A/B 동시: B 우선/A 보존 | E.attachmentGroupBRequiresReviewNeverTitleExclusion; PolicyGoldenGate AG-007 B 주사유·A/B 양쪽 근거 직접 assertion | [~] | 최신 실제 DB/API·운영 확인 |
| ATT-007 | 본문 부족을 같은 첨부가 보완 | E.attachmentCanSupplementMissingBodyWithoutChangingBase; PG.att016And036… | [~] | 실파일 worker→DB→API |
| ATT-008 | 본문 실패와 첨부 처리는 분리 | 별도 detail/descriptor 경로; a884ac1 Linux35679213890의 WorkerDB.bodyFetchFailureStillCollectsAttachmentsAndPreservesBaseFailureEvidence 통과(worker12/12·실패/생략0 XML 확인) | [~] | 본문 수집기 실패+유효 첨부 실사이트 표본 및 최종 운영 확인 |
| ATT-009 | 파일 간 AND 금지 | E.separateFilesCannotSatisfyAnd | [~] | 실제 다중 첨부 판정/locator |
| ATT-010 | FORM은 참고 근거 | E.formIsContextOnlyEvenWhenItContainsFullCombination | [~] | 관리자 역할 수정·재검수 흐름 |
| ATT-011 | UNKNOWN/불명확 scope는 검수 | E.unknownRoleAndUnreliablePdfScopeRequireReview | [~] | 실제 UNKNOWN 역할 표시/후속 행동 |
| ATT-012 | 정상 영역 0건은 NO_FILES | B.verifiedEmptyContainerIsDifferentFromMissingOrBrokenSelector; W.att012…; PG.att012And057… | [~] | 대상 profile별 실사이트 0건 |
| ATT-013 | selector 실패는 NO_FILES 아님 | B.verifiedEmptyContainerIsDifferentFromMissingOrBrokenSelector; S.changedLayoutAndUnresolvedAdditionalDownloadAreNotNoFiles; W.att013… | [~] | profile별 발견 실패·운영 표시 |
| ATT-014 | 일부 NOTICE 실패도 전체 근거 보존 | E.partialFileCannotBeHiddenByAnotherSuccessfulFile; W.att014EveryFile…; RetryEvidence 비선택 성공/실패 보존; W.manualRetryMultipleFailuresReusesNewSuccessOnItsNextLease | [~] | 최신 PG·다중 실파일 실패/재시도·성공 파일 재사용 |
| ATT-015 | 제외 문맥은 긍정 후보 아님 | E.negativeEligibilityIsNotPositiveCandidate | [~] | 본문/첨부 상충 표본·운영 검수 |
| ATT-016 | 혼합/스캔 PDF 품질 | X.blankPdfRequiresOcrAndDoesNotBecomeNoFiles; W.att020Ocr… | [~] | 텍스트+스캔 혼합 PDF 실파일 전체 페이지 검증 |
| ATT-017 | 한글 PDF/HWP/HWPX locator | X.pdfTextIsExtractedWithoutInventingReliableTableScopes; X.hwpxKeepsKoreanParagraphAndCellLocationsSeparate; X.hwp5KoreanRecordsHaveParagraphNotFakePageNumbers | [~] | 합성 fixture 외 모든 대상 profile/형식 실파일; 한글 PDF |
| ATT-018 | HWP 추정 page 번호 금지 | X.hwp5KoreanRecordsHaveParagraphNotFakePageNumbers | [~] | 실제 HWP section/paragraph/cell API 표시 |
| ATT-019 | 암호/손상/0byte 상태 | X.encryptedPdfIsExplicitlyBlocked; X.hwpEncryptedFlagIsNotIgnored; X.hwpTruncatedRecordIsNotCompleteText; Runtime AR-004/005/012 고정 입력 검사 | [~] | 실제 Linux 실행·0byte/손상 실파일의 worker 상태/로그 |
| ATT-020 | HTML 위장 파일 추출 금지 | F.errorHtmlIsNeverAcceptedBecauseItsNameSaysPdf; X.htmlNamedPdfIsNotAPdf; W.att020Ocr… | [~] | 공식 다운로드 endpoint 실패 응답 실증 |
| ATT-021 | 확장자 없는 binary 검증 | F.signatureAndMimeAcceptSupportedFormatsWithoutUrlExtensions | [~] | 실제 Content-Disposition/MIME/격리 parser |
| ATT-022 | redirect allowlist 재검증 | D.redirectRevalidatesPathBeforeDnsOrSecondHttp; D.postRedirectNeverForwardsFileNamesOrParametersToAnotherEndpoint | [~] | 기관별 GET redirect 실제 연결·host lease 교체 |
| ATT-023 | SSRF/DNS 재바인딩 방어 | AttachmentPinnedDownloadClientTest; D.stalledDnsTimesOutWithoutStartingHttpAndResolverExits | [~] | Linux 실제 pinned transport/사설망 canary |
| ATT-024 | 파일/스트리밍/누적 예산 | D.reservesBytesBeforeReadingAndKeepsActualBytesSeparate; D.deniedBudgetReadsNoBytesAndDeletesOnlyNewPartialFile; PG.att024And060…; T.att024… | [~] | 최신 PG + 실제 20/80 MiB 경계 |
| ATT-025 | 압축 폭탄과 앱 생존 | D.compressedOrOverLimitResponsesAreRejectedBeforeReading; X.archiveBombIsRejected; 9a1bb45 Linux35681482535 runtime5/5: 실제 HWP20,000/20,001문단 경계·LIMIT_EXCEEDED·다음 정상 추출·소유 원본 정리0.99초, 실제 자식 heap 제한/부모 재개0.279초 통과. 추출기90/90·실패/생략0 | [~] | af2ad8f의 JSON null 기대값 시험 오류는 수정 후 실제 재검증으로 해소. OS OOM killer/다양한 악성 parser 입력·최종 운영 검증은 별도 |
| ATT-026 | XXE/ZIP 경로 이탈 | X.hwpxDtdAndExternalEntityAreRejected; X.zipTraversalIsRejectedWithoutCreatingFiles; Runtime AR-008/009 CORRUPT·원본 정리 검사 | [~] | 실제 Linux 실행·canary 읽기/외부 요청 0 |
| ATT-027 | timeout/OOM/프로세스 종료 정리 | I.processCommandRequiresNetworkPidFilesystemAndMemoryIsolation; W.failedSaveNeverLeavesOriginalFiles; T.att038…; a884ac1 Linux35679213890 Runtime.actualThirtySecondTimeoutKillsObservedChildTreeAndReleasesOwnedOriginal 30.303초 통과: 실제30초 TIMEOUT·관측 자식 JVM 종료·소유 원본 정리·다음 추출 재개 | [~] | 최종 운영 설치/환경 대조와 강제 crash·다양한 악성 parser 입력 검증은 별도 |
| ATT-028 | 매크로/embedded/JS 비실행 | S.arbitraryJavascriptAndTraversalAreNeverExecutedOrRequested; I.processCommandRequiresNetworkPidFilesystemAndMemoryIsolation | [~] | 매크로/embedded 파일 실표본 |
| ATT-029 | 동일 키 멱등 | PG.att029And030…; PG.att029SimultaneousSameKeyReturnsExactlyOneJob; PG.att029CompletedEvaluation…; Role/Retry/CollectionService 동일 actor/key/정규화 요청 | [~] | 최신 PG 재실행·정책/배치 API |
| ATT-030 | 같은 키 다른 요청 409 | PG.att029And030ReserveIdempotentlyWithoutChangingBaseOrAllowingDifferentRequests; CollectionService 다른 actor/body/key 충돌 및 HTTP 409 | [~] | 최신 PG·정책/배치 API |
| ATT-031 | lease 동시 claim/늦은 응답 | PG.att031TwoWorkers…; PG.att031ExpiredOwner…; W.att031SealedCrashRecovery…; W.restartRetriesFailedFileOnly…/Checkpoint; RetryEvidence 최종 fence | [~] | 최신 PG checkpoint 복구·실제 두 worker/중간 장애·sealed 복구 |
| ATT-032 | 같은 URL binary 교체 | set binary/manifest hash 계약; PG.att014And032…; W.newJobGenerationDoesNotReusePriorJobEvenWithSameLocator | [~] | 같은 URL 다른 binary 실수집 2세대 |
| ATT-033 | 같은 본문 첨부 갱신 예약 | SourceServiceImplTest.attachmentPlanIsFrozen…; PG.sameContentOnALaterRunWaits24HoursThenCreatesANewGeneration; IntakeServiceTest | [~] | 최신 PG 24시간 경계·실제 바이너리 교체 |
| ATT-034 | 내용/검수/version 변경 충돌 | PG.att034ChangedSource…; PG.att034And044BaseChange… | [~] | 검수 변경 API 동시성/늦은 worker |
| ATT-035 | 원문 삭제 후 되살림 금지 | PG.att035And038…; W.att001MissingEligibleSource…; a884ac1 Linux35679213890 WorkerDB.sourceDeletedDuringRealExtractionCannotBeRecreatedByLateWorkerResult 통과(worker12/12·실패/생략0 XML 확인) | [~] | 최종 운영 설치에서 삭제/늦은 결과/임시 자원 정리 대조. 임시 Linux DB 증거와 운영을 구분 |
| ATT-036 | source 불일치 접근 404 | HTTP.sourceMismatchReturns404Wrapper; HistoryService/HistoryController 다른 source/evaluation/file 404; ReadService 제목 제외·QA·기본 판정 없는 조회 차단 | [~] | 최신 PG·운영 각 READ 역할의 교차 source 요청 |
| ATT-037 | 다른 source/release DB 근거 거부 | Linux35050057694 Migration3/3·PG.att061PolicyProfileAndRuleMustMatchWithoutUpdatingSourceOnFailure 통과 | [~] | 운영 API·최종 설치에서 교차 근거 거부 재확인 |
| ATT-038 | 제목 제외 cascade/임시 정리 | PG.att035And038…; T.att038SuccessAndExceptionScopes…; T.att038ExpiredOrphan… | [~] | Linux 실제 OS crash 후 원본 정리 |
| ATT-039 | 역할 변경은 새 set/STALE | RoleService/RoleController: 새 SEALED set·추출 provenance/시각 보존·버전/STALE·OFF 무HTTP; PG 역할/동시 요청/재사용 무결성 4건; 복구 UI·Node·합성 브라우저 OFF에서 역할 변경 예약/검수 잠금 | [~] | 최신 PG 실행·역할 변경 후 재검수 운영 E2E |
| ATT-040 | 미완료/오래된 확인 전환 거부 | ReviewServiceTest stale/setHash/OPEN/처리중/cross-source; ReviewControllerSmokeTest 409; RetryService/새 PG 선택 재시도 후 확인 STALE; 단건 복구 UI·Node 및 합성 브라우저 409 공유 쓰기 잠금/입력 보존/재확인 검증 | [~] | 최신 PG 실행·실제 운영 두 세션 충돌/재검수 |
| ATT-041 | 기존 V1/V2 검수 우회 차단 | 기존 전환/분류/재분류 service guard 테스트; PG.att041LegacyGuard… | [~] | 운영 소비자 경로·브라우저 확인 |
| ATT-042 | 첨부 미적용 기존 전환 유지 | AnnouncementSourceV2ConversionServiceImplTest 등 기존 회귀 | [~] | 운영 미적용 source 정상 DRAFT 흐름 |
| ATT-043 | 정상 확인→DRAFT 멱등 | ReviewServiceTest 같은 요청/다른 요청/자동 활성화 차단; ReviewControllerSmokeTest; PG 확인·동시 DRAFT/actor-source key race; 합성 브라우저 확인→별도 초안/응답 유실 재시도, Node 동일 payload/key | [~] | 최신 PG 실행·운영 중복 클릭 E2E |
| ATT-044 | OFF는 새 요청만 중지/검수 유지 | W.att040OffBeforeDetail…; PG.att040OffImmediately…; CurrentServiceTest; Current PG projection; PublicationImpact의 해당/전체 규칙 고정 작업·검수/적용/원복 관측·OFF 중지조건 해제 영향(읽기 전용) | [~] | 정책 게시/교체 transaction·실제 OFF·sealed 복구·최신 PG |
| ATT-045 | 역할/CSRF | HTTP/Current/Review/Role/Retry/History/CollectionControllerSmokeTest: READ 3역할, WRITE 2역할; PolicyControllerSmokeTest·PolicyServiceTest: READ 3역할/ADMIN 변경·금지 필드400/CSRF/직접 service 방어; 정책 UI8 SSR·22 Node·합성 브라우저 읽기 역할 변경0/명시 동의 | [~] | 정책 전체 QA/게시 API 및 실제 운영 세션·CSRF 확인 |
| ATT-046 | 배치 범위 고정 | V74 전체 목록·V75 고정 분할→batch 연결/전체 집계; a884ac1 Linux35679213890 BackfillIntegrationTest14/14·실패/생략0: 1001건 두 분할/신규 미편입/삭제·경합/불변 소속 XML 확인; 전체 분할 UI17 Node·8 SSR | [~] | 실제 승인 범위 운영 실행/최종 대조·운영 브라우저 |
| ATT-047 | scope/분류 preview 네트워크 0 | Batch/BackfillService 순수 전체/단일 범위 조회; BatchPreviewService exact 봉인 metadata·불변 이력/선택·HTTP0; 배치 UI 전체 페이지 개수/중복/최종 지문 대조·선택 보존 | [~] | 최신 PG/worker·운영 실제 HTTP0 및 실제 브라우저 검증 |
| ATT-048 | preview 뒤 버전 변경 충돌 | BatchPreview/Application CAS; BackfillService 전체 지문409, V75 분할 예약/시작/claim 입력 확인 및 기관 변경 차단 계약; 배치/분할 UI 응답 유실 동일키 유지·401/403/409 Node | [~] | 최신 PG·분할 예약/실행 실제 경합·운영 적용/충돌·실제 관리자 브라우저 |
| ATT-049 | 추가 검수/전환 뒤 rollback 거부 | 배치 원복 영향/ADMIN·CSRF 승인 API와 전체 inputHash 재검증 worker; 일반 원복 UI의 버전·효과/충돌/유실 동일 요청·잠금; 배치 승인 이력/영수증 대조·다른 배치 결과 해제 Node/SSR; 새 검수·DRAFT·기관 변경 차단 PG | [~] | 실제 PostgreSQL·일반/배치 복구 브라우저·운영 검증 |
| ATT-050 | 변경 없는 binding 원복 | 배치 고정 승인/조건부 CAS; 일반 APPLIED/실패 예약 원자적 복구·확인/실패 보존·무효 확인 STALE; 일반/배치/전체 분할 UI. a884ac1 Linux35679213890 PG192/192·실패/생략0의 approvalHistoryPagesStayCompleteWhileLaterApprovalsAdvanceTheBatch·sourceDeletionDoesNotRewriteOrHideOriginalApprovalHistory·mixedApplicationAndRollbackHistoryRetainsOriginalImpactAfterCompletion 통과 XML 확인 | [~] | 운영 되돌리기·일반/전체 배치 브라우저·승인 범위 분할 실행/대조 |
| ATT-051 | 연결된 운영 공고 보호 | 기존 일반 경로 보호 유지. V87~V90, LINKED_EVIDENCE_ONLY 전용 범위·예약·수집 제어·불변 연결 snapshot·별도 재검수 경고 구현. 00d16d9 Linux CI37087157343 성공과 합성 worker/DB 계약 증거는 본 문서 최신 기록 및 linked-evidence 설계 문서 참조. 10-05 11:56 배포37256870718 성공·운영 V90 설치 확인 | [~] | 실제 연결 공고의 외부 파일 worker·운영 브라우저·운영 업무 데이터 불변 검증 필요. 설치 성공은 연결 공고 E2E 성공이 아니다. 운영 표본 수집은 대상·영향 승인 후 실행 |
| ATT-052 | 원문/secret/XSS 분리 | D.formIsBoundedImmutableAndDoesNotAppearInDiagnosticStrings; 근거/History HTTP no-store·좌표만 반환; PG.att036And052…; 합성 브라우저 HTML 문자 비실행·코드포인트 강조, 새 SSR/Node 테스트 | [~] | 운영 실제 로그·HAR·화면 검증; 수집 오류 로그 점검 |
| ATT-053 | 새 DB/업그레이드/checksum | Linux35693601984/272dafd의 freshSchemaAndV71UpgradePreservePriorChecksums4.036초 통과. 기존10테이블/V71 합성 업무 데이터 보존, V72~83 순차/빈 DB validate, false CHECK INSERT·UPDATE 실제 거부. 운영 설치 JAR 대 이력80/80 checksum 일치(SSM638ec0bb-b863-4bd0-a2b7-a87eb589f3c6, 쓰기0) | [x] | 이 요구의 필수 증거 충족. 전체 운영 업무/Provider/브라우저 완료를 의미하지 않음 |
| ATT-054 | OFF/COLLECT_ONLY 기존 Golden 유지 | 기존 분류/수집 회귀와 E/CurrentServiceTest | [~] | 정책별 통합 Golden + 운영 ACTIVE 동일 release |
| ATT-055 | 동시 확인/전환 | source 잠금·평가 current unique; a884ac1 Linux35679213890 PG.concurrentConfirmationsAllowOneWriterAndReturnConflictForStaleVersion·concurrentSameDraftRequestCreatesOneAnnouncement 통과(PG192/192·실패/생략0 XML 확인) | [~] | 최종 운영 세션의 동시 검수/DRAFT E2E 검증 |
| ATT-056 | 문단/불명확 셀 AND 금지 | E.separateParagraphsCannotSatisfyAnd; E.unknownRoleAndUnreliablePdfScopeRequireReview | [~] | 실제 PDF/HWP/HWPX 표/문단 worker evidence |
| ATT-057 | OPEN/pending 판정 ID null | PG.att031UnsealedOrExpiredWorker…; CurrentServiceTest.enforcePending…; HistoryService pending의 과거 판정은 NOT_CURRENT | [~] | 최신 PG 및 운영 화면 pending→sealed |
| ATT-058 | source 같아도 다른 입력 조합 거부 | Linux35050057694 immutableEvidenceRejectsMismatchedBindingsAndCascadesWithSource·역할 근거 소속 시험 통과 | [~] | 최신 운영 API에서 교차 set/policy/base 거부 검증 |
| ATT-059 | 추출기 비밀·망·파일 격리 | f496d2e Linux35631537040 및9a1bb45 Linux35681482535의 실제 subprocess 환경/호스트 파일/loopback 차단·입력/라이브러리 read-only 통과. 09-22 운영9a1bb45의 CI 코드 지문·추출기1.0.3·JAR별 QA/worker 경로·공용/QA library set·bwrap/prlimit 존재 대조. 10-05 11:56 worker true 확인 이력과 기존 HWPX 단건 처리 이력은 운영 인수 문서 참조 | [~] | worker 활성화·일반 파일 처리 이력은 운영의 비밀·망·호스트 파일 격리 직접 검증을 대신하지 않는다. 최신 설치의 격리 검증 필요. 합성 canary/설치 metadata 일치를 모든 악성 파일/자원 고갈·운영 E2E 검증으로 확대하지 않음 |
| ATT-060 | 전역 동시성/한도 상향 금지 | PG.att060DownloadAndHostAndExtractionCaps…; PG.att024And060…; D/T 한도 검사; CollectionService 공유 수동 한도·132 HTTP 상한; PolicyService/HTTP 초안 1~80 MiB·임의 실행 설정 거부 | [~] | 최신 PG + 두 실제 worker·실제 정책 검증/게시 상한 |
| ATT-061 | base/정책 release 불일치 | E.mismatchedRuleReleaseIsRejectedBeforeEvaluation; PG.att061PolicyProfileAndRule…; Intake/SourceService/MapperBinding 미일치 ENFORCE·퇴역 규칙 준비 차단; PolicyService 초안 DRAFT/ACTIVE 규칙 허용; PublicationImpact 최신 QA/필수 재검증 및 정책 UI의 조회 버전·QA 누락/불일치·퇴역 선택 차단 | [~] | 최신 PG 실행·정책 전체 QA/게시/교체·운영 검증 |
| ATT-062 | COLLECT_ONLY는 preview만 변경 | CurrentServiceTest.collectOnlyKeepsBaseEffective…; HistoryService CURRENT_PREVIEW와 AUTO 태그 구분; Current/History PG projection | [~] | 최신 PG 목록/count/태그와 운영 브라우저 |
