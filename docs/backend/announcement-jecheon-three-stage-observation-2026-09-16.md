# 제천 고정 표본의 제목·본문·첨부 세 단계 관측

## 목표 / 범위

전체 ATT-001~062와 Gate0~8의 실파일 근거를 확장한다. 기존 제천 모델의 발견·signature 검증을 실제 Linux 추출·역할·종합 판정까지 연결한다. 엔진6/첨부 모델18/전용 본문17/형식3과 운영 전체 대상 분모를 유지한다. 이번 변경은 시험·CI·보고서 검증기이며 production Java, DB migration, v1/v2 API, UI, 운영 데이터와 정책을 바꾸지 않는다.

## 고정 표본과 제목 선행 판정

기존 catalog의 제천 참조3건을 삭제하거나 정상 공고로 미리 승인하지 않는다. 2026-09-16 Windows에서 제한된 HTML 조회로 제목 표제 셀을 확인하고 임시 DB의 DRAFT seed를 실제 분류기에 전달하여 판정했다.

| 고정 참조 | 확인한 업무 성격 | 제목 단계 | 알려진 전체 첨부 / 이번 처리 |
|---|---|---|---|
| JECHEON-403587 | 농지전수조사 조사원 추가 모집 | COMBINATION_NOT_MATCHED | 1개 / 본문·첨부 요청0의 음성 표본 |
| JECHEON-403530 | 청년 주택자금 대출이자 지원 | COMBINATION_MATCHED | HWPX1개 / 본문→전체 파일 추출→종합 판정 |
| JECHEON-403490 | 방문운동 지원사업 제공기관 모집 | COMBINATION_MATCHED | HWPX2개 / 본문→전체 파일 추출→종합 판정 |

403587은 자동 제외 키워드 적중으로 오기하지 않는다. 대상·지원 조합 미충족이며, 관측 상태 `TITLE_NOT_ELIGIBLE_NOT_FETCHED`로 남긴다. 이 사전 음성 기대값과 현재 규칙이 달라지면 실패한다. 미충족인데 파일 검증을 위해 제목 Gate를 우회하지 않는다. 다른 기존 태백/양평 표본의 후보 assertion·자동 제외 처리는 보존한다.

HTML 진단은 총7요청, 각 요청 연결3초/전체12초/응답1MiB/redirect0/재시도0으로 제한했다. 첫3회는 잘못 가정한 제목 span이0개였고, 다음1회에서 HTTP200·285483bytes 및 제목 th 구조를 확인한 뒤3회에서 정확한 제목만 출력했다. 첨부 요청·원문 파일 저장·DB 쓰기는0이다. 이 조사와 이후 CI의 수집 흐름 요청 예산은 별도로 보고한다.

## 구현 계약

- 검증용 제목 layout을 CLASSIC_LABEL/COMPACT_SUBJECT/COMPACT_LABEL로 구분한다. 제천은 단일 `div.p-wrap.bbs.bbs__view > table.p-table.block`에 직접 소속한 제목 th와 인접 td만 허용한다. 중첩 제목·중복·주변 페이지 대체는 거부한다. 기존 worker 시험도 같은 layout 선택을 사용한다.
- `attachmentBbsOfficialFileObservation -PsanebBbsObservationGroup=JECHEON`으로 고정3건을 순차 실행한다. 임의 URL/기관/ALL 입력은 받지 않는다. 기존 기본값 TAEBAEK과 운영 플래그 비활성은 유지한다.
- 한 공고420초/44요청 예약/80MiB, 파일20MiB, 상세1MiB, 단일 추출·단일 fork를 유지한다. 그룹 전체 상한132요청/240MiB이며 음성 표본은0요청이어야 한다. 제목 통과 후 BODY의 A/B·검수 상태만으로 첨부를 중단하지 않는다.
- 수동 workflow 입력 `observe-jecheon-official-files=true`만 새 외부 관측을 실행한다. push/일반 단위시험에서는 켜지지 않는다. 동일 SHA 전체 Linux 계약을 먼저 통과해야 하며 배포·정책 게시·운영 DB 접근 경로를 만들지 않는다.
- 새 Node 판정기는 현재 실행의 JUnit3건/실패·오류·생략0, 정확한3공고 JSON·시각·원본 정리·요청/byte 상한·운영 쓰기0을 요구한다. 403587의 본문·발견·첨부·요청0과 나머지2공고의 전체3파일·BODY·관리자 검증을 검사한다.
- 파일 부분/암호/OCR/손상 등을 관측 성공과 구분하며 완전성 표시가 실제 품질과 다르면 실패한다. 정상 후보나 기대값 승인·전체 정책 QA 성공으로 승격하지 않는다. metadata만 보관하고 원본·본문·추출 텍스트는 업로드하지 않는다.

## 검증과 남은 작업

- [x] 원격 `f7b7396`의 Linux34944248951 성공/JUnit·태백 고정 HWPX2개·HWP worker metadata를 회수했다. root2677=2412통과/265조건부 생략/실패0이며 별도 추출기88·패키지20·job192·migration17·worker12·runtime1·정책 부모2·Flyway3도 실패/생략0이다. 제천 추가 전 SHA의 근거다.
- [x] 태백 HWPX 고정 비교2/2 PASSED·전체 완전성true. HWP1건은9734자/495블록·PARTIAL_TEXT·TECHNICAL_EXCEPTION·검수이며 정상 공고로 세지 않는다. 두 실행 모두 운영 쓰기0/원본 정리true다.
- [~] 첫 로컬 표적13건 중12통과/1실패: Windows 애플리케이션 제어의 initdb.exe 실행 차단(CreateProcess4551). 보안 완화·차단 해제·운영 DB 대체는 하지 않았다. 후속 표적44건=43통과/Linux전용1생략, 최종 전체 회귀는 통과했으나 OS 차단의 영구 해소를 주장하지 않는다.
- [x] 최종 로컬 전체 회귀5분4초 성공: root2680=2416통과/264조건부 생략/실패0, 패키지20/20. 추출기88·bootJar는 UP-TO-DATE로 선행 결과를 재사용했고 probe JAR는 다시 생성했다. Node 보고서 판정기63건/생략·실패0 및 구문/diff 검사 통과. production JAR SHA256은 직전f7b7396과 같은917d743ee8634d8af6cf736102f9fa198cf71aa29fb205d7787abe5fe8b09905다. Java/임시PG·단발 Node는 종료됐고 사용자 output은 보존했다.
- [x] `f5d06e4` [Linux35044622501](https://github.com/FrostyCityMan/saneB/actions/runs/35044622501) 성공 및 보관 metadata/JUnit을 회수·대조했다. root2680=2415통과/265조건부 생략/실패0이며 추출기88·패키지20·job192·migration17·worker12·runtime1·정책 부모2·Flyway3은 실패/생략0이다. 제천 별도3/3도 통과했다.
- [x] 제목 음성1건은 요청0, 양성2건은 BODY AVAILABLE와 전체 HWPX3개 COMPLETE_TEXT다. 403530은11645자/562블록·MIXED_DOCUMENT_ROLES, 403490의 두 파일은4692자/207블록·MIXED_DOCUMENT_ROLES 및4792자/304블록·INITIAL_HEADING_REQUIRED다. 역할은 모두UNKNOWN/최종REVIEW_REQUIRED이며 정상 공고/정책 QA로 승격하지 않았다. 예약9요청/5,069,800bytes·원본 정리true·운영 쓰기0이다.
- [ ] 전체 수집처의 정상3공고/정상 다중 첨부·형식 검증, 정책 QA와 승인 범위 운영 적용·업무 브라우저 E2E가 남는다. 취소된 배포는 자동 재개하지 않는다.

실행 명령:

```powershell
.\gradlew.bat :test --tests '*AnnouncementAttachmentBbs*ContractTest' --tests '*AnnouncementAttachmentOfficialWorker*Test' --tests '*AttachmentContractWorkflowTest' attachmentContractQaTest bootJar --no-daemon --console=plain --max-workers=1
node --test scripts/qa/attachment-jecheon-observation-report.test.mjs scripts/qa/attachment-taebaek-hwp-report.test.mjs scripts/qa/attachment-contract-report.test.mjs
.\gradlew.bat :test :attachment-extractor:test attachmentContractQaTest bootJar attachmentOfficialWorkerProbeJar --no-daemon --console=plain --max-workers=1
```

브라우저는 이번 증분에서 실행하지 않았으며 운영 관리자 업무 E2E 성공을 주장하지 않는다. 새 SHA 수동 Linux 관측은 실제 실행 ID·결과를 후속 확인한다.
