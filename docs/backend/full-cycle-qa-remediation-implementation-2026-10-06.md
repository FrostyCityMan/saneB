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
- [~] G1 결과 단위 테스트 통과; 실제 PG 양방향 경합·중복 로그 보존 검증 대기.
- [~] G2 현재 후보/count/대시보드/이력 보존 실제 PG 검증 대기.
- [~] G3 Node 기본/필수 첨부 전환·응답 유실 재시도 통과; 실제 PG rollback·중복 연결 검증 대기.
- [~] G4 동적 입력 첫 저장·중복·실패 보존 Node 3/3 통과. 태그 fixture 통과. 운영 최초 저장 재현과 전체 과분류 분석은 미완료.
- [~] G5 전체 test/bootJar 및 Linux 격리 DB CI 대기.
- [ ] G6 운영 코드 반영·동일 JAR/DB/정책 확인 대기. 전체 브라우저 사이클은 정책상 미실행이며 별도 검증 필요.

### 실행 기록

- `gradlew.bat compileJava --no-daemon --max-workers=1`: 성공.
- `gradlew.bat :test --tests '*ApplicationProgressServiceImplTest' --tests '*AnnouncementSourceTargetContextTest' --tests '*AnnouncementSourceClassificationEngine*' --tests '*AnnouncementSourceV2ConversionServiceImplTest' --no-daemon --max-workers=1`: 성공. 이후 추가 테스트는 전체 회귀에 포함한다.
- Node 지정 11개 suite: 151건 중 148 통과, 3 Windows 미지원 Linux 심볼릭 링크 검사 생략, 실패 0. 브라우저 대역 실행이며 실제 브라우저 성공으로 표현하지 않는다.
- unqualified `test --tests`는 extractor 모듈의 대상 없음으로 실패했다. `:test`로 올바른 루트 범위를 지정한 재실행은 성공했다. 실패 이력은 성공으로 바꾸지 않는다.
- Windows Docker daemon 미접속: 실제 PostgreSQL은 운영 DB가 아닌 Linux CI의 임시 loopback DB로 검증한다.
- 서울 읽기 전용 사전 점검 `37479311260`: CodeDeploy `d-7870D16X8`의 설치/bundle JAR 동일, DB V91·migration 실패 0, COLLECT_ONLY 정책 v2, 활성 첨부 작업 0, 이전 JAR 존재. 오래된 스케줄 RUNNING 1건은 보존하며 이번 회귀 수정과 분리한다. DB transaction READ ONLY/ROLLBACK, writes=0.

## 운영 반영 및 남은 위험

배포 결과는 후속 검증에서 기록한다. 운영 설치를 끝내더라도 관리자→사용자 브라우저 전 구간·모바일 검증을 통과했다고 주장하지 않는다. 규칙 버전/기존 데이터는 이번 코드 배포에서 변경하지 않는다. 기존 불일치 데이터는 별도 preview·승인·좁은 보정이 필요하다.
