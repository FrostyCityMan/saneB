# 공고 전환 검수 입력 간소화 운영 반영

- 승인 범위: 공고 전환 화면의 중복 분류 입력 제거, 검수 방법·사유 중심 입력, 기존 분류를 비공개 초안으로 전달.
- 코드 SHA: `c81bc3ec362c6d7693f6edfe46fb1361505fc31d`.
- 대상: 서울(`ap-northeast-2`), 기존 `saneb` / `saneb-dev` CodeDeploy.
- 지원대상·지원형태·대표 대상·소득 기준은 공고 입력에서 확인·수정한다. 자료 오류의 필수 추가 확인, 기존 권한·CSRF·버전·중복 방지·재시도 보호는 유지한다.
- 기본 검수의 선택적 방법 코드만 감사 metadata에 추가한다. 검수 사유 원문은 감사 metadata에 넣지 않는다. 기존 호출의 필드 생략을 허용한다.
- DB migration·정책·규칙·기존 데이터 재처리·자동 활성화 없음. 목업과 기존 미추적 파일은 배포 커밋에서 제외한다.

## Gate

- [x] 요청한 코드·테스트·계약 8개 파일만 커밋·푸시. 이전 운영 SHA 대비 별도 기능 변경 없음.
- [x] Java 관련 75개 테스트는 구현 시 통과. 배포 전 동일 입력 재검증은 UP-TO-DATE. `bootJar` 빌드 성공.
- [x] 배포 전 Node 전환 회귀 테스트 30/30, `git diff --check` 통과.
- [x] 사전 읽기 전용 점검 [38195077387](https://github.com/FrostyCityMan/saneB/actions/runs/38195077387) 성공. V91·migration 실패 0, 활성 첨부 작업 0. 설치/bundle SHA-256 `7e22d65f419832708fdc84e0dc7cf243a4d306638e2712d7e8d653848cc72f9c` 일치. 정책 COLLECT_ONLY v2 및 기존 worker/source batch 설정 유지. 오래된 RUNNING 스케줄 1건은 변경하지 않는다. 진단 writes=0/ROLLBACK.
- [x] 사전 HTTP health 200/UP, 비인증 보호 API 401.
- [x] Linux CI [38195143672](https://github.com/FrostyCityMan/saneB/actions/runs/38195143672) 전체 성공. 단위·HTTP·실제 임시 DB·migration·artifact, 독립 실행기, 정책 QA 연결·취소·정리 검증 통과. 필수 DB 7개 suite 280/280 통과(실패·오류·생략 0).
- [x] 고정 태그 `deploy-approved-c81bc3e-20261012`의 운영 배포 [38196104573](https://github.com/FrostyCityMan/saneB/actions/runs/38196104573) 성공. 배포 전 workflow 재테스트·빌드 통과, CodeDeploy `d-5OFW3Z5BL` Succeeded. `attachment_qa=false`.
- [x] 사후 HTTP health 200/UP, 비인증 보호 API 401. 운영 `/js/saneb-announcement-conversion.js`는 승인 소스와 줄바꿈 정규화 후 일치하며 SHA-256은 `b9e17754d9f85da0beca375b8fd3350ea6acc87033a222d79373e92e24b0098e`다.
- [x] 사후 JAR·DB 읽기 전용 점검 [38196514877](https://github.com/FrostyCityMan/saneB/actions/runs/38196514877) 성공. 설치/bundle SHA-256 `93ce5711b5e5d97483488a75072770f5c709b29a52ba021d8bfde40619c04624` 일치. 복구 JAR는 배포 직전 지문과 일치한다. DB V91·migration 실패 0, 정책 COLLECT_ONLY v2/hash 및 worker/source batch 설정 유지, 활성 첨부 작업 0. 진단 writes=0/ROLLBACK. 서비스 시작 2026-10-12 11:36:25 KST.

## 판정과 실행 결과

Decision: **Conditionally ready** — 요청한 운영 설치와 서버 사후 검증 완료. 남은 배포 차단 사항은 없으나 실제 브라우저 렌더링·운영 공고 전환 쓰기 테스트는 미실행이다.

- 실행: `node --test scripts/qa/attachment-conversion.test.mjs` → 30/30 통과.
- 실행: `gradlew.bat :test` 관련 5개 suite 및 `bootJar --no-daemon --max-workers=1` → 성공. Java 75개 테스트는 동일 입력으로 UP-TO-DATE, JAR 빌드는 실행됨.
- 실행: `gh workflow run deploy.yml --ref deploy-approved-c81bc3e-20261012 -f attachment_qa=false` → 성공.
- 실행: 기존 고정 진단 workflow 사전·사후 조회, Node HTTP health/JS/401 비교 → 성공. Node와 단일 실행 Gradle 프로세스 종료.
- 현재 코드는 일반 검수 입력 2종만 표시한다. 실제 자료 오류가 있으면 필수 추가 확인이 나타나는 것은 의도된 동작이다.

## 실행 및 복구 기준

기존 Actions OIDC/CodeDeploy 경로에서 승인 코드 고정 태그로 배포한다. `attachment_qa=false`로 운영 외부 파일 QA를 실행하지 않는다. `backup.sh`는 기존 JAR를 `app.jar.previous`로 보존하며 `validate.sh`는 health 실패 시 기존 JAR 복구를 시도한다. 이번 변경은 DB schema/정책을 변경하지 않는다.

브라우저 QA는 현재 요청의 명시적 지시가 없어 정책상 미실행이다. 서버 테스트·정적 리소스 HTTP 검증을 실제 브라우저 렌더링이나 운영 데이터 전환 성공으로 표현하지 않는다.
