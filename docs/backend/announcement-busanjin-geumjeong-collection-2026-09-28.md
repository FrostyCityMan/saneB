# 부산진구·금정구 첨부 수집 연결 — 2026-09-28

## 범위와 확인한 구조

전 지역 첨부 수집을 우선한다. HWP 추출 보완은 중지 상태이며 이번에는 제목1차 정책 → 공식 상세/첨부 목록 → 전체 binary 다운로드 → signature/hash → 원본 정리만 검증한다. 운영 DB·정책·worker 설정·기존 데이터는 변경하지 않는다.

공식 구청 메뉴의 iframe에서 각 기관의 `eminwon` 게시판 연결을 확인했다. 운영15:56 읽기 전용 영수증의 `LGS-000032`, `LGS-000038`은 모두 `SAFE_SAEOL_EMINWON_COMPACT`, 활성 상태다. 두 상세 모두 form1 POST·th 첨부 라벨·goDownLoad3인자·FileDown.jsp GET 구조를 사용하므로 기존 `SaeolGetAttachmentDiscoveryProfile`에 고정 bean2개만 추가한다. 실행 엔진과 기존 프로필 hash는 바꾸지 않는다.

부산진구 제목은 `table.jin_gosi_table` 내부 `th.w_90` 제목 라벨/`td.w_330`, 금정구는 `table.bbs_vtype`의 `th[colspan=4]`다. 저장 HTML 검증은 정확한 제목과 전체 첨부 수를 확인하며 원본을 재요청하지 않는다. JavaScript는 실행하지 않고 공식 다운로드 인자와 경로만 해석한다.

## 표본과 정책 경계

| 지역 | 공고 | 이번 처리 | 첨부 |
|---|---|---|---|
| 부산진구 | 51342 | 청년 응시료 지원, 제목 통과 대상 | HWP1 |
| 부산진구 | 50698 | 노인일자리, 제목 조합 미충족·파일 요청 금지 | 수집 성공 수에서 제외 |
| 부산진구 | 47592 | 노인일자리, 제목 조합 미충족·파일 요청 금지 | 수집 성공 수에서 제외 |
| 부산진구 | 47279 | 청년 응시료 지원 변경 공고, 제목 통과 대상 | HWP1 |
| 부산진구 | 45554 | 청년 응시료 지원 모집 공고, 제목 통과 대상 | HWP1 |
| 금정구 | 43289 | 청년 전월세 중개수수료 지원, 제목 통과 대상 | HWPX3 |

처음 노인일자리 제목의 통과를 가정한 계약 검사는26건 중1실패했다. 임시 PostgreSQL의 ASCR-000001 DRAFT seed 판정이 `COMBINATION_NOT_MATCHED`임을 확인하고, 규칙 변경 없이 음성 표본으로 남겼다. 보정 후 계약3검사가 통과했다. 이것은 운영 ACTIVE 규칙 변경이나 검증이 아니다.

부산진구 공식 iframe의 `list_gubun=A`를 생략한 초기 목록 조회는 지난 자료를 충분히 제공하지 않았다. 공식 조건을 적용한 재조회에서 청년 응시료 공고47279·45554를 추가 확보했다. 둘은2024년 자료로 다운로드 구조 확인용이며 현재 신청 가능한 공고로 주장하지 않는다. 서로 다른 공고 ID를 유지하고 최초 음성 표본을 삭제하지 않는다.

금정구는 통과 공고1건만 고정했다. 첨부3개를 내려받아도 지역당 공고3건 기준을 충족한 것으로 바꾸지 않는다. 남은2건 이상의 적격 표본은 후속 작업이다. 모든 catalog 참조는 `expectation:null`이며 정상 기대값/정책 게시 권한을 부여하지 않는다.

## 요청 예산과 원본

- 사전 조사: 부산진구12 GET, 금정구6 GET, 총18회. 요청당15초·1MiB 상한이며 TLS 검증을 유지했다. 공식 메뉴 HTML2개는 메모리에서만 확인했다. 나머지16개 HTML은 작업 전용 임시 폴더에 보관했다가 검증 후 정리한다.
- 저장 HTML byte: 부산진구128,801·금정구35,176. 메모리 공식 메뉴의 decoded UTF-8 byte는 각각205,778·91,585이며 원시 전송량과 동일하다고 주장하지 않는다.
- 수집 실행기 공고당 최대6요청 예약·23MiB, 파일당20MiB. 적격 부산진구3건 최대18요청/69MiB, 금정구1건 최대6요청/23MiB. 제목 음성2건은0요청이어야 한다. 조사 포함 지역별 상한은 부산진구30·금정구12요청 예약이다.
- 기존 지역의 누적 예산은 초기화하지 않으며 소진된 중구/보은 또는 중지된 HWP 추출 QA를 실행하지 않는다.
- 임시 폴더는 `build/temporary-collection-busan-c43ff159b9424592a5b197171674b595`다. 원본 파일명/텍스트/개인정보는 수집 대장에 복사하지 않는다.

## 검증 경로

```powershell
.\gradlew.bat :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=BUSANJIN -PsanebCollectionWindowsTrust=true --no-daemon
.\gradlew.bat :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=GEUMJEONG -PsanebCollectionWindowsTrust=true --no-daemon
.\gradlew.bat :test --tests '*BusanSupportAttachmentCollectionContractTest' --tests '*SaeolGetAttachmentDiscoveryProfileTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
```

저장 HTML 검사는 `SANEB_BUSAN_SUPPORT_SAVED_DETAILS`, 운영 영수증/현재 등록 대조는 기존 읽기 전용 영수증에 대한 opt-in 환경변수를 해당 테스트 프로세스에만 주입한다. Windows-ROOT는 로컬 신뢰 저장소 선택이며 TLS 검증 우회가 아니다.

## 실제 검증 결과

- 부산진구:5검사 실패/생략0. 적격3공고 HWP3개254,976byte 다운로드/signature 확인. 제목 미충족2건은 파일0요청. 실행기12요청 예약·6,574,019byte 예약, 원본 정리 모두 확인.
- 금정구:1검사 실패/생략0. HWPX3개266,923byte 다운로드/signature 확인. 실행기6요청 예약·2,370,428byte 예약, 원본 정리 확인. 파일3개를 공고3건으로 세지 않으며 지역 Gate는 계속 미완료다.
- 총4공고6파일521,899byte,18요청 예약·8,944,447byte 예약. 조사 포함36요청 예약(부산진구24·금정구12)이다. 예약량에는 본문 상한이 포함되므로 실전송량과 구분한다.
- 부산진구 프로필 hash=`c744c614ff76525cadd5e0bb1b14a6a3c225d6328872471a4c59414d27c6f9f5`, 금정구=`0cac11b19737f21b4e8f0c5adf5ba650a97bd5b54bf3d03d12c984e81ead762f`. 기존7개 충족 지역의 profile hash도 유지됐다.
- JUnit 부산진구5검사 hash=`c79d7f57bb52e8d64ca49e59817490d2a813a194f616270f2dfc48bd9a627de4`, 금정구1검사 hash=`c45bbd7781b5443a4467437878ba50b911327a985b55e872627d285a7ebcf842`. 공통 JUnit 경로는 후속 실행으로 교체되므로 각 실행 직후 검사 수와 hash를 기록했다. 개별 원시 보고서는 `build/reports/attachment-regional-collection/BUSANJIN-*.json`, `GEUMJEONG-*.json`에 남고 [색인](attachment-collection-receipt-index-2026-09-28.json)이 각 hash/producer class hash를 고정한다.
- 최종 회귀93검사(새올23·카탈로그64·부산 계약3·등록 대조3), 실패/생략0. Node19검사·bootJar 실제 생성 통과. 전체 프로젝트/운영/Linux 검사는 재실행하지 않았다.
- 새 등록23프로필(지자체22·기업마당1), 실행 구현 클래스9개 유지. catalog53참조/19지역 프로필·보관 기대값1·정상0. 정책225대상 연결23/미등록202이며 지역은 활성223개 중 등록22/미등록201이다. 운영 상태 재조회가 아닌 기존15:56 읽기 전용 영수증과 최신 로컬 코드 대조다.
- [수집 대장](attachment-collection-regional-ledger-2026-09-28.json)은 **8지역 충족/215잔여**다. 등록됐지만 미완료14지역(금정구 포함)과 미등록201지역을 구분한다. 과거 음성/실패 기록을 성공으로 덮지 않는다.
- 저장 HTML16개163,977byte는 검증 후 삭제했다. 다시 확인하려면 공개 사이트에서 다시 받아야 한다. 기존 사용자 파일과 프로세스는 보존한다.

운영 상시 worker·추출·정책 QA·브라우저 E2E는 별도이며 브라우저는 현재 명시 지시가 없어 정책상 미실행이다.
