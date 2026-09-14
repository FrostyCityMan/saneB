#!/usr/bin/env bash
# 배포 hook과 임시 디렉터리 시험에서 공유한다. 운영 DB/환경 파일/프로세스를 다루지 않는다.
install_attachment_contract_release() (
  set -euo pipefail
  local release_root="$1" payload="$2" app_jar="$3" bundle_jar="$4"
  local digest bundle_digest release staging=''
  fail_release() { echo "ATTACHMENT_CONTRACT_INSTALL=$1" >&2; exit 1; }
  [[ -d "$release_root" && ! -L "$release_root" ]] || fail_release ROOT_INVALID
  release_root="$(realpath -e -- "$release_root")"
  [[ "$release_root" != / ]] || fail_release ROOT_INVALID
  [[ -d "$payload" && ! -L "$payload" && -f "$app_jar" && ! -L "$app_jar" && -f "$bundle_jar" && ! -L "$bundle_jar" ]] || fail_release INPUT_INVALID
  payload="$(realpath -e -- "$payload")"
  # 심볼릭 링크/특수 파일은 복사하거나 따라가지 않는다. root 경로도 코드 전용이다.
  [[ -z "$(find "$payload" ! -type f ! -type d -print -quit)" ]] || fail_release PAYLOAD_TYPE_INVALID
  for directory in lib bin config extractor; do
    [[ -d "$payload/$directory" ]] || fail_release PAYLOAD_INCOMPLETE
  done
  for file in bin/run-attachment-contract-qa.sh config/hosts config/logback-qa.xml config/logging.properties; do
    [[ -f "$payload/$file" ]] || fail_release PAYLOAD_INCOMPLETE
  done
  [[ -n "$(find "$payload/lib" -maxdepth 1 -name 'saneb-attachment-contract-qa-*.jar' -type f -print -quit)" ]] || fail_release PAYLOAD_INCOMPLETE
  [[ -f "$payload/extractor/bin/attachment-extractor" && -d "$payload/extractor/lib" ]] || fail_release PAYLOAD_INCOMPLETE
  digest="$(sha256sum -- "$app_jar")"; digest="${digest%% *}"
  bundle_digest="$(sha256sum -- "$bundle_jar")"; bundle_digest="${bundle_digest%% *}"
  [[ "$digest" =~ ^[0-9a-f]{64}$ && "$digest" == "$bundle_digest" ]] || fail_release APP_JAR_MISMATCH
  release="$release_root/$digest"
  # 기존 release는 불변이다. 덮어쓰기/의존성 overlay 대신 완전 일치를 요구한다.
  if [[ -e "$release" || -L "$release" ]]; then
    [[ -d "$release" && ! -L "$release" && -z "$(find "$release" ! -type f ! -type d -print -quit)" ]] || fail_release RELEASE_INVALID
    diff -qr -- "$payload" "$release" >/dev/null || fail_release RELEASE_CONTENT_MISMATCH
    echo 'ATTACHMENT_CONTRACT_INSTALL=ALREADY_INSTALLED'
    exit 0
  fi
  cleanup_release() {
    if [[ -n "$staging" && -d "$staging" && ! -L "$staging" ]]; then
      local resolved
      resolved="$(realpath -e -- "$staging")"
      [[ "$(dirname -- "$resolved")" == "$release_root" && "$(basename -- "$resolved")" == .install-* ]] || return 1
      rm -rf -- "$resolved"
    fi
  }
  trap cleanup_release EXIT
  staging="$(mktemp -d "$release_root/.install-XXXXXXXX")"
  cp -R --no-preserve=ownership -- "$payload/." "$staging/"
  find "$staging" -type d -exec chmod 755 {} +
  find "$staging" -type f -exec chmod 644 {} +
  chmod 755 "$staging/bin/run-attachment-contract-qa.sh" "$staging/extractor/bin/attachment-extractor"
  diff -qr -- "$payload" "$staging" >/dev/null || fail_release COPY_MISMATCH
  # CodeDeploy concurrency는 한 번에 하나다. 동일 경로가 생겼어도 기존 release를 덮지 않는다.
  mv -T -n -- "$staging" "$release"
  [[ ! -d "$staging" ]] || fail_release RELEASE_ALREADY_CREATED
  staging=''
  trap - EXIT
  echo 'ATTACHMENT_CONTRACT_INSTALL=INSTALLED'
)

# launch 시점의 실제 JAR로 선택한다. 이전 JAR 복구도 이전 release를 선택한다.
select_attachment_contract_release() (
  set -euo pipefail
  local root="$1" jar="$2" digest
  [[ -f "$jar" && ! -L "$jar" ]] || return 1
  digest="$(sha256sum -- "$jar")"; digest="${digest%% *}"
  [[ "$digest" =~ ^[0-9a-f]{64}$ ]] || return 1
  printf '%s/%s' "$root" "$digest"
)
