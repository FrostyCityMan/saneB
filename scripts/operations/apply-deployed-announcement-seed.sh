#!/usr/bin/env bash
set -euo pipefail

SQL_FILE="${1:-/home/ubuntu/app/scripts/operations/seed-deployed-announcements-20260614.sql}"
ENV_FILE="${SANEB_ENV_FILE:-/home/ubuntu/app/app.env}"

if [[ ! -f "$SQL_FILE" ]]; then
  echo "SQL file not found: $SQL_FILE" >&2
  exit 10
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Environment file not found: $ENV_FILE" >&2
  exit 11
fi

if ! command -v aws >/dev/null 2>&1; then
  echo "aws CLI is required." >&2
  exit 12
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 is required." >&2
  exit 13
fi

if ! command -v psql >/dev/null 2>&1; then
  echo "psql is required. Install postgresql-client first." >&2
  exit 14
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

: "${AWS_REGION:?AWS_REGION is required.}"
: "${DB_SECRET_ARN:?DB_SECRET_ARN is required.}"
: "${DB_URL:?DB_URL is required.}"
: "${DB_USERNAME:?DB_USERNAME is required.}"

SECRET_JSON="$(
  aws secretsmanager get-secret-value \
    --region "$AWS_REGION" \
    --secret-id "$DB_SECRET_ARN" \
    --query SecretString \
    --output text
)"

DB_PASSWORD="$(
  SECRET_JSON="$SECRET_JSON" python3 - <<'PY'
import json
import os

payload = json.loads(os.environ["SECRET_JSON"])
password = payload.get("password")
if not password:
    raise SystemExit("password is missing in DB secret")
print(password)
PY
)"

read -r DB_HOST DB_PORT DB_NAME < <(
  DB_URL="$DB_URL" python3 - <<'PY'
import os
from urllib.parse import urlparse

jdbc_url = os.environ["DB_URL"]
prefix = "jdbc:"
if jdbc_url.startswith(prefix):
    jdbc_url = jdbc_url[len(prefix):]
parsed = urlparse(jdbc_url)
host = parsed.hostname or ""
port = parsed.port or 5432
dbname = (parsed.path or "").lstrip("/")
if not host or not dbname:
    raise SystemExit("DB_URL must include host and database name")
print(host, port, dbname)
PY
)

export PGPASSWORD="$DB_PASSWORD"
export PGCLIENTENCODING=UTF8

psql \
  "host=$DB_HOST port=$DB_PORT dbname=$DB_NAME user=$DB_USERNAME sslmode=require" \
  -v ON_ERROR_STOP=1 \
  -f "$SQL_FILE"
