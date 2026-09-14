# 전체 Provider QA 대상 계약과 분할 실행 경계

## 현재 단계

2026-09-15 실제 운영 읽기 전용 조회: 활성 미삭제 지자체223개, 목록 파서41종이다. 이 수치는 첨부 엔진6종/등록 프로필12개 또는 정상 첨부 지원 수와 같지 않다. 기존 모든 대상의 분모를 유지하며 실제 연결·본문/첨부 영역·형식 적용성과 남은 기관을 확인해야 한다. [운영 근거](../deployment/attachment-runtime-baseline-2026-09-15.md)를 참조한다. 아래 초기 구현 시점 기록은 현재 전체 구현/운영 검증 완료를 뜻하지 않는다.

Gate3 진행, 전체 Gate0~8 최종 통과0이다. 이 문서는 ATT-001~062와 실제 전체 Provider/운영 검증을 대체하지 않는다.

- [x] 시스템 첨부 프로필12개의 기관/목록 parser 결합을 코드로 선언한다.
- [x] 기업마당·정부24·현재 활성 미삭제 지자체 전체를 빠짐없이 요구 목록에 포함한다.
- [x] 미구현·파서 불일치·중복 결합·단순 시스템 결합을 구분하고 읽기 전용 API로 조회한다.
- [x] 내부 snapshot schema6에 전체 요구 목록과 catalog 계획을 고정하고 MISSING 단계에는 요구 목록 지문/요약을 저장한다.
- [~] 고정 공고 전체 파일의 다운로드·격리 추출 실행 단위를 구현하고 로컬 대역 검증을 진행한다. 실제 Linux/실파일 실행은 미완료다.
- [ ] 공식 상세 주소 및 공개 비민감 표본·형식/변형별 기대 결과를 전체 대상에 등록한다.
- [ ] 전체 Provider 실파일 QA를 기존 상한 안에서 분할 실행하고 서버 소유 근거 원장으로 검증한다.
- [ ] 실제 Linux/PG·전체 정책 QA·운영 배포·브라우저 검증을 완료한다.

요구 목록·조회 기능과 고정 공고 실행 단위를 구현했다. 실제 전체 Provider 실행기·분할 작업 원장·정책 VERIFIED를 구현했다고 보지 않는다.

## DB/API 계약

기존 `AnnouncementAttachmentPolicyValidationMapper.selectTargetList`를 재사용한다. 활성·미삭제 전체 수집원을 조회하며 최근 목록 실패/과거 QA 상태를 이유로 추가 제외하지 않는다. 이 조회 결과를 현재 운영 DB에서 직접 얻지 않은 회차에는 운영 대상 수를 단정하지 않는다.

`GET /api/v2/admin/announcement-attachment-policies/{policyId}/provider-qa-plan?page=1&size=20`

- 활성 ADMIN/OPERATOR/APPROVER만 조회하며 일반 사용자·파트너·REVIEWER는 접근할 수 없다. Controller와 Service 양쪽에서 검사한다.
- 응답은 ApiResponse 안에 planHash, isPolicyManifestCurrent, summary, targets(PageResponse)를 넣는다. size1~100, 정수 페이지와 UUID 정책 ID, no-store를 유지한다.
- Linux 설치·규칙 실행·QA worker ON이 필요하지 않다. 조회 시 외부 HTTP/첨부 다운로드/QA 예약/정책 변경은0이다. POST/PUT 실행 경로는 없다.
- 원문 URL, parser configuration JSON, 임시 경로, API key는 응답에 넣지 않는다. 기관 시스템 코드·목록 parser 코드·정책에 사용하는 첨부 profile code/hash만 제공한다.
- planHash는 **구조적 요구 목록**의 지문이다. URL/원문/실파일 현재성·정책 전체 snapshotHash·실행 성공을 대신하지 않는다. 전체 정책 snapshot은 기존 configurationHash와 설치 지문도 별도로 포함한다.
- isPolicyManifestCurrent는 저장된 정책의 전체 프로필 manifest와 현재 등록 목록의 일치만 뜻한다. 실제 운영 정책 활성화나 QA 통과가 아니다.

요구 목록 조회 자체는 추가 migration이 없다. 후속 V79 원장과 schemaVersion6의 providerQaPlan/providerQaCatalog가 전체 요구 목록·catalog 계획을 고정한다. 이전 schema1~5 이력은 수정하지 않으며 현재 입력과 달라 재사용할 수 없다. 기존 `/api/v1`과 기존 v2 요청/응답 shape는 유지한다. catalog9건은 REFERENCE_ONLY/실행 기대값0이며 세부 구현·전체 잔여는 `announcement-attachment-provider-qa-catalog-2026-09-12.md`를 따른다.

## 대상·상태 정의

기업마당/정부24 두 전역 대상과 활성 지자체 N개의 합계 N+2개다. parser 이름이 같다는 이유로 다른 기관을 지원한다고 간주하지 않는다. 프로필의 source binding 선언이 없으면 전체 provider 지원으로 추론하지 않는다.

| 상태 | 의미 | 다음 필수 작업 |
|---|---|---|
| SYSTEM_BINDING_MATCHED | provider·기관 코드·목록 parser에 프로필 하나가 결합됨 | 공식 상세 URL/세부 설정·전체 공고/파일·형식별 실제 QA |
| PROFILE_MISSING | 해당 기관/provider 결합이 없음 | 첨부 프로필과 고정 공개 표본 구현 |
| LIST_PARSER_MISMATCH | 기관 프로필은 있으나 현재 목록 parser와 다름 | 시스템 설정/구현 대조, 새 검증 |
| PROFILE_AMBIGUOUS | 동일 기관/parser를 여러 프로필이 처리한다고 선언 | 시스템 등록 중복 해소 후 검증 |

대상 ID/기관 코드 중복, 잘못된 코드, 잘못된 hash, 같은 provider/profile 중복, 대상1000개 초과 등은 조용히 자르지 않고 실패한다. 조회 순서가 달라도 정렬된 요구 목록은 같다. 사용자가 parser/selector/임의 URL을 선택하거나 편집하는 기능은 없다.

minimumNormalNoticeCount는 QA 계획의 대상별 최소 정상 공고3건을 합산한 **요구량**이다. requiredFormats의 PDF/HWP/HWPX는 확인할 형식이며 해당 기관에 모든 형식이 실제 존재한다거나 현재 추출을 지원한다는 뜻이 아니다. 없는 형식/빈 첨부/redirect/미지원 다운로드는 확인 근거와 미확인 상태를 남겨야 하며 성공으로 채우면 안 된다.

summary의 isExecutionPlanComplete=false, isQaPassed=false, isSingleLeaseCoverageGuaranteed=false를 유지한다. 표본·실행 원장이 아직 없는데 대상 수와 결합 수만으로 이 값을 true로 바꾸지 않는다. currentHttpRequests는0이다. unboundProfileCount는 현재 대상과 기관 코드 후보 관계조차 없는 등록 수이며 parser mismatch와 별도다.

## 다음 실행 구조에 필요한 계약 — 미구현

출처별3건/형식별 파일·다중 첨부·실패 변형을 하나의 기존8분 lease에 모두 넣으면 정상 완료를 보장할 수 없다. 현재 코드의 한도를 늘리거나 적은 표본으로 전체 분모를 줄이는 것으로 해결하지 않는다.

1. DB-first로 **서버 소유 Provider QA 원장**과 고정 표본/파일 실행 항목을 설계한다. snapshot에 정책/규칙·전체 대상·profile/업무 코드·추출기·고정 표본 catalog 지문을 결합한다. 관리자 제출 성공 JSON·과거 단일 파일 CLI 결과를 원장에 가져오지 않는다.
2. 실행 가능한 작은 항목마다 소유 lease·취소·전역/기관 슬롯·요청/byte 상한·일시 원본 정리를 적용한다. 진행 항목의 lease를 무제한 연장하지 않으며, 완료 항목과 미완료 항목의 전체 분모를 고정한다.
3. 전체 요청량은 고정 표본/파일 계획에서 산정한다. 현재 최소 공고 수만 보고 파일 수·bytes·실행 시간을 꾸며내지 않는다. 기존 허용 상한보다 큰 운영 요청은 범위·영향을 제시하고 승인받는다.
4. 같은 source/표본 안의 모든 파일과 변형이 끝나야 그 표본을 완료할 수 있다. 일부 파일·기관 성공으로 전체 Provider 단계 PASSED를 만들지 않는다. UNKNOWN 역할, OCR/부분/미지원은 분리한다.
5. 원장의 최신 입력과 실제 실행 근거를 전체 검증한 후 기존 정책 validation-run에서 참조한다. 부모 validation 시각과 실제 Provider 실행 시각을 구분하고, 코드/정책/규칙/대상 변경·누락·취소·기간 경과 시 과거 근거를 거부한다. 이 재사용/유효기간·원자적 원장 완료 계약은 후속 DB/API 설계에서 확정해야 한다.
6. 정책 게시 transaction은 현재 전체 입력/근거를 재검증하며 source 수집·ENFORCE·기존 데이터 적용·운영 공고 활성화를 자동 수행하지 않는다.

## 고정 공고 실행 단위 — 로컬 구현, 운영 미연결

분할 원장이 호출할 `AttachmentProviderQaCaseExecutor`를 먼저 구현한다. HTTP endpoint나 scheduler에 직접 연결하지 않는다. 원장·전체 표본 catalog·게시 verifier 연계 전에는 운영 실행/전체 Provider PASSED를 제공하지 않는다.

코드: `qa/AttachmentProviderQaCase.java`, `qa/AttachmentProviderQaCaseExecutor.java`. 이 단계는 새 DB/API 계약을 만들지 않는다. 전체 정책 QA의 PROVIDER_PROFILES는 여전히 MISSING이다. 실행 단위의 `ExecutionControl`은 향후 DB 원장의 짧은 transaction으로 소유권 확인·공유 host/추출 임대·예산 예약을 수행해야 한다. 대역 control 또는 단위 결과를 운영 근거로 제출하는 경로는 제공하지 않는다.

- 입력은 서버에서 고정한 case ID, 기관/공고 연결, 제목·규칙, profile/runtime 지문, 발견 기대 상태와 **전체** 파일 locator/binary 지문·형식·추출 기대 상태·핵심 문구다. 관리자 URL/성공 JSON 업로드로 대체하지 않는다.
- 제목 선행 분류에서 제외되면 상세/첨부 요청과 임시 원본 생성을 하지 않는다. 예상하지 않은 제목 제외를 정상 파일 QA 성공으로 계산하지 않는다.
- 파일 목록/locator/형식/허용 여부가 고정 입력과 달라지면 새 파일을 내려받지 않는다. 파일별 binary 지문을 확인한 뒤에만 격리 추출한다. 공고에 파일이 여러 개면 전부 결과 행에 남기며 실패·취소 이후 미실행도 숨기지 않는다.
- production profile/download flow/pinned transport/signature validator/Linux extractor를 사용한다. 호출자가 소유 lease 확인, host/추출 permit, 요청/byte 예약을 제공해야 하며 이를 생략하는 운영 기본값은 없다.
- 단위 실행 hard cap은 최대 420초, 상세 1 MiB, 파일 20 MiB, 공고 전체 80 MiB, 요청 예약 44회, 파일 10개다. 호출자는 더 작은 한도를 고정할 수 있다. 개별 30초 작업 전 정리 여유를 확인하고 각 요청·byte 예약·추출 전후에 취소/만료를 확인한다. 임대 무제한 연장은 없다.
- 모든 텍스트·원본은 실행 중에만 취급하고 원본 정리 뒤 hash/count/고정 상태만 반환한다. 문서명/URL/본문/핵심 문구/예외 원문은 결과에 반환하지 않는다. OCR/부분/비지원의 기대 동작 일치는 완전 추출 지원과 별개다.
- 수용 검증: 전체 다중 파일, 예상하지 않은 추가/누락/중복, 형식/binary 변경, 핵심 문구 실패, 일부 실패 후 나머지, 취소/예산/시간 상한, title 제외 요청 0, runtime 변경, cleanup 실패, flow/permit 해제, 민감 원문 미반환. 실제 Linux/공식 파일/DB 원장 검증은 별도 필수다.

`SINGLE_FIXED_NOTICE_PROVIDER_QA` 결과의 PASSED는 **고정 표본의 기대 동작 일치**만 의미한다. 예를 들어 예상한 빈 영역·미지원 파일·OCR 상태의 정상 처리는 PASSED일 수 있으나 allTextComplete=false다. 파일별 실제 처리 상태와 미실행 분모를 보존하고 isPolicyQaPassed는 항상 false다. 원본 정리 실패·설치 runtime 변경·예상 문구 불일치·누락 파일은 PASSED가 아니다. requestReservations는 DNS/실제 HTTP 전에 확보한 요청 예산으로, 성공 응답 수와 같지 않다.

표본 제목은 고정 입력의 제목 선행 Gate이며 이번 실행기가 최신 목록을 재수집했다는 뜻이 아니다. 전체 catalog/원장은 최신 출처·제목 버전·규칙·업무 코드 지문과 유효기간을 결합해야 한다. 분류 전체 결정·본문/DB worker·게시·상시 수집 검증은 해당 통합 경로에서 별도로 수행한다.

## 검증

- 표적 프로필/요구 목록/예약 snapshot/Service/HTTP145건·독립 QA 패키징14건·bootJar:1분4초 성공.
- 실제 Spring 프로필 등록을 제한된 컨텍스트로 구성한 단위 검증은12프로필/11지자체 결합+기업마당을 확인했다. 정부24는 PROFILE_MISSING이며 전체13개 요구 대상 중12개가 시스템 결합됨을 확인했다. 이는 현재 운영223개 등을 조회한 결과가 아니다.
- HTTP 검증은 실제 보안 필터와 Controller를 사용하고 Service는 대역이다. 응답 wrapper/no-store/페이지·권한·읽기 전용 계약만 증명한다. 실제 DB 조회/운영 브라우저 증거가 아니다.
- 공통 interface/프로필 코드에 결합 선언을 추가하여 profile hash와 전체 업무 코드 지문이 변경됐다. 기존 실제 다운로드 기록을 새 artifact의 추출/운영 성공으로 재사용하지 않는다.
- 최종 전체 회귀/생략/자원/원격 권한 상태는 최신 진행 기록에 남긴다.
- 최종 전체 회귀는3분41초 성공, root1752통과/217생략·extractor25·QA14·Node130통과다. 독립191건은 INVENTORY_ONLY/실행0이며 실제 Provider/운영 검증은 이번 미실행이다.

### 실행 단위 추가 후 검증 — 2026-09-12 09:40

- `AttachmentProviderQaCaseExecutorTest` 최종42건 통과. actual profile flow·signature·임시 원본 저장/정리를 사용하되 HTTP/Linux extractor와 원장 control은 대역이다. PDF/HWP/HWPX signature 입력도 합성 bytes이며 실제 문서 추출 증거가 아니다.
- 표적 수정 검증42건25초 성공, 첫 전체 강제 회귀3분39초 성공. 정리 도중 취소 및 예상하지 않은 OCR 상태 기록을 보강한 최종 전체 회귀는3분14초 성공, root1794통과/217조건부 생략·extractor25·독립QA14·Node130통과다. 추출기/Node의 변경 없는 코드는 직전 실제 통과 결과를 사용했다.
- DB 원장·catalog·scheduler/정책 게시 연결은 미완료다. 새 실행 단위를 운영에서 실행하거나 현재 Provider 전체 QA를 통과시킨 것이 아니다. 실제 Linux/PG·공식 표본·운영 브라우저는 실행하지 않았다. 자세한 명령/산출물 SHA/권한/자원 종료는 최신 진행 기록을 따른다.
