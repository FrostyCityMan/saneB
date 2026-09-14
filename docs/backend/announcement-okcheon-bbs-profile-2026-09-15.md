# 옥천군 BBS 본문·첨부 모델

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
