# 보은 실제 worker·임시 DB·API 검증

## 2026-09-22 서울 승인 임시 QA — 고정3건 성공, 정상 후보0

사용자가 보은221499·221497·218812의 서울 임시 격리 QA를 승인했다. 상한은132요청/240MiB/20분·CPU1·메모리768MiB·임시공간1GiB다. 옥천 보류는 유지하고 운영 설치/DB/정책/worker 변경은 제외했다.

- 사전 SSM `6a368e81-bfdc-41c0-9774-b3fff3c4279b`에서 메모리 여유951MiB/cgroup v2/도구/현재 JAR를 확인했다. DB 사전 `a84e994c-9921-44e6-bf89-71adf75305eb`, 사후 `10c14ada-2c72-49cb-8269-2a04182a0fcf` 모두 V83/실패0·첨부 정책/set/file/extraction/job/batch0·원문2945·READ ONLY/ROLLBACK/쓰기0이다.
- 실제 실행 SSM `98c4627a-36f6-4c52-875d-7af57758ba2a` Success. 설치 JAR `12985f8cd4d710f24da85d2f82c9556d719264aef5f43ac1a57239687bc9c2bc` 전후 동일·health UP·transient unit 종료를 확인했다. 현재9a1bb45 전용 `/opt` 불변 QA/추출기1.0.3을 읽기 전용으로 사용했다. 코드 지문은d49cb22d…로 운영과 동일하다. 로컬 Windows 묶음6ff295d8…는 사용/업로드하지 않았다.
- 시험 클래스·스크립트2개만 담은78,731bytes 패키지(c8e17350…/probe7eb19361…)를 전달했다. 실제 JUnit 발견3/통과3/실패·생략·중단·container 실패0, 실행65.595초다. 모든 공고의 BODY AVAILABLE/ACCEPTED, 전체 첨부 실제 추출→worker EVALUATED/job SUCCEEDED→임시 DB/API 검증, 전체 텍스트 완전성true를 확인했다.

| 공고 | 실제 파일 | 추출 결과 | 종합 판정 |
|---|---|---|---|
| 221499 | HWPX 97,068bytes | COMPLETE_TEXT·8,129자·397블록, UNKNOWN/MIXED_DOCUMENT_ROLES | FINAL_REVIEW_EXCEPTION / REVIEW_REQUIRED |
| 221497 | HWPX 95,092bytes | COMPLETE_TEXT·7,821자·378블록, UNKNOWN/MIXED_DOCUMENT_ROLES | FINAL_REVIEW_EXCEPTION / REVIEW_REQUIRED |
| 218812 | PDF 219,929bytes | COMPLETE_TEXT·5,566자·6블록, UNKNOWN/STRUCTURE_UNCERTAIN | FINAL_REVIEW_EXCEPTION / REVIEW_REQUIRED |

요청 예약 합계12/예약 byte7,391,673, 실제 파일 수신412,089bytes로 승인 상한 이내다. 예약량을 실제 전체 트래픽으로 표현하지 않는다. 원본 정리true·lease0·임시 DB/전송 공간 정리true, S3 전송 객체 제거와 로컬 소유 ZIP2개 제거도 확인했다. 사용자 output은 보존했다. 첫 CLI 수신의 한글 고정 metadata 키 인코딩 문제는 프로세스 한정 UTF-8로 동일 SSM 결과를 다시 조회해 대체문자0인 원본 metadata를 보관했다. 외부 수집을 재실행하지 않았다.

준비 검증: Python 실행기15/15, Node 보고서·Bash33/33, Java probe18+임시 DB 준비4=22/22, 실패/생략0·Gradle1분20초 성공. 기존 main Java·migration·운영 배포는 변경하지 않았다. 보관 결과는 `build/temporary-bbs-qa-2c7cc3ace7404142983932c905f1be45/result.json`이며 원본·추출 텍스트는 없다.

이 결과는 이전 GitHub DNS/TIMEOUT 실패를 성공으로 소급하지 않는다. **서울에서 수집·추출·임시 DB/API 연결은 성공했으나 정상 기대값0·정책 QA false**다. 혼합 문서와 PDF 구조 예외를 임의로 NOTICE/GUIDE로 바꾸지 않았고 catalog 기대값도 자동 등록하지 않았다. 운영 상시 worker·정책 게시·ENFORCE·기존 데이터·인증 브라우저 E2E 및 전체 Provider 검증은 남는다.

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
- [x] `36cd7fd0d6629a12ed8247603713ffc66d4eee9c`의 [Linux35053587664](https://github.com/FrostyCityMan/saneB/actions/runs/35053587664)에서 계약 단계는 통과했다. 보관 XML root2721=2455통과/266조건부 생략/실패0, 별도 추출기88·패키징20·job192·migration17·runtime1·worker12·정책 부모2·Flyway3은 모두 실패·생략0이다. 독립 namespace/PG와 부모 연결·취소·정리 단계 success도 확인했다.
- [!] 같은 실행의 보은 실제 파일 단계는 JUnit3건 모두 실패했다. 본문 `DNS_LOOKUP_FAILED`·시도0, worker EVALUATED/job PARTIAL_FAILED·첨부 발견 불완전·파일0이며 실제 추출은 미실행이다. 전체 workflow의 terminal 결과는 failure다. 자세한 관측과 다음 행동은 아래를 따른다.
- [ ] 역할·텍스트 기대값 검토, 정상3공고·다중첨부 표본, 정책 QA·전체 수집처·운영·브라우저 Gate를 완료한다.

```powershell
node --test scripts/qa/attachment-boeun-worker-report.test.mjs scripts/qa/attachment-jecheon-worker-report.test.mjs scripts/qa/attachment-official-worker-probe.test.mjs
.\gradlew.bat :test --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' --tests '*AnnouncementAttachmentOfficialWorkerPreparationTest' --tests '*AnnouncementAttachmentBbsOfficialObservationContractTest' --tests '*AttachmentContractWorkflowTest' attachmentOfficialWorkerProbeJar --no-daemon --console=plain --max-workers=1
```

명시적 Linux 실제 실행은 `bash ./gradlew attachmentBoeunWorkerIntegrationTest --no-daemon --console=plain --max-workers=1`이다. 이 문서는 실행 결과가 없는 단계의 성공을 선기록하지 않는다. 새 엔진/모델은 아니므로 엔진7·첨부 모델19·전용 본문18·형식3, catalog30참조/실행 기대값1/정상0을 유지한다.

## 2026-09-16 Linux 실패 관측과 진단 경계

같은 실행35053587664를 완료까지 관측했으며 재시작하지 않았다. 04:08:37Z·04:08:53Z·04:08:54Z에 고정3건 모두 TITLE COMBINATION_MATCHED였으나 BODY FETCH_FAILED/DNS_LOOKUP_FAILED·attempt0이었다. `ProviderContentUrlValidator.selectPublicAddress`는 JDK resolver의 UnknownHostException 또는 주소 없음에 이 코드를 부여한다. TLS/본문 selector/파일 형식 실패로 단정하지 않는다.

worker가 반환한 EVALUATED는 정상 파일 처리 성공이 아니다. 세 작업은 PARTIAL_FAILED이고 발견0·파일0이며 API 전체 파일 대조 전 `DATABASE_API / OFFICIAL_WORKER_INCOMPLETE`로 종료됐다. 상세 첨부 발견의 별도 원인 코드는 이번 metadata에 없어 본문 DNS 실패와 동일한 근본 원인이라고 확정하지 않는다. HWPX/PDF 품질·역할·정상 후보는 미검증이다.

- 합계9요청·6,291,456bytes는 **예산 예약량**이며 실수신 트래픽 수치가 아니다. 원본 정리true·lease0·운영 쓰기0을3건 모두 확인했다.
- 실패 후 로컬 Windows `Resolve-DnsName`과 Google Public DNS의 A 조회는 같은 공개 IPv4 주소를 반환했고, AAAA는 정상 응답/주소 없음이었다. 이 비교는 Linux runner에서 동일하게 해석됨을 증명하지 않으며 사이트 전체 장애 또는 특정 resolver 장애로 단정할 수 없다.
- CI 원본·파일/추출문은 보관하지 않았다. 다운로드한 metadata/JUnit은 Git 제외 경로 `build/qa-results/run-35053587664-boeun/`, 계약 XML은 `build/qa-results/run-35053587664-contracts/`에 있다.
- 이번3표본은 한 workflow 실행의 결과이며 독립적인3회 재시도 성공/실패로 세지 않는다. 동일 파일 QA를 무작정 다시 실행하지 않는다. 다음은 Linux의 OS/JVM 이름 해석을 한정된 진단으로 비교하고, 운영 재인증 후 실제 운영 네트워크와 구분하는 것이다. IP 하드코딩·DNS 설정 교체·TLS/SSRF 완화는 하지 않았다.

## 고정 Linux DNS 비교

`scripts/qa/attachment-dns-diagnostic.mjs`와 독립 Java 도우미는 보은과 비교 대상 제천의 공개 호스트 두 개만 조회한다. OS `getent ahosts`, Java21 `InetAddress.getAllByName`, Node 시스템 DNS의 A/AAAA 결과를 분리한다. Node resolver를 다른 서버로 교체하지 않으며 JVM hosts/IPv4 옵션도 변경하지 않는다. 프로세스별15초·Node 조회별5초·총8논리 조회/최대80초와 기동 여유, CI 단계3분 상한이다. 논리 조회 수는 실제 DNS 패킷 수가 아니다.

- 기존 QA 브랜치 push의 명시적 `[boeun-dns-diagnostic]` 표식만 실제 조회를 시작한다. 일반 단위 테스트는 합성 응답만 사용한다. 기존 수동10입력과 공식 파일 검증 조건은 변경하지 않는다.
- HTTP·본문·첨부·DB 요청은0이며 운영 설정/정책/데이터를 변경하지 않는다. 운영 인증정보를 요구하지 않는다. 자식 프로세스에는 PATH/LANG만 전달한다.
- 결과에는 상태 코드·주소 개수/종류·주소 집합 지문만 넣고 주소 원문·resolver 설정·예외 메시지·환경값은 넣지 않는다. `OBSERVED_NOT_COLLECTION_QA`는 관측 완료이지 DNS 정상/공식 수집/정책 QA 성공이 아니다.
- OS/JVM의 주소 집합 차이는 IPv6 표기·주소 선택/현재성 차이일 수도 있으며 근본 원인을 자동 확정하지 않는다. 새 runner의 관측은 이전 실패 당시의 상태를 소급 증명하지 않는다.
- 실제 Linux 결과와 이후 보은 파일 재검증은 별도 기록한다. DNS 조회만으로 BODY/첨부/정상 공고 Gate를 통과시키지 않는다.

### 2026-09-16 13:26 KST 관측 — 새 Linux 환경에서 DNS 해석 성공

`8589016448fa0b63b8e4ff5cca4a0e7dfe3d4af2`의 [Linux35055559668](https://github.com/FrostyCityMan/saneB/actions/runs/35055559668) DNS 단계와 보관 JSON을 직접 확인했다. 04:26:15Z~04:26:27Z의 보은·제천 모두 OS/JVM/Node A가 같은 IPv4 1개 지문을 반환했다. Node AAAA는 ENODATA이고 OS/JVM 비교는 RESOLVED_SAME_SET이다. HTTP0·다운로드0·운영 쓰기0이다. 원본 artifact는 Git 제외 `build/qa-results/run-35055559668-dns/result.json`에 있다.

이 결과는 새 Linux 환경에서 이전 DNS 실패가 재현되지 않았다는 근거이며 원인 해결·기관 정상 수집의 증거가 아니다. 실패 당시35053587664의 보고서를 바꾸거나 성공으로 재계산하지 않는다. 조회 상태의 변화가 확인되어 같은 코드 SHA에 보은 고정3건만 한 차례 재검증하는 [수동35055669009](https://github.com/FrostyCityMan/saneB/actions/runs/35055669009)를 시작했다. 최대132요청/240MiB, 기존 TITLE/BODY/전체 파일/DB/API/정리 기준을 유지한다. 전체 계약 실행35055559668과 실제 파일 실행35055669009의 최종 결과는 아직 미확인이다. 중복 재시작·운영 활성화·기대값 자동 승격은 하지 않는다.

### 2026-09-16 13:49 KST 재검증 결과 — 본문 시간 초과

35055559668은 전체 success,35055669009는 전체 failure로 완료됐다. 둘 다8589016이며 후속 DNS 오류 분류 수정본은 아니다. 두 번째 실행의 계약 root XML도2722=2456통과/266생략/실패0이다. 보은 실제 JUnit은3건 모두 실패했다.

04:48:59Z·04:49:17Z·04:49:26Z의 고정3건은 BODY FETCH_FAILED/TIMEOUT·2회 시도, worker NETWORK_TIMEOUT/job RETRY_WAIT로 끝났다. 발견 완료·파일 처리 수는 미확정(null), 파일 결과[]·추출 미실행이다. RETRY_WAIT는 이 시험의 임시 DB 상태이며 운영에서 재시도가 예약되었다는 뜻이 아니다. 발견0으로도 임의 치환하지 않는다.

모두 원본 정리true·lease0·운영 쓰기0, 합계 예약9회/6,291,456bytes다. 실제 수신 트래픽으로 계산하지 않는다. 보관 metadata/JUnit은 `build/qa-results/run-35055669009-boeun/`, 계약은 `build/qa-results/run-35055669009-contracts/`에 있다. DNS 실패와 이번 TIMEOUT은 구분하며 외부 방화벽/특정 서버 장애를 확정하지 않는다. 상태 변화 없이 동일 요청을 다시 반복하지 않는다.
