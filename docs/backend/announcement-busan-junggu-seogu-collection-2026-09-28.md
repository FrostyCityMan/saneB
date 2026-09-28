# 부산 중구·서구 첨부 수집 연결 — 2026-09-28

## 범위와 근거

HWP1.0.16 추출 개선은 보류한다. 제목 정책 → 공식 상세 식별 → 전체 첨부 발견 → PDF/HWP/HWPX binary 다운로드/signature/hash → 원본 정리만 검증한다. 운영 DB·정책·worker·기존 데이터는 변경하지 않는다.

09-28 15:56:12 읽기 전용 운영 영수증의 활성/미연결 중구 `LGS-000028`, 서구 `LGS-000029`는 `SAFE_SAEOL_EMINWON_COMPACT`다. 같은 영수증을 현재 코드와 대조하며 운영을 새로 조회한 결과가 아니다. 대구 중구·대전 서구 프로필과 별개이며 해당 지역의 기존 예산을 재사용하지 않는다.

- 중구 대표 메뉴 `DOM_000000103001002000`에서 `eminwon.bsjunggu.go.kr` iframe을 확인했다. 상세 form1 POST·table.bbs_vtype·th(colspan4) 제목·첨부파일 th/인접 td(colspan3)다.
- 서구 대표 메뉴 `DOM_000000103001013000`의 최초 GET은302/0byte였다. HEAD는200, 헤더를 함께 확보한 후속 GET은200/110,189byte였고 `eminwon.bsseogu.go.kr` 공식 iframe을 확인했다. 최초302 목적지는 확보하지 못했으므로 원인이나 정상 리다이렉트 여부를 단정하지 않는다. 자동 redirect·TLS 우회·인증은 사용하지 않았다.
- 서구 상세는 form1 POST·바깥 table(width100%/border0/cellspacing1/cellpadding0)·th.w_90 제목/인접 td·첨부파일 th/td다. HTML 주석의 과거 table과 실제 DOM을 구분한다.
- 두 기관 모두 공식 goDownLoad3인자·FileDown.jsp GET이다. 기존 새올 GET 엔진에 고정 bean만 추가하며 엔진·기존 프로필 hash는 변경하지 않는다. 임의 host/path/추가 query·source/parser/hash 불일치를 거절한다. 기존 목록 수집기의7개 상세 query와 실제 응답이 일치함을 확인했다.
- catalog6건은 모두 `expectation:null`이다. 정상 기대값 승인·정책 게시·자동 공고 활성화가 아니다.

## 고정 표본과 예산

|지역|공고|공식 첨부|
|---|---|---|
|중구|29433 청년1인가구 호신용품|HWP1|
|중구|29432 청년 자격증 시험 응시료|HWP1·PDF1|
|중구|29138 청년 프로그램|HWP1|
|서구|36796 청년 창업자 임차료 수정 공고|HWPX3|
|서구|36758 청년 창업자 임차료 재공고|HWPX3|
|서구|36645 청년 창업자 임차료 최초 공고|HWPX3|

서구는 같은 사업의 다른 공고 ID3건이며 서로 다른3개 사업을 검증한 것이 아니다. 변경 공고 사이 동일 binary가 있다면 다운로드 횟수와 고유 파일 수를 구분한다. 현재 신청 가능 여부를 확정하지 않는다.

사전 조사 중구7·서구9회(HEAD1 포함), HTML15개288,991byte와 응답 헤더1개다. 연결7초/전체15초/요청당1MiB, 자동 redirect 없음. 기관별 전체30요청 상한 안에서 공고당6요청/23MiB·파일당20MiB로 실행한다. 조사 포함 최대 중구25·서구27회. 기존 기관의 누적 예산은 변경하지 않는다.

## 검증 명령

```powershell
.\gradlew.bat :test --tests '*BusanJungguSeoguCollectionContractTest' --tests '*SaeolGetAttachmentDiscoveryProfileTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar --no-daemon
.\gradlew.bat :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=BSJUNGGU -PsanebCollectionWindowsTrust=true --no-daemon
.\gradlew.bat :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=BSSEOGU -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
```

저장 HTML 검사는 `SANEB_BUSAN_JUNGGU_SEOGU_SAVED_DETAILS`, 등록 대조는 기존 비식별 운영 영수증 환경변수를 주입한다. Windows-ROOT는 로컬 인증서 저장소 선택이며 TLS 우회가 아니다. 전체 프로젝트/Linux/운영/브라우저 검증을 대신하지 않는다.

## 실제 검증 결과

- 로컬99검사(새 계약6·새올26·catalog64·등록 대조3), 실패/생략0. Node19검사·bootJar 실제 생성 통과. 전체 프로젝트 테스트를 재실행한 것은 아니다.
- 중구 실제3검사 실패/생략0. HWP3·PDF1 총4파일1,075,499byte를 다운로드/signature/hash 검증하고 원본을 정리했다.13요청/7,384,346byte 예약, 조사 포함20/30요청이다.
- 서구 실제3검사 실패/생략0. 각 공고의 전체 첨부3개씩 HWPX9건650,692byte를 다운로드/signature/hash 검증하고 원본을 정리했다.18요청/6,972,751byte 예약, 조사 포함27/30요청이다. 고유 binaryHash는6개이며9개 독립 파일이나3개 독립 사업을 검증한 것으로 표현하지 않는다.
- 합계6공고·첨부 다운로드13건·고유 binary10개·총1,726,191byte다. 실행31요청/14,357,097byte 예약, 조사16회 포함47요청. 예약값은 본문 최대치를 포함하며 실제 전송량과 다르다. 추가 파일 진단/재다운로드는 없었다.
- 중구 프로필 hash=`f34ba1e71cd71f2acc568483a9af1c42e57068b8e640ad637af7e1a068c6d3d0`, 서구=`a30d9feec8dd4f5ac341a97e59b1ef45d22b460e90f0d5f9bf38c0f00e7e3a6b`.
- JUnit hash 중구=`667f06c43c01de6cbad0502f5163bf078a4e0bc3909152df7771cf2eec404134`, 서구=`44091c9a8ea0b5cd5d9cd5b7980f3003f4e0e75960f2a441cc9a5491bf1f124b`. 실행 직후 각각 기록했으며 공통 JUnit 파일은 후속 실행으로 교체된다. 개별 비식별 보고서는 `build/reports/attachment-regional-collection/BSJUNGGU-*.json`, `BSSEOGU-*.json`이다.
- 조사 HTML15개288,991byte와 헤더1개를 삭제했다. 작업용 helper·소유 폴더도 삭제 확인했다. 재확인에는 외부 재조회가 필요하며 기존 사용자 파일은 보존했다.
- 실제 등록33프로필(지역32·기업마당1), catalog83참조/29지역 프로필·보관 기대값1/정상 승인0. 활성223지역 등록32/미등록191, 정책225대상 연결33/미등록192(지역191+정부24)다. 같은15:56 운영 영수증과 최신 로컬 코드를 비교한 결과이지 운영 반영 결과가 아니다.
- [최신 대장](attachment-collection-regional-ledger-2026-09-28.json)은 **15충족/208잔여 = 등록 미완료17 + 미등록191**다. 보관91영수증/최신68공고 재현 통과. 이전13충족 지역의 프로필·근거가 변경되지 않았음을 검증했다. 비활성21지역은 활성화하지 않는다.

이전 북구 signature 실패·해운대 형식 불일치·사상 JPEG 미지원은 계속 미완료다. HWP 추출 개선·운영 정책 게시·ENFORCE·기존 데이터 적용·운영 배포는 하지 않았다. 브라우저는 현재 요청의 명시 지시가 없어 정책상 미실행이다. 이번 성공은 상시 운영 worker·전체 텍스트 분석·정책 QA·장기 Goal 완료가 아니다.
