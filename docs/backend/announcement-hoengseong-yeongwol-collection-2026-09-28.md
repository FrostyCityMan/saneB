# 횡성·영월 첨부 수집 전용 검증 — 2026-09-28

## 범위와 완료 경계

사용자의 전 지역 첨부 수집 우선 지시에 따라 기존 두 프로필을 현재 코드로 검증한다. HWP1.0.16 추가 개발과 첨부 텍스트 추출은 실행하지 않는다. 공식 제목 정책 → 본문/상세 식별 → 전체 첨부 목록 → 실제 다운로드 → signature/크기/hash 검증 → 원본 정리를 수행한다. 운영 DB·정책·worker 설정은 변경하지 않는다.

09-14 이전 영수증은 현재 프로필 해시와 다르고 제목 1차 판정·공식 제목 동일성 확인이 없으므로 현재 Gate로 바로 승격하지 않았다. 해당 과거 다운로드 성공 자체를 취소하는 것은 아니다.

## 고정 표본

| 지역 | 공고 | 제목 판정 | 예상 첨부 |
|---|---|---|---|
| 횡성 | 424679 | 조합 충족 | HWPX 1 |
| 횡성 | 424078 | 조합 충족 | HWPX 2 |
| 횡성 | 424077 | 조합 충족 | HWPX 2 |
| 영월 | 157529 | 조합 미충족, 파일 요청 금지 | 과거 PDF 2; 이번 미요청 |
| 영월 | 157016 | 조합 충족 | HWPX 1 |
| 영월 | 156846 | 조합 미충족, 파일 요청 금지 | 과거 HWPX 1; 이번 미요청 |
| 영월 | 150619 | 조합 충족 | PDF 1·HWP 1 |
| 영월 | 147676 | 조합 충족 | PDF 1·HWP 1 |

두 음성 표본은 기록에서 삭제하지 않으며 성공 표본 수에 넣지 않는다. 147676은 2025년 공고로 구조·다운로드 검증용이며 현재 접수 가능 공고라는 뜻이 아니다. 분류 규칙은 임시 loopback PostgreSQL에 migration을 적용해 읽은 ASCR-000001 DRAFT seed다. 운영 ACTIVE 규칙 검증으로 표현하지 않는다.

## 요청 예산과 원본 관리

- 공식 표본 제목/파일 수 확인용 사전 조사: 13 GET, 요청당 HTTPS·15초·응답 1MiB 상한. decoded UTF-8 합계 1,705,978byte이며 전송 원시 byte와는 다르다. 조사 HTML은 프로세스 메모리에서만 처리했고 저장하지 않았다.
- 사전 조사는 횡성 상세6·영월 상세5·두 지역 목록2 요청이다. 횡성 첫 제목 selector 미일치 후 정확한 subject selector를 확인하기 위한 추가3요청을 포함한다. 다운로드 재시도는 아니다.
- 실제 실행기: 표본당 최대5요청 예약·43MiB, 파일당20MiB. 본문 최대2요청/2MiB를 보수적으로 먼저 예약한다. 통과 표본6건의 상한30요청·258MiB이며 제목 중단2건은0요청이다. 사전 조사 포함 이번 회차 최대43요청 예약이다.
- 기존09-12/09-14 캠페인 기록은 별도 역사로 보존한다. 이 실행이 이전 예산/차단/승인 기록을 초기화하지 않는다. 중구·보은 등 소진된 캠페인은 실행하지 않는다.
- Windows-ROOT 신뢰 저장소를 로컬 QA에만 선택한다. 인증서·호스트 검증은 유지하며 TLS 우회는 없다.
- 각 실행은 임시 원본을 finally에서 삭제하고 `originalFilesRemoved`를 기록한다. 수집 결과는 원문·파일명·URL 대신 비식별 metadata만 대장에 이관한다.

## 실행 및 근거

```powershell
.\gradlew.bat :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=HOENGSEONG -PsanebCollectionWindowsTrust=true --no-daemon
.\gradlew.bat :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=YEONGWOL -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
.\gradlew.bat :test --tests '*AnnouncementAttachmentBbsOfficialObservationContractTest' --tests '*AttachmentCollectionOnlySummaryTest' --tests '*StandardBbsAttachmentDiscoveryProfileTest' :bootJar --no-daemon
```

### 실제 결과

- 횡성:3검사 실패/생략0,3공고·HWPX5개·587,367byte,14요청 예약·7,419,495byte 예약. 모든 원본 정리 확인.
- 영월:5검사 실패/생략0. 제목 미충족2건은0요청. 통과3공고·HWPX1/PDF2/HWP2,1,391,202byte,14요청 예약·7,903,842byte 예약. 모든 원본 정리 확인.
- 총6개 통과 공고·10파일·1,978,569byte. 실행기28요청 예약·15,323,337byte 예약, 조사 포함41요청 예약. 본문 상한을 포함한 예약량이며 실제 전송 요청/byte와 동일하다고 주장하지 않는다.
- 원시 보고서: `build/reports/attachment-regional-collection/HOENGSEONG-*.json`, `YEONGWOL-*.json`. producer class SHA256=`e575d1faf10dbc2879d1e57479549206aaf969faa7938e0eb7cd7208b631d842`.
- JUnit 횡성3검사 hash=`5fea89cd908f6ce9fdc4e1780ff457207d337cf202c96577466c51756d27ebf7`, 영월5검사 hash=`7c0879e1a1a10aff4d30272f9974ad4aab4d23a118e03f4d1d9e6269f87bd950`. 공통 JUnit 경로는 뒤 실행으로 교체되므로 각 실행 직후 hash와 검사 수를 기록했다. 공고별 원시 보고서 및 그 hash는 별도로 남는다.
- [색인](attachment-collection-receipt-index-2026-09-28.json)의52영수증/최신29공고 재현 통과. [대장](attachment-collection-regional-ledger-2026-09-28.json)은 활성223지역 중6충족/217잔여로 갱신됐다. 등록19/미등록204는 그대로이며 catalog44/16·정상 기대값0은 증가시키지 않는다.
- Node19검사 통과. 제목 중단을 정상 수집으로 세지 않고, 요청 또는 발견 흔적이 있는 중단 보고서는 거부한다. Java 회귀110검사(표준BBS81·관측계약26·수집요약3) 실패/생략0, Gradle2분6초 성공. bootJar는 생산 코드 변경이 없어 UP-TO-DATE였으며 새 빌드 실행으로 계수하지 않는다.
- 이번 실행이 생성한 Node·Gradle·Java 프로세스 종료를 확인했다. 기존 사용자의 Java/Node 프로세스, `output/`, `scripts/qa/__pycache__/`는 변경·종료하지 않았다.

로컬 다운로드 성공은 Linux 운영 상시 수집·텍스트 추출·정상 기대값·정책 게시·전체 E2E 완료가 아니다. 현재 요청에 브라우저 실행 지시가 없으므로 브라우저 QA는 정책상 미실행이다.
