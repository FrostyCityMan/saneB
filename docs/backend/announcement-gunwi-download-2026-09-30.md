# 군위 본문·첨부 연결과 실파일 다운로드

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대가 우선이다. 한 링크·파일의 오류 때문에 정상 파일을 버리지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지를 유지한다. HWP 추출기 1.0.16 추가 개선과 전체 장기 goal 완료는 별도다.

- [x] 군위 LGS-000053 / GUNWI_NOTICE_TABLE 본문·첨부 프로필 연결.
- [x] 공식 첨부 영역, 숫자 파일 링크, 확인한 공식 파일 서버 이동만 허용.
- [x] 부분 발견 오류와 정상 파일 보존, 미지원 형식·첨부 없음·발견 실패 구분.
- [x] 실제 본문200자·HWP1개208,896byte 다운로드 및 기본 형식 검증.
- [x] 확대 Java253건 통과/실패0/생략0, Node23/23, bootJar, 259영수증/214공고 재현.
- [x] 기존225카탈로그·258영수증·213공고·다른 지역 상태와 이전 요청 원장 보존.
- [x] 조사 원본5개 정리, 운영 쓰기0.
- [ ] 남은89지역: 미등록53 + 등록 후 다운로드 성공 미확인36.
- [ ] HWP 텍스트 추출·구간 분석·운영 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E.

## 구현과 실제 근거

공식 등록 목록 `https://www.gunwi.go.kr/ko/page.do?mnu_uid=666&boardType=notice`와 제목 검색에서 표본을 확인했다. 목록 제목은 줄임표로 표시되어 실제 상세 제목을 검증했다.

표본은 `GUNWI-25554`, 제목은 `2026년도 군위군 소상공인 융자금 이차보전금 지원사업 운영계획 공고`다. 상세는 `https://www.gunwi.go.kr/ko/page.do?mnu_uid=666&not_ancmt_mgt_no=25554&cmd=2`다. 현재 신청 가능 여부나 지원 자격을 검증한 결과는 아니다.

`GunwiNoticePage`는 고정 메뉴666·cmd2와 단일 `div.boardView`를 검증한다. 본문은 `div.cont > div.board_content`, 제목은 `div.title > h4`, 첨부는 `div.title > div > ul`로 분리한다. 메뉴·담당부서·첨부 파일명은 본문 근거에 포함하지 않는다. 최초 실제 HTML fixture 검사에서 본문의 중간 `div.cont`를 누락한 오류1건을 발견해 수정했다. 합성 검사만으로 구조 일치를 단정하지 않았다.

`GunwiAttachmentDiscoveryProfile`은 같은 공고 번호의 `/programs/board/saeol/notice/download.do` 숫자 링크만 처리한다. 조사 HEAD에서 확인한 HTTPS `eminwon.gunwi.go.kr/emwp/jsp/ofr/FileDown.jsp` 이동을 허용하며 기존 `SaeolGetAttachmentDiscoveryProfile`의 파일명·업로드 디렉터리 검증을 재사용한다. 일반 링크·JavaScript 실행·임의 호스트·다른 공고 링크는 허용하지 않는다. 최대10파일·UNKNOWN 역할·PDF/HWP/HWPX만 다운로드하며 발견 오류가 있어도 검증된 descriptor는 유지한다. 공통 엔진 및 다른 지역 프로필 지문은 변경하지 않았다.

2026-09-30 04:40 KST 관측(UTC `2026-09-29T19:40:44.548523200Z`)에서 제목 COMBINATION_MATCHED, 본문 AVAILABLE200자 / ACCEPTED / TARGET_SUPPORT_CONFIRMED, 첨부 FOUND·complete=true를 확인했다. HWP1개208,896byte가 DOWNLOADED다.

- 프로필: `LOCAL_GUNWI_BOARD_V1`
- 프로필 지문: `5793704940063960a864465a58565cb03c0b8adf1b9b7878ea28603f621a3e89`
- 파일 SHA256: `bf6ea14b8bc371b5ae5a4feb6064b6aeabd8d95fb12d50e1d42575dd6dfe09dd`
- 결과: `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`, 원본 정리true, 운영 쓰기0.

신규 카탈로그 expectation은 null이다. 기본 형식 검증은 HWP 텍스트 추출 성공이 아니며 본문 ACCEPTED도 관리자 최종 확정이 아니다.

## 집계와 요청 예산

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷의 활성223지역이다. 이번 운영 재조회는 없다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 최소1개 파일 다운로드 확인 | 133 | 134/223 (60.1%) |
| 다운로드 미확인 | 90 | 89 |
| 미등록 | 54 | 53 |
| 등록 후 다운로드 미확인 | 36 | 36 |
| 첨부 오류가 남은 지역 | 43 | 43 |
| 기존3표본·전체 첨부 Gate 충족/잔여 | 16/207 | 16/207 |

지역170+기업마당1=171프로필, 카탈로그226공고/169대상이다. 다운로드 확인율은 전체 구현·운영 완료율이 아니다.

이번 조사4요청(목록·검색·상세 GET 각1, 첨부 링크 HEAD1)과 실제 관측의 본문 포함 예약5회로 누적 예약 상한9회다. 조사 GET 각15초/HTML2MiB, 실제 관측 최대6요청/23MiB·상세1MiB로 제한했다. 관측 예약 byte는2,519,040이며 예약 수를 실제 wire 요청 수로 단정하지 않는다. 과거 다른 지역 요청 원장은 그대로 보존했다.

조사 HTML·헤더 원본5개638,088byte는 정확한 경로·길이·SHA256을 대조한 후 삭제했다. 실파일 임시 원본은 실행기가 정리했다. 원본을 다시 확보하려면 공식 사이트 재조회가 필요하다. 원문·연락처·헤더 값은 대장에 기록하지 않았다.

## 실행 명령 / 결과 / 미검증

```powershell
# 군위 실제 fixture와 보관 대상 스냅샷 환경변수 활성화 후
.\gradlew.bat :test --tests '*GunwiDownloadContractTest' --tests '*GyeongbukSecondDownloadContractTest' --tests '*SaeolGetAttachmentDiscoveryProfileTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=GUNWI -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

최초 표적107건 중 실제 HTML 본문 선택 오류1건 실패, 수정 후107건 통과(27초). 확대253건 모두 통과·bootJar·실제 관측 성공(2분7초). Node23/23 및259영수증/214공고 재현 통과다. 조사 원본 정리 후 fixture 환경변수 없이 실행하면 조건부 실제 HTML 검사1건은 생략된다.

전체 프로젝트 테스트·첨부 추출·AWS·운영 DB·실제 운영 worker/DB/API/UI·운영 E2E는 이번 회차 미실행이다. 브라우저는 현재 요청에 명시되지 않아 사용자 정책상 생략했다. 목록 스케줄러 자동 유입까지 이번 관측으로 입증하지 않았다. migration/API/UI/분류 정책/추출기/운영 설정·배포·정책 게시·ENFORCE·기존 데이터 적용은 변경하지 않았다.
