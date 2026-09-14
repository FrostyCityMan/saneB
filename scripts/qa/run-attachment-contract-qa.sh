#!/usr/bin/env bash
set -euo pipefail

# 소스 checkout/운영 환경변수 없이 임시 loopback PostgreSQL만 실행한다.
if [[ $# -ne 0 || "$(uname -s)" != Linux || "$(id -u)" == 0 ]]; then
  echo 'ATTACHMENT_CONTRACT_QA=NON_ROOT_LINUX_NO_ARGUMENTS_REQUIRED' >&2
  exit 1
fi
for tool in /usr/bin/bwrap /usr/bin/prlimit /usr/bin/timeout; do
  [[ -x "$tool" ]] || { echo 'ATTACHMENT_CONTRACT_QA=RUNTIME_UNAVAILABLE' >&2; exit 1; }
done
java_binary="$(readlink -f -- "$(command -v java)")"
[[ -x "$java_binary" && "$java_binary" == */bin/java ]] || exit 1
java_home="$(dirname -- "$(dirname -- "$java_binary")")"
distribution="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)"
[[ -d "$distribution/lib" && -f "$distribution/config/logback-qa.xml" && -f "$distribution/config/hosts" ]] || exit 1
work="$(mktemp -d /tmp/saneb-contract-qa.XXXXXXXX)"
cleanup() {
  local resolved
  resolved="$(realpath -- "$work")"
  if [[ "$(dirname -- "$resolved")" != /tmp || "$(basename -- "$resolved")" != saneb-contract-qa.* ]]; then
    echo 'ATTACHMENT_CONTRACT_QA=CLEANUP_PATH_INVALID' >&2
    return 1
  fi
  rm -rf -- "$resolved"
}
trap cleanup EXIT
chmod 700 "$work"
mkdir "$work/tmp"
mounts=(--ro-bind /usr /usr --ro-bind /bin /bin --ro-bind /lib /lib)
mounts+=(--ro-bind "$java_home" /jre)
mounts+=(--ro-bind "$distribution/config/hosts" /etc/hosts)
[[ ! -d /lib64 ]] || mounts+=(--ro-bind /lib64 /lib64)
for file in /etc/ld.so.cache /etc/passwd /etc/group /etc/nsswitch.conf; do
  [[ ! -f "$file" ]] || mounts+=(--ro-bind "$file" "$file")
done
# Ubuntu JDK의 설정 symlink만 해석한다. 전체 /etc 또는 사용자 home은 공유하지 않는다.
if [[ -f "$java_home/conf/security/java.security" ]]; then
  java_security="$(readlink -f -- "$java_home/conf/security/java.security")"
  [[ "$java_security" == "$java_home/"* ]] || mounts+=(--ro-bind "$java_security" "$java_security")
fi
# private PID/IPC/network namespace 전체가 종료되므로 timeout 시 PostgreSQL 자손도 남지 않는다.
# /home, /opt, app.env, 운영 socket/DB/네트워크는 namespace에 넣지 않는다.
if output="$(env -i PATH=/usr/bin:/bin LANG=C.UTF-8 \
  /usr/bin/timeout --kill-after=5 600 \
  /usr/bin/prlimit --as=2147483648 --cpu=480 --nofile=256 --nproc=128 --fsize=134217728 -- \
  /usr/bin/bwrap --unshare-all --die-with-parent --new-session --cap-drop ALL \
  "${mounts[@]}" --proc /proc --dev /dev --tmpfs /dev/shm --tmpfs /tmp \
  --ro-bind "$distribution" /qa --bind "$work" /work --chdir /work \
  --clearenv --setenv PATH /usr/bin:/bin --setenv LANG C.UTF-8 --setenv HOME /work --setenv TMPDIR /work/tmp \
  --setenv SANEB_ATTACHMENT_JOB_TEST true --setenv SANEB_ATTACHMENT_MIGRATION_TEST true --setenv SANEB_ATTACHMENT_WORKER_QA true \
  /jre/bin/java -Xms32m -Xmx384m -XX:ActiveProcessorCount=1 -XX:+UseSerialGC \
  -XX:MaxMetaspaceSize=192m -XX:CompressedClassSpaceSize=64m -XX:ReservedCodeCacheSize=64m -Xss512k \
  -Djava.io.tmpdir=/work/tmp -Djava.net.preferIPv4Stack=true -Dlogback.configurationFile=/qa/config/logback-qa.xml \
  -Djava.util.logging.config.file=/qa/config/logging.properties \
  -cp '/qa/lib/*' com.saneb.qa.AttachmentContractQaMain 2>/dev/null)"; then
  qa_exit=0
else
  qa_exit=$?
fi
# 정리 실패 또는 외부 timeout은 성공 보고보다 우선한다. 부분 원문/DB는 산출물로 보관하지 않는다.
cleanup
trap - EXIT
if [[ -z "$output" || ${#output} -gt 1048576 || "$output" != '{"'* ]]; then
  echo 'ATTACHMENT_CONTRACT_QA=PROCESS_OR_ISOLATION_FAILED' >&2
  exit 1
fi
printf '%s\n' "$output"
echo 'ATTACHMENT_CONTRACT_QA_CLEANUP=SUCCEEDED'
exit "$qa_exit"
