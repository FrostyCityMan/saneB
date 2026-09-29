# 금천구 본문·첨부 수집 연결

## 현재 단계 / Gate

전 지역에서 수집 가능한 첨부를 먼저 확보하고 발견·다운로드·추출 실패를 독립적으로 기록한다. 제목 → 본문 → 첨부 → 관리자 최종 검증과 제목 제외 원문 비저장·자동 활성화 금지를 유지한다. HWP 추출기 1.0.16 추가 개선은 이번 범위가 아니다.

- [x] 금천구 LGS-000019 / SAEOL_GOSI의 본문·공식 첨부 영역 연결.
- [x] 실제 과거 공고 본문59자·HWP3개299,008byte 다운로드와 기본 형식 검증.
- [x] 표적112건 통과, 확대244건 통과·조건부1건 생략, bootJar 성공.
- [x] Node23/23, 263영수증/218공고 재현, 기존 근거 보존.
- [x] 조사 HTML3개 정리·이번 실행 프로세스 종료·운영 쓰기0.
- [ ] 남은85지역: 미등록49 + 등록 후 다운로드 성공 미확인36.
- [ ] 첨부 텍스트 추출·구간 분석·상시 worker·운영 DB/API/UI·DRAFT·기존 데이터·운영 E2E의 전체 Gate.

## 공식 경로와 구현

등록 목록 `https://www.geumcheon.go.kr/portal/tblSeolGosiDetailList.do?key=294&rep=1`에서 GET 검색 폼의 실제 필드 `searchCnd=notAncmtSj`, `searchKrwd`를 확인했다. 제목 소상공인 검색으로 과거 지원금 공고를 선정했다. 목록·검색·상세는 각각1회, 모두 HTTP200이었다.

표본 `GEUMCHEON-27579`:

- 제목: 집합금지 및 영업제한 업종 소상공인 폐업지원금 변경 공고
- 상세: `https://www.geumcheon.go.kr/portal/tblSeolGosiDetailView.do?key=294&notAncmtMgtNo=27579`
- 2022년 과거 공고이며 현재 신청 가능·자격 충족을 의미하지 않는다.

`GeumcheonNoticePage`는 HTTPS 고정 호스트·상세 경로·메뉴294·숫자 공고 번호와 정확한2개 query만 허용한다. `#contents.cts294 > .program > .veterinary_contract.view > .p-wrap.bbs.bbs__view > table.p-table.block`의 단일 구조를 검증한다. 제목은 `td[data-brl-flag=1]`, 본문은 `td[data-brl-flag=7]`로 분리해 담당부서·연락처·이메일·첨부·메뉴가 본문 분류에 섞이지 않게 한다.

`GeumcheonAttachmentDiscoveryProfile`은 첨부파일 label의 같은 행 셀 아래 `ul.p-attach > li.p-attch__item`만 읽는다. 사이트의 실제 철자인 `p-attch__item`을 사용한다. `seol_file_download`의 세 문자열은 기존 선형 호출 파서에 전달하며 JavaScript를 실행하지 않는다. 검증한 값만 HTTPS `eminwon.geumcheon.go.kr/emwp/jsp/ofr/FileDown.jsp`의 GET 요청으로 만든다. 기존 새올 파일 경로·이름 검증을 재사용하고 redirect·POST·임의 호스트는 허용하지 않는다.

같은 파일의 숨김 경로, 제목과 파일 인수가 일치하는 `SViewerSeol`, 동일 다운로드 URL의 `call_viewer_tts`는 보조 요소로만 구분한다. 미리보기·음성듣기 요청은 하지 않는다. 값 불일치·추가 스크립트·미해결 링크는 발견 오류로 남기되 독립적으로 확인한 정상 파일 descriptor를 보존한다. 최대10파일·미지원 형식 구분·UNKNOWN 문서 역할을 유지한다.

프로필을 worker의 기존 등록 체계, 본문 client, QA 카탈로그와 관측 실행기에 연결했다. 카탈로그 expectation은 null이며 정책 승인으로 간주하지 않는다. 기존 공통 엔진과 기존 프로필 지문은 변경하지 않았다. migration·DB·API·화면 계약 변경은 없다.

## 실제 관측

2026-09-30 05:32 KST(UTC `2026-09-29T20:32:37.920887400Z`)에 제목 COMBINATION_MATCHED, 본문 AVAILABLE59자 / ACCEPTED / TARGET_SUPPORT_CONFIRMED, 첨부 FOUND·complete=true를 확인했다. 본문 ACCEPTED는 관리자 최종 확정이 아니다.

- 프로필: `LOCAL_GEUMCHEON_BOARD_V1`
- 지문: `ed094c9033072892502ce49d4aa9524817807ef45228984a02a7c1d0f93c22ab`
- 결과: `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`, 원본 정리true, 운영 쓰기0.

| 형식 | byte | SHA256 |
|---|---:|---|
| HWP | 147,968 | `43967fb76451a1ca70a8814e30bea45ab61c909d8c411b3ef6b1a11aee48d11b` |
| HWP | 76,288 | `6aee2c1168f324b8a2e78c957347cbebeeb3162abefde2f707a689df6d7e5fe9` |
| HWP | 74,752 | `390fcce6cd5ed0ba934032e45590c3151cf689d8dbe4ffdd243e63b4be45d2e7` |

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷의 활성223지역이다. 이번 운영 재조회는 없다. 최소1파일 다운로드 관측은137→138지역(61.9%), 잔여86→85(미등록50→49, 등록 후 미확인36 유지)다. 첨부 오류가 남은43지역은 성공 지역과 중복될 수 있다. 기존3표본·전체 파일 Gate는16충족/207잔여로 별도 유지한다.

지역174+기업마당1=175프로필, 카탈로그230공고/173대상, 영수증263개/최신218공고다. 다운로드 확인율은 전체 구현·운영 완료율이 아니다.

## 검증·예산·정리

```powershell
# 실제 금천 HTML fixture, 기존 운영 대상 스냅샷 환경변수를 활성화하고 실행
.\gradlew.bat :test --tests '*GeumcheonDownloadContractTest' --tests '*HongcheonDownloadContractTest' --tests '*SaeolGetAttachmentDiscoveryProfileTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=GEUMCHEON -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

표적112건 통과(36초). 확대245건 중244통과·실패0·조건부1생략, bootJar와 실제 관측 성공(2분3초). 생략1건은 원본이 이미 정리된 홍천 실제 HTML 검사이며 금천 실제 HTML 검사는 통과했다. 원본 정리 후 금천 fixture 환경변수 없이 재실행하면 금천 실제 HTML 검사도 조건부 생략된다. 합성 테스트에는 정상3형식·미지원 형식·중복/상충·10파일 상한·소스 지문·호스트·query·redirect 차단·보조 링크 불일치·부분 성공 보존을 포함한다.

조사3요청(각15초/HTML2MiB), 관측1회(최대6요청/23MiB·상세1MiB). 본문 포함 관측 예약6회·예약byte2,723,840, 조사 포함 누적 예약 상한9회다. 예약 수를 실제 wire 요청 수로 단정하지 않는다. 예산을 초기화한 반복 요청은 없다.

조사 HTML3개963,273byte는 절대 경로·크기·SHA256 확인 후 삭제했다. 실제 파일 임시 원본은 관측 실행기가 정리했다. 근거 보고서와 해시는 보존하며 원본 복구에는 공식 사이트 재조회가 필요하다. 임시 편집 도구도 삭제했다. 이번 Node·Gradle·Java 실행 자원이 남지 않았음을 확인했고 기존 다른 작업 프로세스는 건드리지 않았다.

전체 프로젝트 테스트·첨부 추출·AWS·운영 DB·운영 상시 유입·운영 E2E는 이번 회차 미실행이다. 현재 요청에 브라우저 지시가 없어 정책상 브라우저 검증을 생략했다. 운영 배포·정책 게시·ENFORCE·기존 데이터 적용은 변경하지 않았다.
