# 용인·의왕 첨부 수집 및 성남 목록 오류 분리

## 현재 단계 / Gate

성공 파일을 수집하고 실패는 별도 기록하는 지역 확대 단계다. 모든 오류 해결이나 3표본 확보를 다음 지역 착수 조건으로 삼지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지 및 HWP 추출기 1.0.16 개선 보류를 유지한다.

- [x] 용인 GET·의왕 POST 프로필 2개 연결 및 본문 확보.
- [x] HWPX 2개·HWP 1개, 총 529,839byte 다운로드.
- [!] 성남 기존 공식 목록 진입 주소 HTTP404·1,076byte. 다른 지역 수집을 막지 않고 현행 주소 확인 후속으로 분리.
- [x] 계약 160건·Node 23건·bootJar 및 187영수증/145공고 재현.
- [x] 조사 원본 7개 174,159byte와 관측 임시 원본 정리.
- [ ] 미등록 119지역 연결, 등록 후 다운로드 미관측 19지역 후속.
- [ ] 전국 목록→상시 worker·추출·DB/API·정책 승인·운영 E2E Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 이번에 운영 설정을 재조회하거나 변경하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 첫 파일 다운로드 관측 지역 | 83 | **85/223 (약 38.1%)** |
| 다운로드 미관측 지역 | 140 | **138** |
| 로컬 프로필 등록 지역 | 102 | **104** |
| 미등록 지역 | 121 | **119** |
| 등록 후 다운로드 미관측 | 19 | **19** |
| 최신 등록 표본의 발견·파일 오류 지역 | 23 | **23** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

다운로드 관측률은 전체 개발·운영 완료율이 아니다. 지역 104+기업마당 1=105프로필, 카탈로그 160참조/지역 101개다. 신규 expectation은 null이며 기존 승인 기대값 1개를 유지한다. 이전 185영수증·143공고 및 실행 metadata를 보존했다. 성남 목록 오류는 등록 표본 오류 23지역 집계와 별도다.

## 실제 결과 및 구현 계약

| 지역 / 표본 | 본문 | 발견 / 다운로드 | 파일 |
|---|---:|---|---|
| 용인 LGS-000086 / 145475 | 651자 | 2 / 2 | HWPX 366,489+102,934byte |
| 의왕 LGS-000109 / 38883 | 478자 | 1 / 1 | HWP 60,416byte |

두 결과는 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`, 본문 중간 판정은 `TARGET_SUPPORT_CONFIRMED`다. 텍스트 추출·구간 분석·DB/API 저장·최종 정책 승인으로 해석하지 않는다.

용인 표본은 2026년 온라인 플랫폼 비용 지원 추가모집 공고(등록 7월 1일, 접수 7월 20~24일)다. 이미 지난 모집을 현재 접수 가능한 공고로 표시하거나 운영 공고를 생성하지 않았다. 의왕 표본은 2025년 12월 31일 등록한 2026년 특례보증·이차보전 지원 계획이다. 현재 신청 가능 여부는 검증하지 않았다.

- 용인 `LOCAL_YONGIN_GET_V1`: `eminwon.yongin.go.kr`·SAFE_SAEOL_EMINWON_CELL 바인딩. 공식 form1/post의 contentDiv.boardGroup > boardDefalutView 아래 첨부 dl/dt/dd만 공통 새올 GET 검증기에 전달한다. 본문·메뉴 링크는 첨부로 취급하지 않는다. FileDown.jsp GET, 같은 요청 redirect만 허용한다.
- 의왕 `LOCAL_UIWANG_POST_V1`: `eminwon.uiwang.go.kr`·SAFE_SAEOL_EMINWON 바인딩. 태안과 동일하게 확인된 고전 표의 중첩 td 첨부 영역과 nnn hidden 4개 폼을 별도 프로필로 구현했다. 상세 `subCheck=N`, 다운로드 FileDownNew.jsp POST 불투명 3인자+고정 `isHome=Y`를 사용한다.
- 상세 source/parser·원문 URL hash·HTTPS443 고정 출처·정확한 요청 필드·10파일 상한·signature/MIME 검증·파일 역할 UNKNOWN을 유지한다. 스크립트를 실행하거나 불투명 인자를 해독·영구 저장하지 않는다.
- 발견 일부 실패·지원하지 않는 파일·다운로드 실패를 분리하고 정상 descriptor를 보존하는 기존 수집 전용 경로를 유지한다. 공유 클래스와 기존 profile hash, TLS/MIME 정책은 변경하지 않았다.
- 저장소 V61의 용인·의왕 공식 목록 주소와 검색 파라미터, 공개 목록의 공고 ID를 확인했다. 성남은 V55에서 정정했던 포털 진입 주소가 현재 404였다. 과거 새올 주소가 여전히 유효하다고 추측하여 바인딩하지 않았다.
- migration·DB/API·화면·운영 정책·worker·추출기 변경 없음. 고정 상세 표본 성공이며 목록에서 상시 worker까지의 자동 유입은 별도 Gate다.

Profile hash: 용인 `cd03c2c38066ca53b84aa038045716bf06fe19bf16e005dc6a6003d592751ded`, 의왕 `19cc70f9f0e568efc2635a194a4c508199b654a6846545ea891a7ed44c07214b`. Producer class는 `63d3e1709f9503957e15dd4c92165724f1eaac8ddfaa3b3ed24c153e2d5fe7db`다.

## 요청량 / 정리

공식 조사 GET 7회: 용인 3·의왕 3·성남 1. 요청당 15초/연결 7초/1MiB/자동 redirect 0/TLS 검증 유지. 성남 404는 추가 요청 없이 후속으로 분리했다.

관측 예약 상한 9회·본문 예약 포함 4,751,088byte. 조사 포함 16회 상한이며 실제 HTTP 요청 수와 동일시하지 않는다. 각 관측 최대 6요청·23MiB다. 조사 원본 7개 174,159byte를 길이·SHA256 대조 후 삭제했다. 관측 임시 원본도 정리하고 비식별 hash·결과 metadata만 보존했다. 사용자 output·__pycache__ 및 기존 프로세스는 보존한다.

## 검증 명령 / 결과

```powershell
.\gradlew.bat :test --tests '*CapitalNextDownloadContractTest' --tests '*ChungcheongNextDownloadContractTest' --tests '*GangwonNextDownloadContractTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=YONGIN,UIWANG' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

Inventory는 로컬 보관 target-inventory-20260928-receipt.txt를 사용했다. `SANEB_CAPITAL_SURVEY_FIXTURE=true`로 이번 조사 HTML에 대한 제목·첨부 경계 2건도 실행했다. 해당 환경변수는 복원했고 원본은 검증 후 삭제했으므로 향후 일반 테스트에서는 이 2건이 조건부 생략된다. 합성 회귀 테스트는 원본 없이 실행된다.

첫 실행 계약 160건 통과·실패/오류/생략 0, 실파일 관측·bootJar 포함 1분44초 BUILD SUCCESSFUL. Node 23건·187영수증/145공고 재현 및 diff 검사 통과.

전체 테스트·Linux 임시 DB/API·AWS 운영 조회·배포·브라우저 QA는 미실행이다. 현재 지역 확대 요청에 브라우저 지시가 없어 정책상 생략했다. `[skip deploy]` 범위이며 전체 장기 Goal은 진행 중이다.
