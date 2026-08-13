#!/usr/bin/env bash
set -euo pipefail

APP_NAME="saneb"
APP_DIR="/home/ubuntu/app"
ENV_FILE="${APP_DIR}/app.env"
LOG_FILE="${APP_DIR}/app.log"
JAR_FILE="${APP_DIR}/app.jar"
PREVIOUS_JAR_FILE="${APP_DIR}/app.jar.previous"
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

wait_for_health() {
  local health_url="$1"
  local max_wait_seconds="$2"
  local deadline=$((SECONDS + max_wait_seconds))
  local response=""

  while [ "${SECONDS}" -le "${deadline}" ]; do
    if response="$(curl --fail --silent --show-error --max-time 5 "${health_url}" 2>&1)"; then
      printf '%s' "${response}"
      return 0
    fi
    sleep "${SLEEP_SECONDS}"
  done

  return 1
}

restore_previous_jar() {
  [ -f "${PREVIOUS_JAR_FILE}" ] || return 1

  echo "Deployment validation failed. Restoring the previous application jar." >&2
  cp -f "${PREVIOUS_JAR_FILE}" "${JAR_FILE}"
  chown ubuntu:ubuntu "${JAR_FILE}"
  systemctl restart "${APP_NAME}.service"

  if wait_for_health "${health_url}" 60; then
    echo
    echo "Previous application jar was restored and passed health validation." >&2
    return 0
  fi

  echo "Previous application jar restoration did not recover health." >&2
  return 1
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

health_url="http://127.0.0.1:${port}${health_path}"

if systemctl is-active --quiet "${APP_NAME}.service" && wait_for_health "${health_url}" "${MAX_WAIT_SECONDS}"; then
  echo
  echo "Health check passed: ${health_url}"
  exit 0
fi

echo "Health check failed after ${MAX_WAIT_SECONDS}s: ${health_url}" >&2
print_diagnostics
restore_previous_jar || true
exit 1
