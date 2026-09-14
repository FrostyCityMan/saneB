#!/usr/bin/env bash
set -euo pipefail

# 임시 GitHub runner 전용이다. QA JVM/PG를 runner 공용 UID와 분리하며 기존 자원 한도는 유지한다.
if [[ $# -ne 0 || "$(uname -s)" != Linux || "${GITHUB_ACTIONS:-}" != true || "$(id -u)" == 0 ]]; then
  echo 'POLICY_DB_QA=EPHEMERAL_CI_NON_ROOT_REQUIRED' >&2
  exit 1
fi
qa_root="$(git rev-parse --show-toplevel)"
[[ "$(pwd -P)" == "$qa_root" && "$(git rev-parse HEAD)" == "${GITHUB_SHA:-}" ]] || exit 1
qa_java="$(readlink -f -- "$(command -v java)")"
[[ -x "$qa_java" && "$qa_java" == */bin/java ]] || exit 1
qa_java_home="$(dirname -- "$(dirname -- "$qa_java")")"
qa_account=saneb-policy-qa
qa_uid=''
qa_gid=''
qa_work=''
qa_account_created=false
qa_runner_uid="$(id -u)"
qa_runner_gid="$(id -g)"
qa_report_name=TEST-com.saneb.domain.announcementattachment.service.impl.AttachmentWorkerDbQaLinuxIntegrationTest.xml

# 기존 계정/그룹을 재사용하거나 지우지 않는다.
if getent passwd "$qa_account" >/dev/null || getent group "$qa_account" >/dev/null; then
  echo 'POLICY_DB_QA=ACCOUNT_ALREADY_EXISTS' >&2
  exit 1
fi
cleanup() {
  local failed=0 resolved current_uid current_gid probe_status
  if [[ "$qa_account_created" == true ]]; then
    current_uid="$(id -u "$qa_account")" || return 1
    [[ "$qa_uid" =~ ^[1-9][0-9]*$ && "$current_uid" == "$qa_uid" && "$qa_uid" != "$qa_runner_uid" ]] || return 1
    # 이 실행에서 생성한 UID만 종료한다. runner 공용 프로세스와 다른 계정은 건드리지 않는다.
    if sudo -n pkill -TERM -u "$qa_uid"; then :; else [[ $? == 1 ]] || failed=1; fi
    for attempt in {1..10}; do
      if pgrep -u "$qa_uid" >/dev/null; then sleep 0.2; else probe_status=$?; [[ "$probe_status" == 1 ]] || failed=1; break; fi
    done
    if sudo -n pkill -KILL -u "$qa_uid"; then :; else [[ $? == 1 ]] || failed=1; fi
    for attempt in {1..10}; do
      if pgrep -u "$qa_uid" >/dev/null; then sleep 0.2; else probe_status=$?; [[ "$probe_status" == 1 ]] || failed=1; break; fi
    done
    if pgrep -u "$qa_uid" >/dev/null; then failed=1; else [[ $? == 1 ]] || failed=1; fi
  fi
  if [[ -n "$qa_work" ]]; then
    resolved="$(realpath -- "$qa_work")" || return 1
    [[ ! -L "$qa_work" && "$resolved" == "$qa_work" && "$(dirname -- "$resolved")" == /tmp
       && "$(basename -- "$resolved")" == saneb-policy-parent-qa.* ]] || return 1
    sudo -n rm -rf -- "$resolved" || failed=1
    [[ ! -e "$resolved" && ! -L "$resolved" ]] || failed=1
  fi
  if [[ "$qa_account_created" == true ]]; then
    sudo -n userdel "$qa_account" || failed=1
    if getent group "$qa_account" >/dev/null; then
      current_gid="$(getent group "$qa_account" | cut -d: -f3)"
      if [[ "$current_gid" == "$qa_gid" ]]; then sudo -n groupdel "$qa_account" || failed=1; else failed=1; fi
    fi
    if getent passwd "$qa_account" >/dev/null || getent group "$qa_account" >/dev/null; then failed=1; fi
  fi
  if [[ "$failed" != 0 ]]; then echo 'POLICY_DB_QA_CLEANUP=FAILED' >&2; return 1; fi
  echo 'POLICY_DB_QA_CLEANUP=SUCCEEDED'
}
finish() {
  local result=$?
  trap - EXIT
  if ! cleanup; then result=1; fi
  exit "$result"
}
trap finish EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

sudo -n useradd --system --user-group --no-create-home --shell /usr/sbin/nologin "$qa_account"
qa_account_created=true
qa_uid="$(id -u "$qa_account")"
qa_gid="$(id -g "$qa_account")"
qa_work="$(mktemp -d /tmp/saneb-policy-parent-qa.XXXXXXXX)"
sudo -n chown "$qa_uid:$qa_gid" "$qa_work"
sudo -n -u "$qa_account" mkdir "$qa_work/source" "$qa_work/home" "$qa_work/gradle"
# .git/runner 인증 설정/미추적 파일/기존 build를 복사하지 않고 정확한 커밋만 전달한다.
git archive --format=tar HEAD | sudo -n -u "$qa_account" tar -xf - -C "$qa_work/source"

qa_exit=0
# 외부 자격증명이나 runner 환경을 상속하지 않는다. 공개 Gradle/Maven 의존성만 새로 받는다.
if sudo -n -u "$qa_account" env -i PATH="$qa_java_home/bin:/usr/bin:/bin" LANG=C.UTF-8 \
    JAVA_HOME="$qa_java_home" HOME="$qa_work/home" GRADLE_USER_HOME="$qa_work/gradle" \
    JAVA_OPTS='-XX:ActiveProcessorCount=1 -XX:+UseSerialGC' \
    /usr/bin/timeout --kill-after=5 900 /bin/bash -c '
      set -euo pipefail
      cd -- "$1"
      options=(--no-daemon --console=plain --max-workers=1 "-Dorg.gradle.jvmargs=-Xmx512m -XX:ActiveProcessorCount=1 -XX:+UseSerialGC -Dfile.encoding=UTF-8")
      # 새 소스의 산출물을 준비한 단일-use daemon은 종료시킨 뒤 다음 JVM에서 실제 시험한다.
      /bin/bash ./gradlew attachmentContractQaTestClasses installAttachmentContractQa "${options[@]}"
      exec /bin/bash ./gradlew attachmentPolicyDbQaIntegrationTest --rerun "${options[@]}"' \
    saneb-policy-db-qa "$qa_work/source"; then :; else qa_exit=$?; fi

qa_report="$qa_work/source/build/test-results/attachmentPolicyDbQaIntegrationTest/$qa_report_name"
qa_target="$qa_root/build/test-results/attachmentPolicyDbQaIntegrationTest"
# 결과 부재는 실패다. 클래스 하나의 일반 XML 파일만 회수하며 symlink는 거부한다.
if sudo -n test -f "$qa_report" && ! sudo -n test -L "$qa_report"; then
  mkdir -p -- "$qa_target"
  sudo -n cp -- "$qa_report" "$qa_target/$qa_report_name"
  sudo -n chown "$qa_runner_uid:$qa_runner_gid" "$qa_target/$qa_report_name"
else
  echo 'POLICY_DB_QA=REQUIRED_REPORT_MISSING' >&2
  qa_exit=1
fi
exit "$qa_exit"
