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
- [~] 사전 운영 진단·Linux CI·고정 SHA 배포·사후 설치/health 확인 진행 중.
- [ ] 브라우저 QA: 현재 요청 명시 지시가 없어 정책상 미실행. MockMvc/Node 검증을 브라우저 성공으로 표현하지 않는다.

## 명령

- `node --test scripts/qa/person-identity.test.cjs scripts/qa/consultation-management.test.cjs`
- `gradlew.bat :test --tests "com.saneb.domain.consultation.*" --tests "com.saneb.db.ConsultationManagementDbTest" :bootJar --no-daemon --max-workers=2` (임시 DB 전용 loopback port 지정 시에만 DB 테스트 실행)
- `gradlew.bat :test --tests "com.saneb.domain.consultation.*" --tests "com.saneb.config.ApiCsrfHeaderFilterTest" --tests "com.saneb.domain.auth.controller.AuthControllerSmokeTest" --tests "com.saneb.domain.auth.controller.AuthViewControllerSmokeTest" --tests "com.saneb.domain.announcementattachment.controller.AnnouncementAttachmentBrowserCsrfIntegrationTest" :bootJar --no-daemon --max-workers=2`
- 최초 비한정 `test` 명령은 첨부 하위 모듈에 상담 필터가 적용되어 실패했다. 위 `:test` 명령으로 범위를 명확히 하여 재검증했다.

## 복구와 제한

- 기존 CodeDeploy backup/validate로 직전 `app.jar.previous`를 보관하고 health 실패 시 복구한다.
- schema 변경이 없어 DB 다운그레이드/데이터 삭제가 필요하지 않다.
- 신규 접수의 v1 멱등성은 추가하지 않는다. 응답 불명 시 자동 POST 재시도 없이 목록을 확인한다.
- UI는 회원 선택과 요청 내용을 요구하지만 v1 API의 기존 선택 입력 계약은 유지한다.
- 실 브라우저 시각·접근성·실제 운영 저장 E2E는 아직 검증하지 않았다.
