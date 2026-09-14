# Provider QA 분할 실행 원장

## 범위와 Gate

Gate1/3 진행, 전체 Gate0~8 최종 통과0이다. 고정 공고 실행 단위를 기존 운영 자원 제한과 같은 DB 원장에 연결한다. 전체 Provider catalog/관리자 예약 API/정책 검증 연결·실제 Linux/PG/운영 검증은 별도 잔여 작업이다.

- [~] V79 additive migration 작성: 불변 실행 범위와 공고별 실행 항목, 원자적 준비·claim·예산·취소·완료 계약. 실제 PG migration 미실행.
- [x] DAO/Mapper 및 내부 실행 Service에서 실제 CaseExecutor와 연결. Controller/scheduler에는 미연결.
- [~] 공유 다운로드2/호스트1/추출1 슬롯 및 만료·다른 소유자 보호 구현. 실제 DB 경합은 미검증.
- [x] 내부 Service35건·실행기42건·Mapper35건·migration 정적90건 표적 통과. 실제 PG14사례 작성, 실행은 미완료.
- [ ] 전체 공개 표본 catalog·관리자 예약/조회·정책 전체 QA 연결.

## 저장 계약

`announcement_attachment_provider_qa_runs`는 정책/규칙 버전, snapshot/catalog/업무 코드/runtime 지문, 전체 요구 범위 metadata, 고정 case 수, 전체 요청/byte 승인 상한, 누적 사용량, 요청자/멱등 키와 상태를 저장한다. `announcement_attachment_provider_qa_cases`는 고정 순번/case code, 입력/profile 지문, 예상 파일 수, 공고별 요청/byte/시간 한도, 실행 소유 token/만료, 결과 metadata를 저장한다.

case 제목·원문 URL·실행 원문·예상 문구를 원장에 복사하지 않는다. 시스템 catalog에서 동일한 입력을 재구성하고 input hash가 일치해야 실행한다. 보고서에는 hash/count/고정 상태만 들어간다. 관리자 성공 JSON/임의 URL 업로드 경로는 제공하지 않는다.

run 생성과 모든 case 물질화·READY 전환은 같은 DB transaction으로 끝내야 한다. 예상 case 개수/순번/누적 승인 한도가 맞지 않으면 봉인할 수 없다. READY 이후 입력/범위/한도는 변경하지 않는다. 전체 요구 범위는 case 성공 여부와 무관하게 보존한다.

run 상태는 BUILDING→READY→RUNNING→COMPLETED/FAILED, 취소는 CANCEL_REQUESTED→CANCELLED다. **COMPLETED는 예약한 case들의 실행 기대값 일치일 뿐 전체 Provider 또는 정책 PASSED가 아니다.** 요구 대상별 정상3공고/형식·변형 coverage와 모든 실제 실행 근거를 별도 verifier가 확인하기 전 PROVIDER_PROFILES MISSING을 유지한다.

각 case는 PENDING→RUNNING→PASSED/FAILED/CANCELLED다. 한 번 claim한 소유권과 최대8분 만료를 연장/교체하지 않는다. 만료 또는 실패한 case를 같은 원장에서 자동 재실행하지 않는다. 재시도는 새로운 고정 계획/요청량 승인에 포함한다. 늦게 돌아온 결과는 현재화하지 않는다.

## 자원과 실행

- 기존 `announcement_attachment_resource_leases`에 Provider QA case 소유자 두 컬럼을 additive로 추가한다. 일반 job/정책 QA/Provider QA 중 정확히 하나만 소유한다.
- 기존 다운로드 GLOBAL1~2, HOST1, EXTRACTION1 슬롯을 같이 사용한다. 별도 풀이 없으며 살아 있는 소유권을 덮어쓰지 않는다. 만료 슬롯 인수 시 기존 두 Mapper도 새 소유자 컬럼을 명시적으로 비운다.
- HTTP 한 단계/추출 최대30초보다 긴40초 permit을 얻는다. 같은 case lease가 최소40초 이상 남아야 새 작업을 시작한다. case의 실제 실행은 기존420초/파일10개/44요청/80MiB hard cap을 유지한다.
- 소유권/정책·규칙 버전/취소를 각 요청·byte 예약 때 확인한다. run과 case 예산은 한 transaction에서 함께 증가하고 환급하지 않는다. 다운로드는 GLOBAL+HOST가 모두 확보되어야 시작한다.
- 실행 Service는 실제 HTTP/추출 동안 DB transaction을 유지하지 않는다. 종료 시 원본 정리·결과 입력 지문·예산 대조 후 소유 token으로만 결과를 확정하고 자기 permit만 반환한다.
- 후속 V80 관리 API/스케줄러 연결은 `announcement-attachment-provider-qa-management-2026-09-12.md`를 따른다. 내부 실행 설정 기본값은 false다. 이 문서/단위 테스트로 운영 ON 또는 정책 게시를 대신하지 않는다.
- 이미 다른 항목이 RUNNING이면 새 claim은0건으로 끝낸다. 전역 queue lock과 DB unique constraint를 같이 유지하며 실행 중 항목을 재claim하지 않는다. 공유 자원 부족은 실행기 RESOURCE_BUSY 등 실패 상태로 남기며 같은 항목을 무제한 재시도하지 않는다.

## 구현 파일과 로컬 검증

- `V79__add_provider_qa_execution_ledger.sql`, `AttachmentProviderQaRows`, `AnnouncementAttachmentProviderQaDao`, `AnnouncementAttachmentProviderQaMapper.xml`, 내부 ExecutionService/ServiceImpl을 추가했다. 기존 두 자원 Mapper는 만료 슬롯 인수 시 새 소유자 필드를 비우도록만 확장했다.
- 내부 Service 검증35건은 DAO/실행기 대역이다. OFF·입력/코드 변경·재claim 금지·짧은 transaction·다운로드/호스트 원자적 확보·추출 공유·예산 거부·취소/만료·결과 지문/사용량/원본 정리·실패 시 자기 자원 반환을 확인한다. 표적 전체202건 통과, PG185건은 조건부 생략이다.
- 첫 표적 테스트6건 실패는 transaction 내부 호출을 검증하는 Mockito answer가 대역 재설정 시 먼저 실행된 fixture 문제였다. `doReturn/doAnswer` 재설정으로 수정했으며 실행 시 transaction 검증은 유지했다. 수정 후 표적 명령은32초에 성공했다.
- PostgreSQL14사례를 기존 소유 loopback DB suite에 추가했다. 원자적 생성/예산 합계·불변 이력·전체 분모·독점 claim/경합·예산 양쪽 누적·공유 job/정책 QA 자원·다른 owner 보호·취소·실제 DB 시각 만료·입력 변경·완료와 정책 게시 분리를 다룬다. 환경 조건으로 생략됐으며 SQL 정적/대역 테스트를 실제 DB 성공으로 대체하지 않는다.
- `AnnouncementAttachmentMigrationTest`의 V78→V79/빈DB 검증과 기존 checksum 보존 검증을 확장했다. 실제 Linux/PG, 전체 공개 표본 및 정책 verifier, 운영 배포/브라우저는 미완료다. 전체 회귀와 산출물·자원 결과는 최신 진행 기록에 남긴다.

## 필수 검증

불완전한 준비 transaction 거부, 범위/예산 불변, 중복 claim·다른 token·늦은 응답 거부, 실제 공유 슬롯 경합/반환, run/case 예산 원자성, 부분 실패와 미실행 분모, 취소·만료, 원본 정리 이전 성공 금지, 정상 내부 Service→CaseExecutor→DAO 흐름을 확인한다. Windows의 Linux/PG 차단은 우회하지 않으며 실제 SQL 검증 미실행을 성공으로 표현하지 않는다.
