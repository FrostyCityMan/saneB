# 옥천군 BBS 본문·첨부 모델

## 2026-09-22 실제 관측 결과 — 본문·상세 접속 실패

- [Linux35630969706](https://github.com/FrostyCityMan/saneB/actions/runs/35630969706), SHA `0bfa7522cfbd636a78dc2bea851500648761b6ba`는 최종 failure다. 관측 JUnit3건 중 제목 중단1건 통과/본문·상세2건 실패/생략0이며, 다음 표는 실제 결과 JSON 기준이다.

| 고정 공고 | 제목 | 본문 | 첨부 단계 | 예약 요청/bytes |
|---|---|---|---|---|
| 193369 | COMBINATION_MATCHED | 2시도 TIMEOUT/0자 | DETAIL_DISCOVERY TRANSPORT_TIMEOUT, 파일0/추출 미실행 | 3 / 2,097,152 |
| 193297 | COMBINATION_MATCHED | 2시도 TIMEOUT/0자 | DETAIL_DISCOVERY TRANSPORT_TIMEOUT, 파일0/추출 미실행 | 3 / 2,097,152 |
| 193187 | COMBINATION_NOT_MATCHED | 요청 없음 | 요청 없음 | 0 / 0 |

- 총6예약/4,194,304bytes는 본문 최대 응답량 예약을 포함하며 실제 다운로드된 파일 크기가 아니다. 세 보고서 모두 originalFilesRemoved=true, 운영쓰기0·기대값 승인false·정책 QA false다. 파일0을 정상 NO_FILES나 파서 성공으로 계산하지 않는다.
- 로컬에서193369 상세 URL에 TLS 검증 유지·redirect 미추적·연결5초/전체12초/최대1MiB의 단일 GET 진단을 수행했다. HTTP403/318bytes/0.148초/redirect0이며 원문은 저장하지 않았다. GitHub timeout과 다른 경로의 거부 응답을 확인한 것이지, 사이트 자체 장애·지역 제한·차단 주체를 확정하거나 production 본문 수집 성공을 증명한 것은 아니다. 자동 재시도/보안 완화/서울 서버 추가 실행은 하지 않는다.
- 같은 CI의 기본 XML2754=2488통과/266조건부 생략/실패·오류0, 추출기88·패키징20·job192·migration17·runtime1·worker12·Flyway3·정책 부모2 실패/생략0이다. 독립PG221/221 및 독립/부모 정리 성공을 확인했다. 이 코드 계약 통과와 외부 관측 실패를 분리한다. 결과는 `build/qa-results/run-35630969706-okcheon/`와 `run-35630969706-contracts/`에 있다.
- 다음은 접속 거부 원인과 허용된 수집 경로 확인이다. 접속이 확보된 뒤 같은 고정 표본을 관측하고, 실제 파일 내용·역할을 검토한 후에만 catalog 기대값으로 채택한다. 전체 수집처 QA·정상 후보·상시 worker·운영 E2E는 미완료다.

## 2026-09-22 독립 Linux 실제 관측 실행 범위

- 장기 goal의 실제 수집 모델 검증으로 기존 고정3공고를 GitHub Linux QA에서 한 번 실행한다. 서울 임시 QA의 태백1공고 범위를 확대하거나 운영 서버/DB를 사용하는 작업이 아니다. 기존 `[okcheon-observation]` 표식 경로를 사용하며 코드·migration·catalog 기대값·정책은 변경하지 않는다.
- 사전 기준선은1a19e63의 Linux35629360526 성공이다. 이번 실행도 자체 계약 검증 성공을 먼저 요구하고 동일 실행의 JUnit3건과 전체 metadata를 확인한다. 예상은 제목 통과193369/193297의 BODY와 각 HWPX1개, 제목 미충족193187의 본문/첨부 요청0이다. 예상과 관측이 다르면 실패로 남기고 자동으로 기대값을 교체하지 않는다.
- 기존 상한은 전체132요청/240MiB, 공고당44요청/80MiB/420초·파일당20MiB, 관측 단계12분이다. 실패/지연 시 같은 실행을 조회하며 관측 timeout만으로 재시작하지 않는다. 전체 파일 추출 결과와 역할, 종합 분류, 원본 정리를 대조한다.
- 성공하더라도 관측 사실이며 사전 기대값 승인·정상 후보 coverage·운영 상시 worker/DB/API/UI 성공은 아니다. 원문은 보관하지 않고 해시/길이/품질/역할/판정/요청량 metadata와 JUnit만7일 보관한다. 실제 결과는 실행 종료 후 기록한다.

## 2026-09-21 관측 결과 검사기·CI 경로

- `scripts/qa/attachment-okcheon-observation-report.mjs`는3공고 JSON과 JUnit의 현재 실행 시각·전체 표본·기관 일치·요청/bytes 상한·원본 정리를 검사한다. 제목 미충족193187은 BODY/첨부 요청0을 요구한다. 통과193369/193297은 각각 HWPX1개의 발견·추출 결과와 관리자 최종 검증 필요 상태를 확인한다.
- 부분 추출은 관측 사실로 보존하되 완전 분석으로 표시하지 않는다. 전체 텍스트 추출과 정상 후보/기대값 승인/정책 승인은 별개이며 결과에서 후자 승인을 생성하지 않는다. 누락·중복·과거 보고서·형식 변경·정책 승인·운영 쓰기·원본 잔류를 거부하는 회귀를 포함한다.
- Linux workflow의 `[okcheon-observation]` 명시 표식과 계약 성공이 모두 있을 때만 실행한다. 공고 관측과 결과 검증을 한 단계로 묶고 12분 제한, metadata/JUnit만7일 보관한다. 일반 push에서 외부 요청하지 않으며 이 경로의 실제 관측은 아직 실행하지 않았다.
- 옥천25건을 포함한 제천/충주 공통 검사기74건을 재실행해 실패·생략0을 확인했다. 합성 보고서 검사 시험이며 실제 파일 추출·전체 Provider·운영 정책 QA의 성공 증거로 사용하지 않는다.

## 2026-09-21 세 단계 관측 연결

- 기존 등록 표본3개를 공통 관측기의 `OKCHEON` 그룹에 연결한다. 제목→본문→전체 첨부 텍스트→종합 판정 순서와 혼합 문서 UNKNOWN을 유지하며 HTML parser/역할 규칙·DB·catalog 기대값은 변경하지 않는다. 이 과정에서 발견한 [BBS 리다이렉트 식별자 검사](announcement-bbs-redirect-identity-2026-09-21.md)는 별도 production 보완이다.
- 공식 페이지193369/193297/193187을 제한된 상세 GET 각1회로 조회해 HTTP200 및 `p-table__subject_text` 표식 각1개를 확인했다. 원문·담당자·연락처·첨부파일은 보관하지 않았다. 외부 검색 도구에서는 접근 불가였으므로 그 실패를 사이트 전체 장애로 단정하지 않는다.
- 고정 제목은 각각 환경개선 지원사업, 중소기업육성자금 융자 지원계획, 일반음식점 주방환경 개선 사업이다. 실제 원문 제목 전체는 테스트의 고정 입력이며 현재 DRAFT의 판정과 별도로 확인한다. 초기 DRAFT에서 중단되는 사례는 운영 정책의 정답 또는 지원사업 부적격이라는 뜻이 아니다.
- 단위/임시 DB 계약에서3개 source가 기존 catalog와 같고 기대값null·기관/공고/미리보기 요청 경계를 보존하는지 검증한다. 외부 관측은 아래 명시적 task에서만 수행하며 일반 `test`는 외부 요청하지 않는다.
- 전체 상한3공고/132요청/240MiB, 공고당44요청/80MiB/420초·파일당20MiB다. 제목 중단은 본문·첨부 요청0이며 보고서에서 제외하지 않는다. TLS·SSRF·추출 격리·원본 정리·metadata만 보관·운영 쓰기0은 그대로다.
- 실제 새 Linux 추출/역할/worker·DB/API·정상 후보 기대값·운영 검증은 아직 미실행이다. 현재 Windows에는 실행 가능한 Linux 격리 환경이 없으므로 우회하여 비격리 파일 추출을 하지 않는다. 아래09-15 다운로드/signature 통과와 구분한다.
- 실제 DRAFT seed를 적용한 제목 계약 결과는193369/193297 조합 통과,193187 조합 미충족이다. 첫 표본은 `기업`(보조)+`지원사업`(강함), 둘째는 `기업`+`융자` 근거를 검증했다. 사전 중단1건을 관측 분모에 남기고 요청하지 않는다. production 규칙의 변경이나 전체 공고 적합성 판단이 아니다. 이 경계와 source 일치·요청 제한을 포함한 표적179건이 실패/생략0으로 통과했다.

```powershell
.\gradlew.bat :test --tests '*AnnouncementAttachmentBbsOfficialObservationContractTest' --tests '*AttachmentDocumentRoleClassifierTest' --no-daemon --console=plain --max-workers=1
```

Linux에서만 실행할 후속 명령(현재 미실행):

```bash
bash ./gradlew attachmentBbsOfficialFileObservation -PsanebBbsObservationGroup=OKCHEON --no-daemon --console=plain --max-workers=1
```

## 현재 단계 / Gate

P3 기관 모델 구현 증분이며 전체 Gate는 **Not ready**다. 제목 → 정제 본문 → 실제 첨부 텍스트 → 최종 관리자 검증 순서를 보존한다. 본문 A/B·정보 부족은 첨부 처리를 중단시키지 않는다. 파일명·미리보기로 내용이나 문서 역할을 확정하지 않는다.

## 범위와 구현 전 검증 계획

- V61/V62의 `LGS-000140 / HEURISTIC_NOTICE`, `www.oc.go.kr`, `bbsNo=40 / key=236`에만 결합한다. 운영 DB 상태의 확인이나 활성화는 아니다.
- 기존 BBS 엔진의 COMPACT 구조를 재사용한다. 단일 공식 표의 제목·본문 셀과 `파일` 표제의 직접 첨부 목록을 요구한다. 메뉴·담당자·첨부명은 BODY에서 제외한다.
- 상세 `/www/selectBbsNttView.do`, 다운로드 `/www/downloadBbsFile.do?atchmnflNo=양의정수`만 요청한다. 같은 첨부 ID의 공식 미리보기는 구조만 확인하고 요청하지 않는다.
- 공식 공개 표본은 193369, 193297, 193187이다. 조사에서 각각 첨부1개가 확인됐다. 첫 파일의 제한된 헤더/8byte 관측은 HTTP200·application/x-msdownload·ZIP 서명이다. 전체 다운로드·추출 성공으로 계산하지 않는다.
- 기관별 고정 파일 헤더 정책을 명시하고 기존 기관으로 MIME 예외가 전파되지 않는지 시험한다. TLS·signature·확장자·Content-Disposition 검증은 보존한다.
- 단위시험은 기관/URL/게시판 경계, 메뉴·첨부명 제거, 중복·중첩 표식/부분 발견, UNKNOWN 유지, registry 결합을 검증한다. opt-in 실제 시험은 BODY3건 및 발견된 파일 전부의 다운로드/signature와 원본 정리를 검증한다.
- 실제 Linux 추출/역할/기대값/worker DB/API/UI와 운영 검증은 별도다. 단일 첨부3건을 정상 다중첨부 QA 완료로 계산하지 않는다. catalog 기대값을 추정하지 않는다.

## 결과

- [x] 고정 profile `LOCAL_OKCHEON_BBS_V1`과 전용 BODY 정제를 연결했다. 첨부 엔진6/전체 profile16(지자체15)/전용 BODY15기관/추출 형식3이며 전체 운영 대상 지원 완료가 아니다.
- [x] 첫 단위177건에서 세션 URL 예외 전파·기존 profile 건수2건이 실패했다. 횡성의 익명 session 경로 예외를 횡성 host에만 한정했으며 옥천 등 다른 기관은 기존 음성 시험대로 거부한다. 건수 assertion은 새 결합을 반영했다.
- [x] 최종 표적177건과 공식6건(본문3/첨부3)은 실패·생략0,47초에 통과했다. 본문은 각각 AVAILABLE/시도1/redirect0이다.
- [x] 공개3공고의 전체3파일은 HWPX signature/확장자/Content-Disposition까지 통과했다. 모두 실제 `application/x-msdownload`였으며 기관의 고정 헤더 옵션에만 허용한다. 같은 COMPACT인 횡성의 기존 octet/MIME 설정은 유지하고, 옥천은 UTF-8 header octet 복원을 사용하지 않는다.
- [x] schema2 공개 참조21건/기대값0/중복0이다. 새3건은 REFERENCE_ONLY이며 실행·정책 QA 성공을 만들지 않는다. schema1은 그대로다.
- [x] 전체 회귀·임시 DB Flyway 검증은4분23초 성공했다. root2568=2309통과/259조건부 생략/실패0, QA 패키지20/20, 원래 Flyway3/3이다. 첫 회귀의 snapshot 참조 건수18 assertion1실패는 실제21건 반영으로 해결했고 실행 기대값0·정책 QA false 검증을 유지했다.
- [ ] 같은 SHA Linux 및 운영 검증.
- [ ] 실제 파일 텍스트·역할 기대값과 전체 대상 QA.

기존 migration·v1·DB/API shape·UI·운영 정책·기존 데이터는 변경하지 않는다. 사용자 Word 산출물을 보존한다.

### 공개 표본과 근거 범위

| 공식 공고 | BODY | 전체 파일 | signature 결과 |
|---|---|---|---|
| [193369](https://www.oc.go.kr/www/selectBbsNttView.do?key=236&bbsNo=40&nttNo=193369) | AVAILABLE | HWPX1 / 87,388bytes | 통과 |
| [193297](https://www.oc.go.kr/www/selectBbsNttView.do?key=236&bbsNo=40&nttNo=193297) | AVAILABLE | HWPX1 / 382,264bytes | 통과 |
| [193187](https://www.oc.go.kr/www/selectBbsNttView.do?key=236&bbsNo=40&nttNo=193187) | AVAILABLE | HWPX1 / 96,379bytes | 통과 |

최종 첨부 시험은 상세3+파일3=6요청, 예약2,016,015bytes, 원본 정리3/3, DB 쓰기0이다. BODY는 별도3요청이며 앞선 구조/8byte 진단 트래픽을 합친 전체 요청량은 아니다. 발견 descriptor 역할 UNKNOWN을 유지했다. Linux 추출기·텍스트/역할 분류·worker DB/API/UI를 이 시험으로 대신하지 않는다. 단일 첨부3건이므로 정상 다중첨부 표본도 남아 있다.

고정 헤더 정책을 profile hash에 포함했다. BBS 공유 구현 변경으로 기존 BBS profile hash도 달라지므로 이전 관측이나 정책 QA를 현재 버전에 재사용하지 않는다. 중첩 표의 제목을 바깥 공고 제목으로 오인하는 경계도 차단했다. 정책/기존 snapshot을 소급 변경하거나 새 hash로 덮어쓰지 않았다.

```powershell
.\gradlew.bat :test --tests '*StandardBbsAttachmentDiscoveryProfileTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaPlanTest' --tests '*AttachmentProviderQaCatalogTest' attachmentProfileDiscoveryQa --tests '*StandardBbsAttachmentProfileLiveQaTest.discoversOkcheonFilesAndChecksBoundedProductionTransport' --tests '*StandardBbsBodyContentLiveQaTest.readsOkcheonBodyWithoutRequestingFiles' '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL' bootJar --no-daemon --console=plain --max-workers=1
.\gradlew.bat :test :attachment-extractor:test attachmentContractQaTest flywayIntegrationTest -PsanebFlywayEphemeral=true bootJar installAttachmentContractQa --no-daemon --console=plain --max-workers=1
```

보고서: `build/test-results/attachmentProfileDiscoveryQa/TEST-*.xml`, `build/reports/attachment-profile-discovery-qa/LOCAL_OKCHEON_BBS_V1-*.json`. metadata만 보관하며 파일·본문 원문을 문서/로그에 복사하지 않는다. 운영·브라우저 검증은 이번 증분에서 미실행이다.

최종 production JAR SHA256은 `e2acf6573847a4af2e183831b9b4f1c666e29bc56426faabeed77954e0956cdf`다. 최종 회귀의 extractor/bootJar는 UP-TO-DATE이며 새 실행 성공으로 세지 않는다. bootJar는 앞선 표적/공식 검증에서 새로 생성했다. 직전 SHA `d18216e`의 Linux 실행32는 원래 Flyway3건까지 성공했지만 이번 코드의 동일 SHA 운영 증거는 아니다. 사용한 단발 Node와 Gradle/임시 PostgreSQL은 종료하고 사용자 Word2개를 보존했다.
