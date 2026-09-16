# 충주 고정 공고의 worker·임시 DB·API 검증 경로

## 단계 / 범위

전체 ATT-001~062·Gate0~8과 제목→정제 본문→실제 첨부 텍스트→관리자 최종 검증을 유지한다. [충주 모델](announcement-chungju-eminwon-profile-2026-09-16.md)의 공개 고정3건을 기존 실제 worker 시험에 연결하는 증분이다. 새 사이트 모델, production Java, migration, API/UI, 운영 정책·데이터 변경은 없다. 엔진7/첨부 모델19/전용 BODY18/추출 형식3, catalog30참조/실행 기대값1/정상0을 그대로 둔다.

## 실행 계약

| 표본 | 고정 제목 단계 | 실제 검증 경로 |
|---|---|---|
| CHUNGJU-72625 | COMBINATION_NOT_MATCHED | 원문 저장0·본문/첨부 요청0·원본/lease 정리 |
| CHUNGJU-72039 | COMBINATION_NOT_MATCHED | 원문 저장0·본문/첨부 요청0·원본/lease 정리 |
| CHUNGJU-70852 | COMBINATION_MATCHED | 정제 본문→base 판정 저장→job 예약→실제 worker→HWP1개 격리 추출→임시 DB→API/근거 대조 |

- 규칙은 시험이 직접 소유하는 loopback 임시 PostgreSQL의 fixture다. 운영 DB 접속 정보를 받지 않고 정책 게시·ENFORCE·기존 데이터 적용을 수행하지 않는다. 이 fixture의 실행용 ACTIVE는 운영 규칙 활성화 증거가 아니다.
- worker는 production 발견 profile, gateway, 파일 검증기, Linux 격리 추출기, DB/서비스/Mapper를 사용한다. API 대조는 MockMvc이며 인증된 실제 브라우저 검증이 아니다. 본문 A/B·부족·실패를 저장한 뒤 가능한 첨부 분석까지 계속하지만, 본문 실패를 전체 성공으로 바꾸지 않는다.
- 고정 음성의 중단 사유가 바뀌거나 양성이 제목 제외로 바뀌면 외부 요청 전에 실패한다. 기존 JUnit이 EXCLUDED이면 종료하던 경로를 보강했으며, 기존 probe의 표본별 기대 상태 검사도 유지한다. 제목 제외를 원문 복원·파일 검증을 위한 통과로 바꾸지 않는다.
- 본문·첨부 전체 파일 수, 실제 추출 호출 수, DB의 텍스트/품질/역할/블록, API의 같은 값, 다른 원문 근거404, 자동 confirmation/link0, 원본·lease 정리를 검증한다. PARTIAL_TEXT/UNKNOWN은 불완전/검수 상태를 보존하고 정상 공고·전체 정책 QA로 승격하지 않는다.
- 한 공고420초/44요청 예약/80MiB, 파일20MiB, 단일 fork/384MiB JVM과 기존 shell 상한을 유지한다. 그룹 전체 상한132요청/240MiB이며 현재 고정 음성2건은 예약0이어야 한다. host/path/DNS/TLS/redirect 검증을 완화하지 않고 최초 요청과의 소속도 대조한다.

## 독립 실행·패키징

- 기존 `run-attachment-official-worker-probe.sh`와 Java probe에 명시적 `CHUNGJU` 그룹만 추가했다. 양평 기본값·태백 두 그룹과 원래 분모는 보존하고 `ALL`, 임의 URL/공고번호는 허용하지 않는다.
- 충주 표본 정의를 독립 probe에 포함되는 관측 클래스로 이동했다. 로컬 live QA가 그 정의를 읽으며, probe에는 없는 `ChungjuEminwonProfileLiveQaTest`를 관측 클래스에서 역참조하지 않는다. 해당 클래스를 사용할 수 없는 별도 classloader에서도3표본 로딩을 검증한다.
- probe JAR에는 시험 class만 포함한다. production 코드·설정·인증정보·JUnit 의존성을 복제하지 않는다. 실행기는 비root·깨끗한 환경·PID 격리·JAR/설치 코드 지문을 확인하고 실제 설치된 같은 코드/추출기를 사용해야 한다. 마지막 운영 설치787c594/추출기1.0.1에 이 새 probe를 지문 검사 우회로 실행하지 않는다.
- 새 `attachmentChungjuWorkerIntegrationTest`는 별도 명시 실행 task와 `attachment-chungju-worker` 보고서를 사용한다. 일반 test/push에서 외부 요청을 켜지 않으며 기존 GitHub workflow에 충주 worker 자동 실행을 추가하지 않았다. 알려진 CI 접속 실패를 무조건 재시도하지 않는다.

## 실행 환경과 검증 결과

- [!] 이전 `174230a`의 Linux35047528916은 계약 검증 성공과 별도로 충주 BODY/DETAIL 시간 초과로 전체 failure다. 충주 파일 추출은 실행되지 않았다.
- [!] 로컬 Docker가 정지 상태임을 확인한 뒤 `docker desktop start --timeout 45`를1회 요청했다. 기동 로그에서 파일 접근 불가·경로 형식 오류가 다시 관측됐고 정상 engine 응답을 얻지 못했다. 종료되지 않은 기동/조회 CLI는 취소하고 `docker desktop stop --timeout 30` 후 이번에 시작한6개 프로세스가 모두 없어졌으며 서비스 Stopped를 확인했다. 초기화/재설치/설정 변경/파일 삭제/비격리 추출은 없다. 상세 원인은 아직 미확정이다.
- [x] 표적 Java45건/실패·생략0, 실제 임시 PostgreSQL 준비2건을 포함해1분32초 성공했다. 양평과 충주의 실제 seed 제목·A/B·조합 중단 계약, 합성 본문 판정 저장→현재 버전 job 예약, 독립 classloader 표본 로딩을 검증했다. 실제 HTTP/추출 성공이 아니다.
- [x] Node/Bash 검사6건/실패·생략0과 probe JAR 생성이 완료됐다.
- [x] 첫 전체 회귀는 실행 handle 소실·JVM0·새 전체 XML 부재로 완료를 입증하지 못했다. 중복 실행이 없음을 확인한 후 새 전체 회귀4분42초 성공을 확인했다. root2713=2448통과/265조건부 생략/실패0, 패키징20/20·Node65/65다. 추출기88·bootJar·probe JAR는 유효한 선행 결과 재사용(UP-TO-DATE)이며 새 실행으로 합산하지 않는다.
- [x] 웹 JAR SHA256은c2680a76f97a1e891dc0aa3b83f876c8ba350a2ccdc7346eb12bae2292d80632로 불변이고 probe JAR는1f97172549d714e0ed2dabf18a0aa3657ceb76de1a947ca7fb7f51f06b8577aa다. JAR 항목에서 시험 class만 포함됨을 확인했다. 이 로컬 지문은 운영 설치 증거가 아니다.
- [ ] 새 코드의 실제 Linux 충주 worker·파일·DB/API 관측, 운영 인증/동일 버전 설치, 정책 QA, 승인된 운영 적용·브라우저 E2E는 미완료다. 이 경로 구현만으로 Gate를 통과 처리하지 않는다.

표적 검증:

```powershell
.\gradlew.bat :test --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' --tests '*AnnouncementAttachmentOfficialWorkerPreparationTest' --tests '*AnnouncementAttachmentBbsOfficialObservationContractTest' --tests '*AttachmentContractWorkflowTest' attachmentOfficialWorkerProbeJar --no-daemon --console=plain --max-workers=1
node --test scripts/qa/attachment-official-worker-probe.test.mjs
```

접근 가능한 Linux에서 같은 코드/추출기 준비 후 별도로 실행할 명령(이번 증분에서는 미실행):

```bash
bash ./gradlew attachmentChungjuWorkerIntegrationTest --no-daemon --console=plain --max-workers=1
```

운영 서버를 사용하는 경우 먼저 재인증·현재 설치 지문·승인 범위를 확인한다. 임시 QA 실행과 운영 worker 활성화·정책 게시·기존 데이터 적용 승인은 서로 다른 작업이다.
