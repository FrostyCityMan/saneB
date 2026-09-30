# 청주 본문·공개 POST 첨부 연결

## 현재 단계 / Gate

전 지역의 첨부 발견·다운로드 연결을 우선한다. 정상 파일은 수집하고 실패는 별개 오류로 보존한다. HWP 추출기 개선은 보류하며 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E 전체 goal은 미완료다.

- [x] `LGS-000136 / SAEOL_GOSI` 공식 본문·첨부 영역과 PDF 공개 POST 연결.
- [x] collector 저장 canonical URL·공고 식별자와 고정 상세 요청의 호환 확인.
- [x] 청주150057 본문65자·PDF1개276,910byte 실제 수집, 형식 검증·임시 원본 정리.
- [x] 집중 Java151개, 선택 회귀 Java363개, bootJar, Node23개 통과.
- [x] 기존328기록 보존,329관측 기록·265최신 공고 재현.
- [~] 185/223수집원 다운로드 확인(83.0%),38잔여(미연결10+등록 다운로드 미확인28).
- [ ] 이천70639·영광29278 본문·첨부 연결. 링크 확인을 다운로드 성공으로 집계하지 않는다.
- [ ] 상시 유입·첨부 추출·DB/API/UI·전체 운영 E2E 후속.

## 구현과 계약

`CheongjuNoticePage`는 HTTPS `www.cheongju.go.kr/www/selectEminwonNoticeView.do`와 고정 key281, 빈 nowDongGn/notAncmtSeCd, 숫자 notAncmtMgtNo를 검증한다. 공식 `div#board.p-wrap.bbs.bbs__view > table.bbs_basic` 안의 제목·내용·파일 행을 사용하며 본문의 중첩 표는 메타데이터로 오인하지 않는다. 메뉴·푸터의 별도 PDF는 공고 첨부로 취급하지 않는다.

`CheongjuAttachmentDiscoveryProfile`은 공식 `form2`의 POST와 빈 hidden 필드 user_file_nm/sys_file_nm/file_path3개, 파일 셀의 `ul.p-attach > li.p-attach__item > a.p-attach__link`만 연결한다. `goDownLoad`의 문자열 인자3개를 해석하고 JavaScript는 실행하지 않는다. 다운로드는 고정 HTTPS `eminwon.cheongju.go.kr/emwp/jsp/ofr/FileDown.jsp`로 전송한다. 기존 새올 GET 검증기는 파일 경로·이름 검증에만 재사용하고 GET 전송은 하지 않는다.

파일10개 상한·중복 상충·미지원 형식·잘못된 링크·폼 변경·경로 및 호스트 변경을 구분한다. 개별 오류나 미확인 장식이 있어도 이미 검증한 정상 descriptor는 유지한다. 파일 역할은 UNKNOWN이다. PDF만 실제 파일로 검증했고 HWP/HWPX는 합성 계약 테스트 범위다.

첫 집중 실행에서151개 중3개가 실패했다. 잔여 링크 검사에 li 컨테이너 자신이 포함되어 정상 첨부도 미완료로 표시되는 원인이었다. 컨테이너 자신만 제외하고 하위 잔여 요소 검사는 유지하여 재실행151개를 통과했다. 실패를 없애기 위해 테스트를 제거하거나 완료 조건을 완화하지 않았다.

등록 프로필214개(지역213+기업마당1), 카탈로그274공고/213대상/참조273+기존 기대값1이다. DB/API·migration V85까지·A/B 규칙·HWP 추출기·공통 정규화기는 변경하지 않았다.

## 실제 관측

2026-09-30 19:07:28 KST, `CHEONGJU-150057`(2026년도 청주형 소상공인 육성자금 지원 공고):

- 제목 COMBINATION_MATCHED, 본문 AVAILABLE/ACCEPTED65자.
- 발견1·다운로드1·실패0, PDF276,910byte.
- 파일 SHA-256: `9abe2d88d68e5f32efc280e11bb49a8a17bbe6231026513592c71f55540ce39e`.
- 프로필 지문: `416704ccaa2bc6811441924e844b6b60a46a5e538daabf9f530707b3cf66d326`.
- 보고서 `build/reports/attachment-regional-collection/CHEONGJU-150057.json`.
- 예약 요청4/6회, 예약 바이트2,595,246/24,117,248.
- 원본 정리=true, 운영 쓰기=0, 추출/정책 QA/기대값 승인/전체 텍스트 분석=false.

분모는2026-09-28 15:56:12 KST inventory의 활성 지역 수집원223개다. 다운로드185확인·38잔여, 현재 지문 기준 오류 지역45는 성공 지역과 중복된다. 엄격한 전체 세트 Gate16/223은 유지한다. 고정 공고1개의 실파일 관측을 상시 운영·텍스트 추출 완료로 표현하지 않는다.

## 다음 대상 조사

추가 공개 조사11회(GET9·POST2), 요청당12초·2MiB·수집기 User-Agent·TLS 검증·자동 redirect 금지. 조사 원문 파일은 저장하지 않았고 인증·세션 값은 보관하지 않았다.

- 청주1GET:150057 상세의 공식 제목·내용·파일 행과 폼 구조 확인.
- 이천6회(GET4·POST2): 등록 메뉴 `/portal/contents.do?mid=0402000000`는 고시01 목록으로302, 공식 일반공고 메뉴 mid0402020000도302다. 일회성 query 값을 쓰지 않은 고정 목록은 각각200. 일반공고 공식 POST 검색(searchType=tit,searchTxt=소상공인)200에서70639를 확인했다. 상세 POST `https://www.icheon.go.kr/portal/saeol/gosi/view.do?notAncmtMgtNo=70639&mid=0402020000`200, 제목은 `2027년 지방보조금 지원계획 (소상공인 지원) 공고`다. `form#detailForm` 안 bod_wrap/bod_view/view_cont, `ul#updateFileList`와 HWPX1개 표시를 확인했다. 공식 goDownload 함수는 `https://eminwon.icheon.go.kr/emwp/jsp/ofr/FileDown.jsp`에 세 공개 query 필드를 보내는 GET이다. 별도 direct-download 폼과 미리보기는 공고 파일 요청으로 단정하지 않는다. 등록 고시/일반공고 목록 범위는 상시 유입 후속이다.
- 영광4GET: 등록 목록200, sc_word만 사용한 검색에서는 일치 행을 찾지 못했고 공식 sc_key=subject와 함께 재조회하여29278을 확인했다. 상세200에서 `2026년 하반기 영광군 소상공인 소규모 경영환경 개선사업 신청자 모집 공고.hwpx` 링크를 확인했다. 상세 URL은 등록 `/bbs/`의 b_id=gosigonggo,site=headquarter_new,mn=9059에 type=view,bs_idx=29278과 검색 조건을 사용했다. 제목 정책 통과와 첨부 영역·실제 다운로드는 후속 검증 전이다.

## 검증과 미실행 범위

- `gradlew.bat --no-daemon :test --tests '*CheongjuDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest'`: 최초1분1초151개 중3실패, 수정 후39초151개·실패/오류/생략0.
- 회귀 선택: Cheongju/Gwangyang/Ulju/Hwaseong 계약, 목록 collector, 본문 client, catalog, policy snapshot, inventory, file type, worker probe. `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=CHEONGJU -PsanebCollectionWindowsTrust=true`와 함께5분6초 성공.363개·실패/오류/생략0이며 전체 suite 통과로 확대하지 않는다.
- Node stage/receipts/availability 테스트23개 통과.
- `node scripts/qa/verify-collection-receipt-index.mjs`:329기록/265공고 재현. 이전328기록 해시·상태·샘플과 청주 외 지역 결과 불변 확인.
- `node scripts/qa/report-collection-availability.mjs`:185확인·38잔여 재현.

운영 DB·설정·worker·정책·ENFORCE·기존 데이터·배포는 변경하지 않았다. 브라우저 검증은 사용자 정책상 미실행이다. AWS 인증과 이전 연제·구례 조사 원본 정리 미완료는 별도 후속으로 유지한다.
