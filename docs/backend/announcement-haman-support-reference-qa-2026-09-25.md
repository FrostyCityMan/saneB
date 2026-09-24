# 함안군 지원사업 참조 QA — 2026-09-25

## 목적과 판정 기준

전체 첨부7엔진/19프로필 중 참조가 없는 함안 프로필의 실공고 근거를 확보한다. 최소3정상 공고·전체 해당 형식·실제 격리 추출·구간·worker DB/API·운영/브라우저라는 전체 완료 기준을 유지한다. 현재 확보 대상은1건이며3건으로 확대해 보고하지 않는다.

- [x] 기존 프로필/공식 상세/요청 경계와 과거 구조 표본을 확인했다.
- [x] 고정 공식 제목·첨부 개수를 확인하고 소속 관계 검증과 제한된 표본 계약을 추가했다.
- [x] 합성 제목/URL/예산 회귀를 검사했다.
- [x] 기존 pinned transport로 실제 본문·전체 첨부 다운로드/signature 및 원본 정리를 검사했다.
- [x] 실제 통과 후 catalog의 미승인 참조로 연결하고 기존 기대값을 보존했다.
- [ ] 나머지2공고·해당 형식·서울 격리 추출/구간/worker DB/API와 검토된 기대값.

성공: 고정 제목·정확한source/profile·전체 파일 수·TLS/URL/bytes 경계·원본 정리를 입증한다. 실패: 검색/링크 성공을 실제 파일 추출로 취급, 파일명 역할 승격, 기대값 자동 승인, 원장 초기화, 운영 변경이다.

## 출처와 사전 관측

[기업마당 공고](https://www.bizinfo.go.kr/sii/siia/selectSIIA200Detail.do?pblancId=PBLN_000000000117309)에서 함안군2026 소상공인 육성자금 지원계획을 찾았다. 공고 번호41306은 검색 결과에서 후보로 얻은 뒤 [함안 공식 상세](https://eminwon.haman.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=41306&subCheck=Y)의 실제 제목으로 대조했다. 검색 결과나 단축URL만으로 identity를 확정하지 않았다.

1. HttpClient41306: HTTP200/419byte, form/제목/첨부 구조0.
2. 기존43065 curl 대조: exit0/HTTP200, form1·table5·다운로드 호출1. 과거 표본의 현재 구조 확인이며 지원사업 후보 등록은 아니다.
3. V37 고정 목록page1: curl exit28/본문0, 공고번호를 얻지 못했다. 목록 수집 성공이 아니다.
4. curl41306: exit0/HTTP200, 공식 제목 `2026년도 함안군 소상공인 육성자금(이자) 지원계획 공고` 정확히 일치, form1·다운로드 호출1. 자동redirect/재시도/파일 다운로드는0.

응답 차이가 User-Agent 때문인지 클라이언트/시점 차이 때문인지는 미확정이다. 함안 전체 장애나 정상 상시 수집으로 단정하지 않는다. 단축URL2개는 웹 조회/로컬HttpClient/제한curl에서 연결되지 않아 더 반복하지 않았다. 단축URL 호출은 함안 서버 원장과 구분한다.

사전 함안 요청4회는 실패/대조 요청까지 보수적으로4MiB를 예약한다. 각1MiB/15초/redirect0/재시도0이다. 이번 지원사업 탐색 캠페인은60요청/96MiB 상한으로 관리하며 새 모드로 초기화하지 않는다. 기존43065의09-12 다운로드2회 등 역사적 구조 QA는 별도 실행 이력으로 보존한다.

## 로컬 검증 경로

기존 `LOCAL_HAMAN_GET_V1`/`LGS-000233`/`SAFE_SAEOL_EMINWON_CELL`을 사용한다. 정확한HTTPS host/path/query, 공인IP pinning, 첨부 공식영역/goDownLoad 해석·20MiB 파일 한도는 변경하지 않는다. HWP/PDF/HWPX signature만 확인하고 Windows에서 외부 binary 문서를 파싱하지 않는다.

제목은 단일POST form1→고정table 속성→해당table 소속의 `제목` td→바로 다음td에서만 읽는다. 중첩표·중복form/table·다른 제목을 일치 근거로 사용하지 않는다. 파일 역할은UNKNOWN이고 기대값은null이다.

새 `hamanSupportDiscoveryQa`는 정확한1공고의 제목/전체 파일과 본문2시험만 실행한다. 기존 달성 누적 원장의 JUnit 영수증을 덮어쓰지 않도록 보고서 경로를 분리한다. 최대8요청/24MiB 다운로드 시험과 최대2요청/2MiB 본문 상한, CPU 단일JVM/256MiB·각120초/30초를 유지한다. 사전4+최대10=14회/30MiB로 캠페인 상한 안이다. 후속 Linux 관측 예산도6회/23MiB로 고정하지만 이번에는 서버 실행 모드나 운영 설정을 활성화하지 않는다.

```powershell
.\gradlew.bat :test --tests '*AnnouncementAttachmentBbsOfficialObservationContractTest' --tests '*SaeolGetAttachmentDiscoveryProfileTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --no-daemon --max-workers=1
.\gradlew.bat :hamanSupportDiscoveryQa --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'
```

운영DB·정책·worker·기존 데이터·배포는 변경하지 않는다. 브라우저는 현재 명시 요청 정책상 미실행이며 전체9Gate=8부분/1차단을 유지한다.

## 실제 결과와 남은 경계

표적105건 실패/오류/생략0을 확인했다. 최초 시험에서 중첩 표의 `제목`을 바깥label로 받아들이는 결함1건을 발견했고, label 안의 중첩table도 거부하도록 수정했다. 재시험을 선행시험 종료 전에 시작한1회는 Gradle 결과 파일 잠금으로 실패했다. 두 실행 종료 확인 후 직렬 실행했다. `test` 선택이 하위 추출기에도 적용되어 해당 시험 없음으로 실패한 명령은 `:test`로 바로잡았다. 실패 실행을 성공으로 합산하지 않는다.

명시 `:test ... :hamanSupportDiscoveryQa`는21초에 성공했다. 표적105건은 앞선 성공 결과 UP-TO-DATE이며, 새 실사이트2시험은 발견/전체다운로드1.596초·본문0.187초, 실패/오류/생략0이다. 본문AVAILABLE·비어 있지 않음·attempt1/redirect0을 확인했다. 공개파일을 Windows에서 텍스트 추출하지 않았다.

- profileHash: `c7cb4961b49e97449fe09afb77c5a4d009d3bf2df93d611c6c547046ad926513`
- 상세hash: `458df2b31d1adec6c0f0231ea058edba499b674dfd3aad473be21a68f4935524`
- HWP1개/101,888byte, binaryHash: `c8d37ea0142d19f7270c8231cde80028e8e40a3b1a73d02038dca01207a5bb97`
- 첨부ID hash: `9361395dcdb2d3bfec1cda4bddd1892d8d239a9bac52c2dc63ceba2bc722f0ff`
- 실행시간: 2026-09-25 02:19:57~02:19:59 KST, 원본 정리true/DB쓰기0/운영활성화false.

원장은 사전4회/4MiB + 실제발견다운로드2회/107,754예약byte + 본문보수적2회/2MiB = **8/60회·6,399,210/100,663,296byte**, 잔여52회/94,264,086byte다. 본문 actual attempt1과 예약2회를 구분한다. 단축URL 진단은 두 URL 각각웹1/HttpClient1/curl1이며 이후 중단했다. 함안 서버 요청으로 합산하지 않는다.

`build/reports/haman-support-discovery-qa/LOCAL_HAMAN_GET_V1-41306.json` 및 `build/test-results/hamanSupportDiscoveryQa`에 비식별 영수증을 보존했다. 기존 `attachmentProfileDiscoveryQa`의 달성 본문3시험 XML도 그대로 남아 있음을 확인했다.

catalog는 **37참조/13기관/저장 기대값1/정상0**이다. 함안1건은 expectation=null인 REFERENCE_ONLY이며 실행 가능한 정답이나 정상3건으로 처리하지 않는다. 참조 없는 등록 프로필은6개(대구 중구·부산 본청·강북·대전 서구·화천·기업마당)다. 함안에는 나머지2개 이상의 적격 표본, 해당 형식, 실제 Linux 추출·구간·worker DB/API 검증이 남는다. 반복 수집/운영 설정을 변경하지 않았다.

선행 `60fdc93` [Linux36031713182](https://github.com/FrostyCityMan/saneB/actions/runs/36031713182)는completed/success다. 이 함안 변경의 CI 또는 현재 운영 반영 증거로 대신하지 않는다.

## 확대 회귀와 실패 수정

첫 첨부 모듈 확대 실행은 1,976건 중 실패1·조건부 생략8로 종료됐다. 실패는 정책 스냅샷의 catalog 예상 건수36과 실제37의 불일치였다. 참조 추가에 맞춰 예상 건수만37로 수정했으며 실행 가능한 기대값0건·QA미통과 검증은 유지했다. 수정 후 정책 스냅샷14건 실패/오류/생략0,24초 성공을 확인했다. 전체 회귀 재실행 결과는 이 표적 결과와 별도로 기록한다.

```powershell
.\gradlew.bat :test --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --no-daemon --max-workers=1
.\gradlew.bat :test --tests 'com.saneb.domain.announcementattachment.*' :attachment-extractor:test :attachmentContractQaTest :attachmentBbsObservationProbeJar :bootJar --no-daemon --max-workers=1
node --test scripts/qa/attachment-bbs-observation-probe.test.mjs
git diff --check
```

Node5건·diff 검사를 통과했다. 불필요한 실사이트 재호출은 하지 않았으며 이번 수정으로 캠페인 누적 요청량이 증가하지 않았다.

최종 확대 실행은3분33초에 성공했다. JUnit XML 집계는 첨부 모듈1,992건=통과1,968/조건부 생략24/실패·오류0, 추출기149건·패키지20건 모두 통과다. probe/bootJar는 이번 소스로 앞서 생성한 결과 UP-TO-DATE이며 운영 설치를 뜻하지 않는다. 별도 함안 실사이트2건의 영수증은 보존했다. 이전catalog36행과 기대값이 완전히 동일하고 새 함안1건의 expectation=null임을 Node로 재확인했다. 작업 소유 Node/Java/PostgreSQL 잔여 프로세스0을 확인했다.
