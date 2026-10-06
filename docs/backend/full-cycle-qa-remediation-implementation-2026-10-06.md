# 사내비 전체 사이클 QA 회귀 수정 구현 기록

## 범위와 기준선

- 사용자 승인: 상세 설계 기준 구현 및 운영 반영. 브라우저 실행은 현재 요청에 없으므로 미실행한다.
- 설계: [전체 사이클 수정 설계](full-cycle-qa-remediation-design-2026-10-06.md).
- 기준선: `codex/attachment-three-stage-linux-qa`, `48f2b3505842232d6301bbb529691f2036b5c888`, V91. 기존 사용자 미추적 문서/QA 산출물/SSO 도구는 보존한다.
- 적용 skill: long-goal-operating-protocol, ui-ux-operating-principles, frontend-design-core, frontend-ui-engineering, release-readiness-gate. 기존 SSR/Bootstrap·현재 계약 재사용, 데이터 무결성·오류 회복 우선, Gate별 증거 분리를 적용한다.

## 변경 내용

1. 진행 결과: 같은 진행의 수정은 부모 잠금을 선행한다. 마지막 행동과 접수 변경이 APPROVED/REJECTED/SUPPLEMENT_REQUIRED/STOPPED 결과를 대기로 되돌리지 않는다. 결과가 있는 일반 중단은 정정 안내, 기존 상태 불일치는 명시적 409이며 자동 보정하지 않는다. 단계/현재 포인터 갱신 건수가 틀리면 transaction을 원복한다.
2. 현재 후보: 사용자 BASIC 목록/count, 대시보드 후보/금액/FINAL count, 신규 진행 가능 조회에 승인·NORMAL·KST 접수 기간 필터를 적용한다. 기존 매칭/진행 이력은 보존한다.
3. 검수→초안: 서버 conversion-context를 두 화면에서 재사용한다. BASE_REVIEW는 태그 확인·검수 사유·DRAFT 생성·연결을 원자적으로 저장한다. 필수 첨부는 기존 엄격 hash/버전/확인 경로를 유지한다. 본문 미확보·최신 수집 실패·제외/보관·중복 후보는 차단한다. 중복 요청은 기존 연결 UUID를 반환한다.
4. 공고 입력: UUID 이동과 기존 ANN 코드 정확 조회를 지원한다. 조회 실패 중 저장을 잠그고 늦은 상세/동적 항목 응답을 무시한다. 옵션 조건은 0행 가능하며 마지막 빈 행을 삭제할 수 있다. 양수 수치 정책은 유지하고 구체적인 오류를 표시한다.
5. 사용자 동적 입력: 로딩 전 저장 및 중복 저장을 차단하고 요청값을 비동기 대기 전에 고정한다. 실패 시 입력을 유지한다. 미저장 변경/필수 값 미입력 동안 다음 행동을 차단한다. 기존 첫 저장 실패의 원인은 아직 단정하지 않으며 운영 브라우저 재현이 남아 있다.
6. 표시: instant를 Asia/Seoul로 변환하여 진행·검증 시간을 표시한다. 기존 inProgressCount 라벨을 현재 진행 건수로 정정한다.
7. 분류: 본인 신청 절차·육아휴직 단어를 지원대상 확정 대신 CONTEXT_ONLY 근거로 보존하는 좁은 문맥 방어 및 positive/negative fixture를 추가했다. A/B 우선순위·다중 분류를 보존한다. SRC-017843의 게시 규칙별 전체 근거 추적, 일반 구간 판정 확장, 운영 규칙 게시/기존 공고 재분류는 완료로 주장하지 않는다.

신규 migration·의존성·스택 변경은 없다. 운영 공고 자동 활성화·규칙 게시·정책 전환·재분류·운영 데이터 보정은 실행하지 않는다.

## 검증 Gates

- [x] G0 기존 지침·설계·기준선 확인.
- [x] G1 결과 단위 테스트 및 실제 PG 양방향 경합·중복 행동 로그 보존 검증 통과.
- [x] G2 현재 후보/count/페이지/대시보드/마감일/이력 보존 및 신규 진행 차단 실제 PG 검증 통과.
- [x] G3 Node 기본/필수 첨부 전환·응답 유실 재시도, 실제 PG confirmation rollback·중복 연결 영수증·DRAFT/코드 조회 검증 통과.
- [~] G4 동적 입력 첫 저장·중복·실패 보존 Node 3/3 및 태그 fixture 통과. 운영 최초 저장 재현과 게시 규칙별 전체 과분류 분석은 미완료다. 새 규칙 게시·기존 데이터 재분류는 실행하지 않았다.
- [x] G5 최신 SHA Linux 전체 test/bootJar·격리 실제 DB CI 성공. 필수 7개 suite 280건은 실패·오류·생략 없이 통과했다. 일반 root suite의 조건부 생략은 아래에 별도 기록한다.
- [~] G6 운영 코드 설치·동일 JAR/DB/정책/health 확인은 완료. 관리자→사용자 전체 브라우저 사이클·시각/반응형 검증은 현재 요청의 브라우저 정책에 따라 미실행이다. 전체 사이클 완료로 보고하지 않는다.

### 실행 기록

- `gradlew.bat compileJava --no-daemon --max-workers=1`: 성공.
- `gradlew.bat :test --tests '*ApplicationProgressServiceImplTest' --tests '*AnnouncementSourceTargetContextTest' --tests '*AnnouncementSourceClassificationEngine*' --tests '*AnnouncementSourceV2ConversionServiceImplTest' --no-daemon --max-workers=1`: 성공. 이후 추가 테스트는 전체 회귀에 포함한다.
- Node 지정 11개 suite: 151건 중 148 통과, 3 Windows 미지원 Linux 심볼릭 링크 검사 생략, 실패 0. 브라우저 대역 실행이며 실제 브라우저 성공으로 표현하지 않는다.
- unqualified `test --tests`는 extractor 모듈의 대상 없음으로 실패했다. `:test`로 올바른 루트 범위를 지정한 재실행은 성공했다. 실패 이력은 성공으로 바꾸지 않는다.
- Windows Docker daemon 미접속: 실제 PostgreSQL은 운영 DB가 아닌 Linux CI의 임시 loopback DB로 검증한다.
- 로컬 전체 `test bootJar`: 루트 4,720건 중 4,313 통과·407 조건부 생략·실패 0, 추출기 251/251 통과. 최종 보완 후 추가 단위 54/54와 bootJar도 통과했다. 실행 대상/시점이 다른 집계는 합산하지 않는다.
- Linux `37480859698`: Flyway 12건 중 10 통과·2 실패. 실패는 같은 테스트 transaction에서 JdbcTemplate 변경 후 MyBatis가 기존 조회 값을 재사용한 후보/FINAL 집계 기대값이었다. JDBC fixture 변경 뒤 테스트 세션 캐시를 명시적으로 비워 별도 HTTP 조회의 새 SQL 실행을 재현한다. 운영 쿼리/기대값/필수 Gate는 완화하지 않는다. 결과 보존·양방향 경합·기본 전환 rollback/중복 영수증은 이 실행에서 통과했지만 전체 CI 성공으로 표현하지 않는다. 실패 artifact는 `output/qa/remediation-ci-37480859698`에 보존했다. 수정 후 최신 SHA 전체 Linux 재검증이 필요하다.
- `7ffaef4` CI `37480259111`은 최종 보완 SHA로 대체되어 취소했으며 성공 근거가 아니다.
- 최신 Linux CI [37482152866](https://github.com/FrostyCityMan/saneB/actions/runs/37482152866), SHA `4d121eb850669490d357fc9cf226aa85565fcd2e`: 전체 성공. root 4,723건 중 4,315 통과·408 조건부 생략·실패 0, 추출기 251/251 통과. Flyway 실제 PG 12/12, Job 229/229, migration 4/4, backfill 14/14, 격리 runtime 7/7, worker 12/12, 정책 부모 QA 2/2의 필수 합계 280건은 생략 0이다. 공식 사이트 관측 옵션은 OFF이며 새 지역 수집 성공의 근거가 아니다.
- 성공 artifact: `output/qa/remediation-ci-37482152866`. 실패 artifact와 구분해 보존한다. Node/Gradle/조회 CLI는 종료됐고 서브에이전트·브라우저는 새로 실행하지 않았다.
- 서울 읽기 전용 사전 점검 `37479311260`: CodeDeploy `d-7870D16X8`의 설치/bundle JAR 동일, DB V91·migration 실패 0, COLLECT_ONLY 정책 v2, 활성 첨부 작업 0, 이전 JAR 존재. 오래된 스케줄 RUNNING 1건은 보존하며 이번 회귀 수정과 분리한다. DB transaction READ ONLY/ROLLBACK, writes=0.

## 운영 반영 및 남은 위험

Decision: **Conditionally ready**. 승인된 코드 설치 범위는 완료했지만 전체 제품 사이클 종료 기준은 아직 충족하지 않았다.

### 운영 코드 설치 완료 — 2026-10-07 KST

- 배포 SHA `4d121eb850669490d357fc9cf226aa85565fcd2e`, 고정 태그 `deploy-approved-4d121eb-20261006`.
- [배포 workflow 37484892043](https://github.com/FrostyCityMan/saneB/actions/runs/37484892043): 성공. `attachment_qa=false`로 실제 외부 파일 QA/정책 활성화를 실행하지 않았다.
- CodeDeploy `d-6IFJSSK7L`, revision `deployments/FrostyCityMan/saneB/4d121eb850669490d357fc9cf226aa85565fcd2e.zip`. 배포 target은 기존 서울 saneB/saneb-dev 단일 인스턴스이며 서비스 재시작을 포함한다.
- [사후 읽기 전용 SSM 37486152127](https://github.com/FrostyCityMan/saneB/actions/runs/37486152127): 성공. 설치 JAR와 해당 배포 bundle 지문은 모두 `032657ab55bc21bd1ec4bc7501b9f2ff9d60c8a01730f85d206974f6944ef41f`이며 verified=true다. 이전 JAR는 배포 전 설치 지문 `54ad28b30d836162e855c9b200ba8e2c6b1349821972f1438bb995ff6ba28e7a`와 일치한다.
- DB V91·migration 실패 0. 기존 첨부 COLLECT_ONLY 정책 v2·row_version=2·정책 hash 유지. worker=true/source batch=true/정부24 상세 본문 플래그 UNSET 유지. 활성 첨부 작업 0, 오래된 RUNNING 스케줄 1건은 변경하지 않았다. 진단 SQL READ ONLY/ROLLBACK·writes=0.
- HTTP 읽기 전용 smoke: `/actuator/health` 200/UP. 공고 전환·공고 입력·진행 입력·수집 목록 JavaScript 4개가 승인 소스와 줄바꿈 정규화 후 hash 일치한다. 인증 없는 신규 코드 조회/전환-context API는 각각 401이다. 실제 브라우저 렌더링·관리자 권한별 조작 검증과 구분한다.
- 신규 migration·규칙 게시·정책 전환·기존 데이터 재분류/보정·실결제·기관 제출을 실행하지 않았다. 서비스 재시작으로 로그인 세션이 만료될 수 있다.

### 전체 Gate 종료 전에 필요한 후속 검증

1. 사용자의 명시적 브라우저 QA 요청 후 관리자→사용자 1사이클, 첫 유효 동적 입력 저장, 확정 결과 후 마지막 행동, 숨김/이력 보존, 작은 화면/오류 회복을 운영에서 검증한다. 현재 HTTP 주소의 인증/개인정보 전송 위험은 별도 운영 과제다.
2. SRC-017843의 실제 게시 규칙·원문 구간별 과분류 근거를 추적하고 일반 구간 판정/새 규칙 초안을 검증한다. 기존 공고 제안 태그가 자동 정정됐다고 주장하지 않는다. 운영 게시·재분류는 별도 승인한다.
3. 기존 결과/진행 불일치와 오래된 RUNNING 스케줄은 읽기 전용 preview·근거 확인 후 필요한 대상에 한해 별도 승인받아 보정한다. 상태 일괄 UPDATE나 이력 삭제는 하지 않는다.

구현 기록만으로 전체 서비스·모든 지역 크롤링·실제 파일 정확도·전체 브라우저 사이클이 완료됐다고 판단하지 않는다.
