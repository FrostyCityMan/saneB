# 상주 본문·HWP·PDF 수집 연결

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대를 우선한다. 파싱·수집 오류는 별도로 남기고 정상 파일부터 처리한다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지를 유지한다. HWP 추출기 1.0.16 추가 개선은 보류하며 전체 goal은 미완료다.

- [x] 상주 LGS-000208 / SAFE_SANGJU_GOSI 본문·첨부 연결.
- [x] 공식 표·다운로드 폼과 요청 범위, 부분 오류 보존 계약 검증.
- [x] 본문597자·HWP1개133,632byte·PDF1개108,815byte 실제 다운로드 및 기본 형식 검증.
- [x] Java213건 통과/실패0/생략0, Node23/23, bootJar, 260영수증/215공고 재현.
- [x] 기존226카탈로그·259영수증·214공고·다른 지역 상태·이전 요청 원장 보존.
- [x] 조사 원본4개 정리·이번 Node/Gradle 자원 종료·운영 쓰기0.
- [ ] 남은88지역: 미등록52 + 등록 후 다운로드 성공 미확인36.
- [ ] 첨부 추출·구간 분석·운영 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E.

## 구현과 공식 관측

등록 목록 `https://www.sangju.go.kr/page/10297/10606.tc`에서 공식 검색 form과 상세 이동 규칙을 확인했다. 검색은 `/gosi/list.tc`의 GET 제목 검색이며, 상세는 `/gosi/detail.tc`의 mn10297/mgtNo 숫자 조합이다. JavaScript를 실행하지 않고 고정 호출값만 읽는다.

고정 표본 `SANGJU-27590`의 제목은 `2026년 상주시 소상공인 희망드림 특례보증 지원계획 공고`이며 URL은 `https://www.sangju.go.kr/gosi/detail.tc?mn=10297&mgtNo=27590`다. 현재 지원 자격·신청 기간을 검증한 결과는 아니다.

`SangjuNoticePage`는 단일 `form#form1 table.comp-tbl_datatype`의 제목·고시공고 내용·첨부파일 행을 구분한다. 메뉴·담당자·부서·파일명은 본문 근거에 포함하지 않는다. 다른 메뉴·중복 query·불명확한 구조는 거부한다.

`SangjuAttachmentDiscoveryProfile`은 공식 `form#form2`의 정확한 세 hidden 필드와 POST 목적지 `https://eminwon.sangju.go.kr/emwp/jsp/ofr/FileDownNew.jsp`를 확인한다. `fnFileDown`의 세 문자열만 기존 선형 `AttachmentDownloadInvocation` 파서로 읽는다. 전달값은 복호화하거나 감사 기록에 원문으로 남기지 않는다. 고정 호스트·HTTPS443·POST·허용 필드·길이·값 형식을 검사하고 redirect는 허용하지 않는다. 파일명 끝의 표시용 쉼표는 제거한다.

같은 파일 중복·충돌, 최대10파일, PDF/HWP/HWPX 외 형식의 다운로드 금지, UNKNOWN 역할, 발견 실패·첨부 없음·부분 실패 구분을 유지한다. 잘못된 링크와 정상 링크가 섞여도 정상 descriptor는 보존한다. 공통 HTTP 전송기·호출값 파서·기존 지역 프로필 지문은 변경하지 않았다.

2026-09-30 04:53 KST(UTC `2026-09-29T19:53:10.082596100Z`) 관측에서 제목 COMBINATION_MATCHED, 본문 AVAILABLE597자 / ACCEPTED / TARGET_SUPPORT_CONFIRMED, 첨부 FOUND·complete=true, 두 파일 DOWNLOADED를 확인했다.

- 프로필: `LOCAL_SANGJU_GOSI_V1`
- 프로필 지문: `71a8665de7dba5f9eec5e0a41ac28c1a154bacb92b7e803e0ce57c313a53ca9d`
- HWP SHA256: `32d9c55ce06676903e98a5bfa63d69cd85dc819c07c9822caece92589de3b4a8`
- PDF SHA256: `94f6a54691cc6ba1af51064b315b349cc64fa9e9cb1b0e7af2df0f0f576277a8`
- 결과: `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`, 원본 정리true, 운영 쓰기0.

신규 카탈로그 expectation은 null이다. 파일 기본 형식 검증은 텍스트 추출·구간 분석 성공이 아니며 본문 ACCEPTED도 관리자 최종 확정이 아니다.

## 집계 / 예산 / 정리

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷의 활성223지역이다. 이번 운영 재조회는 없다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 최소1개 파일 다운로드 확인 | 134 | 135/223 (60.5%) |
| 다운로드 미확인 | 89 | 88 |
| 미등록 | 53 | 52 |
| 등록 후 다운로드 미확인 | 36 | 36 |
| 첨부 오류가 남은 지역 | 43 | 43 |
| 기존3표본·전체 첨부 Gate 충족/잔여 | 16/207 | 16/207 |

지역171+기업마당1=172프로필, 카탈로그227공고/170대상이다. 다운로드 확인율은 전체 구현·운영 완료율이 아니다.

조사3요청(목록·검색·상세 GET 각1)과 실제 관측 본문 포함 예약5회로 누적 예약 상한8회다. 조사 각15초/HTML2MiB, 실제 관측 최대6요청/23MiB·상세1MiB로 제한했다. 관측 예약 byte는2,650,895이며 예약 수를 실제 wire 요청 수로 단정하지 않는다.

조사 HTML·헤더4개937,434byte는 정확한 경로·길이·SHA256 대조 후 삭제했다. 실제 파일 임시 원본은 실행기가 정리했다. 원본은 공식 사이트 재조회가 필요하며, 원문·연락처·동적 파일 전달값은 저장소에 기록하지 않았다.

## 검증 명령 / 결과 / 미검증

```powershell
# 실제 상주 fixture와 보관 대상 스냅샷 환경변수 활성화 후
.\gradlew.bat :test --tests '*SangjuDownloadContractTest' --tests '*AttachmentDownloadInvocationTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=SANGJU -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

표적108건 모두 통과(37초), 확대213건 모두 통과·bootJar·실제 관측 성공(2분). Node23/23, 260영수증/215공고 재현 통과다. 원본 정리 후 fixture 환경변수 없이 실행하면 실제 HTML 검사1건은 조건부 생략된다.

전체 프로젝트 테스트·첨부 추출·AWS·운영 DB·실제 운영 worker/DB/API/UI·운영 E2E는 이번 회차 미실행이다. 브라우저 검증은 현재 요청에 명시되지 않아 정책상 생략했다. 스케줄러의 자동 유입까지 이번 관측으로 입증하지 않았다. migration/API/UI/분류 정책/추출기/운영 설정·배포·정책 게시·ENFORCE·기존 데이터 적용은 변경하지 않았다.
