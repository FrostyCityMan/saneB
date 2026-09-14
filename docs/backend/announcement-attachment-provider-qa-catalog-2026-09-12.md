# Provider QA 시스템 표본 catalog와 전체 실행 계획

## 범위와 상태

Gate3 진행이다. 전체 ATT001~062/Gate0~8을 유지한다. 서버가 배포한 고정 표본을 V79 실행 원장에 연결하기 전에 전체 대상·누락·파일 기대값·시간/요청/byte 계획을 검증한다. 관리자가 URL·parser·정답 JSON을 입력하는 기능은 만들지 않는다.

- [x] 고정 classpath JSON catalog와 엄격한 입력·전체 scope 대조를 구현했다.
- [x] 참조 표본/실행 기대값/누락·형식 coverage·분할 계획을 분리했다. 현재 참조9/실행 기대값0이다.
- [x] 기존 정책 QA snapshot schema6에 전체 catalog 지문·계획을 연결했다. 공개 API/운영 예약 연결은 아직 없다.
- [ ] 실제 공식 파일의 전체 목록·binary/추출 품질·기대 문구를 확인하여 catalog를 채운다.
- [ ] 관리자 예약·취소/조회 API, scheduler 및 정책 전체 QA verifier를 연결한다.
- [ ] 실제 Linux/PG/전체 Provider·배포·운영 브라우저를 검증한다.

## 계약

고정 리소스 `announcement-attachment/provider-qa-catalog-v1.json`만 읽는다. 경로/host/selector를 환경변수나 관리자 입력으로 대체하지 않는다. schema1·version·중복 case/기관 공고·원문 source 결합을 엄격히 검증한다. 잘못된 JSON/미지원 schema/unknown field/크기 초과는 빈 catalog로 대체하지 않는다.

각 notice에는 case code, 시스템 profile code, provider/기관/parser/source identity와 선택적 expectation을 둔다. expectation이 없는 항목은 REFERENCE_ONLY이며 실행 입력·정상 공고 coverage·예산에 포함하지 않는다. 기존 BBS 공개 표본9건은 이 상태로만 등록한다. 과거 다운로드 결과를 추출/현재성/정책 성공으로 가져오지 않는다.

expectation은 관측 시각·profile 지문·실제 제목·발견 상태/완전성·모든 파일 locator/binary/형식/품질/최소 텍스트·문구 및 최대420초/44회/80MiB를 고정한다. 관측은 미래 시각을 허용하지 않고7일 이내여야 한다. 7일은 입력 사용 기한이지 내용이 변경되지 않았다는 보장이 아니며, 실제 실행기가 파일 목록/binary/runtime 지문을 다시 검사한다. 최신 운영 source/title 버전의 결합은 예약 coordinator의 별도 필수 조건이다.

전체 scope의 기업마당·정부24·활성 지자체를 모두 남긴다. catalog의 표본이 현재 전체 대상에 없거나 다른 parser/profile로 바뀌면 제외/자동 교체하지 않고 오류 또는 준비 불가로 기록한다. 같은 기관의 동일 공고를 여러 정상 표본으로 세지 않는다. 참조/만료/profile 불일치/입력 오류/정상 기대 공고 부족/형식 누락을 각각 표시한다.

같은 기관/공고 identity 중복은 catalog 오류이고, 서로 다른 identity가 같은 실제 상세 URI로 해석되면 DUPLICATE_DETAIL로 기록해 실행/정상 coverage에 중복 산입하지 않는다. 임의 축소된 최소 정상 수/형식 목록은 거부한다. 현재 규칙의 제목 선행 판정과 TITLE_BLOCKED/일반 발견 기대값이 다르면 TITLE_EXPECTATION_CHANGED이며 네트워크를 호출하지 않는다.

정상 기대 공고는 FOUND+complete이고 모든 파일이 지원 형식/COMPLETE_TEXT 및 UNKNOWN이 아닌 역할 기대값을 가지며, 적어도 한 파일이 NOTICE/GUIDE인 공고다. NO_FILES·TITLE_BLOCKED·부분·OCR·비지원 혼합·UNKNOWN·양식/참고자료만 있는 음성 사례는 개별 실행 대상일 수 있으나 정상3공고 요구량을 채우지 않는다. 형식 coverage는 역할 기대값을 포함한 COMPLETE_TEXT 기대 파일의 추출 형식 범위이며 정상 후보 여부와 별개다. 이 값은 **계획의 기대값 coverage**이며 실제 통과/추출 지원 판정이 아니다.

### 2026-09-15 역할 기대값 연결

완전 추출 파일의 `roleExpectation`에 `ruleVersion`, `rulesHash`, `roleCode`, `reasonCode`, `textHash`, `blocksHash`, `assessmentHash`를 추가한다. 현재 `document-role-1.0.1` 규칙과 원문 없는 전체 Assessment의 canonical JSON SHA-256을 고정한다. Assessment에는 실제 block/code point 근거가 포함되므로 역할명만 일치해서는 통과하지 않는다. 기대값은 실제 파일을 검토하여 서버 catalog에 작성하며 실행 결과로 자동 승인·갱신하지 않는다.

기존 quality-only case/결과의 누락 필드는 직렬화에 추가하지 않아 과거 입력/결과 hash를 보존한다. 다만 새 catalog 실행은 COMPLETE_TEXT에 역할 기대값이 없으면 EXPECTATION_INVALID이며 실행 입력으로 만들지 않는다. PARTIAL/OCR/미지원 파일에 역할 성공 기대값을 붙일 수 없다. 규칙/텍스트/위치 지문이 바뀌면 관측·기대값을 다시 검토해야 한다.

CaseExecutor는 실제 추출 block의 자료형·전체 텍스트 범위·단일 위치를 검증한 후 worker와 같은 역할 판정기를 사용한다. 파일 결과에는 고정 `roleAssessmentHash`만 추가하여 기존32KiB 원장 한도와 원문 비노출을 유지한다. 실제 값이 고정 역할·사유·텍스트·block·Assessment 지문 중 하나라도 다르면 ROLE_EXPECTATION_CHANGED, 구조가 잘못되면 ROLE_EXTRACTION_STRUCTURE_INVALID다. 파일명/URL/본문/다른 첨부는 역할 입력이 아니다.

이 구현으로 공식 기대값이 생긴 것은 아니다. 참조9/실행 기대값0이며 출처별 실제 제공 형식/미확인 적용성, 공식 파일 검토·실행은 계속 잔여다. 모든 출처에 세 형식을 강제하는 기존 적용성 문제를 이번 변경으로 해결했다고 보고하지 않는다.

준비된 각 case는 현재 규칙/runtime을 별도로 결합한 입력 지문을 갖는다. 전체 실행 계획은 catalog와 전체 요구 목록의 지문, 모든 상태, 전체 정상/형식 누락을 보존한다. 분할은 순서가 고정된 실행 가능 항목만 대상으로 하며, 항목 최대시간+정리/DB 여유60초 합계를 각23시간 이내로 묶는다. V79 run의24시간을 늘리지 않으며 원장에 기록할 전체 요구 scope와 분할 case 분모는 구분한다. 최종 verifier는 모든 분할·누락을 다시 대조해야 한다.

catalog에서 실행 가능한 항목이 있다는 이유로 HTTP를 호출하거나 원장을 예약하지 않는다. 게시/ENFORCE/원문 수집/기존 데이터 적용도 하지 않는다. 전체 계획 complete와 실제 QA passed는 서로 다른 값이며 이 경로에서 isQaPassed는 항상 false다.

## 구현·검증 경계

`AttachmentProviderQaCatalog`가 classpath 리소스와 전체 계획을 관리한다. `AttachmentProviderQaCaseContract`로 기존 실행기의 syntax/파일 기대값/한도 검증을 공유하여 계획과 실행의 조건이 갈라지지 않게 했다. 이 분리는 기존 TITLE 제외·420초/44회/80MiB·파일10개·품질/문구 검증을 완화하지 않는다.

`AttachmentPolicyValidationSnapshotFactory` schema6의 providerQaCatalog에는 catalogVersion/catalogHash/scopeHash·전체 target/case metadata·분할별 case code/상한을 저장한다. URL·제목·텍스트·기대 문구나 Prepared.inputs는 snapshot/audit에 복사하지 않는다. 이전 schema1~5 이력을 수정하지 않으며 현재 snapshot과 달라 재사용하지 않는다. PROVIDER_PROFILES 단계는 계속 MISSING이고 최종 정책 verifier는 아직 이 catalog의 실제 실행 근거를 완성하지 못했다.

참조9건은 앞선 공식 다운로드 실측의 공고 식별자만 이관했다. 원본 재다운로드·추출 기대값 추정·과거 JSON 성공 업로드는 하지 않았다. 기존 normalizer와 현재 production BBS profile을 사용한 오프라인 검증은 정확한 source hash/host/게시판/기관/parser 결합을 확인한다. 현재 운영 대상이223개라는 과거 수치를 이번 실행 수로 간주하지 않는다.

신규 표적 테스트는 전체 요구 분모·참조와 실행의 분리·정상3공고·PDF/HWP/HWPX 기대 coverage·음성 품질·중복 source/상세·제목 규칙 변화·profile/관측 만료·파일 한도·350case 분할·안전한 metadata·실행기와 입력 hash 일치 및 실제 리소스9참조를 다룬다. 실제 HTTP/Linux/PG는 호출하지 않는다. 첫 컴파일 실패는 테스트 Target 생성자에 불필요한 인자를 넘긴 문제였으며 실제5필드 계약으로 수정했다. 최종 결과는 최신 진행 기록을 따른다.
