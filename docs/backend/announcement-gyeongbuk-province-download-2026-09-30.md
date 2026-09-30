# 경상북도 새 게시판 본문·파일 목록 연결

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결을 우선한다. 정상 파일은 수집하고 발견·다운로드·추출 오류는 분리한다. HWP 추출기 개선은 1.0.16에서 보류하며 전체 장기 goal의 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E는 미완료다.

- [x] `LGS-000200 / SAEOL_GOSI` 새 공식 게시판 본문·첨부 GET 연결.
- [x] 공식 첨부 영역과 HTML 내부의 고정 파일 정보를 결합. JavaScript 실행 없음.
- [x] 기존 저장 URL의 고정 importUrl 인코딩 호환과 식별자 보존.
- [x] 고정 공고1370252 본문90자·PDF1개157,076byte 다운로드·형식 검증·원본 정리.
- [x] 집중 Java155개·선택 회귀 Java392개·bootJar 통과.
- [x] 이전332개 관측 기록과 경상북도 외 지역 결과 보존.
- [~] 다운로드 확인187/223수집원(83.9%), 잔여36(미연결7+등록 다운로드 미확인29).
- [ ] 새 게시판 목록의 자동 유입·운영 등록 주소 전환 검증.
- [ ] 첨부 텍스트 추출·구간 분석·상시 worker·운영 E2E 후속.

## 구현과 계약

기존 V61 등록 주소는 `https://www.gb.go.kr/Main/page.do?mnu_uid=6789&BD_CODE=gosi_notice`다. 앞선 조사에서 새 공식 목록 `https://www.gb.go.kr/page/10109/67.do`로 이동하는 것을 확인했다. 이번 구현은 고정 상세 표본을 대상으로 하며, 기존 목록 수집기가 새 목록에서 상세 주소를 자동 생성한다고 확인한 것은 아니다. 운영 등록 주소·설정·migration은 변경하지 않는다.

`GyeongbukProvinceNoticePage`는 HTTPS www.gb.go.kr의 고정 메뉴 경로와 pageDtlOrdrNo=1,boardMngNo=71,숫자 boardNo,importUrl=/board/view.do만 허용한다. 공통 canonicalizer가 저장한 `%252Fboard%252Fview.do`는 이 고정 값에 한해서 `%2Fboard%2Fview.do`로 복원한다. 임의 경로·추가 query·재귀 decode·HTTP 전환은 허용하지 않는다. 저장 URL과 providerNoticeId는 보존하고 요청 전에 기존 URL 검증을 다시 적용한다.

`form#boardViewForm[method=get]`의 직접 제목 p와 `div.board_view > div.text` 본문을 분리한다. 파일은 `ul.table_shape.input_form.board_view` 내부 `div.td_shape.file`의 공식 `div.fileWrap.fileId` 영역만 사용한다. 메뉴·담당부서·첨부 이름을 본문으로 넣지 않는다.

`GyeongbukFileManifest`는 공식 위젯의 `boardView.viewFile.putFileHtml` 호출에서 JSON 문자열5개(파일 ID·번호·표시 이름·표시 크기·확장자)만 읽는다. 주석·문자열 속 가짜 호출·다른 객체·계산식을 실행하지 않는다. malformed 항목을 별도 오류로 남기고 뒤의 정상 항목은 유지한다. 공식 위젯 초기화 코드가 있는 단일 script와 파일 영역의 ID를 대조한다. JavaScript·브라우저·eval은 실행하지 않는다.

`GyeongbukProvinceAttachmentDiscoveryProfile`은 같은 호스트의 `/file/readFile.do?fileId=...&fileNo=...` 고정 GET만 생성한다. source/parser·식별자·파일 ID/번호·이름/확장자·method·query·redirect를 검증한다. 최대10개, 중복/상충, 미지원 형식, 알 수 없는 첨부 요소를 분리한다. 파일 ID는 locator에 그대로 넣지 않고 hash로 남기며 역할은 UNKNOWN이다. 파일 목록이 비정상인 상태를 첨부 없음으로 처리하지 않는다.

실제 검증 형식은 PDF다. HWP/HWPX 및 첨부 없음 구조는 합성 계약 테스트 범위이며 실제 관측으로 확대하지 않는다. 프로필217개(지역216+기업마당1), 카탈로그278공고/216대상(참조277+기존 기대값1)이다. Flyway V85까지·DB/API·공통 정규화기·A/B 정책·HWP 추출기는 변경하지 않았다.

## 실제 관측

2026-09-30 19:56:59 KST, `GYEONGBUK-PROVINCE-1370252`(2026년 하반기 대학생 학자금대출 이자지원 공고):

- 제목 COMBINATION_MATCHED, 본문 AVAILABLE/ACCEPTED90자.
- 첨부 FOUND·complete=true·발견1·다운로드1·실패0, PDF157,076byte.
- 파일 SHA-256: `e816ea1730989a448cef8d8c3ec20cc450d939da9c9a86781e215a0cf780c972`.
- 프로필 지문: `c20eaa96154b1252f02953a0bd6f2a3d5272e6347b25656e925d0406717840d6`.
- 보고서: `build/reports/attachment-regional-collection/GYEONGBUK-PROVINCE-1370252.json`.
- 예약 요청4/6회·예약 바이트2,680,212/24,117,248. 예약량은 실제 파일 수신량과 다르다.
- 원본 정리=true·운영 쓰기=0·추출/정책 QA/기대값 승인/전체 텍스트 분석=false.

이번 별도 구조 조사는 고정 상세 GET1회, 요청당12초·2MiB·수집기 User-Agent·TLS 검증·자동 redirect 금지로 수행했다. 제목·본문·첨부 영역과 다섯 고정 파일 정보를 확인했으며 HTML 원문은 파일로 저장하지 않았다. 이전 영광 문서에 기록한 경상북도 조사 요청은 이번 요청 수에 중복 합산하지 않는다.

분모는2026-09-28 15:56:12 KST inventory의 활성 지역 수집원223개이며 현재 운영 재조회는 아니다. 다운로드 확인187·잔여36, 현재 지문 오류 지역46은 성공 지역과 중복된다. 엄격한 전체 세트 Gate16/223은 유지한다. 수집원별 고정 표본1개의 다운로드 성공을 전 지역 상시 운영·텍스트 추출 완료율로 표현하지 않는다.

## 검증 / 보존 / 미실행

- `gradlew.bat --no-daemon :test --tests '*GyeongbukProvinceDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest'`:59초,155개·실패/오류/생략0.
- GyeongbukProvince/Yeonggwang/Icheon/Cheongju/Gwangyang/Ulju/Hwaseong 계약, 목록 collector, 본문 client, catalog, policy snapshot, inventory, file type, worker probe 선택 회귀와 `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=GYEONGBUK_PROVINCE -PsanebCollectionWindowsTrust=true`:3분31초,392개·실패/오류/생략0. 전체 프로젝트 suite 통과로 확대하지 않는다.
- 기존332개 기록의 파일 hash·상태·샘플을 재검증한 후 새 관측을 추가하고 경상북도 외 지역 결과 불변을 확인했다.
- `node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs`:23개 통과.
- `node scripts/qa/verify-collection-receipt-index.mjs`:333기록/269최신 공고 재현.
- `node scripts/qa/report-collection-availability.mjs`:187확인·36잔여·오류 지역46 재현.
- `git -c core.autocrlf=false diff --check`:통과.

운영 DB·설정·worker·정책·ENFORCE·기존 데이터·배포 변경 없음. 브라우저 검증은 사용자 정책상 미실행이다. 이천 TLS·AWS 인증·기존 연제/구례 원본 정리 미완료 등 이전 오류는 별도 후속으로 유지한다. 장기 goal 운영 절차에 따라 집중 테스트→회귀→실제 다운로드 근거를 분리했다.
