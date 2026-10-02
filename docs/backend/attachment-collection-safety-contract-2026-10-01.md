# 수집 전용 검증 계약 분리

## 승인·범위

2026-10-01 사용자 승인에 따라 로컬 설계·구현한다. 운영 게시, worker 활성화,
ENFORCE 전환, 기존 데이터 적용, 추가 배포는 이 승인에 포함하지 않는다.
추출기는 1.0.16을 유지한다. 기존 V1 API와 V1~V85 migration은 수정하지 않는다.

2026-10-02 사용자는 이번 변경만 QA 브랜치에 커밋·푸시하고 배포 없는 Linux CI를
실행하는 범위를 승인했다. 운영 게시·worker 활성화·ENFORCE·기존 데이터 적용·배포는
실행하지 않는다. 실제 PostgreSQL 검증 결과는 CI 종료 후 별도로 기록한다.

## 계약

| 계약 코드 | 적용 | 필수 단계 | 성공 상태 |
| --- | --- | --- | --- |
| STRICT_V1 | 기존 이력, ENFORCE, OFF | 분류 정답·설치 runtime·전체 provider 실파일·worker DB 복구 | VERIFIED |
| COLLECTION_SAFETY_V1 | 새 COLLECT_ONLY QA | 분류 정답·설치 runtime·수집원 결합 점검·worker DB 복구 | COLLECTION_VERIFIED |

계약 코드는 불변 QA 입력 snapshot에 저장하고 snapshot hash에 포함한다.
계약 코드가 없는 과거 이력은 STRICT_V1이다. 알 수 없는 계약은 거부한다.
수집 전용 성공은 ENFORCE 승인으로 사용할 수 없다. 정책 모드·버전 변경은
고정 입력을 무효화하며 새 QA와 별도 게시가 필요하다.

수집원 결합 점검은 현재 전체 시스템 목록과 프로필 결합을 검증한다.
결합 성공 대상이 하나도 없으면 실패한다. 누락·중복·파서 불일치 대상은
전체 분모에 남기고 상태별로 분리한다. 정상 결합 대상만 기존 실행 경계로 처리한다.
이 점검은 외부 접근, 다운로드 또는 추출 성공 증명이 아니다. API key 부재,
외부 차단, 첨부 발견 실패, 다운로드 실패, 추출 실패는 실제 실행 결과로 별도 기록한다.
전체 provider별 정상 3공고·전체 첨부 정답은 ENFORCE 계약에만 요구한다.

네 단계의 증거는 서버가 생성하고 현재 설치·코드·규칙·대상 목록에 결합한다.
SSRF 방어, 파일·요청·시간 한도, 격리 추출, 원본 정리, lease, 취소,
부분 성공 보존 및 다음 공고 처리 검증은 완화하지 않는다.
게시 잠금·범위 봉인·현재성 재검증·멱등·원자적 이전 정책 폐기는 유지한다.

## API·화면

기존 v2 실행 조회에 validationContractCode를 추가한다. COLLECTION_SAFETY 단계와
COLLECTION_VERIFIED를 별도 표시하며 전체 실파일 검증 완료로 표현하지 않는다.
게시 영향 조회·준비 범위·게시 실행은 동일한 계약을 판정한다.
사용자가 parser나 성공 증거를 선택·제출하지 않는다.

## 검증 체크리스트

- [x] V86 코드·정적 계약: 새 상태·단계, 불변 계약, 모드/단계 혼합 차단 및 게시 DB 방어.
- [x] 서비스 구현·대역 회귀: 계약별 실행·완료·게시·준비 범위·영향 조회 연결.
- [x] UI 구현·비브라우저 계약: 수집 전용과 전체 검증을 명확히 구분.
- [x] 단위 회귀: 과거 STRICT_V1, 수집 전용 성공, ENFORCE 재사용 거부, 변조·누락 거부.
- [x] 실제 PostgreSQL migration/trigger 및 Linux 검증: 2026-10-02 Linux36883912352/0fa8631 success. V85→V86 업그레이드·기존 checksum/행 보존·수집 게시 제약과 필수273시험의 실패·오류·생략0을 artifact XML로 확인했다. 최초36881855740 실패는 아래에 보존한다.
- [x] 로컬 선택 test 453개(21 suite), 실패·오류·생략 0 및 bootJar 성공.
- [x] Node 화면 계약 45개, 실패·생략 0. 브라우저는 현재 명시 지시가 없어 실행하지 않았다.

## 실행·증거 경계

실행 명령:

```powershell
.\gradlew.bat :test --tests '*AttachmentPolicy*Test' --tests '*AnnouncementAttachmentPolicy*Test' --tests '*AttachmentCollectionSafetyGateTest' --tests '*AnnouncementAttachmentCollectionServiceTest' --tests '*AttachmentDownloadBoundaryTest' --tests 'com.saneb.db.MigrationContractTest' bootJar --no-daemon --console=plain --max-workers=1
node --test scripts/qa/attachment-policy-ui.test.mjs
git diff --check
```

2026-10-01 위 Gradle 명령 실행은 1분48초 성공이다. 중간 실행에서 신규 테스트의 지역변수 선언 누락과
Mockito 중첩 stubbing 오류를 발견·수정한 후 동일 범위로 재실행했다. 테스트를 생략하지 않았다.
V85→V86 업그레이드·기존 checksum/행 보존과 수집 계약 게시/ENFORCE 재사용 거부의
실제 DB 시험을 기존 Linux harness에 추가했으나, 로컬 453개 통과 수에는 포함하지 않는다.
2026-10-01 로컬 구현 단계에서는 외부 공고 요청·운영 조회/변경·커밋·푸시·배포를
수행하지 않았다. 2026-10-02 후속 범위는 승인된 QA 브랜치 커밋·푸시와 내부 Linux
fixture 검증이며 외부 공고 관측 옵션은 활성화하지 않는다.

화면 Design Read: 기존 Thymeleaf/Bootstrap 관리자 작업 공간을 유지하고 상태·계약·영향을
텍스트로 구분한다. 사용자 목표는 수집 가능 여부와 분류 적용 승인 여부를 혼동하지 않는 것이다.
주 사용자는 한국어 운영 관리자, 위험 R2(게시), 주요 과업은 초안 QA·근거 확인·게시 영향 검토다.
레이아웃·키보드/초점·확인/재조회 절차는 유지했고 상태/오류/충돌 경로는 Node 대역으로 검증했다.
실제 렌더링·접근성·반응형은 브라우저 미실행으로 미확인이다.

## 2026-10-02 첫 Linux CI와 후속 보완

커밋 `db4fca54d4f9ed401897cdf372e9bdece9f06ac2`를 QA 브랜치에 푸시했다.
[Linux CI 36881855740](https://github.com/FrostyCityMan/saneB/actions/runs/36881855740)는 실패이며
artifact `11172236202`의 XML에서 일반 시험 1건과 job DB 시험 1건의 실패를 확인했다.
두 시험은 목록 projection 변경 전의 `inputSnapshotJson=null` 기대값을 유지하고 있었다.
전체 snapshot 비노출 원칙은 그대로 유지하며 계약 코드·모드 두 필드만 반환하는 정적 SQL과
실제 DB 목록을 검증하도록 보완했다. 과거 계약 기본값과 새 수집 계약도 각각 확인한다.
시험을 삭제·생략하거나 실패를 성공으로 보고하지 않는다.

첫 실행에서 migration18·runtime6·worker12·Flyway3·산출물 계약20은 실패·생략0이다.
job DB212 중1실패, 일반4596 중1실패/조건부생략381이며 추출기 시험은 별도다.
V85→V86 업그레이드와 새 수집 게시/ENFORCE 재사용 차단 시험은 통과했지만 전체 Gate는
미완료다. 독립 산출물·정책 부모 실행은 앞 단계 실패로 실행되지 않았다. 후속 전체 CI가 필요하다.

후속 로컬 검증은 `:test --tests 'com.saneb.db.AnnouncementAttachmentMapperBindingTest'
--tests '*AttachmentPolicy*Test' --tests '*AnnouncementAttachmentPolicy*Test'
--tests '*AttachmentCollectionSafetyGateTest' --tests 'com.saneb.db.MigrationContractTest'
bootJar --no-daemon --console=plain --max-workers=1`로 실행했다. 20 suite/457시험,
실패·오류·생략0, BUILD SUCCESSFUL(1분34초)이다. 실제 DB 두 목록 projection의 검증은
후속 Linux CI로 확인한다. 제품 코드나 migration은 이번 실패 보완에서 변경하지 않았다.

## 2026-10-02 Linux Gate 완료·설치 후보

검증 SHA는 `0fa8631879e427e28c0470a515e6b56162df1d5c`다.
[Linux CI 36883912352](https://github.com/FrostyCityMan/saneB/actions/runs/36883912352)는
success이며 artifact `11174407016`의 XML과 필수 보고서 판정 결과를 회수했다.
일반4596시험 중4215통과·381조건부생략·실패/오류0이다. 생략381을 통과로 산입하지 않는다.
별도 필수273시험(산출물20·job DB212·migration18·runtime6·worker12·Flyway3·정책 부모2),
추출기251시험, Node531시험은 실패·오류·생략0이다. 전체 Gradle/bootJar·독립 산출물 실행과
정책 부모 연결·취소·정리도 통과했으며 `POLICY_DB_QA_CLEANUP=SUCCEEDED`를 확인했다.

새 계약의 COLLECT_ONLY 게시·완료 이력 불변·ENFORCE 재사용 거부와 과거/새 목록 projection
시험이 실제 PostgreSQL에서 실행됐다. 원문·운영 DB·외부 사이트 요청을 사용하는 시험은
활성화하지 않았다. 이는 내부 계약 근거이며 운영 수집·실파일 전수 성공·브라우저 근거가 아니다.
첫 실패와 미실행 단계 기록은 이전 실행 이력으로 유지한다.

**Decision: Conditionally ready — 검증 SHA의 코드 설치 후보 범위.**

| Gate | 상태 | 근거·필요 조치 |
| --- | --- | --- |
| Git·구현·빌드 | [x] | QA 브랜치 푸시와 고정 SHA Linux success; 후속 기록은 문서 전용 |
| DB·호환성 | [x] | V86 실제 upgrade/trigger·checksum/행 보존; V1 및 과거 migration 미변경 |
| 보안·격리·복구 계약 | [x] | runtime/worker/정책 부모 및 취소·정리 시험 통과 |
| 운영 현재 기준선·백업·설정 | [!] | 후속 읽기 전용 사전 인증에서 AWS_AUTH_REFRESH_REQUIRED; 로그인 갱신 후 확인 필요 |
| 코드·V86 운영 설치 승인 | [ ] | 새 SHA 설치·migration·재시작은 이번 커밋/CI 승인에 미포함 |
| 운영 수집·분류·브라우저 | [ ] | 정책 게시·worker 활성화·ENFORCE·기존 데이터 적용·브라우저 미실행 |

실행 명령: `git commit -F ...`, QA 브랜치 `git push`, `gh run view/download`,
기존 Linux workflow의 전체 Gradle·Node·독립 QA harness, artifact XML 집계다.
미실행: 새 운영 설치·현재 health/DB/플래그/백업 조회·외부 공고 관측·운영 업무 E2E.
브라우저는 사용자 정책에 따라 현재 명시 지시가 없어 실행하지 않았다.

남은 위험: V86은 과거 파일을 수정하지 않지만 CHECK/보호 함수 교체를 포함하므로 JAR 원복과
DB DDL 복구는 별개다. 설치 전 이전 JAR·추출기와 DB 복구 경로를 재확인한다.
다음 순서는 설치 범위 승인 → 현재 기준선/복구 경로 확인 → 동일 SHA 배포 빌드 통과 →
코드/V86 설치·재시작 → JAR/DB checksum·health·기존 설정 유지 확인이다.
정책 게시·첨부 worker 활성화·ENFORCE·기존 데이터 적용은 해당 영향도와 범위 승인 후 별도 수행한다.

## 2026-10-02 운영 설치 사전 확인 — 인증 갱신 필요

자동 goal 후속 턴에서 기존 승인된 읽기 전용 진단의 `Invoke-SanebAwsReadOnly.ps1 -Action Inventory`를
실행했다. STS 인증 단계에서 `AWS_AUTH_REFRESH_REQUIRED`로 종료되어 현재 계정/프로젝트 일치,
EC2·SSM·운영 runtime/DB/health·백업·배포 상태는 조회하지 못했다. 과거10월1일의 V85/health/플래그
확인을 현재 상태로 재사용하지 않는다. 새 배포 실행·운영 쓰기·브라우저 실행은0이다.
도구는 TLS 검증을 유지하고 프로세스 환경을 복원했으며 `OWNED_CA_BUNDLE_REMOVED`를 확인했다.

GitHub의 고정SHA `0fa8631`/Linux36883912352 success와 저장소 배포 변수5개의 존재를 재확인했다.
Node로 Git blob을 대조하여 기존 migration82개 변경0·V86만 추가(현재83파일), 검증 SHA와
현재 HEAD의 src/scripts/workflow 차이0을 확인했다. Node 실행은 종료됐다.
남은 입력은 AWS 로그인 갱신과 고정SHA 코드/V86 설치·재시작 승인이다. 범위를 확대해
정책 게시·worker 활성화·ENFORCE·기존 데이터 처리를 실행하지 않는다.

성공 기준은 승인된 로컬 구현·커밋/푸시·Linux 검증 완료다. 운영 수집 시작은 별도 Gate다.
일부 검증만 실행하거나 skip된 검증을 완료로 보고하지 않는다.

## 2026-10-02 10:03 KST 코드·V86 설치 승인과 사전 확인

사용자가 AWS 로그인 갱신과 고정SHA `0fa8631879e427e28c0470a515e6b56162df1d5c`
코드/V86 설치·서비스 재시작을 승인했다. root/저장소 계정 일치·서울 대상1대/SSM Online을
확인했고 인증 차단은 해소됐다. 기존 b638ad7/V85·service active/내부health UP,
migration82파일/82이력 checksum 불일치0, 첨부정책/파일/job0과 기존 플래그를 재확인했다.
실제 서비스 DB의 Aurora 자동백업3개 available·7일 보존·암호화/삭제보호·PITR 가용성과
이전 JAR/불변 extractor release 보존을 확인했다. 실제 복원/rollback 시험은 미실행이다.

고정 ref `codex/redeploy-0fa8631-20261002`를 승인SHA에 생성·대조했다.
로컬 설치 helper Node17시험 중14통과/Windows 조건부생략3/실패0이다. Linux 전용3시험은
Windows 성공 수에 포함하지 않으며 배포 Linux 검증에서 실행한다. Node는 종료됐다.
배포는 `attachment_qa=false`로 실행하며 정책 게시·첨부 worker 활성화·ENFORCE·기존 데이터
처리·외부 파일 QA·브라우저는 제외한다. 새 설치 결과와 전체goal 완료를 미리 선언하지 않는다.

## 2026-10-02 사용자 일시정지·배포 취소

goal 상태는 paused다. 배포 Actions36949131922를 빌드/시험 단계에서 취소했고
최종 completed/cancelled를 확인했다. bundle/AWS 인증 설정/S3 업로드/CodeDeploy 생성은
모두 skipped이며 새 코드 설치·V86 운영 migration·서비스 재시작은 실행되지 않았다.
사전 확인·인증 갱신과 고정 ref는 보존하며 재개 지시 전 추가 실행하지 않는다.

## 2026-10-02 장기 goal 재개·동일 SHA 설치 재실행

- [x] SSO 전환은 조회 전용으로 검증했으며 권한을 확대하지 않았다. 별도로 기존 승인된 root 읽기 전용 SSM 진단을 사용했다.
- [x] Runtime `5dd694a1-0b34-4f0c-ae65-ebe118728931`, Database `7b336e89-1d84-4a19-abfb-003887056460`, MigrationIntegrity `ee4d403c-4950-4cb0-bbf0-449a8e1f89a9` 모두 Success. b638ad7/V85, 82개 migration checksum 일치, 서비스 active/health UP, 첨부정책·파일·job0, 기존 플래그 유지다.
- [x] Aurora 자동백업3개 available/7일 보존, PITR 최신15:01:50 KST, 이전 JAR/release 보존 확인. 실제 복구 시험은 미실행이다.
- [x] 설치 helper Node17개 중14통과/Windows 조건부생략3/실패0. 사용한 Node 종료. Git HTTPS 조회는 로컬 CA 오류였으며 TLS를 해제하지 않고 인증된 GitHub API로 고정 ref와 승인 SHA 일치를 확인했다.
- [~] `gh run rerun 36949131922` attempt2/고정SHA0fa8631의 Linux 빌드·배포를 재실행했다. attempt1 취소는 이력으로 보존한다. 설치 결과와 V86 운영 적용은 아직 확인 전이다.
- [ ] 정책 게시·첨부worker 활성화·ENFORCE·기존 데이터 적용·운영 업무 E2E는 별도 미완료다. 현재 설치 실행을 수집 시작이나 전체 goal 완료로 보고하지 않는다.

### 15:14 KST 설치 후 검증 완료

Actions36949131922 attempt2 success/CodeDeploy d-K9Y4RHO4L Succeeded.
승인SHA0fa8631 bundle/설치JAR 일치, DB V86/83개 migration checksum 일치,
service active/health UP, 이전 JAR/release 보존과 기존 플래그 유지 확인.
첨부정책·파일·job0이며 정책 게시/worker/ENFORCE/기존 데이터 적용은 하지 않았다.
상세 지문·SSM 영수증·검증 범위는 [운영 기준선](../deployment/attachment-runtime-baseline-2026-09-15.md)의 최신 항목을 따른다.
코드 설치 하위 Gate만 완료이며 전체 goal은 active다. 사용자 로그인 후 관리자 정책·배치·기존 데이터·대기열·첨부 상세를 읽기 전용으로 확인했다. 정책/배치0, 준비 후보0, 3단계 미적용2945건이며 표본의 검수/DRAFT 버튼은 비활성이다. 업무 변경 E2E는 남아 있다.

정책 게시 안내의 모드 구분 누락을 로컬에서 수정하고 Node46/Java8/bootJar를 통과했다. 운영0fa8631에는 이 문구 수정이 미반영이다.
다음 승인 범위는 COLLECT_ONLY 초안1건(구간1.0.4), 정책 QA 기능 활성화·재시작, 수집 전용 QA1회다.
정책 게시·첨부 worker 활성화·ENFORCE·기존 데이터 배치는 이 준비 단계에 포함하지 않는다.

### 2026-10-02 15:41 KST 안내 수정 Linux 검증 완료

- [x] 고정 SHA `688dfe92d244889eb1bbcab9e9415e46b75b9c43`의 [Linux36973533218](https://github.com/FrostyCityMan/saneB/actions/runs/36973533218) success. 운영0fa8631과 비교한 제품 변경은 게시 안내 한 문장과 회귀 테스트뿐이며 migration·수집 로직 차이는0이다.
- [x] artifact `11213545562`를 새 경로에 내려받아 XML 집계: 일반4596 중4215통과/381조건부생략/실패0. 별도필수273(계약20/job212/migration18/runtime6/worker12/정책DB2/Flyway3) 및 추출기251은 실패·오류·생략0이다.
- [x] CI Node532통과/실패·생략0. 독립 패키지의 namespace·PostgreSQL 실행과 정책 부모의 실제 DB 연결·취소·정리 성공, `POLICY_DB_QA_CLEANUP=SUCCEEDED` 확인.
- [x] 보고서 집계에 사용한 Node는 종료됐다. 이 CI는 외부 공고 요청·운영 배포·운영 쓰기 없는 내부 검증이며 브라우저 E2E 증거를 대신하지 않는다.
- [!] 다음 운영 준비 승인 대기: COLLECT_ONLY 초안1건(구간1.0.4), 정책 QA 기능 활성화·서비스 재시작, 수집 전용 QA1회. 게시·상시 worker·ENFORCE·기존 데이터 배치는 제외한다. 미승인 설정을 변경하지 않는다.

실행 근거: `gh run view 36973533218`, `gh run download 36973533218`, Node XML 헤더 집계와 `validateSuite`, CI의 TAP 집계·정리 결과 대조, `git diff --check`.
검증 완료를 전체 goal 완료로 확대하지 않는다. 운영 설치는 여전히0fa8631/V86이고 안내 수정본은 미배포다.
