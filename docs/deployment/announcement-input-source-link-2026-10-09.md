# 공고 입력 운영 메모·원문 링크 배포

- 요청: 운영 메모 세로 확대와 원문이 있는 공고의 1단계 위 원문 링크 하나 표시, 운영 반영.
- 코드 SHA: `1e75ae2b506d436e93784bfbae27ebc311f22d58`.
- 고정 배포 태그: `deploy-approved-1e75ae2-20261009`.
- 대상: 서울(`ap-northeast-2`)의 기존 `saneb` / `saneb-dev` 배포 그룹.
- 운영 메모는 3줄에서 10줄로 확대한다. `sourceUrl`을 상세 응답에 추가하고 기존 수집 원문 연결에서 주소를 읽는다. HTTP(S) 원문이 있으면 링크 하나를 표시하며 신규 입력·상세 전환 시 이전 링크를 제거한다.
- DB migration·정책·규칙·기존 데이터 보정 없음. 기존 Java 21/Spring Boot/MyBatis/Thymeleaf 구조를 유지한다.

## 검증 및 배포 Gate

- [x] 변경 9개 파일을 검토하고 해당 파일만 커밋·푸시. 기존 미추적 파일 보존.
- [x] 구현 시 Java/API·템플릿 테스트 18건, Node 회귀 테스트 27건 통과.
- [x] 배포 전 `git diff --check`, `node --check src/main/resources/static/js/saneb-announcement-input.js` 통과.
- [x] Linux CI [37868565763](https://github.com/FrostyCityMan/saneB/actions/runs/37868565763) 전체 성공. 단위·HTTP·실제 임시 DB·migration·artifact/빌드, 독립 첨부 QA 실행기, 정책 부모 QA 연결·취소·정리 검증 통과. 필수 DB 7개 suite 280/280 통과(실패·오류·생략 0).
- [x] 배포 전 읽기 전용 점검 `37868588068` 성공. DB V91·migration 실패 0, 설치/bundle JAR 지문 `032657ab55bc21bd1ec4bc7501b9f2ff9d60c8a01730f85d206974f6944ef41f` 일치, 기존 정책 COLLECT_ONLY v2·worker/source batch 설정 유지, 활성 첨부 작업 0. 기존 오래된 RUNNING 스케줄 1건은 보존한다.
- [x] 운영 배포 workflow [37869513927](https://github.com/FrostyCityMan/saneB/actions/runs/37869513927) 성공. 설치 전 기존 workflow의 전체 테스트·빌드 통과. CodeDeploy `d-MWKST859L` 성공.
- [x] 운영 HTTP `/actuator/health` 200/UP, `/js/saneb-announcement-input.js` 200 및 승인 소스와 줄바꿈 정규화 후 일치. JavaScript SHA-256: `5671b15c40fe230dd23a17418077e4e962d2c6b691fed0d9611dc75dcf7b285d`.
- [x] 사후 읽기 전용 JAR·DB 점검 [37870007128](https://github.com/FrostyCityMan/saneB/actions/runs/37870007128) 성공. 설치 JAR와 배포 bundle SHA-256 `7e22d65f419832708fdc84e0dc7cf243a4d306638e2712d7e8d653848cc72f9c` 일치. 이전 JAR는 배포 직전 설치 지문과 일치한다. DB V91·migration 실패 0, 정책 COLLECT_ONLY v2/hash·worker/source batch 설정 유지. 진단 writes=0/ROLLBACK.
- [x] 배포 결과 기록. 서비스 시작 시각 2026-10-09 10:28:08 KST.

## 판정 및 실행 결과

Decision: **Conditionally ready** — 요청한 코드 운영 설치와 서버 검증은 완료했다. 실제 브라우저 렌더링은 현재 요청 정책에 따라 미검증이며 최종 시각 검증을 통과했다고 주장하지 않는다.

- 실행: `gh workflow run deploy.yml --ref deploy-approved-1e75ae2-20261009 -f attachment_qa=false` → 성공.
- 실행: 동일 태그의 `diagnose-codedeploy.yml` 사전·사후 읽기 전용 점검 → 성공.
- 실행: Node의 HTTP health 및 정적 JavaScript 비교 → 200/UP 및 승인 소스 일치. Node 프로세스 정상 종료.
- 남은 배포 차단 사항 없음. 커밋은 요청한 변경과 이 배포 기록만 포함하며 기존 미추적 파일을 보존한다.

## 실행 범위와 복구

기존 GitHub Actions OIDC와 CodeDeploy 배포를 사용한다. `attachment_qa=false`로 코드 설치·서비스 재시작만 수행한다. 배포 스크립트는 기존 JAR를 `app.jar.previous`로 보존하고 health 실패 시 복구를 시도한다.

브라우저 검증은 현재 요청의 명시적 지시가 없어 사용자 정책에 따라 미실행이다. 서버 템플릿 테스트·HTTP 확인을 브라우저 렌더링 검증으로 표현하지 않는다.
