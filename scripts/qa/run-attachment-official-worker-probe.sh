#!/usr/bin/env bash
set -euo pipefail

# 명시한 설치 QA와 별도 시험 JAR만 사용한다. 기존 합성 QA의 네트워크 격리는 변경하지 않는다.
[[ ( $# -eq 4 || $# -eq 5 ) && "$(uname -s)" == Linux && "$(id -u)" != 0 ]] || exit 1
case_group="${5:-YANGPYEONG}"
[[ "$case_group" == YANGPYEONG || "$case_group" == TAEBAEK || "$case_group" == TAEBAEK_HWP || "$case_group" == CHUNGJU || "$case_group" == JECHEON || "$case_group" == BOEUN || "$case_group" == BOEUN_SEGMENT || "$case_group" == BOEUN_STRUCTURAL ]] || exit 1
qa_distribution="$(realpath -- "$1")"
probe_jar="$(realpath -- "$2")"
[[ "$3" =~ ^[a-f0-9]{64}$ && "$4" =~ ^[a-f0-9]{64}$ ]] || exit 1
[[ -f "$probe_jar" && "$(sha256sum -- "$probe_jar" | cut -d' ' -f1)" == "$3" ]] || exit 1
[[ -d "$qa_distribution/lib" && -d "$qa_distribution/extractor" && -f "$qa_distribution/config/logback-qa.xml" ]] || exit 1
for tool in /usr/bin/bwrap /usr/bin/prlimit /usr/bin/timeout; do [[ -x "$tool" ]] || exit 1; done
java_binary="$(readlink -f -- "$(command -v java)")"
java_home="$(dirname -- "$(dirname -- "$java_binary")")"
work="$(mktemp -d /tmp/saneb-official-worker.XXXXXXXX)"
cleanup() {
  local resolved
  resolved="$(realpath -- "$work")"
  [[ "$(dirname -- "$resolved")" == /tmp && "$(basename -- "$resolved")" == saneb-official-worker.* ]] || return 1
  rm -rf -- "$resolved"
}
trap cleanup EXIT
chmod 700 "$work"
mkdir "$work/tmp"
mounts=(--ro-bind /usr /usr --ro-bind /bin /bin --ro-bind /lib /lib --ro-bind "$java_home" /jre)
[[ ! -d /lib64 ]] || mounts+=(--ro-bind /lib64 /lib64)
for file in /etc/ld.so.cache /etc/passwd /etc/group /etc/nsswitch.conf /etc/hosts /etc/resolv.conf; do
  [[ ! -f "$file" ]] || mounts+=(--ro-bind "$file" "$file")
done
[[ ! -d /etc/ssl/certs ]] || mounts+=(--ro-bind /etc/ssl/certs /etc/ssl/certs)
if [[ -f "$java_home/conf/security/java.security" ]]; then
  java_security="$(readlink -f -- "$java_home/conf/security/java.security")"
  [[ "$java_security" == "$java_home/"* ]] || mounts+=(--ro-bind "$java_security" "$java_security")
fi
# Debian/Ubuntu JDK는 디렉터리가 아니라 개별 .policy 파일이 /etc 아래로 연결될 수 있다.
# 설치된 고정 정책 파일만 읽기 전용 연결하며 crypto.policy/인증서 검증을 변경하지 않는다.
[[ -d "$java_home/conf/security/policy" ]] || exit 1
java_crypto_policy="$(readlink -f -- "$java_home/conf/security/policy")"
[[ "$java_crypto_policy" == "$java_home/"* ]] || mounts+=(--ro-bind "$java_crypto_policy" "$java_crypto_policy")
for java_policy_relative in limited/default_US_export.policy limited/default_local.policy limited/exempt_local.policy unlimited/default_US_export.policy unlimited/default_local.policy; do
  [[ -f "$java_home/conf/security/policy/$java_policy_relative" ]] || exit 1
  java_policy_file="$(readlink -f -- "$java_home/conf/security/policy/$java_policy_relative")"
  [[ "$java_policy_file" == "$java_home/"* ]] || mounts+=(--ro-bind "$java_policy_file" "$java_policy_file")
done
# 공식 사이트 요청 때문에 네트워크는 공유한다. 공개 요청은 기존 profile/DNS/redirect pinning과
# 공고당44요청/80MiB(BOEUN_SEGMENT·BOEUN_STRUCTURAL는5요청/24MiB)로 제한한다.
# 운영 home/env/socket은 공유하지 않으며 파일 추출은 별도 network 격리다.
# PID namespace 전체 종료로 임시 PostgreSQL 자손을 회수한다. 원문 보고서는 외부로 보관하지 않는다.
if output="$(env -i PATH=/usr/bin:/bin LANG=C.UTF-8 \
  /usr/bin/timeout --kill-after=5 1400 \
  /usr/bin/prlimit --as=2147483648 --cpu=900 --nofile=256 --nproc=128 --fsize=134217728 -- \
  /usr/bin/bwrap --unshare-user --unshare-pid --unshare-ipc --unshare-uts --unshare-cgroup-try \
  --die-with-parent --new-session --cap-drop ALL \
  "${mounts[@]}" --proc /proc --dev /dev --tmpfs /dev/shm --tmpfs /tmp \
  --ro-bind "$qa_distribution" /qa --ro-bind "$probe_jar" /probe.jar --bind "$work" /work --chdir /work \
  --clearenv --setenv PATH /usr/bin:/bin --setenv LANG C.UTF-8 --setenv HOME /work --setenv TMPDIR /work/tmp \
  --setenv SANEB_ATTACHMENT_OFFICIAL_WORKER_QA true \
  /jre/bin/java -Xms32m -Xmx384m -XX:ActiveProcessorCount=1 -XX:+UseSerialGC \
  -XX:MaxMetaspaceSize=192m -XX:CompressedClassSpaceSize=64m -XX:ReservedCodeCacheSize=64m -Xss512k \
  -Djava.io.tmpdir=/work/tmp -Djava.net.preferIPv4Stack=true -Dlogback.configurationFile=/qa/config/logback-qa.xml \
  -Djava.util.logging.config.file=/qa/config/logging.properties \
  -cp '/probe.jar:/qa/lib/*' com.saneb.db.AnnouncementAttachmentOfficialWorkerProbe "$4" "$case_group" 2>/dev/null)"; then
  probe_exit=0
else
  probe_exit=$?
fi
cleanup
trap - EXIT
[[ -n "$output" && ${#output} -le 65536 && "$output" == '{"'* ]] || {
  echo 'OFFICIAL_WORKER_PROBE=PROCESS_OR_ISOLATION_FAILED' >&2; exit 1;
}
printf '%s\n' "$output"
echo 'OFFICIAL_WORKER_PROBE_CLEANUP=SUCCEEDED'
exit "$probe_exit"
