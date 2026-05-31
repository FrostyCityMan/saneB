#!/usr/bin/env bash
set -euo pipefail

APP_NAME="saneb"
APP_DIR="/home/ubuntu/app"
ENV_FILE="${APP_DIR}/app.env"
LOG_FILE="${APP_DIR}/app.log"

read_env_value() {
  local key="$1"
  local line
  line="$(grep -E "^${key}=" "${ENV_FILE}" | tail -n 1 || true)"
  local value="${line#*=}"
  value="${value%$'\r'}"
  value="${value%\"}"
  value="${value#\"}"
  value="${value%\'}"
  value="${value#\'}"
  printf '%s' "${value}"
}

print_diagnostics() {
  echo "SERVICE_STATUS"
  systemctl status "${APP_NAME}.service" --no-pager || true
  echo "APP_LOG"
  tail -n 160 "${LOG_FILE}" || true
}

[ -f "${ENV_FILE}" ] || {
  echo "Missing environment file: ${ENV_FILE}" >&2
  exit 1
}

port="$(read_env_value SERVER_PORT)"
health_path="$(read_env_value HEALTH_PATH)"

if [ -z "${port}" ]; then
  port="8080"
fi

if [ -z "${health_path}" ]; then
  health_path="/actuator/health"
fi

if ! systemctl is-active --quiet "${APP_NAME}.service"; then
  echo "${APP_NAME}.service is not active." >&2
  print_diagnostics
  exit 1
fi

if ! curl --fail --silent --show-error --max-time 5 --retry 12 --retry-delay 5 "http://127.0.0.1:${port}${health_path}"; then
  echo "Health check failed: http://127.0.0.1:${port}${health_path}" >&2
  print_diagnostics
  exit 1
fi

echo
echo "Health check passed: http://127.0.0.1:${port}${health_path}"
