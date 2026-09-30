# 장흥 공식 본문·GET 첨부 연결

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결 단계다. 정상 파일을 우선 수집하고 개별 오류는 별도 보존한다. HWP 추출기 개선은 보류하며 전체 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E goal은 미완료다.

- [x] 공식 등록 수집원과 지원 공고 상세의 제목·본문·첨부 영역 확인.
- [x] 장흥 전용 본문 선택기·첨부 프로필·카탈로그·회귀 테스트 구현.
- [x] Node23개 검사 통과.
- [x] Java286개 검사·bootJar·실제 HWP 첨부 관측.
- [x] 영수증305건·공고244개 지역 대장 갱신 및 재현 검증.

## 근거와 구현 경계

등록 수집원은 `LGS-000189 / SPRING_BBS`, 공식 목록은 `https://www.jangheung.go.kr/www/organization/news/notification`이다. 앞선 신안 작업에서 공식 검색 폼으로 지원 공고27081·27319·28243을 확보했으며 해당 검색 요청은 신안 조사 기록에 이미 포함되어 있다.

이번 추가 조사는 [2026년 장흥군 소상공인 대출금 이차보전 지원사업 공고](https://www.jangheung.go.kr/www/organization/news/notification?idx=27081&mode=view) 상세 GET1회·15초 제한이며 HTTP200이다. 조사 User-Agent는 `saneB-AnnouncementCollector/1.0`, 실제 Java 다운로드 수집기는 기존 `saneB-attachment-collector/1.0`을 유지한다. 조사 성공과 실제 수집기 관측을 구분한다. TLS 검증·자동 redirect 금지, 원문 HTML 파일 저장0이다.

- `div#content > div.view_title > p.title` 제목과 `div.view_box:not(.file_area)` 본문을 선택한다. 메뉴·이전/다음글·부서·만족도 폼·첨부 파일명은 본문에서 제외한다.
- `div.view_box.file_area`의 첨부파일 라벨, `div.file_cnt > ul.file_list > li`의 파일명·확장자·다운로드 버튼을 검증한다.
- `window.open('정확한 공개 파일 URL', '_blank')` 문자열만 읽고 JavaScript를 실행하지 않는다. 공식 `eminwon.jangheung.go.kr/emwp/jsp/ofr/FileDown.jsp` GET3필드와 안전한 파일 경로는 기존 새올 검증기를 재사용한다.
- 같은 첨부 항목의 `/Viewer_gosi/공고번호_번호` 바로가기는 요청하지 않는다. 불명확한 링크·제어 요소는 오류로 남기고 정상 파일은 보존한다.
- 최대10파일, UNKNOWN 역할, 중복 제거, 파일명·확장자·서명·MIME 검증, 동일 요청 외 redirect 거부를 유지한다. 미지원 형식은 다운로드하지 않는다.
- 프로필은 `LOCAL_JANGHEUNG_GET_V1`이다. 기존 프로필 지문·공통 다운로드 엔진·키워드·추출기·DB/API·migration은 변경하지 않는다. 카탈로그는 참조용이며 기대값 승인 null이다.

해당 상세는 공공누리 제4유형(출처표시·상업적 이용 금지·변경 금지)을 표시한다. 공개 파일 연결 검증을 서비스에서의 상업적 재사용 승인으로 해석하지 않는다. 운영 재사용 범위 확인은 별도 후속이다.

## 검증 결과

첫 검증에서 본문 선택기 삽입 위치 오류로 compileJava가 실패했고, 실제 본문 선택 메서드로 이동하여 수정했다. 당시 외부 관측은 실행되지 않았다.

- 실행: `gradlew.bat --no-daemon :test`에서 장흥 계약·본문·카탈로그·정책 snapshot·inventory·파일 형식·worker probe 검사를 선택하고 `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=JANGHEUNG -PsanebCollectionWindowsTrust=true`를 실행했다. 수정 후4분6초·종료0, Java286개 통과·실패/오류/생략0, bootJar 성공이다.
- 실제 관측: 2026-09-30 13:50:24 KST, `JANGHEUNG-27081`, 제목 조합 통과, 본문483자 AVAILABLE, FOUND/complete, HWP1개82,432byte 다운로드·형식 검증 성공이다.
- 파일 SHA-256: `0770934520324569096386cd894325a395ecde278108158584b16f08a4d1b25e`.
- 요청 예약 상한 포함 실적4/6회·2,359,808/24,117,248byte, 임시 원본 정리 확인, 운영 쓰기0이다. 첨부 텍스트 추출·정책 QA·전체 분석은 미실행이며 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`다.
- Node23개 검사, 영수증305건·공고244개 재현 검증 통과. 기존 영수증과 타 지역 상태를 그대로 보존했다.
- 최신 수집 가능 집계는 **166/223수집원(74.4%)·57잔여(미연결29+등록 다운로드 미확인28)**다. 프로필195개(지역194+기업마당1), 카탈로그253공고/194대상/참조252+기존 기대값1이다. 전체 첨부 세트의 엄격 Gate16/223과 구분한다.

운영 DB/설정/worker/정책/ENFORCE/기존 데이터/배포 변경은 없다. 브라우저는 사용자 정책상 미실행이다. AWS 인증 갱신과 이전 연제·구례 조사 원본 정리 미완료는 별도 후속이다.
