# 세종 본문·첨부 연결과 울주 목록 표본 후속

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 외부 공고 자동 활성화 금지를 유지한다. HWP 추출기 1.0.16 추가 개선은 보류하며 전체 장기 goal과 운영 E2E는 미완료다.

- [x] 세종 일반공고 본문 경계·직접 GET 첨부 프로필 연결.
- [x] 실제 HTML 제목·본문·첨부 경계 및 오류 분리 계약 검증.
- [x] 세종 본문232자·HWP1개65,536byte 실제 수집·파일 형식 검증.
- [x] Java232통과/조건부1생략, Node23통과, bootJar, 수집 근거248건/공고204건 재현.
- [x] 조사 HTML7개1,438,203byte 길이·SHA256 대조 후 개별 삭제.
- [ ] 미등록63지역 연결, 등록 후 다운로드 미확인34지역 후속.
- [ ] 울주 승인된 제목 조건을 충족하는 공개 표본 확보 및 상세·첨부 연결.
- [ ] 전국 목록 자동 유입·첨부 내용 분석·상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E.

## 지역 집계

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷의 활성223지역이다. 이번 운영 조회·변경은 없다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 실제 파일 최소1개 다운로드 확인 | 125 | **126/223 (56.5%)** |
| 다운로드 미확인 | 98 | **97** |
| 프로필 등록 지역 | 159 | **160** |
| 미등록 지역 | 64 | **63** |
| 등록 후 다운로드 미확인 | 34 | **34** |
| 첨부 발견·파일 오류 관측 지역 | 41 | **41** |
| 기존3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역160+기업마당1=161프로필, 카탈로그216공고/159대상이다. 신규 expectation=null이며 기존 승인 기대값은 그대로다. 다운로드 확인률은 전체 개발·운영 완료율이 아니며, 오류41지역은 본문·목록 오류 전체를 합한 수치가 아니다.

## 실제 관측

2026-09-30 01:54 KST 로컬 Java 수집 경로에서 세종 LGS-000083의 고정 공개 공고68219를 검증했다. 제목은 `'26년 세종특별자치시 소상공인자금 지원계획 변경공고`다.

- 제목 COMBINATION_MATCHED, 본문 AVAILABLE·232자·TARGET_SUPPORT_CONFIRMED.
- 첨부 FOUND·complete=true, HWP1개65,536byte DOWNLOADED.
- 파일 SHA256 `c7ae1a6627243525766e2656a49093ce35e04cd89c2f41a10fdb9c51cc2b7f8a`.
- 프로필 `LOCAL_SEJONG_BOARD_V1`, 지문 `eedf85a413bf20b5240c4ff78a1fa1ccd9a0a91df15922447808ffcb948a6042`.
- 원본 삭제 확인. 첨부 역할은 UNKNOWN이며 텍스트 추출·구간 분석은 실행하지 않았다.

본문 ACCEPTED는 규칙상 후보 판정이다. 현재 모집 여부·관리자 확정·운영 등록·정책 QA·기대값 승인·운영 E2E를 증명하지 않는다. 고정 상세 표본의 성공이 전체 목록에서의 자동 유입이나 상시 수집 완료를 뜻하지 않는다.

## 구현 경계와 오류 처리

`SejongNoticePage`는 일반공고 C1_1의 정확한 경로에서 `div#txt > div.table-responsive > table.table.table-bordered`를 선택하고, 제목·파일첨부 라벨의 셀과 `td.tbl_cnts.cell_left` 본문을 분리한다. 담당부서·문의전화·메뉴·파일 이름을 본문에 혼합하지 않는다. 중복 구조·빈 본문은 기존 오류 처리로 남긴다.

`SejongAttachmentDiscoveryProfile`은 공식 첨부 칸의 `div.download` 내부 직접 링크만 허용한다. 대표 홈페이지 상세와 공식 eminwon 파일 서버의 HTTPS443·정확한 경로·세 query 필드·날짜 디렉터리·안전한 파일명을 검증한다. 공백과 한글 파일명은 URI 인코딩 후 요청한다. 파일명·시스템명이 같은 형식일 때만 지원 형식으로 표시한다.

공식 파일 형식 아이콘은 정해진 이미지 경로와 속성만 식별해 제거한다. 미해결 링크·요소·비정상 요청은 오류로 보존하면서 정상 descriptor를 유지한다. 다른 host·사설 주소·중복 query·경로 이탈·임의 함수·POST·다른 파일로의 redirect는 허용하지 않는다. JavaScript·미리보기·일괄 다운로드는 실행하지 않는다. 중복·상충·10파일 상한·미지원 파일 비다운로드를 검증했다.

기존 지역 프로필 지문·migration·DB/API/UI·추출기·운영 source 설정·분류 규칙을 변경하지 않았다.

## 울주 후속과 조사 예산

울주 LGS-000082는 기존 공식 메뉴에서 고시공고 목록으로 이동한다. 공개 이동 안내의 같은 기관 공식 목록 경로를 확인하고, 임시 query 값을 저장하거나 사용하지 않는 정규 목록 요청으로 구조를 조사했다. `seCode=01`에서 소상공인 검색은 결과가 없었고, 지원 검색은 지원사업 표본으로 채택할 수 없는 행정성 제목2건이었다. 상세·파일 요청은 하지 않았다. 이 결과만으로 울주 전체에 지원사업이나 첨부가 없다고 단정하지 않는다. 다른 공식 목록 범위의 적합성·제목 조건 충족 표본·상세 연결은 후속이다.

- 이번 조사7회: 세종 목록·소상공인 검색·상세3회, 울주 메뉴·이동 후 목록·소상공인 검색·지원 검색4회. 각 요청 최대15초/2MiB, 자동 redirect·인증·쿠키 사용 없음.
- 세종 실제 관측 예산은 최대6예약/23MiB·상세1MiB다. 보고된 예약4회/2,383,872byte이며 조사 포함 상한11회다. 예약 수를 실제 HTTP 횟수와 동일시하지 않는다.
- 조사 HTML7개1,438,203byte를 길이·SHA256 대조 후 개별 삭제했다. 복구가 필요하면 승인 범위 내에서 다시 수집해야 한다. 첨부 원본은 관측 도구의 자체 정리 결과를 확인했다.
- 원문·개인정보·인증정보는 근거 인덱스나 문서에 복사하지 않았다.

## 실행 명령 / 결과

1. `SANEB_SEJONG_SURVEY_FIXTURE=true`에서 `:test --tests '*SejongDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest'`: 최종98통과,21초. 앞선2회는 신규 테스트가 기존 Request 생성자의 GET-form·빈 POST 거부를 잘못 기대해 실패했다. 생성자 거부와 세종의 POST 거부를 각각 검증하도록 테스트를 수정했으며 운영 보안 검증은 완화하지 않았다.
2. 같은 fixture와 기존 읽기 전용 inventory receipt로 `:test`의 Sejong·DaejeonNext·본문·공식 관측 계약·파일 형식·inventory·catalog·policy snapshot 대상, `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=SEJONG -PsanebCollectionWindowsTrust=true`: 1분46초 성공. Java233개 중232통과/1조건부 생략/실패0. 이전 DaejeonNext 실제 HTML fixture는 원본 정리 후 조건부 생략됐고, 이번 세종 fixture는 실행했다.
3. `node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs`: 23통과.
4. `node scripts/qa/verify-collection-receipt-index.mjs`: 248영수증/204공고 재현. 기존247영수증/203공고 보존.
5. `node scripts/qa/report-collection-availability.mjs`: 126/223확인·97잔여·첨부 오류41지역.

운영 설치·DB·정책·ENFORCE·기존 데이터 배치·worker 설정·배포 변경은 없다. 사용자 정책에 따라 브라우저 검증은 미실행이며 운영 완료로 표현하지 않는다. 다음 미등록 지역 연결을 계속하고 실패·미선정 표본은 별도 후속으로 관리한다.
