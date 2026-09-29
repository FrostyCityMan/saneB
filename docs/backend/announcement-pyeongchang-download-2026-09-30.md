# 평창 본문·첨부 연결 및 DNS 실패 분리

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드를 우선 확대한다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 외부 공고 자동 활성화 금지는 유지한다. HWP 추출기 1.0.16 추가 개선은 보류한다. 전체 장기 goal과 운영 E2E는 미완료다.

- [x] 평창 LGS-000127 / SPRING_BBS 시스템 프로필·본문 수집기 연결.
- [x] 실제 HTML의 제목·본문·첨부 영역과 고정 새올 GET 인자 확인.
- [x] 정상 파일 보존, 부분 오류, 중복·상충, 미지원 형식, 10파일 제한, 요청·redirect 경계 검증.
- [x] 본문 324자와 HWP 1개 발견. 실제 다운로드 실패를 별도 기록.
- [x] Java 표적 회귀 203통과/0실패/0생략, Node23/23, bootJar 성공.
- [x] 253영수증/209공고 재현 및 조사 원본 정리.
- [ ] 평창 파일 서버 DNS 시간 초과 후속 및 다른 미등록 지역 연결.
- [ ] 전국 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E 완료.

## 구현 계약

공식 목록은 `https://www.pc.go.kr/portal/government/government-notification`이다. 공개 표본 `PYEONGCHANG-41378`의 상세는 `?noticeMgrNo=41378`, 제목은 `평창군 소상공인 특례보증 지원사업 개정 공고`다.

`PyeongchangNoticePage`는 `#contentsArea` 바로 아래의 고시공고 표에서 제목·내용·첨부파일 행을 각각 선택한다. 본문에는 메뉴·담당부서·연락처 행·첨부 파일명을 섞지 않는다. 상세 ID, 고정 HTTPS host/path와 허용 query만 받으며 중복 query·임의 mode를 차단한다.

`PyeongchangAttachmentDiscoveryProfile`은 공식 첨부 행의 `div.attachFile > a`만 읽는다. `goDownLoad`의 문자열 3개를 기존 선형 파서로 해석하여 공식 `eminwon.pc.go.kr/emwp/jsp/ofr/FileDown.jsp` GET을 만든다. 사이트가 제공한 함수에서 이 경로를 확인했으며 JavaScript·미리보기는 실행하지 않는다. 본문 전체의 일반 링크는 첨부가 아니다.

- 표시 이름과 함수 인자의 이름이 일치해야 하며 고정 업로드 디렉터리와 안전한 파일명을 검증한다.
- PDF/HWP/HWPX만 다운로드 대상이다. 미지원 형식은 metadata로 구분한다.
- 잘못된 개별 링크·상충·누락은 오류로 남기면서 정상 descriptor는 보존한다. 빈 첨부 영역과 영역 발견 실패는 다르다.
- 최대10파일, 상세1MiB, 동일 GET만 redirect 허용, 파일 역할 UNKNOWN을 유지한다.
- 기존 공통 엔진·프로필 지문을 수정하지 않고 평창 프로필만 추가한다. DB migration·API 계약·분류 규칙·추출기·운영 정책은 변경하지 않는다.

## 실제 관측

2026-09-30 03:19 KST 로컬 Java 수집 경로에서 제목 COMBINATION_MATCHED, 본문 AVAILABLE 324자 / ACCEPTED / TARGET_SUPPORT_CONFIRMED, 첨부 FOUND·complete=true를 확인했다. ACCEPTED는 후보 판정이며 관리자 확정이 아니다.

첨부 HWP1개의 다운로드 단계에서 `ATTACHMENT_DNS_TIMEOUT`이 발생했다. 바이너리·형식 검증 성공 근거가 없으므로 다운로드 성공으로 집계하지 않는다. 관측 결과는 `COLLECTION_ONLY_PARTIAL_NOT_APPROVED`다. 관측 태스크의 exit0은 오류를 기록하고 종료했다는 뜻이지 파일 다운로드 성공이 아니다. 동일 요청을 반복하지 않았다.

- 프로필: `LOCAL_PYEONGCHANG_BOARD_V1`.
- 지문: `51edcffa23c759ef14eca52db6129f62d9556ef895b6ede38393d25636643d0d`.
- 실제 보고서: `build/reports/attachment-regional-collection/PYEONGCHANG-41378.json`.
- 원본 정리 true, 운영 쓰기0, 추출·구간 분석·정책 QA·운영 worker/DB/API/UI 미검증.

## 지역 집계

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷의 활성223지역이며 이번에 운영을 다시 조회하지 않았다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 실제 파일 최소1개 다운로드 확인 | 129 | 129/223 (57.8%) |
| 다운로드 미확인 | 94 | 94 |
| 미등록 지역 | 59 | 58 |
| 등록 후 다운로드 미확인 | 35 | 36 |
| 첨부 발견·파일 오류 관측 지역 | 42 | 43 |
| 기존 3표본·전체 첨부 Gate 충족/잔여 | 16/207 | 16/207 |

지역165+기업마당1=166프로필, 카탈로그221공고/164대상이다. 신규 참조 expectation은 null이며 승인으로 간주하지 않는다. 기존252영수증·208공고와 다른 지역 상태, 이전 요청 원장은 보존한다.

## 삼척 조사와 요청 예산

삼척 LGS-000123의 등록 목록 `https://www.samcheok.go.kr/media/00084/00095.web`에서 공식 제목 검색(`stype=title`, `sstring=소상공인`)으로 `mgtNo=36177`, `amode=view`, `cd=01` 표본을 확보했다. 제목은 `2026년 소상공인 육성자금 융자추천 계획 공고(수정)`이다. `form#saeolGosiVO > .bbs1view1`과 `.attach1`의 HWP1개, `/DownloadEx.do` 경로까지 조사했다. 삼척 파일 요청·프로필 연결·다운로드 확인은 아직 하지 않았다. 다음 지역 연결에 사용할 근거다.

평창 조사4회(목록, 농촌지원 표본 상세, 제목 검색의 읽기 전용 POST, 소상공인 상세), 삼척 조사3회로 합계7회다. 조사 요청은 각15초/2MiB 이내, 인증·쿠키·자동 redirect 없이 TLS 검증을 유지했다. 관측 예산은 최대6요청/23MiB이며 실제 보고서의 본문 포함 예약 상한은4요청/2,252,800byte다. 조사 포함 예약 상한11회이고 계획 상한13회 이내다. 예약값을 실제 wire 요청 수로 단정하지 않는다.

조사 HTML7개 총1,021,655byte는 기록한 SHA256·길이와 정확한 경로를 대조한 뒤 삭제하고 부재를 재확인했다. 필요하면 공식 사이트에서 다시 조회해야 한다. 파일 원문·연락처·세션 값은 문서/원장에 저장하지 않는다.

## 검증 명령 / 결과

```powershell
# 보관 운영 대상 스냅샷과 이번 실제 HTML fixture 환경변수 활성화 후 실행
.\gradlew.bat :test --tests '*PyeongchangDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=PYEONGCHANG -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

최종 Gradle1분58초 성공, Java203/203·Node23/23·bootJar 통과다. 최초 표적102건 중1건은 빈 form의 POST 요청을 구성한 테스트가 기존 요청 객체의 유효성 제약에 걸렸고, 비어 있지 않은 합성 form으로 수정했다. 보안 검증을 완화하지 않았다. 실제 HTML fixture는 실행했고 보관 원본 정리 후 환경변수 없이 실행하면 해당 조건부1건은 생략된다.

원장 패치 생성 중 반복 구조의 짧은 문맥이 다른 지역에 잘못 매칭된 것을 전체 영수증 재현 검증이 발견했다. 지역 코드가 포함된 전체 항목으로 수정한 뒤 253영수증/209공고를 다시 재현했으며 다른 지역이 원래 상태와 일치함을 확인한다. 저장소 반영 전에 검출·복구했고 운영 데이터는 사용하지 않았다.

이번에는 변경 관련 표적 회귀를 실행했다. 전체 프로젝트 테스트·운영 DB·AWS·실제 운영 worker·텍스트 추출·브라우저 E2E를 통과했다고 주장하지 않는다. 브라우저 검증은 현재 요청에 명시되지 않아 사용자 정책상 생략했다. 운영 배포·정책 게시·ENFORCE·기존 데이터 적용은 실행하지 않는다.
