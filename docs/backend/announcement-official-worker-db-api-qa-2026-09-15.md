# 공식 파일 → worker → 임시 DB → API 검증 연결

## 현재 단계 / Gate

P3 진행, 전체 Gate0~8 / ATT-001~062는 **Not ready**다. 변경된 `제목 1차 → 정제 본문 2차 → 실제 첨부 텍스트 3차 → 관리자 최종 검증` 순서를 보존한다. 이번 변경은 시험·실행 설정·기록이며 production Java/API/UI/schema를 변경하지 않는다. 기존 공식 파일 관측과 별도로 실제 worker 저장·조회 경로를 검증하도록 확장했다.

## 2026-09-15 후속 — 설치 runtime의 고정 공식 시험

최종 SSM **`31cf27d7-cf0c-49f7-badd-10ffc1eeefcb` Success**, 고정3건 **3통과/실패·생략·중단·container 실패0**이다. 웹 JAR SHA256 `16a1bb74a13e7d88c180fe6f3eb98e97c105c8362592f9c96de572890523f437`, 별도 probe SHA256 `d41a1de17eac91dd9e8aae04fd17bc61ce6358d5800db44ec83bf7cd59e9e01f`, 전체 업무 코드 지문 `ad55e33ab0a04fae4cf76e607ce5bb0db39b1406e3452ba0579ae4b3be35b2bb`다.

| 고정 공고 | 실제 처리 | 최종 의미 |
|---|---|---|
| YANGPYEONG-312241 | BODY AVAILABLE/1시도, HWPX1개 발견·처리/116,740bytes/11,398자, 추출 텍스트 DB·API 일치 | PARTIAL_TEXT/UNKNOWN, TECHNICAL_EXCEPTION·REVIEW_REQUIRED/ATTACHMENT_INCOMPLETE. 정상 완료 후보 아님 |
| YANGPYEONG-311846 | BODY AVAILABLE/1시도, 전체2파일 처리: PDF237,822bytes/4,195자 + JPG미지원 BLOCKED, DB·API 일치 | PARTIAL_TEXT/UNKNOWN 및 미지원 포함. TECHNICAL_EXCEPTION·REVIEW_REQUIRED 유지 |
| YANGPYEONG-311507 | TITLE_COMBINATION_NOT_MATCHED, 원문/작업 저장 전 종료, BODY·첨부 요청0 | 제외 표본이며 파일 추출 성공으로 계산하지 않음 |

지원 파일2건의 실제 추출 결과↔DB 텍스트↔API 전체 필드·블록·종합 상태, 잘못된 원문404/no-store, 자동 confirmation/link0을 확인했다. 두 양성 경로의 요청 예약 상한 사용은 각각4회/2,533,380bytes·4회/2,654,462bytes다. 운영 DB 쓰기0, lease0, 원본/소유 임시 PostgreSQL namespace/전송 패키지 정리 성공, 설치 JAR 불변이다. **정책 QA 기대값 승인·완전 텍스트·전체 Provider·관리자 인증 E2E 통과가 아니다.**

- 운영 V83/SHA476c8f7 배포와 합성221건 서버 격리 시험은 성공했다. 공식 사이트 접속과 전체 파일 worker 처리는 별도 확인한다.
- `attachmentOfficialWorkerProbeJar`는 기존 공식 worker 시험·고정 표본 helper·JUnit launcher만 별도 JAR에 담는다. 웹 JAR와 이미 설치된 불변 합성 QA 패키지를 변경하거나 시험용 라이브러리를 운영 classpath에 추가하지 않는다.
- `run-attachment-official-worker-probe.sh`는 비root, private PID/IPC/UTS namespace, 깨끗한 환경, 소유 임시 작업 경로를 사용한다. 공개 사이트 접속 때문에 이 전용 probe의 네트워크만 공유하며 기존 합성 QA의 `--unshare-all`과 실제 파일 추출기의 별도 네트워크 격리는 그대로다. 운영 home/env/socket은 mount하지 않는다. 시스템의 공개 TLS trust store만 읽기 전용으로 사용한다.
- 기존 JUnit `BeforeAll/BeforeEach/TempDir/AfterAll` 및 사례별420초 timeout을 그대로 실행한다. 전체3건·생략0·실패0·container 실패0 및 고정3개 metadata 보고서가 모두 있어야 성공이다. TITLE 제외1건을 실제 첨부 추출 성공으로 세지 않는다. 공개 텍스트/URL/예외 메시지는 반환하지 않는다.
- 첫 서버 실행 `fe281ad4-0649-4aff-90a9-758521b8880f`는3건 중 제목 제외1건 통과/본문 진입2건 Java 초기화 실패다. 성공으로 표시하지 않으며 원인 예외 자료형/호출 위치로 조사한다. 임시 원본·lease·전송 산출물 정리 및 설치 JAR 불변을 확인했다. 운영 DB 쓰기/정책 게시/ENFORCE는0이다.
- 두 번째 `57ec7a23-2af5-4e9f-9371-16c8557dcbf6`는 원인을 `javax.crypto.JceSecurity.setupJurisdictionPolicies`의 SecurityException으로 특정했다. 디렉터리만 읽기 전용 연결한 세 번째 `3dc2e7de-85a8-4753-97b1-338156fc4b68`도 같은 실패라 반복 실행을 중단했다.
- 고정 runtime 읽기 전용 SSM `44bf41c5-77a8-4947-bd1e-7275f9d0252a`에서 JDK 정책 디렉터리는 실제 디렉터리이고5개 `.policy` 파일이 각각 `/etc/java-21-openjdk/security/policy/...`로 향하는 symlink임을 확인했다. 이5개 설치 파일의 실제 대상을 읽기 전용 mount하고 DB 생성 전 JCE/TLS 초기화를 검사하도록 수정했다. TLS 검증/crypto.policy/운영 JDK 파일/기존 합성 QA 격리를 변경하지 않았다.
- 네 번째 `06d34e92-8986-4863-b452-4d02abd2e999`에서는 JCE 문제가 해결되고 BODY2건 AVAILABLE/각1시도, HWPX116,740bytes/11,398자 및 PDF237,822bytes/4,195자 실제 추출·worker EVALUATED/DB 저장까지 진행했다. 두 파일은 PARTIAL_TEXT이며 API 비교에서 실패해 성공으로 표시하지 않았다. 원본/lease/전송 자원 정리·설치 JAR 불변이다.
- 후속 로컬 회귀로 DTO의 LongNode와 HTTP JSON의 IntNode 직접 비교가 같은 값에도 false인 것을 재현했다. 기대값도 wire serialization 후 비교하도록 수정하고 standalone MVC의 날짜 직렬화를 동일 ISO 설정으로 맞췄다. 필드 누락/숫자 변경은 여전히 실패이며 모든 필드·배열 순서를 유지한다. 표적5건은38초 통과했다. 이 수정은 production JSON/API 변경이 아니다.
- 로컬 최초 컴파일은 JUnit launcher compile classpath 누락으로 실패했고 기존 runtime launcher를 testCompileOnly로 연결한 뒤 표적3건과 JAR 생성이59초 성공했다. Node/Bash14건 통과·기존 Linux 전용2건 생략, 실패0이다. 전체 회귀와 후속 실파일 결과는 완료 후 기록한다.

재현은 `./gradlew attachmentOfficialWorkerProbeJar`로 별도 시험 JAR를 만든 다음, 승인된 비root Linux 환경에서 다음 launcher를 실행한다. 설치 웹 JAR 내부 `attachment-qa-code/catalog.json` SHA256을 코드 지문으로 사용한다. 임의 업무 코드/DB 접속값/URL을 주입하지 않는다.

```bash
bash scripts/qa/run-attachment-official-worker-probe.sh "$QA_DISTRIBUTION" "$PROBE_JAR" "$PROBE_SHA256" "$APPLICATION_CODE_HASH"
```

## 과거 CI 외부 차단 근거

- [Linux 실행36](https://github.com/FrostyCityMan/saneB/actions/runs/34912804386), SHA `0f34f9eeaf805ac604ca4218ee89c68a6caee159`는 공식 관측 단계 실패다. 기본 계약·독립 DB·부모 연결 시험은 성공했지만 전체 workflow는 failure다.
- 양평312241/311846: BODY 각각2시도 TIMEOUT, 이어서 DETAIL_DISCOVERY/TRANSPORT_TIMEOUT. 파일 다운로드·텍스트 추출은 미실행이다. 각각 요청 예약3회, 임시 원본 정리true다.
- 양평311507: TITLE_COMBINATION_NOT_MATCHED, TITLE_EXCLUDED_NOT_FETCHED, BODY/첨부 요청 예약0회다. 제외 표본을 성공 표본으로 바꾸지 않았다.
- 앞선 태백 실행30/31의 시간 초과와 합쳐 CI에서 공식 게시판 접근 실패가 반복됐다. 서버 차단·리전·네트워크 원인은 확정하지 않았다. 같은 접근 조건에서 다른 실행기를 재시도해도 성공 근거가 되지 않으므로 자동 재실행하지 않는다.
- Windows에서 과거 공식 BODY/다운로드가 성공한 사실은 해당 시점·클라이언트의 증거다. Linux 추출 또는 실제 worker 성공으로 대체하지 않는다. 현재 WSL에는 docker-desktop만 있으며 별도 개발 Linux 배포판은 없다. Docker engine pipe는 앞선 확인에서 없었다. 시스템 재설치·보안 완화·운영 환경 변경은 하지 않았다.
- 원시 파일/본문을 GitHub artifact로 옮겨 실패를 우회하지 않는다. 운영 AWS 재인증 응답과 승인된 실행 환경의 접근 상태 확인이 남아 있다.

## 구현한 검증 경로

1. `AnnouncementAttachmentOfficialWorkerIntegrationTest`는 기존 BBS 관측의 고정 양평3표본을 재사용한다. 임의 URL/기관 또는 ALL 입력을 제공하지 않는다. TITLE 제외 표본도 분모에 남기고 DB 원문·작업 예약 전 종료한다.
2. 최신 Flyway 전체를 적용한 **직접 소유한 loopback 임시 PostgreSQL**만 사용한다. 외부 DB 접속 정보를 받지 않는다. 실행용 ACTIVE 규칙·ENFORCE 정책 fixture는 이 임시 DB에만 생성하고 정책 QA/운영 게시 성공으로 보고하지 않는다.
3. TITLE은 현재 seed 규칙, BODY는 실제 `LocalGovernmentNoticeProviderContentClient`와 공통 분류기를 사용한다. BODY A/B·부족·실패 결과로 첨부를 중단하지 않는다. 실제 BODY/evaluation은 `AnnouncementSourceClassificationPersistenceServiceImpl`의 transaction/DAO/Mapper를 통해 저장한다.
4. 저장 후 현재 baseEvaluation/sourceVersion/attachmentVersion을 다시 읽어 job을 예약한다. 실제 `AnnouncementAttachmentWorkerServiceImpl`, download gateway·lease/예산, 기관 profile, signature 검사, Linux 격리 추출기, `document-role-1.0.2`, 평가 저장 경로를 사용한다. HTTP/추출 응답을 합성 파일로 교체하지 않는다.
5. 모든 첨부의 DB file/quality/role/extraction 연결과 추출 텍스트 일치, 역할 block hash, 실제 Controller/Service/DB의 JSON·블록 조회·다른 원문 접근404·no-store를 검사한다. standalone MockMvc이므로 인증/CSRF/브라우저 E2E는 아니다.
6. JPG 미지원 파일은 `BLOCKED/UNSUPPORTED_FORMAT`으로 전체 목록에 남는다. 임의 다운로드 차단을 미지원 성공으로 처리하지 않는다. 지원 파일의 추출 누락, BODY 실패, 전체 발견 수 변경은 실패다. 부분/OCR/미지원/UNKNOWN을 정상 후보로 승격하지 않는다.
7. 최종 검수/공고 link가 자동 생성되지 않았는지 검사한다. 관리자 최종 확인 필요를 유지하며 실제 DRAFT 생성·운영 사용자 동작을 이 시험으로 대체하지 않는다.
8. 공고당44요청/80MiB, 3건 전체132요청/240MiB 상한이다. BODY2요청/2MiB 상한을 먼저 예약한다. worker 자체 예산·DB transaction 밖 HTTP/추출·자원 lease를 우회하지 않는다.
9. 실제 텍스트/파일명/URL/HTTP 헤더/예외 원문을 로그·JUnit/artifact로 내보내지 않는다. opt-in JVM은 기존 logback OFF 설정을 사용하고 실패는 단계와 고정 코드로 보고한다. DB/API 텍스트 비교는 boolean assertion으로 확인한다. metadata만 남기고 임시 원본·lease·소유 DB·HTTP 자원을 정리한다.

## 검증 종류와 완료 기준

| 검증 | 의미 | 현재 상태 |
|---|---|---|
| 준비 계약 | 합성 BODY의 실제 분류 저장 → 현재 버전 job 예약 | 통과, 공개 파일 성공 아님 |
| 기존 worker 통합 | 합성 HTTP/PDF/HWP/HWPX → 실제 격리 추출/DB/API/검수/DRAFT | 기존 Linux12건 통과, 공개 파일 성공 아님 |
| 신규 공식 worker | 실제 BODY/파일 → worker/DB/API | 고정 양평3건 서버 통과. 지원 파일2건은 부분 텍스트·미완료 상태 보존 |
| 정책 QA 기대값 | 전체 대상/profile/형식/다중 파일의 사전 승인 기대값 비교 | 미완료, catalog 기대값0 |
| 운영 적용/E2E | 동일 SHA 배포·승인 범위 데이터·관리자 브라우저 | V83 설치·읽기 전용 관리자 화면 확인. 활성화/배치 적용·업무 E2E 미완료 |

`WORKER_DB_API_OBSERVED_NOT_APPROVED`는 고정 표본의 실행·저장·조회 연결을 관측했다는 의미다. `isWholeTextAnalysisComplete`는 BODY/모든 첨부 텍스트의 완전성만 나타낸다. 정책 QA/기대값 승인/전체 정상 후보/최종 검수 성공은 별도다. `TITLE_EXCLUDED_NOT_FETCHED`를 첨부 추출 성공으로 계산하지 않는다.

## 실행

2026-09-15 최종 로컬 검증은16분3초 성공이다. root2631=2369통과/262조건부 생략/실패0, 별도 실제 job192·패키지20·원래 Flyway3은 실패/생략0이다. 신규 준비1건·workflow10건도 root에서 통과했다. Node20통과/2 Linux 전용 생략이다. bootJar/추출기는 UP-TO-DATE이며 production JAR SHA256은 `19b1d2fb08bde9e29a9523e41e2a8352a3ad8e68bbf5e98b0a55826867f90d40`다. 사용한 단발 Node/JVM/임시 PG가 종료되었다. 공식 worker 자체 실행과 현재 SHA Linux/운영 검증의 성공 근거가 아니다.

일반 `test`는 공식 worker 환경값을 false로 고정한다. 합성 BODY 준비 시험은 실제 임시 PostgreSQL을 사용하고 외부 HTTP/파일 추출을 하지 않는다.

```powershell
.\gradlew.bat :test --tests '*AnnouncementAttachmentOfficialWorkerPreparationTest' --tests '*AnnouncementAttachmentBbsOfficialObservationContractTest' --tests '*AttachmentContractWorkflowTest' --no-daemon --console=plain --max-workers=1
.\gradlew.bat :test :attachment-extractor:test attachmentContractQaTest attachmentJobIntegrationTest flywayIntegrationTest -PsanebFlywayEphemeral=true bootJar installAttachmentContractQa --no-daemon --console=plain --max-workers=1
node --test scripts/qa/attachment-contract-report.test.mjs scripts/qa/attachment-contract-release.test.mjs
```

공식 사이트 접근 상태가 바뀌거나 승인된 Linux 실행 경로가 확보된 뒤에만 아래를 실행한다. CI 입력 `verify-official-worker`는 기본false/수동 dispatch만 허용하며 push 표식은 제공하지 않는다. artifact에는 고정3 metadata JSON과 해당 JUnit만7일 보관한다.

```bash
bash ./gradlew attachmentOfficialWorkerIntegrationTest --no-daemon --console=plain --max-workers=1
```

기존 migration V1~V83, v1 API, 운영 정책·데이터·배포를 변경하지 않았다. 엔진6/profile17/전용 BODY16기관/형식3, catalog 참조24/기대값0과 미연결 기관 분모를 유지한다. `output/` 사용자 Word2개를 보존한다.
