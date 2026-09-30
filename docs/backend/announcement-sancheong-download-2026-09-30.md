# 산청 본문·BBS 첨부 연결

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결 단계다. 정상 파일은 수집하고 개별 실패는 별도로 보존한다. HWP 추출기 추가 개선은 보류하며 전체 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E goal은 미완료다.

- [x] 산청 공식 상세·직접 파일 경로와 고정 게시판 계약 구현.
- [x] 현재 목록 형태와 기존 파서의 제목·날짜·상세 URL 해석 회귀 검사.
- [x] 본문·파일 셀 분리, 같은 파일 미리보기 비실행, 정상 파일 보존·오류 분리.
- [x] Java327개·bootJar·Node23개 검사 통과.
- [x] 본문84자·HWPX2개514,232byte 실제 수집·형식 검증과 임시 원본 정리.
- [x] 영수증314건·최신251공고 대장 재현.
- [ ] 첨부 텍스트 추출·전체 분석·운영 상시 수집·운영 E2E는 별도 후속.

## 구현 근거 및 범위

수집원은 `LGS-000238 / SAEOL_GOSI`, 등록 목록은 `https://www.sancheong.go.kr/www/selectBbsNttList.do?bbsNo=118&key=158`이다. 고정 표본은 [2026년 소상공인 소규모 경영환경 개선지원 사업(3차) 공고164021](https://www.sancheong.go.kr/www/selectBbsNttView.do?key=158&bbsNo=118&nttNo=164021)다.

- `SancheongNoticePage`: HTTPS·정확한 호스트/상세 경로·`key=158/bbsNo=118`·숫자 공고번호를 고정한다. 유일한 `table.bbs_default_view` 안의 제목·내용·파일 레이블 셀을 분리하며 중첩 표를 다른 필드로 오인하지 않는다.
- `SancheongAttachmentDiscoveryProfile`: 파일 셀의 `ul.brd_file > li`만 읽고 `/downloadBbsFile.do?atchmnflNo=...` 직접 GET만 승인한다. 파일명·아이콘 alt를 대조하고 동일 파일 ID·파일명의 `/previewBbs.do` 링크는 요청하지 않는다. 최대10파일·UNKNOWN 역할·동일 요청 redirect·중복/미지원/오류 분리를 유지한다.
- 실제 헤더는 `application/octer-stream; charset=UTF-8`, 비ASCII 파일명이며 두 파일 모두 ZIP 선두다. 산청에만 기존 MIME 오타·UTF-8 파일명 호환 옵션을 적용하고 공통 검증기는 변경하지 않는다. 최종 성공은 선두 확인이 아니라 전체 다운로드·형식 검증으로 판정했다.
- 목록 관측 행은 구분/공고번호/제목 링크/부서/날짜 순서다. V29의 `SAEOL_GOSI` 선택기 `table tbody tr`, `td a`, `td:last-child`와 현재 형태를 재현한 테스트에서 제목, `yyyy.MM.dd.` 날짜, 첨부 프로필에 연결되는 canonical 상세 URL을 확인했다. 명칭이 SAEOL이라는 이유만으로 다른 파서로 바꾸지 않는다. 운영 DB의 현재 선택기 값이나 상시 유입은 이번에 조회하지 않았다.
- 카탈로그는 참조용 expectation null이다. migration·DB/API·키워드·추출기·운영 설정·기존 데이터는 변경하지 않는다.

## 검증 결과

`gradlew.bat --no-daemon :test`의 산청 계약, 목록 수집기, 본문, 카탈로그, snapshot, inventory, 파일 형식, worker probe 선택 검사와 `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=SANCHEONG -PsanebCollectionWindowsTrust=true`를 실행했다.

- Gradle 4분22초·종료0, Java327개 통과·실패/오류/생략0, bootJar 성공.
- 2026-09-30 15:09:43 KST: 제목 조합 통과, 본문84자 AVAILABLE/ACCEPTED, 첨부 FOUND/complete, HWPX2개 다운로드·형식 검증 성공.
- 파일1: 428,742byte, SHA-256 `7c83924e75b88bc4aaa672565c8cc478c45d26fbb06de03c42948396a8c042ef`.
- 파일2: 85,490byte, SHA-256 `c73a3dda7ec224b4e2eda79db5e55d61060bcfabe8ad904f805ea3d02f2d83b8`.
- 프로필 지문: `8ae773daa94110f97c4fe7d718ee36318bc2139375cb09f1e32a59caa5a9a1e2`.
- 요청 예약 상한 포함5/6회·3,037,368/24,117,248byte. 관측 임시 원본 정리·운영 쓰기0.
- `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`이며 정책 QA·추출·전체 분석·기대값 승인은 미실행이다.
- Node23개 검사, `verify-collection-receipt-index.mjs`의 영수증314건·최신251공고 재현과 `report-collection-availability.mjs` 집계 통과. 기존 타 지역 근거·프로필 지문을 보존했다.
- **173/223수집원(77.6%) 확인·50잔여(미연결22+등록 다운로드 미확인28)**다. 프로필202개(지역201+기업마당1), 카탈로그260공고/201대상/참조259+기존 기대값1이다. 전체 첨부 세트 엄격 Gate16/223은 그대로다.

## 조사 요청 및 다음 지역

별도 구조 조사9GET: 산청 상세·파일 선두2개·목록4, 의령 등록 주소·공식 이동·세션 없는 목록·검색·상세5회다. 요청당12초·수집기 User-Agent·TLS 검증·자동 redirect 금지를 유지했고 관측된 같은 사이트 공식 이동만 명시적으로 따라갔다. 조사 원문 파일을 저장하지 않았고 Java 관측 예산은 별도다.

의령 `LGS-000232 / HEURISTIC_NOTICE`의 등록 주소는 `/index.uiryeong?menuCd=DOM_000000203003000000`이며 공식302가 게시판 목록을 가리킨다. 이동 응답의 익명 세션 경로는 대장·소스에 저장하지 않는다. 세션 없는 `/board/list.uiryeong?boardId=BBS_0000070&menuCd=DOM_000000203003001001`도 HTTP200이다. 공식 폼의 `searchType=DATA_TITLE`, `keyword` 검색으로 [2026년 의령군 소상공인 육성지원 사업 공고35341](https://www.uiryeong.go.kr/board/view.uiryeong?boardId=BBS_0000070&menuCd=DOM_000000203003001001&startPage=1&dataSid=316445&gosiNo=35341)를 찾았다.

의령 상세는 `div.boardViewWrap`, `p.bdvTit`, `div.bdvCntWrap` 구조다. 실제 공고 식별에 `gosiNo`가 필요하며 목록의 여러 링크가 같은 dataSid를 사용한다. 공식 `eminwon.uiryeong.go.kr/emwp/jsp/ofr/FileDown.jsp` HWP 링크의 query 시작 부분에 탭이 관측됐고, `/customuser/synap.uiryeong`은 미리보기다. 아직 파일 다운로드 전이며 고정 링크 구조·공고 식별·탭 정규화를 검증해야 한다. 페이지 하단의 공공누리 제4유형 표시는 운영 재사용 검토 후속으로 기록한다.

운영 DB·정책·worker·ENFORCE·배치·배포 변경은 없다. 브라우저는 사용자 정책상 미실행이다. AWS 인증 갱신과 이전 연제·구례 조사 원본 정리 미완료는 별도 후속이다.
