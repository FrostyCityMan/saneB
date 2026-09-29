# 은평·서초 첨부 연결과 서울권 접근 오류 분리

## 현재 단계 / Gate

정상 파일부터 수집하고 개별 오류는 분리하여 다른 지역을 진행한다. 오류 전부 해결이나 3표본 확보를 다음 지역 착수 조건으로 삼지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지, HWP 추출기1.0.16 추가 개선 보류는 유지한다.

- [x] 은평 중첩 첨부 칸 연결·서초 기존 새올 GET 엔진 재사용, 프로필2개 추가.
- [x] 서초 HWP1개50,688byte 실제 수집, 두 공고 본문 확보.
- [!] 은평 첨부4개 발견 후 공식 다운로드 응답 모두 HTTP404. 성공으로 집계하지 않는다.
- [!] 동작 목록 HTTP400·강동 HTTPS 목록15초 타임아웃은 미등록 후속으로 분리.
- [x] 최종 계약154건·Node23건·bootJar 통과,170영수증/129공고 재현.
- [x] 조사 원본8개134,266byte 삭제, 이전 근거·사용자 변경 보존.
- [ ] 미등록134지역 연결, 등록 후 다운로드 미관측19지역 후속.
- [ ] 전국 목록→상시 worker, 본문 정제·추출·DB/API·정책 승인·운영 E2E Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 이번 작업에서는 운영 설정을 재조회하거나 변경하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 첫 파일 다운로드 관측 지역 | 69 | **70/223 (약31.4%)** |
| 다운로드 미관측 지역 | 154 | **153** |
| 로컬 프로필 등록 지역 | 87 | **89** |
| 미등록 지역 | 136 | **134** |
| 등록 후 다운로드 미관측 | 18 | **19** |
| 등록 지역 중 발견·파일 오류 근거 있음 | 22 | **23** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

다운로드 관측률은 전체 개발·운영 완료율이 아니다. 지역89+기업마당1=90프로필, 카탈로그144참조/지역86개다. 신규 expectation은 null이고 기존 승인 기대값1개를 유지한다. 이전168영수증·127공고의 profile hash와 기존 실행 metadata는 보존했다.

## 실제 결과와 구현 계약

| 지역 / 표본 | 확보 본문 | 발견 / 다운로드 | 결과 |
|---|---:|---|---|
| 은평 LGS-000013 / 48267 | 370자 | 4 / 0 | `COLLECTION_ONLY_PARTIAL_NOT_APPROVED`, 파일4개 `ATTACHMENT_HTTP_404` |
| 서초 LGS-000023 / 43962 | 690자 | 1 / 1 | `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`, HWP50,688byte |

두 표본은 변경하지 않은 DRAFT 제목 조합을 통과했고 본문 중간 판정은 `TARGET_SUPPORT_CONFIRMED`다. 최종 운영 정책 승인이나 전체 문서 분석 완료로 승격하지 않는다. HWP 내부 구조·텍스트 추출·DB/API 저장은 이번에 검증하지 않았다.

- 은평 `LOCAL_EUNPYEONG_GET_V1`: 공식 `eminwon.ep.go.kr`·SAFE_SAEOL_EMINWON_COMPACT에 결합한다. `form1`/`table.board2`의 제목과 중첩된 `td > font` 첨부 라벨을 검증한 뒤 **첨부 칸 전체**를 기존 GET 파서에 전달한다. 미확인 요소도 보존하므로 정상 파일과 오류를 각각 기록한다.
- 서초 `LOCAL_SEOCHO_GET_V1`: 공식 `eminwon.seocho.go.kr`·SAFE_SAEOL_EMINWON에 결합한다. 기존 GET 엔진의 `th` 첨부 라벨 처리를 그대로 재사용한다.
- 두 공식 페이지의 `goDownLoad`는 실제 `/emwp/jsp/ofr/FileDown.jsp`와 파일3인자를 사용함을 확인했다. 스크립트는 실행하지 않는다. 은평404를 임의 경로 추측이나 TLS 우회로 감추지 않는다.
- 원문 URL hash·source/parser·고정 호스트/경로/query·HTTPS443·10파일 상한·미지원 형식 분리·역할 UNKNOWN·signature 검증을 유지한다. 은평 adapter의 redirect는 같은 요청에만 허용한다. 기존 공유 엔진의 코드·프로필 hash는 변경하지 않았다.
- 이번 관측은 공식 목록/검색에서 고른 **고정 상세 공고부터 시작**한다. 목록 수집기→상시 worker 자동 유입과 운영 성공의 증거는 아니다.
- migration·DB/API·화면·운영 정책·worker 설정·HWP 추출기 변경 없음.

프로필 hash: 은평 `1a8524e32e6e677733d4d590095c083bd30cfb18eb50e0cf67943031367faa9d`, 서초 `7866e20c8cd9a75c9017eaf703e964148c4d0edaf9c37a75075b0f62e85128b2`. 관측 producer class는 `bf4fd04861ad8ac523cbc768f535828de5b1ef4626a96445b189ad134545d4c0`다.

## 요청량과 정리

공식 조사 GET9회: 은평4(목록·소상공인 검색 무결과·지원 검색·상세), 서초3(목록·검색·상세), 동작1, 강동1. 각각15초/연결7초/1MiB/자동 redirect0/TLS 검증 유지. 동작·강동 오류를 지역 전체 장애로 단정하지 않고 동일 경로를 반복 요청하지 않았다.

실파일 관측은 은평7·서초4=11요청 예약 상한, 본문 예약 포함4,269,364byte다. 은평은 확인된4파일 전부를 시도하도록 본문2+상세1+파일4=최대7요청으로 명시했고 서초 상한은6이다. 관측별23MiB 제한을 유지했다. 조사 포함20회 상한으로 실제 요청 횟수와 예약 상한은 구분한다.

조사 원본8개134,266byte와 관측 임시 원본은 삭제했다. 복구 사본 없이 hash·비식별 metadata만 보존한다. 사용자 `output/`, `scripts/qa/__pycache__/`, 기존 Java/Node 프로세스는 유지하고 작업용 단발 Node·Gradle은 종료했다.

## 검증 명령 / 결과

```powershell
.\gradlew.bat :test --tests '*SeoulFirstDownloadContractTest' --tests '*JejuFirstDownloadContractTest' --tests '*GyeongnamThirdDownloadContractTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=EUNPYEONG,SEOCHO' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

Inventory는 로컬 보관 `target-inventory-20260928-receipt.txt`를 사용했다. 첫 실행은 카탈로그 기대값 한 곳이142로 남아154건 중1실패(1분32초), 외부 관측 실행 전 중단됐다. 실제144참조와 대조해 수정한 최종 실행은154건 실패·오류·skip0, 관측·bootJar 포함1분38초 `BUILD SUCCESSFUL`이다. Node23/23,170영수증/129공고 재현 통과. 수집 전용 명령의 성공은 은평 파일 다운로드 성공을 뜻하지 않는다.

전체 테스트·Linux 임시 DB/API·AWS 운영 조회·배포·브라우저 QA는 미실행이다. 현재 지역 연결 요청에 브라우저 지시가 없어 정책상 생략했다. `[skip deploy]` 범위이며 전체 장기 Goal은 진행 중이다. 오류 해결을 전 지역 연결의 선행 조건으로 두지 않는다.
