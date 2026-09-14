# Provider QA 관리자 계획·예약·조회·취소

## 범위와 Gate

Gate1/3 진행이다. V79 내부 원장과 catalog/snapshot6를 관리자 업무 계약에 연결한다. 전체 ATT001~062와 운영 브라우저까지의 목표를 유지한다. 현재 catalog 참조9/실행 기대값0이므로 실제 예약·외부 요청 성공으로 보고하지 않는다.

- [~] V80: 승인 계획/분할 metadata와 원자적 봉인 제약 구현. 실제 PostgreSQL 실행은 미검증.
- [~] ADMIN 정확한 분할/예산 확인·멱등 예약과 운영 읽기 3역할의 계획/원장/항목 페이지·ADMIN 취소 구현. HTTP/Service 표적 검증 통과, 실제 DB 연결은 미검증.
- [~] scheduler의 재구성/현재성 대조·case 실행 Service 연결 구현. 기본 OFF, 실제 Linux/Provider 실행은 미검증.
- [ ] 관리자 화면·전체 표본 기대값·정책 전체 verifier·실제 Linux/PG/운영 E2E.

## DB-first 계약

V80은 `announcement_attachment_provider_qa_run_plans`를 추가한다. V79 run에 1:1로 planHash, 분할 번호/전체 분할 수, catalog 전체 case 수/실행 가능한 case 수, 전체 기대값 coverage 여부, 분할 최대 시간(항목마다60초 여유 포함)을 고정한다. 원문의 제목/URL/내용/기대 문구는 저장하지 않는다.

V80 이후 새 run은 BUILDING 생성 transaction에서 계획 binding·모든 case·READY를 함께 commit해야 한다. 계획의 시간 합계는 실제 case의 maximum_seconds+60 합계와 같아야 하고23시간 이내다. 전체 분모>=실행 가능 분모>=이번 분할 case 수를 보존한다. 이전 run 이력은 변경하지 않는다. 계획 이력은 수정·삭제하지 않는다.

## HTTP 계약

기준 경로: `/api/v2/admin/announcement-attachment-policies/{policyId}/provider-qa-runs`.

- GET `/execution-plan?page=1&size=20`: 현재 검증된 설치/정책/규칙/catalog의 snapshot/plan 지문, 전체 대상·case·기대값 준비 수, 전체 준비 여부, 분할별 case code/요청/byte/시간 상한 페이지를 반환한다. 원장/HTTP 쓰기는 없다. 설치 검증이 불가능하면 구체적인409 사유를 반환한다.
- POST `/`: ADMIN이 조회 버전·snapshotHash/catalogHash/planHash·분할 번호·이번 공고 수/요청/byte/시간 상한·사유·범위/네트워크 확인을 제출한다. 전체 기대 coverage가 미완료면 별도 미완료 인지도 필수다. Idempotency-Key가 필요하다. 임의 URL/file/profile/source ID/성공 결과는 입력할 수 없다.
- GET `/`, GET `/{runId}`, GET `/{runId}/cases`: ADMIN/OPERATOR/APPROVER가 원장/항목 metadata를 조회한다. 목록은 pagination/no-store/ApiResponse/PageResponse를 사용한다. lease token, idempotency key, requestHash, 원문/추출문은 노출하지 않는다.
- PUT `/{runId}/cancellation`: 활성 ADMIN이 최신 run version·사유로 READY/RUNNING만 취소한다. 대기 항목은 CANCELLED로 보존하고 실행 중 항목은 소유자가 정리한다. OFF에서도 이력 조회·취소는 가능하다.

같은 키/같은 관리자·정책·요청의 재전송은 저장된 원장을 반환한다. 다른 입력/관리자의 키 재사용은409다. global queue→부모 run 또는 규칙→정책 순서의 짧은 잠금을 사용한다. runtime/code 확인·외부 HTTP는 예약 transaction 안에서 실행하지 않는다. 같은 정책의 새 예약은60초 간격이며 이미 저장된 같은 키 재조회는 이 제한/worker OFF보다 먼저 처리한다.

예약은 이 QA 분할의 최대 요청량/bytes만 승인한다. source 수집·게시 정책·ENFORCE·기존 데이터 적용·운영 공고 활성화를 승인하지 않는다. 전체 catalog 준비 또는 run COMPLETED를 실제 정책 QA PASSED로 바꾸지 않는다. 전체 계획과 모든 분할의 실제 근거를 판정할 verifier는 별도 필수다.

## 실행 재구성과 종료

별도 Provider QA 설정이 true일 때만 단일 소유 executor가 5초 간격으로 원장을 확인한다. 한 poll은 한 공고만 실행한다. queue→run 잠금 아래 취소·만료·종료를 정리하고, 실행 중인 case가 있으면 새 실행을 시작하지 않는다. 런타임 설치 검증과 실제 HTTP/추출은 쓰기 transaction 밖에서 수행한다.

현재 정책/규칙·snapshot/catalog/계획·애플리케이션 코드/runtime 지문·전체 분모·선택 분할 예산/시간과 정렬된 전체 case code/입력/profile/파일 수/상한을 기존 원장과 대조한다. 변경은 미실행 case만 INPUT_CHANGED 실패로 남기며 새 입력으로 바꿔 실행하지 않는다. V80 계획이 없는 과거 활성 원장도 새 실행 근거로 사용하지 않는다. 이미 실행 중인 소유자/자원은 직접 해제하지 않는다.

설치 재검증 불가는 PREPARATION_UNAVAILABLE이고 성공/입력 변경으로 추정하지 않는다. 다음 poll에서도 취소/24시간 만료 정리는 먼저 수행한다. 실제 ExecutionService는 claim·요청마다 DB 버전/소유권/예산을 다시 확인하고 같은 case를 재claim하지 않는다. 결과 PASSED/COMPLETED를 정책 전체 QA 완료나 운영 자동 활성화로 바꾸지 않는다.

스케줄러는 작업 중 중복 제출을 하지 않고 종료 시 자기 executor만 interrupt/shutdown한다. 반복되는 동일 상태/예외 원문은 로그에 누적하지 않는다. 원장 조회/취소는 실행 설정이 OFF여도 허용하지만 서버 역할 검증은 항상 유지한다.

## 검증 경계

Service·MockMvc·Mapper 바인딩·DDL 정적 시험과 실제 PostgreSQL 전용 계약 시험을 작성했다. 첫 컴파일 실패는 테스트의 페이지 이름(totalElements)을 기존 totalCount로 정정했다. 다음 표적 실패는 JSON long/정수 node 타입 차이를 실제 계획 변경으로 오인한 서비스 결함이었으며 동일 JSON 직렬화 후 값 비교로 수정했다. 실제 값/지문 변경 거부는 유지한다.

전체 회귀 수와 실제 실행/생략 여부는 최신 진행 기록을 따른다. 실제 Linux/DB·전체 공식 표본 기대값·관리자 UI·정책 전체 verifier·운영 배포/E2E는 필수 잔여이며 본 문서의 로컬 검증으로 대체하지 않는다.
