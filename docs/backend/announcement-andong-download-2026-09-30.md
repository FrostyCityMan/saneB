# 안동 표형 본문·첨부와 미리보기 구분

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대를 우선한다. 정상 파일은 먼저 수집하고 파싱·다운로드·추출 오류는 따로 기록한다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지를 유지한다. HWP 추출기 1.0.16 추가 개선과 전체 goal 완료는 별도다.

- [x] 안동 LGS-000204 / SPRING_BBS 본문·첨부 연결.
- [x] 공식 표 경계·같은 파일의 미리보기 구분·정상 파일 보존 계약 검증.
- [x] 본문451자·HWPX2개/PDF1개 총1,123,922byte 다운로드 및 기본 형식 검증.
- [x] 확대 Java241건 통과/실패0/생략0, Node23/23, bootJar, 261영수증/216공고 재현.
- [x] 기존227카탈로그·260영수증·215공고·다른 지역 상태와 이전 요청 원장 보존.
- [x] 조사 원본5개 정리·이번 Node/Gradle 프로세스 종료·운영 쓰기0.
- [ ] 남은87지역: 미등록51 + 등록 후 다운로드 성공 미확인36.
- [ ] 첨부 추출·구간 분석·운영 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E.

## 공식 조사와 구현

등록 목록 `https://www.andong.go.kr/portal/saeol/gosi/list.do?mId=0401020100`에서 공식 POST 검색과 상세 링크를 확인했다. 첫 검색의 searchType을 잘못 지정해 제목 필터 결과를 얻지 못했고, 실제 option 값 `tit`로 수정한 검색에서 고정 표본을 확보했다. 첫 요청도 조사4회에 포함하며 실패 이력을 삭제하지 않았다.

표본 `ANDONG-63386`의 제목은 `2026년 안동시 소상공인 간판설치비용 지원사업 신청자 모집 공고`다. 상세 URL은 `https://www.andong.go.kr/portal/saeol/gosi/view.do?notAncmtMgtNo=63386&isLinkage=Y&mId=0401020100`다. 현재 신청 기간·지원 자격을 검증한 결과는 아니다.

`AndongNoticePage`는 메뉴0401020100·isLinkageY·숫자 공고 식별자를 검증한다. 상세 폼 다음의 숨김 제목과 바로 뒤 `table.bod_view`만 선택한다. 최초 실제 HTML 테스트에서 표를 폼 내부로 가정한 선택 오류1건을 확인해 실제 인접 구조로 수정했다. 제목 `th.title`, 본문 `td.cont > div.cont_box`, 첨부 `th.list_file`의 인접 `td.box_file > ul.list_file`를 분리한다. 메뉴·부서·연락처·파일명은 본문에 섞지 않는다.

`AndongAttachmentDiscoveryProfile`은 공식 영역의 `goDownload` 세 문자열만 기존 선형 호출 파서로 읽고, 기존 새올 파일 URL 검증기로 파일명과 고정 업로드 경로를 검사한다. 실제 다운로드 목적지는 HTTPS `eminwon.andong.go.kr/emwp/jsp/ofr/FileDown.jsp`로 제한하며 redirect·임의 호스트·다른 요청 이동을 허용하지 않는다.

미리보기는 바로 앞의 정상 다운로드 링크와 공고 번호·파일 인수3개가 일치하고 정해진 미리보기 이미지 구조인 경우에만 별도 UI 링크로 식별한다. 미리보기 자체는 호출하지 않는다. 일치하지 않는 미리보기는 발견 오류로 남기고 정상 파일은 유지한다. 최대10파일·UNKNOWN 역할·PDF/HWP/HWPX만 다운로드·미지원 형식·첨부 없음·부분 실패 구분을 유지한다. 공통 파서와 파일 검증기 및 기존 지역 프로필 지문은 변경하지 않았다.

## 실제 결과와 집계

2026-09-30 05:06 KST(UTC `2026-09-29T20:06:00.784780300Z`) 관측에서 제목 COMBINATION_MATCHED, 본문 AVAILABLE451자 / ACCEPTED / TARGET_SUPPORT_CONFIRMED, 첨부 FOUND·complete=true를 확인했다. 세 파일 모두 DOWNLOADED다.

| 형식 | byte | SHA256 |
|---|---:|---|
| HWPX | 514,981 | `46231ca1cd737f889d800bd55453a3291c0a2ed9a7e2e0633207423e13879fe0` |
| HWPX | 67,216 | `51802627791b5ecae9912b11166771844978fb6b0da6118e09bc51b1b44cfc63` |
| PDF | 541,725 | `c84f25b5aac52bdc9efc377dc9b7f02bab93520d4ad14ca5ac3e493f4569253b` |

프로필은 `LOCAL_ANDONG_TABLE_V1`, 지문은 `96c13247c83fe0218e20bb571dc54680f817e0dff7d10b2dc1bafbd206abd7b4`다. 결과는 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`, 원본 정리true, 운영 쓰기0이며 신규 카탈로그 expectation은 null이다. 기본 형식 검증은 텍스트 추출 성공이 아니고 본문 ACCEPTED도 관리자 최종 확정이 아니다.

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷의 활성223지역이다. 이번 운영 재조회는 없다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 최소1개 파일 다운로드 확인 | 135 | 136/223 (61.0%) |
| 다운로드 미확인 | 88 | 87 |
| 미등록 | 52 | 51 |
| 등록 후 다운로드 미확인 | 36 | 36 |
| 첨부 오류가 남은 지역 | 43 | 43 |
| 기존3표본·전체 첨부 Gate 충족/잔여 | 16/207 | 16/207 |

지역172+기업마당1=173프로필, 카탈로그228공고/171대상이다. 다운로드 확인율은 전체 구현·운영 완료율이 아니다.

## 예산·정리·검증

조사4요청(목록1·검색2·상세1)과 실제 관측 본문 포함 예약6회로 누적 예약 상한10회다. 조사 각15초/HTML2MiB, 실제 관측 최대6요청/23MiB·상세1MiB로 제한했다. 관측 예약 byte는4,073,042이며 예약 수를 실제 wire 요청 수로 단정하지 않는다.

조사 HTML·헤더5개3,393,102byte는 정확한 경로·길이·SHA256을 대조하고 삭제했다. 실제 파일 임시 원본은 실행기가 정리했다. 원본은 공식 사이트 재조회가 필요하며 원문·연락처·헤더 값은 저장소에 기록하지 않았다.

```powershell
# 실제 안동 fixture와 보관 대상 스냅샷 환경변수 활성화 후
.\gradlew.bat :test --tests '*AndongDownloadContractTest' --tests '*AttachmentDownloadInvocationTest' --tests '*SaeolGetAttachmentDiscoveryProfileTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=ANDONG -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

최초 표적110건 중 실제 HTML 선택 오류1건 실패, 수정 후110건 통과(28초). 확대241건 모두 통과·bootJar·실제 관측 성공(1분58초). Node23/23, 261영수증/216공고 재현 통과다. 원본 정리 후 fixture 환경변수 없이 실행하면 실제 HTML 검사1건은 조건부 생략된다.

전체 프로젝트 테스트·첨부 추출·AWS·운영 DB·실제 운영 worker/DB/API/UI·운영 E2E는 이번 회차 미실행이다. 브라우저 검증은 현재 요청에 명시되지 않아 정책상 생략했다. 스케줄러 자동 유입은 이번 관측으로 입증하지 않았다. migration/API/UI/분류 정책/추출기/운영 설정·배포·정책 게시·ENFORCE·기존 데이터 적용은 변경하지 않았다.
