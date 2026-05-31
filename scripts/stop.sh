#!/usr/bin/env bash
set -euo pipefail

APP_NAME="saneb"
JAR_FILE="/home/ubuntu/app/app.jar"

if systemctl list-unit-files "${APP_NAME}.service" >/dev/null 2>&1; then
  systemctl stop "${APP_NAME}.service" || true
fi

if pgrep -f "${JAR_FILE}" >/dev/null 2>&1; then
  pkill -TERM -f "${JAR_FILE}" || true
  sleep 10
fi

if pgrep -f "${JAR_FILE}" >/dev/null 2>&1; then
  pkill -KILL -f "${JAR_FILE}" || true
fi
