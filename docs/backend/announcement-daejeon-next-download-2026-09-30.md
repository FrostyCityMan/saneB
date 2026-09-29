# 유성·대덕 첨부 연결과 제목 중단·실파일 수집 분리

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계다. 제목 → 본문 → 첨부 → 관리자 최종 검증을 유지한다. HWP 추출기 1.0.16 추가 개선은 보류하며, 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E를 포함한 전체 장기 goal은 미완료다.

- [x] 유성·대덕 본문 경계와 공식 첨부 POST 프로필 2개 연결.
- [x] 실제 HTML에서 유성 PDF4개·대덕 HWP1개/PDF2개 발견 계약 확인.
- [x] 유성 제목 조합 미충족 시 본문·첨부 요청 0회 중단 확인. 다운로드 성공으로 집계하지 않음.
- [x] 대덕 본문 509자 확보 및 HWP1개·PDF2개 실제 다운로드·형식 검증.
- [x] Java236통과/조건부1생략, Node23통과, bootJar, 기존 근거 보존·재현 검증.
- [x] 조사 HTML8개 6,465,045byte 길이·SHA256 대조 후 개별 삭제.
- [ ] 미등록64지역 연결, 등록 후 다운로드 미확인34지역 후속.
- [ ] 전국 목록 자동 유입·첨부 내용 분석·상시 worker·DB/API/UI·운영 E2E.

## 지역 집계

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷의 활성223지역이다. 이번 운영 조회·변경은 없다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 실제 파일 최소1개 다운로드 확인 | 124 | **125/223 (56.1%)** |
| 다운로드 미확인 | 99 | **98** |
| 프로필 등록 지역 | 157 | **159** |
| 미등록 지역 | 66 | **64** |
| 등록 후 다운로드 미확인 | 33 | **34** |
| 첨부 발견·파일 오류 관측 지역 | 41 | **41** |
| 기존3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역159+기업마당1=160프로필, 카탈로그215공고/158대상이다. 새 항목 expectation=null이며 기존 승인 기대값은 변경하지 않았다. 다운로드 확인률은 전체 개발·운영 완료율이 아니다. 유성은 연결이 추가됐지만 승인된 제목 조건을 충족하는 실제 다운로드 표본이 없어 미확인34지역에 포함한다. 오류41지역은 본문·목록 오류 전체 집계가 아니다.

## 실제 관측

2026-09-30 01:36 KST 로컬 Java 수집 경로에서 관측했다.

| 지역 / 공고 | 제목·본문 | 첨부 결과 |
|---|---|---|
| 유성 LGS-000075 / 35533 | COMBINATION_NOT_MATCHED, TITLE_NOT_ELIGIBLE_NOT_FETCHED | 본문·첨부 요청0회, 파일0개. 기술 조사 HTML의 PDF4개 발견은 다운로드 성공이 아님 |
| 대덕 LGS-000076 / 1102472206 | 제목 조합 충족, 본문509자·TARGET_SUPPORT_CONFIRMED | HWP138,752byte·PDF2,757,765byte·PDF2,773,443byte, 총5,669,960byte 검증 |

유성 표본은 `[2021년 청년, 신혼부부 전세임대 입주자 모집] 공고`, 대덕 표본은 `2026년 대덕뱅크(소상공인 대출지원사업) 공고`다. 현재 모집·운영 등록·관리자 확정의 증거가 아니라 고정 공개 표본의 기술 관측이다. 유성 제목을 통과시키기 위해 키워드·판정 규칙을 변경하지 않았다. 본문 ACCEPTED도 최종 승인 결과가 아니다.

프로필 지문:

- 유성 `7bf701682b9c3bac42dc4719e3ad34edf8b2db7411234c6fd03f4b4fb70fb2d7`.
- 대덕 `dce81282a890b291e5d0421fc06e9f09ace58f669f0281b4ff51c5261d10bfd7`.

대덕 파일 SHA256:

- HWP `9c2b7ee1d783878938ab001fdb5abf70f9483eb064d8efffbe48b07d73582fd4`.
- PDF `3c971a9e69da5fca8b394d12af07edfe4433ed66bf96bde8b1ced5a986bd50fa`.
- PDF `5360bfa64962aa4649b1c6ec7b17d0a23e066ba492f0554fc77b40ed31343ef6`.

관측 보고서는 원본 삭제를 확인하며, 첨부 역할은 UNKNOWN이다. 텍스트 추출·구간 분석·정책 QA·기대값 승인·운영 E2E를 통과한 것으로 해석하지 않는다.

## 구현과 제한

`DaejeonNextNoticePage`에서 유성 program--contents/bbs--view 및 대덕 table2023의 제목·본문·첨부 칸을 각각 선택한다. 메뉴·담당부서·파일 이름을 본문에 혼합하지 않는다.

두 사이트의 `fn_egov_downFile`에서 세 문자열 인자만 파싱한다. JavaScript는 실행하지 않는다. 공식 fileForm 구조와 세 필드, 고정 HTTPS 파일 host/path를 검증한 뒤 POST를 구성한다. 대덕의 초기 hidden 필드 두 값은 실측 HTML에서 뒤바뀌어 있고 클릭 함수가 덮어쓰므로, 초기 구조를 확인하되 실제 요청은 함수의 파일명·시스템명 순서로 전달한다. 외부 form action·임의 함수 실행·사설 host·다른 요청으로의 redirect는 허용하지 않는다.

유성의 짝지어진 미리보기 링크는 같은 세 인자인지 확인한 뒤 제외하며 호출하지 않는다. 공식 첨부 칸의 미해결 요소·실패 파일은 별도 오류로 유지하고 정상 descriptor를 보존한다. 파일10개 상한, 미지원 형식 비다운로드, 중복·상충 감지를 유지한다.

유성 상세는 실제 약1.2MB이므로 기존 선택형 상세 한도2MiB를 해당 프로필에 적용했다. 대덕은1MiB다. 이 제한은 프로필 지문과 관측 예산에 포함한다. 다른 지역 프로필·migration·DB/API/UI·추출기·운영 설정을 변경하지 않았다.

## 조사·요청 예산과 정리

- 이번 조사8회: 유성5회(여행사 상세, 청년 검색, 전세임대 상세, 장애인 검색, 청소년 검색), 대덕3회(목록, 소상공인 검색, 상세). 각각 최대15초/2MiB.
- 유성의 이전 `metroNextDownloadRun` 조사3회를 보존했다. 이번5회를 더한 누적 조사8회이며 요청 대장을 초기화하지 않았다.
- 유성 HTML 구조 조사는 수집 파이프라인 밖의 제한된 기술 조사다. 해당 원본을 사용자 후보나 성공 다운로드 근거로 저장하지 않고 이번 검증 후 삭제했다. 실제 파이프라인 제목 중단은 추가 요청0회로 검증했다.
- 관측 예산: 유성 최대7예약/26MiB, 대덕 최대6예약/23MiB. 실제 보고 예약 합계6회/7,914,568byte. 조사 포함 요청 상한14회이며 예약 수를 무조건 실제 HTTP 횟수라고 표현하지 않는다.
- 조사 원본8개는6,465,045byte이며 길이·해시 대조 후 개별 삭제했다. 복구가 필요하면 승인 범위 내에서 다시 수집해야 한다. 파일 원본은 관측 도구 자체 정리 결과를 확인했다.
- 본문·첨부 원문, 개인정보, 인증정보를 문서나 영수증 인덱스에 복사하지 않았다.

## 실행 명령 / 결과

1. `SANEB_DAEJEON_NEXT_SURVEY_FIXTURE=true`에서 `:test --tests '*DaejeonNextDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest'`: 32초 성공.
2. 같은 fixture 및 기존 읽기 전용 inventory receipt로 `:test`의 DaejeonNext·MetroNext·본문·공식 관측 계약·파일 형식·inventory·catalog·policy snapshot 대상과 `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=YUSEONG,DAEDEOK -PsanebCollectionWindowsTrust=true`: 1분47초 성공. Java237개 중236통과/1조건부 생략/실패0. 생략은 이전 MetroNext 실제 HTML fixture이며 이번 유성·대덕 fixture는 실행했다.
3. `node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs`: 23통과.
4. `node scripts/qa/verify-collection-receipt-index.mjs`: 247영수증/203공고 재현. 기존245영수증/201공고 내용 보존.
5. `node scripts/qa/report-collection-availability.mjs`: 125/223 확인·98잔여·오류41지역. 전체3표본 Gate16/207은 별도 지표다.

운영 설치·DB·규칙·ENFORCE·배치·worker 설정·배포 변경은 없다. 사용자 브라우저 실행 정책에 따라 브라우저 검증은 미실행이며 운영 완료로 보고하지 않는다. 다음 작업은 남은 미등록 지역 연결과 성공 미확인 지역의 별도 오류·표본 확보다.
