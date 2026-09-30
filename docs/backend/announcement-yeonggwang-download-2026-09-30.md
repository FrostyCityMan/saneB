# 영광 본문·첨부 GET 연결과 제목 중단 분리

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결을 우선한다. 정상 파일은 유지하고 개별 오류는 별도 기록한다. HWP 추출기 개선은 보류하며 전체 장기 goal의 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E는 미완료다.

- [x] `LGS-000195 / SPRING_BBS` 공식 본문·첨부 GET 연결.
- [x] collector 저장 URL의 /bbs 정규화 형태와 검색 포함 링크의 identity 호환 검증.
- [x] 29278 제목 조합 미충족/요청0과29330 적격 공고 실수집을 별도 관측.
- [x] 29330 본문277자·HWPX1개133,205byte 다운로드·형식 검증·임시 원본 정리.
- [x] 집중 Java154개·선택 회귀 Java382개·bootJar·Node23개 통과.
- [x] 기존330기록 보존,332관측 기록·268최신 공고 재현.
- [~] 다운로드 확인186/223수집원(83.4%),37잔여(미연결8+등록 다운로드 미확인29).
- [ ] 경상북도 새 공식 게시판·스크립트 내 파일 정보 연결.
- [ ] 상시 유입·텍스트 추출·전체 운영 E2E 후속.

## 구현과 계약

`YeonggwangNoticePage`는 HTTPS `www.yeonggwang.go.kr`의 /bbs와 /bbs/ 경로를 허용한다. b_id=gosigonggo,site=headquarter_new,mn=9059,type=view,숫자 bs_idx를 고정하고 공식 제목 검색의 sc_key=subject/sc_word 쌍만 선택적으로 허용한다. 검색 조건은 상세 재요청에 전달하지 않는다. collector가 trailing slash를 제거한 저장 URL도 실제 HTTP200/같은 제목을 반환하는지 확인했다.

공식 `div#board_view > table`에서 제목·첨부 th행과 td.leftcell.rightcell 내부 div.board_view_contents를 분리한다. 본문 안의 중첩 표, 담당부서·메뉴·첨부 이름은 메타데이터 또는 본문으로 오인하지 않는다. 첨부 행이 없는 구조는 미확인 오류이며 빈 첨부 셀과 구분한다.

`YeonggwangAttachmentDiscoveryProfile`은 첨부 셀의 직접 a링크에서 `goDownLoad`의 세 문자열 인자만 선형 해석한다. JavaScript는 실행하지 않는다. 고정 HTTPS `eminwon.yeonggwang.jeonnam.kr/emwp/jsp/ofr/FileDown.jsp`로만 GET하며 기존 새올 파일 경로·파일명 검증기를 재사용한다. 원문 저장 문자열 hash와 기존 raw URL의 canonical hash를 모두 확인하며 다른 공고·호스트·method·query·파일 요청 변경은 거부한다.

정상 파일과 잘못된 링크·중첩 링크·미지원 형식·중복 상충·10파일 초과를 분리한다. 파일명에 괄호·쉼표가 있어도 파싱하며 파일 역할은 UNKNOWN이다. 실제 검증 형식은 HWPX이고 PDF/HWP는 합성 계약 테스트 범위다.

프로필216개(지역215+기업마당1), 카탈로그277공고/215대상/참조276+기존 기대값1이다. migration V85까지·DB/API·공통 정규화기·A/B 규칙·HWP 추출기는 변경하지 않았다.

## 실제 관측

2026-09-30 19:36:27 KST, `YEONGGWANG-29278`(소상공인 소규모 경영환경 개선사업 신청자 모집):

- TITLE_EXCLUDED_NOT_FETCHED, TITLE_COMBINATION_NOT_MATCHED.
- 이번 수집 관측의 요청·예약 바이트0, files=[]다. 본문·첨부 다운로드를 수행하지 않았다.
- 기존 별도 구조 조사 HTTP 요청은 이전 문서에 보존한다. 과거에도 전혀 조회하지 않았다는 뜻이 아니다.
- 제목의 신청자 모집은 CONTEXT이고 지원유형 조합을 대신하지 않는다. 수집 성공을 만들기 위해 seed·제목 정책을 바꾸거나 본문을 앞 단계에 넣지 않았다. 정책상 누락 가능성은 기술 수집 오류와 다른 검토 항목이다.

2026-09-30 19:36:39 KST, `YEONGGWANG-29330`(2026년 전남광주 청년 문화복지카드 지원사업 대상자3차 모집 공고):

- 제목 COMBINATION_MATCHED, 본문 AVAILABLE/ACCEPTED277자.
- 첨부 발견1·다운로드1·실패0, HWPX133,205byte.
- 파일 SHA-256: `39ab8f2f3ec543f9687123309a053602d800ff575a5dacdfa6391ba88d711bb7`.
- 프로필 지문: `5ff945137a2f265d2b45b25a6808ddc3cd1eb6755dd4d8c7cae4c1e27f1cf738`.
- 보고서 `build/reports/attachment-regional-collection/YEONGGWANG-29278.json`, `YEONGGWANG-29330.json`.
- 적격 공고 예약 요청4/6회·예약 바이트2,713,685/24,117,248. 제목 중단 공고는0이다.
- 두 보고서 모두 원본 정리=true·운영 쓰기=0·추출/정책 QA/기대값 승인/전체 텍스트 분석=false.

분모는2026-09-28 inventory의 활성 지역 수집원223개다. 다운로드 확인186·잔여37, 현재 지문 오류 지역46은 성공 지역과 중복된다. 엄격한 전체 세트 Gate16/223은 유지한다. 한 고정 공고의 파일 성공을 상시 운영·추출 완료로 표현하지 않는다.

## 다음 대상 조사

공개 GET8회, 요청당12초·2MiB·수집기 User-Agent·TLS 검증·자동 redirect 금지. 조사 원문 파일은 저장하지 않았다.

- 영광3회: sc_key=subject/sc_word=지원 공식 검색200에서29330 확인, 고정 상세200, collector 저장 canonical 상세200/같은 제목 확인.
- 경상북도5회: 공식 page.js1회, 상세1370252 3회, fileupload.js1회 모두200. 앞선 턴의 주소 변경 조사와 별도다. pageFormSubmit은 form을 현재 pathname에 제출하고 importUrl에 `/board/view.do`를 넣는다. method 미지정의 기본 GET을 코드에서 확인하고 `https://www.gb.go.kr/page/10109/67.do?pageDtlOrdrNo=1&boardNo=1370252&boardMngNo=71&importUrl=%2Fboard%2Fview.do`로 상세200을 직접 확인했다.
- 경상북도 공식 상세는 form#boardViewForm,div.table_shape_tit의 p제목,ul.table_shape.input_form.board_view 메타데이터,div.board_view 내 div.text 본문 구조다. 파일 영역은 div.td_shape.file의 div.fileWrap.fileId,공개 data-vl=FL00000001196이다.
- 상세 HTML의 `viewFile.putFileHtml`은 고정 문자열5개(파일 ID,번호1,공고문.pdf 표시명,표시 크기157076,확장자pdf)를 제공한다. 공개 fileupload.js는 이를 `/file/readFile.do?fileId=...&fileNo=...` 다운로드 링크로 렌더링한다. JavaScript 실행·브라우저 없이 구조를 읽었다. 아직 실제 PDF 다운로드·signature 검증 전이므로 성공에 집계하지 않는다. 후속 구현에서는 공식 파일 영역의 ID와 고정 리터럴 호출을 결합해야 한다.

## 검증 / 보존 / 미실행

- `gradlew.bat --no-daemon :test --tests '*YeonggwangDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest'`:1분7초,154개·실패/오류/생략0.
- Yeonggwang/Icheon/Cheongju/Gwangyang/Ulju/Hwaseong 계약,목록 collector,본문 client,catalog,policy snapshot,inventory,file type,worker probe 선택 회귀와 `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=YEONGGWANG -PsanebCollectionWindowsTrust=true`:4분17초,382개·실패/오류/생략0. 전체 suite 통과로 확대하지 않는다.
- Node stage/receipts/availability23개 통과.
- `verify-collection-receipt-index.mjs`:332기록/268최신 공고 재현. 이전330기록 해시·상태·샘플과 영광 외 지역 결과 불변 확인.
- `report-collection-availability.mjs`:186확인·37잔여 재현.

운영 DB·설정·worker·정책·ENFORCE·기존 데이터·배포 변경 없음. 브라우저 검증은 사용자 정책상 미실행이다. 이천 TLS·AWS 인증·기존 연제/구례 원본 정리 미완료 등 이전 오류는 별도 후속으로 유지한다.
