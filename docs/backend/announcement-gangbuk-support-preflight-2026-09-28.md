# 강북 지원사업 상세 사전 확인 및 공통 관측 전달 경로 보완

## 목표와 완료 경계

등록된 강북 프로필에 지원사업 고정 참조가 없는 공백을 줄인다. 전체 ATT001~062+구간 분석/9 Gate와 운영 대상225개 범위를 유지한다. 이번 결과는 **상세 식별·첨부 발견**이며 본문 판정·실제 파일 다운로드/추출·worker DB/API·정상 기대값·운영 E2E 성공이 아니다.

- [x] 실제 worker가 사용하는 다단계 파일 전달을 공통 실파일 관측 테스트에도 연결했다.
- [x] 고정 지원사업179490의 DRAFT 제목 판정·공식 상세 식별·전체 첨부 영역 발견을 확인했다.
- [ ] 실제 본문 판정 → 발견한4개 전체 다운로드 → Linux 격리 추출·구간 분석 → worker/임시 DB/API.
- [ ] 추가 지원사업2건·형식별/정상 다중 첨부 근거 및 기대값 검토.
- [ ] 정책 QA·정확한 영향 승인·같은 SHA 운영 적용·역할별 운영 브라우저 검증.

catalog40참조/14프로필·저장 기대값1/정상0은 변경하지 않았다. 발견만으로 참조나 정상 기대값을 추가하지 않는다. 전체 Gate는8부분/1차단이다.

## 발견한 검증 경로 차이와 수정

`AnnouncementAttachmentBbsOfficialObservationTest`는 직접 pinned download를 호출하여 강북의 bridge GET → 게재기간 POST → 최종 파일 POST를 처리하지 못했다. 운영 `AttachmentDownloadGateway`는 이미 `AttachmentProfileDownloadFlow`를 사용하고 있다.

관측 파일 다운로드도 같은 flow로 연결했다. 공고 전체 누적 요청·byte 예산과 파일당 최대4 HTTP 요청을 함께 적용하며, bridge·기간 응답의byte도 합산한다. 게재기간 만료/예산 부족/부분 다운로드는 실패로 남기고 임시 파일을 정리한다. `isHome=Y` 우회·만료기간 무시·파일명 역할 확정은 없다. 기존 직접 GET 프로필과 제목/본문/첨부 판정 순서는 유지한다. 제품 코드·DB/API·migration·정책은 변경하지 않았다.

## 강북179490 실제 관측

후보는 [강북 공식 새소식](https://child.gangbuk.go.kr/portal/bbs/B0000145/view.do?menuNo=200081&nttId=182614)이 연결한 공고번호다. 새소식에 나온 파일 수를 실제 법정 게시판 검증 결과로 간주하지 않고 등록된 정확한host/path/menuNo의 [공식 상세](https://child.gangbuk.go.kr/portal/bbs/B0000245/view.do?menuNo=200082&nttId=179490)를 직접 확인했다.

- 고정 제목: `2026년 청년 어학・자격시험 응시료 지원 사업 모집 공고`.
- 2026-09-28 16:10:03~16:10:05 KST 로컬 상세 확인. 서울 서버 실행이나 운영 상태 조회는 아니다.
- 현재 migration을 적용한 별도 loopback 임시 PostgreSQL의 DRAFT seed에서 `COMBINATION_MATCHED` 확인 후 요청했다. 운영 ACTIVE 규칙 판정이라고 주장하지 않는다.
- 규칙 hash `609cea985b8f547292340539ab68293e6148ee76fd8636d270680aa19d7227ae`.
- `LOCAL_GANGBUK_LEGAL_GET_V1`, profile hash `6aa8ef570fdaf1dcc94e6e66b85d326a197de1057b6564368e97dc87ae781158`.
- 단일 `form#board`의 직접 hidden `nttId`와 직접 제목을 대조했다. 상세 hash `d41253cf9e689591d741b949cf1a9bc13c0179629ce70fdfeea00dcd5ec64b1c`.
- 발견 `FOUND/complete=true`, 전체4개, 형식 힌트 순서 HWPX/HWP/HWPX/HWPX, 미해석 경고0. 힌트는 binary signature 검증이 아니다.
- 첨부 다운로드0·본문 판정 미실행·운영 쓰기0·기대값 승인false·정책 QAfalse. 상세 임시 원본 삭제 확인.
- 결과 `build/qa-results/gangbuk-support-detail-179490-result.json`, SHA256 `fef3c6fef563c6c79cfcdc3d728aacc5d7cab56656bd9f9f22bb9b0f7cabcac9`.

## 예산과 중복 방지

이번 상세 사전 확인은 최대1요청·1MiB·전송30초/전체 시험90초, redirect/자동 재시도/파일 다운로드0이다. 실제 요청 예약1/1·278,528/1,048,576byte다. `gangbuk-support-detail-179490-reservation.json`을 네트워크 전에 CREATE_NEW로 확보하고 실패/중단 시에도 보존한다. 같은 경로를 재실행하면 예약 파일 때문에 중단하며 예약을 지워 우회하지 않는다.

09-12 강북184761/184744/184759/184760의 과거 구조 QA와09-15 본문 smoke는 별도 이력이다. 그 요청량을 없애거나 새 표본의 성공으로 이전하지 않는다. 후속179490 전체파일 QA에서는 이번1회/278,528byte를 누적 기준선에 더하고, 실행 전에 전체 파일/bridge/기간/본문 요청 상한과 기존 승인 범위를 대조한다. 중구32/32 등 다른 기관의 한도를 빌리지 않는다.

## 실행 검증

- `:test --tests '*AnnouncementAttachmentObservationTransferContractTest' --tests '*LegalBoardAttachmentTransferTest'`: 신규8+기존9=17통과.
- `:test --tests '*AnnouncementAttachmentBbsOfficialObservationContractTest' --tests '*AnnouncementAttachmentObservationTransferContractTest' --tests '*LegalBoardAttachment*Test'`: 55통과/외부 시험1조건부 생략·실패0. 실제 임시 seed의 기존 제목 stop/본문 검수 후 첨부 진행 계약을 보존했다.
- 기본 `:test --tests '*GangbukSupportDetailPreflightTest'`: 제목/식별 회귀1통과, 실제 조회1조건부 생략. 일반 CI에서 새 외부 요청은 실행하지 않는다.
- 명시 `gangbukSupportDetailPreflight`:2통과/생략0, 실제 상세 조회1회. 시험용 원본/임시 PostgreSQL/Gradle 종료. TLS 검증을 유지했다.
- 모든 Gradle 명령은 로컬 wrapper, `--no-daemon --max-workers=1`을 사용했다. 상세 실행에는 Windows ROOT trust store를 전달했으며 TLS 검증 비활성화는 없다.
- `attachmentBbsObservationProbeJar attachmentContractQaTest bootJar`: 관측 JAR 재생성·패키징20통과, bootJar는 제품 코드 불변으로 UP-TO-DATE 성공. 전체 root suite는 이번 증분에서 다시 실행하지 않았고 표적 회귀와 구분한다.
- `node --test scripts/qa/attachment-bbs-observation-probe.test.mjs`:9통과/생략0. 새 명시 task가 기존 지역 task/CI 외부 호출에 섞이지 않음을 확인했다. 실행한 Node는 종료했다.
- `git diff --check`: 오류0. 직전 e753374 Linux run36389360033은 조회 당시 진행 중이므로 이번 코드 또는 해당 실행의 통과로 보고하지 않는다.

브라우저는 현재 명시 지시가 없어 정책상 미실행이다. 정책/기존 데이터/배포/운영 설정은 변경하지 않는다.

## 전체4파일 서울 격리 관측 준비 — 후속

- [x] `GANGBUK_OBSERVATION` 단일179490 모드를 Java/Bash/Python에 연결했다. 임의 공고/다른 지역/정상 파일만 선택은 허용하지 않는다. 상세에서 관측한4개 locator hash와 HWPX/HWP/HWPX/HWPX 순서를 파일 요청 전에 대조한다.
- [x] 제목 DRAFT seed 판정 → 실제 본문 판정 → 전체4파일 다단계 전달·signature → Linux 격리 추출·역할/구간 → 종합 판정 경로를 재사용한다. 최초 binary 관측이므로 아직 없는 binary/text 기대값을 꾸며서 승인하지 않는다.
- [x] 본문 최대2요청 예약과 상세/파일/bridge/기간/redirect를 합쳐 신규20요청·32MiB, CPU1/메모리768MiB/임시공간1GiB/20분 상한이다. 이전 상세1회/278,528byte를 보존해 단일 지원사업 범위 누적21요청·33MiB로 제한한다. 자동 재시도 실행은 없다.
- [x] 관측 JSON은4파일 전체 분석을 위해 격리 내부 최대256KiB로 제한하고 전송은 기존 원문 없는 구간 요약/전체 분석 hash를 사용한다. 다른 모드의64KiB 제한은 그대로다. 파일 전체를 읽지 못하거나 분석 지문/형식/목록이 달라지면 완료로 판정하지 않는다.
- [x] 로컬 실행 helper는 사전 영수증 지문, 기존 실행 plan과 단일 예약을 확인한 뒤 CREATE_NEW로 예약한다. 같은 표본의 예약을 지워 재실행하지 않는다. 과거 다른 강북 표본 이력을 초기화하지 않는다.
- [x] Java 표적52통과/외부 상세1생략·패키징20통과·Python33통과·Node9통과. 최초 Node의 이전 모드 문자열 고정 비교1건은 신규 명시 모드/전체 기존 분기 검증으로 갱신 후 통과했다. 관측 JAR 재생성·bootJar UP-TO-DATE 성공. 전체 root 재실행/신규 Linux CI 결과는 별도다.
- [x] AWS root 계정/저장소 대상 일치·서울1인스턴스·SSM online·기존9a1bb45/Succeeded 확인. 이 조회만으로 실파일/운영 DB 상태를 성공 처리하지 않는다.
- [~] 실제 서울 관측·원본/임시 자원 정리 영수증은 실행 후 별도로 기록한다. 준비 상태를 실파일 성공이나 DB/API 저장 성공으로 표현하지 않는다. 기본 관측 구간 분석과 명시1.0.4 worker DB/API 검증은 분리한다.

### 실행 추적

- 소스 `21b5833504ef7d5ae2a445359a5f48a540624343`, QA 브랜치 원격 일치. 운영 배포 없음.
- 단일 실행 `63e2cb239ec24b70921e61601b692f34`, plan `build/temporary-bbs-qa-63e2cb239ec24b70921e61601b692f34/plan.json`.
- 135파일/88,406,161byte, archive SHA256 `86539512d82afb5c6a0255a5e8f8faf96705e1b9d782b8d74a0b0559d5f0beb9`, application codeHash `a15049147cd15f81eb45d07615e5b20c87c3785659b850d41641f39c19c8d549`. 제품 코드가 변하지 않아 application 지문은 이전과 같고 QA probe와 소스 revision은 별도로 고정했다.
- `build/qa-results/gangbuk-observation-179490-reservation.json`에 이전1회/278,528byte와 신규20회/32MiB를 원자적으로 예약했다. 전송 지연/관측 timeout을 이유로 새 실행을 만들지 않고 기존 핸들과 commandId를 추적한다.
- 후속 로컬 `:test` 전체3,101개=2,805통과/296조건부 생략·실패/오류0,7분26초 성공. 앞의 표적 검사 이후 전체 회귀 결과이며 생략된 외부/운영 QA를 성공으로 세지 않는다.

### 서울 실제 결과 — 미완료 및 정리 완료

- SSM `64189da9-8226-4dc6-b5a4-52f5d541f384`, Failed/exit1. 서버 실행31.455초, JUnit1개 중 실패1/생략0/컨테이너 실패0. 패키지 전송 시간과 실제 QA 시간은 구분한다.
- 영수증 `build/temporary-bbs-qa-63e2cb239ec24b70921e61601b692f34/result-utf8.json`, SHA256 `bbe9144fb7462bb094104a093e4a0b94b60d07cd77a45cc7e661629354721a7e`.
- 제목 조합 통과, 실제 본문 AVAILABLE/시도1/redirect0/435자, 본문 단독ACCEPTED다. 본문 hash `7a5dd488606054bd68d36675ee402408171ed686c8ced3ef112ad69ee23bd0bd`.
- 첨부4개 모두 발견하고 locator/형식 대조 후3개 다운로드·격리 추출. 네 번째HWPX는 `FILE_DOWNLOAD/ATTACHMENT_HTTP_400`으로 실패했다. 기존 영수증에는 bridge/기간/최종 전송 하위 단계가 없으므로 세부 원인이나 영구 장애를 단정하지 않는다.

| 발견 순서 | 형식 | 실제 bytes | 추출 문자/block | 결과 |
|---|---|---:|---|---|
| 1 | HWPX | 84,397 | 1,696 / 41 | COMPLETE_TEXT, 파일FORM·기본구간1개FORM |
| 2 | HWP | 140,800 | 1,221 / 51 | PARTIAL_TEXT, UNSUPPORTED_CONTROL1 |
| 3 | HWPX | 70,441 | 706 / 61 | COMPLETE_TEXT, 파일/기본구간UNKNOWN·INITIAL_HEADING_REQUIRED |
| 4 | HWPX 형식 힌트 | 미확인 | 미실행 | HTTP400, 다운로드 실패 |

- 관측 runtime1.0.14, 기본 구간 분석1.0.0이다. 명시1.0.4 worker DB/API 또는 정상 기대값 검증이 아니다.
- 앞3개 binary hash는 순서대로 `5c1ab6a267430b7644ea5851c373722116e44fc1de6f03ce39dff26fd23872eb`, `049a173ab67f08e031f811100ad762ace20ea3eb9cade0f986165f80df6fb8fb`, `99b8a2729756577944e6dfc72ddf80963f65eb54acc67ead336d3fa428f6fcdd`다. text/분석 hash는 영수증에 보존한다.
- 종합 `REVIEW_REQUIRED/ATTACHMENT_INCOMPLETE`, 전체 관측 `INCOMPLETE`, isWholeTextAnalysisComplete=false다. 일부 파일 성공을 전체 통과로 숨기지 않았다. 파일4개 중3개 처리와2개 완전 텍스트는 기관 지원 완료가 아니다.
- 이번13요청/2,679,122예약byte, 선행 상세 포함 **14/21요청·2,957,650/34,603,008byte**, 잔여7요청이다. 전체20요청 실행을 그대로 반복할 수 없다. 다음은 실패 파일의 전달 하위 단계 진단과 HWP unsupported control 원인 분리다. 단일 파일 진단도 전체4개 분모를 대체하지 않는다.
- 원본 제거·probe 정리·서버 전송 임시 파일 제거·unit inactive, 운영 DB미사용·쓰기0·운영JAR불변·healthUP 확인. 자기 실행의S3 객체 삭제/부재 확인·plan.cleaned=true, 지문/절대 경로 확인 후 로컬ZIP 삭제. plan/영수증/예약은 보존했다.
- 소유 Gradle/Node/AWS 실행은 종료했으며 기존 사용자 Java35696은 종료하지 않았다. 브라우저/운영 정책 게시/배포/ENFORCE/기존 데이터 적용은 하지 않았다.
