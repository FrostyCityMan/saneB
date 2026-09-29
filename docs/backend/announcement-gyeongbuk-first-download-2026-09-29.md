# 경북 포털 4지역 첨부 연결·첫 다운로드

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 우선 작업을 계속한다. 성공 파일은 보존하고 본문·발견·다운로드·추출 실패는 별도로 관리한다. 한 지역의 오류 해결이나 3표본 확보를 다음 지역 착수 조건으로 요구하지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지를 유지한다. HWP 추출기 1.0.16 추가 개선은 보류한다.

- [x] 김천·구미·영천·문경 공식 상세와 첨부 영역 확인.
- [x] 공통 처리기1개·시스템 프로필4개·미승인 참조4개 추가.
- [x] 4공고의 첨부5개 실제 다운로드·파일 signature 검증.
- [x] 계약121건·Node23건·bootJar·실파일 관측4건 통과.
- [x] 135영수증/최신105공고 재현 및 과거 근거 보존, 임시 원본 정리.
- [ ] 경주·경산·의성 등 미등록 지역의 다음 연결.
- [ ] 목록 상시 수집 연결·본문 정제·첨부 분석·worker DB/API·운영 E2E.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 이번 실행에서 운영 DB·설정·정책은 조회하거나 변경하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 첫 다운로드 관측 지역 | 50 | **54/223 (약24.2%)** |
| 다운로드 미관측 지역 | 173 | **169** |
| 로컬 프로필 등록 지역 | 61 | **65** |
| 미등록 지역 | 162 | **158** |
| 등록 후 다운로드 미관측 | 11 | **11** |
| 등록 지역 중 파일·발견 오류 근거 있음 | 10 | **10** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

24.2%는 파일 다운로드 관측률이지 전체 개발·운영 완료율이 아니다. 로컬 등록은 지역65+기업마당1=66프로필, 카탈로그120참조/지역62개다. 기존 expectation1개를 보존했고 신규 기대값은 승인하지 않았다. 운영 목록 parser와 profile 이름이 일치하는 것만으로 상시 수집 성공을 주장하지 않는다.

## 실제 파일 결과

| 지역 / 공고 | 파일 | 실제 크기 | 본문 확보 / 중간 근거 |
|---|---|---:|---|
| 김천 LGS-000203 / 41691 | HWPX1 | 112,241byte | 10,378자 / BODY_GROUP_B_MATCHED |
| 구미 LGS-000205 / 65352 | HWP1 | 163,840byte | 1,445자 / BODY_GROUP_A_MATCHED |
| 영천 LGS-000207 / 36723 | HWP1 | 106,496byte | 1,947자 / BODY_GROUP_A_MATCHED |
| 문경 LGS-000209 / 44422 | PDF1 + HWPX1 | 553,470 + 94,862byte | 3,828자 / BODY_GROUP_B_MATCHED |

총5파일 **1,030,909byte**다. 4공고 모두 제목 조합 통과·상세 식별·첨부 발견 완료, 파일 실패0·미지원0·미실행0이다. 최종 관측 상태는 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`이며 본문 중간 판정은 모두 `REVIEW_REQUIRED`다. 본문 확보 길이와 A/B 근거는 정제 품질·최종 정책 정확성의 증거가 아니다. 특히 김천의 긴 본문은 메뉴 혼입 여부를 후속 검증해야 한다. 구미2025년·영천2024년 표본은 다운로드 검증용이며 현재 모집 중인 공고로 표시하지 않는다.

영천 공식 스크립트의 HTTP 파일 주소는 그대로 요청하지 않고 동일 기관·경로의 HTTPS 다운로드 성공을 확인했다. 문경 PDF를 다운로드했다는 사실은 PDF 텍스트 추출·구간 분석 완료가 아니다. HWP/HWPX도 동일하다.

## 구현 경계와 남은 연결

`GyeongbukPortalAttachmentDiscoveryProfile`은 공식 `form#detailForm` 안의 제목 및 `dl.view_file` 첨부 영역만 해석한다. 김천은 고정 `fileDownFrm`의 공개 POST3필드, 나머지는 GET3query다. `goDownload`/`goDownloadPost`의 세 문자열만 기존 선형 해석기로 읽으며 JavaScript를 실행하지 않는다. 공통 해석기와 기존 profile hash는 변경하지 않았다.

- 기관·목록 parser·source URL hash·공고 ID·메뉴 ID·HTTPS 호스트·경로를 고정 검증한다.
- 문경·구미의 바로보기 링크는 같은 공고의 직전 다운로드와 파일3인자가 일치할 때만 중복 뷰어로 구분한다. 뷰어를 파일 수집 성공으로 세지 않는다.
- 미해석 링크·미지원 형식·파일 수 초과는 오류로 남기되 정상 descriptor를 보존한다. 파일명으로 문서 역할이나 정상 판정을 자동 승인하지 않는다.
- 다른 파일/기관으로 redirect, 중복 query, 임의 폼 필드, 경로 우회, script 추가를 허용하지 않는다.
- migration·DB/API·화면·운영 worker·정책은 변경하지 않았다.

문경 목록은 `gosiView('portal','list',공고번호,메뉴번호,...)` 함수이며 공식 함수 본문으로 상세 경로를 확인했다. 현 저장소 기본 AUTO 링크 해석은 명시 URL/data 속성 또는 script 내부 URL을 우선 사용한다. 이번 고정 표본은 공식 목록에서 식별한 상세를 직접 사용했으므로 **운영 목록부터 자동 유입되는지는 미검증**이다. 현재 운영 parser의 세부 필드는 재조회하지 않았고, 후속 목록 계약 검증 대상으로 남긴다. 김천·구미·영천도 이번 검증은 고정 상세→파일이며 운영 스케줄러부터의 E2E가 아니다.

## 조사 및 예산

| 지역 | 사전 조사 | 관측 요청 예약 | 합계 |
|---|---:|---:|---:|
| 김천 | 7 | 4 | 11/30 |
| 구미 | 7 | 4 | 11/30 |
| 영천 | 7 | 4 | 11/30 |
| 문경 | 7 | 5 | 12/30 |
| 경주·경산·의성 | 각1 | 0 | 각1 |

조사31요청(GET23·HEAD4·POST4), 관측 예약17회, 총48회다. HEAD403과 이후 GET200은 별도 결과로 보존한다. 공통 포털 이동 경로는 공식 같은 호스트로 확인한 뒤 요청했고 일회성 query 값은 코드·문서·로그에 보관하지 않았다. 조사 요청은 각15초/연결7초/1MiB/자동 redirect0/TLS 검증 유지다.

관측은 공고당 최대6요청·23MiB, 실제 예약 합계17회·10,844,925byte다. 본문 상한 예약을 포함하므로 HTTP 실제 횟수/전송 byte와 동일하다고 표현하지 않는다. 경주·경산·의성은 목록 HTTP200 및 검색 구조까지만 조사했으며, 상세/첨부 성공이나 부재로 세지 않는다.

조사 HTML19개·4,866,493byte와 관측 상세/첨부 임시 원본을 삭제했다. 복구 사본 없이 hash·크기·상태만 남겼다. 단발 Node와 Gradle은 종료했고 사용자 기존 프로세스·`output/`·`scripts/qa/__pycache__/`는 보존했다.

## 검증 명령 / 결과

```powershell
.\gradlew.bat :test --tests '*GyeongbukFirstDownloadContractTest' --tests '*JeonnamThirdDownloadContractTest' --no-daemon
.\gradlew.bat :test --tests '*GyeongbukFirstDownloadContractTest' --tests '*JeonnamThirdDownloadContractTest' --tests '*JeonnamSecondDownloadContractTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=GIMCHEON,GUMI,YEONGCHEON,MUNGYEONG' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

초기26건 성공(46초), 확장121건 및 bootJar·실파일4공고 성공(1분25초). 실패·오류·skip0. Node23/23, 135영수증/최신105공고 재현 성공. 이전131영수증과 기존 실행 metadata를 보존했다. inventory export는 기존 읽기 전용 영수증과 로컬 등록 비교이며 새 운영 조회가 아니다.

전체 프로젝트 테스트·Linux worker 임시 DB/API·AWS 운영 조회·운영 배포·브라우저 검증은 실행하지 않았다. 브라우저는 현재 지역 연결 작업에서 명시 지시가 없어 정책상 생략했다. `[skip deploy]` 변경이며 전체 Goal은 계속 진행 중이다. 다음 묶음은 경주·경산·의성 및 나머지 경북 지역을 우선한다.
