# 울주 본문·첨부 연결 및 광양 후속 조사

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결을 우선한다. 정상 파일은 수집하고 개별 발견·다운로드·추출 오류는 분리한다. HWP 추출기 추가 개선은 보류하며 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E 전체 goal은 미완료다.

- [x] `LGS-000082 / SAEOL_GOSI` 공식 제목·본문·첨부 분리와 파일 GET 연결.
- [x] 공고67082의 본문696자·HWPX1개58,690byte 다운로드·형식 검증·임시 원본 정리.
- [x] 집중 Java148개, 회귀 선택 Java346개, bootJar, Node23개 통과.
- [x] 기존 기록 보존과327개 관측 기록·263개 최신 공고 재현.
- [~] 183/223수집원 다운로드 확인(82.1%), 40잔여(미연결12+등록 다운로드 미확인28).
- [ ] 광양 공고61869 본문·공개 POST 첨부 연결.
- [ ] 울주 고시/일반공고 목록 범위 보정과 상시 유입 검증.
- [ ] 추출·worker 영속화·DB/API/UI·관리자 최종 검증·운영 E2E 후속.

## 구현

`UljuNoticePage`는 HTTPS `www.ulju.ulsan.kr/ulju/saeol/gosi/view.do`의 공고번호와 고정 고시/일반공고 메뉴를 검증한다. 입력 범주를 다른 범주로 변환하지 않는다. 공식 `form#detailForm > div.bod_wrap > div.bod_view` 아래 제목 `h4`, 본문 `div.view_cont`, 첨부 `dl.view_file`을 분리한다. 메타데이터·첨부 파일명·메뉴는 본문 판정 근거에 섞지 않는다.

`UljuAttachmentDiscoveryProfile`은 공식 `ul#updateFileList > li`의 다운로드 링크만 해석한다. `goDownload_location`의 세 문자열 인자를 기존 안전한 호출 해석기와 새올 파일 경로 검증기에 연결한다. `eminwon.ulju.ulsan.kr/emwp/jsp/ofr/FileDown.jsp` GET만 허용하고 다른 호스트·경로·POST·redirect 전환은 거부한다. JavaScript를 실행하지 않는다.

`downloadToHttp` 미리보기는 같은 공고·같은 파일을 가리킬 때 부속 요소로만 인정하고 요청하지 않는다. 미리보기 오류나 다른 파일의 실패가 정상 파일 descriptor를 삭제하지 않는다. 미지원 형식·빈 첨부·변경된 구조·중복 상충·10파일 제한은 별도 상태로 기록한다. 역할은 UNKNOWN이며 파일명으로 자동 공고문 판정하지 않는다.

기존 공유 엔진·HWP 추출기·API·분류 규칙·migration(V85까지)은 변경하지 않았다. 프로필212개(지역211+기업마당1), 카탈로그272공고/211대상/참조271+기존 기대값1이다.

## 실제 관측

2026-09-30 18:31:39 KST, `ULJU-67082`(2026년 제2차 울주군 소상공인 자금 특례보증 지원 공고(수정)):

- 제목 COMBINATION_MATCHED, 본문 AVAILABLE/ACCEPTED, 696자.
- 발견1·다운로드1·실패0. HWPX58,690byte.
- 파일 SHA-256: `71697fac878f9e7181afaabe698da1827b609d0a31fbdb0a8b62964146905d57`.
- 프로필 지문: `746894d85e5edb61672324a5c5429f6402f4f90a2ff1185c5f23f8e3beb201e8`.
- 보고서 `build/reports/attachment-regional-collection/ULJU-67082.json`.
- 예약 요청4/6회·예약 바이트2,417,986/24,117,248.
- 원본 정리=true·운영 쓰기=0·추출/정책 QA/기대값 승인/전체 텍스트 분석=false.

다운로드 확인은183/223, 잔여40이다. 현재 지문 기준 오류 지역45개는 성공 지역과 중복된다. 엄격한 전체 세트 Gate는16/223으로 유지한다. 분모는2026-09-28 inventory이며 이번에 운영 등록 상태를 재조회하지 않았다. HWPX 실파일1개 관측을 PDF/HWP 실파일 검증이나 상시 운영 완료로 확대하지 않는다.

## 목록 범위 및 다음 대상

울주 목록·상세 구조 조사는 [화성 후속 기록](announcement-hwaseong-download-2026-09-30.md)의 재개 후7회에 포함되어 있으며 이번 관측에서 중복 집계하지 않는다. V61 등록 메뉴는 고시0403010000, 이번 표본은 일반공고0403020000이다. 고정 상세 연결이 상시 일반공고 유입을 증명하지 않으므로 목록 범위는 별도 후속으로 둔다. 코드상 일반 목록 링크 해석은 `data-action`을 지원하지만 실제 운영 설정·최신 목록→worker 전체 흐름은 미검증이다.

이번 추가 공개 조사는 광양3GET(검색1·상세2)이다. 요청당12초·2MiB, 수집기 User-Agent·TLS 검증·자동 redirect 금지, 원문 파일 저장0을 유지했다.

- 앞선 공식 검색 POST404는 보존한다. 동일 공개 검색 필드의 GET은200이며 공고61869(2026년 광양시 소상공인 융자금 이차보전 계획 공고(2차))를 반환했다. 인증·CSRF 우회는 하지 않았다.
- 상세 `/saeol/gosi.es?mid=a10909020000&act=view&type_code=02,04&seq=61869&nPage=1`은200. 공식 영역 `div.p-wrap.bbs.bbs_view`, 제목 `th.bbs_tit`, 본문 `td.view_content`, 첨부 `td.view_file`을 관측했다. 정확한 상하위 구조 검증은 후속 구현에서 수행한다.
- 첨부 HWPX 표시1개, `goDownLoad` 세 문자열 인자와 `form[name=nnn][method=post]`를 확인했다. 공식 action은 `https://eminwon.gwangyang.go.kr/emwp/jsp/ofr/FileDownNew.jsp`, 공개 파일 필드는 user_file_nm/sys_file_nm/file_path다. 실제 다운로드 전이므로 성공 수에 포함하지 않는다.

## 검증과 미실행 범위

- `gradlew.bat --no-daemon :test --tests '*UljuDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest'`: 55초,148개·실패/오류/생략0.
- 회귀 선택: Ulju/Hwaseong 계약·목록 collector·본문 client·catalog·policy snapshot·inventory·file type·worker probe. `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=ULJU -PsanebCollectionWindowsTrust=true`와 함께3분52초 성공,346개·실패/오류/생략0. 전체 테스트 suite 통과로 확대하지 않는다.
- Node stage/receipts/availability 테스트23개 통과.
- `node scripts/qa/verify-collection-receipt-index.mjs`:327기록/263공고 재현. 이전326기록의 해시·상태·샘플을 보존하고 울주 외 지역 결과 불변을 확인했다.
- `node scripts/qa/report-collection-availability.mjs`:183확인·40잔여 재현.
- `git -c core.autocrlf=false diff --check`:통과.

사용한 단발 Node와 Gradle 검증 프로세스는 종료됐다. 운영 DB·설정·worker·정책·ENFORCE·기존 데이터·배포는 변경하지 않았다. 브라우저는 사용자 정책상 미실행이다. AWS 인증과 이전 연제·구례 조사 파일 정리 미완료는 별도 후속으로 유지한다.
