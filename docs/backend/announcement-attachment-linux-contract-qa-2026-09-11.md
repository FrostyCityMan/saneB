# 첨부 DB·작업 계약 Linux 검증 경로

## 범위와 현재 상태

### 2026-09-14 — 변경된 3단계 흐름의 Linux QA 재개

- GitHub 저장소 소유 계정의 push 권한을 현재 API로 확인했다. 아래의 과거 접근 차단 기록은 현재 권한 상태가 아니다.
- 기본 브랜치를 변경하거나 배포 workflow를 실행하지 않고 `codex/attachment-three-stage-linux-qa` 브랜치에 검증 범위를 게시한다. 이 정확한 브랜치의 push와 수동 실행만 QA trigger로 허용하며, 운영 자격증명·OIDC·정책 활성화는 사용하지 않는다.
- 제목 제외 후 상세/첨부 요청 금지, 본문 판정 뒤 첨부 분석 지속, 기술 미완료와 최종 검증 후보 분리라는 [변경된 처리 설계](announcement-three-stage-filtering-workflow-2026-09-14.md)를 유지한다. 새 대기열 Node 18사례와 실제 DB 9상태·29행/전체 count 검증을 Linux 실행 범위에 포함한다.
- 현재 독립 계약 목록은 job190·migration2·backfill13·worker8=213건이고 additive migration은 V81까지다. 목록 확인은 실행 성공이 아니다. 이 단락 작성 시점의 원격 Linux 실행 결과는 미확인이다.
- 변경한 workflow 구조 테스트 5건과 대기열/보고서 Node 28건은 로컬 통과했다. 이전 전체 로컬 결과 및 환경 생략은 최신 실증 결과와 분리한다. 운영 배포 판정은 계속 **Not ready**다.

첫 원격 결과: [34824820039](https://github.com/FrostyCityMan/saneB/actions/runs/34824820039), SHA `bd148024611284b6c8ca2d6997e5ed2e87309721`, **실패**. 실제 PG job192건 중147통과/45실패/생략0이다. V81까지의 초기 migration은 수행됐지만 migration 전용 회귀·격리 runtime/worker·독립 산출물 실행은 이 실행에서 미실행이다. 컬럼 매핑39건과 시험 fixture6건을 수정 후 같은 범위로 다시 실행한다. 이후 Gradle `--continue`로 독립 task 실패도 함께 모으되 실패 종료 코드와 보고서 Gate를 유지한다.

두 번째 [34825668605](https://github.com/FrostyCityMan/saneB/actions/runs/34825668605), SHA `17243d54a132598d37e13753482eaef8fe6a9959`도 **실패**. job191/192, 첨부 migration2/2, 분할1/13, Linux 합성12파일 격리12/12, worker7/8통과를 확인했다. 분할의 GROUP BY parameter SQL 오류와 지연 제약/미지원 파일 상태의 시험 기대값을 수정 후 재검증한다. 독립 실행에 포함할 DB2개 parameterized 입력을 고정 @Test로 분리하여 사전 목록은215건으로 일치시켰다. 독립 namespace 전체 실행과 정책 부모 연결은 아직 미실행이다.

세 번째 [34826506160](https://github.com/FrostyCityMan/saneB/actions/runs/34826506160), SHA `6dc12e12073e6e46a9ab7566380949eb9cd48c42`는 주 Gradle 검증을 통과했다. job192/192, migration2/2, 분할13/13, Linux 합성12파일 격리12/12, worker8/8, root2064통과/241조건부 생략, extractor25·독립 QA 패키지15·Node152통과 및 bootJar/설치 패키지 생성 성공이다. 그러나 독립 namespace 실행은 `CLEAN_ENVIRONMENT_REQUIRED`로 실패했고 임시 자원 정리는 성공했다. 이후 정책 부모 연결은 미실행이므로 workflow 전체는 실패다.

원인: bubblewrap의 `--clearenv`는 `PWD`를 제거하지 않으며 `--chdir /work`에 맞춘 값을 유지한다([공식 v0.6.1 문서](https://github.com/containers/bubblewrap/blob/v0.6.1/bwrap.xml)). 실행기의 허용 목록에서 이 고정 값이 누락됐다. `PWD=/work`만 검증하도록 수정하고 누락·다른 경로·경로 정규화 표현과 외부 설정/DB 환경변수는 계속 거부한다. 보안 격리나 전체 시험 분모를 완화하지 않는다. 수정 뒤 전체 원격 검증을 다시 실행해야 한다.

### 이전 구현 기록

09-12 후속: 독립 QA 산출물 `installAttachmentContractQa`에 실제 worker/격리 추출/DB 연결 8사례와 정부24 출처 정합성 1사례를 추가했다. 현재 고정 목록은 job168·migration2·backfill13·worker8=191건이며, Linux 실제 실행은 여전히 미확인이다. migration 검증은 V78까지 포함한다. HTTP는 고정 합성 입력이고 실제 사이트 수집이 아니다. 아래 과거159건은 당시 기록이다. 상세·명령·승인 경계는 [독립 worker·DB QA 산출물](announcement-attachment-contract-runtime-2026-09-12.md)을 따른다.

- [x] 수동 실행 전용 workflow와 필수 보고서 판정기 구현.
- [x] 보고서 판정기 자체 테스트 10건, workflow YAML 구조·승인 경계 테스트 4건 로컬 통과.
- [ ] 원격 workflow 실행, 정책 초안/개정7건·분류 이력3건·비동기 QA계약12건·배치 범위5건·수집 제어6건·preview/선택6건·적용8건·확인 복구 근거6건·원복 승인/worker8건·일반 작업 예약/적용 근거8건·일반 승인/원자적 복구12건·일반 작업 이력1건·배치 승인 이력3건·게시 영향 관측3건을 포함한 최신159개 job DB 테스트 및2개 migration 테스트 실제 통과 확인.
- [ ] 추가한 `attachmentRuntimeIntegrationTest`의 12개 고정 합성 파일 Linux 실제 격리 실행.
- [ ] `attachmentWorkerIntegrationTest`의 8개 worker/DB/추출 사례와 독립 산출물의 191개 전체 계약 실행. 목록 발견만으로 통과하지 않는다.
- [ ] 별도 Spring Flyway integration, 실제 공개 파일/worker/운영 API/브라우저 검증.

Windows Code Integrity의 embedded PostgreSQL library 거부와 Docker 엔진 불가를 우회하기 위해 OS 보안을 완화하지 않는다. 기존 GitHub 저장소의 임시 Ubuntu runner에서 검증 가능한 경로를 추가한 것이며, 이 문서 작성이나 로컬 단위 테스트로 Linux Gate를 통과 처리하지 않는다.

## 실행 경로

- workflow: `.github/workflows/attachment-contract-qa.yml`, 이름 `첨부 DB·작업 계약 Linux QA`.
- trigger: `workflow_dispatch`와 정확한 QA 브랜치 `codex/attachment-three-stage-linux-qa`의 push만 사용한다. 기본 브랜치 push 자동 실행, 배포, cron, AWS OIDC, 운영 secret 참조가 없다. 권한은 `contents: read`다.
- Ubuntu 22.04/Java 21, 최대 30분, 동시 실행 1개, Gradle worker 1개로 실행한다.
- 대상 task: `:test :attachment-extractor:test attachmentContractQaTest attachmentJobIntegrationTest attachmentMigrationTest attachmentRuntimeIntegrationTest attachmentWorkerIntegrationTest bootJar :attachment-extractor:installDist installAttachmentContractQa --rerun-tasks --no-daemon --console=plain --max-workers=1`. 이후 `run-attachment-contract-qa.sh`를 실행하며 실패를 무시하지 않는다. 실제 원격 실행 결과는 아직 없다.
- 해당 임시 runner에서만 `bubblewrap`을 설치하고 `/usr/bin/bwrap`, `/usr/bin/prlimit`을 확인한다. 보안 정책/sysctl/AppArmor 해제나 privileged container를 사용하지 않는다. 도구 설치·실제 namespace 실행 실패는 실패로 남긴다.
- `attachmentRuntimeIntegrationTest`는 설치된 distribution으로 12개 고정 합성 PDF/HWP/HWPX를 처리한다. 파일별 기대 품질·정확한 문자/좌표·원본 정리·전후 runtime 지문을 검사하며, 운영 URL/DB/임의 관리자 파일 입력을 사용하지 않는다. Windows에서 이 전용 task를 실행하면 격리 부재로 실패한다.
- `attachmentJobIntegrationTest`와 `attachmentMigrationTest`가 각자의 환경 조건을 true로 설정하고 loopback 전용 임시 PostgreSQL을 생성·종료한다. DB URL/사용자/비밀번호를 입력받거나 운영 환경에서 가져오지 않는다.
- 공개 첨부 QA·규칙 export·별도 Spring Flyway integration 환경 조건은 false다. 의존성 다운로드를 제외하고 외부 공고 Provider를 호출하지 않는 계약 검증이다.

커밋된 대상 SHA가 원격에 있고 현재 로그인 계정에 Actions 실행 권한이 있을 때 이 workflow를 선택해 수동 실행한다. 실행하지 않았다면 Actions 성공 링크를 만들거나 성공했다고 보고하지 않는다. 기존 `deploy.yml`은 변경하지 않았으며 별도 배포·승인 경계를 그대로 따른다.

## 생략 방지와 증거

- `scripts/qa/attachment-contract-report.mjs`는 네 전용 task(DB job, migration, 설치 runtime, worker)의 필수 5개 클래스 XML 헤더를 읽는다. 보고서 부재, 다른 클래스, 0건, 실패, 오류, 조건부 생략이 있으면 종료 코드 1이다. workflow가 Gradle 시작 전에 기록한 `ATTACHMENT_QA_STARTED_AT`보다 오래된 보고서 또는 시작 시각 누락도 거부한다. 과거 로컬 전용 task 결과를 최신 실행으로 재사용하지 않는다.
- 판정기는 JUnit 보고서 집계의 검증 장치다. 테스트 내용을 보증하거나 전체 ATT-001~062/실파일/운영 E2E 완료를 판정하지 않는다. 다른 root test의 환경 조건 생략도 별도로 보고해야 한다.
- `--rerun-tasks`와 새 runner checkout을 사용하여 이전 로컬 결과를 채택하지 않는다. XML은 해당 코드 SHA 이름의 Actions artifact로 7일 보관한다. 이 경로는 합성 테스트 데이터만 사용하며 운영 원문/자격증명/HAR를 artifact에 넣지 않는다.
- 실행 증거에는 대상 commit SHA, Actions run URL, 각 suite의 실행/통과/실패/오류/생략 건수와 시각을 기록한다. DB 계약 통과는 설치 artifact·운영 migration 성공과 다르다.

## 실패 시

- runner의 initdb/라이브러리/테스트/제약 위반은 실제 실패로 남긴다. skip·assertion 삭제·격리 해제로 성공시키지 않는다.
- workflow 시간 제한 또는 취소 시 runner 수명 종료로 임시 자원이 폐기된다. 정상 테스트 종료는 embedded PostgreSQL close와 single-use Gradle daemon 종료를 사용한다.
- 서버 test를 운영 DB에 직접 실행하거나 운영 서비스를 중지하지 않는다. 정책 게시·ENFORCE·기존 데이터 일괄 적용은 이 workflow와 무관하며 정확한 범위 승인 이후 별도로 수행한다.
