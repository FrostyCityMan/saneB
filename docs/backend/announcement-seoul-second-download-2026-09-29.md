# 마포 본문·첨부 연결과 강남·도봉 표본 조사

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계이며 전체 Gate는 미완료다. 정상 파일 보존·개별 오류 분리, 제목 → 본문 → 첨부 → 관리자 검증, 제목 제외 원문 비저장, 자동 활성화 금지, HWP 추출기1.0.16 개선 보류를 유지한다.

- [x] 마포 공식 고시공고 본문·현재 첨부 목록 연결, 기존 새올 GET 파일 검증 재사용.
- [x] 마포19775 본문142자 AVAILABLE/ACCEPTED, HWP1개67,072byte 실제 다운로드.
- [x] Java213통과/조건부1생략, Node23·bootJar·실제 관측1건 통과.
- [x] 기존224영수증/180공고 보존, 총225영수증/181공고 재현.
- [x] 조사 HTML9개1,714,624byte 길이·SHA256 검증 후 개별 삭제.
- [ ] 미등록86지역 연결, 등록 후 다운로드 미확인28지역 후속.
- [ ] 강남·도봉 적격 표본 확보, 전국 목록 자동 유입·상시 worker·추출·DB/API/UI·운영 E2E.

## 지역 집계

2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷을 분모로 사용했다. 운영 설정은 이번에 재조회하거나 변경하지 않았다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 실제 파일 최소1개 다운로드 확인 | 108 | **109/223 (48.9%)** |
| 다운로드 미확인 | 115 | **114** |
| 프로필 등록 지역 | 136 | **137** |
| 미등록 지역 | 87 | **86** |
| 등록 후 다운로드 미확인 | 28 | **28** |
| 첨부 오류 관측 지역 | 33 | **33** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역137+기업마당1=138프로필. 카탈로그193공고/136대상, 신규 expectation=null, 기존 승인 기대값1개 유지. 다운로드 확인률은 전체 개발·운영 완료율이 아니다. 오류 관측 지역은 성공 지역과 겹칠 수 있어 잔여 수에 합산하지 않는다.

## 실제 관측

마포 `LGS-000015 / MAPO-19775`: `2026년 마포구 소기업·소상공인 특별신용보증 지원 계획 공고`. 제목 조합 통과, 공식 제목 일치·본문142자·현재 첨부1개 발견 및 다운로드 성공. 파일 역할 UNKNOWN, 정책 QA/기대값 승인/첨부 텍스트 추출/운영 쓰기는 수행하지 않았다.

- HWP SHA256: `ee25f70e59424e4388cfeecceaf910f53f323400f46729e409de0eea877d473d`.
- 프로필 hash: `637228782395cbbe19891d9e852a9358f0511e7422ea000cb41b7f896b9bde90`.
- producer class hash: `693038f71d9fd497349ffbfcf6620a57351af8f06b8492f0301c0a9a27f9097b`.

강남은 공식 목록200, `keyfield=BNI_MAIN_TITLE`의 ‘소상공인’ 검색 응답에 결과 없음이 표시됐다. 도봉은 기존 Contents 주소에서 새 Contents 주소, `/WDB_DEV/gosigong_go/`로 이어지는 공식 이동 경로를 확인했고 새 목록200을 받았다. 제목 검색 `strSearchType=1`/‘소상공인’은 등록 데이터 없음이었다. 각1회의 검색 결과일 뿐 지역 전체 지원 공고 부재를 뜻하지 않으며 두 지역은 미등록·표본 미확보로 유지한다.

## 구현 계약

`MapoNoticePage`는 `bbs_view`의 제목 라벨, 본문 표의 단독 colspan=4 셀, 현재 `bbs_view_file`의 첨부 목록을 분리한다. 메뉴·담당자·첨부명을 본문에 포함하지 않고 주석 처리된 과거 첨부 표도 읽지 않는다. `MapoAttachmentDiscoveryProfile`은 고정 수집원/목록 parser/원문 hash/HTTPS 포털 path/단일 bcId를 검증한다. 현재 목록 안의 `a.file_name`만 다운로드 대상으로 사용한다.

파일3필드·경로·파일명은 기존 새올 검증기를 재사용한다. `viewapplys`는 정확한 공고번호·미리보기 URL이 해당 다운로드와 일치하는지만 비교하며 스크립트·뷰어·HTTP 링크는 호출하지 않는다. 일부 미리보기나 파일 링크 실패 시 정상 descriptor를 보존한다. PDF/HWP/HWPX 및 미지원 확장자, 중복·부분 오류·10파일 제한·외부 host/HTTP/추가 query 거부, 본문 구조 변경을 테스트했다. 공통 프로필·fingerprint·기존 migration·DB/API·UI·HWP 추출기·운영 설정은 변경하지 않았다.

## 검증·요청량·미검증

```powershell
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_AUDIT='true'
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_RECEIPT='C:\PersonalProject\saneB\build\qa-results\target-inventory-20260928-receipt.txt'
$env:SANEB_MAPO_SURVEY_FIXTURE='true'
.\gradlew.bat :test --tests '*MapoDownloadContractTest' --tests '*SokchoDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=MAPO' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

첫 Gradle 실행1분46초 성공. Java214건 중213통과·이전 속초 실제 HTML fixture1건 조건부 생략, 신규 마포 실제 HTML 검사와 외부 관측 통과. Node23통과·bootJar 성공. 환경변수는 실행 전 값을 보관하고 finally에서 복원했다. 원본 삭제 후 신규 fixture 환경변수는 켜지 않는다.

조사9요청: 마포3·강남2·도봉4. 요청당15초/2MiB, TLS 검증·자동 redirect0을 유지하고 도봉은 응답에 관측한 공식 이동 주소만 개별 요청했다. Java 관측은6요청/23MiB 상한 중 본문 포함예약4회·2,319,872byte, 조사 포함예약 상한13회다. 예약 수와 실제 HTTP 호출 수는 다르다. 관측 파일은 finally에서 정리했고 조사9개 파일도 hash 대조 후 삭제했다.

전체 테스트·전국 상시 수집·첨부 텍스트 추출·운영 DB/API/UI·배포·정책 활성화·재분류는 이번 검증 범위가 아니다. 브라우저는 현재 명시적 실행 지시가 없어 사용자 정책상 생략했다. 커밋·푸시는 `[skip deploy]` 범위다.
