# 첨부 부분 성공 보존과 지역 연결 우선 처리

## 2026-10-01 승인 후 로컬 구현

사용자가 수집 전용 검증 계약의 설계·구현을 승인했다. V86과 `COLLECTION_SAFETY_V1` / `COLLECTION_VERIFIED`로 기존 엄격 검증과 분리하는 로컬 변경을 진행했다. 운영 정책 게시·worker 활성화·ENFORCE·기존 데이터 적용·추가 배포는 승인 범위에 포함하지 않는다. [현재 설계·검증 체크리스트](attachment-collection-safety-contract-2026-10-01.md)를 따른다. 아래 결정 대기 기록은 승인 전 확인 이력이다.

## 2026-10-01 운영 수집 전환의 계약 차이 — 승인 전 기록

코드 설치 이후 상시 수집 진입 조건을 확인했다. **부분 성공 보존은 구현됐지만, 수집 전용 정책 게시 기준이 아직 엄격한 분류 적용 기준과 결합돼 있다.** worker 설정만 변경하면 된다고 보고하지 않는다.

- `AnnouncementAttachmentCollectionServiceImpl.selectPrepared`는 worker 활성, 동일 ACTIVE 규칙의 ACTIVE 정책, `COLLECT_ONLY` 또는 `ENFORCE`, 정확한 시스템 profile/실행 지문을 요구한다.
- `AnnouncementAttachmentPolicyPublicationServiceImpl`과 `AttachmentPolicyPublicationQaVerifier`는 모드와 관계없이 최신 VERIFIED 및 네 단계의 실제 근거를 요구한다. V77 DB 게시 trigger도 최신 VERIFIED/네 단계 PASSED를 검사하므로 서비스 분기만 바꾸거나 DB 상태를 직접 덮어써서는 안 된다.
- `AttachmentProviderQaEvidenceGate`는 전체 대상의 기대값 완비, 대상별 정상 공고3개, 전체 파일/정상 텍스트 및 형식 근거를 요구한다. `AttachmentProviderQaPlan`은 기업마당·정부24와 모든 활성 지자체를 포함한다. 다운로드 관측 성공210/223이나 정상 파일을 남기는 부분 성공은 이 계약의 전체 QA 통과가 아니다.
- 설치 시점 정책/작업/첨부0, worker 비활성이다. 승인된 설치는 이 계약을 변경하거나 정책을 게시하지 않았다. API key 부재인 국가2채널은 지역223개 분모와 구분한다.

제안(미승인·미구현): `COLLECT_ONLY`에 수집 안전성 중심의 별도 검증 계약을 두고, `ENFORCE`의 엄격한 분류/정상 기대값 검증은 유지한다. 수집 전용은 출처별 등록/현재 지문/접근 상태, SSRF·허용 도메인·파일형식/크기·시간·격리·저장 무결성·예산·취소/원본 정리를 검증한다. 실패/미지원/첨부 없음/미실행을 분리하며 실패 대상의 수집 결과를 성공으로 승격하지 않는다. 전수 정상 추출을 다른 대상의 수집 시작 조건으로 삼지 않는다. 대상 출처와 오류를 정책의 감사 가능한 범위로 고정하고 운영자 파서 선택이나 임의 URL 입력은 허용하지 않는다.

영향: DB-first additive migration과 새 검증 계약/버전·게시 영수증, `/api/v2` 정책/영향 응답, 관리자 수집 가능/분류 적용 가능 상태, 회귀·실제 DB 검증이 필요하다. V1/기존 migration과 과거 VERIFIED의 의미는 보존한다. `COLLECT_ONLY` 자료는 최종 확정·자동 활성화·기존 데이터 적용으로 연결하지 않는다. HWP 고도화는 계속 보류한다.

계약 변경 설계·구현 승인이 필요하며, 그 승인 자체를 운영 정책 게시/worker 활성화/ENFORCE/기존 데이터 적용 승인으로 해석하지 않는다. 이 결정을 받기 전에는 기존 게시 Gate를 우회하지 않는다.

이번 확인: 보관963영수증/293표본 재현 성공, 등록223/223·실파일210/223·미확보13·부분 오류가 있는33곳·엄격 수집16/223. 외부 요청/운영 쓰기0이다. 로컬 게시 verifier10/게시 service22/수집 service25=57시험 실패·생략0, Gradle24초 성공. 이 시험은 현재 차단 계약의 회귀 검증이며 새 수집 전용 계약 구현 증거가 아니다.

## 2026-10-01 Linux 근거 회수·다음 공고 진행 검증 보완

SHA `116b9c5c7ca1114f124800c7fad7f88c2c87d4c4`의 [Linux36843364966](https://github.com/FrostyCityMan/saneB/actions/runs/36843364966)은 success다. artifact `11153295953`(487,237byte)의 전용 XML에서 실제 임시 DB worker12개·runtime6개·job210개·migration/backfill18개·Flyway3개·정책 부모2개가 실패/생략0임을 확인했다. 원래 부분 성공 즉시 저장, 정상 역할 보존, 잘못된 격리 응답의 파일 단위 실패와 다음 추출 복구가 포함된다. 기본 test의 조건부 생략과 전용 suite 실행을 구분하며 중복 합산하지 않는다. 실제 사이트 HTTP와 운영 상시 worker 성공의 증거는 아니다.

검토 결과 `transientFileFailureSealsPartialResultsImmediatelyAndLeavesNextJobUnblocked`는 PARTIAL_FAILED 보존과 후속 IDLE만 검사했다. 이를 다음 공고의 실제 처리가 입증된 것으로 해석하지 않는다. 같은 시험에 별도 source의 작업 예약→실제 격리 HWPX 추출→SEALED/다운로드 성공/COMPLETE_TEXT→job SUCCEEDED를 추가했다. 첫 공고의 봉인 집합·성공/실패 파일·PARTIAL_FAILED 및 두 공고의 기존 제목/본문 평가가 그대로인지, 공고 link가 자동 생성되지 않는지, 자원 lease/임시 원본이 정리되는지도 검사한다. 합성 HTTP와 임시 DB이며 외부 사이트·운영 쓰기는 없다.

로컬 `gradlew.bat --no-daemon --max-workers=1 :compileTestJava :test --tests '*AnnouncementAttachmentWorkerServiceTest' --tests '*AttachmentContractWorkflowTest' :bootJar`는31초 성공, worker46+workflow21=67개 실패/생략0이다. 보강한 Linux 전용 통합 시험은 Windows에서 컴파일만 확인했으며 다음 QA 브랜치 CI의 정확한 SHA 결과를 확인해야 한다. HWP 파서 개선·응용 동작·migration·정책은 이번 보완에서 변경하지 않았다.

현황은 로컬 연결223/223·실파일210/223·미확보13이다. 잔여 오류를 전체 수집의 중단 조건으로 바꾸지 않되, 정책 게시/ENFORCE/운영 worker 활성화는 별도 승인과 검증 대상이다. 아래 초기27지역 및188미등록 수치는 당시 기준선이다.

## 승인·목표

2026-09-29 사용자 지시: 파싱·수집 실패는 별도의 오류로 남기고 수집 가능한 자료부터 처리한다. HWP 추출기 1.0.16 추가 개선은 계속 보류한다. 모든 지역에서 첨부를 연결하는 작업이 개별 오류 복구나 지역별 세 표본 확보 때문에 정지하지 않도록 한다.

## 변경 계약

1. 일반 `COLLECT`: 한 파일의 HTTP 5xx·timeout 때문에 성공 결과 저장을 전체 재시도 이후로 미루지 않는다. 성공 파일과 실패 파일을 같은 봉인 집합에 즉시 저장하고 기존 평가 계층이 `PARTIAL_FAILED`/검수 필요 여부를 결정한다. 다운로드·추출 근거와 오류는 기존 DB/API 필드를 그대로 사용한다.
2. 파싱: 발견 결과가 부분 실패라도 시스템 프로필에서 검증한 descriptor는 계속 처리한다. 발견 오류는 집합 `discoveryStatus`/`warningCodes`, 다운로드 오류는 파일 `downloadStatus`/`failureCode`, 텍스트 추출 오류는 `extraction.quality`로 구분한다. 추출 파싱 예외나 시간 초과가 이미 성공한 다운로드를 실패로 바꾸지 않는다.
3. 상세 자체를 받지 못해 파일을 발견하지 못한 경우 기존 최대 3회 재시도를 유지한다. 명시적 `RETRY_FILES` 작업도 선택 범위·기존 재시도 상한을 유지한다. 일반 수집 후 실패 파일 재처리는 이 별도 흐름이며 이번 변경이 무제한 자동 재처리를 만들지 않는다.
4. lease·예산·정책 fence·원본 정리·DB 저장 무결성 실패는 계속 중단/보류한다. checkpoint 기반 재시작도 유지한다. 오류 무시는 보안·데이터 무결성 검사 무시를 뜻하지 않는다.
5. 수집 전용 관측은 `COLLECTION_ONLY_PARTIAL_NOT_APPROVED`와 성공·실패·미지원·미실행 건수를 기록한다. 부분 발견도 확인한 파일부터 내려받는다. 명령 성공은 관측 실행 성공이지 전체 첨부·정책 QA 완료가 아니다. 제목 일치·네트워크·저장 등 관측 자체의 오류는 여전히 실패로 보고하고 다음 표본은 독립 실행한다.
6. `sanebBbsObservationGroup`에 쉼표로 최대 8개 지역 그룹을 명시할 수 있다. 선택 단계에서 잘못된 그룹을 검사하고 중복 공고를 제거하며 한 JVM에서 순차 실행한다. 기존 표본별 요청/용량 상한과 누적 지역별 예산은 변경하지 않는다. 자동 전체 지역 실행이나 이미 소진한 예산 재설정은 없다.
7. 고정 DRAFT seed를 읽기 위해 매 표본마다 임시 PostgreSQL 생성과 전체 migration을 반복하던 검증 경로를 수정했다. 같은 JVM에서 한 번 읽은 불변 규칙/대상 목록만 재사용한다. 최초 읽기 직후 DB는 닫고 실행 간 캐시·운영 규칙 캐시는 만들지 않는다.

## 진척을 분리하는 기준

`attachment-collection-availability.mjs`는 기존 엄격 대장을 대체하지 않는 별도 집계다. 제목 적격·상세 식별·현재 프로필 hash·signature·원본 정리가 확인된 실제 다운로드가 최소 하나 있으면 **다운로드 관측 지역**으로 계산한다. 부분 실패가 있어도 해당 성공은 버리지 않는다.

- 2026-09-28 운영 대상 스냅샷: 활성 223지역.
- 보관된 98영수증/최신 75공고 기준 실제 다운로드 관측: **27지역**, 미관측 **196지역**.
- 로컬 프로필 연결: 35지역. 미등록 188지역, 연결됐지만 실제 다운로드 미관측 8지역.
- 기존 세 표본·전체 파일 기준: 16충족/207잔여. 이 수치를 27충족으로 덮어쓰지 않는다.
- 현재 프로필에 연결된 관측 중 오류가 남은 지역: 5지역. 미등록/미관측 지역은 오류 관측이 없을 수 있으므로 이 5가 전체 blocker 수는 아니다.
- 위 수치는 신규 다운로드 증가가 아니라 같은 보관 근거의 재분류다. 신규 외부 요청·운영 DB 변경은 0이다.

## 이후 실행 순서

1. 미등록 188지역을 목록 parser/공식 상세 구조별로 묶고 재사용 가능한 프로필부터 연결한다.
2. 지역별 첫 정상 첨부 다운로드를 우선 확보한다. 세 표본 확보를 다른 지역 진입 조건으로 쓰지 않는다.
3. 파싱·접근·파일 오류는 별도 큐/보고서로 남기고 다음 지역으로 이동한다. 원인 변화 없는 실패 URL을 반복 요청하지 않는다.
4. 전체 연결 확대 후 오류 복구·다표본 검증·HWP 추출 보완·운영 E2E를 별도 Gate로 수행한다.

## 검증·한계

- [x] 일반 수집 성공/일시 파일 오류 동시 저장, 부분 발견 후 알려진 파일 수집, 추출 파싱 오류 후 다음 파일 처리 회귀 테스트.
- [x] lease 보류 후 checkpoint 재사용 및 명시적 실패 파일 재시도 계약 유지 테스트.
- [x] 수집 전용 부분 상태/성공 수/발견 미완료 구분 및 명시적 다지역 순차 선택 테스트.
- [x] 기존 98영수증 재이관에서 기존 대장과 일치 확인. 외부 요청 없음.
- [ ] 실제 Linux 격리 추출기+임시 DB/API worker 통합 테스트는 이번 로컬 Windows 실행에서 미수행. 해당 테스트 사양은 새 부분 저장 정책에 맞춰 수정했고 컴파일 확인 대상이다.
- [ ] 운영 배포·상시 수집 실행·신규 지역 실파일 조회는 별도 후속 작업이다.
- [ ] 브라우저 검증은 현재 요청에 명시되지 않아 사용자 정책에 따라 생략한다.

스키마/migration, `/api/v1` 계약, 자동 활성화 정책, HWP 추출기 코드는 변경하지 않는다. 부분 성공 자료를 최종 확정 공고나 전체 텍스트 분석 완료로 승격하지 않는다.

### 재현 명령

```powershell
.\gradlew.bat :test --tests "com.saneb.domain.announcementattachment.*" :bootJar --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
```

첫 확대 검사에서 과거 프로필/참조 수를 고정한 테스트 5개가 실패했다. 현재 등록 구성(20·23·33개)과 catalog 90참조를 확인해 기대값을 갱신했다. 오류 상태·부분 성공·보안 검사는 삭제하지 않았다.

최종 결과: 첨부 도메인 JUnit XML 총 2,138건 중 **2,098 통과/40 조건부 생략/0 실패**, `bootJar` 성공. Node **23/23** 통과, 98영수증/75최신 표본 재현 일치. 수정된 Linux worker DB/API 통합 검사는 컴파일만 확인했고 실행 성공 수에 포함하지 않는다. 확대 Gradle 실행은 이번 비교에서 5분32초→2분28초였다(프로필 기대값 수정과 JVM 내 seed 재사용 반영, 일반 성능 보장 수치는 아님). 사용한 단발 Node와 Gradle 프로세스는 종료했다.
