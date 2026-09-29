# 춘천 JSON 본문·첨부와 worker 공통 경로 연결

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 외부 공고 자동 활성화 금지를 유지한다. HWP 추출기 1.0.16 추가 개선은 보류한다. 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E를 포함한 전체 장기 goal은 미완료다.

- [x] 춘천 LGS-000117 / CHUNCHEON_NOTICE_JSON 시스템 프로필 연결.
- [x] 공식 JSON 공고 ID·제목·본문·파일 배열 계약과 UTF-8/MIME/요청 경계 구현.
- [x] worker·단건 QA·실제 관측 실행기에 명시적 JSON 프로필 분기 연결.
- [x] 합성 worker에서 부분 발견 후 정상 파일 보존, 공고 ID 불일치 후 파일 요청 차단 확인.
- [x] 합성 단건 QA에서 JSON 제목 대조와 실제 production 형식 검사 연결 확인. HTTP·추출기는 대역이며 운영 E2E 증거가 아니다.
- [x] 전체 첨부 도메인 회귀·bootJar·춘천 실제 HWP 다운로드·252영수증/208공고 재현 통과.
- [x] 본문 382자·HWP 1개 104,960byte 수집·형식 검증, 임시 원본 정리.
- [ ] 미등록 59지역 연결, 등록 후 다운로드 미확인 35지역 후속.
- [ ] 전국 목록 자동 유입·첨부 내용 분석·운영 상시 worker·DB/API/UI·운영 E2E.

## 명시적 JSON 계약

춘천 공식 상세 화면은 정적 HTML 안에 본문/첨부를 넣지 않고 고정 JSON API로 조회한다. 앞선 [구조 조사](announcement-gangwon-province-download-2026-09-30.md)의 공개 표본 73071을 사용한다.

- 화면 경로: `https://www.chuncheon.go.kr/cityhall/administrative-info/notice-info/notice-announcement/view/?notAncmtMgtNo=73071`.
- 본문/첨부 API: `https://www.chuncheon.go.kr/_chuncheon/noticeView.do?notAncmtMgtNo=73071`.
- 상세 응답의 `board.not_ancmt_mgt_no`가 요청 공고 ID와 일치해야 한다. 제목과 본문은 문자열이어야 하며, 파일 목록은 명시적인 배열이어야 한다. 누락/null 배열을 첨부 없음으로 보지 않는다.
- JSON 중복 key·후행 문서·잘못된 UTF-8·상한 초과·공고 ID 불일치는 고정 오류 코드로 처리한다. 원문·연락처·임의 exception 메시지를 감사 metadata에 남기지 않는다.
- JSON 안의 본문 HTML은 Jsoup으로 텍스트만 추출하고 script·메뉴·첨부 링크를 정리한다. 담당부서·연락처·작성자·파일명 필드는 본문과 섞지 않는다.
- 실측 API 응답에는 Content-Type 헤더가 없다. 빈 MIME 예외는 춘천 전용 프로필/고정 API에만 한정한다. 명시적 `application/json`과 UTF-8만 추가 허용하며 기존 HTML 프로필의 MIME 계약을 넓히지 않는다.
- 본문 클라이언트도 공식 화면 URL을 같은 host의 고정 JSON API로 매핑한다. JSON API의 redirect는 다른 공고나 경로로 따라가지 않는다.

`AttachmentJsonDetailProfile`은 별도 선택 인터페이스다. 기존 `AttachmentDiscoveryProfile`을 수정하지 않으므로 기존 프로필의 지문·허용 계약을 보존한다. worker·QA는 JSON 프로필에서만 원문 JSON을 전달하고 기존 프로필은 계속 HTML을 파싱한다. 춘천 프로필 지문에는 JSON 인터페이스·춘천 파서·query 파서가 포함된다. 애플리케이션 코드/QA 산출물이 변경되므로 운영 정책 QA/배포 완료를 뜻하지 않는다.

## 첨부 발견·부분 실패

`file` 배열에서 `file_seq`, `file_nm`, `sys_file_nm`, `file_path` 문자열을 검증한다. 숫자 순번, 안전한 파일명, `/ntishome/file/upload/ofr/ofr/{8자리 날짜}` 경로만 사용하여 공식 `eminwon.chuncheon.go.kr/emwp/jsp/ofr/FileDown.jsp` GET을 구성한다. UTF-8 URL 인코딩을 적용하며 JSON을 JavaScript로 실행하지 않는다.

- 원래 파일명과 시스템 파일명의 PDF/HWP/HWPX 형식이 일치할 때만 다운로드한다. metadata의 파일 크기를 실제 파일 검증으로 취급하지 않는다.
- 중복·상충 순번/파일, 잘못된 행은 오류로 기록한다. 이미 확인한 정상 descriptor는 보존한다. 전체 JSON 문법·중복 key 오류는 목록을 신뢰할 수 없으므로 발견 실패다.
- 최대 10파일, 상세 1MiB, 고정 HTTPS443 host/path·source/parser·URL hash·동일 요청 redirect 경계를 유지한다.
- 파일 역할은 UNKNOWN이다. 파일명으로 공고/신청서를 확정하지 않는다.
- 공식 화면이 첨부를 표시하지 않는 공고 18207·18265·26243·40349·65304·68021은 첨부 프로필의 요청 단계에서 차단한다. 제공자의 제한을 우회하지 않는다.
- 원본은 임시 공간에서만 처리하며 파일 다운로드·형식 검증과 추출·구간 분석 성공은 별개다.

## 테스트 중 발견·수정

1. 첫 표적 검사 218건 중 합성 단건 QA 2건이 제목 단계에서 중단됐다. 합성 규칙은 `지원금`을 요구했지만 테스트에 실제 `지원사업` 제목을 사용한 것이 원인이다. 합성 제목을 기존 합성 규칙에 맞췄고 실제 표본 제목·DRAFT 규칙은 변경하지 않았다.
2. 다음 검사에서 합성 기대값이 production과 다른 locator JSON 정렬 방식으로 hash를 계산했고, 제목 중단 시 예상 파일을 `NOT_RUN`으로 유지하는 기존 계약을 빈 목록으로 잘못 기대했다. production hash 함수를 사용하고 `NOT_RUN`/파일 요청 0/추출 미실행을 명시적으로 검증하도록 수정했다. 이후 단건 QA suite가 통과했다.
3. 첫 확대 검사 2,771건 중 이전 화순 프로필 등록 후 갱신되지 않은 registry/QA 계획 개수 5건이 실패했다. 실제 구성은 새올18, 화천 결합21, 법정공고 결합24, 표준BBS 결합34개이며 이 결합의 QA 계획은 정부24 미등록 항목을 포함한 35대상이다. 기대값과 테스트 이름을 갱신하고 `LOCAL_HWASUN_GET_V1` 포함을 추가 검증한다. host·지문·MIME·파일 검사는 삭제하거나 완화하지 않는다.

## 실제 관측과 지역 집계

2026-09-30 03:01 KST 로컬 Java 수집 경로에서 `CHUNCHEON-73071`을 관측했다. 실제 제목은 `2026년 소상공인 경영환경개선 지원사업 공고`이며 합성 QA 제목과 구분한다.

- 제목 COMBINATION_MATCHED, 본문 AVAILABLE 382자 / TARGET_SUPPORT_CONFIRMED, 첨부 FOUND·complete=true.
- HWP 1개 104,960byte / SHA256 `aadc7e5829a556199291cd0190c6e2db950e60c143d3f4137385782fee195db6`.
- 프로필 `LOCAL_CHUNCHEON_JSON_V1` / 지문 `8ed23a6c233504cb788e7098f3625a3a289314a20046848a72f7ee6339384b73`.
- 결과 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`. 이번 실제 파일은 다운로드·형식 검증만 했으며 HWP 텍스트 추출·구간 분석·관리자 확정은 하지 않았다. 본문 ACCEPTED는 조건 기반 후보 판정이다.

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷의 활성 223지역이다. 새 운영 조회는 하지 않았다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 실제 파일 최소 1개 다운로드 확인 | 128 | **129/223 (57.8%)** |
| 다운로드 미확인 | 95 | **94** |
| 프로필 등록 지역 | 163 | **164** |
| 미등록 지역 | 60 | **59** |
| 등록 후 다운로드 미확인 | 35 | **35** |
| 첨부 발견·파일 오류 관측 지역 | 42 | **42** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역 164 + 기업마당 1 = 165프로필, 카탈로그 220공고/163대상이다. 신규 expectation=null이며 과거 219참조·기대값과 251영수증/207공고를 보존했다. 다운로드 확인률은 전체 개발·운영 완료율이 아니다.

## 요청 예산·정리

- 앞선 `gangwonProvinceDownloadRun`의 춘천 조사 4회를 보존했다. 이번에는 구현 fixture 확인을 위해 공식 JSON 1회만 재조회했다. 요청 최대 15초/2MiB, TLS 검증 유지, 인증·쿠키·자동 redirect·미리보기 사용 없음.
- 실제 관측 상한은 6요청/23MiB, 상세 1MiB다. 관측 보고서의 본문 포함 예약 상한은 4요청/2,204,416byte이며, 이전 조사 4회와 이번 조사 1회를 포함한 누적 요청 상한은 9회다. 승인된 이번 계획 상한 11회(이전4+조사1+관측최대6) 이내이며 예약값과 실제 wire 요청 수를 구분한다.
- 조사 원본 `CHUNCHEON-view.json` 2,304byte를 SHA256·길이·정확한 경로 대조 후 개별 삭제했다. 관측 임시 JSON/HWP는 실행기가 정리했다. 원본은 공식 사이트를 다시 조회해야 확보할 수 있다. 원문·연락처·다운로드 query는 감사 metadata에 남기지 않았다.
- 단발 Node/Java/Gradle과 사용한 임시 패치 도구를 정리하며 기존 사용자 `output/`·`scripts/qa/__pycache__/`는 보존한다.

## 검증 명령 / 결과 / 미검증

```powershell
# 보관 운영 대상 스냅샷과 이번 실제 JSON fixture 환경변수 활성화 후 실행
.\gradlew.bat :test --tests 'com.saneb.domain.announcementattachment.*' --tests '*LocalGovernmentNoticeProviderContentClientTest' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=CHUNCHEON -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

최종 확대 실행 4분43초 성공. JUnit XML 합계 2,811항목 중 **2,740통과/71조건부 생략/0실패/0오류**, Node 23/23, bootJar 성공, 실제 춘천 관측 성공. Gradle 콘솔의 테스트 수 2,771/생략31은 비활성 parameterized/container 40개를 세는 방식이 XML 집계와 달라 통과 수는 동일하다. 생략은 다른 기관의 외부 실측, 이미 정리된 HTML fixture, Linux 추출/런타임 조건부 검사 등이며 해당 항목을 통과로 처리하지 않는다. 춘천 실제 JSON fixture와 별도 수집 관측은 실행했다.

기존 251영수증과 신규 1영수증을 재이관해 208최신 공고 및 지역 대장과 일치했다. 다른 지역의 상태·프로필 지문·이전 요청 원장은 유지했다. 합성 worker/QA는 실제 production 코드 경로를 검증하지만 HTTP·Linux 추출 대역을 사용하므로 운영 worker/DB/API 통과 증거가 아니다.

## 운영 경계와 후속

스키마/migration·`/api/v1`·관리자 UI·분류 정책·추출기·운영 DB/설정·ENFORCE·기존 데이터는 변경하지 않는다. 운영 배포는 실행하지 않는다. 브라우저 검증은 현재 요청에 명시되지 않아 사용자 정책에 따라 생략한다.

춘천이 완료되더라도 다른 미등록 지역 및 수집 실패 지역을 계속 처리한다. 이번 합성 worker 검증을 실제 운영 worker·DB/API·관리자 검수·DRAFT·운영 E2E 통과로 표시하지 않는다.
