# 충청권 4지역 첫 첨부 수집과 부분 실패 보존

## 현재 단계 / Gate

전 지역의 첨부 발견·다운로드 연결을 우선한다. 2026-09-29 사용자 승인대로 개별 오류를 기록하고 수집 가능한 파일·다음 지역을 계속 처리한다. HWP 추출기 1.0.16 추가 개선은 보류한다. 장기 목표의 본문·첨부 텍스트 분석·worker DB/API·최종 검수·운영 E2E 범위를 축소하지 않는다.

- [x] 음성·논산·당진·청양 프로필4개와 실제 첨부6개 확인.
- [x] 당진 발견 부분 실패에도 확인된 파일2개 보존.
- [x] 서천·태안 서버 오류 및 본문 호스트 오류를 별도 기록.
- [x] 계약·catalog·로컬 등록 검사95건, 실파일 관측4건, Node23건, bootJar 통과.
- [ ] 나머지 지역 연결, 본문/추출 보완, 운영 상시 수집·전체 E2E Gate.

대상 분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷의 활성223지역이다. 이번에 운영 상태를 다시 조회하거나 변경하지 않았다.

| 지표 | 이전 | 현재 |
|---|---:|---:|
| 실제 다운로드 관측 지역 | 29 | **33** |
| 다운로드 미관측 지역 | 194 | **190** |
| 로컬 프로필 연결 지역 | 37 | **41** |
| 미등록 지역 | 186 | **182** |
| 기존 3표본·전체 파일 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

미관측190 = 미등록182 + 등록 미관측8. 33/223(약14.8%)는 첫 파일 다운로드 관측률이며 전체 목표 진행률·운영 수집 적용률이 아니다. 현재 프로필 근거 중 발견/파일 오류가 남은 지역은6곳이며, 본문 오류·미등록·미관측 전체 blocker 수를 뜻하지 않는다.

## 지역별 근거

| 지역 / 소스 | 고정 공고 | 첨부 결과 | 본문 / 별도 오류 |
|---|---|---|---|
| 음성 / LGS-000145 | 52473 소상공인 지원자금 | HWPX1개, 106,520byte | 본문448자 확보·2차 규칙 `TARGET_SUPPORT_CONFIRMED`; 최종 승인 아님 |
| 논산 / LGS-000153 | 50928 노란우산 공제가입 장려금 | HWP1개, 151,040byte | 본문 `DETAIL_HOST_NOT_ALLOWED`, 요청시도0 |
| 당진 / LGS-000155 | 57265 소상공인 화재보험료 | HWPX2개, 226,812byte | 발견 `ATTACHMENT_LINK_UNRESOLVED`, 본문 호스트 오류 |
| 청양 / LGS-000159 | 37758 소상공인 화재보험료 | HWPX2개, 221,970byte | 본문 호스트 오류 |
| 서천 / LGS-000158 | 공식 메뉴 | HTTP502, 미등록 유지 | 1회 요청 뒤 중단; 첨부 없음으로 판정하지 않음 |
| 태안 / LGS-000162 | 44242 사업장 시설개선 | 공식 상세 HTTP500, 미등록 유지 | 메뉴·목록·검색·상세 총4회 뒤 중단 |

당진 공식 첨부 칸의 `goPreviewGO(...)` 바로보기 링크2개가 공통 다운로드 규칙 밖에 있어 발견 상태가 부분 실패다. 확인한 `goDownLoad(...)` 파일2개는 모두 내려받아 signature/hash를 확인했다. 미해석 링크를 몰래 삭제하거나 전체 발견 성공으로 바꾸지 않았다. 결과는 `COLLECTION_ONLY_PARTIAL_NOT_APPROVED`이다. 나머지3공고는 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`이며 모두 정책·추출 승인과 다르다.

## 구현 범위

- `ChungcheongAttachmentProfileConfiguration`: 음성은 기존 새올 th 구조, 당진·청양은 검증된 단양형 form/98% 구조 재사용.
- `NonsanAttachmentDiscoveryProfile`: 공식 `form1 > table.bbs_view`의 제목 및 `첨부화일` 칸을 검증한 뒤 해당 표기만 정규화한다. 전체 첨부 칸과 미해석 링크를 유지한다.
- 공식 소스/목록 parser/호스트/쿼리 결합, HTTPS·파일 형식·크기·원본 정리 검사를 보존한다. 기존 프로필 hash·migration·API·자동 활성화 정책은 변경하지 않았다.
- 4개 QA 참조의 expectation은 null. 전체42프로필(지역41+기업마당1), catalog96참조/지역38개, 정상 승인0.
- 본문 호스트 오류는 공식 메뉴와 별도 새올 호스트의 연결 계약 문제다. 검사 완화·운영 URL 교체 없이 후속 오류로 남겼다.

## 예산·정리·검증

조사20요청(메뉴6·iframe4·검색5·상세5), 임시 HTML20개/890,816byte. 수집 관측18요청 예약·9,121,989byte 예약, 실제 본문1·상세4·파일6 = 11회 전송이다. 합계 **실제31회 / 상한 예약38회**. 지역별 누적 예약은 음성7/30·논산8/30·당진9/30·청양9/30·서천1/30·태안4/30이다. 관측은 공고당6요청/23MiB 상한, 조사 요청은15초/1MiB/redirect0으로 제한했다. TLS 검증·접근통제 우회는 없었다.

실파일6개 합계 **706,342byte**. 관측 harness가 원본을 삭제했고 모든 결과에 `originalFilesRemoved=true`가 있다. 조사 HTML20개도 소유 경로를 검증해 삭제했다. 원문 복구본은 보존하지 않고 비식별 hash·용량·오류 보고서만 유지한다. 단발 Node/Gradle 실행은 종료됐으며 운영 데이터/설정 쓰기0이다.

```powershell
.\gradlew.bat :test --tests '*ChungcheongFirstDownloadContractTest' --tests '*ChungbukFirstDownloadContractTest' --tests '*AttachmentCollectionOnlySummaryTest' :bootJar --no-daemon
.\gradlew.bat :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=EUMSEONG,NONSAN,DANGJIN,CHEONGYANG' -PsanebCollectionWindowsTrust=true --no-daemon
.\gradlew.bat :test --tests '*ChungcheongFirstDownloadContractTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
```

첫 검사23/23(1분3초), 실파일 관측4/4(46초), 마지막 계약검사95/95(1분) 및 bootJar 성공. 실파일4/4는 실행·오류기록 계약 통과이며 당진의 발견 부분 실패를 성공으로 바꾼 수치가 아니다. 마지막 검사에서 inventory export 환경을 임시 주입해 보관된 읽기 전용 영수증과 현재 로컬 등록을 대조했고 환경을 복원했다. Node23/23 및 **104영수증/최신81공고 재현 일치**. 전체 프로젝트 테스트·Linux worker DB/API·브라우저·운영 배포는 이번에 실행하지 않았다. 브라우저는 현재 지역 연결 요청 범위에 명시되지 않아 정책상 생략했다.

생산자 class hash: `262b67bc8da9e63bd75a298d53b2939c2ba2725cfe75a103d36d1a5f8383695a`. [근거 index](attachment-collection-receipt-index-2026-09-28.json)의 `chungcheongFirstDownloadRun`에 보고서 hash·오류·지역 누적 예산을 기록했다. 이전 영수증과 조사 이력은 보존했다.

다음은 미등록182지역 중 접근 가능한 공식 상세의 공통 구조 연결이다. 당진 바로보기 처리, 세 지역 본문 호스트 연결, 서천/태안 서버 오류, 지역별 추가 표본 확보는 후속 항목이며 다음 지역 진행을 막지 않는다.
