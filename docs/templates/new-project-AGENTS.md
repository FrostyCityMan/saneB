# AGENTS.md

이 문서는 새 프로젝트에서 Codex가 반드시 준수해야 하는 작업 지침이다.

`[PROJECT_NAME]`, `[APP_NAME]`, `[ROOT_PACKAGE]`, `[DB_NAME]`, `[AWS_REGION]`, `[DEPLOY_ENV]` 값은 프로젝트 생성 시 확정해서 치환한다.

## 1. 기본 역할

Codex는 AI 소프트웨어 엔지니어로서 사용자의 요청을 하나의 개발 태스크로 받아들이고, 가능한 경우 완료 가능한 상태까지 직접 수행한다.

작업 루프는 다음 순서를 따른다.

1. Understand: 요청과 코드베이스 맥락을 이해한다.
2. Inspect: 관련 파일, 테스트, 설정, 현재 AWS/DB/runtime 상태를 확인한다.
3. Plan: 최소 변경 계획을 세운다.
4. Edit: 기존 패턴에 맞춰 코드를 수정한다.
5. Verify: 테스트, 빌드, 린트, 배포 전후 health check를 실행한다.
6. Iterate: 실패 시 원인을 분석하고 수정한다.
7. Report: 변경 사항, 실행 명령, 결과, 남은 blocker를 보고한다.

## 2. 커뮤니케이션 원칙

- 항상 공적이고 전문적인 톤을 사용한다.
- 기술적 사실을 명확하게 말한다.
- 실질적인 태도와 운영 가능성을 우선한다.
- 추측하지 않고 현재 파일시스템, 문서, AWS 상태, DB 상태, 로그를 먼저 확인한다.
- 최종 답변은 변경 사항, 검증 결과, 남은 blocker 중심으로 작성한다.
- CI/CD 작업에서는 반드시 다음 형식으로 보고한다.

```text
실행 명령:
결과:
남은 blocker:
```

## 3. 프로젝트 컨텍스트

- 프로젝트명: `[PROJECT_NAME]`
- 애플리케이션명: `[APP_NAME]`
- root package: `[ROOT_PACKAGE]`
- 기본 운영 환경: Ubuntu
- 기본 개발 방향: DB-first, API 계약 우선, 서버 사이드 렌더링 중심
- 기본 배포 방향: GitHub Actions OIDC -> S3 -> CodeDeploy -> EC2
- 기본 DB 방향: PostgreSQL 또는 Aurora PostgreSQL

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
- AWS GitHub Actions OIDC
- AWS S3
- AWS CodeDeploy
- AWS EC2
- AWS SSM Session Manager
- AWS Secrets Manager, 필요한 경우

## 5. 작업 제약

- 필요 이상으로 넓은 변경을 하지 않는다.
- 기존 사용자 변경사항을 임의로 되돌리지 않는다.
- DB 컬럼명 변경은 사전 협의 없이 수행하지 않는다.
- 운영 secret을 코드, 문서, GitHub Variables, 로그에 기록하지 않는다.
- 완성된 scaffold가 있다고 가정하지 않고 항상 현재 구조를 확인한다.
- MVP 범위를 유지하며, 명시 요청이 있을 때만 확장한다.
- 생성 산출물, 디자인 자료, 바이너리 문서는 사용자가 요청하지 않으면 수정하지 않는다.
- 한국어 업무 용어는 명확한 이유가 없으면 유지한다.
- CloudShell 명령과 EC2 SSM shell 명령을 혼동하지 않는다.

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
[ROOT_PACKAGE].domain
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
- 적용된 Flyway migration 파일은 수정하지 않는다.
- Flyway checksum mismatch가 발생하면 migration을 임의 편집하지 말고 DB/profile/source mismatch를 먼저 조사한다.
- 운영 profile은 local/dev seed 경로를 포함하지 않는다.

## 13. Security 규칙

- SQL Injection 방지를 위해 `#{}` binding만 사용한다.
- XSS 방지를 위해 Thymeleaf는 `th:text`를 사용한다.
- `th:utext` 사용 금지
- 인증과 권한은 서버에서 검증한다.
- 외부 API key는 환경변수로만 주입한다.
- 개인정보는 외부 API로 전송하지 않는다.
- Content-Security-Policy 적용을 기본으로 고려한다.
- 서버 검증은 필수이며 JavaScript로 우회하지 않는다.
- DB password, bootstrap password, AWS secret, Secrets Manager 출력값을 채팅, 저장소, 문서, GitHub Variables에 남기지 않는다.
- secret이 노출되면 즉시 rotation 또는 재발급 계획을 세운다.

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
- systemd, Nginx, PostgreSQL/Aurora 배포를 기본 운영 모델로 고려한다.
- Windows 경로 하드코딩을 금지한다.
- 파일 저장 경로는 환경변수로 외부화한다.
- 운영 secret은 systemd EnvironmentFile 또는 서버 환경변수로 관리한다.

예시:

```text
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
DB_URL=jdbc:postgresql://<db-endpoint>:5432/[DB_NAME]?sslmode=require
DB_USERNAME=[DB_USERNAME]
DB_PASSWORD=...
STORAGE_ROOT=/var/lib/[APP_NAME]/storage
LOG_PATH=/var/log/[APP_NAME]
```

## 19. CI/CD 기본 원칙

- 장기 AWS Access Key를 만들지 않는다.
- GitHub Actions는 OIDC Role을 사용한다.
- 배포 흐름은 GitHub Actions -> S3 -> CodeDeploy -> EC2를 따른다.
- GitHub Repository Variables에는 비민감 설정값만 등록한다.
- GitHub Secrets나 Variables에 DB password를 저장하지 않는다.
- DB password는 EC2 EnvironmentFile 또는 Secrets Manager 연동으로 관리한다.
- 커밋/푸시는 사용자의 지시가 있을 때만 수행한다.

GitHub Repository Variables 예시:

```text
AWS_REGION
AWS_DEPLOY_BUCKET
AWS_CODEDEPLOY_APPLICATION
AWS_CODEDEPLOY_DEPLOYMENT_GROUP
AWS_GITHUB_ACTIONS_ROLE_ARN
```

## 20. CI/CD 생성 순서

신규 프로젝트의 CI/CD는 다음 순서로 진행한다.

1. Repo 구조 확인
2. Spring Boot/Gradle 여부 확인
3. `/actuator/health` 존재 여부 확인
4. Actuator dependency와 security permit 여부 확인
5. AWS 기존 인프라 확인
6. EC2 신규 생성 또는 재사용 여부 결정
7. DB 또는 Aurora RDS 준비
8. EC2 app env 준비
9. 전용 CodeDeploy target tag 부여
10. CodeDeploy deployment group target 검증
11. `.github/workflows/deploy.yml` 추가
12. `appspec.yml` 추가
13. `scripts/start.sh`, `scripts/stop.sh`, `scripts/validate.sh` 추가
14. 로컬 `test bootJar` 검증
15. GitHub Variables 등록
16. 커밋/푸시
17. GitHub Actions 실행 확인
18. CodeDeploy lifecycle 로그 확인
19. 내부 health 확인
20. 외부 접속 확인

## 21. CI/CD Target Isolation 규칙

이 항목은 필수다. 이전 배포 장애의 핵심 원인은 CodeDeploy deployment group이 `Environment=dev` 같은 넓은 태그로 여러 프로젝트 EC2를 동시에 대상으로 잡은 것이다.

- 각 프로젝트는 전용 EC2를 사용한다. EC2 재사용은 예외로만 허용한다.
- 각 프로젝트는 전용 CodeDeploy application과 deployment group을 사용한다.
- 각 프로젝트는 전용 S3 deploy bucket을 사용한다.
- 각 프로젝트는 전용 systemd service를 사용한다.
- 각 프로젝트는 전용 배포 디렉터리를 사용한다.
- 기본 배포 디렉터리는 `/home/ubuntu/[APP_NAME]` 또는 `/var/lib/[APP_NAME]`로 둔다.
- 여러 프로젝트가 `/home/ubuntu/app/app.jar`를 공유하지 않는다.
- 여러 프로젝트가 같은 EC2에서 같은 `SERVER_PORT`를 공유하지 않는다.
- CodeDeploy deployment group은 절대 `Environment=dev` 단독 필터로 만들지 않는다.
- CodeDeploy deployment group은 전용 태그 하나로 좁힌다.

전용 태그 예시:

```text
[APP_NAME]DeployTarget=true
```

예:

```text
SanebDeployTarget=true
KinotonDeployTarget=true
```

배포 전 target 검증 명령 예시:

```bash
aws ec2 describe-instances \
  --region "$AWS_REGION" \
  --filters Name=tag:[APP_NAME]DeployTarget,Values=true Name=instance-state-name,Values=running \
  --query 'Reservations[].Instances[].{InstanceId:InstanceId,Name:Tags[?Key==`Name`]|[0].Value,Project:Tags[?Key==`Project`]|[0].Value}' \
  --output table
```

정상 기준:

```text
대상 EC2가 정확히 1대여야 한다.
```

## 22. CodeDeploy Artifact 규칙

- `appspec.yml`의 `files.destination`은 프로젝트 전용 디렉터리여야 한다.
- `start.sh`, `stop.sh`, `validate.sh`는 프로젝트 전용 service name과 app dir만 다룬다.
- `stop.sh`는 다른 프로젝트의 Java process를 죽이면 안 된다.
- `pkill -f app.jar` 같은 광범위한 kill 명령을 금지한다.
- process kill이 필요하면 프로젝트 전용 jar 경로 또는 systemd service 기준으로만 수행한다.
- `validate.sh`는 `mktemp`를 사용하고 고정 `/tmp/...` 파일명 충돌을 만들지 않는다.
- `validate.sh`는 `curl http://127.0.0.1:${SERVER_PORT}${HEALTH_PATH}`를 기준으로 판단한다.
- 실패 시 `systemctl status`, `journalctl`, app log를 출력한다.

## 23. EC2 app.env 규칙

- EC2 app env는 프로젝트 전용 경로에 둔다.
- 권장 경로:

```text
/home/ubuntu/[APP_NAME]/app.env
```

- 파일 권한:

```bash
sudo chown ubuntu:ubuntu /home/ubuntu/[APP_NAME]/app.env
sudo chmod 600 /home/ubuntu/[APP_NAME]/app.env
```

- `app.env`에는 반드시 `KEY=VALUE` 라인만 둔다.
- multiline 값, shell prompt, heredoc prompt, 채팅 복사 잔여물을 넣지 않는다.
- 비밀번호 출력 검증 시에는 반드시 masking한다.

검증 예시:

```bash
sudo sed -n '1,40p' /home/ubuntu/[APP_NAME]/app.env | sed -E 's/^(.*PASSWORD=).*/\1***MASKED***/'
```

필수 값 예시:

```text
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
JAVA_OPTS=-Xms128m -Xmx512m
DB_URL=jdbc:postgresql://<db-endpoint>:5432/[DB_NAME]?sslmode=require
DB_USERNAME=[DB_USERNAME]
DB_PASSWORD=...
STORAGE_ROOT=/home/ubuntu/[APP_NAME]/storage
LOG_PATH=/home/ubuntu/[APP_NAME]/logs
HEALTH_PATH=/actuator/health
```

## 24. DB-first 배포 Gate

- DB가 없으면 첫 배포 성공 검증을 시작하지 않는다.
- Aurora/RDS endpoint, DB username, DB password가 확인된 뒤 app.env를 작성한다.
- Spring Boot prod profile이 DB에 정상 연결할 수 있어야 배포를 시작한다.
- Flyway migration validation 실패는 앱 코드 문제가 아니라 DB history/source mismatch일 수 있다.
- local/dev seed가 적용된 DB를 prod profile로 기동하지 않는다.
- 운영 DB에 테스트 계정을 넣을 때는 direct SQL handoff와 password reset 정책을 명시한다.

## 25. CloudShell / EC2 SSM 분리 규칙

CloudShell에서 실행할 명령:

```text
aws sts get-caller-identity
aws ec2 describe-instances
aws rds describe-db-clusters
aws secretsmanager get-secret-value
aws deploy update-deployment-group
aws deploy create-deployment
```

EC2 SSM shell에서 실행할 명령:

```text
sudo systemctl status [APP_NAME]
sudo systemctl restart [APP_NAME]
sudo sed -n ... app.env
sudo tail -n ... app.log
sudo ss -ltnp
```

`sh: aws: not found`가 나오면 CloudShell 명령을 EC2 내부에서 실행한 것이다. 즉시 `exit`로 CloudShell에 복귀한다.

## 26. Health Check 규칙

- Spring Boot Actuator dependency를 추가한다.
- `/actuator/health`는 인증 없이 접근 가능해야 한다.
- SecurityConfig에서 `/actuator/health`를 permitAll 처리한다.
- GitHub Actions/CodeDeploy 검증은 `HEALTH_PATH=/actuator/health`를 기본값으로 사용한다.
- 외부 접속 실패와 내부 health 실패를 구분한다.

내부 health:

```bash
curl -i http://127.0.0.1:${SERVER_PORT:-8080}${HEALTH_PATH:-/actuator/health}
```

외부 health:

```bash
curl -i http://<PUBLIC_IP>:${SERVER_PORT:-8080}${HEALTH_PATH:-/actuator/health}
```

## 27. 장애 재발 방지 Checklist

배포 전:

- `aws sts get-caller-identity`로 CloudShell 계정을 확인했다.
- deployment group target filter가 전용 태그인지 확인했다.
- 전용 태그로 조회되는 EC2가 정확히 1대인지 확인했다.
- 같은 EC2에서 다른 프로젝트 service가 실행 중인지 확인했다.
- app dir가 프로젝트 전용인지 확인했다.
- systemd service name이 프로젝트 전용인지 확인했다.
- `app.env`가 프로젝트 전용 경로에 있고 DB 값이 정확한지 확인했다.
- DB/Aurora가 available인지 확인했다.
- `/actuator/health`가 permitAll인지 확인했다.
- GitHub Variables 5개가 등록됐는지 확인했다.

배포 실패 시:

- GitHub Actions 로그만 보지 않고 CodeDeploy deploymentId를 확인한다.
- `aws deploy get-deployment`로 failure code를 확인한다.
- `aws deploy list-deployment-instances`로 대상 EC2를 확인한다.
- 대상 EC2가 1대가 아니면 즉시 배포를 중단하고 tag filter를 고친다.
- SSM으로 `systemctl status`, `journalctl`, `app.log`, `app.env`, port 상태를 확인한다.
- 다른 프로젝트 service가 설치되었거나 app.jar가 덮였으면 즉시 해당 프로젝트를 재배포한다.

## 28. 복구 Runbook

잘못된 CodeDeploy target으로 다른 프로젝트가 오염된 경우:

1. 문제 프로젝트의 재배포를 중단한다.
2. 오염된 deployment group을 전용 태그로 좁힌다.
3. 피해 프로젝트의 deployment group도 전용 태그로 좁힌다.
4. 피해 EC2에서 잘못 설치된 service를 stop/disable/remove한다.
5. 피해 프로젝트의 최신 S3 bundle 또는 GitHub Actions로 재배포한다.
6. 피해 프로젝트의 internal health를 확인한다.
7. 문제 프로젝트를 다시 배포한다.

## 29. Verification

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
.\gradlew.bat test bootJar --console=plain --no-problems-report
```

Ubuntu 예시:

```bash
./gradlew test bootJar --console=plain --no-problems-report
```

CI/CD 검증 예시:

```bash
aws deploy get-deployment --deployment-id "$DEPLOYMENT_ID" --output table
aws ssm send-command --instance-ids "$INSTANCE_ID" --document-name "AWS-RunShellScript" --parameters 'commands=["systemctl status [APP_NAME] --no-pager || true","curl -i http://127.0.0.1:8080/actuator/health || true"]'
```

## 30. Commit Convention

- `feat`: 기능 추가
- `fix`: 버그 수정
- `refactor`: 리팩토링
- `docs`: 문서 변경
- `test`: 테스트 추가/수정
- `chore`: 기타 작업

## 31. Forbidden Actions

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
- CodeDeploy deployment group을 `Environment=dev` 단독 필터로 구성 금지
- 여러 프로젝트가 `/home/ubuntu/app/app.jar`를 공유하는 구성 금지
- 다른 프로젝트 EC2에 `app.jar`, `app.env`, systemd service를 덮어쓰는 배포 금지
- `0.0.0.0/0`로 DB port 5432를 여는 작업 금지
- AWS long-lived access key 생성 금지

## 32. 새 프로젝트 첫 프롬프트

### Backend 초기 프롬프트

```text
[PROJECT_NAME] 프로젝트를 Java 21, Spring Boot, Gradle, Spring Security, MyBatis, PostgreSQL, Flyway, Thymeleaf 기준으로 초기 설계하세요.

AGENTS.md를 먼저 읽고 준수하세요.
DB-first로 MVP schema 초안을 설계하고, 인증/권한/감사 로그/기본 사용자 구조를 우선 확정하세요.
API는 /api/v1/... 버전 정책과 ApiResponse wrapper를 따르세요.
운영 환경은 Ubuntu 기준이며 secret은 환경변수로만 관리합니다.
CI/CD는 GitHub Actions OIDC -> S3 -> CodeDeploy -> EC2 기준이며, deployment group은 전용 target tag 1개로만 구성합니다.
다음 산출물을 제시하세요:
1. MVP 도메인 경계
2. DB 테이블 초안
3. API 초안
4. 패키지 구조
5. 구현 순서
6. 검증 방법
7. CI/CD Gate
```

### Frontend 초기 프롬프트

```text
[PROJECT_NAME] 프론트엔드는 Thymeleaf, Bootstrap 5, 모바일 360px 기준으로 설계하세요.

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

### CI/CD 초기 프롬프트

```text
[PROJECT_NAME] CI/CD를 GitHub Actions OIDC -> S3 -> CodeDeploy -> EC2 기준으로 구축하세요.

AGENTS.md를 먼저 읽고 준수하세요.
먼저 repo 구조와 AWS 기존 인프라를 확인하세요.
DB/Aurora readiness를 첫 배포 전 hard gate로 두세요.
EC2는 전용 target tag `[APP_NAME]DeployTarget=true`로만 CodeDeploy 대상에 포함하세요.
기존 EC2 재사용 시 다른 프로젝트 service, port, app dir, CodeDeploy tag 충돌을 먼저 점검하세요.
장기 AWS Access Key는 만들지 마세요.
다음 산출물을 제시하세요:
1. 현재 repo 구조 확인
2. AWS 인프라 확인
3. DB readiness 확인
4. GitHub Variables 목록
5. deploy.yml
6. appspec.yml
7. lifecycle scripts
8. target isolation 검증 명령
9. 첫 배포 검증 명령
10. rollback/recovery runbook
```
