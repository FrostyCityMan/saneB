# 서울 강서구 본문·PDF·HWPX 첨부 연결

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결을 우선하며 정상 파일과 개별 오류를 분리한다. 제목→본문→첨부→관리자 최종 검증, 외부 공고 자동 활성화 금지를 유지한다. HWP 추출기1.0.16 추가 개선과 전체 운영 E2E는 이번 단계의 완료 항목이 아니다.

- [x] LGS-000017 / SAEOL_GOSI 공식 본문·첨부 GET 연결.
- [x] 저장 URL 식별자 보존, 검색 포함 주소와 공고번호만 있는 상세 주소의 호환 검증.
- [x] 고정 공고66840 본문550자·PDF/HWPX2개284,716byte 다운로드·형식 검증·원본 정리.
- [x] 집중 Java156개·선택 회귀 Java415개·bootJar·Node23개 통과.
- [x] 이전334개 기록 보존,335기록/271최신 공고 재현.
- [~] 다운로드 확인189/223수집원(84.8%),34잔여(미연결5+등록 다운로드 미확인29).
- [!] 송파33174 서버 응답의 제목 칸 공백. 파일 성공으로 집계하지 않음.
- [ ] 나머지 지역·목록 자동 유입·텍스트 추출·구간 분석·상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E 전체 Gate.

## 구현과 계약

V36 등록 목록은 `https://www.gangseo.seoul.kr/gs040301`이다. 이전 남동구 작업에서 공식 청년 소식이 연결한 공고66840과 첨부 버튼의 실제 GET 경로를 확인했다. 이번에는 검색·페이지 필드를 포함한 상세 주소와 공고번호만 있는 `https://www.gangseo.seoul.kr/gs040301/view?mgtNo=66840`이 모두 HTTP200·같은 제목을 반환하는지 확인했다. 과거 공고62174의 HTTP400과 목록/검색 시간 초과는 보존하며 새 성공으로 지우지 않는다.

`SeoulGangseoNoticePage`는 HTTPS www.gangseo.seoul.kr의 고정 상세 경로와 숫자 mgtNo만 최종 요청에 남긴다. 공식 검색/페이지 query의 제한된 키만 허용하고 파일 요청에는 전달하지 않는다. 원래 저장 URL의 hash와 기존 raw URL의 canonical hash를 검증한다. 공통 정규화기를 변경하지 않는다.

공식 div.board-view-wrap 내부에서 다음 영역을 구분한다.

- 제목: board-view-head > top-element > subject.
- 본문: board-view-body > view-content > gosi-con. 메타데이터와 첨부를 제외한다.
- 첨부: board-view-body > file-element의 ‘첨부파일’ dl > dd > ul.

`SeoulGangseoAttachmentDiscoveryProfile`은 각 li의 span.name 표시 이름·크기와 span.download의 다운로드 버튼을 대조한다. goDownLoad의 고정 문자열3개만 선형 해석하며 JavaScript는 실행하지 않는다. 기존 새올 GET 검증기를 재사용해 HTTPS `eminwon.gangseo.seoul.kr/emwp/jsp/ofr/FileDown.jsp`의 파일명·저장명·날짜 경로를 검사한다. 괄호·쉼표·공백이 있는 파일명을 유지하고 locator에는 파일 이름 대신 hash를 저장한다.

HTTPS443·GET·동일 파일 요청·source/parser·query·본문 경계 검증을 유지한다. 최대10개, 중복 상충, 미지원 형식, 미해석 첨부 요소를 분리하고 정상 파일은 보존한다. 본문 밖의 링크·미리보기는 요청하지 않는다. 역할은 UNKNOWN이다. 실제 관측 형식은 PDF/HWPX이고 HWP·첨부 없음은 합성 계약 테스트 범위다.

프로필219개(지역218+기업마당1), 카탈로그280공고/218대상(참조279+기존 기대값1)이다. 기존 프로필 지문·Flyway V85까지·DB/API·A/B 정책·HWP 추출기는 변경하지 않았다.

## 실제 관측

2026-09-30 20:24:36 KST, `SEOUL-GANGSEO-66840`(강서구 미취업청년 자격증 응시료 지원 사업 신청자 모집 공고):

- 제목 COMBINATION_MATCHED, 본문 AVAILABLE/ACCEPTED550자.
- 첨부 FOUND·complete=true, 발견2·다운로드2·실패0.
- PDF180,919byte, SHA-256 `72fcb3758ccbddcb8958b6031c935324e65f7587be1da5a702302781896dcbb3`.
- HWPX103,797byte, SHA-256 `d4a7670bd635a68bc20906d6a176d482f613b559bbdae097f9e68499647c7425`.
- 프로필 지문 `e2cceb2c7806d369bedb8ec63b7e21ffba9c36b641bd9ff6ce2ff6d7868afe29`.
- 보고서 `build/reports/attachment-regional-collection/SEOUL-GANGSEO-66840.json`.
- 예약 요청5/6회·예약 바이트2,545,708/24,117,248. 예약량은 실제 파일 수신량과 다르다.
- 원본 정리=true·운영 쓰기=0·추출/정책 QA/기대값 승인/전체 텍스트 분석=false.

분모는2026-09-28 15:56:12 KST inventory의 활성 지역 수집원223개다. 현재 운영 재조회는 없으며 고유 행정구역 수와도 구분한다. 다운로드 확인189·잔여34, 오류 지역46은 성공 지역과 중복된다. 엄격한 전체 세트 Gate16/223은 유지한다. 최소1표본 다운로드 비율을 전체 개발·추출·운영 완료율로 표현하지 않는다.

## 송파 진단 / 요청 경계

이번 별도 공개 조사 GET3회(강서2+송파1), 각12초·2MiB 상한·수집기 User-Agent·TLS 검증·자동 redirect 금지다. HTML 원문 파일을 저장하지 않았고 응답 자원을 종료했다. 이전 남동구 작업의 강서 조사2회는 별도 이력이다.

송파 `https://www.songpa.go.kr/www/selectGosiData.do?not_ancmt_mgt_no=33174&key=2776`은 HTTP200이다. 공식 form[name=gosiFrm]의 서버 렌더링 제목 셀은 텍스트 길이0이며 script/input도 없다. 같은 응답에 고시공고번호24자·등록일10자·본문85자·첨부3개와 고정 공고 ID가 존재한다. 제목을 채우는 동적 조회는 이번 확인 범위에서 찾지 못했다. 삭제용 함수는 호출하지 않았다.

따라서 현재 오류는 단순 제목 selector 변경과 구분해야 한다. 후속에서는 공식 목록의 제목·공고 ID·고시공고번호와 상세를 대조하는 식별 계약을 검토할 수 있다. 이 계약을 검증하기 전에는 제목을 추측하거나 빈 제목 검증을 우회하지 않는다. 이번 송파 파일 요청은0이며 수집 성공 집계에 추가하지 않았다.

## 검증 / 보존 / 미실행

- `gradlew.bat --no-daemon :test --tests '*SeoulGangseoDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest'`:45초,156개·실패/오류/생략0.
- SeoulGangseo/Namdong/Bupyeong/GyeongbukProvince/Yeonggwang/Icheon/Cheongju/Gwangyang/Ulju/Hwaseong 계약, 목록 collector, 본문 client, catalog, policy snapshot, inventory, file type, worker probe 선택 회귀와 `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=SEOUL_GANGSEO -PsanebCollectionWindowsTrust=true`:3분37초,415개·실패/오류/생략0. 전체 프로젝트 suite 통과로 확대하지 않는다.
- `node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs`:23개 통과.
- `verify-collection-receipt-index.mjs`:335기록/271최신 공고 재현. 이전334기록의 hash·상태·샘플, 강서 외 지역 결과 불변 확인.
- `report-collection-availability.mjs`:189확인·34잔여·오류 지역46 재현.

장기 goal 운영 절차에 따라 집중 테스트→선택 회귀→실제 다운로드 근거를 분리했다. 운영 DB·설정·worker·정책·ENFORCE·기존 데이터·배포 변경 없음. 브라우저 검증은 사용자 정책상 미실행이다. 남동 등록 주소·이천 TLS·AWS 인증·연제/구례 조사 원본 정리 미완료 등 기존 후속을 유지한다.
