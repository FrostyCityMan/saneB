# 울산 남구 공식 목록 Linux 진단

## 2026-10-01 후보 보존 서울 재검증·로컬 수집 경로 연결

기준 HEAD `116b9c5c7ca1114f124800c7fad7f88c2c87d4c4`. 갱신된 AWS 인증으로 승인된 서울 임시 QA를 수행했다. SSM `4d3ba580-6bd8-448b-9664-4b00707f65a5`, 실행 `ce9e0991b5a04adeb742f41df1023016`은 14.474초 후 `INCOMPLETE`/SSM Failed로 종료됐다. 고정53732의 공식 제목 일치·DRAFT 제목 조합 통과·본문81자·**첨부 후보1개**를 확인했다. 미해석 href 경고 `ATTACHMENT_LINK_UNRESOLVED`와 발견 `FAILED`/complete=false는 보존했다. 파일 전송 시도는 `TRANSPORT_OR_PROBE_FAILED`로 끝났으며, 정상 파일 크기·hash·signature 근거가 없어 **실파일 성공0**이다. 세부 전송 원인은 이 보고서로 확정하지 않는다.

profile SHA `b4cfb3818a370924d22d170708061de2e3c155a9715ee311bc2c65a89b706a8e`, 실행 코드 SHA `193176f946e41fa357d520c040ede8ee4d9fd33e52995cc36076d6fb5a0a2284`, 패키지 SHA `4a718c2d2bb669b74e1c1ce50f9521c9ef5c1563abd73f859e42dc4e8265db2f`. 22파일/17,538,090byte 전송 패키지이며 상한8요청·22MiB·CPU1개·메모리512MiB·임시공간1GiB·20분이다. 예약24,576byte는 상세 응답 예약량이며 파일 다운로드량이 아니다. 실제 HTTP 호출 수는 계측되지 않아 상한을 실적으로 보고하지 않는다.

원본·서버 전송 임시파일 정리, unit 종료, 운영 설치 JAR 불변·health UP을 확인했다. 소유 S3 패키지와 로컬 `package.zip`도 정리했으며 plan/result 영수증은 보존했다. 운영 DB·정책·worker 설정은 변경하지 않았다. 이 일회성 probe는 정규 수집 영수증 스키마가 아니므로 963영수증/293표본 대장에 성공 또는 새 표본으로 편입하지 않았다.

사용자의 부분 수집 우선 원칙에 따라, 실파일 성공 전까지 로컬 연결까지 보류하던 순서를 조정했다. 이미 실측한 공식 DOM과 안전한 후보 해석을 사용하여 본문 provider·Spring 프로필 bean·고정 QA 참조 `ULSAN_NAMGU-53732`를 연결했다. 카탈로그 expectation은 null이며 정책 QA 승인/정상 추출 근거로 승격하지 않는다. 본문에서 메뉴·첨부명·담당 정보를 제외하고, 중복 form·잘못된 query·빈 본문을 별도 오류로 검증한다. 제목→본문→첨부→관리자 최종 검증, 자동 활성화 금지는 유지한다.

- [x] 본문/프로필 집중165테스트·bootJar 성공(37초)
- [x] 등록/카탈로그/본문/제목/예산 회귀299테스트, 실패·생략0(4분45초). 최초 실행의 테스트 fixture 프로필 누락2실패는 실제 등록을 fixture에도 추가해 해결했으며 기대값을 약화하지 않았다.
- [x] Node 대장 계약27개 통과,963영수증/293표본 재현 통과
- [x] 2026-09-28 운영 읽기 전용 snapshot을 현재 로컬 등록과 다시 비교: **223/223 연결·미연결0**, 인벤토리 SHA `fbad8eaab0978b4afc7bac24c50def11ab6aabc3fd86f292b63460e31ac8ed0d`. 현재 운영 수집 성공 수치가 아니다.
- [!] 실파일 확보 **210/223·미확보13**, 엄격3표본/전체첨부16/223 유지. 남구는 후보 확보 후 전송 실패이며 동일 요청을 추가 반복하지 않았다.
- [ ] 신규 등록 코드의 전체 Linux CI, 실제 상시 worker·운영 DB/API/UI·전체 E2E

검증 명령은 `gradlew.bat --no-daemon --max-workers=1 :test --tests '*UlsanNamgu*Test' --tests '*UlsanFirstDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar`다. 마지막 실행에서 인벤토리 opt-in 환경변수와 보관된 `build/qa-results/target-inventory-20260928-receipt.txt`를 사용했으며 새 운영 조회는 없었다. Node는 collection stage/receipts/availability/github-receipts 테스트와 verify/report 스크립트를 실행했다. HWP 고도화는 보류하며 브라우저는 현재 명시 요청이 없어 정책상 미실행이다. 아래 기록은 연결 전 단계 이력이다.

## 2026-10-01 서울 Java 수집기 실행·클릭 링크 보완

기준 HEAD `09901d74ec3618ddd51079c059ae29cc8e05373c`. 로그인 갱신 후 root 계정·저장소 대상·서울 리전·SSM Online을 다시 확인했다. 최신 운영 배포는 `d-NCB2HF3YK`/`9a1bb4569bcc3c13bf3bc30b51021b9149c67054`이며 변경하지 않았다.

- SSM `8b065a9c-d78a-4ed7-9d82-5ef1092863b7`: 실제 `AttachmentPinnedDownloadClient`로 고정53732 상세를 확보했다. 공식 목록 제목 hash 일치, 제목 `2026년도 울산 남구 소상공인 경영안정자금 융자지원계획 2차 공고`, DRAFT 제목 판정 `COMBINATION_MATCHED`, 본문81자다. 첨부는 `ATTACHMENT_LINK_UNRESOLVED`·descriptor0이므로 다운로드 성공이 아니다.
- 원문 없는 인자 형태 진단 SSM `098cf2f9-3843-4198-b7ea-184b92a3151d`: Python 직접 상세1GET·최대2MiB가 `REQUEST_TIMEOUT`으로 끝났다. 같은 진단을 재실행하지 않았다.
- SSM `b791e1cf-d30d-4196-a099-b054b4ebac74`: Java 진단을 보강해 동일 제목·본문을 다시 확인했다. 공식 첨부 anchor는 `onclick`을 사용하고 `href`에는 함수 문자열이 없었다. 기존 href 전용 해석이 실패한 근거다. 인자 원문·파일명·본문 원문·쿠키는 보고하지 않았다.

두 Java 실행의 profile hash는 `c1343de6bbeb448d54d92848e470219fdedc36af687a306739a0a436051c2579`, 실행 코드 hash는 `8393941e8a50470ba2683f6f7da0771a6fe2b327a0550d20345027e95e8940f3`다. 로컬 임시 DB에서 내보낸 DRAFT 394규칙의 SHA는 `0514e8b4fcec106fd708c99d7615f7c8fad852a8bdf954368abe3c4143c7350c`이며 운영 활성 규칙과 동일하다고 주장하지 않는다. 서버 DB와 추출기는 실행하지 않았다.

각 실행 상한8요청·22MiB, CPU1개·메모리512MiB·임시공간1GiB·20분 이내다. 첫6.494초·후속4.168초, 각각 예약24,576byte·실파일0이다. 이 probe는 실제 HTTP 호출 횟수를 계측하지 않으므로 최대 요청 수와 실제 호출 수를 혼동하지 않는다. 두 실행 모두 원본/전송 임시파일 정리·unit 종료·설치 JAR 불변·health UP을 확인했고 소유 S3 패키지도 삭제했다.

`UlsanNamguAttachmentDiscoveryProfile`에서 inert href의 고정 `goDownLoad` 세 문자열 onclick만 정적으로 해석하도록 보완했다. JavaScript 실행은 없고 다른 URL·임의 후속 호출·미해석 링크는 기존 오류로 분리한다. 기존 새올 경로·호스트·파일 수·파일명 검증은 유지한다. 다른 지역 어댑터와 공통 GET 엔진은 수정하지 않았다. 아직 Spring bean·본문 provider·QA 카탈로그 연결은 하지 않았다.

검증: `installAttachmentContractQa attachmentQaRuleSnapshot` 35초 성공·규칙 snapshot1통과. 클릭 링크 보완 후 `:test --tests '*UlsanNamguAttachmentDiscoveryProfileTest' --tests '*SaeolGetAttachmentDiscoveryProfileTest' --tests '*AttachmentDownloadInvocationTest' :bootJar installAttachmentContractQa` 39초·Java40통과/실패·생략0. Node 관련28개 통과,963영수증/293표본 재현 통과다. 사용한 Node/Gradle JVM은 종료했다. 변경 어댑터의 서울 재검증은 별도 결과를 확인해야 한다.

### 클릭 인자 확인과 전송 경량화

89,555,186byte 패키지 `b97da3a109a84a55a8199b1b1c58e132` 전송이 지연돼 해당 소유 AWS 업로드 프로세스만 종료했다. 종료 전후 읽기 전용 검사에서 완성 S3 객체 없음·uploaded=false·SSM ID 없음이었다. 서버 실행이나 공고 요청이 시작된 것으로 계산하지 않는다. 필요한 실제 수집 런타임만 남겨 17,537,591byte·22파일로 축소했고 운영 설치물은 수정하지 않았다.

SSM `4ac24012-609e-4134-82f9-2fdbdb30ead4`는 축소 패키지로 3.769초 실행됐다. 제목/본문은 동일하고, onclick의 단일 인용 문자열3개·확장자 HWPX·정상 날짜별 저장 디렉터리까지 확인했다. 다만 href가 당시 무동작 허용 문자열과 일치하지 않아 `ATTACHMENT_LINK_UNRESOLVED`를 유지했다. 원문 인자/경로 값은 기록하지 않았고 실파일0·예약24,576byte다. profile SHA `4504831161b09f7c32cb278e44485cd685630d301d9325408b3fff90b888b2ba`, 코드 SHA `f3c21b56a1855418179e7583536d2817ef985686f2561e0906969c305e44728f`다. 프로세스/원본/패키지 정리·설치 JAR 불변·health UP을 확인했다.

무동작 href는 ASCII 공백을 정규화하고 빈 `javascript:`/`javascript:;`도 허용하도록 보완했다. 다른 URL이나 임의 함수의 허용은 아니다. 동일 집중검증은 최종47초·40통과/실패·생략0, bootJar/별도 QA 패키지 생성 성공이다. 첨부 해석 실패3회는 그대로 기록하며 실제 파일 성공으로 바꾸지 않는다. 마지막 변경 검증도 실패하면 동일 표본을 추가 반복하지 않는다.

### 최종 결과와 남은 Gate

SSM `31d5a4bb-e456-4b4c-9327-faa6949f5da9`도 4.002초에 `INCOMPLETE`로 종료됐다. href는 공백 차이가 아닌 미해석 형식이며, 세 다운로드 인자와 정상 디렉터리는 그대로다. profile SHA `74264eae8e6ae8b1e2b099c0dca02174dc14ab405145b6d9818fc2249d4bec59`, 코드 SHA `77744c17b038109238de40cf4c96727f5aeb212257a87fd1d228e416ed7717cb`. 실파일0·예약24,576byte, 원본/서버 전송파일/프로세스/소유 S3 패키지 정리·설치 JAR 불변·health UP을 확인했다. 추가 네트워크 조회는 중단했다. 이번 회차 전체 상한은 Java4회×8요청/22MiB + Python1회×1요청/2MiB이며, 각 고정 실행은 재시작하지 않았다. 전송 중단은 별도이며 소스 요청0이다.

- [x] AWS 갱신·서울 대상 확인, 실제 Java 제목·본문 확보, 실패 단계 분리
- [x] 로컬 후보 보존 보완: 정확한 onclick 세 문자열에서 기존 호스트/경로/파일명 검증을 통과한 descriptor는 보존한다. 미해석 href를 따라가거나 실행하지 않으며 `FAILED`/`complete=false`/`ATTACHMENT_LINK_UNRESOLVED`를 별도 유지한다. 임의 onclick·잘못된 저장 경로는 계속 거부한다.
- [x] 후보 보존 회귀: 다른 상대/외부/loopback href와 임의 스크립트 href가 다운로드 요청으로 사용되지 않으며, 검증된 공식 파일 후보만 유지되는지 검사했다. 최종 같은 집중 Gradle 명령35초·**41통과/실패·생략0**·bootJar 성공.
- [!] 서울 첨부 해석 실패4회·실파일0. 마지막 후보 보존 변경은 로컬 테스트만 통과했으며 위 서버 실행에 포함되지 않았다. 운영 수집 성공·배포 완료로 표현하지 않는다.
- [ ] 별도 실행 계획에서 후보 보존 코드의 실파일 검증 후 bean·본문 provider·카탈로그·인벤토리·영수증 연결

실행 도구/영수증은 `build/qa-tools/UlsanNamguFirstFileProbe.java`, `build/qa-tools/run-ulsan-namgu-temporary.py`, 각 `build/temporary-bbs-qa-<executionId>/result-utf8.json`에 있다. 이 일회성 probe 결과는 기존 정규 수집 영수증 스키마로 자동 편입하지 않았다. 최신 코드 CI는 별도 확인해야 한다. 이번 회차에서 확인한 이전 HEAD의 Linux run36839748384는 마지막 조회 당시 실행 중이었다.

확보210/223·미확보13·미연결1·엄격전체첨부16/223은 유지한다. HWP 고도화·DB/API/Flyway·운영 설정은 변경하지 않았다. 브라우저는 현재 명시 요청이 없어 정책상 미실행이며 전체 goal은 미완료다.

## 2026-10-01 서울 구조 확인·등록 전 어댑터

기준 HEAD `46ea381ef7eb8790fd0b6bde3a575848d86edc8d`. 승인된 서울 임시 QA에서 운영 설치·DB·설정을 변경하지 않고 Python 표준 라이브러리로 고정 공식 경로만 조회했다. root 계정의 프로젝트 대상 일치·서울 리전·Ubuntu 1대·SSM Online, 운영 배포 `d-NCB2HF3YK`/`9a1bb4569bcc3c13bf3bc30b51021b9149c67054` 불변을 확인했다.

| 조회 | SSM 실행 ID | 결과 |
| --- | --- | --- |
| 공식 폼→소상공인 검색→첫 후보 상세 | `0d4aa3fd-2da5-4a76-8242-5013ec915f9e` | 3요청 모두200, 후보7건·53732의 HWPX 표기 링크1개 |
| 53732 상세 단독 DOM 조회 | `1b506263-0eb3-43b9-b1de-89bb50ed93da` | 1요청 REQUEST_TIMEOUT, DOM 미확인 |
| 공식 폼→같은 상세 DOM 비교 | `d0900e94-9477-4c67-8027-2f6f39b73ed2` | 2요청 모두200, 공개 세션 쿠키 없이 구조 확인 |

첫 조회 시각은 17:45 KST다. 폼 16,813byte/SHA `01a08111db57ed6b8804ccab86885f6175b9dd4ca4a4fc89253bf90836ccd28a`, 검색 25,640byte/SHA `d85382fa2971570db1747dfd3de98414264b2ffe4e0df8d45bc72fde2a93ade6`, 상세 20,384byte/SHA `6c14c28d8b26eb99ce0953f35d9f739cc059fc5c2d6d17db0f26fc2aa27cf004`다. 마지막 상세 응답 hash도 같다. 마지막 진단 스크립트는 `build/qa-tools/ulsan-namgu-structure-readonly.py`, SHA `8b027434085ec731fac230528a219c31c2b3fbd12bfd11ad61dacfa8a5242503`다.

공식 검색 후보 ID는53732·53628·53554·52786·52350·50575·49986이다. 진단의 후보 선택은 운영 제목 규칙 통과 판정이 아니다. 첫 후보 제목 길이37·SHA `2893374bb36426de317c4a6dc0a6e8f0553915b4aa3ec9129d0d0dba3e8c4108`만 보존했으며 다른 후보 상세는 요청하지 않았다.

실제 구조는 `form[name=form1][method=post] > div.bbs_detail.bbs_detail_basic` 아래 제목 `div.bbs_detail_tit > h2`, 첫 번째 `ul.bbs_detail_content2`의 li/a 첨부 링크, 두 번째 같은 class 목록의 공고 정보, `div.bbs-view-content` 본문이다. table은0개다. 이전 표 라벨 중심 진단의 빈 배열을 첨부 부재로 해석하면 안 된다. `goDownLoad` 호출1개, `FileDown.jsp` 문자열과 encodeURI 호출3개를 확인했지만 불투명 인자/본문/파일명은 기록하지 않았다. 독립 GET 실패와 후속 성공만으로 세션 필수나 전송 오류 원인을 단정하지 않는다.

서울 요청은 총6회·상한12MiB·실제 성공 응답100,034byte이며, 다운로드0·원문 저장0·운영쓰기0이다. 각 요청10초·프로세스 전체45초 경계와48초 watchdog, TLS/호스트 검증·redirect 금지·고정 호스트를 유지했다. 소유 임시 CA는 각 실행 후 정리했다.

### 로컬 구현·검증 경계

`UlsanNamguNoticePage`와 `UlsanNamguAttachmentDiscoveryProfile`을 추가했다. 공식 div/ul 영역 전체를 기존 새올 GET 엔진으로 전달하고 UNKNOWN 역할·최대10파일·고정 요청·미지원/미해석 링크 분리 계약을 유지한다. 빈 첨부 구조는 실제로 관측하지 않았으므로 NO_FILES로 확정하지 않는다. 두 번째 정보 목록에 파일 링크가 나타나는 등 영역 변경은 오류로 남긴다.

**아직 Spring bean 등록·본문 provider 연결·QA 참조 추가를 하지 않았다.** 실제 HTML의 다운로드 인자를 새 어댑터로 파싱하고 파일까지 확보한 증거가 필요하다. 현재 코드는 등록 전 어댑터이며 유일 미연결 수집원1개가 해소됐다고 집계하지 않는다. 보조 supportBusiness 게시판으로 대체하지도 않았다.

실제 클라이언트 확인용 `build/qa-tools/UlsanNamguFirstFileProbe.java`는 고정53732·목록 제목 hash 일치·기존 DRAFT 제목 판정 통과 후에만 파일1개를 처리하도록 작성했다. 요청 상한8(동일 URI redirect 포함)·예산22MiB·추출/운영 쓰기 없음이다. Windows 실행은 상세 수신 전에 `TRANSPORT_OR_PROBE_FAILED`·예약byte0으로 끝났다. 제목 판정과 파일 요청은 실행되지 않았고 소유 원본/임시 폴더는 정리했다. 이 결과를 다운로드 성공으로 계산하지 않는다.

첫 컴파일은 fingerprint 함수 인자 수 불일치로 실패했고 기존 두 인자 계약의 hash 결합으로 수정했다. 최종 `.\gradlew.bat --no-daemon --max-workers=1 :test --tests '*UlsanNamguAttachmentDiscoveryProfileTest' --tests '*UlsanFirstDownloadContractTest' --tests '*SaeolGetAttachmentDiscoveryProfileTest' :bootJar`는37초·**43통과/실패·생략0**, bootJar 생성 성공이다. Node 울산 진단·영수증·가용성28개도 통과했다. 기존963영수증/293표본을 재현했다.

다운로드 확보210/223·미확보13·프로필미연결1·엄격전체첨부16/223은 불변이다. HWP 고도화·DB/API/Flyway·운영 변경은 없으며 브라우저는 현재 명시 요청이 없어 정책상 미실행이다. 다음은 확보한 구조와 고정53732를 서울의 실제 Java 수집기 QA로 연결해 확인한 뒤 bean/본문/참조/인벤토리를 함께 등록하는 것이다. 동일 Windows 실패 요청의 자동 반복은 하지 않는다.

## 2026-10-01 고정 상세 직접 조회 후속

기준 HEAD `71ea61563b7a9d7593582d03d629a61bac62823d`. 기존 Linux 목록 run36782051228에서 확보한 첫 ID54578을 기존 새올 상세 요청 계약으로 직접 조회했다. 실패했던 소상공인 검색 POST를 반복하거나 번호를 탐색하지 않았다.

- 첫 Windows .NET 조회: `2026-09-30T23:58:50.0871796Z`, HTTP200·text/html·20436byte, SHA `94c9701d040df27b85b918d1fc561936e4af165444342aff83c469f636e9c55d`. form1/post, goDownLoad 함수, 첨부파일 문자열, 공식 FileDown.jsp 경로가 있었다. 지원사업 제목 적격·공식 첨부 영역·다운로드 성공은 아직 확인하지 않았다.
- 이후 구조 진단3회 중1회는 로컬 Python stdin 문자 인코딩 오류였다. 다른1회도 PowerShell 텍스트 변환이 개입했으므로 HTML 원본 근거로 사용하지 않는다. raw byte 전달로 수정한1회는 첫 응답과 같은 SHA였지만 단순 진단 파서가 form1 내부 라벨을 찾지 못했다. 응용 파서 오류나 실제 첨부 부재로 단정하지 않는다.
- 마지막 원문 없는 구조 확인은 curl 종료28(시간 초과)이었다. 이 회차 직접 요청은총5회, 각15초·2MiB 상한, redirect·쿠키·인증 우회·TLS 예외 없음, 원문/첨부 파일 저장0·운영쓰기0이다. 로컬 동일 조회를 더 반복하지 않는다.

새 `collectKnownDetailSurvey`는 기존 공식 목록의 ID54578에만 **1GET·최대2MiB·15초**를 허용한다. 검색·목록·파일은 요청하지 않는다. Linux의 `[ulsan-namgu-known-detail-01]` 최초 push에서만 실행하며 이전 목록/지원사업 진단과 상호 배타적이다. 기존6회 Linux 진단 요청과 이번 최대1회를 구분해 누적 최대7회로 관리한다. 과거 미사용 상한을 재시도 예산으로 전환하지 않는다.

`summarizeAttachmentPlacement`는 script/style/주석 속 첨부 문구를 제외하고 실제 라벨 주변의 태그·제한된 구조 속성·폼 열림/닫힘 수·알려진 함수 사용 여부만 기록한다. 원문 텍스트·href 인자·input value·쿠키 값은 기록하지 않는다. 이 구조 metadata는 파서 등록 근거를 조사하는 자료이며 DOM 검증·분류·다운로드 성공을 대신하지 않는다. 응답이 실패하면1회 오류 보고 후 종료한다.

Node19개·workflow Java21개 통과, 대장704/289 재현은 유지한다. 응용 Java·Flyway·DB/API·운영 수집원 설정은 변경하지 않았으며 bootJar는 이번 진단 수정에서는 재실행하지 않았다. 브라우저는 현재 명시 요청이 없어 정책상 미실행이다. Linux 실제 결과는 아직 미확인이다.

공식 `supportBusiness` 게시판에도 첨부 지원사업이 존재하지만 기존 LGS-000079 고시공고와 다른 게시판이다. 별도 보조 수집원 추가 여부는 사용자에게 질의했으며 답변 전 대체·추가·운영 활성화를 하지 않는다.

### 고정 상세 Linux 실행 결과

[run36794563420](https://github.com/FrostyCityMan/saneB/actions/runs/36794563420), HEAD `27efa7980aa0f474f109a699ab63ced62672c638`, job110154910957, artifact11133358167을 확인했다. `2026-10-01T00:08:09.080Z`에1요청으로 HTTP200·UTF-8·20436byte를 수신했으며 응답 SHA는 위 Windows 원본과 동일하다. `build/qa-github-runs/36794563420/known-detail.json`의 SHA는 `87a44cd0cae247bf0988d44ab3ee90626de14782e18c3adfd06fd26f13a94463`다.

form1/POST와 goDownLoad 함수, 확장자 문자열은 확인됐으나 직접 라벨·table class·script/주석을 제외한 첨부 라벨 위치는 빈 배열이다. 이는 해당 진단기로 실제 첨부 영역을 확인하지 못했다는 뜻이며 첨부 부재, 지원사업 부재, 정상 파일 발견으로 단정하지 않는다. 과거 단순 문자열 존재와 실제 DOM 영역 확인은 구분한다. 본문 원문·파일·쿠키 저장0·다운로드0·운영쓰기0이다.

Linux 진단 누적 실제 요청7회이며 이번 실행은1회로 끝냈다. 전체 계약 job은 artifact 확인 당시 실행 중이다. 미연결 울산 남구1개·최신 표본 미확보18개·현재 코드 실파일16/223을 유지한다. 같은 고정 조회를 더 반복하지 않으며 공식 상세 구조의 새 근거나 별도 보조 수집원 추가 결정이 필요하다. 다른 지역 파일 수집 작업까지 이 오류 때문에 중단하지 않는다.

## 현재 단계 / Gate

- [x] 기존 조사와 V61 공식 endpoint 확인
- [x] 고정 경로 전용 진단기·단위 테스트·명시적 CI 실행 조건 구현
- [x] 로컬 검증 후 QA 브랜치 최초 push에서 Linux 공개 조회
- [x] 공식 목록 응답과 상세 공고 ID10개 확보
- [ ] 대상 공고 제목·상세 본문·첨부 구조 확인
- [!] 울산 남구 첨부 프로필 미연결 유지

전 지역 첨부 발견·다운로드 우선 방침에 따라 진행한다. 기준 HEAD `73f38fcfef386ae463a27657f64f9c60c2539e0e`의 다운로드 근거는203/223이고 잔여20개다. 분모는2026-09-28 활성 수집원 스냅샷이며 고유 지자체 수나 전체 goal 완료율이 아니다. HWP 추출 고도화는 보류한다.

## 문제와 실행 범위

Windows 기존 조사에서 공식 iframe 조회 폼은 한 차례 확보했으나 목록 POST와 후속 초기 GET이 SocketException으로 실패했다. 같은 환경에서 반복하지 않고 GitHub Linux runner에서 공개 목록의 전송·문자셋·숫자 상세 ID만 확인한다. 다른 소식 게시판으로 대체하거나 운영 endpoint를 변경하지 않는다.

1. 공식 고시공고 메뉴 GET1회.
2. 기존 조사로 확인한 공식 새올 종료공고 iframe GET1회.
3. 두 번째 응답에서 조회 폼·목록 method·endpoint 구조가 확인될 때만 V61 기반 공개 목록 POST1회. 첫10행·page1·종료공고(Y), 검색어 없음.

총 최대3요청·응답당2MiB·누적6MiB·요청당15초다. 자동 재시도·redirect 추적·인증 우회·TLS 검증 해제는 없다. 공개 JSESSIONID는 같은 새올 호스트의 POST에 메모리로만 전달하고 로그·보고서에 쓰지 않는다. HTML과 파일 원본도 저장하지 않는다. 문자셋이 불명확하거나 공개 폼을 확인하지 못하면 POST를 생략하고 오류를 기록한다.

`attachment-ulsan-namgu-survey.mjs`의 보고서는 HTTP 상태·응답 크기/hash·문자셋·구조 여부·알려진 상세 호출의 숫자 ID만 포함한다. 상세 ID 추출은 목록 진단이며 제목 분류·첨부 발견 성공 판정이 아니다. 임의 상세 URL이나 첨부를 후속 요청하지 않는다. 수집 성공 대장에 추가하지 않는다.

## CI 및 검증

`[ulsan-namgu-list-survey-01]`이 있는 QA 브랜치 최초 push에서만 실행한다. 재실행에는 외부 요청을 하지 않는다. 기존 전체 계약 job과 그 실패는 보존하고 진단 metadata만 별도 artifact로 올린다. 운영 배포·DB·worker·정책·규칙·자동 활성화는 변경하지 않는다.

```powershell
node --test scripts/qa/attachment-ulsan-namgu-survey.test.mjs scripts/qa/attachment-regional-linux-workflow.test.mjs
.\gradlew.bat --no-daemon :test --tests '*AttachmentContractWorkflowTest'
node scripts/qa/verify-collection-receipt-index.mjs
git -c core.safecrlf=false diff --check
```

브라우저는 현재 사용자 지시가 없어 정책상 미실행이다. 실제 Linux 결과 확인 전 목록 연결·첨부 수집이 가능하다고 보고하지 않는다.

로컬 Node 관련57개 통과, workflow 집중 Gradle17초 종료0, 대장674영수증/289표본 재현 통과, diff 공백 검사 통과다. 애플리케이션 Java·Flyway·프로필은 이번에 수정하지 않았고 bootJar는 재실행하지 않았다. 충북 실파일 재검증은 연결 단계 실패로 별도 문서에 기록했다.

## Linux 관측 결과

[run36782051228](https://github.com/FrostyCityMan/saneB/actions/runs/36782051228), HEAD `07f13c3e7be9be6688ecb766655a6a80f760ee38`, job `110114441292`, artifact `11128212392`에서 실제 목록 진단 보고서를 확보했다. 관측 시각은 `2026-09-30T21:52:02.984Z`다. 전체 계약 job은 이 결과를 내려받은 시점에 실행 중이며 통과로 보고하지 않는다.

| 단계 | HTTP / 문자셋 | 응답 크기 | 확인 |
| --- | --- | ---: | --- |
| 공식 메뉴 GET | 200 / UTF-8 | 163874byte | 공식 새올 iframe 연결 |
| 새올 폼 GET | 200 / UTF-8 | 16813byte | 공개 목록 조회 form |
| 목록 POST | 200 / UTF-8 | 26344byte | 숫자 상세 ID10개 |

상세 ID: `54578`, `54577`, `54571`, `54570`, `54563`, `54543`, `54535`, `54460`, `54505`, `54494`. 제목·분류 적합성·상세·첨부는 아직 확인하지 않았다. 함수 호출의 숫자 ID를 확보했다는 뜻이며 수집 가능한 지원사업10건이라는 뜻은 아니다.

실제3요청·응답207,031바이트, 다운로드0·운영쓰기0·원문 파일 작성false다. Windows 실패를 전체 사이트 중단으로 일반화하지 않는다. Linux의 공개 세션 GET→POST로 목록에 도달했으나 현재 애플리케이션 수집 성공이나 운영 성공을 입증한 것은 아니다. receipt 성공 대장과203/223 수치는 유지한다.

다음 단계는 같은 공개 폼에서 지원대상·지원유형 후보의 제목을 확인한 뒤, 고정 상세 공고의 첨부 영역과 다운로드 계약을 확인하는 것이다. 아직 실제 구조가 없는 첨부 프로필은 임의 등록하지 않는다. 원본은 메모리에만 처리했고 JSON metadata는 `build/qa-github-runs/36782051228/result.json`에 보관했다.

## 지원사업 상세 구조 후속 진단

새 `[ulsan-namgu-support-survey-01]` 최초 push는 위 목록 진단과 상호 배타적이다. 같은 공식 폼 GET→고정 `소상공인` 제목 검색 POST→검색 결과의 지원 관련 후보1건 상세 GET만 수행한다. 실제 폼의 UTF-8과 목록의 상세 조회 method를 확인한 경우만 후속 요청한다. 추가 최대3요청·6MiB이며 앞선3요청과 합쳐 두 진단 누적 상한6요청·12MiB다.

제목은 메모리에서 후보를 찾는 데만 사용하고 ID·제목 hash·길이만 기록한다. 이 후보 선택은 구조 조사용이며 공통 A/B 분류 판정·운영 후보 등록이 아니다. 상세에서도 제목·본문·파일명·불투명 다운로드 인자를 저장하지 않고 form 이름·method·공식 action 경로·input 이름·표 class·라벨·다운로드 함수명·확장자 언급 수만 보관한다. 파일 다운로드는 실행하지 않으며 이 결과를 첨부 성공으로 집계하지 않는다.

후속 조사 결과에 근거해 실제 Java 프로필과 제목→본문→첨부 판정 흐름을 구현·검증한다. 공통 분류기·관리자 최종 검증·운영 자동 활성화 금지는 유지한다.

후속 진단 변경은 Node61개 통과, workflow 계약19개를 `:test --tests '*AttachmentContractWorkflowTest' --rerun`으로 실제 재실행하여 통과(17초)했다. 최초 실행은 UP-TO-DATE여서 새 검증으로 집계하지 않았다. 대장674영수증/289표본 재현을 유지하고 애플리케이션 build·운영·브라우저 검증으로 확대해 보고하지 않는다.

## 후속 실행 실패 및 우선순위 전환

[run36783048800](https://github.com/FrostyCityMan/saneB/actions/runs/36783048800), HEAD `fe285ebfdc12dd7ef97c18f316e1a572cf69ae2d`, job `110117766359`, artifact `11128063833`의 `support-structure.json`을 확인했다. `2026-09-30T22:01:54.167Z` 관측에서 첫 공개 폼 GET이 `REQUEST_TIMEOUT`으로 종료됐다. 요청1회 후 멈췄으며 검색 POST·상세 GET·첨부 다운로드는 수행하지 않았다.

후보 빈 배열은 지원사업 부재가 아니라 조회 미실행이다. 앞선 Linux 목록200·ID10개 근거와 후속 시간 초과를 함께 유지하며 사이트 접근이 불안정한 상태로 구분한다. 같은 요청을 반복하지 않고, 아직 Linux 파일 전송 비교를 수행하지 않은 잔여12수집원 묶음을 우선한다. 남구 프로필은 미연결, 전체203/223·잔여20개를 유지한다. 두 진단의 실제 요청은 누적4회이며 승인 상한의 미사용분을 자동 재시도에 쓰지 않는다.

직전 `07f13c3` 전체 계약 실행은 루트4,418개 중4,097통과·1실패·320생략으로 종료됐다. 기존 화천 고정 worker 계약 실패이며 목록 진단 성공을 전체 Gate 통과로 보고하지 않는다. `fe285eb` 전체 계약 job은 후속 metadata 확인 시점에 실행 중이었다.

## 전 지역 관측 후 제한된 후속 조회

잔여12수집원 Linux 관측과 대장 반영을 마친 기준 HEAD `6435b3557ea79e0f1d4e61329f0458c5eabf2f1c`에서 다운로드 근거204/223·잔여19개다. 울산 남구는 목록200과 후속 timeout이 함께 관측된 유일한 미연결 수집원이다. 전체 격리 QA 승인 범위에서, 기존 지원사업 구조 진단기를 새로운 최초 push에 한 번만 실행한다. 이전 실패 run을 재실행하거나 미사용 요청 예산을 자동 소비하지 않는다.

이번 추가 상한은3요청·6MiB·요청당15초이며, 앞선 두 진단과 합한 설정 상한은9요청·18MiB다. 실제 이전 요청은4회이므로 이번 실행 후 실제 요청은 최대7회다. 공식 폼 GET→UTF-8/조회 폼 확인→고정 소상공인 검색 POST→실제 목록 method와 후보가 확인된 경우 상세1건 GET 순서를 유지한다. 오류 발생 시 해당 단계에서 종료하며 자동 재시도하지 않는다.

새 호스트·다운로드 경로·TLS 예외·브라우저 위장·첨부 다운로드·운영 쓰기는 추가하지 않는다. 동일한 `[ulsan-namgu-support-survey-01]` 명시 표식의 새 커밋만 실행하며, 진단 결과는 해당 SHA artifact로 구분한다. 실제 상세 구조를 확보하지 못하면 미연결을 유지하고 추정 프로필을 등록하지 않는다. 이 재조회 자체는 수집 성공 수치에 반영하지 않는다.

### 제한된 후속 조회 결과

[run36786616504](https://github.com/FrostyCityMan/saneB/actions/runs/36786616504), HEAD `dbe591cf9fb8ded3e811f259a44c4d622d468fd3`, job `110130035580`, artifact `11130750157`의 metadata를 확인했다. 관측 시각은 `2026-09-30T22:39:55.979Z`다. 공개 폼 GET은HTTP200·UTF-8·16,813byte이고 조회 폼이 확인됐다. 후속 소상공인 검색 POST는`REQUEST_TIMEOUT`이었다.

실제2요청, 상세0·다운로드0·운영쓰기0·원문 파일 작성false다. 세 차례 Linux 진단의 실제 요청은누적6회다. 폼 응답 hash는`01a08111db57ed6b8804ccab86885f6175b9dd4ca4a4fc89253bf90836ccd28a`. 검색 결과가 없다는 뜻이 아니라 검색 완료 전 전송 실패이며, 남구 프로필 미연결과 다운로드204/223·잔여19개를 유지한다. 같은 요청의 후속 자동 반복은 하지 않는다. 실제 구조를 확보할 새로운 공식 응답 또는 접근 환경 근거가 필요하다.

목록 진단 단계의 정상 종료는 조회 보고서를 저장했다는 뜻일 뿐 검색·첨부 성공을 뜻하지 않는다. 이 SHA의 전체 계약 검증은 metadata 확보 시점에 실행 중이다. 별도 직전 HEAD `6435b35`의 [run36786013687](https://github.com/FrostyCityMan/saneB/actions/runs/36786013687)은 기존 화천 worker 고정 계약1실패, 루트4,419개 중4,098통과·320생략으로 종료됐다. 이 실패를 숨기거나 고정 기대값을 변경하지 않았다.
