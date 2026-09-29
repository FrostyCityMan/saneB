# 오산시 본문·첨부 연결 및 파일 다운로드 시간 초과 분리

## 현재 단계 / Gate

전 지역에서 수집 가능한 첨부를 먼저 확보하고 발견·다운로드·추출 실패를 각각 기록한다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지를 유지한다. HWP 추출기1.0.16 추가 개선은 이번 범위가 아니다.

- [x] 오산시 LGS-000104 / SAEOL_GOSI를 기존 포털 새올 GET 엔진에 연결.
- [x] 공식 목록·검색·상세200, 실제 본문125자·HWP 첨부2개 발견.
- [!] 두 파일 모두 FILE_DOWNLOAD / TRANSPORT_TIMEOUT. 수신·다운로드 성공으로 집계하지 않음.
- [x] 표적116건·확대236건 모두 통과, bootJar·Node23/23·268영수증/222공고 재현.
- [x] 원본6개1,220,050byte와 실행 자원 정리, 기존 다른 지역 근거 보존.
- [ ] 잔여83개 수집원: 미등록45 + 등록 후 다운로드 성공 미확인38.
- [ ] 첨부 텍스트 추출·구간 분석·상시 worker·운영 DB/API/UI·DRAFT·기존 데이터·운영 E2E 전체 Gate.

## 공식 조사와 구현

등록 목록 `https://www.osan.go.kr/portal/saeol/gosi/list.do?mId=0302010000`에서 화면의 검색 폼과 `searchType=tit`, `searchTxt=소상공인`, `page=1` POST를 확인했다. 검색 결과의 `data-action` 상세 링크와 현재 목록 파서 계약을 대조했다. 인증·세션·토큰 주입 없이 목록·검색·상세 각1회 모두200을 확인했다.

- 표본: `OSAN-50603`.
- 제목: 2026년 오산시 소상공인 특례보증 지원계획 공고.
- 상세: `https://www.osan.go.kr/portal/saeol/gosi/view.do?notAncmtMgtNo=50603&mId=0302010000`.
- `form#detailForm[name=detailForm][method=post] div.bod_view` 내부 `h4`가 제목, `div.view_cont`가 본문이다. 담당부서·등록일·첨부명·메뉴는 본문에 섞지 않는다.
- `dl.view_file`의 ‘첨부 파일’ label 다음 `dd`에서2개 `goDownload` 링크를 발견했다. 공식 함수는 `https://eminwon.osan.go.kr/emwp/jsp/ofr/FileDown.jsp`로3개 파일 인자를 GET 전송한다. 원문 JavaScript와 미리보기는 실행하지 않는다.

`OsanAttachmentProfileConfiguration`은 `GyeongbukPortalAttachmentDiscoveryProfile`을 새로 만들지 않고 재사용한다. 기관/source/parser/host/menu를 코드에서 고정하고 기존의 부분 성공 보존, 최대10파일, 중복·상충·미지원 형식, UNKNOWN 문서 역할, HTTPS·동일 요청 경계와3인자 검증을 유지한다. 파일명만으로 공고문/신청서 역할을 확정하지 않는다.

`OsanNoticePage`와 본문 client 분기는 위 공식 상세·정확한 query2개·공식 본문 영역만 허용한다. 카탈로그·Spring 등록·QA 실행기에 연결했으며 expectation은 null이다. 기존233개 카탈로그 항목, 공통 엔진과 파일 검증기, DB/migration, API/UI, 운영 source·정책·worker 설정을 변경하지 않았다.

## 실제 관측과 오류

2026-09-30 06:38 KST(UTC `2026-09-29T21:38:34.112410600Z`):

- 프로필 `LOCAL_OSAN_PORTAL_V1`, 지문 `c8c9f4bcceba369ed102fe0035d2a6f5df47a98d54a811bb8e3282f00291c13a`.
- 제목 COMBINATION_MATCHED, 본문 AVAILABLE125자 / ACCEPTED / TARGET_SUPPORT_CONFIRMED.
- 첨부 FOUND·complete=true·2개.
- 두 파일 모두 FAILED / FILE_DOWNLOAD / TRANSPORT_TIMEOUT.
- 결과 COLLECTION_ONLY_PARTIAL_NOT_APPROVED, 원본 정리true, 운영 쓰기0.

파일 서버의 연결 시간 초과를 본문 실패나 ‘첨부 없음’으로 바꾸지 않는다. 실제 파일 수신량·해시·형식 검증 성공 근거가 없으므로 다운로드 성공으로 집계하지 않는다. 이번 결과만으로 서버 영구 장애·파일 소실을 단정하지 않으며, 동일 요청을 즉시 반복하지 않고 다른 수집원 연결을 진행한다.

## 집계·예산·검증

분모는2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷의 활성223지역 수집원이다. 이번 운영 재조회는 없다. 최소1파일 다운로드 확인140/223(62.8%)·잔여83은 유지한다. 미등록46→45, 등록 후 미확인37→38이다. 오류가 남은 수집원44→45는 성공 수집원과 중복될 수 있다. 기존3표본·전체 파일 Gate는16충족/207잔여다. 수집원 집계를 모든 하위 경로·고유 행정구역·전체 운영 완료율과 동일시하지 않는다.

지역178+기업마당1=179프로필, 카탈로그234공고/177대상(233reference-only), 영수증268개/최신222공고다.

```powershell
# 실제 HTML fixture와 기존 읽기 전용 inventory 환경변수 활성화 후
.\gradlew.bat :test --tests '*OsanDownloadContractTest' --tests '*GyeongbukFirstDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=OSAN -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

표적116건 통과(36초), 확대236건 통과·실패0·생략0, bootJar·관측 태스크 완료(2분28초)다. 관측 태스크 성공을 파일 다운로드 성공으로 표현하지 않는다. Node23건과 기존 영수증·다른 지역 근거 보존 검증이 통과했다. 원본 정리 후 fixture 환경변수 없이 실행하면 실제 HTML 검사1건은 조건부 생략된다.

조사3요청은 각15초/2MiB로 제한했다. 실제 관측1회는 최대6요청/23MiB·상세1MiB이며 본문 포함 예약5회/2,506,752byte, 조사 포함 누적 예약 상한8회다. 예약 수·예약 byte를 실제 wire 요청 수·성공 수신량으로 단정하지 않는다. 재시도 관측은 실행하지 않았다.

조사 HTML·헤더6개1,220,050byte를 정확한 절대 경로·크기·SHA256 대조 후 삭제했다. 복구에는 공식 사이트 재조회가 필요하며 근거 보고서·해시는 보존한다. 임시 편집 도구 및 이번 Node·Gradle·Java 실행 자원을 정리하고 기존 다른 작업 프로세스와 미추적 파일은 보존했다.

전체 프로젝트 테스트·첨부 텍스트 추출·AWS·운영 DB·상시 유입·운영 E2E는 미실행이다. 브라우저 검증은 현재 요청에 명시되지 않아 정책상 생략했다. 작업 브랜치 `[skip deploy]` 범위이며 운영 배포·정책 게시·ENFORCE·기존 데이터 처리는 하지 않았다.
