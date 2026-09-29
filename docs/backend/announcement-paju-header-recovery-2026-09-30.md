# 파주 파일명 헤더 복원과 PDF 다운로드

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드를 우선하고 개별 실패는 분리한다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지는 유지한다. HWP 추출기 개선·운영 정책·DB 변경은 이번 범위가 아니다.

- [x] LGS-000096 / PAJU-20260119101158902의 기존 파일명 헤더 오류 실측·복원.
- [x] 파주에만 기존 UTF-8 복원 옵션 연결. 공통 validator·발견 엔진·광명 지문 유지.
- [x] Java 수집 경로에서 본문144자·PDF1개93,037byte 다운로드 및 기본 형식 검증.
- [x] 관련 Java237통과·조건부2생략, bootJar, Node23/23,273영수증/224최신공고 재현.
- [x] 진단 원본4개386,424byte와 이번 실행 자원 정리. 이전 실패 보존.
- [ ] 잔여80개 수집원: 미등록43 + 등록 후 다운로드 성공 미확인37.
- [ ] 추출·구간 분석·상시 worker·운영 DB/API/UI·DRAFT·기존 데이터·운영 E2E 전체 Gate.

## 실측과 코드 변경

공식 상세 `https://www.paju.go.kr/user/board/BD_board.view.do?bbsCd=1022&seq=20260119101158902&q_ctgCd=4063`에서 UUID 기반 `/component/file/ND_fileDownload.do` 직접 첨부 링크를 확인했다. 상세1회·파일1회 진단 모두200이었다. 파일93,037byte·PDF prefix, Content-Type 헤더 없음, Content-Disposition의 UTF-8 바이트를 확인했다. 실제 파일명·전체 헤더는 문서에 복사하지 않는다.

파일명 헤더를 Latin-1 문자열로 읽으면 제어문자로 해석됐고 엄격한 UTF-8 복원 후에는 제어문자가 없었다. 실제 파일·헤더 fixture에서 기본 validator는 ATTACHMENT_DISPOSITION_INVALID, 기존 UTF-8 옵션을 적용하면 PDF 기본 형식 검증을 통과했다. Content-Type 누락은 기존 validator의 허용 동작이며 새로운 MIME 예외를 추가하지 않았다.

`CapitalEighthAttachmentProfileConfiguration`의 파주에만 기존 `Utf8DispositionAttachmentDiscoveryProfile`을 적용했다. URL·호스트·공고 식별·UUID·GET/redirect·개수·부분 성공·미리보기 제외 계약은 기존 엔진 그대로다. 기본 signature·확장자·경로·제어문자 검증, 공통 wrapper와 카탈로그는 변경하지 않았다. 광명 지문 `1c53827f824f0f270c3133cebb905ef5cc4525ea5d15694ef24ebb875b7c9ba7`과 제물포·검단의 동작을 보존한다. DB/migration·API/UI·추출기·운영 설정은 변경하지 않았다.

## 실제 관측

2026-09-30 07:40 KST(UTC `2026-09-29T22:40:21.391336500Z`):

- 표본: 2026년 파주시 소상공인 운전자금 지원계획 공고. 현재 모집 중이라는 의미는 아니다.
- 프로필 LOCAL_PAJU_BOARD_V1, 지문 `8020910ad3b864c3553617812b44243ee18af212385f613b232c79846e40098e`.
- 제목 COMBINATION_MATCHED, 본문 AVAILABLE144자 / ACCEPTED / TARGET_SUPPORT_CONFIRMED.
- 첨부 FOUND·complete=true·PDF1개 DOWNLOADED·93,037byte.
- 파일 SHA256 `25c6b8002fc63f61c1b0d48e456317433aabfa4398adb905a1a006c1e0a5d7f5`.
- COLLECTION_ONLY_OBSERVED_NOT_APPROVED, 원본 정리true, 운영 쓰기0.

미리보기는 요청하지 않았다. PDF 텍스트 추출·구간 분석·관리자 확정·상시 운영 성공은 이번 검증이 아니다. 이전 실패 보고서는 `PAJU-20260119101158902-BEFORE-UTF8.json`으로 해시 일치 복사하고 영수증 경로만 변경했다. 신규 성공 근거를 추가했으며 이전 실패를 성공으로 바꾸거나 삭제하지 않았다.

## 검증·집계·정리

```powershell
# 실제 진단 fixture 환경변수 SANEB_PAJU_HEADER_FIXTURE=true
.\gradlew.bat :test --tests '*PajuHeaderCompatibilityTest' --tests '*CapitalEighthDownloadContractTest' --tests '*AttachmentFileTypeValidatorTest' --no-daemon
# 같은 fixture와 기존 읽기 전용 inventory 환경변수 사용
.\gradlew.bat :test --tests '*PajuHeaderCompatibilityTest' --tests '*JemulpoHeaderCompatibilityTest' --tests '*CapitalEighthDownloadContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=PAJU -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

1차23건 중22통과·조건부1생략(27초). 확장239건 중237통과·실패0·조건부2생략, bootJar와 실제 관측 성공(2분2초)이다. 생략은 원본이 이미 정리된 제물포 진단 fixture와 수도권 이전 HTML fixture다. 이번 파주 실파일 fixture는 통과했다. 원본 정리 후에는 해당 환경변수를 켜지 않는다.

분모는2026-09-28 15:56:12 KST 읽기 전용 스냅샷의 활성223지역 수집원이다. 최소1파일 다운로드 확인142→143/223(64.1%), 잔여81→80, 미등록43 유지, 등록 미확인38→37, 첨부 오류45→44다. 오류는 성공 수집원과 겹칠 수 있다. 지역180+기업마당1=181프로필, 카탈로그236공고/179대상,273영수증/224최신공고다. 기존3표본·전체 파일 Gate는16충족/207잔여다. 단일 공고 다운로드를 모든 공고·전체 운영 완료로 표현하지 않는다.

진단2요청(각15초, HTML2MiB·파일10MiB 상한), 실제 관측1회 최대6요청/23MiB·상세1MiB, 본문 포함 실제 예약4회/2,485,101byte, 진단 포함 예약 상한6회다. 예약과 wire 요청 수는 구분한다. 원본4개는 절대 경로·크기·SHA256 대조 후 개별 삭제했으며 복구에는 공식 재조회가 필요하다. 보고서·해시는 보존했고 실제 관측 원본은 실행기가 정리했다. 이번 Node·Gradle·Java는 종료했으며 기존 프로세스·무관한 미추적 파일은 보존했다.

전체 프로젝트 테스트·추출·AWS·운영 DB·상시 유입·운영 E2E는 미실행이다. 브라우저는 현재 단계의 명시적 요청이 없어 정책상 생략했다. `[skip deploy]` 작업 브랜치 범위이며 운영 배포·정책 게시·ENFORCE·기존 데이터 처리는 하지 않았다.
