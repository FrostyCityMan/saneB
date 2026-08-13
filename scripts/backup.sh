#!/usr/bin/env bash
set -euo pipefail

APP_DIR="/home/ubuntu/app"
JAR_FILE="${APP_DIR}/app.jar"
PREVIOUS_JAR_FILE="${APP_DIR}/app.jar.previous"
TEMP_PREVIOUS_JAR_FILE="${PREVIOUS_JAR_FILE}.tmp"

install -d -o ubuntu -g ubuntu -m 755 "${APP_DIR}"

if [ ! -f "${JAR_FILE}" ]; then
  rm -f "${PREVIOUS_JAR_FILE}" "${TEMP_PREVIOUS_JAR_FILE}"
  echo "No previous application jar exists. Rollback backup was not created."
  exit 0
fi

cp -f "${JAR_FILE}" "${TEMP_PREVIOUS_JAR_FILE}"
chown ubuntu:ubuntu "${TEMP_PREVIOUS_JAR_FILE}"
chmod 640 "${TEMP_PREVIOUS_JAR_FILE}"
mv -f "${TEMP_PREVIOUS_JAR_FILE}" "${PREVIOUS_JAR_FILE}"

echo "Previous application jar backup is ready."
