# 섹션형 사이드바 운영 배포

## 범위와 기준선

- 사용자 승인: 참고 이미지의 섹션형 사이드바를 사내비 색상으로 구현한 후 운영 반영.
- 변경 파일: `src/main/resources/static/css/saneb-dashboard.css`, `scripts/qa/workspace-style-contract.test.cjs`.
- 작은 섹션 제목(13px), 아이콘 메뉴(14px), 크림색 배경과 청록색 선택 배경을 적용하고 트리 연결선을 제거한다.
- 기존 메뉴 경로·권한·한 그룹 접기 동작을 유지한다. DB/API/수집 정책/환경 설정 변경 및 재수집은 하지 않는다.
- 브랜치: `codex/attachment-three-stage-linux-qa`.
- 배포 코드: `bf7f41e65e420788daaaa88929a14bd0adcc6da4`.
- 고정 태그: `deploy-approved-bf7f41e-20261012`.
- 직전 운영 코드: `930833228e8de5e127a9985aa2350b54ebde45e2`, CodeDeploy `d-LWOKKXCBL`.
- 기존 미추적 파일은 수정·삭제·커밋하지 않는다.

## Gate·명령과 결과

| Gate | 상태 | 근거 / 필요한 조치 |
|---|---|---|
| 범위·계약·복구 | [x] | AGENTS.md, Git diff, 배포 workflow, backup/validate hook 확인. 신규 migration 없음 |
| Node 회귀 | [x] | 아래 명령 27건 통과, 실패·생략 0 |
| 로컬 Java·bootJar | [x] | 구현 단계 23개 suite/107건 통과, 빌드 성공. 이번 재확인은 17초/14개 UP-TO-DATE로 새 실행 건수에 중복 계산하지 않음 |
| 사전 운영 진단 | [x] | 읽기 전용 진단 성공, 설치/bundle 일치, V91/실패 0, 직전 JAR 존재 |
| 커밋·푸시 | [x] | 승인된 2개 파일만 한국어 커밋, branch 및 고정 태그 push |
| 배포 테스트·설치 | [x] | 실행 38231084911 성공, 전체 테스트·빌드 5분 36초, CodeDeploy d-PINIC4CBL 성공 |
| 별도 Linux DB 계약 CI | [~] | 실행 38231080002 진행 중 |
| 사후 운영·파일·health | [x] | 실행 38231972782 성공, 설치/bundle 일치·직전 JAR 보존, 운영 CSS 일치, HTTP 200/UP |
| 브라우저 시각 검증 | [ ] | 현재 요청에 명시 지시가 없어 사용자 정책상 미실행 |

실행 명령:

```powershell
node --test scripts/qa/workspace-style-contract.test.cjs scripts/qa/consultation-management.test.cjs scripts/qa/collected-navigation.test.mjs
.\gradlew.bat :test --tests '*ViewControllerSmokeTest' :bootJar --no-daemon --max-workers=2
git diff --check
gh workflow run diagnose-codedeploy.yml --ref deploy-approved-9308332-20261012 -f deployment_id=d-LWOKKXCBL -f ssm_deployment_preflight=true -f ssm_app_log_check=false -f ssm_attachment_qa_check=false
gh workflow run deploy.yml --ref deploy-approved-bf7f41e-20261012 -f attachment_qa=false
gh workflow run diagnose-codedeploy.yml --ref deploy-approved-bf7f41e-20261012 -f deployment_id=d-PINIC4CBL -f ssm_deployment_preflight=true -f ssm_app_log_check=false -f ssm_attachment_qa_check=false
Invoke-WebRequest http://15.165.36.6:8080/css/saneb-dashboard.css -UseBasicParsing
Invoke-WebRequest http://15.165.36.6:8080/actuator/health -UseBasicParsing
```

- 사전 진단: [38230957288](https://github.com/FrostyCityMan/saneB/actions/runs/38230957288).
- 배포: [38231084911](https://github.com/FrostyCityMan/saneB/actions/runs/38231084911).
- 사후 진단: [38231972782](https://github.com/FrostyCityMan/saneB/actions/runs/38231972782).
- 별도 Linux DB 계약 CI: [38231080002](https://github.com/FrostyCityMan/saneB/actions/runs/38231080002).
- 직전 코드의 별도 DB CI 38227742764도 현재 조회에서 성공을 확인했다. 이번 코드 CI 성공을 의미하지 않는다.

## 운영 기준선·복구

- 사전 설치/bundle SHA-256: `7d2b2406bb3dc281f600437cb2cbbf1eff95b35c1e0bbb78aba6f3b8923c30bf`.
- 사후 설치/bundle SHA-256: `19cfdb04b288bed4f84916d1ed61c190a5ecad9c9c00332f4e13ddf2e01ac887`.
- 사후 직전 JAR hash는 사전 설치 hash와 일치한다. 서비스 재시작은 2026-10-12 18:43:46 KST.
- DB endpoint SHA-256: `54d78493dd529524fc95495bf94711d44a35aaf08f63c915ce9e2549b581d26a`.
- DB V91, migration 실패 0.
- 정책 COLLECT_ONLY v2 / row version 2 / hash `88c3d36238dfe4d356186a434353ae770a6daeff491fbfb11e2bf9c4def9c4fc`.
- 첨부 worker/source batch true, 정부24 본문 flag UNSET. 활성·대기 첨부 작업 0.
- 연결 실행이 없는 9월 RUNNING 일정 1건은 기존 기록으로 보존한다.
- 사전/사후 진단 모두 readOnly=on, writes=0, transaction=ROLLED_BACK. DB endpoint·migration·정책 hash/row version·worker/source batch/정부24 flag가 유지됐다.
- 운영 CSS는 HTTP 200이며 UTF-8 디코딩·줄바꿈 정규화 후 승인된 로컬 파일과 일치했다. 새 섹션 토큰 포함·기존 트리 연결선 제거도 확인했다. health는 HTTP 200/UP.
- 배포 hook은 기존 JAR를 `.previous`에 보존하고 health 실패 시 복구한다. 신규 migration 없음.

## 판정·미실행·잔여 위험

Decision: **Conditionally ready**. 승인된 코드의 운영 설치와 사후 검증을 완료했다. 브라우저 검증은 정책상 미실행이고, 별도 장시간 Linux DB 계약 CI는 보고 시점 진행 중이다. 이번 변경은 Java·DB·migration 변경이 없는 CSS/테스트 수정이며 배포 workflow의 전체 테스트·빌드와 운영 전후 점검은 성공했다.

- 브라우저 QA 및 실제 픽셀 배치·줄바꿈·반응형·키보드 조작은 미검증이다. 정적 대비/스타일 검증을 브라우저 검증으로 간주하지 않는다.
- 외부 첨부 QA, 재수집, 정책 게시, 운영 데이터 편집은 수행하지 않는다.
- 배포 차단 항목은 없다. 별도 DB CI 완료는 해당 실행 결과에서 확인해야 하며 진행 중 결과를 성공으로 계산하지 않는다. 실제 시각적 수용 판정은 별도 브라우저 QA가 필요하다.
- Node 명령과 단발 Gradle은 종료했다. 타 작업 프로세스는 종료 대상으로 삼지 않는다.
