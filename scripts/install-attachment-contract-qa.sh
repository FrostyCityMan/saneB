#!/usr/bin/env bash
set -euo pipefail

# CodeDeploy가 검증한 동일 bundle에서 설치한다. 정책/worker 활성화나 QA 실행은 하지 않는다.
[[ $# -eq 0 && "$(id -u)" == 0 ]] || exit 1
bundle_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)"
source "$bundle_root/scripts/attachment-contract-release.sh"
release_root=/opt/saneb/attachment-contract-qa-releases
[[ ! -L "$release_root" ]] || exit 1
install -d -o root -g root -m 755 "$release_root"
install_attachment_contract_release "$release_root" "$bundle_root/attachment-contract-qa" \
  /home/ubuntu/app/app.jar "$bundle_root/app.jar"
# 코드 전용 helper를 설치한다. 애플리케이션 계정은 설치 경로를 변경할 수 없다.
install -o root -g root -m 644 "$bundle_root/scripts/attachment-contract-release.sh" \
  /opt/saneb/attachment-contract-release.sh
