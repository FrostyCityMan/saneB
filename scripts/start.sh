#!/usr/bin/env bash
set -euo pipefail

APP_NAME="saneb"
APP_DIR="/home/ubuntu/app"
ENV_FILE="${APP_DIR}/app.env"
JAR_FILE="${APP_DIR}/app.jar"
LOG_FILE="${APP_DIR}/app.log"
LAUNCH_SCRIPT="${APP_DIR}/launch-saneb.sh"
SERVICE_FILE="/etc/systemd/system/${APP_NAME}.service"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

require_env() {
  local key="$1"
  if ! grep -Eq "^${key}=.+" "${ENV_FILE}"; then
    fail "Required environment value is missing in ${ENV_FILE}: ${key}"
  fi
}

has_env() {
  local key="$1"
  grep -Eq "^${key}=.+" "${ENV_FILE}"
}

install -d -o ubuntu -g ubuntu -m 755 "${APP_DIR}" "${APP_DIR}/logs" "${APP_DIR}/storage"
touch "${LOG_FILE}"
chown ubuntu:ubuntu "${LOG_FILE}"

[ -f "${JAR_FILE}" ] || fail "Application jar does not exist: ${JAR_FILE}"
[ -f "${ENV_FILE}" ] || fail "Environment file does not exist: ${ENV_FILE}"

require_env "SPRING_PROFILES_ACTIVE"
require_env "SERVER_PORT"
require_env "DB_URL"
require_env "DB_USERNAME"

if ! has_env "DB_PASSWORD"; then
  require_env "DB_SECRET_ARN"
fi

cat > "${LAUNCH_SCRIPT}" <<'LAUNCH'
#!/usr/bin/env bash
set -euo pipefail

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

resolve_secret_region() {
  if [ -n "${AWS_REGION:-}" ]; then
    printf '%s' "${AWS_REGION}"
    return
  fi

  if [ -n "${AWS_DEFAULT_REGION:-}" ]; then
    printf '%s' "${AWS_DEFAULT_REGION}"
    return
  fi

  if [[ "${DB_SECRET_ARN:-}" == arn:aws:secretsmanager:* ]]; then
    printf '%s' "${DB_SECRET_ARN}" | cut -d: -f4
    return
  fi

  printf ''
}

read_db_password_from_secret() {
  command -v aws >/dev/null 2>&1 || fail "aws CLI is required when DB_SECRET_ARN is used."
  command -v python3 >/dev/null 2>&1 || fail "python3 is required when DB_SECRET_ARN is used."

  local secret_region
  secret_region="$(resolve_secret_region)"
  [ -n "${secret_region}" ] || fail "AWS_REGION, AWS_DEFAULT_REGION, or a regional DB_SECRET_ARN is required."

  local secret_string
  secret_string="$(
    aws secretsmanager get-secret-value \
      --region "${secret_region}" \
      --secret-id "${DB_SECRET_ARN}" \
      --query SecretString \
      --output text
  )"

  SECRET_STRING="${secret_string}" python3 - <<'PY'
import json
import os
import sys

secret_string = os.environ.get("SECRET_STRING", "")
try:
    payload = json.loads(secret_string)
except json.JSONDecodeError:
    if secret_string:
        print(secret_string)
        sys.exit(0)
    sys.stderr.write("SecretString is empty.\n")
    sys.exit(1)

password = payload.get("password")
if not password:
    sys.stderr.write("SecretString does not contain a password field.\n")
    sys.exit(1)

print(password)
PY
}

if [ -n "${DB_SECRET_ARN:-}" ]; then
  DB_PASSWORD="$(read_db_password_from_secret)"
  export DB_PASSWORD
elif [ -z "${DB_PASSWORD:-}" ]; then
  fail "DB_PASSWORD or DB_SECRET_ARN is required."
fi

exec /usr/bin/java ${JAVA_OPTS:-} -jar /home/ubuntu/app/app.jar
LAUNCH

chown ubuntu:ubuntu "${LAUNCH_SCRIPT}"
chmod 700 "${LAUNCH_SCRIPT}"

cat > "${SERVICE_FILE}" <<SERVICE
[Unit]
Description=saneB Spring Boot application
After=network-online.target
Wants=network-online.target

[Service]
User=ubuntu
WorkingDirectory=${APP_DIR}
EnvironmentFile=${ENV_FILE}
ExecStart=${LAUNCH_SCRIPT}
SuccessExitStatus=143
Restart=always
RestartSec=10
StandardOutput=append:${LOG_FILE}
StandardError=append:${LOG_FILE}

[Install]
WantedBy=multi-user.target
SERVICE

systemctl daemon-reload
systemctl enable "${APP_NAME}.service"
systemctl restart "${APP_NAME}.service"
