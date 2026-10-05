# 서울 진단 IAM 보완 및 운영 사전 점검

## 2026-10-05 19:09 KST 승인 앱 코드 후속 설치 완료

- 사용자는 앱 코드 `5f0e274ea88e6c700a8fc671d098df6807e784ca`의 서울 서버 설치·재시작을 승인했다. 정부24 상세 기능 활성화·신규 수집·정책 게시·ENFORCE·기존 데이터 적용은 제외했다.
- Linux 계약 CI37261775271 성공과 실제 XML 검토 기록을 재확인했다. 기존 설치00d16d9 대비 migration/배포 hook 변경0이다. 로컬 Node 배포 계약18건 중15통과·Linux 전용3생략; 서버 성공으로 확대하지 않는다.
- 배포 전 읽기 전용 진단37293642573/SSM c5e03de0-4ccd-40ab-9764-c32fa2215a06 성공. V90·migration 실패0, 첨부 active/waiting job·QA·대기 일정0, 설치/기존 배포 JAR 일치. 과거 미종결 일정은 동일 ID·시각이었다.
- 불변 태그 `deploy-approved-5f0e274-20261005`를 승인 SHA로 생성·푸시하고 배포37293800988을 실행했다. headSha 일치, attachment_qa=false, CodeDeploy `d-378ZM2S6L` 성공이다. 진단/문서 최신 HEAD를 앱 코드로 대신 배포하지 않았다.
- 후속 읽기 전용 진단37294663729/SSM 99c1223b-8eb7-486f-bade-8b1ccf9ac639 성공. 서비스 시작19:07:03 KST, DB V90·migration 실패0·READ ONLY/ROLLBACK·진단 업무 쓰기0. 공개 health HTTP200/UP 확인.
- 설치/배포 원본 JAR SHA-256: `c9b7eeef6dee1b2bd00c41374dbdbf9d7884dc292273b6f49f7e7043964be667`. 이전 복구 JAR는 V90, `5cc938a7be4aa1228b2602c68fbbb9c1f731ecc0821f9f341d21c81fb4e23a0e`로 배포 직전 설치와 일치한다. 복구 실행 자체를 시험한 것은 아니다.
- 기존 개정2 COLLECT_ONLY 정책 ID/hash/row_version은 사전·사후 동일하다. worker/source batch true 유지, 정부24 상세 환경변수 UNSET 유지. 승인 코드 기본값 false이며 활성화 설정을 추가하지 않았다. 프로세스 환경변수 관측을 모든 외부 Spring 설정의 유효값 검증으로 확대하지 않는다.
- 후속 첨부 active/waiting job·정책/공급자 QA·대기 일정0. 과거 미종결 일정1건은 동일 ID·시각으로 남았다. 전체 업무 데이터 불변을 별도 전수 대조한 것은 아니다.
- 코드 설치 하위 Gate만 완료했다. 정부24 실제 API·첨부 모델·실파일 수집·검수/DRAFT·기존 데이터 적용·운영 브라우저 E2E는 별도 미완료다. 이번 요청은 설치 승인으로 한정되어 브라우저 검증은 미실행이다.

## 승인과 변경 범위

- 사용자가 기존 GitHub 배포 역할의 최소 SSM 권한 보완을 승인하고 root 계정을 지정했다. 로그인 갱신 후 STS root 계정, 저장소 역할 ARN, 최신 성공 배포의 단일 인스턴스와 로컬 대상 pin의 일치를 확인했다.
- `saneb-github-actions-role`에 인라인 정책 `SanebApprovedDiagnosticSeoulSingleInstance`를 추가했다. 기존 인라인 정책 2개, 신뢰 정책, 관리형 정책 및 SSO 조회 역할은 변경하지 않았다.
- `ssm:SendCommand`: 서울의 승인된 EC2 인스턴스 1대와 AWS 소유 `AWS-RunShellScript` 문서로 제한했다.
- `ssm:GetCommandInvocation`: 리소스 단위 권한 제한을 지원하지 않아 `Resource: *`와 서울 리전 조건을 사용한다. 다른 명령 결과를 IAM 수준에서 단일 인스턴스로 제한했다고 해석하면 안 된다.
- `AWS-RunShellScript` 자체는 임의 쉘 실행 권한이다. 읽기 전용은 고정 진단 코드의 동작이며 IAM이 SQL/명령 내용을 읽기 전용으로 강제하는 것은 아니다.
- AWS Access Analyzer 정책 검사 findings 0건, 추가 정책 재조회 일치 확인. 임시 공개 CA 및 정책 전송 파일은 정리했다.

## 실행 결과

- Git 기준선: `62b453c4774893e00c45659da3ab59875eaaca8d`.
- GitHub 진단: https://github.com/FrostyCityMan/saneB/actions/runs/37256100474 — 성공.
- 진단 대상 최신 성공 CodeDeploy: `d-R1AEF7V4L`.
- SSM 명령: `9cba8ed2-40e0-47b0-97a8-625f20ab19d8`.
- 관측 시각: 2026-10-05 11:37 KST.
- DB 읽기 전용 transaction 확인 후 ROLLBACK, 업무 데이터 쓰기 0.
- DB migration V86, migration 실패 0.
- 첨부 active/waiting job, 정책 QA, 공급자 QA, 대기 일정 각각 0.
- 수집 일정 실행 이력 중 RUNNING 1건. 실제 실행 중인지 미종결 이력인지 이번 집계만으로 확정하지 않는다.
- 설치 JAR과 이전 복구용 JAR 존재, 양쪽 내장 migration 최대 V86.
- 설치 JAR SHA-256: `403127f717293c73989685b874d487cf76e0031a6b7dd010833e7c9f8402b390`.
- 첨부 worker와 source batch 환경 플래그 true. 이는 수집 성공 증거가 아니다.
- 공개 health 조회 UP. 브라우저 검증은 현재 요청 정책에 따라 미실행.

## 남은 Gate

- V87~V90은 운영 DB에 아직 반영되지 않았다. 이번 작업은 IAM 보완과 읽기 전용 진단이며 코드 배포, 서비스 재시작, 정책 게시, ENFORCE, 기존 데이터 처리는 수행하지 않았다.
- 재시작 전 RUNNING 수집 이력 1건의 실제 실행 여부와 종료/회복 조건을 확인한다. 임의 취소나 상태 갱신은 수행하지 않는다.
- 복구 JAR 존재는 DB 복구 검증을 대신하지 않는다. DB 백업/복구 및 승인된 배포 artifact 검증은 별도 Gate다.

## 후속 읽기 전용 대조 — 2026-10-05 11:45 KST

- 진단 run https://github.com/FrostyCityMan/saneB/actions/runs/37256580005 성공. 진단 코드 `787ae2a`; 로컬 안전성 테스트 9건 통과.
- RUNNING 실행 `44681524-e5b7-4dcd-808f-204823043d39`: 예정 시각 2026-09-07 12:30 KST, 마지막 갱신 2026-09-09 22:56:52 KST, 연결 run 없음.
- 현재 서비스 시작 시각 2026-10-02 21:37:36 KST. 이 이력은 현재 프로세스보다 오래되었으며, 서비스 코드가 실행 슬롯을 새로 선점한 경우에만 동기 수집을 시작하고 기존 슬롯 충돌 시 반환하는 구조와 대조했다. 현재 프로세스가 수행 중인 해당 작업이 아니라 과거 미종결 슬롯으로 판단한다. 최초 중단 원인까지 증명한 것은 아니다.
- 이력 상태·스케줄·운영 데이터는 수정하지 않았다. 이 미종결 이력은 재시작을 막는 현재 실행 작업으로 계산하지 않지만 별도 운영 정합성 개선 항목으로 남긴다.
- `saneb-readonly`의 실제 계정·역할 pin 검증 후 RDS 메타데이터를 조회했다. 실행 서비스의 DB endpoint SHA-256과 Aurora endpoint 일치, 상태 available, 자동 백업 보존 7일, 암호화 및 삭제 방지 true.
- 조회 당시 복원 가능 구간: 2026-09-28 04:04:10 ~ 2026-10-05 11:44:08 KST. 실제 복원은 실행하지 않았으므로 복원 시험 통과로 표현하지 않는다.
- 남은 배포 Gate는 승인된 `00d16d9` artifact/배포 ref 고정, migration 및 롤백 호환성 최종 대조다. 새 진단 HEAD를 승인된 앱 코드 대신 배포하지 않는다.

## 승인 코드 배포 착수 — 2026-10-05 11:49 KST

- 이전 설치 코드 `371b1ac` 대비 migration 변경은 V87~V90 추가 4개이며 기존 migration 수정 없음.
- 동일 SHA `00d16d9cecd01159006a316c5cc7be3c30fd64f6` CI run37087157343 성공을 재조회했다.
- 배포/복구 helper 및 테스트가 승인 SHA와 동일함을 대조하고 Node 배포 계약 시험을 실행했다. Windows 결과 15통과/실패0/Linux 전용 생략3. Node·bash 테스트 프로세스 종료 및 fixture 자동 정리.
- 태그 `deploy-approved-00d16d9-20261005`를 정확한 승인 SHA로 생성·푸시했다. 새로운 진단 코드를 앱 배포 대상으로 사용하지 않았다.
- 배포 run https://github.com/FrostyCityMan/saneB/actions/runs/37256870718 시작. `headSha` 일치, `attachment_qa=false`. 시작 시점 테스트·빌드 진행 중이며 서버 설치 완료 증거는 아직 없다.
- 기존 환경 플래그 보존, 정책 게시·ENFORCE·기존 데이터 처리 없음. 이번 배포가 성공해도 실파일·검수/DRAFT·연결 근거 E2E Gate가 자동 완료되지는 않는다.

## 참고

### 배포 후 직접 검증 — 2026-10-05 11:56 KST

- 배포 run37256870718 성공, 정확한 코드 `00d16d9cecd01159006a316c5cc7be3c30fd64f6`, CodeDeploy `d-A8GG4SJ6L`. Linux 테스트·빌드 단계 5분19초 성공.
- 후속 진단 https://github.com/FrostyCityMan/saneB/actions/runs/37257357172 성공. SSM `0b5d725b-2f1a-44b3-8580-54ab0be7657c`, DB READ ONLY/ROLLBACK, 진단 업무 쓰기0.
- 운영 DB V90, migration 실패0. 서비스 시작 2026-10-05 11:55:04 KST, 공개 health UP.
- 설치 JAR와 해당 배포 원본 JAR의 SHA-256이 `5cc938a7be4aa1228b2602c68fbbb9c1f731ecc0821f9f341d21c81fb4e23a0e`로 일치한다. 이전 V86 JAR은 `403127f717293c73989685b874d487cf76e0031a6b7dd010833e7c9f8402b390`으로 보존됐다.
- 활성 정책은 기존 개정2 `7df4c0a2-0d6f-402b-bb63-6ce39f2fef9c`, COLLECT_ONLY, row_version2. 정책 재게시·ENFORCE·기존 데이터 처리 없음.
- 첨부 active/waiting job·정책/공급자 QA·대기 일정0. 과거 미종결 일정1은 동일 ID/시각으로 보존. worker/source batch 플래그 true 유지.
- 코드 설치·migration 하위 Gate는 완료했다. 현재 설치에 대한 정책 지문/검증 범위, 실제 파일 worker, 운영 브라우저, 검수→DRAFT 및 기존 데이터 배치의 전체 Gate는 별도 미완료다.

- AWS 권한 참조: https://docs.aws.amazon.com/service-authorization/latest/reference/list_ssm.html
- 로컬 실행 도구: `build/qa-tools/Invoke-SanebApprovedIamDiagnostic.ps1` (Git 제외 경로). 기본 조회, `-Apply`는 동일 이름 정책 존재 시 덮어쓰지 않고 중단한다.
