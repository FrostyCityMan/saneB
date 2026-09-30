# 화성 본문·첨부 연결 및 목록 범위 후속

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드를 우선한다. 정상 파일은 보존하고 개별 오류는 분리한다. HWP 추출기 추가 개선은 보류하며 전체 worker·DB/API/UI·운영 E2E goal은 미완료다.

- [x] `LGS-000088 / SAFE_HWASEONG_LEGAL_NOTICE`의 제목·본문·첨부 셀 분리.
- [x] 고정 공고 141781의 본문 429자·HWP 1개 다운로드 확인.
- [x] 선택 Java 테스트 338개, bootJar, Node 테스트 23개 통과.
- [x] 기존 기록 보존과 326개 관측 기록·262개 최신 공고 재현.
- [~] 223개 활성 지역 수집원 중 182개 다운로드 확인(81.6%), 41개 잔여.
- [ ] 일반공고 상시 유입을 위한 등록 목록 범위 보정.
- [ ] 첨부 추출·worker 영속화·DB/API·관리자 최종 검증·운영 E2E 후속.

## 구현과 계약

`HwaseongNoticePage`는 `www.hscity.go.kr/www/gosi/BD_selectNoticeDetail.do`의 숫자 공고번호를 검증하고 `div.board_write > table`의 제목·내용·첨부파일 셀을 분리한다. 본문 안의 중첩 표는 메타데이터 행으로 취급하지 않는다. 상세 주소는 기존 DB 템플릿과 동일하게 공고번호만 남긴다. 공식 범주 01/04가 붙은 주소도 동일 상세로 정규화하며 추가 query·다른 호스트·HTTP·경로 변조는 허용하지 않는다.

`HwaseongAttachmentDiscoveryProfile`은 공식 첨부 셀의 `div.file_down > ul > li > a`와 `goDownLoad`의 세 문자열 인자만 해석한다. 기존 새올 파일 검증기로 `eminwon.hscity.go.kr/emwp/jsp/ofr/FileDown.jsp`의 GET을 검증한다. JavaScript를 실행하지 않으며, 미리보기·바로듣기 버튼은 동일 공고·동일 파일일 때만 부속 요소로 인정하고 요청하지 않는다.

잘못된 미리보기·미해석 링크·미지원 파일은 별도 상태로 기록하며 다른 정상 파일 descriptor를 버리지 않는다. 최대 10파일, PDF/HWP/HWPX, UNKNOWN 역할, 동일 요청 경계와 기존 TLS·용량·시간 제한을 유지한다. 실제 파일은 HWP만 확인했으며 PDF/HWPX는 합성 계약 테스트 범위다.

기존 migration(V85까지), DB/API 계약, 제목 A/B 규칙, HWP 추출기, 운영 설정은 변경하지 않았다. 프로필은 211개(지역210+기업마당1), 카탈로그는 271공고/210대상/참조270+기존 기대값1이다.

## 실제 관측

2026-09-30 18:11:40 KST, `HWASEONG-141781`(2026년 화성시 소상공인 자금지원사업):

- 제목 `COMBINATION_MATCHED`, 본문 `AVAILABLE / ACCEPTED`, 429자.
- 첨부 발견1·다운로드1·실패0. HWP 189,440byte.
- 파일 SHA-256: `3af1dc13d92fa21ccf3e54f6df82105cb6d97fb771b855a01892bb5eb189b1d9`.
- 프로필 지문: `3631512c4e1cb61e6cd8a2e7a4b30d2ea789e5897c5f091ce6b3fba2ad6bc073`.
- 보고서: `build/reports/attachment-regional-collection/HWASEONG-141781.json`.
- 요청 예약 상한 사용4/6회, 예약 바이트2,802,688/24,117,248.
- 원본 정리=true, 운영 쓰기=0, 추출·정책 QA·기대값 승인·전체 텍스트 분석=false.

수집원 분모는 2026-09-28 inventory다. 다운로드 관측은 182/223, 잔여41(미연결13+등록 다운로드 미확인28), 현재 지문 기준 오류 지역45(성공 지역과 중복)다. 엄격한 전체 세트 Gate는 16/223으로 유지한다. 다운로드 1개 확인을 상시 운영·전체 Gate 완료로 해석하지 않는다.

## 목록 범위와 다음 대상

기존 등록 `BD_notice.do`는 고시 범주01, 지원사업이 있는 공식 `BD_selectGosiList.do`는 일반공고 범주04다. 공고번호만 사용하는 기존 상세 템플릿은 이번 표본에서 정상 동작했다. 따라서 상세 연결은 완료했지만 일반공고가 상시 목록 수집으로 유입된다는 증거는 아니다. 등록 수집 주소 보정과 목록→상세 회귀 검증은 후속이며 운영 DB를 변경하지 않았다.

이번 구조 조사 4GET은 화성 상세2회(범주 포함/미포함), 울주 고시 목록1회, 광양 고시 목록1회다. 요청당 12초·2MiB·TLS 검증·수집기 User-Agent·자동 redirect 금지를 유지하고 HTML 원문을 저장하지 않았다. 울주의 일반공고 메뉴와 광양의 공고 메뉴를 다음 연결 대상으로 확인했다. 이 두 지역은 다운로드 성공에 포함하지 않는다.

## 검증과 한계

- 집중 테스트: `gradlew.bat --no-daemon :test --tests '*HwaseongDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest'`, 147개 통과.
- 범위 검증: `:test`에 Hwaseong 계약·목록 collector·본문 client·catalog·policy snapshot·inventory·file type·worker probe를 선택하고 `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=HWASEONG -PsanebCollectionWindowsTrust=true` 실행. 3분33초 성공, 선택 테스트338개·실패/오류/생략0. 일시정지 요청 시 이미 종료되어 결과를 재실행하지 않고 확인했다.
- `node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs`: 23개 통과.
- `node scripts/qa/verify-collection-receipt-index.mjs`: 기존 기록 해시·샘플 보존, 326개 기록·262개 공고 재현.
- `node scripts/qa/report-collection-availability.mjs`: 182확인·41잔여 재현.
- `git -c core.autocrlf=false diff --check`: 통과.

브라우저는 사용자 정책상 미실행이다. AWS 인증 만료와 이전 연제·구례 조사 파일 정리 미완료는 별도 후속으로 유지한다. 운영 반영·정책 활성화·재분류는 이번 단계에서 수행하지 않았다.

## 재개 요청 후 다음 지역 조사

화성 코드·대장 커밋 `4c98d4353557d433d9d0312fcaedd38d6ce64905`를 QA 브랜치에 푸시하고 원격 SHA 일치를 확인했다. 해당 SHA의 Actions 조회에서 실행이 반환되지 않아 CI 통과로 표시하지 않는다.

추가 공개 조사 7회(5GET+2POST)를 수행했다. 앞의 화성 연결 조사4회와 별도이며 요청당 12초·2MiB·TLS 검증·수집기 User-Agent·자동 redirect 금지, 조사 원문 파일 저장0을 유지했다.

1. 울주 일반공고 메뉴 `https://www.ulju.ulsan.kr/ulju/contents.do?mId=0403020000`는 범주04 목록으로 302 이동한다. 이동의 일회성 값은 출력 전에 가리고 저장하지 않았다. 일회성 값을 제외한 `/ulju/saeol/gosi/list.do?seCode=04&mId=0403020000`은 200이다.
2. 울주 공식 목록 폼의 공개 검색 POST(`page=1`, `seCode=04`, `searchType=tit`, 소상공인 검색)는 200, 지원 공고67082·67015·66105를 반환했다. 상세 링크는 `data-action`에 있고 목록 JavaScript는 `req.post`를 사용한다.
3. `https://www.ulju.ulsan.kr/ulju/saeol/gosi/view.do?notAncmtMgtNo=67082&mId=0403020000`는 GET200이다. 제목은 2026년 제2차 울주군 소상공인 자금 특례보증 지원 공고(수정)이며 `form#detailForm > div.bod_wrap > div.bod_view` 안의 `h4`, `div.view_cont`, `dl.view_file`로 제목·본문·첨부가 나뉜다.
4. 울주 공식 파일 목록은 `ul#updateFileList > li`다. 내려받기 `goDownload_location`의 세 문자열 인자로 `https://eminwon.ulju.ulsan.kr/emwp/jsp/ofr/FileDown.jsp` GET을 구성한다. `downloadToHttp`는 별도 미리보기이므로 요청 대상이 아니다. HWPX 표시1개를 발견했지만 아직 다운로드·형식 검증하지 않았으며 성공 수에 포함하지 않는다.
5. 광양 공고 메뉴 `https://gwangyang.go.kr/menu.es?mid=a10909020000`는 `/saeol/gosi.es?mid=a10909020000&type_code=02,04`로 302 이동하며 해당 목록은 200이다. 공식 검색 폼의 공개 필드 `type_code=02,04`, `nPage=1`, `keyField=T`, 소상공인 검색 POST는 404였다. 원인은 미확정이며 검색 실패를 상세/첨부 실패로 확대하지 않는다. 인증·보안 우회나 반복 재시도는 하지 않았다.

울주 역시 등록된 고시 범주01과 이번 일반공고 범주04가 다르다. 후속 구현은 고정 상세 본문·첨부 연결부터 진행하되, 목록 입력·카테고리 범위와 상시 수집은 별도로 검증해야 한다. 현재 다운로드 확인182·잔여41은 변하지 않는다.
