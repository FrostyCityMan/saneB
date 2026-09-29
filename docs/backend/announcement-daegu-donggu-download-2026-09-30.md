# 대구 동구 포털 새올 재사용과 본문·첨부 수집

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대를 우선한다. 수집·파싱 오류는 별도로 기록하고 정상 파일은 계속 수집한다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지를 유지한다. HWP 추출기 1.0.16 추가 개선은 보류하며 전체 장기 goal은 미완료다.

- [x] 대구 동구 LGS-000046 / SAEOL_GOSI 본문·첨부 연결.
- [x] 기존 포털 새올 엔진 재사용. 신규 크롤링 엔진·공통 검증기 변경 없음.
- [x] 실제 본문299자·HWPX1개57,861byte 다운로드 및 기본 형식 검증.
- [x] Java225통과/0실패/0생략, Node23/23, bootJar.
- [x] 257영수증/212공고 재현, 기존223카탈로그·다른 지역 상태·과거 요청 원장 보존 확인.
- [x] 조사 원본10개2,184,605byte 정리, 운영 쓰기0.
- [ ] 남은91지역: 미등록55 + 등록 후 다운로드 미확인36.
- [ ] 첨부 텍스트 추출·구간 분석·운영 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E.

## 조사와 연결

충남도 LGS-000147의 등록 목록 `https://www.chungnam.go.kr/cnportal/province/province/list.do?menuNo=500487`은 HTTP404다. 1회 결과를 기록하고 반복 요청하지 않았다. 최신 공식 경로 확인은 후속이며 운영 source URL은 변경하지 않았다.

대구 동구 등록 메뉴 `https://www.dong.daegu.kr/portal/contents.do?mid=0201020000`는 같은 호스트의 `/portal/saeol/gosi/list.do`로 이동한다. 메뉴 응답과 HEAD로 목적지를 확인했고, 세션 없이 고정 `seCode=01&mid=0201020000` 목록에 접근했다. 공식 form의 `searchType=tit`, `searchTxt=소상공인` 검색으로 공고60818을 확인했다. 동적 값·쿠키·인증값은 요청에 재사용하거나 저장소에 기록하지 않았다.

고정 표본:

- 코드 `DAEGU_DONGGU-60818`.
- 제목 `2026년 하반기 대구광역시 동구 소상공인 경영안정자금 지원사업 공고`.
- 상세 `https://www.dong.daegu.kr/portal/saeol/gosi/view.do?notAncmtMgtNo=60818&mid=0201020000`.

공식 목록의 `data-action` 상세 링크를 사용하며 상세 GET200을 확인했다. `DaeguPortalAttachmentProfileConfiguration`이 기존 `GyeongbukPortalAttachmentDiscoveryProfile`에 host·file host·mid·source/parser를 고정하여 연결한다. 기존 공통 엔진 코드를 수정하지 않았으며 다른 지역의 프로필 지문도 유지한다.

공식 `form#detailForm div.bod_view > dl.view_file`의 첨부 영역만 사용한다. `goDownload`의 세 인자를 파싱하여 `https://eminwon.dong.daegu.kr/emwp/jsp/ofr/FileDown.jsp` GET으로 전달하고 JavaScript는 실행하지 않는다. 같은 요청만 허용하는 redirect 경계, HTTPS443, 고정 디렉터리, 안전한 파일명, PDF/HWP/HWPX 형식, 최대10파일, UNKNOWN 역할을 유지한다. 하나의 미해결 링크가 정상 파일을 지우지 않으며 미지원 형식·첨부 없음·발견 실패도 분리한다.

`DaeguDongguNoticePage`는 정확한 host/path·mid·공고 번호와 단일 `form#detailForm[name=detailForm][method=post] div.bod_view`를 검증하고 `div.view_cont`만 본문으로 사용한다. 제목·담당부서·첨부·메뉴·푸터는 섞지 않는다.

대구 서구 LGS-000047도 공식 메뉴 이동 후 목록 접근은 성공했지만, 게재기간 내 목록에서 소상공인 제목 검색 결과가 없었다. 이는 이번 검색의 표본 미확보이지 해당 지역의 첨부가 없다는 뜻이 아니다. 과거 게재 목록·다른 적합 표본 탐색은 후속으로 남기며 상세·파일 요청 및 프로필 등록은 하지 않았다.

## 실제 관측

2026-09-30 04:11 KST 수집 전용 Java 경로 1회:

- 제목 COMBINATION_MATCHED.
- 본문 AVAILABLE299자 / ACCEPTED / TARGET_SUPPORT_CONFIRMED. 관리자 확정이 아닌 후보 근거다.
- 첨부 FOUND·complete=true, HWPX1개57,861byte / DOWNLOADED.
- 바이너리 SHA256 `7bc13f8c5832a598a4c8fe207869d15f4a6e1bad3a4590508aa9374d359b7f54`.
- 프로필 `LOCAL_DAEGU_DONGGU_PORTAL_V1`, 지문 `73c777cceeb67c4b4792d85f252090a4c44870343bfc6402bdc5cdb0e99372a5`.
- `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`, 원본 정리true, 운영 쓰기0.

다운로드·기본 형식 검증 결과이며 HWPX 텍스트 추출·구간 분석·운영 상시 수집 성공을 의미하지 않는다. 신규 카탈로그 expectation은 null이다. 목록 조사와 고정 상세 관측을 운영 스케줄러의 신규 목록 자동 유입 E2E로 보고하지 않는다.

## 집계와 요청 원장

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷의 활성223지역이다. 이번 운영 재조회는 없다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 최소1개 파일 다운로드 확인 | 131 | 132/223 (59.2%) |
| 다운로드 미확인 | 92 | 91 |
| 미등록 지역 | 56 | 55 |
| 등록 후 다운로드 미확인 | 36 | 36 |
| 첨부 오류가 남은 지역 | 43 | 43 |
| 기존3표본·전체 첨부 Gate 충족/잔여 | 16/207 | 16/207 |

지역168+기업마당1=169프로필, 카탈로그224공고/167대상이다. 기존256영수증·211공고·동구 외 지역 상태·이전 요청 원장은 보존한다. 충남 목록404·서구 표본 미확보는 조사 원장에 기록하며 위 첨부 오류 지역43에 별도로 더하거나 다운로드 성공으로 집계하지 않는다.

조사 요청은 충남1회, 동구5회, 서구4회 총10회다. HTML8회는 각15초/2MiB, 목적지 확인용 HEAD2회는 각15초로 제한했다. 실제 관측1회는 최대6요청/23MiB·상세1MiB이며 본문 포함 예약4회/2,556,421byte다. 조사 포함 예약 상한14회로 계획 상한16회 이내다. 예약 상한을 실제 wire 요청 수로 단정하지 않는다.

조사 HTML·HEAD 원본10개2,184,605byte를 해시·길이·정확한 경로와 대조한 후 삭제했다. 실제 파일 원본은 관측 실행기가 정리했다. 원문은 공식 사이트에서 다시 조회해야 확보할 수 있으며 원장에는 원문·연락처·헤더 값이 없다.

## 검증 명령 / 결과 / 한계

```powershell
# 실제 동구 fixture 및 보관 운영 대상 스냅샷 환경변수 활성화 후
.\gradlew.bat :test --tests '*DaeguDongguDownloadContractTest' --tests '*GyeongbukFirstDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=DAEGU_DONGGU -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

최초 표적105건 중1건은 빈 POST가 요청 생성 단계에서 예외를 던지는 계약을 테스트가 잘못 가정해 실패했다. 생성 단계의 거부와 비어 있지 않은 POST의 프로필 거부를 각각 검사하도록 테스트를 수정했으며 수집 보안 조건은 완화하지 않았다. 수정 후105건 통과(23초). 확대225건 모두 통과·bootJar·실제 관측 성공(2분1초), Node23/23, 257영수증/212공고 재현 통과다. 실제 동구 HTML fixture를 사용했고 원본 정리 후 해당 환경변수 없이 실행하면 조건부 fixture1건은 생략된다.

전체 프로젝트 테스트·AWS·운영 DB·실제 운영 worker/DB/API/UI·추출·운영 E2E는 이번 회차 미실행이다. 브라우저 검증은 현재 요청에 명시되지 않아 정책상 생략했다. DB migration/API/UI/분류 정책/추출기/운영 설정·배포·정책 게시·ENFORCE·기존 데이터 적용은 변경하지 않았다.
