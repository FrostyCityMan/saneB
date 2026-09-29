# 대구 남구·북구 첨부 연결 및 실파일 수집

## 현재 단계 / Gate

정상 파일부터 수집하고 개별 오류는 분리해 다른 지역을 진행한다. 오류 전부 해결이나 3표본 확보를 다음 지역 착수 조건으로 삼지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지, HWP 추출기1.0.16 추가 개선 보류를 유지한다.

- [x] 대구 남구 GET·북구 암호화 인자 POST 프로필2개 연결.
- [x] 두 지역 본문 및 HWP2개177,152byte 실제 수집.
- [!] 영종 목록 HTTP200이지만20byte 비목록 응답. 미등록 후속으로 분리.
- [x] 계약144통과/1조건부 생략·Node23건·bootJar,172영수증/131공고 재현.
- [x] 조사 원본9개127,172byte 삭제, 기존 근거·사용자 변경 보존.
- [ ] 미등록132지역 연결, 등록 후 미관측19지역 후속.
- [ ] 전국 목록→상시 worker, 본문 정제·추출·DB/API·정책 승인·운영 E2E Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 이번 작업에서는 운영 설정을 재조회하거나 변경하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 첫 파일 다운로드 관측 지역 | 70 | **72/223 (약32.3%)** |
| 다운로드 미관측 지역 | 153 | **151** |
| 로컬 프로필 등록 지역 | 89 | **91** |
| 미등록 지역 | 134 | **132** |
| 등록 후 다운로드 미관측 | 19 | **19** |
| 등록 지역 중 발견·파일 오류 근거 있음 | 23 | **23** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

다운로드 관측률은 전체 개발·운영 완료율이 아니다. 지역91+기업마당1=92프로필, 카탈로그146참조/지역88개다. 신규 expectation은 null이고 기존 승인 기대값1개를 유지한다. 이전170영수증·129공고의 profile hash와 기존 실행 metadata는 보존했다.

## 실제 결과와 구현 계약

| 지역 / 표본 | 확보 본문 | 발견 / 다운로드 | 파일 |
|---|---:|---|---|
| 대구 남구 LGS-000048 / 38607 | 195자 | 1 / 1, 발견 완료 | HWP60,928byte |
| 대구 북구 LGS-000049 / 55924 | 381자 | 1 / 1, 발견 완료 | HWP116,224byte |

두 표본 모두 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`다. 변경하지 않은 DRAFT 제목 조합을 통과했고 본문 중간 판정은 `TARGET_SUPPORT_CONFIRMED`다. 최종 운영 정책 승인, HWP 텍스트 추출, 전체 문서 분석, DB/API 저장은 검증하지 않았다.

북구55924는 **2023년 청년창업 특례보증 지원사업 변경 공고**다. 현재 공개 목록에서 확인해 다운로드 구조 검증에 사용했으며 현재 접수 가능한 모집 공고로 표시하거나 운영 공고로 생성하지 않았다. 처음 조사한68708은 임산부 지원 공고로 제목 대상 키워드 충족 근거가 없어 다운로드 표본으로 사용하지 않았다. 해당 상세는 구조 조사만 수행하고 파일을 요청하지 않았다.

- 남구 `LOCAL_DAEGU_NAMGU_GET_V1`: 공식 `eminwon.nam.daegu.kr`·SAFE_SAEOL_EMINWON 바인딩. `form1`/`table.boardw.wps_100` 제목과 `th` 첨부 라벨, `FileDown.jsp` 파일3인자 GET을 확인했다. 기존 `SaeolGetAttachmentDiscoveryProfile`을 수정 없이 재사용한다.
- 북구 `LOCAL_DAEGU_BUKGU_POST_V1`: 공식 `eminwon.buk.daegu.kr`·SAFE_SAEOL_EMINWON 바인딩. 공식 `form1`/`table.view`의 첨부 칸과 `nnn` hidden3개 폼을 확인했다. `goDownLoad`는 불투명3인자를 `/emwp/jsp/ofr/FileDownNew.jsp`에 POST한다.
- 북구는 기존 달서 프로토콜 구현을 바탕으로 별도 프로필을 추가했다. 기존 달서 코드·hash를 변경하지 않았다. 암호화 인자를 해독·로그·문서·카탈로그에 저장하지 않고 같은 공식 출처로만 전달한다. locator에는 비식별 hash를 사용한다. POST 필드 추가/변조와 GET 전환·다른 요청 redirect를 거부한다.
- 스크립트 실행 없이 고정 함수3인자만 파싱한다. 원문 URL hash·source/parser·HTTPS443·고정 호스트/경로/query·10파일 상한·미지원 형식 분리·역할 UNKNOWN·signature 검증을 유지한다. MIME/TLS 정책 완화는 없다.
- 고정 상세 공고부터 시작한 관측이다. 목록 수집기→상시 worker 자동 유입과 운영 성공으로 확대 해석하지 않는다. migration·DB/API·화면·운영 정책·worker 설정·HWP 추출기 변경 없음.

프로필 hash: 남구 `8061574eae7865f37de929761a7fcfb6c16841cf09fe88831b6f8eefb716cd5d`, 북구 `9ee12e417dfc5bf57e5ab521819fad8ab98e291b535d03a35503fe79b9fa99ed`. 관측 producer class는 `f89f1a000402def00888382191ddcfeefbde170c7bbdd99d131009b616f52d2c`다.

## 요청량과 정리

공식 조사 GET9회: 남구3(목록·검색·상세), 북구5(목록·소상공인 검색 무결과·지원 검색·상세2개), 영종1. 각각15초/연결7초/1MiB/자동 redirect0/TLS 검증 유지. 영종의 한 경로에서 받은 비정상 응답을 기관 전체 장애로 단정하지 않고 반복 요청하지 않았다.

실파일 관측은 각4회=8요청 예약 상한, 본문 예약 포함4,384,377byte다. 조사 포함17회 상한이며 실제 HTTP 요청 수와 동일시하지 않는다. 관측당 최대6요청·23MiB 제한을 유지했다.

조사 원본9개127,172byte와 관측 임시 원본을 삭제했다. 복구 사본 없이 hash·비식별 metadata만 보존한다. 사용자 `output/`, `scripts/qa/__pycache__/`, 기존 Java/Node 프로세스는 유지하고 작업용 단발 Node·Gradle은 종료했다.

## 검증 명령 / 결과

```powershell
.\gradlew.bat :test --tests '*MetroSaeolFirstDownloadContractTest' --tests '*SuseongDalseoCollectionContractTest' --tests '*SeoulFirstDownloadContractTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=DAEGU_NAMGU,DAEGU_BUKGU' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

Inventory는 로컬 보관 `target-inventory-20260928-receipt.txt`를 사용했다. 첫 실행은 잘못된 제목 라벨을 만드는 테스트 문자열이 남구의 scope 속성 있는 th에 적용되지 않아145건 중1실패/1생략(1분27초), 외부 관측 실행 전 중단됐다. 잘못된 라벨 검증 조건은 유지하고 fixture 변형을 수정했다. 최종145건 중144통과/1조건부 생략/0실패·오류, 관측·bootJar 포함1분31초 `BUILD SUCCESSFUL`. 생략1건은 기존 수성·달서 보관 HTML 전용 검사로 환경변수·원본이 제공되지 않아 실행하지 않았다. Node23/23,172영수증/131공고 재현 통과.

전체 테스트·Linux 임시 DB/API·AWS 운영 조회·배포·브라우저 QA는 미실행이다. 현재 지역 연결 요청에 브라우저 지시가 없어 정책상 생략했다. `[skip deploy]` 범위이며 전체 장기 Goal은 진행 중이다. 오류 해결을 전 지역 연결의 선행 조건으로 두지 않는다.
