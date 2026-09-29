# 제물포 파일명 헤더 복원과 첨부 3개 다운로드

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드를 우선하고 개별 실패는 분리한다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지는 유지한다. HWP 추출기 개선·운영 정책·DB 변경은 이번 범위가 아니다.

- [x] LGS-000055 / JEMULPO-14094의 기존 파일명 헤더 오류 원인 실측.
- [x] 제물포에 기존 UTF-8 복원 옵션 연결. 공통 validator·발견 엔진·미추홀·검단 지문 유지.
- [x] Java 수집 경로에서 본문107자·HWPX3개166,884byte 다운로드 및 기본 형식 검증.
- [x] 관련 Java237통과·조건부2생략, bootJar, Node23/23,272영수증/224최신공고 재현.
- [x] 진단 원본4개279,409byte와 이번 실행 자원 정리. 이전 실패 보고서 보존.
- [ ] 잔여81개 수집원: 미등록43 + 등록 후 다운로드 성공 미확인38.
- [ ] 추출·구간 분석·상시 worker·운영 DB/API/UI·DRAFT·기존 데이터·운영 E2E 전체 Gate.

## 원인과 최소 변경

공식 상세 `https://www.jemulpo.go.kr/main/bbs/bbsMsgDetail.do?msg_seq=14094&bcd=announce`에서 개별 첨부3개를 확인했다. 진단은 상세1회와 첫 첨부1회만 수행했다. 두 응답 모두200이었으며 첫 파일은70,106byte, MIME `application/octet-stream;charset=UTF-8`이었다.

Content-Disposition은 UTF-8 바이트이지만 Latin-1 문자열로 읽으면 제어문자로 해석됐다. 엄격한 UTF-8 복원 후에는 제어문자가 없었다. 실파일·실제 헤더 fixture에서 기본 검증은 ATTACHMENT_DISPOSITION_INVALID, 기존 UTF-8 복원 옵션은 HWPX 기본 형식 검증 통과로 재현했다. 실제 파일명·헤더 원문은 문서에 남기지 않았다.

`IncheonSecondAttachmentProfileConfiguration`에서 제물포 프로필만 기존 `Utf8DispositionAttachmentDiscoveryProfile`로 감쌌다. URL·호스트·공고 식별·요청 방식·redirect·첨부 개수·부분 성공·미리보기 제외는 기존 엔진에 그대로 위임한다. 공통 MIME/signature/확장자/경로/제어문자 검증은 변경하지 않았다. 미추홀 지문 `80d669d61bfcdc6d92ea1af53c73cde316a228cbb4317c97b17bf5f59d49006e`는 유지한다. 검단·영종과 기존 카탈로그에도 변경이 없다. 관리자 선택 기능·DB/migration·API/UI·추출기·운영 설정은 추가하거나 변경하지 않았다.

## 실제 수집 근거

2026-09-30 07:31 KST(UTC `2026-09-29T22:31:40.408349100Z`):

- 표본: 2026년 제물포구 청년 컬처페이 지원사업 참여자 모집공고. 현재 모집 중이라는 의미는 아니다.
- 프로필: LOCAL_JEMULPO_BOARD_V1, 지문 `d5d18183fd183745100c9ea92bd355a4394418048601f0f379176ad4fba701d5`.
- 제목 COMBINATION_MATCHED, 본문 AVAILABLE107자 / ACCEPTED / TARGET_SUPPORT_CONFIRMED.
- 첨부 FOUND·complete=true·3개 모두 DOWNLOADED, COLLECTION_ONLY_OBSERVED_NOT_APPROVED.
- 원본 정리true, 운영 쓰기0. 미리보기·전체 압축 다운로드는 요청하지 않았다.

| 파일 | 수신량 | 형식 | SHA256 |
|---|---:|---|---|
| 1 | 70,106byte | HWPX | e078acdada2c0b70bb0cd3448c858a0605208c01dc0c1fb86ac3ef0319c448c0 |
| 2 | 43,372byte | HWPX | cdd5c8975b17df5932f2e497ad9a5496f60e720246652a4408b64a8c66472808 |
| 3 | 53,406byte | HWPX | c111b08170f61a8efa05b491ea404ee30a30e13acb894255514196054af0048b |

기본 ZIP signature와 예상 형식·응답 검증을 통과했다. ZIP 내부 HWPX 구조·텍스트 추출·구간 분류·관리자 확정은 이번 검증이 아니다. 이전 실패는 `JEMULPO-14094-BEFORE-UTF8.json`으로 해시 일치 복사하고 영수증 경로만 옮겼다. 신규 성공 근거를 추가했으며 이전 실패를 삭제하거나 성공으로 덮어쓰지 않았다.

## 검증·집계·정리

```powershell
# 실제 진단 fixture 환경변수 SANEB_JEMULPO_HEADER_FIXTURE=true
.\gradlew.bat :test --tests '*JemulpoHeaderCompatibilityTest' --tests '*IncheonSecondDownloadContractTest' --tests '*AttachmentFileTypeValidatorTest' --no-daemon
# 같은 fixture와 기존 읽기 전용 inventory 환경변수 사용
.\gradlew.bat :test --tests '*JemulpoHeaderCompatibilityTest' --tests '*GeomdanHeaderCompatibilityTest' --tests '*IncheonSecondDownloadContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=JEMULPO -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

1차23건 중22통과·조건부1생략(27초). 확장239건 중237통과·실패0·조건부2생략, bootJar와 실제 관측 성공(2분1초)이다. 생략은 원본이 정리된 검단 진단 fixture와 인천권 이전 HTML fixture다. 이번 제물포 실파일 fixture는 통과했다. 원본 정리 후에는 해당 환경변수를 켜지 않는다.

분모는2026-09-28 15:56:12 KST 읽기 전용 스냅샷의 활성223지역 수집원이다. 최소1파일 다운로드 확인141→142/223(63.7%), 잔여82→81, 미등록43 유지, 등록 미확인39→38, 첨부 오류46→45다. 오류는 성공 수집원과 겹칠 수 있다. 지역180+기업마당1=181프로필, 카탈로그236공고/179대상,272영수증/224최신공고다. 기존3표본·전체 파일 Gate는16충족/207잔여다. 단일 공고의 파일3개 성공을 모든 지역 공고·상시 운영 완료로 표현하지 않는다.

진단2요청(각15초, HTML2MiB·파일10MiB 상한), 실제 관측1회 최대6요청/23MiB·상세1MiB, 본문 포함 실제 예약6회/2,477,028byte, 진단 포함 예약 상한8회다. 예약과 실제 wire 요청 수는 구분한다. 원본4개는 허용된 절대 경로·크기·SHA256 대조 후 개별 삭제했다. 복구에는 공식 재조회가 필요하며 보고서·해시는 보존했다. 실제 관측 원본은 실행기가 정리했다. 이번 Node·Gradle·Java는 종료했고 기존 프로세스·무관한 미추적 파일은 보존했다.

전체 프로젝트 테스트·추출·AWS·운영 DB·상시 유입·운영 E2E는 미실행이다. 브라우저는 현재 단계의 명시적 요청이 없어 정책상 생략했다. `[skip deploy]` 작업 브랜치 범위이며 운영 배포·정책 게시·ENFORCE·기존 데이터 처리는 하지 않았다.
