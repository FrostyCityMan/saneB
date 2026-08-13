# saneB Full Development Roadmap v2

작성일: 2026-06-02

## 1. 전환 결정

saneB는 2026-06-02부터 MVP 범위 유지 단계에서 완전 개발 목표 단계로 전환한다.

이 전환은 기능을 무제한으로 추가한다는 뜻이 아니다. 기존 DB-first, API 계약 우선, 서버 권한 검증, 운영 secret 환경변수 관리 원칙을 유지하면서, MVP에서 보류했던 운영 필수 기능을 정식 제품 범위로 승격한다.

## 2. 현재 구현 기준선

현재 저장소 기준으로 구현된 주요 도메인은 다음과 같다.

| 영역 | 구현 상태 | 근거 |
|---|---|---|
| 인증 | 구현됨 | 로그인, 회원가입, 로그아웃, 비밀번호 변경, `auth/me`, 관리자 bootstrap |
| 사용자 대시보드 | 1차 구현됨 | `/api/v1/dashboard/me/...`, 사용자 행동 중심 화면 |
| 관리자/운영 화면 | 구현됨 | 관리자/운영자/승인자/검수자 진입 화면 분리, 사용자·권한·로그·운영 큐 조회 |
| 공고 등록 | 구현됨 | 공고 기본정보, 조건, 필요서류, 진행 단계, 버튼, 수동 상태 |
| 공고별 동적 입력 | 구현됨 | 공고별 입력 요구사항, option, 진행건별 입력값 |
| 파트너 검증 | 구현됨 | 검증 생성, 회원/사업/가족/제한/서류 값 저장, 상태 변경 |
| 매칭 케이스 | 구현됨 | 승인 공고와 회원 기반 수동 매칭, 검증 ID 선택 입력, 조회/상태 변경 |
| 신청 진행 | 구현됨 | progress 생성, 단계 상태, 체크리스트, 공고 단계 버튼 기반 행동 처리, 접수, 결과 저장 |
| 감사 로그 | 구현됨 | 주요 업무 action metadata 기록, 관리자/승인자 조회 화면 |
| 가입 전 후보 확인 | 구현됨 | `/api/v1/pre-signup/candidate-preview`, 개인정보 미저장 임시 후보 수/금액 범위 |
| 장기 미진행/TM 재접촉 | 구현됨 | 24h/48h/7d/14d 스케줄러, 인앱 알림과 운영 큐 |
| 배포 | 부분 구현됨 | Aurora RDS/CI-CD/CodeDeploy 문서와 스크립트 기반 운영 준비 |

현재 구현은 MVP 운영 테스트가 가능한 수준이다. 완전 개발에서는 사용자, 파트너, 운영자, 관리자 각각의 실제 업무 흐름을 끝까지 닫는 것을 목표로 한다.

## 3. 완전 개발 제품 목적

saneB의 완전 개발 목표는 정부지원사업 공고 입력, 대상자 검증, 후보 확인, 최종 매칭, 신청 실행, 결과 관리, 운영 재접촉까지 하나의 업무 플랫폼에서 처리하는 것이다.

제품은 세 가지 운영 성과를 목표로 한다.

1. 운영자는 진행 중이거나 예정된 공고를 빠르게 입력하고 조건/단계/서류를 유동적으로 관리한다.
2. 사용자는 자신이 지금 해야 할 행동을 하나씩 수행하며 검증, 접수, 보완, 결과 확인을 진행한다.
3. 관리자와 파트너는 검증, 매칭, 상담, 서류, 결과, 재접촉 상태를 누락 없이 추적한다.

## 4. 역할 정의

| 역할 | 목적 | 핵심 화면 |
|---|---|---|
| `USER` | 후보 확인, 검증 요청, 서류 제출, 단계별 신청 행동 수행 | 사용자 대시보드, 검증 진행, 신청 진행 상세, 결제/예약 |
| `PARTNER` | 사용자 검증값 입력, 전자증명/서류 확인, 상담 수행 | 파트너 검증 입력, 상담 일정, 보완 요청, 진행 상세 |
| `OPERATOR` | 공고 입력, 조건 관리, 매칭 실행, 신청 진행 운영 | 공고 입력, 매칭 관리, 신청 진행 관리, 운영 큐 |
| `APPROVER` | 검증/매칭/결과의 승인 또는 반려 | 승인 큐, 감사 로그, 결과 검토 |
| `REVIEWER` | 테스트 데이터 기준 조회와 흐름 검수, 상태 확인 | 검수 대시보드, 매칭/검증/신청 진행 조회 |
| `ADMIN` | 전체 운영 설정, 사용자/권한/리포트/배포 운영 관리 | 관리자 대시보드, 사용자 관리, 감사 로그, 운영 리포트 |

역할별 defaultRoute 1차 분리는 현재 구현된 화면 기준으로 완료한다. 사용자는 사용자 대시보드, 파트너는 검증 목록, 운영자는 운영자 대시보드, 승인자는 승인자 큐, 검수자는 검수 대시보드, 관리자는 관리자 대시보드로 진입한다.

| 역할 | 현재 defaultRoute |
|---|---|
| `USER` | `/app/dashboard` |
| `PARTNER` | `/app/partner/verifications` |
| `OPERATOR` | `/app/operator/dashboard` |
| `APPROVER` | `/app/approver/reviews` |
| `REVIEWER` | `/app/reviewer/dashboard` |
| `ADMIN` | `/app/admin/dashboard` |

## 5. 전체 제품 범위

### 5.1 사용자 제품

- 회원가입, 로그인, 로그아웃, 비밀번호 변경
- 약관/개인정보/전자증명/신용조회 동의 이력 저장
- 회원 기본 프로필, 사업자 프로필, 가족 프로필 입력
- 회원가입 전 임시 후보 확인
- 구독/결제 상태 확인
- 상담 예약
- 파트너 검증 진행 상태 확인
- 파일형 서류 제출
- 동적 입력 항목 작성
- 신청 진행 단계별 행동 수행
- 보완 요청 대응
- 결과 확인 및 수령 금액 확인
- 신규 가능 항목 알림 수신

### 5.2 파트너 제품

- 파트너 검증 대상 목록
- 회원/사업/가족 검증값 입력
- 전자증명/필수서류 확인
- 제한 플래그 입력
- 검증 승인/반려 요청
- 상담 일정 관리
- 보완 요청 작성
- 진행 단계 상태 확인

### 5.3 운영자 제품

- 공고 등록/수정/승인 요청
- 공고별 조건, 필요서류, 진행 단계, 버튼, 동적 입력 항목 관리
- 공고 수동 상태 제어
- 매칭 케이스 생성/조회/상태 변경
- 신청 진행 생성/상태 관리
- 장기 미진행 사용자 큐
- 재접촉/TM 큐
- 보완 요청 큐
- 운영 알림 발송
- 조건 유형별 샘플 공고 seed 관리

### 5.4 관리자 제품

- 관리자 대시보드
- 사용자/파트너/운영자/승인자/검수자 계정 관리
- 역할 부여 및 회수
- 감사 로그 조회
- 운영 리포트
- 시스템 설정
- 배포/운영 상태 확인

## 6. v2 DB 확장 후보

운영 migration은 additive 방식으로만 진행한다. 기존 V1/V4 테이블과 API 계약을 깨지 않는다.

| 우선순위 | 도메인 | 신규 테이블 후보 |
|---|---|---|
| P0 | 권한/라우팅 | `role_default_routes`, `user_role_assignments_history` |
| P0 | 관리자 대시보드 | 별도 저장 테이블 없이 집계 우선, 필요 시 `admin_dashboard_snapshots` |
| P1 | 동의 이력 | `user_consents`, `consent_versions` |
| P1 | 파일/서류 | `stored_files`, `document_submissions`, `document_submission_reviews` |
| P1 | 상담 예약 | `partner_availability_slots`, `consultation_reservations`, `consultation_histories` |
| P2 | 구독/결제 | `subscription_plans`, `user_subscriptions`, `payment_transactions`, `refund_transactions` |
| P2 | 알림 | `notification_templates`, `notification_messages`, `notification_delivery_logs` |
| P2 | 운영 큐 | `operation_tasks`, `operation_task_comments`, `operation_task_assignments` |
| P3 | 리포트 | `report_exports`, `admin_report_snapshots` |
| P3 | AI 보조 | `ai_assist_requests`, `ai_assist_results` |

AI 보조 도메인은 개인정보 원문 외부 전송 금지, provider 교체 가능 service 계층, 비식별 audit metadata 저장 Gate를 통과한 뒤에만 착수한다.

## 7. v2 API 확장 후보

모든 신규 API는 `/api/v1/...`의 기존 계약을 깨지 않는 범위에서 additive로 추가한다. 기존 계약을 의미상 변경해야 하면 `/api/v2/...`로 분리한다.

| 우선순위 | API 영역 | 후보 endpoint |
|---|---|---|
| P0 | 관리자 대시보드 | `GET /api/v1/admin/dashboard/summary`, `GET /api/v1/admin/dashboard/queues` |
| P0 | 역할 라우팅 | `GET /api/v1/auth/me`, role별 `defaultRoute` 확장 |
| P0 | 가입 전 후보 확인 | `POST /api/v1/pre-signup/candidate-preview` |
| P1 | 동의 | `GET /api/v1/consents/current`, `POST /api/v1/users/me/consents` |
| P1 | 파일/서류 | `POST /api/v1/files`, `POST /api/v1/document-submissions`, `PATCH /api/v1/document-submissions/{id}/review` |
| P1 | 상담 예약 | `GET /api/v1/consultation-slots`, `POST /api/v1/consultation-reservations`, `PATCH /api/v1/consultation-reservations/{id}/status` |
| P2 | 결제/구독 | `GET /api/v1/subscription-plans`, `POST /api/v1/subscriptions`, `POST /api/v1/payments`, `POST /api/v1/refunds` |
| P2 | 알림 | `GET /api/v1/notifications/me`, `PATCH /api/v1/notifications/{id}/read`, `POST /api/v1/admin/notifications/send` |
| P2 | 운영 큐 | `GET /api/v1/operation-tasks`, `PATCH /api/v1/operation-tasks/{id}/status` |
| P3 | 리포트 | `GET /api/v1/admin/reports/summary`, `POST /api/v1/admin/reports/exports` |

## 8. 화면 IA

| 영역 | 화면 |
|---|---|
| 인증 | 로그인, 회원가입, 비밀번호 변경, 비밀번호 초기화 |
| 사용자 | 사용자 대시보드, 프로필 입력, 동의 관리, 가입 전 후보 확인, 검증 진행, 상담 요청, 결제/구독, 신청 진행 상세, 알림 목록 |
| 파트너 | 파트너 대시보드, 검증 목록, 검증 입력, 상담 일정, 보완 요청 |
| 운영자 | 운영자 대시보드, 공고 입력, 공고 목록/상세, 매칭 케이스 목록/상세, 신청 진행 목록/상세, 상담 수기 배정, 운영 큐 |
| 승인자 | 승인 큐, 검증 승인, 매칭 승인, 결과 승인, 감사 로그 |
| 검수자 | 검수 대시보드, 매칭/검증/신청 진행 조회 |
| 관리자 | 관리자 대시보드, 사용자 관리, 권한 관리, 시스템 설정, 감사 로그, 리포트 |

## 9. 단계별 개발 로드맵

### Phase 0. 기준 정리

목표: MVP 문서와 완전 개발 문서의 경계를 명확히 한다.

작업:
- 완전 개발 로드맵 문서 확정
- role별 defaultRoute 정책 확정
- 기존 `/api/v1/...` 유지 범위와 `/api/v2/...` 분리 기준 확정
- full development Gate 체크리스트 생성

완료 조건:
- 제품 범위, 역할, DB 후보, API 후보, 화면 IA, 단계별 개발 순서가 문서화되어 있다.
- 기존 MVP 계약을 깨는 변경이 없다.

### Phase 1. 권한별 운영 홈

목표: 사용자 대시보드와 운영 계정 화면을 완전히 분리한다.

작업:
- `ADMIN`, `OPERATOR`, `PARTNER`, `APPROVER`, `REVIEWER` defaultRoute 1차 분리 완료
- 관리자 대시보드 API/화면 완료
- 운영자 대시보드 API/화면 완료
- 파트너 검증 목록 화면 개선 완료
- 승인자 큐 화면

완료 조건:
- 관리자에게 사용자 행동카드나 전자증명 행동 CTA가 노출되지 않는다.
- 각 역할은 로그인 직후 자기 업무 화면으로 진입한다.

### Phase 2. 동의/프로필/파일 서류

목표: 사용자 검증과 서류 제출을 실제 운영 가능한 수준으로 만든다.

작업:
- 동의 버전과 사용자 동의 이력 저장
- 파일 업로드 저장소 환경변수 분리
- 서류 제출/검토 테이블
- 사용자 서류 제출 화면
- 파트너/운영자 서류 검토 화면

완료 조건:
- 파일 원문은 감사 로그에 저장되지 않는다.
- 저장 경로와 공개 URL 정책이 분리된다.
- 필수 서류 누락 시 진행 단계 전환이 서버에서 차단된다.

### Phase 3. 상담 예약

목표: 파트너 첫 상담과 후속 상담을 시스템 상태로 관리한다.

작업:
- 파트너 가능 시간 관리
- 사용자 상담 요청 생성/취소
- 운영자/관리자 담당자와 시간 수기 배정
- 파트너/운영자 예약 확정
- 상담 이력 기록
- 예약 상태 기반 대시보드 current action 연동

완료 조건:
- 중복 예약이 DB 제약 또는 transaction으로 차단된다.
- 예약 취소/변경 이력이 남는다.

### Phase 4. 구독/결제

목표: 유료 서비스 운영을 위한 결제와 구독 상태를 도입한다.

작업:
- 요금제, 구독, 결제, 환불 테이블
- TossPayments 기준 provider abstraction
- 결제 성공/실패/환불 webhook 처리. 단, TossPayments 실연동은 상점 key, webhook, redirect URL 확정 뒤 연결
- 구독 상태 기반 진행 제한 정책
- 결제 화면과 관리자 결제 조회

완료 조건:
- 결제 secret은 환경변수로만 관리된다.
- webhook 검증 실패는 처리되지 않는다.
- 결제 실패 시 사용자 행동이 명확히 표시된다.

### Phase 5. 알림과 운영 큐

목표: 진행 지연과 보완 요청을 운영 업무로 자동 정리한다.

작업:
- 알림 template/message/delivery log
- 사용자 알림 목록
- 이메일/SMS/카카오 provider abstraction
- 장기 미진행 큐
- 보완 요청 큐
- 재접촉/TM 큐

완료 조건:
- 개인정보 원문은 provider payload에 필요한 최소값만 포함한다.
- 24시간/48시간/7일/14일 기준 미진행 분류가 중복 없이 기록된다.
- 사용자가 행동을 완료하면 이후 리마인드 기준 시간이 다시 계산된다.
- 발송 실패와 재시도 상태가 기록된다.

### Phase 6. 리포트/감사/운영 hardening

목표: 운영자가 제품 상태를 보고 감사 가능한 수준으로 만든다.

작업:
- 감사 로그 조회 화면
- 관리자 리포트
- CSV/Excel export
- CSRF hardening
- rate limit
- 권한 regression test
- 운영 health/readiness 점검

완료 조건:
- 운영자가 사용자별/공고별/단계별 상태를 추적할 수 있다.
- 보안 Gate 통과 전 외부 배포 확대를 하지 않는다.

### Phase 7. AI 보조 기능

목표: 운영 보조 기능으로만 AI를 제한적으로 도입한다.

작업:
- 공고 요약 보조
- 필요서류 초안 추천
- 운영자 메모 요약
- 사용자 문의 답변 초안

금지:
- AI 자동 승인
- AI 자동 탈락
- 개인정보 원문 외부 전송
- 선정확률, 우선순위, 추천도, 가점 자동 계산

완료 조건:
- AI 결과는 운영자 검토 전 사용자에게 확정값으로 표시되지 않는다.
- audit metadata는 비식별 정보만 저장한다.

## 10. 개발 체크리스트

| 항목 | 상태 | 다음 조치 |
|---|---|---|
| 완전 개발 범위 문서 | 완료 | 본 문서 기준으로 세부 계약 작성 |
| role별 defaultRoute | 완료 | USER/PARTNER/OPERATOR/APPROVER/REVIEWER/ADMIN 기본 진입점 분리 |
| 관리자 대시보드 | 완료 | 관리자 전용 운영 집계 화면과 `/api/v1/admin/dashboard/summary` 유지 |
| 운영자 대시보드 | 완료 | 공고/매칭/신청 진행 업무 홈과 `/api/v1/operator/dashboard/summary` 유지 |
| 파트너 대시보드 | 진행 중 | 검증 목록 화면 완료, 상담/예약은 이후 구현 |
| 검수자 계정/화면 | 완료 | `REVIEWER` 역할, `/app/reviewer/dashboard`, 조회 전용 흐름 적용 |
| 승인자 큐 | 완료 | 공고 승인, 검증 검토, 매칭 확인, 결과 대기 집계와 `/api/v1/approver/reviews/summary` 유지 |
| 검증 없는 수동 매칭 | 완료 | 승인 공고/회원 선택으로 매칭 생성, 검증 ID 선택값 유지 |
| 신청 진행 버튼 UX | 완료 | 버튼 코드 직접 입력 제거, 공고 단계 버튼 표시 및 테스트 검증 완료 |
| 동의 이력 | 완료 | `consent_versions`, `user_consents`, `/api/v1/consents/current`, `/api/v1/users/me/consents` 유지 |
| 파일 업로드 | 완료 | `stored_files`, `document_submissions`, `document_submission_reviews`, `/api/v1/files`, `/api/v1/document-submissions` 적용 |
| 조건 유형별 샘플 공고 | 완료 | local/dev seed에 업력·매출, 지역 제한, 가족 조건, 중복 제한, 예산 소진형 샘플 공고 적용 |
| 가입 전 후보 확인 | 완료 | `/api/v1/pre-signup/candidate-preview`, 개인정보 미저장 후보 수/예상 지원금 범위 적용 |
| 상담 예약 | 완료 | 사용자 요청 접수 후 운영자/관리자 수기 배정 방식으로 `ASSIGNED` 상태 적용 |
| 구독/결제 | 부분 완료 | 월 단순 구독 DB/API와 Toss provider 코드 적용. TossPayments 실결제는 Payment Hardening Gate 보류 |
| 알림 | 완료 | `notification_templates`, `notification_messages`, `notification_delivery_logs`, `/api/v1/notifications/me`, `/api/v1/admin/notifications/send` 적용 |
| 운영 큐 | 완료 | `operation_tasks`, `operation_task_comments`, `operation_task_assignments`, `/api/v1/operation-tasks`, 장기 미진행/TM 재접촉 자동 분류 적용 |
| 감사 로그 화면 | 완료 | 관리자/승인자 조회 화면과 `/api/v1/audit-logs` 목록/상세 유지 |
| 관리자 리포트 | 완료 | `report_exports`, `admin_report_snapshots`, `/api/v1/admin/reports/summary`, `/api/v1/admin/reports/exports` 적용 |
| 앱 로그 관리 | 완료 | 관리자 전용 `/app/admin/app-logs`, `/api/v1/admin/app-logs` 읽기 전용 조회 적용 |
| 운영 readiness | 완료 | Actuator liveness/readiness probe 설정 적용 |
| 서버 화면 CSRF | 완료 | 서버 form과 `/logout` CSRF 적용 |
| API CSRF header | 완료 | 브라우저 세션 `/api/v1/**`, `/api/v2/**` 변경 요청의 `X-XSRF-TOKEN` 검증 적용 |
| API rate limit | 완료 | `/api/v1/**`, `/api/v2/**` 기본 요청 제한 적용, 환경변수로 제한값과 사용 여부 조정 |
| AI 보조 | 완료 | `ai_assist_requests`, `ai_assist_results`, `/api/v1/ai-assist/...` 적용. 기본 provider는 외부 호출 없는 `LOCAL_SAFE` |
| 공고 수집·분류 V2 | 로컬 구현·검증 완료, 운영 보류 | V63~V68, 공통 규칙 엔진, 다중 태그, 판정 근거, 규칙 관리·QA Gate, source 전환 멱등성 구현. 전체 테스트·`bootJar`·PostgreSQL 16 Flyway 통합 검증 통과. 운영 DB 적용·초기 DRAFT 활성화·재분류 실행은 별도 승인 필요 |

## 11. Gate 정책

### DB Gate

- Flyway migration이 schema source of truth다.
- 신규 테이블은 PK/FK/index/unique/check constraint를 함께 확정한다.
- 운영 migration에 테스트 계정이나 샘플 업무 데이터를 넣지 않는다.
- local/dev seed는 profile별 seed 경로만 사용한다.
- 개인정보와 audit metadata를 분리한다.

### API Gate

- 모든 응답은 `ApiResponse<T>` 또는 `PageResponse<T>`를 사용한다.
- 기존 `/api/v1/...` 계약을 깨지 않는다.
- 의미 변경이 필요하면 `/api/v2/...`를 사용한다.
- 권한 검증은 서버에서 수행한다.

### Frontend Gate

- Backend API 계약이 확정된 화면부터 구현한다.
- role/defaultRoute를 추측하지 않는다.
- `th:text`를 사용하고 `th:utext`는 사용하지 않는다.
- 모바일 360px 기준으로 레이아웃을 검증한다.

### Security Gate

- 운영 secret은 환경변수 또는 systemd EnvironmentFile로만 관리한다.
- 파일 저장 경로는 환경변수로 외부화한다.
- 서버 화면 CSRF와 브라우저 세션 API CSRF header 검증을 적용했다.
- 결제/파일/AI provider 호출은 service 계층으로 분리한다.

## 12. 다음 구현 우선순위

1. 공고 수집·분류 V2 격리 QA: 운영 DB 반영 전 migration 백업·rollback 절차, 초기 DRAFT 검수, dry-run 영향 건수, DB 기록이 필요한 경우 내부 QA write·cleanup 절차, 네트워크 egress의 private-range 차단을 확정한다.
2. 운영 승인 후 규칙 활성화와 제한된 신규 수집 canary를 실행한다. 기존 원문 일괄 재분류는 별도 실행 승인 전 금지한다.
3. 전체 운영 실사용 QA
4. TossPayments 실결제 계약 확인 후 Payment Hardening Gate
5. 외부 AI provider 연동 여부 결정

파일형 서류 제출/검토, 상담 수기 배정, 월 단순 구독 DB/API, 알림과 운영 큐, 장기 미진행/TM 재접촉, 관리자 리포트, 서버 화면 CSRF, API CSRF header, API rate limit, AI 보조 DB/API는 완료했다. 공고 수집·분류 V2는 로컬 코드와 migration 구현까지만 완료했으며 운영 DB 반영, 초기 DRAFT 활성화, 재분류 실행은 아직 수행하지 않았다. 다음 Gate는 격리 QA와 영향도 검증이다. TossPayments 실결제 연결은 상점 key, webhook URL, 성공/실패 redirect URL, 자동결제 정책이 확정된 뒤 진행한다. 외부 AI provider 연동은 개인정보 원문 전송 금지 정책과 provider/secret 계약을 별도 승인한 뒤 진행한다.
