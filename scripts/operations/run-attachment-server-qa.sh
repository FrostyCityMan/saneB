#!/usr/bin/env bash
set -euo pipefail

distribution='/opt/saneb/attachment-extractor'
if [ "$(cat "${distribution}/run-server-qa" 2>/dev/null || true)" != 'true' ]; then
  echo 'ATTACHMENT_SERVER_QA=NOT_REQUESTED'
  exit 0
fi

work="$(mktemp -d /tmp/saneb-server-qa.XXXXXXXX)"
cleanup() {
  local resolved
  resolved="$(realpath -- "${work}")"
  case "${resolved}" in
    /tmp/saneb-server-qa.*) rm -rf -- "${resolved}" ;;
    *) echo 'ATTACHMENT_SERVER_QA_CLEANUP=INVALID_PATH' >&2; return 1 ;;
  esac
}
trap cleanup EXIT
chown ubuntu:ubuntu "${work}"
chmod 700 "${work}"

# Spring Boot launcher의 클래스 로딩만 사용한다. Spring application, DB, scheduler는 시작하지 않는다.
# SSM/CodeDeploy 출력에는 main이 만든 해시/상태/개수만 전달한다. app.env는 읽지 않는다.
timeout --kill-after=5 300 runuser -u ubuntu -- env -i PATH=/usr/bin:/bin LANG=C.UTF-8 HOME="${work}" \
  /usr/bin/java -Xms16m -Xmx192m -XX:ActiveProcessorCount=1 \
  -Dloader.main=com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentServerQa \
  -cp /home/ubuntu/app/app.jar org.springframework.boot.loader.launch.PropertiesLauncher "${distribution}" "${work}"
