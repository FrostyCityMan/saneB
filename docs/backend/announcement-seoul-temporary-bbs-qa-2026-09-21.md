# 서울 서버 임시 격리 태백 QA

## 2026-09-22 경로·CLI 호환성 보완 후 실제 관측 성공

- [x] 읽기 전용 사전 점검 SSM `fa63d757-87b1-46aa-a4e3-4a17ac302acb`: 서버 `/usr/local/bin/aws`는 없고 `/usr/bin/aws`가 있다. 이전 실행기 고정 경로 결함을 확인했다. 운영 JAR는 d8696e85…로 동일하다.
- [x] 실행기를 저장소 `scripts/qa/run-temporary-bbs-observation.py`로 옮기고 두 관리 경로의 실행 가능 여부를 확인하도록 수정했다. 실패 단계·공식 공고 작업 시작 여부를 비식별 metadata로 기록한다.
- [x] 재시도 SSM `91fa3811-95e2-4ffa-8549-f52160f07f39`는 PACKAGE_DOWNLOAD_FAILED/공식 공고 작업 시작false였다. 읽기 전용 진단 `cca9a3f1-ebff-4459-986b-3b2ebca650b2`에서 CLI1.22.34와 해당 객체 접근/크기/소유 metadata 일치를 확인했다. CLI v2 전용 인자를 제거하고 AWS_PAGER 환경변수를 쓰도록 보완했다. IAM/버킷 정책 변경은 없다.
- [x] 최종 SSM `10df4968-07d1-49ce-b2c3-d768003c9f9c` Success/responseCode0. 실행 ID `0bb7506bece84c729b58391e61a980cb`, 관측 시각 `2026-09-21T16:32:11.928279006Z`(09-22 KST)이다. 실제 probe27.983초, JUnit1/1·실패/생략/중단0이다.
- [x] 제목 COMBINATION_MATCHED → 본문 AVAILABLE/431자·1시도 → 전체 HWPX2개 COMPLETE_TEXT → 전체 텍스트 분석true다. 첫 파일2041자/62블록 UNKNOWN/MIXED_DOCUMENT_ROLES, 둘째1994자/117블록 FORM이다. 종합 REVIEW_REQUIRED/ATTACHMENT_CONTEXT_REVIEW·최종 관리자 검증을 유지한다.
- [x] 본문 상한 포함5요청 예약/2,377,939bytes, CPU quota/period100000/100000, MemoryMax805306368bytes, 임시공간1073741824bytes를 실제 확인했다. 운영 DB 사용false/쓰기0, 원본·probe·전송 임시 파일 정리true, unitInactive/JAR불변/healthUp=true다.
- [x] 이번 두 재시도의 S3 패키지는 각각 terminal 상태·소유 metadata를 확인한 후 삭제/부재 확인했다. 세 실행 전체의 plan.cleaned=true다. 로컬 package/result는 증거 및 재현용으로 보존하며 사용자 output은 변경하지 않았다.
- [x] 전체2파일의 locator/downloadAllowed/format/binary/quality/문자·블록 수/역할 규칙/text/blocks/assessment hash가 기존 기대값과 일치한다. production 코드 차이는 BBS redirect의 최초 요청 경로·공고 식별자 보존 강화3행뿐이다. 내용·역할·필수 문구·예산·참조30건을 유지한 채 catalogVersion/profileHash/observedAt3개 metadata만 갱신했다. 구지문3종 거부 회귀와 현재 실제 profile hash 대조를 유지했다.
- [x] Python 회귀9건·Node27건(24통과/Windows Linux 전용3생략)·갱신 전 Java41건 통과. catalog 갱신 후 표적159건/패키징20건 실패·생략0,1분34초 성공 및 새 bootJar/QA 패키지 생성이다.
- [x] 전체 로컬 회귀4분24초 성공: root2751=2486통과/265조건부 생략/실패·오류0. 패키징20·추출기88·bootJar는 이 호출에서는 UP-TO-DATE 재사용이다. 표적 실행에서 생성한 새 웹 JAR SHA256은 `15d5e97dd7e440cebae1853d155f030b92ba56ff0f51611233837454edc485cf`다. 모든 로컬 Java/Node 작업 종료를 확인했다.
- [~] 새 Linux/production 고정 비교 결과는 별도 확인한다. 관측 성공을 고정 기대값의 새 재비교 성공으로 대체하지 않는다.

최종 패키지188,072,770bytes/138파일 SHA256 `85356506bf117b68bd24ad74ba8b7b81cd81e548f8ddd34a7b03e4d2e59d11e6`, 관측 application code catalog hash `b53a038435151fba7427f22cc2bd5b4192e92870c5213e36e8cdd85628e24590`다. `build/temporary-bbs-qa-0bb7506bece84c729b58391e61a980cb/result.json`은 원문 없는 상세 근거다. 이는 별도 임시 코드 패키지의 관측이며 운영 설치 SHA의 최신 배포나 worker/DB/API/업무 브라우저 E2E를 증명하지 않는다. 전체 정상 후보0·정책 QA false·Gate0~8 미완료다.

## 2026-09-22 인증 갱신·기존 실행 결과·정리 확정

- [x] 사용자 승인으로 AWS default 로그인을 갱신했다. 현재 Windows 신뢰 루트를 사용했고 SSL 검증을 해제하지 않았다. STS root 및 저장소 대상 계정·서울 리전 일치를 확인했다. 임시 공개 CA 번들은 삭제했다.
- [x] 기존 SSM `35926df3-e555-4c7a-bf95-444a3568d9c9`만 조회했다. 결과는 `Failed`, responseCode1이다. 새 QA 제출/공식 사이트 재요청은 하지 않았다.
- [!] 격리 실행기 결과는 `INCOMPLETE / FileNotFoundError / RUNNER_FAILED`이다. 오류 대상 경로와 실패 단계는 기록에 없어 원인을 특정할 수 없다. 실제 본문·첨부 관측 보고서와 요청량, 자원 제한 실측값도 반환되지 않았다. 성공 또는 요청0으로 간주하지 않는다.
- [x] 해당 실행 종료 시점의 결과에 `unitInactive=true`, `installedJarUnchanged=true`, `healthUp=true`가 확인된다. 이는 09-22에 조회한 이전 실행의 사후 상태이며 09-22 health를 새로 요청한 결과는 아니다. 임시 namespace 종료 근거는 있으나 파일별 삭제 성공 metadata는 없어 파일 정리 검증과 구분한다.
- [x] terminal 상태와 객체 크기/소유 metadata를 재확인하고 해당 실행의 임시 S3 패키지만 삭제했다. 정확한 키의 객체 부재와 로컬 계획 `cleaned=true`를 확인했다. 운영 객체 삭제0이다. 삭제한 전송 패키지는 로컬 `package.zip`으로 재생성/재전송할 수 있으며 자동 재전송하지 않는다.
- [x] `TemporaryPoll`, `TemporaryCleanup` 완료 후 로컬 Node 단발 검증으로 plan/result 일치를 확인했다. Node 및 로그인/조회 명령은 종료됐다. 사용자 `output/`와 기존 변경사항을 보존했다.
- [ ] 다음은 격리 실행기의 파일/실행 경로와 자원 확인 단계를 진단하고, 기존 승인 예산 내 재시도 가능 여부를 판단하는 것이다. 운영 배포·DB·정책·worker·기존 데이터·업무 브라우저 검증은 이번 인증 갱신에서 변경/실행하지 않았다. 전체 Gate 완료로 계산하지 않는다.

아래 09-21 제출/인증 만료 기록은 이력이다. 현재 인증 차단과 S3 정리 대기는 해소됐고, 실제 QA 실행기 실패가 남았다.

## 승인 범위와 현재 상태

2026-09-21 사용자가 서울 서버 임시 격리 QA를 승인했다. 대상은 태백 `TAEBAEK-184816` 1공고이며 최대44요청/80MiB, 패키지200MiB 이하, CPU1개, 임시 공간1GiB, 전체20분이다. 운영 서비스·DB·정책·worker 활성화·기존 데이터 적용은 변경하지 않는다.

- [x] 서울/root·저장소 계정·Ubuntu1대·SSM Online을 재확인했다.
- [x] Runtime SSM `0a4780fd-019e-4ef3-addc-15e85f2358c3` Success: 운영787c594/웹 JAR d8696e85…/DB migration 포함 V83/추출기1.0.1/service active·health UP이다. DB를 이번에 직접 조회했다는 뜻은 아니다.
- [x] 사전 자원 확인 SSM `6dfbe65a-e7ae-45e8-97e7-cba8cd5efe70` Success: 메모리1910MiB/가용946MiB, 디스크23337MiB, cgroup v2/systemd249 및 필수 격리 도구가 있다.
- [x] 기존 관측 시험을 실행하는 별도 probe·패키지를 생성하고 단위/계약을 검증했다. 운영 설치물을 대체하지 않는다.
- [x] 비공개 S3와 서울 리전을 확인하고 고유 키에 임시 패키지를 전송했다. 조건부 생성으로 기존 객체 덮어쓰기를 금지한다.
- [x] SSM `35926df3-e555-4c7a-bf95-444a3568d9c9`로 작업을 한 번 제출했다.
- [!] 제출 후 첫 결과 조회에서 AWS 인증 만료(`AWS_AUTH_REFRESH_REQUIRED`)가 발생했다. 서버 실행 결과·실파일 관측·작업 종료·운영 불변 사후 확인은 아직 미확인이다. 조회 실패를 실행 중단으로 간주하거나 재실행하지 않는다.
- [!] 임시 S3 패키지 삭제는 아직 수행하지 못했다. 인증 갱신 뒤 동일 실행의 terminal 상태와 객체 소유 metadata/크기를 확인하고 자기 객체의 정확한 버전을 삭제해야 한다.

## 식별자와 재개 경로

- 임시 실행 ID: `6f2c30cd17a64f9e94507f4b19fcbd38`
- 로컬 실행 계획: `build/temporary-bbs-qa-6f2c30cd17a64f9e94507f4b19fcbd38/plan.json`
- transient unit: `saneb-temp-bbs-qa-6f2c30cd17a64f9e94507f4b19fcbd38`
- 패키지:138파일/188,072,770bytes, SHA256 `29ca394b1a392a9bc878ed6375982ef77dc16672ddd7868c393349402a0acb2e`
- 검증할 application code catalog SHA256: `b53a038435151fba7427f22cc2bd5b4192e92870c5213e36e8cdd85628e24590`
- 대응 로컬 웹 JAR SHA256: `9e10d863d9c83cdc488487c80806d141a76e664296f9b4d7e94a4c558cd15fdc`

관측용 새 시험 클래스는 이 회차의 별도 probe로 제공한다. 위 웹 JAR는 기존 BBS 식별자 수정본이며 운영 설치본 d8696e85…와 다르다. probe의 존재나 패키지 제출만으로 동일 SHA 운영 배포 또는 QA 성공을 주장하지 않는다.

인증 갱신 후 현재 신뢰된 CA와 저장된 작업 계정의 명령 범위 인증을 적용하고 다음 두 작업을 순서대로 수행한다. 인증 토큰·비밀번호·원문은 채팅/로그/문서에 남기지 않는다.

1. `build/qa-tools/Invoke-SanebAwsReadOnly.ps1 -Action TemporaryPoll -TemporaryPlan <위 plan.json 절대 경로>`로 기존 command ID만 조회한다. 실행 중이면 같은 ID를 기다린다.
2. terminal 결과와 원본 정리·unit 종료·운영 JAR 불변·health를 확인하고, `-Action TemporaryCleanup`으로 자기 임시 S3 객체를 삭제한다. 정확한 객체/버전 확인 없이 넓은 prefix를 삭제하지 않는다.

`TemporaryRun`을 다시 호출하지 않는다. 결과 보고서는 원문을 제외한 metadata만 로컬 `result.json`에 저장한다. 인증이 없는 동안 리소스 정리 설정을 정리 완료의 증거로 대체하지 않는다.

## 격리와 검증 경계

새 관측 probe는 기존 `AnnouncementAttachmentBbsOfficialObservationTest`의 JUnit lifecycle과420초 제한을 재사용한다. 제목→정제 본문→실제 전체 첨부→종합 판정 순서이며 혼합 역할·관리자 최종 검증 요구를 유지한다. 기대값을 자동 갱신하지 않는다.

실행 설정은 transient systemd CPUQuota100%/MemoryMax768MiB/MemorySwapMax0/TasksMax128/RuntimeMax1140초, tmpfs1GiB다. 내부 비root launcher는 깨끗한 환경·PID/IPC 격리·읽기 전용 코드 패키지·실행600초 상한을 요구한다. 개별 파일 추출의 별도 네트워크 격리와30초 제한도 유지한다. 서버 실행에서 실제 제한이 적용됐는지는 결과 metadata로 확인해야 한다.

## 로컬 검증

- Java 관측 probe/기존 관측 계약/workflow 계약41건: 실패·생략0,1분12초. 새 probe JAR 생성 성공. 웹 JAR/기존 QA 배포 패키지는 유효한 이전 산출물을 재사용했다.
- 새 launcher Node 검사4건: 실패·생략0.
- 기존 배포/worker launcher와 함께 Node27건:24통과/Windows에서 Linux 전용3건 생략/실패0.
- workflow 변경 후 Java workflow18건:16초 성공/실패·생략0.
- PowerShell dispatcher/operation 및 Python outer/격리 unit 구문 검증 통과. 구문 검증은 실제 서버 자원 제한/파일 추출 성공이 아니다.
- Node·Gradle 로컬 실행은 종료됐다. 사용자 `output/`는 보존했다. 운영 로그인/브라우저는 이번 승인 범위에 포함해 실행하지 않았다.

전체 ATT001~062/Gate0~8을 축소하지 않는다. 기존 태백 기대값 지문 불일치, 전체 Provider/파일 형식/정상 후보 QA, 정책 게시·ENFORCE·기존 데이터 승인 적용 및 운영 업무 E2E는 계속 남는다.
