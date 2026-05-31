#!/usr/bin/env bash
set -euo pipefail

APP_NAME="saneb"
APP_DIR="/home/ubuntu/app"
ENV_FILE="${APP_DIR}/app.env"
JAR_FILE="${APP_DIR}/app.jar"
LOG_FILE="${APP_DIR}/app.log"
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

install -d -o ubuntu -g ubuntu -m 755 "${APP_DIR}" "${APP_DIR}/logs" "${APP_DIR}/storage"
touch "${LOG_FILE}"
chown ubuntu:ubuntu "${LOG_FILE}"

[ -f "${JAR_FILE}" ] || fail "Application jar does not exist: ${JAR_FILE}"
[ -f "${ENV_FILE}" ] || fail "Environment file does not exist: ${ENV_FILE}"

require_env "SPRING_PROFILES_ACTIVE"
require_env "SERVER_PORT"
require_env "DB_URL"
require_env "DB_USERNAME"
require_env "DB_PASSWORD"

cat > "${SERVICE_FILE}" <<SERVICE
[Unit]
Description=saneB Spring Boot application
After=network-online.target
Wants=network-online.target

[Service]
User=ubuntu
WorkingDirectory=${APP_DIR}
EnvironmentFile=${ENV_FILE}
ExecStart=/bin/bash -lc 'exec /usr/bin/java \${JAVA_OPTS:-} -jar ${JAR_FILE}'
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
