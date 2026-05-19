# AGENTS.md

이 문서는 `saneB` 프로젝트에서 Codex가 반드시 준수해야 하는 작업 지침이다.

## 1. 기본 역할

Codex는 AI 소프트웨어 엔지니어로서 사용자의 요청을 하나의 개발 태스크로 받아들이고, 가능한 경우 완료 가능한 상태까지 직접 수행한다.

작업 루프는 다음 순서를 따른다.

1. Understand: 요청과 코드베이스 맥락을 이해한다.
2. Inspect: 관련 파일, 테스트, 설정을 확인한다.
3. Plan: 최소 변경 계획을 세운다.
4. Edit: 기존 패턴에 맞춰 코드를 수정한다.
5. Verify: 테스트, 빌드, 린트를 실행한다.
6. Iterate: 실패 시 원인을 분석하고 수정한다.
7. Report: 변경 사항과 검증 결과를 보고한다.

## 2. 커뮤니케이션 원칙

- 항상 공적이고 전문적인 톤을 사용한다.
- 기술적 사실을 명확하게 말한다.
- 실질적인 태도와 운영 가능성을 우선한다.
- 추측하지 않고 현재 파일시스템과 문서를 먼저 확인한다.
- 최종 답변은 변경 사항, 검증 결과, 남은 blocker 중심으로 작성한다.

## 3. 프로젝트 컨텍스트

- 프로젝트명: `saneB`
- 제품 방향: 추후 사양서와 도메인 정책을 기준으로 확정한다.
- 현재 기준: Hakgojae AI Learning Bank와 동일한 백엔드/프론트엔드 구조 원칙을 적용한다.
- 기본 운영 환경: Ubuntu
- 기본 개발 방향: DB-first, API 계약 우선, 서버 사이드 렌더링 중심

## 4. 기술 스택

- Java 21
- Spring Boot
- Gradle
- Spring Security
- MyBatis
- PostgreSQL
- Flyway
- Thymeleaf
- Bootstrap 5
- logback

## 5. 작업 제약

- 필요 이상으로 넓은 변경을 하지 않는다.
- 기존 사용자 변경사항을 임의로 되돌리지 않는다.
- DB 컬럼명 변경은 사전 협의 없이 수행하지 않는다.
- 운영 secret을 코드나 문서에 기록하지 않는다.
- 완성된 scaffold가 있다고 가정하지 않고 항상 현재 구조를 확인한다.
- MVP 범위를 유지하며, 명시 요청이 있을 때만 확장한다.
- 생성 산출물, 디자인 자료, 바이너리 문서는 사용자가 요청하지 않으면 수정하지 않는다.
- 한국어 업무 용어는 명확한 이유가 없으면 유지한다.

## 6. API 원칙

- 모든 API는 `/api/v1/...` 형식을 따른다.
- 기존 `v1` 계약을 깨지 않는다.
- 계약 변경이 필요하면 기존 API를 수정하지 않고 `v2`를 신규 생성한다.
- 응답은 Response Wrapper를 사용한다.

```json
{
  "success": true,
  "data": {},
  "message": ""
}
```

- 목록 API는 기본적으로 pagination을 포함한다.
- 모바일을 고려해 응답 필드는 최소화한다.

## 7. 계층 구조

기본 흐름은 다음을 따른다.

```text
Controller -> Service -> ServiceImpl -> DAO -> Mapper XML -> PostgreSQL
```

- Controller: URL 매핑, 요청/응답, DTO 변환만 담당한다.
- Service: 외부 계약 interface를 정의한다.
- ServiceImpl: 비즈니스 로직과 transaction을 담당한다.
- DAO: SQL 호출만 담당한다.
- Mapper XML: 실제 SQL을 작성한다.

## 8. Backend 생성 순서

신규 기능 또는 API 생성 시 다음 순서를 따른다.

1. Response Wrapper
2. Controller
3. Service
4. ServiceImpl
5. DAO
6. Mapper XML
7. DTO
8. Thymeleaf View, 필요한 경우
9. DDL/Flyway migration, 필요한 경우

## 9. Package Structure

기본 패키지 구조는 다음을 따른다.

```text
com.saneb.domain
├── controller
├── service
├── service.impl
├── dao
├── dto
├── vo
└── config
```

실제 root package는 프로젝트 생성 시 확정된 `groupId`를 따른다.

## 10. Java Naming

메서드는 아래 접두사를 사용한다.

- 조회: `select`
- 등록: `insert`
- 수정: `update`
- 삭제: `delete`
- 저장: `save`

조회 메서드 접미사:

- 단건: `Details`
- 목록: `List`

예시:

```text
selectUserDetails()
selectUserList()
insertUser()
updateUser()
deleteUser()
saveUser()
```

금지:

```text
getUser()
findUser()
createUser()
removeUser()
```

## 11. MyBatis 규칙

- `SELECT *` 사용 금지
- `${}` 사용 금지
- 모든 parameter binding은 `#{}` 사용
- 모든 SQL에는 명확한 `-- 주석` 작성
- 컬럼명은 실제 DB 컬럼명과 정확히 일치시킨다.
- 결과는 `resultMap` 또는 명확한 DTO 매핑 사용
- 복잡한 검색은 `SearchCondition` DTO 사용
- 동적 SQL은 필요한 경우에만 사용한다.

## 12. Database 규칙

- PostgreSQL 기준으로 작성한다.
- Flyway migration을 schema source of truth로 취급한다.
- 테이블명과 컬럼명은 `snake_case`를 사용한다.
- PK는 `id` 또는 `{table}_id`를 사용한다.
- boolean 컬럼은 `is_` 접두어를 사용한다.
- FK, index, unique constraint를 명확히 설계한다.
- 운영 migration에 테스트 계정을 포함하지 않는다.
- local/dev seed는 별도 profile 또는 별도 seed 경로에 둔다.
- 개인정보와 운영 감사 로그는 명확히 분리한다.

## 13. Security 규칙

- SQL Injection 방지를 위해 `#{}` binding만 사용한다.
- XSS 방지를 위해 Thymeleaf는 `th:text`를 사용한다.
- `th:utext` 사용 금지
- 인증과 권한은 서버에서 검증한다.
- 외부 API key는 환경변수로만 주입한다.
- 개인정보는 외부 API로 전송하지 않는다.
- Content-Security-Policy 적용을 기본으로 고려한다.
- 서버 검증은 필수이며 JavaScript로 우회하지 않는다.

## 14. Thymeleaf / Frontend 규칙

- 서버 사이드 렌더링을 중심으로 작성한다.
- JavaScript 사용은 최소화한다.
- jQuery는 사용할 수 있다.
- 인라인 JavaScript는 지양한다.
- Header/Footer는 fragment로 분리한다.
- Form 처리는 `th:object` 기반으로 작성한다.
- 모바일 360px 기준으로 반응형을 설계한다.
- Bootstrap 5를 사용할 수 있다.
- secret, token, API key를 브라우저에 노출하지 않는다.
- 화면은 Backend API 계약을 기준으로 구현하고 추측하지 않는다.

## 15. Validation & Error Handling

- 요청 검증에는 `@Valid`를 사용한다.
- `GlobalExceptionHandler`를 구현한다.
- 사용자 메시지와 시스템 메시지를 분리한다.
- 에러 응답도 Response Wrapper와 일관성을 유지한다.
- JavaScript로 서버 검증을 우회하지 않는다.

## 16. Logging

- logback을 사용한다.
- INFO: 주요 업무 흐름
- ERROR: 예외
- DEBUG: 개발 환경에서만 허용
- 개인정보와 secret은 로그에 남기지 않는다.

## 17. AI / 외부 API 연동 원칙

- 외부 AI 호출은 교체 가능한 service 계층으로 분리한다.
- API key는 환경변수로만 주입한다.
- 요청 원문과 응답 원문 저장은 최소화한다.
- 감사 로그에는 provider, model, status, token count, latency, hash, 비식별 metadata를 저장한다.
- 사용자 개인정보는 외부 AI에 전달하지 않는다.
- 실패, timeout, rate limit은 명확한 ErrorCode로 처리한다.

## 18. Ubuntu 운영 기준

- 운영 환경은 Ubuntu 기준으로 둔다.
- Java 21 런타임을 명시한다.
- systemd, Nginx, PostgreSQL 배포를 기본 운영 모델로 고려한다.
- Windows 경로 하드코딩을 금지한다.
- 파일 저장 경로는 환경변수로 외부화한다.
- 운영 secret은 systemd EnvironmentFile 또는 서버 환경변수로 관리한다.

예시:

```text
SERVER_PORT=8080
DB_URL=jdbc:postgresql://localhost:5432/saneb
DB_USERNAME=saneb
DB_PASSWORD=...
STORAGE_ROOT=/var/lib/saneb/storage
OPENAI_API_KEY=...
```

## 19. Verification

구현 전에는 현재 프로젝트 구조와 build 파일을 확인한다.

검증 결과는 다음 형식으로 보고한다.

```text
실행 명령:
결과:
남은 blocker:
```

build system이 도입되어 있으면 프로젝트 local wrapper를 사용한다.

Windows 예시:

```powershell
.\gradlew.bat test
```

Ubuntu 예시:

```bash
./gradlew test
```

## 20. Commit Convention

- `feat`: 기능 추가
- `fix`: 버그 수정
- `refactor`: 리팩토링
- `docs`: 문서 변경
- `test`: 테스트 추가/수정
- `chore`: 기타 작업

## 21. Forbidden Actions

- Controller에 비즈니스 로직 작성 금지
- ServiceImpl에서 직접 SQL 작성 금지
- DAO에 비즈니스 로직 작성 금지
- `SELECT *` 사용 금지
- MyBatis `${}` 사용 금지
- `th:utext` 사용 금지
- JavaScript로 서버 검증 우회 금지
- 운영 secret 저장소 기록 금지
- 기존 API 계약 파괴 금지
- 기존 사용자 변경사항 임의 revert 금지
- DB 컬럼명 사전 협의 없는 변경 금지
```

새 프로젝트 첫 프롬프트는 아래처럼 시작하면 됩니다.

**Backend 초기 프롬프트**
```text
saneB 프로젝트를 Java 21, Spring Boot, Gradle, Spring Security, MyBatis, PostgreSQL, Flyway, Thymeleaf 기준으로 초기 설계하세요.

AGENTS.md를 먼저 읽고 준수하세요.
DB-first로 MVP schema 초안을 설계하고, 인증/권한/감사 로그/기본 사용자 구조를 우선 확정하세요.
API는 /api/v1/... 버전 정책과 ApiResponse wrapper를 따르세요.
운영 환경은 Ubuntu 기준이며 secret은 환경변수로만 관리합니다.
다음 산출물을 제시하세요:
1. MVP 도메인 경계
2. DB 테이블 초안
3. API 초안
4. 패키지 구조
5. 구현 순서
6. 검증 방법
```

**Frontend 초기 프롬프트**
```text
saneB 프론트엔드는 Thymeleaf, Bootstrap 5, 모바일 360px 기준으로 설계하세요.

AGENTS.md를 먼저 읽고 준수하세요.
Backend API 계약이 확정되기 전에는 화면 구현을 시작하지 말고 IA와 API 의존성만 정리하세요.
인증, 권한, 라우팅은 Backend 응답만 신뢰하세요.
secret, token, 외부 API key는 브라우저에 노출하지 마세요.
다음 산출물을 제시하세요:
1. 화면 목록
2. 권한별 진입 화면
3. 공통 layout/fragment 전략
4. API 의존성 표
5. 구현 착수 Gate
```
