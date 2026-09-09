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
- [ ] 한국어 commit, push, 첨부 QA 옵션을 포함한 명시적 배포 실행.
- [ ] CodeDeploy migration/health 및 서버 실파일 결과 확인.
- [ ] 임시 파일·실행 프로세스 정리 및 실제 결과 기록.

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
