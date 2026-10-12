# 사이드바 위계 개선 운영 배포

## 범위

- 요청: 대메뉴보다 하위 메뉴가 크게 보이는 문제 수정과 하위 연결선 보완 후 운영 배포.
- 기존 사내비 CSS·템플릿 재사용. 대메뉴 16px/800, 중메뉴 14px/650, 소메뉴 13px/600.
- 현재 메뉴는 2단 구조다. 소메뉴 스타일만 준비했으며 메뉴 재분류·경로·권한·접기 동작은 변경하지 않았다.
- CSS 장식 연결선, 마지막 항목 종료선, 현재 메뉴 강조선, 포커스 표시, 긴 메뉴명 줄바꿈, 축소 화면의 정책 메뉴 아이콘 보완.
- 변경 파일: `src/main/resources/static/css/saneb-dashboard.css`, `src/main/resources/templates/fragments/layout.html`, `scripts/qa/workspace-style-contract.test.cjs`.
- DB/API/수집 정책/환경 설정 변경, 재수집, 실제 외부 첨부 QA는 범위 밖이다. 운영 재시작은 배포에 포함된다.
- 사용자 정책에 따라 브라우저 QA는 실행하지 않는다. 실제 시각적 배치·줄바꿈·키보드 조작은 미검증 위험이다.

## 코드 기준선

- 브랜치: `codex/attachment-three-stage-linux-qa`.
- 이전 HEAD: `d95be355b9678d8761b16c328805df717ec7ee73`.
- 운영 코드: `930833228e8de5e127a9985aa2350b54ebde45e2`.
- 고정 태그: `deploy-approved-9308332-20261012`.
- 기존 미추적 파일은 수정·삭제·커밋하지 않는다.

## Gate·검증 명령

- [x] AGENTS.md, Git diff, 배포 workflow와 복구 경로 확인.
- [x] `node --test scripts/qa/workspace-style-contract.test.cjs scripts/qa/consultation-management.test.cjs scripts/qa/collected-navigation.test.mjs`: 26건 통과. 이 중 공통 스타일 계약 9건.
- [x] `gradlew.bat :test --tests '*ViewControllerSmokeTest' :bootJar --no-daemon --max-workers=2`: 구현 단계 23개 suite/107건 실패·오류·생략 0, 빌드 2분 45초 성공. 배포 전 재확인은 동일 입력의 UP-TO-DATE 결과로 19초 성공이며 새 테스트 실행 건수로 중복 계산하지 않는다.
- [x] `git diff --check`, staged 범위 검사, 한국어 커밋·푸시.
- [x] 사전 운영 진단 [38227597381](https://github.com/FrostyCityMan/saneB/actions/runs/38227597381): 서울/ap-northeast-2, 현재 배포 `d-GTFTB2CBL`, V91/실패 0, 설치·bundle hash 일치, 직전 JAR 존재, writes=0/ROLLED_BACK.
- [x] 배포 [38227745902](https://github.com/FrostyCityMan/saneB/actions/runs/38227745902): 전체 테스트·빌드 5분 37초 성공, CodeDeploy `d-LWOKKXCBL` 성공. `attachment_qa=false`로 외부 첨부 QA 미실행.
- [~] 별도 Linux DB 계약 CI [38227742764](https://github.com/FrostyCityMan/saneB/actions/runs/38227742764): 자동 실행 중. 배포 workflow의 테스트와 구분한다.
- [x] `Invoke-WebRequest` 운영 CSS 조회: HTTP 200, UTF-8 디코딩·줄바꿈 정규화 후 로컬 파일과 일치. 16px 대메뉴 토큰·하위 연결선 선언 포함 확인.
- [x] `Invoke-WebRequest /actuator/health`: HTTP 200, `UP`.
- [x] 사후 운영 진단 [38228608272](https://github.com/FrostyCityMan/saneB/actions/runs/38228608272): 설치/bundle hash 일치, 직전 JAR hash는 사전 설치값과 일치. V91/실패 0, DB endpoint hash·정책 hash/row version·worker/source batch/정부24 flag 불변, writes=0/ROLLED_BACK.
- [ ] 브라우저 QA: 현재 요청에 명시 지시가 없어 정책상 미실행.

## 운영 기준선과 복구

- 사전 설치/bundle SHA-256: `703e7557431a78c931b00c9a5ddd14812f3b2250ee626b77abccca92b2300c53`.
- 사후 설치/bundle SHA-256: `7d2b2406bb3dc281f600437cb2cbbf1eff95b35c1e0bbb78aba6f3b8923c30bf`.
- 운영 재시작 시각: 2026-10-12 18:12:48 KST.
- DB endpoint SHA-256: `54d78493dd529524fc95495bf94711d44a35aaf08f63c915ce9e2549b581d26a`.
- 정책: COLLECT_ONLY v2, row version 2, hash `88c3d36238dfe4d356186a434353ae770a6daeff491fbfb11e2bf9c4def9c4fc`.
- 첨부 worker/source batch true, 정부24 본문 flag UNSET 유지. 활성·대기 첨부 작업 0.
- 9월 RUNNING 일정 1건은 연결 실행이 없는 기존 기록이며 이번 작업에서 변경하지 않는다.
- 배포 hook은 기존 JAR를 `.previous`에 보존하고 health 실패 시 복구한다. 새 migration 없음.

## 판정

Decision: **Conditionally ready**. 승인된 코드의 운영 설치와 사후 점검은 완료했다. 브라우저 시각 검증은 미실행이며 별도 장시간 DB 계약 CI는 보고 시점 진행 중이다. 이를 성공으로 계산하지 않는다. 이번 변경은 Java·DB·migration 변경이 없는 UI 수정이며 배포 workflow의 전체 테스트·빌드와 사전/사후 운영 DB 점검은 모두 통과했다.

| Gate | 상태 | 근거 / 필요한 조치 |
|---|---|---|
| 변경 범위·로컬 회귀 | [x] | 3개 파일, Node 26건, 동일 입력의 Java 107건 및 bootJar 결과 확인 |
| 배포용 전체 테스트·빌드·설치 | [x] | 배포 workflow 및 CodeDeploy 성공 |
| 운영 파일·health·복구 | [x] | 설치/bundle 일치, CSS 일치, HTTP 200/UP, 직전 JAR 보존 |
| DB·정책·설정 보존 | [x] | 사전/사후 읽기 전용 보고서 대조 |
| 별도 장시간 DB 계약 CI | [~] | 자동 실행 중. 최종 성공은 해당 실행 결과에서 별도 확인 필요 |
| 브라우저 시각 검증 | [ ] | 현재 요청에 명시 지시가 없어 정책상 미실행 |

## 미실행·잔여 위험·후속 확인

- 브라우저 QA, 실제 운영 저장 테스트, 첨부 재수집·정책 게시를 실행하지 않았다.
- 실제 픽셀 배치·줄바꿈·태블릿/모바일 동작·키보드 조작은 정적 테스트만으로 통과 판정하지 않는다.
- 별도 CI 완료 여부는 위 실행 링크로 확인할 수 있다. UI 운영 설치 성공과 전체 DB CI 완료를 혼동하지 않는다.
- 이번 승인 범위의 배포를 막는 추가 항목은 없다. 시각적 수용 판정이 필요하면 별도 브라우저 QA가 필요하다.
- Node 명령은 종료됐으며 Gradle은 `--no-daemon` 단발 실행으로 종료됐다. 기존 사용자 파일과 타 작업 프로세스는 정리 대상으로 삼지 않았다.
