# 기존 서버 배포 및 첨부 실파일 QA

## 실행 범위

사용자는 이 서버가 현재 실사용 서비스가 아니라고 확인하고 보수적인 사전 검증 생략을 지시했다. 별도 staging/QA 서버 구축, 사전 격리 인증, 단계적 canary 출시를 이번 실행의 선행 blocker로 두지 않는다. 기존 서버에 배포한 뒤 실제로 실행하여 결과를 판정한다.

비밀정보 보호, 기존 데이터 보존, 파서의 네트워크·파일시스템 격리는 유지한다. 이 승인은 공고 자동 활성화, 규칙 ACTIVE 게시, 기존 데이터 재분류나 삭제를 뜻하지 않는다. 이번 작업은 실제 서버에서 고정 공개 표본의 다운로드·추출·단일 파일 분류를 실행하는 범위다.

## 변경

- CodeDeploy bundle에 독립 `attachment-extractor` 배포본과 QA 규칙 스냅샷을 포함한다. 설치 경로는 기존 설정 기본값 `/opt/saneb/attachment-extractor`다.
- 필요한 Linux `bubblewrap` 패키지가 없으면 배포 hook에서 설치한다. 서비스 환경값·기존 플래그를 새로 활성화하지 않는다.
- `workflow_dispatch`의 `attachment_qa=true`인 배포에서만 후속 QA hook을 실행한다. 일반 push/기본 배포에서는 표본 다운로드를 실행하지 않는다.
- 규칙 스냅샷은 빌드 머신의 임시 PostgreSQL에서 Flyway DRAFT seed를 읽어 생성한다. 서버 QA에서는 Spring context·DB 연결·scheduler를 시작하지 않는다.
- 별도 CLI main을 기존 bootJar의 PropertiesLauncher로 로드한다. 고정된 기업마당 공개 표본 4개를 제목 Gate → IP 고정 다운로드 → Linux bwrap/prlimit 추출 → 기존 분류 엔진 순서로 실행한다.
- 표본·역할·범위는 로컬 실파일 QA와 같다. `SINGLE_SELECTED_FILE_DIAGNOSTIC`, 공고 전체 첨부 집합 미검증, 운영 데이터 쓰기 0으로 명시한다. 실제 운영 ACTIVE 규칙을 사용했다고 표현하지 않는다.
- 원본·상세 HTML은 작업 전용 임시 경로에서 제거한다. CodeDeploy 출력에는 원문 대신 해시·개수·품질·판정 코드만 남긴다.

## 진행 체크리스트

- [x] AGENTS 및 현재 Git·배포 경로 확인. 기준 HEAD a84893b, 기존 로컬 QA 변경 보존.
- [x] 실사용 중단 위험에 대한 사용자 판단 반영. 사전 검증을 다시 승인 조건으로 요구하지 않음.
- [x] GitHub 저장 계정의 repository push 권한과 서울 리전 확인. 전역 활성 계정 변경 없음.
- [x] 추가 변경의 컴파일·제한된 단위 테스트·DRAFT snapshot 생성.
- [x] 한국어 commit, push, 첨부 QA 옵션을 포함한 명시적 배포 실행.
- [!] CodeDeploy ValidateService의 기존 scripts/validate.sh 시간 초과. 서버 QA hook에는 도달하지 못함.
- [!] GitHub 배포 역할의 ssm:SendCommand 권한 없음. 로컬 AWS 세션 재인증 필요.
- [x] 로컬 Gradle/Node 종료 및 실제 배포 결과 기록. 서버 기동 상태는 직접 확인하지 못함.

## 상태 구분

서버 배포와 표본 QA 성공은 상시 첨부 수집 기능 완성이 아니다. worker/lease/DB 영속 저장/관리자 API·화면/ENFORCE/기존 데이터 배치는 여전히 별도 구현 항목이다. 새 QA 실행 명령은 API endpoint나 scheduler를 제공하지 않는다.

QA 실패는 품질 코드를 포함하여 보고하며 실패한 실행을 성공으로 처리하지 않는다. 이미지 PDF의 OCR_REQUIRED, 불완전/부정 문맥 검수는 예상 동작으로 검증한다. 서버 health 및 V72 실제 반영은 배포 후 별도로 확인할 때까지 미확인이다.

## 검증 기록

- `bash -n scripts/prepare-attachment-extractor.sh`, `bash -n scripts/operations/run-attachment-server-qa.sh`: 성공.
- 최초 `test --tests ...` 호출은 동일 이름의 subproject test에도 필터가 적용되어 추출기 test의 'No tests found'로 실패했다. root 프로젝트 `:test`로 범위를 정정했다. 테스트 삭제나 검증 옵션 완화는 하지 않았다.
- 로컬 AWS STS는 세션 만료로 실패했다. 기존 GitHub Actions OIDC 배포 인증 경로를 사용한다. 인증값은 코드·문서·로그에 기록하지 않는다.
- 브라우저 제어/QA는 현재 요청에 명시되지 않아 사용자 정책상 미실행이다.
- 정정 명령 `:test --tests '*AnnouncementAttachmentServerQaTest' --tests '*IsolatedAttachmentExtractorTest' --tests '*DockerAttachmentQaRunnerTest' attachmentQaRuleSnapshot bootJar :attachment-extractor:installDist`: 성공(25초). root 대상 테스트 6건 통과, snapshot round-trip 1건 통과. 이전 실행에서 완료된 root test/bootJar는 재사용했다.
- 생성된 DRAFT snapshot은 394개 규칙이며 JSON 직렬화 round-trip을 확인했다. Windows에서 PropertiesLauncher가 QA main까지 진입하고 플랫폼 조건으로 종료함을 확인했다. 이 호출은 Linux 추출 성공의 근거가 아니다.

## 실제 배포 결과

- 구현 commit `7bf4f6415f8b1adf419dde56ac12b03aab3a5282`를 master로 push한 후 `attachment_qa=true`로 [배포 실행](https://github.com/FrostyCityMan/saneB/actions/runs/34342467397)을 즉시 시작했다. commit의 skip deploy 표시는 중복 push 배포만 방지하며 이 수동 실행은 실제 진행됐다.
- GitHub Actions의 테스트·빌드, 배포 묶음 생성, OIDC 인증, S3 업로드는 성공했다.
- CodeDeploy `d-R1YA1LOPK`: Failed / HEALTH_CONSTRAINTS. ApplicationStop, DownloadBundle, BeforeInstall, Install, AfterInstall, ApplicationStart는 Succeeded였다.
- ValidateService는 ScriptTimedOut이며 실패 스크립트는 `scripts/validate.sh`다. 이 스크립트 다음 순서인 첨부 QA hook은 실행되지 않았다. Linux 실파일 추출·분류를 성공으로 보고하지 않는다.
- 자동 원복 배포 `d-K39SY9OPK`도 Failed / HEALTH_CONSTRAINTS였다. 이전 버전 복구 성공 또는 현재 서비스 정상 여부를 단정하지 않는다. V72 적용 여부와 DB 상태도 직접 재조회하지 못했다.
- [SSM 포함 진단](https://github.com/FrostyCityMan/saneB/actions/runs/34343151864)은 배포 정보 조회까지 성공했으나 ssm:SendCommand AccessDenied로 서버 로그를 읽지 못했다.
- [CodeDeploy 전용 재진단](https://github.com/FrostyCityMan/saneB/actions/runs/34343343903)은 성공했고 실패 스크립트 이름 및 자동 원복 실패를 확인했다. 진단용 workflow만 보강했으며 재배포 반복은 하지 않았다.
- 로컬 bootJar manifest의 Start-Class는 기존 `com.saneb.SaneBApplication`으로 확인했다. QA main이 웹 애플리케이션 시작 클래스를 대체한 문제는 아니다.
- 로컬 AWS는 만료 상태이고 GitHub 배포 역할로 SSM 실행은 불가하다. 사용자에게 `aws login --region ap-northeast-2` 재인증을 요청했다. 인증 후 실제 기동 로그·서비스 상태 확인, 시간 초과 원인 수정, 재배포 및 서버 QA를 이어간다. 원인이 확인되지 않은 상태에서 health 검증을 성공 처리하거나 임의의 DB 변경을 하지 않는다.

## AWS 재인증 이후 복구·실파일 진단

- 위 배포 실패 기록은 당시 상태다. 새 AWS 인증 후 로컬 Windows 신뢰 인증서 묶음으로 TLS 검증을 유지하면서 서울 리전 STS·SSM 접근을 확인했다. GitHub 배포 역할의 IAM 권한은 변경하지 않았다.
- 애플리케이션 기동 로그의 직접 실패는 PostgreSQL `NoRouteToHostException`이었다. 서버 DB TCP 접속도 errno 113으로 실패했고 HTTP listener가 없었다. systemd의 active 상태만으로 정상 기동을 판정하지 않았다.
- `saneb-dev-aurora`와 writer는 `inaccessible-encryption-credentials-recoverable`이었다. RDS 이벤트에 2026-09-07 11:59/12:09 KST 암호화 키 접근 실패 및 12:12 KST 중단이 기록됐다. 현재 AWS 관리형 `alias/aws/rds`는 Enabled이므로 과거 접근 상실의 세부 원인은 미확정이다.
- 사용자 승인 후 2026-09-09 22:52 KST `start-db-cluster`를 실행했다. 상태가 `starting`으로 바뀌었으며, KMS/IAM/보안그룹/삭제 보호는 변경하지 않았고 직접 데이터 조작 명령은 실행하지 않았다. DB 정상화·재배포 성공은 후속 결과로 별도 기록한다.
- DB 기동 대기 중 실패 배포 archive의 독립 CLI로 공개 표본 4개를 실제 서버에서 실행했다. 상세·첨부 다운로드 4개는 성공했으나 모두 `LINUX_EXTRACTION / FAILED`였다. 임시 원본은 모두 제거했고 QA의 운영 DB 쓰기는 0이다. 배포 hook 성공으로 표현하지 않는다.
- 원인은 Linux JVM 메모리 예약이다. 동일 bwrap 격리의 `java -version`으로 512 MiB 주소 공간/256 MiB heap에서 VM heap 예약 실패를 재현했다. 768 MiB 주소 공간에서는 시작됐으나 기존 512 MiB 상한을 유지하기 위해 채택하지 않았다. 512 MiB/192 MiB heap도 metaspace 예약에 실패했고 512 MiB/128 MiB heap은 성공했다.
- 추출기 heap만 128 MiB로 낮춘다. 주소 공간 512 MiB, CPU 30초, 출력 8 MiB, PID/네트워크/파일시스템 격리는 유지한다. JVM 시작 성공과 실제 PDF/HWP/HWPX 추출 성공은 별도로 검증한다. 큰 문서의 메모리 부족 가능성은 남으며 이를 정상 추출로 취급하지 않는다.
- 변경 후 로컬 `:test --tests '*IsolatedAttachmentExtractorTest' --tests '*AnnouncementAttachmentServerQaTest' bootJar --no-daemon --max-workers=1`: 성공(20초). 전체 suite 재실행은 GitHub 배포 workflow에서 수행하며 브라우저 검증은 사용자 정책상 생략한다.
