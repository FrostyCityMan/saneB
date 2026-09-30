# 광양 본문·공개 POST 첨부 연결

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드를 우선한다. 정상 파일은 수집하고 개별 실패는 분리한다. HWP 추출기 개선은 보류하며 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E 전체 goal은 미완료다.

- [x] `LGS-000182 / SPRING_BBS` 공식 본문·첨부 셀과 공개 파일 POST 연결.
- [x] 목록 collector가 저장하는 canonical URL 및 식별자 계약 검증.
- [x] 공고61869 본문620자·HWPX1개101,815byte 다운로드·형식 검증·임시 원본 정리.
- [x] 집중 Java149개, 회귀 선택 Java354개, bootJar, Node23개 통과.
- [x] 기존327기록 보존과328관측 기록·264최신 공고 재현.
- [~] 184/223수집원 다운로드 확인(82.5%), 39잔여(미연결11+등록 다운로드 미확인28).
- [ ] 청주150057 공식 PDF POST 첨부 연결. 천안 목록403은 별도 오류로 유지.
- [ ] 등록 고시/일반공고 범위와 상시 유입, 추출·DB/API/UI·운영 E2E 후속.

## 구현과 계약

`GwangyangNoticePage`는 HTTPS `gwangyang.go.kr/saeol/gosi.es`의 숫자 공고번호·act=view·고정 메뉴/범주 조합을 검증한다. 고시 메뉴 a10909010000/범주01과 공고 메뉴 a10909020000/범주02,04를 구분하며 임의 메뉴·query는 허용하지 않는다. 공식 `div.p-wrap.bbs.bbs_view > table.p-table.block` 안의 `th.bbs_tit`, `td.view_content`, `td.view_file`로 제목·본문·첨부를 분리한다. 본문 중첩 표는 바깥 표의 메타데이터 셀로 오인하지 않는다.

기존 `AnnouncementSourceIdentityNormalizer`는 쉼표 query를 canonical URL에 추가 인코딩한다. 실제 collector와 같은 `canonicalizeUrl(rawUrl)` 결과를 sourceUrl로, 그 문자열의 hash를 providerNoticeId로 사용하는 fixture를 추가했다. 프로필은 이 정확한 저장 형태와 기존 raw URL 입력을 검증한 뒤 고정 상세 요청으로 정리한다. `02%2C04`로 한 겹 추가 인코딩된 범주만 복원하며 임의 재귀 디코딩은 하지 않는다. 공통 정규화기나 기존 식별자·DB 값을 변경하지 않았다. 저장 형태 URL도 실제 HTTP200/같은 제목·파일 셀을 반환하는지 확인했다.

`GwangyangAttachmentDiscoveryProfile`은 공식 첨부 셀의 다운로드 링크와 `form[name=nnn]`만 사용한다. 폼은 HTTPS `eminwon.gwangyang.go.kr/emwp/jsp/ofr/FileDownNew.jsp`의 POST이며 빈 hidden 필드 user_file_nm/sys_file_nm/file_path 3개만 허용한다. 링크의 `goDownLoad` 세 문자열 인자를 선형 해석하고 스크립트는 실행하지 않는다. 기존 새올 GET 검증기는 파일명·경로 검사에만 재사용하며 GET 전송을 실행하지 않는다.

폼 변경은 파일 요청 전에 분리하고, 개별 링크 오류·미지원 형식은 다른 정상 descriptor를 지우지 않는다. 10파일 제한·중복 상충·빈 첨부·구조 변경·다른 호스트/공고/파일 요청 전환을 테스트했다. 파일 역할은 UNKNOWN이다. 실제 파일 검증은 HWPX1개이며 PDF/HWP는 합성 계약 테스트 범위다.

프로필213개(지역212+기업마당1), 카탈로그273공고/212대상/참조272+기존 기대값1이다. migration V85까지와 DB/API·A/B 분류 규칙·HWP 추출기·공유 엔진 계약은 변경하지 않았다.

## 실제 관측

2026-09-30 18:45:57 KST, `GWANGYANG-61869`(2026년 광양시 소상공인 융자금 이차보전 계획 공고(2차)):

- 제목 COMBINATION_MATCHED, 본문 AVAILABLE/ACCEPTED620자.
- 발견1·다운로드1·실패0, HWPX101,815byte.
- 파일 SHA-256: `2661e4b3117c15019e170932a23cec4f3692a2141a826e06a663e5c8bb6eab7c`.
- 프로필 지문: `83f59ac92c8808984f34db3ddee3a29e7871ce912b4d8de0db3790fe055b2a45`.
- 보고서 `build/reports/attachment-regional-collection/GWANGYANG-61869.json`.
- 예약 요청4/6회·예약 바이트2,379,191/24,117,248.
- 원본 정리=true·운영 쓰기=0·추출/정책 QA/기대값 승인/전체 텍스트 분석=false.

분모는2026-09-28 inventory다. 다운로드 확인184/223·잔여39, 현재 지문 기준 오류 지역45는 성공 지역과 중복된다. 엄격한 전체 세트 Gate16/223은 유지한다. 고정 파일 관측을 상시 운영 완료로 집계하지 않는다. 등록 목록은 고시 범주01, 이번 지원사업은 공고 범주02,04이므로 일반공고 상시 유입은 별도 점검이 필요하다.

## 다음 대상 조사

추가 공개 조사6GET: 광양 상세 구조/저장 URL2회, 청주 목록/검색/상세3회, 천안 등록 목록1회. 요청당12초·2MiB·수집기 User-Agent·TLS 검증·자동 redirect 금지를 유지하고 조사 원문 파일은 저장하지 않았다.

- 청주 등록 `https://www.cheongju.go.kr/www/selectEminwonNoticeList.do?key=281`과 공식 검색 필드 searchKrwd의 소상공인 검색은200. 기본 검색은 전체 항목이며 지원 공고150057 등을 확인했다.
- 청주150057(2026년도 청주형 소상공인 육성자금 지원 공고)의 `selectEminwonNoticeView.do?key=281&nowDongGn=&notAncmtSeCd=&notAncmtMgtNo=150057`은200. 공식 영역은 `div#board.p-wrap.bbs.bbs__view`, 표 `table.bbs_basic`, 파일 링크 `a.p-attach__link`다. PDF 표시1개와 `form2`의 세 공개 필드를 `https://eminwon.cheongju.go.kr/emwp/jsp/ofr/FileDown.jsp`로 submit하는 함수를 확인했다. 정확한 폼 method·본문/첨부 경계는 후속에서 확인한다. 메뉴와 푸터의 별도 PDF는 공고 첨부로 취급하지 않는다. 실제 다운로드 전이다.
- 천안 등록 `https://www.cheonan.go.kr/prog/saeolGosi/GOSI/kor/sub02_02_01/list.do`는403. 원인은 미확정이며 우회·반복 재시도 없이 별도 오류로 남겼다.

이전 광양 검색 POST404 기록은 보존한다. GET 검색과 파일 POST 성공이 과거 검색 요청 실패를 삭제하지 않는다.

## 검증과 미실행 범위

- `gradlew.bat --no-daemon :test --tests '*GwangyangDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest'`:54초,149개·실패/오류/생략0.
- 회귀 선택: Gwangyang/Ulju/Hwaseong 계약·목록 collector·본문 client·catalog·policy snapshot·inventory·file type·worker probe. `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=GWANGYANG -PsanebCollectionWindowsTrust=true`와 함께3분22초 성공,354개·실패/오류/생략0. 전체 suite 통과로 확대하지 않는다.
- Node stage/receipts/availability 테스트23개 통과.
- `node scripts/qa/verify-collection-receipt-index.mjs`:328기록/264공고 재현. 이전327기록 해시·상태·샘플과 광양 외 지역 결과 불변 확인.
- `node scripts/qa/report-collection-availability.mjs`:184확인·39잔여 재현.
- `git -c core.autocrlf=false diff --check`:통과.

운영 DB·설정·worker·정책·ENFORCE·기존 데이터·배포는 변경하지 않았다. 브라우저는 사용자 정책상 미실행이다. AWS 인증과 이전 연제·구례 조사 원본 정리 미완료는 별도 후속으로 유지한다.
