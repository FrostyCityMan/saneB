# 남동구 공식 대표 홈페이지 첨부 연결

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결을 우선하며 정상 파일과 개별 오류를 분리한다. 제목→본문→첨부→관리자 최종 검증, 외부 공고 자동 활성화 금지를 유지한다. HWP 추출기1.0.16 추가 개선과 전체 운영 E2E는 이번 단계에서 완료하지 않는다.

- [x] LGS-000059 / HEURISTIC_NOTICE 공식 대표 홈페이지의 본문·첨부 연결.
- [x] 등록 주소의 DNS 문제와 현재 공식 홈페이지 경로를 구분.
- [x] 고정 공고71702 본문431자·HWPX1개97,102byte 실제 다운로드·형식 검증·원본 정리.
- [x] 집중 Java154개·선택 회귀 Java406개·bootJar·Node23개 통과.
- [x] 기존333개 기록 보존,334기록/270최신 공고 재현.
- [~] 다운로드 확인188/223수집원(84.3%),35잔여(미연결6+등록 다운로드 미확인29).
- [ ] 운영 등록 주소 확인·목록 자동 유입 검증.
- [ ] 강서구 새 고정 표본66840 본문·첨부 연결.
- [ ] 첨부 추출·구간 분석·상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E 전체 Gate.

## 공식 경로와 구현

저장소 V57에 기록된 남동구 주소는 `https://biz.namdong.go.kr/main/news/announce.jsp`다. 이번 로컬 DNS A 조회에서 biz 호스트는 Win32Exception으로 실패하고 www.namdong.go.kr은 IPv4 1개로 해석됐다. 이 결과만으로 전 세계 DNS 상태나 현재 운영 DB 설정을 단정하지 않는다.

`https://www.namdong.go.kr/`의 직접 고시공고 메뉴는 `/main/news/announce.jsp`이며 HTTP200으로 확인했다. 해당 페이지의 공개 폼은 GET `/main/eminwon/eminwonAnnounceList.do`, 제목 검색 keyfield=title/keyword를 사용한다. 소상공인 검색에서는 상세 링크가 없었고 지원 검색에서71702를 확인했다. 검색 결과 부재를 전체 지원 공고 부재로 간주하거나 다른 게시판으로 대체하지 않는다.

고정 상세는 `https://www.namdong.go.kr/main/eminwon/eminwonAnnounceDetail.do?mgt_no=71702`, 제목은 ‘『2026년 남동구 청년도전지원사업』 참여자 모집공고’다. `NamdongNoticePage`는 div.board_view의 직접 div.title>p 제목, div.con>div.detail 본문, div.add_file의 ‘첨부파일’ dl>dd>ul을 분리한다. 부평의 제목 h5와 다르므로 별도 구조 경계를 적용한다. 메뉴·담당부서·연락처·첨부 이름은 본문에 넣지 않는다.

`NamdongAttachmentDiscoveryProfile`은 기존 부평과 같은 공개 첨부 처리 방식을 남동구 호스트·source/parser에 고정한다. 기존 부평 코드·프로필 지문은 변경하지 않는다. 공식 li>a 링크와 파일 형식 아이콘을 확인하고 HTTPS `eminwon.namdong.go.kr/emwp/jsp/ofr/FileDownNew.jsp`의 고정3개 query를 그대로 전송한다. 암호화된 파일 값은 해석하지 않고 locator에는 해시만 남긴다. JavaScript·미리보기는 실행하지 않는다.

HTTPS443·GET·동일 파일 요청·source identity·query·파일명/형식 검증을 유지한다. 최대10개, 중복 상충, 미지원 형식, 미해석 첨부 요소를 분리하고 정상 파일을 보존한다. 실제 검증 형식은 HWPX이며 PDF/HWP와 첨부 없음은 합성 계약 테스트 범위다. 역할은 UNKNOWN이다.

현재 대표 홈페이지를 관측 입력으로 사용했지만 저장소 migration·운영 주소는 변경하지 않았다. 구 biz 등록 주소로 www 상세를 요청하면 본문 client가 DETAIL_HOST_NOT_ALLOWED로 HTTP 이전에 중단하는 계약을 검증했다. 따라서 이번 성공은 현재 공식 경로의 고정 표본 수집 증거이며 기존 운영 목록의 상시 유입 복구 증거가 아니다. 운영 주소는 현재 DB를 확인한 뒤 별도 반영 범위로 다뤄야 한다.

프로필218개(지역217+기업마당1), 카탈로그279공고/217대상(참조278+기존 기대값1)이다. Flyway V85까지·DB/API·공통 정규화기·A/B 정책·HWP 추출기는 변경하지 않았다.

## 실제 관측

2026-09-30 20:11:17 KST, `NAMDONG-71702`:

- 제목 COMBINATION_MATCHED, 본문 AVAILABLE/ACCEPTED431자.
- 첨부 FOUND·complete=true, 발견1·다운로드1·실패0, HWPX97,102byte.
- 파일 SHA-256: `49eaa9341a6e0710c0913e154630c658430081513d0ae05fd4e42509d1eac3d3`.
- 프로필 지문: `232bdda97ebc3aeee465e6c6e4eeb762f8cc2fd26f7b23ecaea9a35ad65fce73`.
- 보고서 `build/reports/attachment-regional-collection/NAMDONG-71702.json`.
- 예약 요청4/6회·예약 바이트2,423,630/24,117,248. 예약량과 실제 파일 수신량은 구분한다.
- 원본 정리=true·운영 쓰기=0·추출/정책 QA/기대값 승인/전체 텍스트 분석=false.

분모는2026-09-28 15:56:12 KST inventory의 활성 지역 수집원223개다. 이번 운영 재조회는 없으며 고유 행정구역 수와도 다르다. 다운로드 확인188·잔여35, 오류 지역46은 성공 지역과 중복된다. 엄격한 전체 세트 Gate16/223은 유지한다. 최소1표본 다운로드 비율을 전체 개발·추출·운영 완료율로 표현하지 않는다.

## 다음 대상 조사 / 요청 경계

별도 공개 조사 GET9회(남동7+강서2), 각12초·2MiB 상한·수집기 User-Agent·TLS 검증·자동 redirect 금지다. DNS 조회2건과 공개 검색엔진 질의4건은 별도다. 조사 HTML 원문을 파일로 저장하지 않았다.

- 남동7회: 대표 홈페이지, 고시공고 메뉴, 실제 목록 endpoint, 소상공인/지원 검색, 고정 상세2회 모두200. 마지막 상세 조회는 본문·파일 영역 구조 확인이다.
- 강서2회: [공식 청년 소식](https://www.gangseo.seoul.kr/eco/eco070102/320248?curPage=1)이 연결한 새 고정 고시공고66840을 조사했다. `https://www.gangseo.seoul.kr/gs040301/view?srchPage=&curPage=1&srchKey=&srchText=&mgtNo=66840`은200이다. 과거62174/검색 실패와 다른 표본이며 기존 오류를 지우지 않는다.
- 강서의 div.board-view-wrap, board-view-head/subject, board-view-body/view-content, file-element>dl>dd>ul 구조와 PDF·HWPX2개 링크를 확인했다. 공개 goDownLoad의 문자열3개는 encodeURIComponent 후 `https://eminwon.gangseo.seoul.kr/emwp/jsp/ofr/FileDown.jsp` GET에 사용된다. 스크립트는 실행하지 않았다. 아직 강서 실제 파일 다운로드 전이므로 집계에 포함하지 않는다.

## 검증 / 보존 / 미실행

- `gradlew.bat --no-daemon :test --tests '*NamdongDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest'`:46초,154개·실패/오류/생략0.
- Namdong/Bupyeong/GyeongbukProvince/Yeonggwang/Icheon/Cheongju/Gwangyang/Ulju/Hwaseong 계약, 목록 collector, 본문 client, catalog, policy snapshot, inventory, file type, worker probe 선택 회귀와 `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=NAMDONG -PsanebCollectionWindowsTrust=true`:3분59초,406개·실패/오류/생략0. 전체 프로젝트 suite 통과로 확대하지 않는다.
- `node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs`:23개 통과.
- `verify-collection-receipt-index.mjs`:334기록/270최신 공고 재현. 이전333기록의 hash·상태·샘플, 남동 외 지역 결과 불변 확인.
- `report-collection-availability.mjs`:188확인·35잔여·오류 지역46 재현.

장기 goal 운영 절차에 따라 집중 테스트→선택 회귀→실제 다운로드를 구분했다. 운영 DB·설정·worker·정책·ENFORCE·기존 데이터·배포 변경 없음. 브라우저 검증은 사용자 정책상 미실행이다. 이천 TLS·AWS 인증·연제/구례 조사 원본 정리 미완료 등 이전 후속은 유지한다.
