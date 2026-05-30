# Deployment Admin Bootstrap

배포 환경의 초기 관리자 계정은 운영 migration이나 seed에 저장하지 않는다.
애플리케이션 기동 시 명시적으로 bootstrap 환경변수가 설정된 경우에만 활성 관리자 존재 여부를 확인하고, 활성 `ADMIN` 계정이 없을 때 1회성 계정을 생성한다.

## Environment

| 변수 | 필수 | 설명 |
|---|---|---|
| `SANEB_BOOTSTRAP_ADMIN_ENABLED` | 예 | `true`일 때만 bootstrap 실행 |
| `SANEB_BOOTSTRAP_ADMIN_LOGIN_ID` | 예 | 생성할 관리자 로그인 ID |
| `SANEB_BOOTSTRAP_ADMIN_PASSWORD` | 예 | 초기 비밀번호. 저장소에 기록 금지 |
| `SANEB_BOOTSTRAP_ADMIN_NAME` | 아니오 | 표시 이름. 기본값 `기초 관리자` |

## Policy

- 활성 `ADMIN` 계정이 이미 있으면 아무 계정도 생성하지 않는다.
- 활성 `ADMIN` 계정이 없고 `SANEB_BOOTSTRAP_ADMIN_LOGIN_ID` 계정도 없으면 새 사용자를 생성하고 `ADMIN` 권한을 부여한다.
- 활성 `ADMIN` 계정이 없고 같은 login ID 계정이 있으면 해당 계정을 활성화하고 `ADMIN` 권한을 부여한다.
- 생성 또는 전환된 계정은 `password_reset_required = true`로 저장한다.
- 비밀번호는 BCrypt hash로만 DB에 저장하며, 로그와 저장소에는 원문을 남기지 않는다.
- 초기 접속 후 bootstrap 환경변수는 제거하거나 `SANEB_BOOTSTRAP_ADMIN_ENABLED=false`로 전환한다.
