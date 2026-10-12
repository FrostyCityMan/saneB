# 상담 관리 목록·상세 분리 운영 반영

## 범위

- 사용자 승인: 상담 목록형 구현 이후 운영 반영 요청.
- 기준 HEAD: `849eab869305b43e269801d5b371407feb94e340`.
- 목록 표·상태 필터·20/50건 페이지 이동, 별도 상세와 신규 접수, 기존 성함·아이디 검색을 연결한다.
- 관리자·운영자는 새 목록으로 이동하고 USER/PARTNER의 기존 화면은 유지한다.
- 관리자 v2 상세/저장 API, 원본 timestamp 조건부 갱신, 서버 상태 전이 재사용, 세션 CSRF, 동시 수정 시 입력 보존.
- v1 필수 입력 변경, 신규 migration, 수집/결제 정책 변경, 운영 상담 데이터 저장 QA는 제외한다.
- 기존 미추적 문서·목업·output·AWS 도구는 커밋 대상에서 제외한다.

## Gate 및 검증

- [x] AGENTS.md, 배포/진단 workflow, backup/validate 경로 확인.
- [x] 로컬 상담·인증·CSRF Java 67건, Node 24건, bootJar, git diff --check 통과.
- [x] 별도 임시 PostgreSQL 16 검증 1건: 전체 migration, 실제 조건부 UPDATE, stale 거절, 이력 실패 시 예약·slot rollback, 성공 후 이력 1건 확인. 임시 컨테이너 종료·제거.
- [x] 사전 운영 진단 [38220201777](https://github.com/FrostyCityMan/saneB/actions/runs/38220201777): V91, migration 실패 0, 설치·배포 bundle SHA-256 일치, 복구 JAR 존재, 활성/대기 첨부 작업 0, writes=0/ROLLED_BACK.
- [x] 코드 `89a23577a562a30fdd8a33f502821284f192f10d`, 고정 태그 `deploy-approved-89a2357-20261012`. 배포 [38220399217](https://github.com/FrostyCityMan/saneB/actions/runs/38220399217)의 전체 테스트·빌드 5분 37초 성공 후 CodeDeploy `d-PVS9K5BBL` 성공.
- [x] 사후 진단 [38221178923](https://github.com/FrostyCityMan/saneB/actions/runs/38221178923): 설치/bundle 일치, 이전 JAR은 배포 전 운영본과 일치. V91·migration 실패 0, DB endpoint 지문·정책 hash/version·worker/source batch 설정 동일. writes=0/ROLLED_BACK.
- [x] 운영 health 200/UP. 상담 관리 JS/model JS/CSS 3개 HTTP 200 및 소스 일치(CRLF/LF 정규화).
- [x] 동일 코드의 별도 Linux QA [38220349282](https://github.com/FrostyCityMan/saneB/actions/runs/38220349282) 성공. 필수 DB 계약 281건 실패·오류·생략 0, 독립 산출물의 임시 DB 재검증 259건 실패·생략 0, 사람 식별 실제 DB PASS, 정책 QA 자원 정리 SUCCEEDED. 두 집계는 중복 범위를 포함하므로 합산하지 않는다.
- [x] 상담 전용 `ConsultationManagementDbTest` 1건은 로컬 임시 PostgreSQL 실행 근거이며, 별도 포트가 없는 Linux CI에서는 생략된다. Linux 필수 DB 281건과 구분한다.
- [ ] 브라우저 QA: 현재 요청 명시 지시가 없어 정책상 미실행. MockMvc/Node 검증을 브라우저 성공으로 표현하지 않는다.

## 명령

- `node --test scripts/qa/person-identity.test.cjs scripts/qa/consultation-management.test.cjs`
- `gradlew.bat :test --tests "com.saneb.domain.consultation.*" --tests "com.saneb.db.ConsultationManagementDbTest" :bootJar --no-daemon --max-workers=2` (임시 DB 전용 loopback port 지정 시에만 DB 테스트 실행)
- `gradlew.bat :test --tests "com.saneb.domain.consultation.*" --tests "com.saneb.config.ApiCsrfHeaderFilterTest" --tests "com.saneb.domain.auth.controller.AuthControllerSmokeTest" --tests "com.saneb.domain.auth.controller.AuthViewControllerSmokeTest" --tests "com.saneb.domain.announcementattachment.controller.AnnouncementAttachmentBrowserCsrfIntegrationTest" :bootJar --no-daemon --max-workers=2`
- 최초 비한정 `test` 명령은 첨부 하위 모듈에 상담 필터가 적용되어 실패했다. 위 `:test` 명령으로 범위를 명확히 하여 재검증했다.
- `gh workflow run deploy.yml --ref deploy-approved-89a2357-20261012 -f attachment_qa=false`: 배포 성공.
- `gh workflow run diagnose-codedeploy.yml --ref deploy-approved-89a2357-20261012 -f deployment_id=d-PVS9K5BBL -f ssm_deployment_preflight=true -f ssm_app_log_check=false -f ssm_attachment_qa_check=false`: 읽기 전용 사후 점검 성공.
- `gh run view 38220349282 --json status,conclusion` 및 로그의 필수 DB 결과 확인: completed/success.

## 복구와 제한

- 기존 CodeDeploy backup/validate로 직전 `app.jar.previous`를 보관하고 health 실패 시 복구한다.
- schema 변경이 없어 DB 다운그레이드/데이터 삭제가 필요하지 않다.
- 신규 접수의 v1 멱등성은 추가하지 않는다. 응답 불명 시 자동 POST 재시도 없이 목록을 확인한다.
- UI는 회원 선택과 요청 내용을 요구하지만 v1 API의 기존 선택 입력 계약은 유지한다.
- 실 브라우저 시각·접근성·실제 운영 저장 E2E는 아직 검증하지 않았다.
- 배포 전 운영 JAR SHA-256: `df4d6594cd6d61af7a3c0d8eb1bc98272c4c7ef59e60af7202400a2afd533739`. 사전 health 200/UP. DB endpoint 지문·COLLECT_ONLY v2·worker/source batch 설정을 사후 비교한다.
- 사전 진단의 오래된 RUNNING 일정 1건은 2026-09-07 예약/09-09 수정 기록이며 연결 실행이 없다. 이번 배포의 활성 작업으로 간주하지 않고 보존한다.
- 최초 Git HTTPS push는 로컬 CA 신뢰 오류로 실패했다. `git -c http.sslBackend=schannel push`로 Windows 신뢰 저장소를 사용해 성공했고 SSL 검증은 끄지 않았다.

## 운영 설치 결과

- 서비스 시작: 2026-10-12 17:00:53 KST.
- 설치/bundle SHA-256: `b7b6116fe68daabaf6a3e8cb8c78aa0f9bcb3e98cfaac2a0e802ce81f210c3a6`.
- 복구 JAR SHA-256: `df4d6594cd6d61af7a3c0d8eb1bc98272c4c7ef59e60af7202400a2afd533739`.
- 운영 진입: `/app/admin/consultations`. 기존 운영자 상담 메뉴와 `/app/consultations` 접근은 새 목록으로 연결된다.
- 브라우저 QA·실제 상담 저장 E2E는 정책상 미실행이며, 설치/HTTP 정적 파일/health 검증과 구분한다.
- workflow의 Node 20 기반 action·setup-java v4 deprecated 및 ubuntu-latest 이미지 전환 경고는 배포 실패가 아니다. 런타임 action 업데이트는 이번 배포와 분리한 후속 유지보수 대상으로 남긴다.

## 최종 판정

- Decision: **Conditionally ready**. 운영 반영과 비브라우저 검증은 완료했으며, 브라우저 기반 시각·접근성·상담 저장 E2E는 현재 요청의 명시 지시가 없어 미실행이다.
- 확인된 배포 차단 사항은 없다. 실제 사용자 흐름 전체의 검증 완료를 의미하지 않는다.
- 검증 결과에 따라 설치 JAR·복구본·DB 및 정책 상태를 구분해 기록했다. 추가 DB 변경이나 운영 상담 데이터 쓰기는 수행하지 않았다.
- 실행한 단기 Node 프로세스와 CI 감시 명령은 종료했다. 로컬 QA 임시 DB는 제거했고 Linux QA 자원 정리 성공을 확인했다. 기존 미추적 파일은 보존했다.
