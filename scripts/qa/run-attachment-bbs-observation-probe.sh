#!/usr/bin/env bash
set -euo pipefail

# 태백1건·옥천/보은/남구3건의 명시된 관측만 허용한다. 자원 제한은 동일하다.
[[ ( $# -eq 4 || ( $# -eq 5 && ( "$5" == FIXED || "$5" == OKCHEON || "$5" == BOEUN_OBSERVATION || "$5" == BOEUN_DIAGNOSTIC || "$5" == OKCHEON_DIAGNOSTIC || "$5" == NAMGU_OBSERVATION || "$5" == NAMGU_STRUCTURE || "$5" == DALSEONG_OBSERVATION || "$5" == DALSEONG_HEADER ) ) ) && "$(uname -s)" == Linux && "$(id -u)" != 0 ]] || exit 1
probe_flag=SANEB_ATTACHMENT_BBS_OFFICIAL_OBSERVATION
probe_args=("$4")
if [[ $# -eq 5 ]]; then
  [[ "$5" != FIXED ]] || probe_flag=SANEB_ATTACHMENT_BBS_FIXED_CASE_QA
  probe_args+=("$5")
fi
qa_distribution="$(realpath -- "$1")"
probe_jar="$(realpath -- "$2")"
[[ "$3" =~ ^[a-f0-9]{64}$ && "$4" =~ ^[a-f0-9]{64}$ ]] || exit 1
[[ -f "$probe_jar" && "$(sha256sum -- "$probe_jar" | cut -d' ' -f1)" == "$3" ]] || exit 1
[[ -d "$qa_distribution/lib" && -d "$qa_distribution/extractor" && -f "$qa_distribution/config/logback-qa.xml" ]] || exit 1
for tool in /usr/bin/bwrap /usr/bin/prlimit /usr/bin/timeout; do [[ -x "$tool" ]] || exit 1; done
java_binary="$(readlink -f -- "$(command -v java)")"
java_home="$(dirname -- "$(dirname -- "$java_binary")")"
mockito=("$qa_distribution"/lib/mockito-core-*.jar)
[[ ${#mockito[@]} -eq 1 && -f "${mockito[0]}" ]] || exit 1
mockito_name="$(basename -- "${mockito[0]}")"
work="$(mktemp -d /tmp/saneb-bbs-qa.XXXXXXXX)"
cleanup() {
  local resolved
  resolved="$(realpath -- "$work")"
  [[ "$(dirname -- "$resolved")" == /tmp && "$(basename -- "$resolved")" == saneb-bbs-qa.* ]] || return 1
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
[[ -d "$java_home/conf/security/policy" ]] || exit 1
java_crypto_policy="$(readlink -f -- "$java_home/conf/security/policy")"
[[ "$java_crypto_policy" == "$java_home/"* ]] || mounts+=(--ro-bind "$java_crypto_policy" "$java_crypto_policy")
for java_policy_relative in limited/default_US_export.policy limited/default_local.policy limited/exempt_local.policy unlimited/default_US_export.policy unlimited/default_local.policy; do
  [[ -f "$java_home/conf/security/policy/$java_policy_relative" ]] || exit 1
  java_policy_file="$(readlink -f -- "$java_home/conf/security/policy/$java_policy_relative")"
  [[ "$java_policy_file" == "$java_home/"* ]] || mounts+=(--ro-bind "$java_policy_file" "$java_policy_file")
done
# 공개 HTTP는 검증된 profile/DNS/redirect 경로만 사용한다. 추출기 자체는 --unshare-all이다.
if output="$(env -i PATH=/usr/bin:/bin LANG=C.UTF-8 \
  /usr/bin/timeout --kill-after=5 600 \
  /usr/bin/prlimit --as=2147483648 --cpu=540 --nofile=256 --nproc=128 --fsize=134217728 -- \
  /usr/bin/bwrap --unshare-user --unshare-pid --unshare-ipc --unshare-uts --unshare-cgroup-try \
  --die-with-parent --new-session --cap-drop ALL \
  "${mounts[@]}" --proc /proc --dev /dev --tmpfs /dev/shm --bind "$work/tmp" /tmp \
  --ro-bind "$qa_distribution" /qa --ro-bind "$probe_jar" /probe.jar --bind "$work" /work --chdir /work \
  --clearenv --setenv PATH /usr/bin:/bin --setenv LANG C.UTF-8 --setenv HOME /work --setenv TMPDIR /work/tmp \
  --setenv "$probe_flag" true \
  /jre/bin/java -Xms32m -Xmx256m -XX:ActiveProcessorCount=1 -XX:+UseSerialGC \
  -XX:MaxMetaspaceSize=128m -XX:CompressedClassSpaceSize=32m -XX:ReservedCodeCacheSize=32m -Xss512k \
  "-javaagent:/qa/lib/$mockito_name" \
  -Djava.io.tmpdir=/work/tmp -Djava.net.preferIPv4Stack=true -Dlogback.configurationFile=/qa/config/logback-qa.xml \
  -Djava.util.logging.config.file=/qa/config/logging.properties \
  -cp '/probe.jar:/qa/lib/*' com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsObservationProbe "${probe_args[@]}" 2>/dev/null)"; then
  probe_exit=0
else
  probe_exit=$?
fi
cleanup
trap - EXIT
[[ -n "$output" && ${#output} -le 20000 && "$output" == '{"'* ]] || {
  echo 'BBS_OBSERVATION_PROBE=PROCESS_OR_ISOLATION_FAILED' >&2; exit 1;
}
printf '%s\n' "$output"
echo 'BBS_OBSERVATION_PROBE_CLEANUP=SUCCEEDED'
exit "$probe_exit"
