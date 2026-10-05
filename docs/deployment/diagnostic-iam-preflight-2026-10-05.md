# 서울 진단 IAM 보완 및 운영 사전 점검

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

## 참고

- AWS 권한 참조: https://docs.aws.amazon.com/service-authorization/latest/reference/list_ssm.html
- 로컬 실행 도구: `build/qa-tools/Invoke-SanebApprovedIamDiagnostic.ps1` (Git 제외 경로). 기본 조회, `-Apply`는 동일 이름 정책 존재 시 덮어쓰지 않고 중단한다.
