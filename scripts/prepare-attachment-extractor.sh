#!/usr/bin/env bash
set -euo pipefail

# 별도 추출 CLI의 기존 설계 의존성만 설치한다. DB/운영 플래그는 변경하지 않는다.
if ! command -v bwrap >/dev/null 2>&1; then
  export DEBIAN_FRONTEND=noninteractive
  apt-get update -qq
  apt-get install -y --no-install-recommends bubblewrap
fi
command -v prlimit >/dev/null
install -d -m 755 /opt/saneb/attachment-extractor
