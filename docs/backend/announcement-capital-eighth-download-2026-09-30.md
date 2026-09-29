# 파주·광명 본문·첨부 연결과 파일 응답 오류 분리

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 외부 공고 자동 활성화 금지를 유지한다. HWP 추출기 1.0.16 추가 개선은 보류하며 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E를 포함한 전체 장기 goal은 미완료다.

- [x] 파주·광명 본문 경계·첨부 프로필2개 연결.
- [x] 실제 HTML의 파주 직접 PDF 링크와 광명 정적 JSON 파일 목록 검증.
- [x] 광명 본문510자·HWP3개313,344byte 실제 다운로드·형식 검증.
- [x] 파주 본문144자 확보, 파일 응답 이름 헤더 검증 실패를 별도 기록.
- [x] Java234통과/조건부1생략, Node23통과, bootJar,250영수증/206공고 재현.
- [x] 조사 원본9개4,624,532byte 길이·SHA256 대조 후 개별 삭제.
- [ ] 미등록61지역 연결, 등록 후 다운로드 미확인35지역 후속.
- [ ] 파주 파일 응답 오류·화성 적합 표본 확보.
- [ ] 전국 목록 자동 유입·첨부 내용 분석·상시 worker·DB/API/UI·운영 E2E.

## 지역 집계

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷의 활성223지역이다. 이번 운영 조회·변경은 없다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 실제 파일 최소1개 다운로드 확인 | 126 | **127/223 (57.0%)** |
| 다운로드 미확인 | 97 | **96** |
| 프로필 등록 지역 | 160 | **162** |
| 미등록 지역 | 63 | **61** |
| 등록 후 다운로드 미확인 | 34 | **35** |
| 첨부 발견·파일 오류 관측 지역 | 41 | **42** |
| 기존3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역162+기업마당1=163프로필, 카탈로그218공고/161대상이다. 신규 expectation=null, 기존 승인 기대값은 보존했다. 다운로드 확인률은 전체 개발·운영 완료율이 아니다. 오류42지역은 본문·목록 문제를 모두 합한 수치가 아니다.

## 실제 관측

2026-09-30 02:11 KST 로컬 Java 수집 경로에서 고정 공개 표본을 관측했다.

| 지역 / 공고 | 본문 | 첨부 |
|---|---|---|
| 파주 LGS-000096 / 20260119101158902 | AVAILABLE144자·TARGET_SUPPORT_CONFIRMED | PDF 표시 링크1개 발견,93,037byte 전송 후 FILE_SIGNATURE / ATTACHMENT_DISPOSITION_INVALID. 다운로드 성공으로 세지 않음 |
| 광명 LGS-000102 / 65908 | AVAILABLE510자·TARGET_SUPPORT_CONFIRMED | HWP121,344byte·98,816byte·93,184byte, 총313,344byte 검증 통과 |

파주 제목은 `2026년 파주시 소상공인 운전자금 지원계획 공고`, 광명 제목은 `2026년 소상공인 경영환경개선 지원사업 모집 공고`다. 고정 표본 기술 검증이며 현재 모집·운영 등록·관리자 확정을 뜻하지 않는다. 두 본문 ACCEPTED는 규칙상 후보 판정이다.

프로필 지문:

- 파주 `1cd76e6edda78e6cf879fe81597a399bd0c29a0641e238f336eb7c6a1b635d1b`.
- 광명 `1c53827f824f0f270c3133cebb905ef5cc4525ea5d15694ef24ebb875b7c9ba7`.

광명 파일 SHA256은 순서대로 `be07923de848eca4d4af605ab55c90e903d35bfc8683d42c903ec062e3443fb8`, `bb1e27a17ff349e21e7e643914f4403e4736d444f50273b62fd8f484cc8709c9`, `aa2aac83ab6844f068e696d410e9c5a6dbec738b27fd8b99f942e5b4b9641043`이다. 파주 전송 해시는 영수증에 남기지만 해시·바이트 수만으로 PDF 형식 검증 성공을 주장하지 않는다. 같은 오류의 외부 재요청이나 검증 완화는 하지 않았다.

## 구현과 제한

`CapitalEighthNoticePage`는 파주의 article-view에서 제목의 고시공고 분류 라벨을 분리하고 article-conetnt 본문, file-list 첨부를 선택한다. 광명은 bbsView의 제목·내용·첨부파일 셀을 분리한다. 담당부서·문의처·파일명·메뉴는 본문 근거에 혼합하지 않는다.

파주는 공식 UUID 기반 직접 GET 다운로드만 허용한다. 짝지어진 바로보기 링크는 같은 UUID인지 비교하되 호출하지 않는다. 바로보기 상충이나 미해결 요소가 있어도 정상 파일 descriptor는 보존한다.

광명은 서버가 전달한 정적 `fileList.push` 객체를 JSON으로 해석한다. 스크립트를 실행하지 않는다. 파일명·시스템명·날짜 디렉터리·파일ID의 네 문자열 필드만 허용하고 중복 JSON key, 외부 함수 호출, 미해석 목록 변경은 오류로 남긴다. 공식 fileDown form의 HTTPS 파일 host/path·POST·세 hidden 필드를 검증하여 고정 요청을 구성한다. 빈 `otherList` 영역만 보고 첨부 없음으로 처리하지 않으며 정적 목록을 함께 검증한다. 정상 항목은 다른 항목 해석 오류와 별도로 보존한다.

두 프로필 모두 source·parser·URL hash, HTTPS443, 같은 요청에 한정된 redirect, 안전한 파일명, 중복·상충·10파일 상한을 유지한다. 미지원 파일은 다운로드하지 않는다. 파일명에 공고·신청서가 있어도 역할을 추정하지 않고 UNKNOWN으로 유지한다. 텍스트 추출·구간 분석·정책 QA·기대값 승인·운영 E2E는 실행하지 않았다.

기존 지역 프로필·migration·DB/API/UI·추출기·분류 규칙·운영 source 설정은 변경하지 않았다. 광명 QA는 공식 HTTPS 상세를 사용했으며 저장된 HTTP 목록에서의 전체 자동 유입을 증명하지 않는다.

## 조사·예산·정리

- 조사9회: 파주 목록·소상공인 검색·공식 board 스크립트 정적 조회·상세4회, 광명 목록·소상공인 검색·상세3회, 화성 목록·소상공인 검색2회. 각 요청 최대15초/2MiB, 자동 redirect·인증·쿠키 사용 없음.
- 화성 LGS-000088은 등록된 `q_notAncmtSeCode=01` 범위의 이번 제목 검색에서 표본 미확보다. 상세·파일 요청은 하지 않았으며 지역 전체에 지원사업이나 첨부가 없다고 단정하지 않는다.
- 관측 예산은 각 지역 최대6예약/23MiB·상세1MiB. 실제 보고 예약 합계10회/5,755,757byte, 조사 포함 상한19회다. 예약 수를 실제 HTTP 횟수와 동일시하지 않는다.
- HTML8개·공식 스크립트1개, 총4,624,532byte를 길이·SHA256 대조 후 개별 삭제했다. 복구가 필요하면 승인 범위 내에서 다시 수집해야 한다. 첨부 원본은 관측 도구 자체 삭제 결과를 확인했다.
- 원문·개인정보·인증정보는 문서나 근거 인덱스에 복사하지 않았다.

## 실행 명령 / 결과

1. `SANEB_CAPITAL_EIGHTH_SURVEY_FIXTURE=true`에서 `:test --tests '*CapitalEighthDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest'`:105통과,31초.
2. 같은 fixture와 기존 읽기 전용 inventory receipt로 `:test`의 CapitalEighth·Sejong·본문·공식 관측 계약·파일 형식·inventory·catalog·policy snapshot 대상, `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=PAJU,GWANGMYEONG -PsanebCollectionWindowsTrust=true`:1분57초 성공. Java235개 중234통과/조건부1생략/실패0. 이전 Sejong HTML fixture는 원본 정리 후 생략됐고 이번 두 지역 fixture는 실행했다. 관측 태스크 성공을 파주 파일 다운로드 성공으로 표현하지 않는다.
3. `node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs`:23통과.
4. `node scripts/qa/verify-collection-receipt-index.mjs`:250영수증/206공고 재현, 기존248영수증/204공고 보존.
5. `node scripts/qa/report-collection-availability.mjs`:127/223확인·96잔여·첨부 오류42지역.

운영 설치·DB·정책·ENFORCE·기존 데이터 배치·worker 설정·배포 변경은 없다. 사용자 정책에 따라 브라우저 검증은 미실행이며 운영 완료로 보고하지 않는다. 남은 미등록 지역 연결을 우선하고 파일·파싱·목록 오류는 별도 후속으로 관리한다.
