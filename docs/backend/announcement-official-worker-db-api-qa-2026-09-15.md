# 공식 파일 → worker → 임시 DB → API 검증 연결

## 현재 단계 / Gate

P3 진행, 전체 Gate0~8 / ATT-001~062는 **Not ready**다. 변경된 `제목 1차 → 정제 본문 2차 → 실제 첨부 텍스트 3차 → 관리자 최종 검증` 순서를 보존한다. 이번 변경은 시험·실행 설정·기록이며 production Java/API/UI/schema를 변경하지 않는다. 기존 공식 파일 관측과 별도로 실제 worker 저장·조회 경로를 검증하도록 확장했다.

## 현재 외부 차단 근거

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
| 신규 공식 worker | 실제 BODY/파일 → worker/DB/API | 구현·컴파일 확인, 실제 실행 미완료 |
| 정책 QA 기대값 | 전체 대상/profile/형식/다중 파일의 사전 승인 기대값 비교 | 미완료, catalog 기대값0 |
| 운영 적용/E2E | 동일 SHA 배포·승인 범위 데이터·관리자 브라우저 | 미완료 |

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
