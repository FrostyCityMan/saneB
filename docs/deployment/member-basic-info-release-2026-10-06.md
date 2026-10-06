# 기본정보 입력 간소화 운영 배포

## 승인·범위

- 사용자가 구현 완료 후 운영 반영을 요청했다.
- 대상 코드: `45d39ae1f0f478a5378af19486e98166e7dbeade`.
- 고정 배포 태그: `deploy-approved-45d39ae-20261006`.
- 일반 사용자 기본정보 입력 UI: 국세·건강보험 단일 선택, 가족별 입력, 서류 사전 표시, 숨긴 기존 값 보존. 상세 변경은 `docs/frontend/member-basic-info-simplification-2026-10-06.md` 참조.
- 구현 파일 및 테스트·구현 문서·CI 연결 11개 파일만 커밋했다. 기존 미추적 목업·AWS 도구·output은 포함하지 않았다.
- 이전 운영 코드 `381c2c9f58b9124f80c208c0ab607732e27cf2d9` 대비 migration·배포 hook·application 설정 변경 없음.
- 정책 게시·수집 실행·기존 자료 재처리·테스트 회원 저장은 하지 않는다. `attachment_qa=false`로 코드 배포만 실행한다.
- 브라우저 QA는 현재 요청에 명시되지 않아 정책상 미실행이다. 공개 health·자산 GET·서버 읽기 전용 진단은 브라우저 검증과 별개다.

## 배포 Gate

- [x] AGENTS.md·배포 절차·범위·원격 기준선 확인
- [x] 로컬 Node 기본정보 22건, Spring 사용자/관리자 HTTP·SSR 10건 통과, bootJar 성공
- [x] 배포 직전 Node 기본정보+배포 계약: 37통과·0실패·Linux 전용 3생략
- [x] 서버 읽기 전용 사전 점검·이전 복구 JAR 확인
- [x] 동일 SHA Linux CI: [37421423097](https://github.com/FrostyCityMan/saneB/actions/runs/37421423097) 성공. 독립 DB·worker 계약 259건 통과, 실패·생략 0건
- [x] 운영 코드 설치·ValidateService: [37422617883](https://github.com/FrostyCityMan/saneB/actions/runs/37422617883), 고정 태그와 headSha 일치, CodeDeploy `d-27REJCB7L` 성공
- [x] 배포 후 health·정적 자산·서버 설치 지문 확인
- [ ] 실제 사용자 입력 저장·브라우저 QA: 이번 작업에서 실행하지 않음

## 사전 상태와 롤백 경로

[사전 진단 37421336508](https://github.com/FrostyCityMan/saneB/actions/runs/37421336508)은 성공했다. 현재 성공 배포 `d-3R44LH97L`와 배포 그룹 일치를 확인한 고정 읽기 전용 진단이다.

- DB V91, migration 실패 0, READ ONLY 트랜잭션 후 ROLLBACK, 진단 업무 쓰기 0.
- 첨부 active/waiting 작업 0, 정책/수집원 QA 0, 대기 일정 0.
- 2026-09-07 예정·09-09 최종 갱신·연결 run 없는 RUNNING 이력 1건은 기존 미종결 이력으로 유지한다. 정상 종료나 현재 실행 중 작업으로 단정하지 않는다.
- 활성 정책 COLLECT_ONLY 개정2 유지. worker/source-batch=true, GOV24 상세 본문 플래그 UNSET 관측.
- 설치 JAR과 직전 배포 원본 SHA-256 `33d930a44423158674cb9c49d87b0c55b6023ec26282a87cc3765a494dc09c3f` 일치. 사전 health `UP`.
- 기존 복구 JAR 존재. 신규 배포의 backup hook이 현재 운영 JAR을 `app.jar.previous`로 보존하고 health 실패 시 validate hook이 이를 복원하는 기존 절차를 유지한다. 실제 롤백 실행 시험은 하지 않았다.
- Linux CI 성공 후 [직전 재점검 37422358550](https://github.com/FrostyCityMan/saneB/actions/runs/37422358550)도 성공했다. 설치 지문·V91·정책·플래그는 위 상태와 일치하며 active/waiting 작업은 0건이었다.

## 실행 기록

- Git 기본 TLS 저장소에서 인증서 체인 오류가 발생해 명령 한정 `git -c http.sslBackend=schannel`로 Windows 인증서 검증을 사용했다. SSL 검증은 끄지 않았다.
- 범위 지정 `git add`, 한국어 본문 포함 Conventional Commit, 작업 브랜치 push 및 고정 태그 push.
- 새 Node 시험을 Linux CI와 배포 workflow에 연결했다. 원격 CI가 통과하기 전에는 설치를 실행하지 않는다.
- `gh workflow run deploy.yml --ref deploy-approved-45d39ae-20261006 -f attachment_qa=false`: 성공. 배포 workflow 테스트·빌드 4분 16초 성공, 서버 재시작 2026-10-06 15:19:07 KST.
- `gh workflow run diagnose-codedeploy.yml --ref deploy-approved-45d39ae-20261006 -f deployment_id=d-27REJCB7L -f ssm_deployment_preflight=true -f ssm_app_log_check=false -f ssm_attachment_qa_check=false`: [37423225174](https://github.com/FrostyCityMan/saneB/actions/runs/37423225174) 성공.
- 사후 진단에서 현재 배포 그룹·성공 배포 일치와 모든 lifecycle 단계(ValidateService 포함) 성공을 확인했다.
- 설치 JAR과 해당 배포 원본 SHA-256은 `6b4442a25a3d0c4f71a90a1b50b5d11e4aa7f5843878fb74c5145dd00fe97480`으로 일치한다. 복구 JAR은 배포 직전 운영 지문 `33d930a44423158674cb9c49d87b0c55b6023ec26282a87cc3765a494dc09c3f`과 일치한다.
- DB V91·migration 실패 0·활성 정책 ID/해시/개정 및 worker/source-batch/GOV24 플래그가 사전 결과와 동일하다. 진단 트랜잭션 READ ONLY·ROLLBACK·쓰기 0건.
- Node HTTP GET 검증: `/actuator/health` HTTP 200·UP. 기본정보 JS 3개와 공통 CSS 모두 HTTP 200, CRLF/LF 정규화 후 저장소 소스와 일치. 브라우저·실제 회원 저장 검증의 대체 증거로 사용하지 않는다.

## 최종 판정

**Conditionally ready** — 승인된 운영 코드 설치·서버 검증은 완료했다. 사용자 지침상 브라우저 QA와 실제 회원 데이터 저장은 하지 않았으므로 해당 사용자 여정까지 검증 완료라고 표현하지 않는다.

| Gate | 상태 | 근거 / 후속 조치 |
|---|---|---|
| 코드·테스트·빌드 | [x] | 고정 SHA Linux CI 및 배포 빌드 성공 |
| 호환성·운영 설정 | [x] | DB/API/migration 변경 없음, V91·정책·플래그 유지 |
| 운영 설치·health | [x] | CodeDeploy·ValidateService 성공, 공개 health UP, 자산/설치 지문 일치 |
| 복구 준비 | [x] | 직전 JAR 보존 확인; 실제 롤백 시험은 미실행 |
| 브라우저·회원 저장 | [~] | 현재 요청에 브라우저 명시 지시 없음. 후속 사용자 검증 필요 |

추가 배포를 막는 실패는 발견하지 않았다. 남은 필수 사용자 여정 확인은 로그인한 일반 사용자의 기본정보 조회·입력·저장·재조회이며, 별도 브라우저 QA 요청 또는 사용자의 직접 테스트로 확인한다.

## 검증 경계와 잔여 위험

- 실제 회원의 조회→수정→저장→재조회 및 모바일 렌더링은 검증하지 않았다. 현재 요청은 운영 반영이며 브라우저 실행은 별도 명시 지시가 필요하다.
- 기존 과거 가족 서류 요약과 `families[]`는 별도 계약으로 보존한다. 과거 요약을 여러 가족으로 자동 변환하지 않으며, 내용이 다르면 운영자 정정이 필요하다.
- 기본정보 저장 시 기존 BASIC 후보 재계산을 유지한다. 신규 공고·공고 상태 변경에 따른 자동 재매칭은 이번 구현·배포 범위가 아니다.
- V91 이전 migration 수정, 신규 migration, 권한·인증 설정 변경은 없다. 업무 데이터는 배포 검증용으로 수정하지 않는다.
- 배포 스킬은 코드·테스트·운영 설치·브라우저 증거를 분리하고, 장기 작업 스킬은 고정 SHA 검증과 배포 전후 비교·복구 경로 확인에 적용했다.
