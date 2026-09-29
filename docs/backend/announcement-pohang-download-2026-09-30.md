# 포항 기존 엔진 연결과 숫자 미리보기 구분

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대를 우선하며 정상 파일은 먼저 수집하고 실패는 별도 기록한다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지를 유지한다. HWP 추출기 1.0.16 추가 개선과 전체 goal 완료는 별도다.

- [x] 포항 LGS-000201 / SAEOL_GOSI 본문·첨부 연결.
- [x] 기존 포털 새올 엔진 재사용, 숫자 미리보기만 좁게 정규화.
- [x] 실제 본문385자·HWPX1개136,459byte 다운로드 및 기본 형식 검증.
- [x] 확대 Java231건 통과/실패0/생략0, Node23/23, bootJar, 262영수증/217공고 재현.
- [x] 기존228카탈로그·261영수증·216공고·다른 지역 상태·이전 요청 원장 보존.
- [x] 조사 원본5개 정리·이번 Node/Gradle 자원 종료·운영 쓰기0.
- [ ] 남은86지역: 미등록50 + 등록 후 다운로드 성공 미확인36.
- [ ] 첨부 추출·구간 분석·운영 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E.

## 공식 조사와 구현

목록 `https://www.pohang.go.kr/portal/saeol/gosi/list.do?mid=0202010000`와 공식 POST 제목 검색에서 표본을 확인했다. 오래된 목록 주소의 동적 값은 재사용하지 않았다. 화면 링크는 `data-action`과 POST 호출을 사용하지만 동일한 상세 URL의 GET 정상 응답도 실제 확인했다. 이것이 운영 목록 스케줄러 연동 성공까지 입증하는 것은 아니다.

고정 표본 `POHANG-73525`의 제목은 `2026년 소상공인 고효율기기 지원사업 시행 공고`다. 상세는 `https://www.pohang.go.kr/portal/saeol/gosi/view.do?notAncmtMgtNo=73525&mid=0202010000`다. 현재 신청 기간·지원 자격을 확인한 결과는 아니다.

`PohangNoticePage`는 고정 메뉴0202010000·숫자 공고 번호·HTTPS·정확한 query와 단일 `form#detailForm div.bod_view`를 검증한다. `div.subject` 제목과 `div.view_cont` 본문을 분리하고 담당부서·첨부·메뉴를 본문에 포함하지 않는다.

첨부는 기존 `GyeongbukPortalAttachmentDiscoveryProfile`에 위임한다. 최초 실제 HTML 검사에서는 포항 미리보기의 두 번째 인수가 숫자 순번인 점 때문에 발견 오류1건이 발생했다. 다운로드 descriptor는 보존됐으나 전체 발견 성공으로 표현하지 않았다.

`PohangAttachmentDiscoveryProfile`은 공식 첨부 영역의 인접 링크를 확인하고 같은 공고·같은 파일 인수3개가 일치하는 숫자 미리보기만 기존 엔진 형식으로 정규화한다. 미리보기는 실행하거나 요청하지 않는다. 불일치·추가 스크립트는 오류로 남기며 정상 파일은 유지한다. 공통 엔진 및 기존 프로필 지문은 수정하지 않았고 새 wrapper 지문에 위임 엔진과 관련 파서를 포함했다.

다운로드는 HTTPS `eminwon.pohang.go.kr/emwp/jsp/ofr/FileDown.jsp`의 검증된 세 필드 GET만 허용한다. 최대10파일·UNKNOWN 역할·PDF/HWP/HWPX만 다운로드·미지원 형식·첨부 없음·부분 실패 구분을 유지한다. 임의 호스트·경로·redirect는 허용하지 않는다.

## 실제 결과 / 집계

2026-09-30 05:17 KST(UTC `2026-09-29T20:17:10.666744500Z`) 관측에서 제목 COMBINATION_MATCHED, 본문 AVAILABLE385자 / ACCEPTED / TARGET_SUPPORT_CONFIRMED, 첨부 FOUND·complete=true, HWPX1개136,459byte DOWNLOADED를 확인했다.

- 프로필: `LOCAL_POHANG_PORTAL_V1`
- 프로필 지문: `f00c02333673443becb486234911ab66ce14e55f5076b7dbb723d5a2c58a7ed2`
- 파일 SHA256: `33c29766f51b56853769b95ca93ab41e5d8779a2d548d2f32b8b47eaf19cc687`
- 결과: `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`, 원본 정리true, 운영 쓰기0.

신규 카탈로그 expectation은 null이다. 기본 형식 검증은 텍스트 추출·구간 분석 성공이 아니며 본문 ACCEPTED도 관리자 최종 확정이 아니다.

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷의 활성223지역이다. 이번 운영 재조회는 없다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 최소1개 파일 다운로드 확인 | 136 | 137/223 (61.4%) |
| 다운로드 미확인 | 87 | 86 |
| 미등록 | 51 | 50 |
| 등록 후 다운로드 미확인 | 36 | 36 |
| 첨부 오류가 남은 지역 | 43 | 43 |
| 기존3표본·전체 첨부 Gate 충족/잔여 | 16/207 | 16/207 |

지역173+기업마당1=174프로필, 카탈로그229공고/172대상이다. 다운로드 확인율은 전체 구현·운영 완료율이 아니다.

## 예산·정리·검증

조사3요청(목록·검색·상세 각1)과 실제 관측 본문 포함 예약4회로 누적 예약 상한7회다. 조사 각15초/HTML2MiB, 실제 관측 최대6요청/23MiB·상세1MiB로 제한했다. 관측 예약 byte는2,528,523이며 예약 수를 실제 wire 요청 수로 단정하지 않는다.

조사 HTML·헤더5개883,417byte는 정확한 경로·길이·SHA256 대조 후 삭제했다. 실제 첨부 임시 원본은 실행기가 정리했다. 원본은 공식 사이트 재조회가 필요하며 원문·연락처·헤더 값은 저장소에 기록하지 않았다.

```powershell
# 실제 포항 fixture와 보관 대상 스냅샷 환경변수 활성화 후
.\gradlew.bat :test --tests '*PohangDownloadContractTest' --tests '*GyeongbukFirstDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=POHANG -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

최초 표적110건 중 실제 HTML 미리보기 발견 오류1건 실패. 수정·회귀 검사 추가 후111건 통과(26초). 확대231건 모두 통과·bootJar·실제 관측 성공(2분8초). Node23/23, 262영수증/217공고 재현 통과다. 원본 정리 후 fixture 환경변수 없이 실행하면 실제 HTML 검사1건은 조건부 생략된다.

전체 프로젝트 테스트·첨부 추출·AWS·운영 DB·실제 운영 worker/DB/API/UI·운영 E2E는 이번 회차 미실행이다. 브라우저 검증은 현재 요청에 명시되지 않아 정책상 생략했다. migration/API/UI/분류 정책/추출기/운영 설정·배포·정책 게시·ENFORCE·기존 데이터 적용은 변경하지 않았다.
