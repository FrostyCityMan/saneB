# 상담 선택 시 현재 상태 동기화 수정

## 범위와 수용 기준

- 요청: 완료 상담을 선택해도 상태 선택란이 담당자 배정으로 남는 문제를 수정하고 운영 반영.
- 시작 HEAD `72e0999c59cf732965ab42b30cc477e207005936`, origin 차이 0/0. 기존 미추적 파일 보존.
- Design Read: 기존 Thymeleaf 상담 화면과 UI 스타일을 유지하고, 조회된 상담의 현재 상태와 편집 선택값을 일치시킨다. 저장은 기존 서버 권한·상태 전이 검증을 유지한다.
- 변경: 선택 및 저장 성공 응답의 statusCode를 select에 반영. 미선택 안내와 조회용 REQUESTED 옵션 추가. 알 수 없는 상태는 빈 값이며 ASSIGNED로 대체하지 않는다. 동일 상태/요청 접수/빈 상태 저장은 전송 전에 구체적으로 안내한다.
- 비목표: DB/API 변경, 상태 전이 정책 변경, 기존 상담 데이터 재처리, 담당자 자동 배정, 운영 데이터 저장 테스트.
- 성공 기준: 완료 등 6개 상태 반영, 재선택 시 이전 상태 제거, 저장 성공 재반영, 실패 시 입력 보존, 관련 테스트·빌드·고정 코드 운영 설치 확인.
- 실패 기준: ASSIGNED 기본값으로 오인 표시, REQUESTED 저장 전송, 기존 API/DB 정책 변경, 미검증 배포/브라우저 QA를 성공으로 표현.

## Gate

- [x] root AGENTS.md, 실제 JS/template/ServiceImpl/기존 테스트/build/배포 흐름 확인.
- [x] Node 14/14 통과 및 JS 문법 확인. 초기 테스트 mock의 실패 응답 구현 오류를 수정한 뒤 재실행했다.
- [x] 상담 Java/Thymeleaf 회귀와 bootJar 성공(1분 3초). 상담 관련 16건 실패·오류·생략 0.
- [x] 사전 운영 읽기 전용 진단 [38215104914](https://github.com/FrostyCityMan/saneB/actions/runs/38215104914) 성공. V91, migration 실패 0, 설치/bundle SHA-256 `5f00aeffe70ddcdf0f1db9a1ebc5d5d11f746d6f06bc939b3c6c14638869545e` 일치. 복구 JAR 존재, 활성/대기 첨부 작업 0. 기존 오래된 RUNNING 일정 1건 보존. writes=0/ROLLED_BACK.
- [x] 고정 코드 `be65b275ab86f4cbf6e24d320a6460620fbc073b`, 태그 `deploy-approved-be65b27-20261012`. 배포 [38215385450](https://github.com/FrostyCityMan/saneB/actions/runs/38215385450)의 전체 테스트·빌드 성공(5분 28초) 후 CodeDeploy `d-F6S740BBL` 성공.
- [x] 사후 읽기 전용 점검 [38216121222](https://github.com/FrostyCityMan/saneB/actions/runs/38216121222) 성공. 외부 health 200/UP, 운영 상담 JS와 코드 원본 일치(CRLF/LF 정규화).
- [x] 별도 Linux 첨부·DB 회귀 [38215354946](https://github.com/FrostyCityMan/saneB/actions/runs/38215354946) 성공. 사람 식별 실제 DB 검증 및 필수 DB 계약 281건 실패·오류·생략 0. 독립 실행 및 정책 QA 정리·취소 검증 포함.
- [ ] 브라우저 QA: 현재 요청 명시 지시가 없어 정책상 미실행. 이전 QA는 결함 재현 근거일 뿐 수정 후 성공 증거가 아니다.

## 명령 및 복구

- `node --test scripts/qa/person-identity.test.cjs`
- `node --check src/main/resources/static/js/saneb-consultations.js`
- `./gradlew.bat :test --tests '*Consultation*' bootJar --no-daemon --max-workers=1 --console=plain`
- `git diff --check`
- 기존 CodeDeploy backup/validate 경로를 유지하며 실패 시 직전 `app.jar.previous` 복구. 새 migration·환경설정·수집 정책 변경 없음.
- 변경 파일: 상담 JS/template, Node 상태 회귀 테스트, ConsultationViewControllerSmokeTest, 이 배포 기록.

## 최종 결과

- Decision: **Conditionally ready**. 승인된 수정·배포·자동 검증 및 운영 health/설치 파일 확인 완료. 수정 후 브라우저·운영 상태 저장 E2E는 미실행이며 완료로 주장하지 않는다. 배포 전 추가 필수 작업 없음.
- 운영 서비스 시작: 2026-10-12 16:06:33 KST. 사후 점검: 16:09 KST.
- 설치/bundle JAR SHA-256 `df4d6594cd6d61af7a3c0d8eb1bc98272c4c7ef59e60af7202400a2afd533739` 일치.
- 복구 JAR SHA-256 `5f00aeffe70ddcdf0f1db9a1ebc5d5d11f746d6f06bc939b3c6c14638869545e`: 배포 전 운영본과 일치.
- DB endpoint 지문, V91·migration 실패 0, 정책 COLLECT_ONLY v2의 hash/row version, worker/source batch flag 모두 사전과 동일. 진단 writes=0/ROLLED_BACK.
- 실제 상담 상태 변경이나 데이터 재처리는 실행하지 않았다. 서버가 거부하는 상태 전이 정책은 그대로이며, 이 수정이 새로운 상태 변경을 허용하지 않는다.
- 미실행: 수정 후 브라우저/실제 운영 상태 저장. 현재 요청에서 브라우저 실행이 명시되지 않아 생략하며 이전 결함 재현 QA와 혼동하지 않는다.
- 로컬 Node와 단발 Gradle 프로세스는 종료했다. 별도 서버·브라우저를 실행하지 않았고 기존 미추적 파일은 보존했다.
