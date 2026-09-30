# 신안 고시공고 본문·공개 POST 첨부 연결

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결 단계다. 정상 파일을 우선 수집하고 개별 발견·전송·형식·본문 오류를 별도 보존한다. HWP 추출기 추가 개선은 보류하며 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E를 포함하는 전체 goal은 미완료다.

- [x] 신안 공식 등록 목록·검색·상세 및 파일 POST 폼 조사.
- [x] 신안 본문 선택기·첨부 프로필·고정 표본·회귀 테스트 구현.
- [x] Node23개 수집 근거·집계 검사 통과.
- [x] Java285개 회귀 검사·bootJar·실제 HWP 첨부 관측.
- [x] 영수증304건·공고243개 지역 대장 갱신 및 재현 검증.

## 조사 근거

V61 등록 수집원은 `LGS-000199 / SPRING_BBS`다. 공식 등록 목록은 `https://www.shinan.go.kr/home/www/openinfo/participation_07/participation_07_04/page.wscms`이며 이번 직접 요청에서는 HTTP200과 고시공고 링크를 확인했다. 과거400 기록을 삭제하거나 원인을 임의로 확정하지 않는다.

공식 GET 검색 폼의 action은 `/home/www/openinfo/participation_07/participation_07_04`, 필드는 `search=search_title`, `not_ancmt_sj`다. 소상공인 검색에서 [2026년도 전라남도 소상공인 육성자금 지원계획 공고](https://www.shinan.go.kr/home/www/openinfo/participation_07/participation_07_04/show/38211?page=1)를 확인하고 해당 상세를 직접 조회했다. 검색엔진이 제공한 eng 하위 호스트는 새로 허용하지 않고 등록된 www 호스트만 사용했다.

`table.show_form`의 제목·내용·첨부파일 행을 명확히 분리한다. 파일은 `2026년 소상공인 육성자금 지원계획 공고문.hwp` 1개다. 공식 goDownLoad 함수는 공개 파일 인자3개를 `form[name=nnn]`의 `user_file_nm`, `sys_file_nm`, `file_path`에 넣어 `https://eminwon.shinan.go.kr/emwp/jsp/ofr/FileDownNew.jsp`로 POST한다. 파일 인자를 해독하거나 경로를 추측하지 않는다.

장흥 `LGS-000189`도 공식 상세25822와 등록 목록의 HTTP200을 확인했다. 상세는 `div#content` 아래 `view_title > p.title`, `view_box` 본문, `view_box.file_area`이며 다운로드 버튼은 공식 `eminwon.jangheung.go.kr/emwp/jsp/ofr/FileDown.jsp` GET 링크를 window.open으로 연다. 별도 `Viewer_gosi`는 미리보기다. 장흥은 연결·파일 다운로드를 실행하지 않았고 성공 수에 포함하지 않았다. 후속 표본 선정 시 제목 정책을 그대로 검증해야 한다.

직접 조사는 총7요청(6GET·1POST), 요청당15초다. 신안 목록·검색·상세3GET, 장흥 목록2GET·상세1GET·공식 검색1POST를 실행했다. 장흥 검색은 공식 세션·검색 폼으로 2026년 지원 공고27081·27319·28243을 확보했으며 상세·첨부 검증은 후속이다. 검색 질의3회는 별도다. 수집기 User-Agent·TLS 검증·자동 redirect 금지를 유지했다. 원문 HTML을 파일로 저장하지 않았으며 공개 검색 폼과 다운로드 폼을 구분했다. 실제 Java 관측은 별도6요청·23MiB 상한이다.

## 구현 경계

- `ShinanNoticePage`: 제목·내용·첨부 셀을 분리하고 중복 표·셀 누락을 구조 변경 오류로 처리한다.
- `ShinanAttachmentDiscoveryProfile`: 시스템 수집원·URL hash·정확한 공식 상세 경로·파일 호스트·공개 POST3필드를 검증한다.
- 영암에서 검증한 표형 처리 패턴을 신안 전용으로 적용한다. 기존 영암 프로필·지문·관측 근거는 변경하지 않는다.
- 파일명 끝의 다운로드 표기만 제거한다. 역할은 UNKNOWN이고 최대10파일·중복 제거·기존 서명/MIME/파일명 검증을 유지한다.
- 미해석 링크·미지원 형식·잘못된 개별 폼을 기록하면서 정상 descriptor는 보존한다. 영역 누락은 첨부 없음이 아니다.
- 카탈로그는 참조 전용·기대값 null이다. 키워드·공통 다운로드 엔진·다른 지역·DB/API·migration·추출기 변경은 없다.

운영 DB/설정/worker/정책 변경·ENFORCE·재분류·배포는 이번 범위가 아니다. 브라우저는 현재 사용자 정책에 따라 미실행이다. AWS 인증 갱신 및 이전 연제/구례 조사 원본 정리 미완료는 별도 후속이다.

## 검증 결과

- 실행: `gradlew.bat --no-daemon :test`에서 신안 계약·본문·카탈로그·정책 snapshot·inventory·파일 형식·worker probe 패키지 검사를 선택하고 `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=SHINAN -PsanebCollectionWindowsTrust=true`를 실행했다. Java285개 통과, 실패·오류·생략0, bootJar 성공이다.
- 실제 관측: 2026-09-30 13:31:14 KST, `SHINAN-38211`, 제목 조합 통과, 본문150자 AVAILABLE, 첨부 FOUND/complete, HWP1개123,392byte 형식 검증·다운로드 성공이다.
- 파일 SHA-256: `f5189b33159b6e4ba5179e04b36c3d0d8c7eb50d3517a1e6d2e24a2020603515`.
- 요청 예약 상한 포함 실적4/6회·2,310,656/24,117,248byte, 임시 원본 정리 확인, 운영 쓰기0이다. `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`이며 추출·전체 분석·정책 승인 완료가 아니다.
- Node23개 검사와 영수증 재현 검증을 통과했다. 영수증304건·공고243개이며 기존 근거를 보존했다.
- 최신 수집 가능 집계는 **165/223수집원(74.0%)·58잔여(미연결30+등록 다운로드 미확인28)**다. 전체 첨부 세트의 기존 엄격 Gate16/223과 구분한다.
- 프로필194개(지역193+기업마당1), 카탈로그252공고/193대상/참조251+기존 기대값1이다. 운영 적용과 브라우저 검증은 하지 않았다.
