# 대구 서구 현재·과거 메뉴 연결과 첨부 수집

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대를 우선하며 수집·파싱 오류는 정상 파일과 독립적으로 기록한다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지를 유지한다. HWP 추출기 1.0.16 추가 개선은 보류하며 전체 장기 goal은 미완료다.

- [x] 대구 서구 LGS-000047 / SPRING_BBS 현재·과거 게재 메뉴를 단일 지역 프로필로 연결.
- [x] 기존 포털 새올 엔진 재사용, 공식 본문 분리와 요청 경계 검증.
- [x] 과거 게재 표본 본문68자·HWP1개33,792byte 실제 다운로드 및 기본 형식 검증.
- [x] Java227통과/0실패/0생략, Node23/23, bootJar, 258영수증/213공고 재현.
- [x] 기존224카탈로그·서구 외 지역·이전 요청 원장 보존 확인, 조사 원본 정리.
- [ ] 현재 메뉴 신규 공고 실파일 관측. 과거 표본을 현재 신청 가능한 공고로 취급하지 않는다.
- [ ] 남은90지역: 미등록54 + 등록 후 다운로드 미확인36.
- [ ] 첨부 추출·구간 분석·운영 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E.

## 조사와 구현

[직전 조사](announcement-daegu-donggu-download-2026-09-30.md)의 서구4요청은 게재기간 내 목록에서 소상공인 표본을 찾지 못했다는 기록이다. 이번에는 그 목록에서 확인한 공식 과거 게재 메뉴 `https://www.dgs.go.kr/portal/contents.do?mid=0601020200`를 사용했다. 같은 호스트 `/portal/saeol/gosi/list.do?seCode=01&endYn=Y&mid=0601020200` 이동을 확인하고 공식 form으로 제목 검색을 수행했다. 동적 값·쿠키·인증값을 재사용하거나 저장소에 기록하지 않았다.

표본 `DAEGU_SEOGU-26070`은 `소기업.소상공인 방역물품비 지원금 시행 공고`이며 상세는 `https://www.dgs.go.kr/portal/saeol/gosi/view.do?notAncmtMgtNo=26070&mid=0601020200`다. 과거 공고의 전송·파싱 경로를 확인하는 표본이지, 현재 지원 자격이나 신청 기간이 유효한 공고라는 뜻이 아니다.

`DaeguSeoguAttachmentDiscoveryProfile`은 기존 `GyeongbukPortalAttachmentDiscoveryProfile`을 두 공식 메뉴(mid0601020100/0601020200)에 한정하여 재사용한다. 지역 binding은 하나이며 두 메뉴 설정과 위임 엔진 지문을 하나의 프로필 hash에 포함한다. 공통 엔진과 다른 지역 프로필 지문은 수정하지 않았다. 현재 메뉴는 구현·합성 계약 검증이며 이번 실제 파일 관측은 과거 메뉴다.

첨부는 공식 `form#detailForm div.bod_view > dl.view_file`만 읽는다. `goDownload`의 파일명·시스템 파일명·고정 업로드 디렉터리를 파싱하고 `https://eminwon.dgs.go.kr/emwp/jsp/ofr/FileDown.jsp`로 GET한다. JavaScript·미리보기는 실행하지 않는다. HTTPS443·정확한 query·같은 요청 redirect 경계·최대10파일·UNKNOWN 역할·PDF/HWP/HWPX만 다운로드·부분 실패 보존을 유지한다. 현재→과거 메뉴 redirect도 허용하지 않는다.

`DaeguSeoguNoticePage`는 두 메뉴 외 주소를 거부하고 공식 단일 `div.bod_view > div.view_cont`만 본문으로 선택한다. 제목·담당부서·첨부·메뉴·푸터는 본문에서 제외한다. 미지원 형식, 발견 실패, 확인된 첨부 없음, 파일별 실패는 서로 구분한다.

## 실제 관측 / 집계

2026-09-30 04:22 KST 수집 전용 Java 관측1회에서 제목 COMBINATION_MATCHED, 본문 AVAILABLE68자 / ACCEPTED / TARGET_SUPPORT_CONFIRMED, 첨부 FOUND·complete=true를 확인했다. HWP1개33,792byte가 DOWNLOADED이며 SHA256은 `48c1f2d1f7670e158435fa7e6873cc27f568e4423ab2940e0f901a961be78198`이다.

프로필은 `LOCAL_DAEGU_SEOGU_PORTAL_V1`, 지문은 `be14fb35752e9194dcb2abacc45c9feccad8c7e1af70beef6ec1e7b3c466a7b7`이다. 결과는 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`, 원본 정리true, 운영 쓰기0이다. 신규 QA 카탈로그 expectation은 null이다. 기본 형식 검증은 HWP 텍스트 추출·구간 분석 성공을 의미하지 않고 본문 ACCEPTED도 관리자 최종 확정이 아니다.

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷의 활성223지역이며 이번 운영 재조회는 없다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 최소1개 파일 다운로드 확인 | 132 | 133/223 (59.6%) |
| 다운로드 미확인 | 91 | 90 |
| 미등록 지역 | 55 | 54 |
| 등록 후 다운로드 미확인 | 36 | 36 |
| 첨부 오류가 남은 지역 | 43 | 43 |
| 기존3표본·전체 첨부 Gate 충족/잔여 | 16/207 | 16/207 |

지역169+기업마당1=170프로필, 카탈로그225공고/168대상이다. 기존257영수증·212공고·다른 지역 상태·모든 이전 요청 원장은 보존한다. 다운로드 확인율은 운영 전체 완료율이 아니다.

## 요청 예산과 정리

이전 서구4요청 + 이번 과거 메뉴·목록·검색·상세4요청 + 실제 관측의 본문 포함 예약4회 = 누적 예약 상한12회다. 기존 이력을 초기화하지 않았으며 계획 상한14회 이내다. 조사 각15초/HTML2MiB, 실제 관측 최대6요청/23MiB·상세1MiB로 제한했다. 관측 예약 byte는2,630,656이며 예약값을 실제 wire 요청 수로 단정하지 않는다.

조사 HTML·응답 헤더 원본5개1,486,118byte는 SHA256·길이·정확한 경로를 대조한 뒤 삭제했다. 실제 파일 임시 원본은 실행기가 정리했다. 원문은 공식 사이트에서 다시 조회해야 확보할 수 있으며 원장에 원문·연락처·헤더 값은 저장하지 않았다.

## 검증 명령 / 결과 / 미검증

```powershell
# 실제 서구 fixture 및 보관 운영 대상 스냅샷 환경변수 활성화 후
.\gradlew.bat :test --tests '*DaeguSeoguDownloadContractTest' --tests '*GyeongbukFirstDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=DAEGU_SEOGU -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

표적 실행 성공(35초), 확대227건 모두 통과·bootJar·실제 관측 성공(2분7초), Node23/23, 258영수증/213공고 재현 통과다. 실제 과거 메뉴 HTML fixture는 실행했으며 원본 정리 후 해당 환경변수 없이 실행하면 조건부 fixture1건은 생략된다.

전체 프로젝트 테스트·AWS·운영 DB·실제 운영 worker/DB/API/UI·첨부 텍스트 추출·운영 E2E는 이번 회차 미실행이다. 브라우저 검증은 현재 요청에 명시되지 않아 정책상 생략했다. 현재 메뉴의 신규 파일·스케줄러 자동 유입은 이번 검증으로 입증하지 않았다. migration/API/UI/분류 정책/추출기/운영 설정·배포·정책 게시·ENFORCE·기존 데이터 적용은 변경하지 않았다.
