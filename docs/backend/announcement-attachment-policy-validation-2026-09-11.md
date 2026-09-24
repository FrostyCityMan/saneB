# 첨부 정책 검증·게시 실행 계약

## 2026-09-24 설치 runtime 계약 갱신

추출기1.0.4/`attachment-runtime-2`는 기존12개 입력에 검증된 PDF 문단·표 행 입력2개를 더한14개를 요구한다.
정책 실행과 저장 결과 verifier는 같은 suite/전체 case 순서·텍스트·scope·지문을 확인한다.
과거12개 성공 결과를 현재 정책 QA로 재사용하지 않으며 기존 저장 이력 자체는 변경하지 않는다.
원본 정리·파일30초·8분 lease/40초 잔여 검사는 유지한다. 지역별 실파일 검증이나 정책 게시 승인과 별개다.
아래12개 표본 및 초기 미실행 설명은 작성 당시 기록이며, 현재 버전 검증 결과는 장기 진행 기록을 따른다.

현재 상태: Gate 4 부분 구현, **Not ready**. 정책 초안 CRUD, 분류 정답 세트, 설치 런타임 실행기와 비동기 QA 예약·취소·단계별 DB 이력을 연결했다. 09-12 관리자 초안·QA·게시 영향 조회 화면을 추가했다. 실제 전체 profile·worker DB 증거가 없는 실행은 `INCOMPLETE`로 끝난다. Linux 실제 실행·전체 QA·정책 게시 실행·운영 적용·실제 운영 브라우저는 완료되지 않았다.

09-12 기관 identity/독립 실행 연결: 최신 QA snapshot schema6는 지자체 target publicCode·독립 QA artifact/code/runtime/전체 suite/case와 전체 Provider 요구 목록·catalog/분할 계획을 고정한다. 읽기 전용 provider-qa-plan은 전체 기관의 결합·미구현·불일치·중복 및 요구량을 제공하며 실제 QA 성공이 아니다. catalog의 공식 참조9건은 실행 기대값0이며 임의 성공으로 전환하지 않는다. 독립 QA 보고서 schema2는 실제 업무 코드/추출기 runtime의 전후 지문을 제공하지만 inventory는 runtimeHash=null·실행0이다. 부모 소유 namespace·취소·정리·lease 및 게시 verifier를 WORKER_DB_RECOVERY에 연결했다. PROVIDER_PROFILES는 MISSING이며 해당 목록의 hash/요약을 근거로 남긴다. 실제 Linux/PG 성공은 미확인이다. 상세는 `announcement-attachment-policy-qa-bridge-2026-09-12.md`, `announcement-attachment-provider-qa-scope-2026-09-12.md`, `announcement-attachment-provider-qa-catalog-2026-09-12.md`를 따른다.

09-12 07:08 코드 지문 보강: 수동 클래스 목록을 전체 main class/resource 빌드 목록과 실제 바이트 대조로 교체했다. QA 내부 입력은 schemaVersion2·installed.executionCodeHash를 사용하며 파일 검증은 transaction 밖에서 한다. 게시 직전 DB 잠금 안에서는 준비한 정책/scope/최신 QA/단계/snapshot과 최신 값을 재대조한다. 새 코드·리소스에서 과거 QA를 재사용하지 않는다. 이 수정은 추가 두 검증 단계의 MISSING을 해소하지 않으며 상세는 `announcement-attachment-code-fingerprint-2026-09-12.md`를 따른다.

## 검증 단계와 승인 경계

| 단계 | 현재 구현 | 증명하는 것 | 증명하지 않는 것 |
|---|---|---|---|
| 초안 저장 | 구현 | 허용 입력·설정 한도·버전·개정·권한 | QA 성공/운영 모드 변경 |
| CLASSIFICATION_GOLDEN | 구현 | 현재 DB 규칙으로 분류 정답 세트 AG-001~030 직접 실행 | 첨부 발견/다운로드/격리 추출/worker·DB·UI·운영 성공 |
| SYNTHETIC_INSTALLED_RUNTIME | QA worker에 12개 합성 표본 연결, Linux 실제 실행 미확인 | 설치된 격리 추출기의 파일별 품질·근거 위치·원본 정리와 실행 전후 지문 | 게시 승인·실사이트·전체 profile·worker/DB·운영 성공 |
| 설치 런타임·실파일·worker QA | 미완료 | 정확한 코드/규칙/profile/런타임에서 실제 작업과 실패 처리가 작동 | 기존 데이터 전체 적용 |
| validation-runs | 예약·고정 입력·claim·취소·부분 이력 구현, 전체 검증 실행 미완료 | 정책/규칙/profile/수집원/코드/설치 지문에 결합한 단계별 상태 | 누락된 실제 profile·worker DB 검증 또는 운영 게시 승인 |
| publication-impact | 읽기 전용 관측 API 구현(09-12) | 해당/전체 규칙의 보호 대상·고정 작업·최신 QA metadata/차단 사유 | scope 고정/QA 최종 재검증/게시 승인·실행 |
| publication | V77 서버 transaction/영수증 및 후속 게시 UI 연결, 전체 QA·추가 증거 verifier·PG/운영 미완료 | 전체 근거와 승인한 현재 범위로 ACTIVE 정책 교체하는 코드 경로 | 대역 테스트로 실제 게시 성공/기존 source 자동 일괄 적용/공고 자동 활성화 |

클라이언트의 `passed`, 사례 수, 규칙/실행 hash 또는 과거 단일 파일 CLI 결과를 전체 성공 근거로 받지 않는다. `classification-checks`는 분류만, `validation-runs`는 비동기 실행과 누락 증거를 구분한다. V77 `/publication`은 전체 QA 근거·현재 입력·정확한 승인 scope를 재검증하며, 현재 미연결 추가 verifier는409로 차단한다. 상세는 `announcement-attachment-policy-publication-2026-09-12.md`를 따른다.

## 분류 정답 세트

`AnnouncementAttachmentPolicyGoldenGate`는 DB에서 읽은 현재 규칙으로 TITLE/BODY base를 구성하고 첨부 분류기를 직접 실행한다. 성공 결과는 suite/엔진 버전, 규칙의 계산 snapshot hash와 실제 RuleSet 내용 hash, 30개 case ID, 결과 hash뿐이다. 고정 합성 표본의 본문/일치 원문은 응답이나 감사 metadata에 넣지 않는다.

| case | 기대 계약 |
|---|---|
| AG-001~002 | 본문 부족을 같은 NOTICE/GUIDE의 유효 조합으로 보완 |
| AG-003~005 | FORM/REFERENCE는 참고 근거, UNKNOWN은 검수 |
| AG-006~008 | 첨부 B는 검수, A/B 동시 근거 보존과 B 주사유, A도 검수 |
| AG-009~012 | 불확실 문맥/제외 문맥 검수, 파일·문단 간 AND 금지 |
| AG-013~016 | 부분 추출·OCR·다운로드 실패·미봉인 상태를 성공으로 숨기지 않음 |
| AG-017~020 | 실제 첨부 없음과 발견 실패 구분, 제목 A/본문 B 검수 보존 |
| AG-021~024 | 제목 제외·미충족, 다른 rule release, 잘못된 code point 근거 차단 |
| AG-025~028 | EXACT_TITLE 첨부 적용 금지, Provider 동일 판정, NO_FILES/FOUND 근거 모순 차단 |
| AG-029~030 | 첨부 없는 본문 부족은 검수, 이모지 포함 code point 위치 일치 |

AG 사례는 ATT-001~062 전체 요구를 대체하는 번호가 아니다. 특히 AG-026은 동일 텍스트의 Provider 독립성이지 실제 사이트별 수집 성공이 아니다. 분류가 성공해도 policyHash/런타임 hash/게시 시각/모드 적용은 바뀌지 않는다.

## 버전 고정과 이력

1. 활성 ADMIN·비밀번호 변경 완료·CSRF·UUID 멱등 키·조회 정책 버전·사유를 확인한다.
2. 짧은 REPEATABLE_READ transaction에서 DRAFT 정책과 DRAFT/ACTIVE 규칙을 읽는다. 게시 규칙의 저장 hash가 현재 행으로 계산한 hash와 다르면 중단한다.
3. DB transaction 밖에서 30개 정답 세트를 실행한다. 외부 네트워크·디스크 파일 처리·수집 job 생성은 없다.
4. 짧은 저장 transaction에서 key → 규칙 공유 잠금 → 정책 행 잠금 순서로 다시 읽고 정책/규칙 버전·내용 지문을 대조한다. 검증 도중 변경되면 409로 저장하지 않는다.
5. 성공한 `CLASSIFICATION_GOLDEN` 이력만 INSERT한다. 실패는 구체적인 case ID의 409이며 성공 이력으로 저장하지 않는다.
6. 같은 actor/정책/입력/key는 같은 이력을 반환한다. 이미 입력 버전이 바뀌었으면 이력은 보존하면서 `isCurrent=false`다. 다른 요청의 key 재사용은 409다.

`isCurrent`는 현재 **정책·규칙 입력 버전**과의 일치 여부다. 배포 artifact·설치 Linux·실사이트 또는 전체 QA 완료 상태가 아니다. 전체 validation에서 별도 실행 지문과 증거를 다시 대조해야 한다.

## 다음 구현의 필수 조건

- [~] 분류·합성 runtime 증거를 정책 rowVersion/입력 hash, 계산 규칙 hash, 코드·suite, 등록 profile code/hash, 활성 수집원 범위, 설치 지문에 결합했다. 전체 실파일·worker 증거와 최종 artifact 대조는 남아 있다.
- [~] 현재 두 단계는 고정 서버 QA 경로에서만 생성한다. 관리자 입력 JSON이나 임의 파일 경로를 성공 증거로 받지 않는다. 전체 profile/worker 검증 실행 경로는 남아 있다.
- [ ] QA DB는 테스트 소유 격리 자원으로 실행한다. 운영 업무 테이블에 통합 테스트를 실행하지 않는다. 별도 유료 staging 신설은 필수 조건이 아니다.
- [ ] 지원 형식·실제 모든 대상 profile·다중 첨부·실패/부분/OCR·worker 저장/복구에 대한 정확한 실행과 기대 결과를 검증한다. 몇 개 CLI 표본이나 합성 분류 결과만으로 게시하지 않는다.
- [ ] Linux 실행 파일 존재/hash뿐 아니라 실제 격리 실행과 원본 정리를 검증한다. Windows host fallback·OS 보안 해제는 허용하지 않는다.
- [ ] 검증 도중 입력 또는 설치 지문이 바뀌면 성공으로 확정하지 않는다. 누락·skip·오래된/다른 실행 증거를 거부한다.
- [~] 게시 전 현재 ACTIVE 정책, 해당/전체 규칙의 고정 수집/적용/원복 작업·기존 검수 binding/연결 공고·누적 계획·공고별 상한을 조회한다(`announcement-attachment-policy-publication-impact-2026-09-12.md`). 관측 지문은 정확한 source scope 고정이나 게시 CAS가 아니다. 신규 대상·전체 요청량의 명시적 범위, 실제 교체/재검증·복구 경계는 남는다.
- [ ] 게시 transaction에서 승인한 대상·건수·정책 버전·QA 지문과 현재 ACTIVE 규칙을 재검증한다. 키워드 규칙을 자동 게시하지 않는다.
- [ ] 게시만으로 기존 source를 일괄 적용하거나 공고를 활성화하지 않는다. 기존 데이터는 별도 고정 배치/승인/복구 경로를 사용한다.
- [~] 관리자 화면에서 초안·개정·QA 예약/취소·단계별 상태/실패 이유·게시 영향 관측을 연결했다(`announcement-attachment-policy-ui-2026-09-12.md`). 실제 전체 QA/게시 실행·운영 브라우저 검증은 남아 있다.

정책 게시·ENFORCE·기존 데이터 일괄 실행은 정확한 대상·건수·요청량 상한·효과·복구 방법을 제시하고 승인받은 뒤 수행한다. 이 문서와 코드 추가는 운영 실행 승인을 대신하지 않는다.

## 설치 런타임 실행기 — 2026-09-11 추가

`AttachmentRuntimeGate.selectValidatedResult()`는 외부 파일 경로나 성공 JSON을 받지 않는 내부 실행기다. 후속 구현에서 `validation-runs` 예약과 정책 QA scheduler/worker에 연결했다. 아래 고정 표본 결과는 전체 validation 또는 publication 성공이 아니며, 실제 전체 profile·worker/DB 실행과 관리자 UI는 필수 미완료다.

1. 빌드 시 별도 extractor `qaFixtures` source set이 12개의 고정 합성 binary를 생성한다. root 서버에는 입력 resource만 포함하고 PDFBox/POI·생성기 코드를 host 실행 classpath에 추가하지 않는다. 제공된 임의 문서나 외부 URL을 빌드에서 처리하지 않는다.
2. 실제 Linux 설치 identity를 먼저 읽는다. Windows, 격리 도구 부재, 잘못된/읽을 수 없는 runtime이면 원본 생성·추출 전에 실패한다. 같은 실행기 객체의 동시 실행은 거부한다. QA worker는 별도로 PostgreSQL 전역 추출 슬롯을 획득하며 수집 worker와 공유한다.
3. 각 입력은 기존 `AttachmentTemporaryStorage`의 소유/용량/잠금 계약으로 생성하고 기존 `IsolatedAttachmentExtractor`에 전달한다. 파일당 30초·512 MiB 주소 공간·128 MiB heap·빈 네트워크/파일시스템 namespace 제한을 완화하지 않는다. 네트워크 요청은 없다.
4. 품질·형식·정확한 text·code point 좌표·locator·scope 신뢰·실제/추정 page 구분을 대조한다. 품질 코드 하나나 문자 수만 맞으면 성공시키지 않는다. 입력 변조, 실패, 취소, 다른 파일 잔존 또는 원본 폴더 정리 실패는 성공 결과를 반환하지 않는다.
5. 전후 설치 identity와 suite 지문이 같은지 재검사한다. suite 지문은 실행/정리 코드와 fixture binary를 포함한다. 결과에는 run ID/시각·runtime/suite/result hash·case ID·입력/text hash·문자/블록 수·정리 여부만 포함한다. 원문·URL·서버 경로·내부 예외 원문은 넣지 않는다.
6. `attachmentRuntimeIntegrationTest`가 실제 설치 distribution으로 12개 전체를 실행한다. 일반 root test의 조건부 생략과 전용 task의 실제 실행을 구분하며, 전용 task는 OS/도구 부재를 성공이나 skip으로 돌리지 않는다.

| 사례 | 정확한 기대 결과 |
|---|---|
| AR-001 | 영문 text PDF, COMPLETE_TEXT, page:1, scopeReliable=false |
| AR-002 | 빈 PDF, OCR_REQUIRED, 근거 0개 |
| AR-003 | HWP 5.x 한글·이모지, COMPLETE_TEXT, section/paragraph와 code point 위치 |
| AR-004~005 | 암호 flag HWP=ENCRYPTED, 잘린 record=CORRUPT |
| AR-006 | HWPX 한글·이모지·서로 다른 표 셀 3개, 근거 범위 분리 |
| AR-007 | HWPX text+미해석 그림, PARTIAL_TEXT |
| AR-008~010 | HWPX DTD/entity=CORRUPT, ZIP 경로 이탈=CORRUPT, 압축 상한=LIMIT_EXCEEDED |
| AR-011~012 | HTML 응답=UNSUPPORTED, 손상 PDF=CORRUPT |

AR도 ATT 전체 번호를 대체하지 않는다. blank PDF는 스캔된 실제 이미지 문서 표본이 아니며 영문 PDF는 한글 PDF 검증이 아니다. 실제 네트워크 canary·OOM/timeout·비정상 종료·다중 첨부 worker 저장/복구·모든 대상 profile 실파일과 운영 브라우저는 여전히 별도 필수 검증이다. 빌드 parser 단위 테스트를 Windows에서 실행하는 것은 신뢰한 합성 입력의 테스트이며 운영 추출의 host fallback이 아니다.

## 비동기 정책 QA 예약·실행 — 2026-09-11 후속 구현

- API prefix: `/api/v2/admin/announcement-attachment-policies/{policyId}/validation-runs`. GET 목록/상세는 ADMIN·OPERATOR·APPROVER, POST 예약과 PUT `/{runId}/cancellation`은 활성 ADMIN만 허용한다. CSRF, 조회 버전, 사유, 예약 UUID 멱등 키를 서버에서 검증한다. 미정의 필드·URL·경로·profile·결과·실행 hash를 받지 않는다.
- 기본 설정 `SANEB_ANNOUNCEMENT_ATTACHMENT_POLICY_QA_ENABLED=false`. true일 때만 새 예약/내부 worker를 허용하며, 같은 키의 기존 예약은 비활성화 후에도 재실행 없이 조회한다. 테스트는 환경변수를 false로 고정한다. 운영 설정은 변경하지 않았다.
- 전역 예약/실행은 1개, 같은 정책은 60초 간격·24시간 3회다. 실패·취소도 요청 횟수에 포함한다. 현재 실행 범위는 고정 합성 표본만 사용하여 **외부 공고 HTTP 요청 0회**이며 정책 게시·수집 job·원문/공고 변경을 하지 않는다.
- 입력은 정책 버전/설정/mode, DRAFT·ACTIVE 규칙 버전/내용, 서버 등록 첨부 profile 목록/hash, 모든 활성·미삭제 지자체 수집원과 목록 profile 설정 지문, 기업마당·정부24 범위, 설치 runtime/suite, 실행 코드·mapper·V73 지문이다. 최근 수집 실패 수집원도 제외하지 않는다. URL/profile 설정 원문은 지문 계산 후 snapshot에 저장하지 않는다. 최대 1,000개 수집원·2,000개 규칙·1.5 MB 입력 상한을 넘으면 누락 저장하지 않고 예약을 거부한다.
- `inputVersionsCurrent`는 조회 시점의 **정책·규칙 DB 버전** 일치 여부다. Linux/profile의 현재성·필수 QA 전체 완료 또는 게시 가능 상태가 아니다. 전체 배포 artifact와 모든 의존성의 최종 재검증은 별도 남은 Gate다.
- PostgreSQL `PENDING → RUNNING` claim과 전역 EXTRACTION 슬롯 확보는 같은 짧은 transaction이다. 슬롯이 사용 중이면 claim을 rollback하여 PENDING을 유지한다. 분류와 parser 실행 중 DB transaction은 열어 두지 않는다.
- 실행 lease는 최대 8분이다. 매 파일 전 QA 실행/전역 슬롯/취소를 확인하고 양쪽 lease가 40초 이상 남아야 다음 파일을 시작한다. 기존 30초 파일 제한과 정리 여유를 보존한다. 취소는 현재 파일의 제한된 실행·정리가 끝난 뒤 반영하며 다음 파일은 시작하지 않는다.
- 성공한 AG-001~030과 AR-001~012는 각각 `PASSED` 단계로 저장한다. WORKER_DB_RECOVERY는 같은 설치·전체 case 목록의 실제 부모 소유 Linux/임시 DB 실행과 정리 결과를 검증해 PASSED/FAILED로 기록한다. `PROVIDER_PROFILES=ALL_PROFILE_REAL_FILE_QA_REQUIRED`는 아직 `MISSING`이다. 세 단계 성공만으로 `VERIFIED`를 반환하는 실행 경로는 없다.

| 실행 상태 | 의미·후속 행동 |
|---|---|
| PENDING | 예약됨. worker 비활성/추출 슬롯 사용 중이면 실행하지 않고 대기한다. 취소 가능 |
| RUNNING | 고정 입력으로 서버 실행 중. 입력 수정 시 최종 성공 확정 불가 |
| CANCEL_REQUESTED | 현재 파일 정리 후 취소 예정. 새 파일/단계 근거 저장 금지 |
| CANCELLED | 대기 취소 또는 실행 중 취소가 반영됨. 필요하면 새 키·현재 버전으로 다시 예약 |
| INCOMPLETE | 실행한 단계는 통과했으나 필수 실파일/worker DB 증거 부족. 게시 불가 |
| CONFLICT | 입력/설치 지문/필수 case 목록이 예약과 불일치. 최신 입력을 확인하고 새 QA 예약 |
| FAILED | QA 실행 실패 또는 lease 만료. 고정 오류 코드와 단계 이력을 확인 |
| VERIFIED | DB에는 네 필수 단계 PASSED·현재 정책/규칙 버전이 모두 있어야 허용. 현재 worker는 이 상태를 생성하지 않음 |

입력·요청 identity·종료 이력과 단계 증거는 수정/삭제할 수 없다. 취소로 lease token/만료/시작 시각을 교체할 수 없고, 오래된 소유자는 완료를 확정하지 못한다. 만료 실행은 다음 worker 회수에서 `FAILED/LEASE_EXPIRED`로 기록하며 자동 성공/재실행으로 처리하지 않는다. 슬롯 정리는 자신의 run/token만 대상으로 하여 재할당된 슬롯을 보존한다.

운영 대응은 새 예약 중지 → 실행 취소 → 현재 파일 정리·종료 상태 확인 → 원인/입력 수정 → 새 키/버전 예약 순서다. 프로세스가 사라졌으면 lease 만료 실패를 확인한다. 이력 삭제, 직접 상태 덮어쓰기, 오래된 증거를 새 run으로 복사하지 않는다. 이 부분 기능은 정책 게시·ENFORCE·배치 승인을 대신하지 않는다.
