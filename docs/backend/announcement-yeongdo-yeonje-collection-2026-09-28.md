# 영도구 첨부 연결·연제구 접근 차단 기록 — 2026-09-28

## 범위와 판정

HWP1.0.16 추출 개선은 보류한다. 공식 상세 식별 → 첨부 전체 발견 → PDF/HWP/HWPX binary 다운로드·signature·hash → 원본 정리만 검증한다. 텍스트 추출·운영 DB·정책·worker·기존 데이터·배포는 변경하지 않았다.

- [x] 영도 `LGS-000031 / SCMS_CARD_NOTICE` 고정 프로필 구현 및 로컬 계약 검증.
- [x] 영도 공식 공고3건의 첨부11개 발견, 실제7개 다운로드/signature/hash 검증.
- [!] 영도 나머지4첨부 HTTP400. 공고2건 미완료이므로 지역 수집 Gate 미충족.
- [!] 연제 `LGS-000040 / SAEOL_GOSI` 공식 검색 POST HTTP403. 첨부 프로필 미등록 유지.
- [ ] 전 지역 완료: 활성223지역 중 **15충족/208잔여 = 등록 미완료18 + 미등록190**.

09-28 15:56:12.116778+09:00 운영 읽기 전용 영수증과 최신 로컬 코드를 대조했다. 이번 운영 재조회나 운영 반영 결과가 아니다. 비활성21지역은 변경하지 않는다.

## 확인한 공식 구조

영도 목록 `/00000/00007/00013.web`은 SCMS 카드형이다. 목록의 기본 상세 링크는 `amode=view / not_ancmt_mgt_no / type=A`다. 공식 제목 검색 결과로 표본을 선정하고 기본 상세 주소에서 같은 공고 제목을 재확인했다. 검색 조건·페이지 query를 포함하는 임의 주소까지 허용하지 않는다.

상세는 `form#saeolGosiVO[name=saeolGosiVO][method=get] > div.bbs1view1`이며 제목은 `h1.h1`, 첨부는 해당 뷰의 `div.attach1 > ul > li > a.filename`이다. 메뉴·만족도 양식·본문 링크는 첨부로 취급하지 않는다. 공식 링크의 `eminwon.yeongdo.go.kr/emwp/jsp/ofr/FileDown.jsp` GET과 파일명·서버파일명·날짜별 경로만 검증한다. JavaScript 실행·임의 host/path/query·HTTP 강등·다른 공고/파일 redirect를 허용하지 않는다.

첨부 영역 전체를 검사하고 미해결 링크·구조 변경·미지원 확장자를 숨기지 않는다. 표시명과 다운로드 이름을 대조하고, 같은 파일의 충돌과10개 초과를 차단한다. 파일 역할은 모두 UNKNOWN이다. HWP도 binary 수집만 하며 추출기를 실행하지 않았다. 새 프로필은 독립 클래스여서 기존 지역 프로필 hash를 변경하지 않는다.

연제 공식 메뉴 `/portal/contents.do?mId=0206030000`은302였다. 두 번째 요청에서 동일 기관의 `/portal/saeol/gosi/list.do?mId=0206030000` 목적지를 확인한 뒤 해당 목록을 GET하여200을 확인했다. 목록의 공식 검색 form은 POST이며 `page=1 / seCode=01 / searchType=tit / searchTxt=지원` 요청은403이었다. 자동 redirect·로그인·차단 우회·추측한 대체 엔드포인트는 사용하지 않았다. 상세·첨부 다운로드는 미실행이며403 원인은 확정하지 않는다.

## 고정 표본의 실제 결과

|영도 공고|발견|다운로드 검증|미완료|
|---|---|---|---|
|36435 소상공인 보증료|HWPX3|3개 전체 성공|없음|
|36164 청년 면접수당 수정 공고|HWPX4|2개 성공|2개 HTTP400|
|35633 청년 면접수당 최초 공고|HWP4|2개 성공|2개 HTTP400|

청년 면접수당2건은 같은 사업의 서로 다른 공고 ID이며 독립 사업2개로 표현하지 않는다. 합계11개 중7개(고유 binary7개),546,686byte를 검증했다. 실패4개는 `FILE_DOWNLOAD / ATTACHMENT_HTTP_400`이며 signature 성공·추출 성공으로 대체하지 않는다. 링크 인코딩 처리와 원격 응답 원인 사이의 인과는 이번 관측만으로 확정하지 않았다. 같은 오류를 반복 요청하거나 성공 표본으로 교체하지 않았다.

조사 영도5회·연제4회, HTML9개1,110,837byte와 헤더1개653byte를 수신했다. 연결7초/요청15초/1MiB 상한, 자동 redirect 없음. 영도 실제 검증은 공고당7요청·23MiB/파일20MiB 상한으로20요청·7,206,782byte를 예약했다. 조사 포함 **영도25/30, 연제4/30**이며 본문 예약 최대치를 포함하므로 실제 전송량과 다르다. 추가 파일 진단·재다운로드는 없었다.

조사 원본10개1,111,490byte는 삭제했고, 검증 작업의 상세·binary 원본도 각 실행에서 정리됐다. 원본 재확인에는 외부 재조회가 필요하다. 비식별 실행 보고서와 hash만 남긴다.

## 검증 명령과 근거

```powershell
.\gradlew.bat :test --tests '*YeongdoCollectionContractTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar --no-daemon
.\gradlew.bat :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=YEONGDO -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
```

저장 HTML 검사에는 `SANEB_YEONGDO_SAVED_DETAILS`, 등록 대조에는 기존 비식별 운영 영수증을 주입했다. Windows-ROOT는 로컬 신뢰 저장소이며 TLS 검증을 끄지 않았다.

- 로컬 초기73검사 중1실패: POST 금지 테스트가 빈 form을 구성하여 Request 생성자에서 먼저 거절됨. 유효한 POST 입력으로 수정 후73검사(계약6·catalog64·등록3), 실패/생략0. bootJar 생성 통과.
- 실제 collection-only3검사 중1성공·2실패·생략0. 외부 파일 실패를 로컬 계약 검사 성공으로 덮지 않는다.
- Node19검사 통과. 보관94영수증/최신71공고 재현 통과, 이전15충족 지역의 프로필·근거를 deepEqual 검증했다.
- 프로필 `LOCAL_YEONGDO_SCMS_GET_V1` hash=`4946b945448284bed947a415159bcc212dbe52de9f819643400041ec1a8fa836`.
- 실제 JUnit hash=`8406d5219f9fcf34ddf42133b2f3c5046d474ea4671cb02412e73aa15aa1d1f4`. 개별 보고서는 `build/reports/attachment-regional-collection/YEONGDO-*.json`; 후속 실행 전 예산과 기존 영수증을 확인한다.
- [수집 대장](attachment-collection-regional-ledger-2026-09-28.json): 등록34프로필(지역33·기업마당1), catalog86참조/30지역 프로필·보관 기대값1/정상 승인0. 정책225대상 연결34/미등록191(지역190+정부24). 활성223지역 수집15충족/208잔여다.

전체 프로젝트/Linux/운영 브라우저 E2E는 실행하지 않았다. 브라우저는 현재 요청의 명시 지시가 없어 정책상 생략했다. 이번 연결 추가는 운영 배포·정책 게시·ENFORCE·상시 worker 검증·전체 Goal 완료가 아니다. 다음에는 미등록 지역을 계속 연결하고, 영도400·연제403은 잔여 예산과 원인 확인 경로를 별도로 관리한다.
