# 대구 중구 지원사업 고정 표본 QA

## 서울 실행 완료 c2ca9c52f36342e6afd7818ac199e9e7

- 소스 b88a6fd406c3014d884b315dcdfeececea065163, QA branch/원격SHA 일치. 서울 root principal·저장소 계정·saneb/saneb-dev·단일 Ubuntu/SSM Online·기존 배포 d-NCB2HF3YK/revision9a1bb4569bcc3c13bf3bc30b51021b9149c67054를 재확인했다. 임시 QA 실행이지 운영 배포가 아니다.
- 로컬 전송 helper에 고정 중구 모드와 최초 실행 누적 원장 검사를 연결했다. 선행 상세/다운로드 영수증·JUnit3건·파일4개 지문·기존 서버 실행 부재를 검사하고 예약 파일을 CreateNew로 생성하여 동일 실행 재제출을 차단한다. 예산 검증9건/Node8건, PowerShell 구문 오류0, 기존 패키지 생성 task29초 UP-TO-DATE를 확인했다.
- 임시 전송 package135파일/88,399,111byte, archiveHash `2a0304bc17e0681b5ae76d637334452e59763d30aef65111d63f4380b10a224b`, codeHash `1100eadaef230a75068a0f465a59ac66280360781b107dbf807a6447471e509c`다. Ubuntu glibc용 의존성을 유지하고 다른OS PostgreSQL3jar만 전송 목록에서 제외했으며 로컬 의존성은 삭제하지 않았다.
- 로컬 plan `build/temporary-bbs-qa-c2ca9c52f36342e6afd7818ac199e9e7/plan.json`, 예약 `build/qa-results/junggu-three-stage-reservation-20260928.json`. 추가 최대12요청/48,234,496byte를 예약했다. 완료 영수증 확인 전 원장13회/6,908,001byte에 이를 합산한 최대25회/55,142,497byte를 사용된 것으로 보수적으로 취급한다.
- [x] SSM `9703133d-d9d8-4eb5-b94c-8c6859b729cc` Success/exit0, probe47.46초/3발견·3통과·실패/생략/중단0이다. 새로 만든 임시 DB의 DRAFT seed hash `609cea985b8f547292340539ab68293e6148ee76fd8636d270680aa19d7227ae`로 제목 통과2건·중단1건을 확인했다. 운영 활성 규칙 확인이나 정책 QA 통과가 아니다.

| 공고 | 실제 본문·전체 파일 관측 | 판정/다음 쟁점 |
|---|---|---|
| 34196 | 본문559자·HWPX159,994byte/2,963자/130블록·PARTIAL_TEXT | REVIEW_REQUIRED/ATTACHMENT_INCOMPLETE. 그림1개 metadata가 있으나 부분 추출의 유일 원인이라고 확정하지 않음 |
| 33626 | 본문127자·HWP127,488byte/3,120자/178블록 COMPLETE_TEXT + PDF209,769byte/4,241자/5블록 PARTIAL_TEXT | 전체2파일을 빠짐없이 처리했으나 REVIEW_REQUIRED. HWP 문서역할 FORM·구간v1.0.0 UNKNOWN3/FORM1, PDF 부분 품질 원인 추가 확인 필요 |
| 33315 | COMBINATION_NOT_MATCHED·본문/상세/첨부0요청·files빈배열 | TITLE_NOT_ELIGIBLE_NOT_FETCHED. 첨부 발견 실패나 본문 없는 정상 후보로 오인하지 않음 |

- extractor1.0.13에서 통과2건의 본문은 ACCEPTED였으나 최종 첨부 결합은 모두 검수 유지다. complete HWP의 구간 metadata는 기본v1.0.0 관측이며, 승인된v1.0.4 구간 worker 저장/DB/API 성공으로 확대하지 않는다. 파일 binaryHash는 선행 관측과 모두 동일하며 원문·파일명은 영구 저장하지 않았다.
- 사용량은 본문2시도 상한 예약을 포함해9회/4,703,888byte(각4회/2,263,943byte·5회/2,439,945byte·0회/0byte)다. 실제 본문 시도는 각1회였으며 예약 합계를 실HTTP 횟수로 단정하지 않는다. 캠페인 누적은 **22/30회·11,611,889/81,788,928예약byte**다. 단일 실행 guard는 유지하며 잔여가 있어도 자동 재실행하지 않는다.
- 최종 영수증 `build/temporary-bbs-qa-c2ca9c52f36342e6afd7818ac199e9e7/result-utf8.json`, SHA256 `c0c5b6b4ff79ab82fafba1dfe2802149951a19fcc38b29db392fadd352bb3dc4`. unitInactive/installedJarUnchanged/healthUp/transportTemporaryFilesRemoved/probeCleanupSucceeded/각 originalFilesRemoved=true, productionDatabaseUsed=false·productionWriteCount=0·정책/기대값 승인false를 확인했다.
- 서버 원본/임시 자원과 전송 객체, 로컬 소유 package.zip을 정리했다. 운영 객체 삭제0, plan/영수증/예약은 보존한다. 로컬 AWS/시험 Java/Node 프로세스는 종료했고 기존 사용자Java PID35696·output/·scripts/qa/__pycache__/는 보존했다. 소스 b88a6fd Linux36383959813은 현재 단위/DB/migration 실행 중으로 전체 CI 성공을 보고하지 않는다.

## 후속: 제목→본문→전체 첨부 Linux 관측 연결

- [x] 로컬 `build/attachment-qa/rules.json`의394규칙과 현재 Java 엔진 대조에서34196/33626은 COMBINATION_MATCHED,33315는 COMBINATION_NOT_MATCHED/EXCLUDED다. 스냅샷 대조는 운영 규칙의 현재 상태 증거가 아니다.
- [x] `JUNGGU` 고정 그룹과 `JUNGGU_OBSERVATION` 명시 모드를 추가했다. 본문은 기존 production pinned client, 첨부는 기존 중구 profile 및 Linux 격리 추출기로 처리한다. signature 단계의 제목 구조 검사를 공유하고 전체 파일/형식/기존 binaryHash·profileHash를 검증한다.
- [x] 33315는 제목 탈락 음성 표본이다. current seed가 달라지면 reserveBody/본문/상세/첨부 요청 전에 실패한다. 성공 보고서는 요청/byte0·files빈배열·후속 단계 필드 없음이어야 한다. 다른2건은 본문2차의 검수/정보 부족을 이유로 첨부3차를 생략하지 않는다.
- [x] Java probe→Bash→Python 서버 실행기의 고정 목록·안전한 보고서 판정을 연결했다. 기존 그룹 기본값/한도/지문을 변경하지 않으며 일반 CI에서 중구 외부 호출은 실행하지 않는다.
- [~] Linux 임시 DB의 최신 migration/DRAFT seed 대조 테스트를 추가했다. Windows에서는1건 명시 생략이며, 로컬 저장 스냅샷 성공으로 이 검증을 대체하지 않는다.
- [x] 서울 전송 helper/누적 영수증 사전 확인 및 단일 실행은 위 완료 영수증으로 확인했다. 이 관측은 구간 metadata와 기존 파일 단위 결합 판정을 기록하는 진단이며, 구간 엔진 worker 저장·DB/API·정상 기대값 승인·최종 관리자 E2E를 증명하지 않는다.

### 유지되는 요청 상한

기존 지원사업 캠페인 원장13회/6,908,001예약byte를 유지한다. 후속1회 관측에서 제목 통과2건에 각각6요청/23MiB, 제목 탈락1건은0요청이므로 **추가 최대12요청/46MiB**다. 예약 합계25회/55,142,497byte로 최초 캠페인30회/78MiB 이내이며 상한을60회 등으로 늘리지 않는다. 운영 DB/설치/정책/worker 변경0과 실패 후 자동 재실행 금지는 그대로다. 과거 검색 도구 내부 트래픽 미확인은 기존 기록과 동일하게 별도 취급한다.

### 검증과 추가 발견

- Java 표적51건 중50통과/실패0/Linux seed1생략, 관측 probe JAR 생성37초 성공.
- 확대 Java230건 중229통과/실패·오류0/동일 seed1생략, 패키지20건 통과와 bootJar 검증 성공. 마지막 캠페인 예산 단언 수정 후 동일230건을47초에 재검증했다(229통과/1생략). 업무 코드가 같아 bootJar는 UP-TO-DATE이며 새 운영 빌드/배포 성공을 의미하지 않는다.
- Python29건 통과, Node 전체35건 중32통과/실패0/Windows의 Linux symlink3생략. 외부 공고 호출은0이다.
- 이전5067d26 Linux36382856909는 completed/failure다. 보관 XML에서 정책 스냅샷의 catalog수37→40 불일치1건을 확인했다. 함안 task 경계 검사 실패와 별개의 누락이다. 수를40으로 수정하면서 새 중구3건이 TARGET_OUTSIDE_SCOPE/inputHash없음/normalNotice=false로 고정되는지도 검사한다. DB/migration 전체 성공으로 보고하지 않는다.
- 13a5056의 CI36383251168은 확인 시점 실행 중이다. 이 소스에는 위 catalog수 수정이 없으므로 새 작업의 성공 근거로 사용하지 않는다.

## 목표·범위·판정 경계

기존 LOCAL_DAEGU_JUNGGU_GET_V1/LGS-000045에서 지원사업 참조가 없는 공백을 채운다. 사용자 모든 격리 QA 승인 범위의 고정 공개 표본 검증이며 운영 DB/설치/정책/worker/기존 데이터는 변경하지 않는다. 보은 추가2회 승인 대기와 별개 기관·표본이다. 정상 기대값을 추측하거나 과거 중구34295의 PDF signature 성공을 새3건의 성공으로 재사용하지 않는다.

전체9Gate=8부분/1차단. 목표는 제목→본문→전체 첨부→구간/분류→최종 관리자 검증이며 아래 발견·signature 증거만으로 전체 완료를 선언하지 않는다.

## 공식 표본과 관측 근거

V41의 공식 목록과 실제 제목 검색 필드 not_ancmt_sj를 확인한 뒤 지원 검색 결과에서 선택했다. 검색 도구의 공식 목록 접근 실패1회와 로컬 직접 HTTP 성공은 별개로 기록한다. 로컬 목록2회(16,219/15,366byte), 상세3회(6,797/5,536/6,097byte) 모두 HTTP200이다. TLS·고정 origin·공개 DNS 확인, 무리다이렉트·12초·응답1MiB 상한으로 읽고 메모리에서만 조사했다. 아직 파일 다운로드/격리 추출/DB 저장 증거가 아니다.

| 공고 ID | 공식 제목 | HTML 첨부 호출 수 |
|---|---|---:|
| 34196 | 2026 다국어 QR메뉴판 지원사업 참여 사업체 모집 | 1 |
| 33626 | 「대구 중구 청년 부동산중개보수 및 이사비 지원사업」모집 공고 | 2 |
| 33315 | 2026년 음식점 위생등급제 컨설팅 지원 업소 모집 공고 | 1 |

세 상세의 table.boardView/제목 th 인접 td와 고정 제목 일치를 확인했다. 제목에 지원사업 표현이 있다는 사실은 운영 규칙의 제목 통과·최종 적합 판정을 의미하지 않는다. 첨부 호출 수는 production profile로 전체 파일 발견을 재검증해야 한다. 원문·파일명·다운로드 인자를 로그/문서에 보관하지 않는다.

## 실행 체크리스트와 한도

1. [x] 공식 목록/상세 고정 ID·제목·구조를 확인한다. 관측용 Node는 종료했으며 원본 파일 저장0.
2. [x] 전용 jungguSupportDiscoveryQa와 오프라인 제목/고정 ID 계약을 검증했다.
3. [x] 고정3건의 production pinned transport·공식 첨부 전체 발견·다운로드 signature 및 정리를 확인했다.
4. [x] 성공한 고정 identity만 catalog reference로 추가했다. expectation은 null을 유지한다.
5. [ ] 본문/전체 PDF·HWP·HWPX Linux 추출·구간 분류·worker DB/API·검토한 기대값은 별도 후속 검증이다.

전용 task는 SANEB_ATTACHMENT_PROFILE_QA와 SANEB_JUNGGU_SUPPORT_QA를 모두 명시한다. 일반 test와 기존 지역 task에서 새 외부 요청을 실행하지 않는다. 세 사례는 각각120초·8요청·24MiB, 합계 최대24요청·72MiB이며 단일 JVM으로 순차 실행한다. 다운로드 파일당20MiB, 상세1MiB, 원본은 finally에서 삭제한다. 이미 수행한 로컬5회와 접근 실패1회를 보수적으로 포함하면 이번 단계 전체 상한30요청·78MiB다. 실제 요청/예약 byte는 실행 영수증으로 차감하며 동일 실행을 반복하지 않는다. 이 단계 상한은 이후 재실행의 무제한 승인이 아니다.

성공 기준: 고정 제목·ID·기관·profile 결합, 발견완전/전체4파일, 지원형식 signature, 실패/추가파일/구조변경을 숨기지 않음, 임시 원본 제거. 실패 기준: 파일명으로 역할 확정, 일부 파일만 성공 처리, 제목 불일치 후 다운로드, 예산 초기화, 새 기대값 자동 승인, 운영 쓰기.

```powershell
.\gradlew.bat :test --tests '*JungguSupportReferenceContractTest' --tests '*SaeolGetAttachmentDiscoveryProfileTest' --no-daemon --max-workers=1
.\gradlew.bat jungguSupportDiscoveryQa --no-daemon --max-workers=1
```

현재 브라우저는 명시 요청 정책상 미실행이다. source/migration/API/UI/운영 규칙은 이 QA 준비에서 변경하지 않았다.

## 실제 실행 결과

- 표적 테스트26건(새 제목/고정범위9·기존 새올17)은29초에 실패/오류/생략0으로 통과했다.
- 전용 실사이트 task는23초 성공, 실제3건/4파일이다. 공식 상세 제목 일치, FOUND/전체 발견완전, 경고0, documentRole UNKNOWN 유지, temporaryCleaned=true를 확인했다. 추출기는 실행하지 않았다.

| 공고 | 실제 첨부 형식·byte | 실제 요청 | 예약 byte |
|---|---|---:|---:|
| 34196 | HWPX 159,994 | 2 | 166,791 |
| 33626 | HWP 127,488 + PDF 209,769 | 3 | 342,793 |
| 33315 | HWP 100,864 | 2 | 106,961 |

- signature 실행7요청/616,545예약byte다. 앞선 목록·상세5회와 도구 열기 실패1회에6MiB를 배정하여 이번 지원사업 캠페인 원장에 **13회/6,908,001byte**를 기록한다. 이는 예약 집계이지 도구 내부 HTTP 횟수/트래픽의 검증된 상한이 아니다. 실제 직접 요청은 Node5회+Java7회, Node 응답50,015byte이며 검색 도구 내부 트래픽은 미확인으로 분리한다. 기존2026-09-12 구조 표본34295의 별도 이력은 유지한다.
- 영수증 `build/reports/junggu-support-discovery-qa/LOCAL_DAEGU_JUNGGU_GET_V1-{34196,33626,33315}.json`, JUnit `build/test-results/jungguSupportDiscoveryQa`를 보존했다. 원본을 영구 저장하지 않았다.
- profileHash `e648e332e85e22fd2a6818eaf48b1a5d7ae58d4cad50b9e9ee0da847d73541ef`. 파일 binaryHash는 각각34196=`4a544f3c98451eb7c002c626157c2b92468962c14756f7b38e3e218954b1b554`,33626 HWP=`ae74fb4881522239bcb91a6f64dc137e1af55a207050ecfdc7e5a01f2a7026d6`/PDF=`6a57302609860d5332ccf95650cd754999270d6e6308acae19f06bd74104326c`,33315=`0d192cd2ad69fde6271fb2f8fc6018a98aada490b35ab1a4c66901b8f7e3bc77`이다.
- catalogVersion `2026-09-28-junggu-support-references-v2`, 전체40참조/14지역/기대값1/정상0이다. 기존 태백 기대값은 내용·시각·지문 모두 보존했다. 참조 추가는 정책 QA 통과·운영 게시가 아니다.
- 최종 표적+bootJar39초 성공: 제목9·새올17·catalog62·workflow18=106건 실패/오류/생략0. canonical source identity를 production normalizer와 대조하고 새3건 모두 REFERENCE_ONLY/inputHash null/normal false임을 검사했다. 기존 VM class-sharing 경고가 있다. Linux 동일SHA·본문·격리 추출·worker/DB/API 검증은 별도로 남는다.

이 변경은 QA task/시험·catalog 참조·문서다. 운영 parser/추출기/분류 규칙/DB/migration/API/UI는 변경하지 않았다. 새3건이 현재 제목 필터를 통과하는지와 원문의 지원 자격·문서 역할은 후속 실제 세 단계 검증으로 판정한다.

후속 확대 검증은 `:test`에 위4종과 `*AttachmentProviderQaCaseExecutorTest`를 포함하고 `:bootJar`를 함께 실행했다.37초 성공,179건(실행기73 포함) 실패/오류/생략0이다. source가 같은 bootJar는 UP-TO-DATE이며 실사이트는 다시 실행하지 않았다. 기존37개 reference/기대값의 JSON 내용이 모두 불변이고 새3개 null-expectation만 추가됐음을 Node로 대조했다. 사용한 단발 Node·Gradle 시험 프로세스는 종료하고 다른 사용자 프로세스와 output/·scripts/qa/__pycache__/는 보존한다.

## 후속 CI 실패와 오프라인 복구

5067d26의 Linux36382856909는 패키지 검증 단계에서 실패했다. 확인 시점 전체 job과 DB/migration 검사는 아직 실행 중이다. Node 검사에서 함안 task부터 attachmentQaRuleSnapshot 직전까지를 잘라 중간에 추가한 중구 task까지 포함한 것이 원인이다. 실제 Gradle task 실행 범위가 합쳐진 것은 아니다.

다음 최상위 tasks.register 선언까지만 읽도록 시험 helper를 수정하고 중구/함안 분리와 CRLF·마지막/누락 task 회귀를 추가했다. 아래 명령은 로컬에서35건/32통과/실패0/Windows에서 Linux 심볼릭 링크3건 생략,15.8초다. 표적8건은 생략 없이 통과했다. 동일SHA Linux 성공은 아직 별도 확인 대상이다.

```powershell
node --test scripts/qa/attachment-contract-release.test.mjs scripts/qa/attachment-official-worker-probe.test.mjs scripts/qa/attachment-bbs-observation-probe.test.mjs
```

새 공고 요청·추출·DB·운영 변경은 없다. 중구 제목 단계 통과 여부를 먼저 실제 seed로 판정해야 하며, 탈락 표본은 본문/첨부0요청으로 유지한다. 발견용4첨부 성공을 세 단계 정상 후보 성공으로 변경하지 않는다. 이 회차의 Node 시험은 정상 종료했고 관련 PID30432/35684 부재를 확인했다.
