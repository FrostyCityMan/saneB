# 이천 본문·첨부 연결과 TLS 오류 분리

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결을 우선한다. 정상 파일은 보존하고 개별 실패는 별개 오류로 관리한다. HWP 추출기 개선은 보류하며 전체 장기 goal의 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E는 미완료다.

- [x] `LGS-000105 / SAEOL_GOSI` 공식 본문·첨부 GET 프로필 연결.
- [x] 저장 canonical URL·공고 식별자와 공식 상세 요청의 호환 검증.
- [x] 집중 Java152개·선택 회귀 Java372개·bootJar·Node23개 통과.
- [x] 공고70639 본문115자, HWPX1개 발견 및 실패 근거 보존.
- [!] 파일 전송 TLS_FAILED. 다운로드0개이며 성공 수집원에 추가하지 않음.
- [~] 185/223수집원 다운로드 확인(83.0%),38잔여(미연결9+등록 다운로드 미확인29).
- [ ] 영광29278 연결, 경상북도 공식 주소 변경 후속.
- [ ] 고시/일반공고 상시 목록 유입·텍스트 추출·전체 운영 E2E 후속.

## 구현과 계약

`IcheonNoticePage`는 HTTPS `www.icheon.go.kr/portal/saeol/gosi/view.do`의 숫자 notAncmtMgtNo와 소문자 mid만 사용한다. 고시0402010000·일반공고0402020000을 구분하고 임의 query·다른 host·인코딩 경로·fragment는 허용하지 않는다. 공식 `form#detailForm > div.bod_wrap > div.bod_view`에서 div.subject,div.view_cont,dl.view_file을 각각 제목·본문·첨부로 선택한다. 부서·메뉴·첨부 이름은 본문에 섞지 않는다.

이전 조사에서 공식 상세 POST200을 확인했고 이번에 같은 상세 GET200을 직접 확인했다. 파일 GET 함수는 공식 `goDownload`이며 `eminwon.icheon.go.kr/emwp/jsp/ofr/FileDown.jsp`의 세 공개 인자를 사용한다. 기존 새올 파일 경로·파일명 검증기를 재사용하고 JavaScript를 실행하지 않는다.

`IcheonAttachmentDiscoveryProfile`은 `ul#updateFileList`의 직접 a.download만 수집한다. `fn_egov_gosi_preview`의 공고번호·파일 순번·세 파일 인자가 같은 파일인지 확인하여 미리보기는 요청하지 않는다. 다운로드 함수3인자는 선형 파서로 읽고 미리보기 앞의 공고/순번은 제한된 숫자만 해석한다. 파일명에 괄호·쉼표가 있어도 파싱되며 미리보기 오류가 정상 파일 descriptor를 폐기하지 않는다. 미지원 형식·불명 링크·중복 상충·10파일 상한·host/method/identity 변경을 검증한다. 역할 UNKNOWN, 기존 TLS·MIME 정책 유지다.

프로필215개(지역214+기업마당1), 카탈로그275공고/214대상/참조274+기존 기대값1이다. 기존 migration V85까지·DB/API·A/B 규칙·공통 정규화기·HWP 추출기는 변경하지 않았다.

## 실제 관측과 진단

2026-09-30 19:20:01 KST, `ICHEON-70639`(2027년 지방보조금 지원계획 (소상공인 지원) 공고):

- 제목 COMBINATION_MATCHED, 본문 AVAILABLE/ACCEPTED115자.
- 공식 첨부 발견1, 발견 완료=true, 다운로드0, 실패1.
- 파일 단계 FILE_DOWNLOAD, 오류 TLS_FAILED, 전송 시도1·완료0. 파일 byte/hash 성공 근거는 없다.
- 프로필 지문 `56cd4e37fcc3b651cdb5db1097426b428f3cfbb524b8725b1fd10622deec06b0`.
- 보고서 `build/reports/attachment-regional-collection/ICHEON-70639.json`.
- 예약 요청4/6회·예약 바이트2,392,064/24,117,248.
- 원본 정리=true, 운영 쓰기=0, 추출/정책 QA/기대값 승인/전체 텍스트 분석=false.

추가 TLS 연결 진단2회는 HTTP 요청이나 파일 다운로드 없이 수행했다. Windows 기본 SslStream 인증서 검증은 통과했으나 관측된 issuer는 로컬 Avast TLS 검사 루트였다. 같은 호스트에 Java21/Windows-ROOT와 HTTPS 호스트 검증·SNI를 적용한 진단은 `SSLHandshakeException: Remote host terminated the handshake`, `EOFException: SSL peer shut down incorrectly`를 반환했다. OS와 Java 사이의 경로 차이는 확인했지만 원 서버·보안 프로그램·프로토콜 중 어느 지점이 원인인지는 미확정이다. 인증서 검증 해제, 보안 프로그램 설정 변경, HTTP 강등, 운영 요청은 수행하지 않았다. 이 결과를 원 서버 인증서 만료나 전체 운영 장애로 단정하지 않는다.

2026-09-28 inventory 기준 다운로드 확인185/223·잔여38은 유지한다. 미연결9, 등록 다운로드 미확인29, 현재 지문 기준 오류 지역46이며 오류 지역은 성공 지역과 겹친다. 엄격한 전체 세트 Gate16/223은 유지한다. 파일 발견·빌드 성공·Windows TLS 통과를 실제 파일 수집 성공으로 대체하지 않는다.

## 다음 대상 조사

공개 조사 GET8회, 요청당12초·2MiB·수집기 User-Agent·TLS 검증·자동 redirect 금지. 조사 원문 파일은 저장하지 않았다. TLS 진단2연결은 이 HTTP 요청 수와 별개다.

- 이천1GET: 공식 상세200과 실제 div.subject·본문·첨부·미리보기 구조 확인. 등록 목록 `/portal/contents.do?mid=0402000000`의 고시 범주와 이번 일반공고 표본의 범위는 별개이므로 상시 목록 유입은 후속이다.
- 영광2GET:29278 상세200. `div#board_view > table` 안 제목·첨부 th행,본문 div.board_view_contents를 확인했다. 공식 `goDownLoad`는 `https://eminwon.yeonggwang.jeonnam.kr/emwp/jsp/ofr/FileDown.jsp`로 세 공개 인자를 보내는 GET이다. HWPX1개 링크이며 다운로드 전이다. V62 바인딩은 SPRING_BBS다.
- 경상북도5GET: 등록 `https://www.gb.go.kr/Main/page.do?mnu_uid=6789&BD_CODE=gosi_notice`는302이며 Location 확인을 위해2회 조회했다. 공식 이동 주소 `https://gb.go.kr/page/10109/67.do`는301, canonical `https://www.gb.go.kr/page/10109/67.do`는200으로 목록·계약 확인을 위해2회 조회했다. `form#boardListForm`,boardMngNo71,pageDtlOrdrNo1,`boardList.view(boardNo)`가 boardNo를 설정하고 `pageFormSubmit(...,"/board/view.do")`를 호출하는 구조다. 지원 표본1370252(2026년 하반기 대학생 학자금대출 이자지원 공고)를 확인했다. pageFormSubmit의 요청 방식과 상세·파일은 후속 확인 전이며 CSRF 값을 저장하거나 출력하지 않았다. 운영 등록 주소·파서를 수정하지 않았다.

## 검증 / 근거 보존 / 미실행

- `gradlew.bat --no-daemon :test --tests '*IcheonDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest'`:1분1초,152개·실패/오류/생략0.
- Icheon/Cheongju/Gwangyang/Ulju/Hwaseong 계약, 목록 collector,본문 client,catalog,policy snapshot,inventory,file type,worker probe 선택 회귀와 `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=ICHEON -PsanebCollectionWindowsTrust=true`:4분57초,372개·실패/오류/생략0. 관측 태스크는 오류 기록 생성에도 성공 종료하므로 파일 성공과 구분한다. 전체 테스트 suite 통과로 확대하지 않는다.
- Node stage/receipts/availability23개 통과.
- `verify-collection-receipt-index.mjs`:기존329기록 해시·상태·샘플 보존,330기록/266최신 공고 재현. 이천 외 지역 결과 불변 확인.
- `report-collection-availability.mjs`:185확인·38잔여 재현.

운영 DB·설정·worker·정책·ENFORCE·기존 데이터·배포 변경 없음. 브라우저는 사용자 정책상 미실행이다. AWS 인증과 기존 연제·구례 조사 원본 정리 미완료는 별도 후속으로 유지한다.
