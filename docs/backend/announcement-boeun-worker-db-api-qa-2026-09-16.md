# 보은 실제 worker·임시 DB·API 검증

## 범위와 성공·실패 기준

기존 [보은 본문·첨부 모델](announcement-boeun-bbs-profile-2026-09-15.md)의 다운로드·signature 검증을 실제 텍스트 추출→worker→임시 PostgreSQL→API까지 확장한다. 전체 ATT-001~062·Gate0~8, 제목→본문→첨부→관리자 최종 검증 순서를 유지한다. 운영 데이터·정책·키워드·과거 migration은 변경하지 않는다.

2026-09-16 공식 상세 HTML 3건을 HTTP200·제목 표식 각1개로 다시 확인했다. 이 제목 확인은 모델 조사이며 생산 수집 성공이 아니다. 아래 실제 검증은 고정 제목을 현재 임시 DRAFT 규칙으로 먼저 판정하고 통과한 경우만 본문·파일을 요청한다. 사전 확인한 양성의 제목이 바뀌거나 제외되면 성공으로 분모를 줄이지 않고 실패한다.

| 고정 공고 | 실제 제목의 지원 내용 | 확인 대상 |
|---|---|---|
| BOEUN-221499 | 청년 월세·주거비 지원 | 본문 → HWPX1 → worker/DB/API |
| BOEUN-221497 | 청년 소상공인 점포 임차료 지원 | 본문 → HWPX1 → worker/DB/API |
| BOEUN-218812 | 소상공인 출산 지원 | 본문 → PDF1 → worker/DB/API |

- 성공: 고정3공고·전체3파일·형식·실제 추출 호출 수, 저장 텍스트·블록·역할 지문·종합 판정과 API 일치, 원본/lease 정리, 운영 쓰기0을 증명한다.
- 실패: 제목 변경·본문 실패·발견 불완전·누락/중복 파일·형식 변경·worker/DB/API 불일치·오래된 보고서·정리 실패·한도 초과를 성공으로 처리하지 않는다.
- 부분 추출/UNKNOWN을 정상 후보로 바꾸지 않는다. 완전성은 기술 상태이며 최종 관리자 검증은 별도다. 완전한 NOTICE/GUIDE 근거가 실제로 확인되어 정상 후보가 되더라도 정책 QA/게시·운영 E2E 성공은 아니다.

## 구현과 실행 경계

- 기존 고정 표본 정의와 probe 그룹에 BOEUN만 추가한다. URL·기관·목록 parser identity는 현재 catalog의 REFERENCE_ONLY3건과 대조하며 기대값을 자동 작성하지 않는다.
- 전용 `attachmentBoeunWorkerIntegrationTest`/보고서 경로를 사용한다. 일반 `test`·push는 공식 사이트에 접속하지 않는다. 수동 workflow의 `verify-boeun-worker=true`만 실제 외부 실행을 허용한다.
- 최대132요청/240MiB(공고당44요청/80MiB·420초), 파일20MiB/전체10개, JVM384MiB·단일 fork를 유지한다. 조사용 HTML3요청은 실제 worker 예산/보고서와 별도다.
- 기존 DNS/IP 고정·SSRF·TLS·redirect·다운로드 allowlist·비root Linux 추출 격리를 재사용한다. Docker/AWS 인증 장애를 보안 완화나 Windows 비격리 파싱으로 우회하지 않는다.
- 보은 판정기는 고정3공고의 HWPX2/PDF1을 구분한다. 제천과 공통인 worker 근거 검증을 재사용하되 음성/양성 수는 각 고정 범위에서 계산한다. 제천 음성1/양성2·3파일 계약은 유지한다.
- artifact는 비식별 JSON3개와 JUnit만 보관한다. 실제 파일·추출 원문·DB 파일·운영 인증정보는 보관하지 않는다.

## 검증 체크리스트

- [x] 공식 HTML 제목3건을 확인했다. 다운로드/추출/운영 성공으로 계산하지 않는다.
- [x] 보은 포함 실제 임시 DB 준비4건·probe18건은 실패·생략0,1분8초 성공이다. 합성 본문이며 공식 HTTP·추출 성공 증거가 아니다.
- [x] 고정 catalog/제목 규칙·실제 임시 DB 준비·probe·workflow의 Java53건 실패·생략0,2분 성공이다. Node 판정기/launcher59건 및 구문 검증도 실패·생략0이다. 변경한 공통 판정기로 제천35051444985의 실제 metadata를 다시 검증해 음성1/양성2/파일3 경계가 유지됨을 확인했다.
- [x] 패키징20건 실패·생략0,23초 성공이다. 추출기88건·bootJar·probe는 유효한 선행 결과 재사용(UP-TO-DATE)으로 새 실행에 합산하지 않는다. 웹 JAR `c2680a76…`는 불변이며 probe만 새 표본 정의를 포함한다. 이번 전체 로컬 root 회귀는 반복하지 않고 후속 같은 SHA의 Linux 전체 계약으로 검증한다.
- [ ] 같은 SHA Linux 전체 계약과 보은 실제 파일3개→worker/DB/API를 검증한다.
- [ ] 역할·텍스트 기대값 검토, 정상3공고·다중첨부 표본, 정책 QA·전체 수집처·운영·브라우저 Gate를 완료한다.

```powershell
node --test scripts/qa/attachment-boeun-worker-report.test.mjs scripts/qa/attachment-jecheon-worker-report.test.mjs scripts/qa/attachment-official-worker-probe.test.mjs
.\gradlew.bat :test --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' --tests '*AnnouncementAttachmentOfficialWorkerPreparationTest' --tests '*AnnouncementAttachmentBbsOfficialObservationContractTest' --tests '*AttachmentContractWorkflowTest' attachmentOfficialWorkerProbeJar --no-daemon --console=plain --max-workers=1
```

명시적 Linux 실제 실행은 `bash ./gradlew attachmentBoeunWorkerIntegrationTest --no-daemon --console=plain --max-workers=1`이다. 이 문서는 실행 결과가 없는 단계의 성공을 선기록하지 않는다. 새 엔진/모델은 아니므로 엔진7·첨부 모델19·전용 본문18·형식3, catalog30참조/실행 기대값1/정상0을 유지한다.
