# 본문 복구·첨부 상태 표시 운영 반영

## 승인 범위

- 사용자 요청: 운영 반영.
- 이번 변경 22개 파일만 커밋·푸시하고 서울 기존 CodeDeploy 경로로 코드 및 V91을 설치한다.
- 앱 코드: `2815c3cdd93bd5289a57b8a1d92ee2e233334e43`.
- 고정 태그: `deploy-approved-2815c3c-20261005`.
- 기존 사용자 미추적 SSO 도구, output 등은 커밋하지 않았다.
- 기존 데이터 본문 복구, 첨부 재수집, 정책 게시, ENFORCE, 정부24 활성화는 실행하지 않는다.
- 브라우저 검증은 현재 요청에서 명시하지 않아 정책상 미실행이다.

## 배포 전 확인

- 읽기 전용 진단 [37307754780](https://github.com/FrostyCityMan/saneB/actions/runs/37307754780) 성공.
- SSM: `bb13333a-1698-4077-9139-a90ac48f4a2f`.
- 설치/배포 원본 JAR 지문: `c9b7eeef6dee1b2bd00c41374dbdbf9d7884dc292273b6f49f7e7043964be667`, 내장 migration V90.
- DB V90, migration 실패 0, 첨부 active/waiting job 및 정책/공급자 QA/대기 일정 0.
- 과거 RUNNING 일정 1건은 이전 진단과 동일한 식별자·2026-09-09 갱신 시각·연결 run 없음이다. 현재 서비스 시작보다 오래된 미종결 이력이며 임의 수정하지 않는다.
- 기존 개정2 COLLECT_ONLY 정책과 worker/source batch 플래그를 보존한다. 진단 READ ONLY/ROLLBACK, 진단 업무 쓰기 0.
- 현재 배포 코드 대비 migration 변경은 V91 추가뿐이다. 기존 migration, 배포 hook, 추출기 코드 변경은 없다.
- 기존 JAR 백업 및 자동 복구 hook을 유지한다. V91은 신규 테이블/함수/trigger 추가이며 기존 데이터 변경이나 기존 스키마 삭제는 없다. DB 역방향 삭제/실제 롤백 훈련은 실행하지 않는다.

## 실행 및 검증

- Git 인증서 검증 오류는 명령별 `-c http.sslBackend=schannel`로 Windows 신뢰 저장소를 사용해 해결했다. TLS 검증을 비활성화하지 않았다.
- 로컬 Node UI/배포 계약 재검증: 52개 중 49통과, 실패0, Linux 전용3생략. 사용한 Node 프로세스 종료.
- 배포 [37307928739](https://github.com/FrostyCityMan/saneB/actions/runs/37307928739): 고정 SHA 일치, `attachment_qa=false`. Linux 전체 테스트·패키지 검증 통과 후에만 설치한다.
- 동일 SHA 계약 CI [37307888030](https://github.com/FrostyCityMan/saneB/actions/runs/37307888030): 운영 데이터/외부 공고를 사용하지 않는 격리 검증이다.
- 배포 workflow 성공. Linux 전체 테스트·빌드 단계 `BUILD SUCCESSFUL in 5m 7s` 확인.
- CodeDeploy `d-VYKQQHT6L`이 2026-10-05 21:18 KST 성공했다. 해당 코드 SHA와 태그 일치.
- 공개 `/actuator/health` UP. 운영 정적 JavaScript에서 새 V2 첨부 조회와 본문 복구 경로, 기존 잘못된 빈 첨부 문구 제거를 확인했다.
- 인증·CSRF 없는 신규 복구 POST를 존재하지 않는 고정 UUID로 요청해 403 차단을 확인했다. 정상 본문 복구 호출이나 데이터 적용은 실행하지 않았다.
- 사후 진단 [37308770394](https://github.com/FrostyCityMan/saneB/actions/runs/37308770394)으로 설치 지문·V91·기존 정책을 최종 대조한다.

## 사후 운영 검증

- 위 사후 진단 성공. SSM `5cbb47dd-0f3e-479c-b3dd-1766dc0f23a3`, READ ONLY/ROLLBACK, 진단 업무 쓰기0.
- DB V91, migration 실패0. 서비스 시작 2026-10-05 21:18:14 KST.
- 설치 JAR와 배포 원본 JAR SHA-256 일치: `54c682e428114bc1f0f10993c683e88b2b4fa64323a45a367beca775a835c9b7`.
- 이전 복구 JAR는 V90이고 배포 전 설치 지문 `c9b7eeef6dee1b2bd00c41374dbdbf9d7884dc292273b6f49f7e7043964be667`과 일치한다. 실제 롤백 실행은 하지 않았다.
- 개정2 COLLECT_ONLY 정책의 id/hash/row_version이 사전 진단과 동일하다. worker/source batch true 및 정부24 상세 플래그 UNSET이 유지됐다. 다른 설정 경로까지 전수 검증했다고 확대하지 않는다.
- 첨부 active/waiting job, 정책/공급자 QA, 대기 일정0. 과거 미종결 일정은 같은 식별자·시각이다. 전체 업무 행의 불변성을 전수 대조한 것은 아니다.
- 기존 본문 복구나 첨부 재수집을 호출하지 않았다. 운영 화면 렌더링/정상 관리자 복구 적용/검수·초안 흐름은 이번 배포 검증에 포함하지 않는다.

## Linux 최종 검증 결과

계약 CI37307888030 성공. Artifact `11345417156`의 XML을 내려받아 직접 집계했다. 일반 테스트의 조건부 생략을 통과로 합산하지 않는다.

| 시험 | 테스트 수 | 실패/오류 | 생략 |
| --- | ---: | ---: | ---: |
| 일반 단위·HTTP | 4,690 | 0 | 402 |
| 추출기 | 251 | 0 | 0 |
| 계약 QA | 20 | 0 | 0 |
| 작업 DB | 229 | 0 | 0 |
| migration/backfill | 18 | 0 | 0 |
| 정책 부모 DB QA | 2 | 0 | 0 |
| 격리 추출기 runtime | 7 | 0 | 0 |
| worker DB | 12 | 0 | 0 |
| Flyway (V91 포함) | 6 | 0 | 0 |

- 별도 독립 패키지 `SYNTHETIC_WORKER_DB_CONTRACTS_V2`: 259/259 통과, 실패/생략0. 위 DB 시험과 중복이 있으므로 합산하지 않는다.
- 정책 부모 격리 실행의 `POLICY_DB_QA_CLEANUP=SUCCEEDED` 확인.
- 로컬에서 중단했던 전체 회귀의 부족한 증거를 동일 코드의 Linux CI로 보완했다. 실외부 공고·운영 브라우저 E2E 통과를 의미하지 않는다.

## Release Gate 최종 판정

Decision: **Ready to release — 승인된 코드·V91 운영 설치 범위 완료**.

| Gate | 상태 | 근거/후속 조치 |
| --- | --- | --- |
| 변경 범위·고정 버전 | [x] | 2815c3c, 고정 배포 태그, 사용자 미추적 파일 제외 |
| 빌드·권한·CSRF·DB 호환성 | [x] | Linux 배포/계약 CI 성공 및 위 XML 집계 |
| 운영 설치·migration | [x] | CodeDeploy 성공, V91, migration 실패0, JAR 지문 일치 |
| health·기본 HTTP | [x] | UP, 새 정적 코드, 비인증 복구 요청403 |
| 정책·롤백 경로 | [x] | 기존 정책/플래그 동일, 직전 JAR 보존, 복구 hook 유지 |
| 브라우저·정상 복구 적용 | [ ] | 현재 요청 정책상 미실행. 설치와 업무 흐름 완료를 구분 |

### 주요 실행 명령

```powershell
git -c http.sslBackend=schannel push origin codex/attachment-three-stage-linux-qa
gh workflow run deploy.yml --ref deploy-approved-2815c3c-20261005 -f attachment_qa=false
gh workflow run diagnose-codedeploy.yml --ref deploy-approved-2815c3c-20261005 -f deployment_id=d-VYKQQHT6L -f ssm_deployment_preflight=true -f ssm_app_log_check=false -f ssm_attachment_qa_check=false
gh run download 37307888030 --name attachment-contract-qa-2815c3cdd93bd5289a57b8a1d92ee2e233334e43 --dir build/qa-results/production-v91-37307888030
```

### 미실행·잔여 위험

- 실제 롤백/DB 복원 훈련, 브라우저 렌더링과 정상 관리자 복구 적용은 미실행이다.
- 기존 본문은 자동으로 정리되지 않는다. 관리자의 미리보기·단건 적용 또는 별도 승인된 일괄 복구가 필요하다.
- 본문 선택자가 확인되지 않는 사이트는 전체 페이지 저장 대신 구조화된 수집 실패로 남을 수 있다.
- 승인된 설치 범위의 추가 선행 조치는 없다. 후속 데이터 복구나 정책 변경은 별도 작업으로 관리한다.
