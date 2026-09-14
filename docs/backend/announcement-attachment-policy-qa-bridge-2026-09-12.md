# 정책 QA와 독립 worker DB 검증의 연결 계약

## 현재 단계

Gate 3 진행 중, 전체 Gate0~8 최종 통과0이다. 이 문서는 기존 전체 목표와 ATT-001~062를 대체하지 않는다.

- [x] 정책 snapshot에 수집원의 시스템 기관 코드 publicCode를 고정하고 누락/중복을 거부한다.
- [x] 독립 QA 프로세스에서 전체 업무 코드와 실제 Linux 추출기 지문을 실행 전후 검증하여 결과에 넣는다.
- [x] 실제 패키지의 별도 JVM inventory가 현재 업무 코드와 일치하되 runtime/실행 성공을 주장하지 않는지 검사한다.
- [x] 부모 소유의 Linux 프로세스·취소·출력 상한·namespace 종료·임시 DB 정리 코드를 구현했다. 실제 Linux 검증은 아래 미완료 항목이다.
- [x] 최신 schema5 예약 입력에 독립 QA 산출물·전체 suite/case·업무 코드·추출기 및 전체 Provider 요구 목록을 고정한다.
- [x] WORKER_DB_RECOVERY 실행과 게시 evidence verifier를 연결했다. DB 잠금 밖에서 실행하고 성공·정리·전체 목록 일치 후에만 PASSED를 저장한다.
- [ ] PROVIDER_PROFILES의 전체 대상 매핑·실파일 실행·게시 검증을 연결한다.
- [ ] 실제 Linux/PG, 정책 전체 QA, 운영 배포·브라우저를 검증한다.

현재 ValidationService의 PROVIDER_PROFILES는 여전히 MISSING이다. WORKER_DB_RECOVERY는 실제 실행 결과에 따라 PASSED/FAILED로 저장한다. 따라서 전체 VERIFIED/정책 게시 성공 경로는 아직 열리지 않으며 ENFORCE·기존 데이터는 변경하지 않았다. 성공 JSON 업로드 API나 빈 실행기를 등록하지 않았다.

후속 schema5/provider-qa-plan 조회는 `announcement-attachment-provider-qa-scope-2026-09-12.md`를 따른다. 아래 schema4는 DB 실행 연결 당시 기록이며 과거1~4 입력은 최신 schema5의 QA로 재사용하지 않는다.

## 09-12 실제 부모 실행 연결

- `AttachmentWorkerDbQaProcess`: shell을 호출하지 않고 부모가 prlimit/bwrap 자식과 전용 `/tmp/saneb-policy-db-qa-*` 작업 경로를 소유한다. 환경변수를 비우고 private PID/IPC/network·읽기 전용 QA 하위 경로·loopback 임시 DB만 허용한다. Windows/root/격리 도구 부재는 차단한다.
- 전역 JVM 실행 슬롯1개, 기존 2GiB address space·CPU480초·파일128MiB 등 상한을 유지한다. 실제 정책 실행 기한은 기존 lease 만료보다60초 이르다. inventory 재검증 최대30초·자식 정리·짧은 완료 transaction 시간을 남기며 lease를 연장하지 않는다. SQL의40초 안전 여유와 취소 상태도 반복 확인한다.
- stdout은 별도 가상 스레드로 최대1MiB까지 읽고 초과 즉시 종료한다. stderr/원문 예외를 보관하지 않으며 중복 필드·뒤따르는 JSON·비object 출력·비정상 종료는 거부한다. 취소/시간 초과/중단 시 소유 namespace를 종료한 후 임시 파일/DB를 삭제한다. 정리가 끝나기 전에는 originalRemoved=true를 반환하지 않는다.
- `AttachmentWorkerDbQaGate`: 시작/종료 시각과 정책 run/snapshot을 결합한다. 네 suite·전체 case ID hash의 정확한 집합, 실행/실패/생략/미실행 수, child artifact/code/runtime 지문을 검증한다. 전체 근거32KiB 초과도 거부하며 일부 결과를 잘라서 성공으로 저장하지 않는다.
- 저장 지문은 JSON object를 정렬 가능한 Map으로 정규화하여 PostgreSQL JSONB의 field 재정렬과 무관하게 계산한다. 게시 시 실제 단계의 createdAt을 검증기에 전달하고 자식/부모의 완료 시각이 저장보다 뒤인 증거를 거부한다.
- QA 입력 내부 schemaVersion4의 installed.workerDbQa는 artifactHash, executionCodeHash, extractorRuntimeHash, suites별 전체 수, 정렬한 전체 caseIds를 저장한다. schema1~3 이력은 변경하지 않으며 새로운 입력과 불일치하므로 재사용할 수 없다. DB migration/API 요청·응답 shape는 변경하지 않는다.
- 설치 경로는 `SANEB_ANNOUNCEMENT_ATTACHMENT_CONTRACT_QA_ROOT`(기본 `/opt/saneb/attachment-contract-qa`)로 외부화한다. 같은 웹 코드/추출기 산출물이 없으면 QA 예약부터 거부한다. 기존 worker/QA 기본 false는 유지한다.
- `attachmentPolicyDbQaIntegrationTest`는 실제 Linux 부모→독립 PG 전체 suite 실행·게시 verifier 및 취소/임시 DB 정리2사례다. 전용 task와 CI 필수 XML 판정에 연결했다. 이 PC에서는 Linux Gate가 없어 실행하지 않았으며 단위 mock/명령 구성 검사는 그 성공을 대신하지 않는다.

## 발견한 결함과 현재 수정

### 정책 대상 identity

실제 첨부 profile은 provider·publicCode·목록 parser와 원문 URL을 함께 확인한다. 기존 정책 snapshot은 source UUID/목록 parser/URL·설정 hash만 기록하여 같은 UUID의 publicCode만 변경하면 대상 지문이 달라지지 않았다.

QA 입력 내부 schemaVersion을3으로 올리고 각 지자체 target에 publicCode를 추가했다. null/공백/100자 초과/제어문자 코드와 중복 UUID·중복 기관 코드는 예약을 거부한다. 원래 URL/설정 원문은 계속 저장하지 않는다. schema1/2 이력은 수정하지 않으며 새 입력과 일치하지 않아 재사용할 수 없다. 요청·응답 wrapper, 기존 v1/v2 HTTP 입력 필드는 바뀌지 않는다. DB 테이블/기존 migration 변경 없이 기존 JSON snapshot 계약을 사용한다.

### 독립 QA 결과의 업무 코드·추출기 연결

독립 QA는 QA JAR·의존성·스크립트/config·추출기 파일의 artifactHash를 계산했지만, 별도로 실행 중인 웹 업무 코드나 추출기 runtimeHash와 같다는 비교 근거를 출력하지 않았다. 산출물 hash 하나와 통과 건수만으로 정책의 설치 지문을 증명하면 안 된다.

현재 보고서 schemaVersion은 `reportSchemaVersion=2`다. scope는 기존 `SYNTHETIC_WORKER_DB_CONTRACTS_V2`를 유지한다.

- executionCodeHash: 자식 JVM이 현재 전체 main class/resource 목록과 classpath의 실제 바이트를 검증한 값. 실행 전후 같아야 한다.
- extractorRuntimeHash: 실제 Linux 실행에서 `/qa/extractor`에 대해 기존 AttachmentRuntimeIdentity가 검증한 값. parser JAR·Java/runtime·java.security·bwrap/prlimit·격리 실행 코드가 포함되며 전후 같아야 한다.
- artifactHash: 기존 전체 QA 산출물 hash와 전후 검사를 유지한다. 앞의 두 지문을 대신하지 않는다.
- inventory는 executionCodeHash만 검증하며 extractorRuntimeHash=null·INVENTORY_ONLY·passed0·notRun 전체를 반환한다. 존재하지 않는 Linux 증거를 Windows에서 생성하지 않는다.

자식 결과의 새 필드는 서버 자동 연결의 필요조건이지 충분조건이 아니다. 부모의 정책 run/snapshot/lease 및 전체 사례 집합 검증을 통과한 실제 실행 결과만 해당 단계 PASSED 증거가 된다. 관리자 성공 JSON 업로드 API는 추가하지 않는다.

## 실행 연결에 적용할 고정 경계

1. 기존 정책 전역 QA1개·8분 lease·추출 슬롯1개·파일 한도를 유지한다. 별도 runner의600초를 정책8분 안에 그대로 실행하거나 lease를 자동 연장하지 않는다.
2. 정책 run이 가진 현재 lease·selectExecutionAllowed를 실행 전에 확인한다. 부모 실행은 남은 시간에서 최소40초의 저장/정리 여유를 제외한 기한으로 제한하고 monotonic 시간으로 감시한다. 시간이 부족하면 미실행/실패 이유를 남기며 단계를 PASSED로 만들지 않는다.
3. 취소/슬롯 상실/기한 도달/상한 초과 시 부모가 소유한 해당 child namespace와 자손만 종료한다. 외부 timeout 시각만 보고 재시작하지 않는다. native DB와 임시 원본이 정리된 후에만 완료 근거를 저장한다. 현재 shell의600초 실행을 취소 불가능한 동기 호출로 사용하지 않는다.
4. 실행은 비root Linux의 별도 PID/IPC/네트워크·파일 namespace다. 기존 독립 QA 자원 상한 이내, 고정 네 suite·임시 loopback DB만 사용한다. 운영 DB/config/secret·외부 URL·임의 클래스/파일·Spring/JUnit 조건 해제 입력을 전달하지 않는다. Windows host fallback은 없다.
5. 부모는 검증한 별도 설치 경로와 자기 임시 작업 경로만 사용한다. 출력은 크기를 제한해 내부 pipe에서 읽고 native 예외/SQL/DB 경로는 보관하지 않는다. 실제 프로세스를 실행하지 않은 metadata만으로 성공을 만들지 않는다.
6. 예약 snapshot에는 **별도 QA 설치 artifact 지문과 기대 suite/case 목록**까지 고정한다. schema4에서 이 입력을 추가했다. child의 executionCodeHash와 extractorRuntimeHash는 예약된 웹 업무 코드/실제 추출기와 같아야 한다.
7. 결과의 run/time·artifact·업무 코드·추출기·suite/case 전체 집합과 실패/생략/미실행/정리 상태를 검증한다. 일부 성공 건수, INVENTORY_ONLY, 다른 실행/오래된 보고서, 덜 실행한 목록은 거부한다. 32KiB 단계 증거 한도를 지키며 전체 검증 결과를 잘라서 성공 저장하지 않는다.
8. 게시 verifier도 같은 계약으로 저장 근거와 현재 설치/입력을 재검증한다. verifier 없는 PASSED 문자열을 허용하지 않는다. 실제 네 단계 모두 검증한 뒤에만 VERIFIED로 마감할 수 있으며 마감 직전 정책·규칙·대상·설치 현재성을 다시 확인한다.

위 부모 실행·추가 산출물 snapshot·취소·게시 검증의 코드 연결은 구현했지만 실제 Linux 통합 성공은 미확인이다. Linux 실행 가능 환경이 없다는 이유로 이 요구를 삭제하지 않는다. 전체 Provider 실파일은 별도 단계이며 독립 DB의 합성 HTTP 성공으로 대체하지 않는다.

## 검증 기록

- 표적 snapshot/전체 코드 지문24건·독립 실행기/패키징14건, bootJar·독립 설치:40초 성공.
- 새 snapshot 회귀는 publicCode만 변경하는 경우·누락/중복 시스템 identity를 포함한다. 실제 별도 JVM 패키지 inventory는 업무 코드 지문 일치·runtimeHash null·통과0을 확인했다.
- 부모 실행 연결 표적 테스트72건·독립 실행기/패키징14건·bootJar:48초 성공. 첫 실행의 테스트 문법 오류와 다음 실행의 Mockito 재설정 오류2건을 수정한 뒤 재검증했다.
- JSONB/기록 시각 보강을 포함한 최종 전체 회귀:3분13초 성공, 서버1740통과/217조건부 생략·추출기25·QA14·Node130통과, bootJar 및 독립 설치 성공. 독립 목록191건의 runtimeHash=null·passed0을 재확인했다. 실제 부모 Linux2사례와 운영 DB/정책/배포는 미실행이다.
- 전체 최종 회귀와 Linux/운영 미확인 경계는 최신 진행 기록을 따른다. 이 문서는 실제 child DB 실행이나 정책 QA 게시 성공 기록이 아니다.
