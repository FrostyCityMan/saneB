#!/usr/bin/env bash
set -euo pipefail

APP_NAME="saneb"
APP_DIR="/home/ubuntu/app"
ENV_FILE="${APP_DIR}/app.env"
LOG_FILE="${APP_DIR}/app.log"
MAX_WAIT_SECONDS="${MAX_WAIT_SECONDS:-90}"
SLEEP_SECONDS="${SLEEP_SECONDS:-5}"

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

health_url="http://127.0.0.1:${port}${health_path}"
deadline=$((SECONDS + MAX_WAIT_SECONDS))

while [ "${SECONDS}" -le "${deadline}" ]; do
  if response="$(curl --fail --silent --show-error --max-time 5 "${health_url}" 2>&1)"; then
    echo "${response}"
    echo
    echo "Health check passed: ${health_url}"
    exit 0
  fi

  echo "Health check not ready yet: ${health_url}"
  echo "${response}"
  sleep "${SLEEP_SECONDS}"
done

echo "Health check failed after ${MAX_WAIT_SECONDS}s: ${health_url}" >&2
print_diagnostics
exit 1
