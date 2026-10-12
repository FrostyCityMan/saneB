# 회원·담당자 성함·아이디 표시 운영 반영

## 범위와 구현 Gate

- 사용자 요청: 상담 관리와 점검된 매칭 관리·검증 목록·검증 입력·검증 진행을 성함·아이디 표시로 구현하고 운영 반영한다.
- 시작 HEAD: `d34308c75f823cd5e91f881832207b3eea354b01`. 기존 미추적 문서·목업·AWS 스크립트·output은 보존하고 배포 커밋에 포함하지 않는다.
- Design Read: 기존 Thymeleaf·공통 CSS 구조를 유지하며 사람을 성함(아이디)으로 확인하고 명시적으로 선택하게 한다. 이번에는 상담 목록/상세 분리 전체를 배포하지 않는다.
- 대상 과업: 운영자의 회원 검색·상담 접수·수기 배정, 파트너와 사용자의 해당 검증 건 확인. 한국어·기존 반응형 유지. 읽기는 R0, 실제 배정은 R2로 변경 대상 확인을 제공한다.
- 비목표: DB migration, 사용자 코드·PK 변경, 데이터 재처리, 권한 확대, 정책 변경, 자동 배정·활성화, 전체 상담 화면 분리.
- 조회·선택: 이름/아이디 병기, 동명이인 구분, 검색 결과 없음/오류 분리, 이전/다음 페이지, 늦은 응답 무시, 검색어 변경 시 선택 해제. 담당자 변경 실패 시 기존 배정은 변경하지 않는다.
- 기존 v1 코드/UUID 입력과 필드는 유지한다. 새 UI만 UUID 선택값을 사용한다. 담당자 조회는 ACTIVE PARTNER/OPERATOR/ADMIN 후보이며 ADMIN/OPERATOR만 조회한다. 기존 v1의 담당자 존재·권한·slot 검증 정책 자체를 강화했다고 주장하지 않는다.
- 새 의존성 없음. 이름·요청·검색 결과는 textContent 또는 th:text로 렌더링한다. 전화·이메일·개인정보 원문을 새 로그에 기록하지 않는다.

## 검증 및 배포 체크리스트

- [x] AGENTS.md, 실제 template/JS/DTO/VO/Service/Mapper/기존 테스트와 배포 경로 확인.
- [x] Git 시작 기준 및 origin 차이 0/0 확인. 기존 변경 보존.
- [x] 사전 읽기 전용 운영 진단 [38209872667](https://github.com/FrostyCityMan/saneB/actions/runs/38209872667) 성공. 서울, 설치/bundle SHA-256 `6ad82b18c7fa5cd85c9f4bec283bfa9053cc78571becee1e37a8c3bce4bbe45b` 일치, V91·migration 실패 0, 복구 JAR 존재. 활성/대기 첨부 작업 0, 정책 COLLECT_ONLY v2, worker/source batch 설정 유지. 오래된 RUNNING 일정 1건은 이번 범위에서 변경하지 않는다. writes=0/ROLLED_BACK.
- [x] 1차 관련 Java/API/view 회귀 및 bootJar 성공.
- [x] Node 선택기·표시·정적 회귀 9/9 성공.
- [x] 추가 Java 권한·이름 표시·회귀 재검증 31건 통과. 실제 DB 테스트 2건은 로컬 환경 조건에 따라 생략됐으며 성공으로 집계하지 않는다. 신규 임시 PostgreSQL 테스트는 Linux CI에서 누락·생략도 차단한다.
- [x] Linux 실제 임시 PostgreSQL: 새 이름 조인·생성자 매핑, 동일 이름, 역할/활성 후보 필터, 페이지, 미배정, 과거 비활성 담당자 표시. `PERSON_IDENTITY_DB_PASS` 확인.
- [x] Linux 전체 CI [38210353942](https://github.com/FrostyCityMan/saneB/actions/runs/38210353942) 성공. 필수 DB 계약 281건 실패·오류·생략 0. 독립 격리 실행 259건도 통과했다(중복 실행이므로 281건과 합산하지 않는다).
- [x] 고정 코드 SHA `27426e85a78b548d866ea1034edc4ca4617c6be5`, 태그 `deploy-approved-27426e8-20261012`로 운영 배포 [38211828543](https://github.com/FrostyCityMan/saneB/actions/runs/38211828543) 성공. CodeDeploy `d-J4FTZL9BL` 및 lifecycle 모두 Succeeded.
- [x] 배포 전후 DB/설정·설치/bundle/복구 JAR 확인, HTTP health와 정적 자산 대조.
- [ ] 브라우저 QA: 현재 요청에 명시 지시가 없어 정책상 미실행. 서버 렌더링/Node 테스트를 브라우저 QA로 표현하지 않는다.

성공 기준: 5개 화면의 사람 코드 표시 제거, 상담 회원·담당자 명시 선택, 기존 API 호환·조회 권한 유지, 관련 자동 검증과 동일 SHA 운영 설치·health 확인.

실패 기준: 이름을 DB 식별자로 사용, 코드 라벨만 바꾸고 코드 입력을 유지, 첫 검색 결과 자동 선택, 일반 사용자에게 후보 디렉터리 개방, 실패/미확인 배포를 완료로 보고.

## 복구 및 검증 경계

CodeDeploy의 기존 backup.sh/validate.sh와 `app.jar.previous`를 유지한다. DB 변경이 없어 역 migration은 없다. 운영 로그인·화면 클릭·실제 상담 등록/배정은 이번 비브라우저 검증으로 증명하지 않는다. Node/Gradle은 단일 실행 후 종료한다. 배포 완료 결과는 아래에 후속 기록한다.

## 실행 근거

- `node --test scripts/qa/person-identity.test.cjs`: 9/9 통과. `node --check`로 상담·매칭 JS 문법 확인.
- `./gradlew.bat :test --tests '*Consultation*' --tests '*PartnerVerification*' --tests '*PersonIdentity*' --tests '*PersonDisplayName*' --tests '*MatchingViewControllerSmokeTest' bootJar --no-daemon --max-workers=1 --console=plain`: 관련 Java 31건 통과, 환경 제한 2건 생략. 새 실제 DB 테스트는 위 Linux CI에서 별도 성공 확인.
- `./gradlew.bat compileJava bootJar --no-daemon --max-workers=1 --console=plain`: 최종 컴파일·JAR 생성 성공.
- `git diff --check`: 통과. 기존 미추적 파일은 staging·commit 대상에서 제외.
- 배포 직전 재점검 [38211593511](https://github.com/FrostyCityMan/saneB/actions/runs/38211593511): 2026-10-12 15:15 KST, 최초 점검과 설치/bundle hash·V91·정책/flag 동일, writes=0/ROLLED_BACK.
- 구현 커밋 `7675c37`, 안내 문구 보완 `27426e8`. 첫 SHA의 중복 CI `38210280578`은 취소했으며 최신 SHA CI 성공만 배포 근거로 사용한다.

## 운영 반영 결과

- Gate 판정: **Conditionally ready**. 승인된 코드 설치·비브라우저 검증은 완료했고, 브라우저 QA·실제 운영 접수/배정은 현재 요청 정책에 따라 미실행이다. 전체 사용자 E2E 완료를 뜻하지 않는다.
- 사후 읽기 전용 진단 [38212365800](https://github.com/FrostyCityMan/saneB/actions/runs/38212365800) 성공. 서비스 시작 2026-10-12 15:22:28 KST, 확인 15:24 KST.
- 설치 JAR과 배포 bundle JAR SHA-256: `5f00aeffe70ddcdf0f1db9a1ebc5d5d11f746d6f06bc939b3c6c14638869545e` 일치.
- 복구 JAR SHA-256: `6ad82b18c7fa5cd85c9f4bec283bfa9053cc78571becee1e37a8c3bce4bbe45b`, 배포 전 설치본과 일치.
- DB endpoint 지문, V91·migration 실패 0, COLLECT_ONLY v2 정책 hash/row version, worker/source batch 설정 모두 사전과 동일. 운영 데이터 재처리·정책 게시·새 migration 없음. 진단 writes=0/ROLLED_BACK.
- Node HTTP 조회: `/actuator/health` 200/UP, 비인증 `/api/v1/consultation-reservations/partner-lookups` 401. 응답 개인정보나 인증정보는 기록하지 않았다.
- HTTP로 조회한 `saneb-person-identity.js`, `saneb-person-picker.js`, `saneb-consultations.js`, `saneb-matching-cases.js` 모두 200이며 코드 원본과 일치(CRLF/LF 정규화 비교).
- 변경 파일군: 상담 Controller/Service/DAO/DTO/VO/Mapper, 검증 DTO/VO/Service/Mapper/ViewController, 공통 PersonDisplayName, 화면 template 5개, 공통 사람 표시/선택 JS와 상담/매칭 JS, 관련 Java/Node 테스트, CI·API 계약 문서. 새 DB schema와 의존성은 없다.
- 보존: 기존 미추적 문서·목업·AWS 도구·output은 커밋하지 않았다. 공고/상담/검증 업무 번호는 사람 코드와 다르므로 유지한다. 기존의 오래된 RUNNING 수집 일정 1건은 이번 변경 대상이 아니다.
- 남은 검증 경계: 인증 후 실제 화면의 렌더링·키보드·모바일·등록/배정 저장 클릭은 미확인이다. MockMvc 서버 렌더링과 Node 가상 DOM 테스트를 운영 브라우저 검증으로 대체 표기하지 않는다. 이번 범위의 배포 blocker는 없으며, 실제 사용 검증은 별도 브라우저 QA 요청 시 수행한다.
- 사용한 로컬 Node·Gradle 단발 명령은 종료됐으며 서버·브라우저 프로세스를 새로 시작하지 않았다. 이 결과 기록의 문서 전용 후속 커밋은 운영 설치 코드 SHA와 구분한다.
