# Aurora RDS CloudShell 구축 절차

이 문서는 `saneB` 운영 DB를 Amazon Aurora PostgreSQL로 구성하고, 애플리케이션을 `prod` profile로 연결하기 위한 실행 절차다. 운영 migration은 `classpath:db/migration`만 사용하며 local/dev seed는 적용하지 않는다.

## 결정 사항

| 항목 | 기준 |
|---|---|
| DB 엔진 | Amazon Aurora PostgreSQL |
| 용량 모델 | Aurora Serverless v2 |
| 기본 ACU | `MIN_ACU=0.5`, `MAX_ACU=2` |
| 네트워크 | Public access 비활성, 애플리케이션 Security Group에서만 5432 허용 |
| 인증정보 | RDS managed master password 사용. 비밀번호는 Secrets Manager에서 조회 후 서버 환경변수로만 주입 |
| Spring profile | `SPRING_PROFILES_ACTIVE=prod` |
| JDBC URL | `jdbc:postgresql://<cluster-endpoint>:5432/saneb?sslmode=require` |

## 사전 확인

CloudShell은 AWS Console에서 같은 Region으로 연다. 애플리케이션 EC2가 이미 있다면 먼저 EC2 Security Group ID를 확인한다.

```bash
export REGION=ap-northeast-2

aws ec2 describe-instances \
  --region "$REGION" \
  --filters "Name=instance-state-name,Values=running" \
  --query 'Reservations[].Instances[].{InstanceId:InstanceId,Name:Tags[?Key==`Name`]|[0].Value,SecurityGroups:SecurityGroups[].GroupId,PrivateIp:PrivateIpAddress}' \
  --output table
```

## Aurora 생성

저장소를 CloudShell에 clone했거나 스크립트 파일을 업로드한 뒤 아래처럼 실행한다.

```bash
export REGION=ap-northeast-2
export APP_SECURITY_GROUP_ID=sg-xxxxxxxxxxxxxxxxx

export DB_CLUSTER_ID=saneb-prod-aurora
export DB_INSTANCE_ID=saneb-prod-aurora-1
export DB_NAME=saneb
export DB_MASTER_USERNAME=saneb_admin

bash scripts/aws/create-aurora-postgresql.sh
```

필요 시 비용과 성능 기준에 맞춰 ACU를 조정한다.

```bash
export MIN_ACU=0.5
export MAX_ACU=4
```

특정 Aurora PostgreSQL engine version을 고정해야 하면 먼저 사용 가능한 버전을 확인한 뒤 `ENGINE_VERSION`을 지정한다.

```bash
aws rds describe-db-engine-versions \
  --region "$REGION" \
  --engine aurora-postgresql \
  --query 'DBEngineVersions[].EngineVersion' \
  --output text

export ENGINE_VERSION=<확인한-engine-version>
```

## 서버 환경변수 반영

생성 스크립트가 출력한 `MASTER_SECRET_ARN`으로 비밀번호를 조회한다. 출력값은 JSON 문자열이므로 `password` 값을 서버 환경변수에만 반영한다.

```bash
aws secretsmanager get-secret-value \
  --region "$REGION" \
  --secret-id "<MASTER_SECRET_ARN>" \
  --query SecretString \
  --output text
```

EC2 서버의 systemd EnvironmentFile 예시는 아래와 같다. 실제 비밀번호는 저장소에 기록하지 않는다.

```bash
sudo install -d -m 750 /etc/saneb
sudo tee /etc/saneb/saneb.env >/dev/null <<'EOF'
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
DB_URL=jdbc:postgresql://<DB_ENDPOINT>:5432/saneb?sslmode=require
DB_USERNAME=saneb_admin
DB_PASSWORD=<MASTER_PASSWORD>
SANEB_BOOTSTRAP_ADMIN_ENABLED=false
EOF
sudo chmod 600 /etc/saneb/saneb.env
```

systemd service에 EnvironmentFile이 없다면 아래 줄을 추가한다.

```ini
EnvironmentFile=/etc/saneb/saneb.env
```

적용 후 재기동한다.

```bash
sudo systemctl daemon-reload
sudo systemctl restart saneb
sudo systemctl status saneb --no-pager
```

## 검증

애플리케이션 재기동 시 Flyway가 Aurora DB에 운영 migration을 적용한다. local/dev seed가 실행되면 안 된다.

```bash
aws rds describe-db-clusters \
  --region "$REGION" \
  --db-cluster-identifier saneb-prod-aurora \
  --query 'DBClusters[0].{Status:Status,Endpoint:Endpoint,Engine:Engine,EngineVersion:EngineVersion}' \
  --output table

sudo journalctl -u saneb -n 120 --no-pager
curl -I http://127.0.0.1:8080/login
```

성공 기준:

- RDS cluster와 instance가 `available`.
- 애플리케이션 로그에 Flyway migration 성공 로그가 남음.
- `/login`이 HTTP 200 또는 배포 라우팅 기준 정상 응답.
- 운영 DB에는 `db/seed/local`, `db/seed/dev` 데이터가 들어가지 않음.

## 운영 주의

- Aurora는 생성 즉시 비용이 발생한다. 테스트가 끝난 리소스는 명시적으로 정리한다.
- `DB_PASSWORD`, bootstrap 관리자 비밀번호, Secrets Manager 출력값은 저장소와 문서에 기록하지 않는다.
- MVP에서는 RDS managed master user를 애플리케이션 접속 계정으로 사용할 수 있다. 운영 사용자가 늘어나면 별도 app role을 만들고 최소 권한으로 분리한다.
- 배포 직후 초기 관리자 bootstrap을 사용할 경우 첫 로그인 후 `SANEB_BOOTSTRAP_ADMIN_ENABLED=false`로 되돌린다.

## 참고 문서

- [Creating an Amazon Aurora DB cluster](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/Aurora.CreateInstance.html)
- [Creating a DB cluster that uses Aurora Serverless v2](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/aurora-serverless-v2.create.html)
- [Manage AWS services from CLI in CloudShell](https://docs.aws.amazon.com/cloudshell/latest/userguide/working-with-aws-cli.html)
