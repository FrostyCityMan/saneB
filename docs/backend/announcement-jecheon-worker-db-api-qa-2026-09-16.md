# 제천 실제 worker·임시 DB·API 검증

## 범위와 완료 기준

기존 [제천 세 단계 관측](announcement-jecheon-three-stage-observation-2026-09-16.md)은 본문·HWPX 추출까지의 증거다. 이번에는 같은 고정 3공고를 production worker, 직접 소유한 임시 PostgreSQL, Service/Mapper, MockMvc API까지 연결한다. 전체 ATT-001~062·Gate0~8을 유지하며 운영·브라우저 검증으로 대체하지 않는다.

| 고정 공고 | 제목 Gate | 이번 실행 대상 |
|---|---|---|
| JECHEON-403587 | 조합 미충족 | 원문 저장·본문·첨부 요청 0 |
| JECHEON-403530 | 조합 충족 | 본문 → HWPX 1개 → worker → DB → API |
| JECHEON-403490 | 조합 충족 | 본문 → HWPX 2개 모두 → worker → DB → API |

음성 표본을 분모에서 빼지 않는다. 다중 첨부 공고의 실제 추출 호출 수를 2회로 고정하고, 기존 양평의 미지원 이미지 발견 분모와 추출 호출 수를 구분한다. 모든 파일의 다운로드·품질·역할·텍스트·블록을 실제 추출 결과와 DB/API에서 대조하며 잘못된 source의 근거 조회 404, 자동 확인·공고 연결 0, 원본·lease 정리를 검증한다.

혼합 역할 UNKNOWN, 부분 추출, 본문 A/B 의무는 그대로다. 사전 관측의 제천 파일 3개가 COMPLETE_TEXT/UNKNOWN이었다는 사실은 새 실행의 성공을 보장하지 않는다. 기술적 완전성과 정상 후보·관리자 최종 확인을 구분하며 catalog 기대값을 자동 등록·승인하지 않는다.

## 실행 경계

- 전용 `attachmentJecheonWorkerIntegrationTest`, `reports/attachment-jecheon-worker`, 별도 JUnit 경로를 사용한다. 일반 `test`와 push는 외부 수집을 켜지 않는다.
- GitHub workflow의 `verify-jecheon-worker=true` 수동 입력으로만 실제 파일 실행한다. 같은 SHA의 전체 Linux 계약 단계를 먼저 통과해야 한다. 운영 인증정보·DB·배포·정책 게시·ENFORCE·기존 데이터 적용을 사용하지 않는다.
- 공고당 420초·44요청·80MiB, 파일 20MiB·전체 10개, JVM 384MiB·단일 fork를 유지한다. 전체 계획 상한은 132요청·240MiB이며 제목 제외 공고는 실제 요청 0이어야 한다.
- 기존 SSRF/DNS/TLS/redirect·Linux 격리와 코드/추출기 지문을 유지한다. Docker 장애나 AWS 인증 만료를 보안 제한 해제로 우회하지 않는다.
- 보고서 판정기는 현재 실행 시각, 정확한 3공고·전체 3파일, 실제 추출 호출, 원본/lease 정리, DB/API 검사 완료를 요구한다. 부분 추출은 TECHNICAL_EXCEPTION/REVIEW_REQUIRED, 완전 추출의 UNKNOWN은 FINAL_REVIEW_EXCEPTION/REVIEW_REQUIRED여야 한다.
- 업로드는 고정 metadata JSON과 JUnit만 포함한다. 원문·첨부·추출 텍스트·DB 파일·인증정보는 보관하지 않는다.

## 검증 기록

- [x] Node 보고서 판정기와 기존 launcher 검사 32건 통과, 실패·생략 0.
- [x] Java 표적 49건 통과, 실패·생략 0, 1분45초 성공. 양평·충주·제천의 실제 임시 PostgreSQL 준비 3건과 probe 패키징을 포함한다. 합성 본문이며 공식 HTTP/파일 추출 증거는 아니다.
- [x] 선행 `85c7f65`의 [Linux35050057694](https://github.com/FrostyCityMan/saneB/actions/runs/35050057694)는 2026-09-16 12:13 KST success다. 보관 XML root2713=2447통과/266조건부 생략/실패0, 별도 추출기88·패키징20·job192·migration17·부모2·runtime1·worker12·Flyway3 모두 실패·생략0을 확인했다. 이번 제천 연결 전 SHA이며 공식 외부 요청 없이 실행한 계약 검증이다.
- [x] 수정본 전체 로컬 회귀 4분43초 성공: root2717=2452통과/265조건부 생략/실패0, 패키징20/20·Node91/91. 추출기88·bootJar·probe는 유효한 선행 결과 재사용(UP-TO-DATE)이며 새 실행으로 합산하지 않는다. Java/임시 PostgreSQL 잔여0, 직접 실행한 단발 Node 종료를 확인했다.
- [x] 웹 JAR SHA256 `c2680a76f97a1e891dc0aa3b83f876c8ba350a2ccdc7346eb12bae2292d80632` 불변, probe JAR `ca5e01db3e2652e5e71d91d1a29af804b93176ac7f17cb8878a09e0512aef6b0` 생성. production Java/추출기/migration 변경0, diff·구문·제한 자격증명 패턴 검사 통과. 사용자 `output/`는 보존한다.
- [ ] 새 SHA의 Linux 전체 계약 및 제천 실제 worker·DB/API 실행.
- [ ] 정책 QA·운영 적용·인증된 관리자 업무 E2E. 이 증분의 성공으로 전체 Gate를 통과 처리하지 않는다.

```powershell
node --test scripts/qa/attachment-jecheon-worker-report.test.mjs scripts/qa/attachment-official-worker-probe.test.mjs
.\gradlew.bat :test --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' --tests '*AnnouncementAttachmentOfficialWorkerPreparationTest' --tests '*AnnouncementAttachmentBbsOfficialObservationContractTest' --tests '*AttachmentContractWorkflowTest' attachmentOfficialWorkerProbeJar --no-daemon --console=plain --max-workers=1
.\gradlew.bat :test :attachment-extractor:test attachmentContractQaTest bootJar attachmentOfficialWorkerProbeJar --no-daemon --console=plain --max-workers=1
node --test scripts/qa/attachment-official-worker-probe.test.mjs scripts/qa/attachment-jecheon-worker-report.test.mjs scripts/qa/attachment-jecheon-observation-report.test.mjs scripts/qa/attachment-chungju-observation-report.test.mjs scripts/qa/attachment-contract-report.test.mjs
```

Linux 실제 실행 명령:

```bash
bash ./gradlew attachmentJecheonWorkerIntegrationTest --no-daemon --console=plain --max-workers=1
```

새 엔진·모델을 추가한 것이 아니다. 엔진 7·첨부 모델 19·전용 본문 18·형식 3, catalog 30참조/실행 기대값 1/정상 공고 0과 모든 대상의 잔여 검증 범위는 유지한다.
