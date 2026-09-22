# 첨부 3단계 처리 운영 기준선 및 QA 패키지 배포

## 2026-09-22 배포 후 Flyway checksum 전수 대조

- 승인 배포 `9a1bb45`/CodeDeploy `d-NCB2HF3YK`의 설치 JAR SHA256 `12985f8cd4d710f24da85d2f82c9556d719264aef5f43ac1a57239687bc9c2bc`와 운영 migration 이력을 비교했다. JAR에 포함된 Flyway10.20.1의 UTF-8/BOM/개행 처리와 동일한 계산을 사용했고 실제 Flyway 계산기로 로컬80파일·인코딩5사례·비교 판정8사례를 먼저 검증했다.
- 최신 버전 V83과 파일 수는 다르다. V2/V3/V5가 없는 파일80개와 이력80건의 version/script/type/success/checksum이 일치했다. 누락·추가·불일치·잘못된 항목0이며 전후 설치 JAR hash도 동일하다.
- 명령: 로컬 `verify-migration-checksum.py` 및 읽기 전용 `Invoke-SanebAwsReadOnly.ps1 -Action MigrationIntegrity`. SSM `638ec0bb-b863-4bd0-a2b7-a87eb589f3c6` Success, READ ONLY·statement8초/lock2초·ROLLBACK·쓰기0이다. migrate/repair/설정 변경/재시작을 실행하지 않았다. 소유 임시 CA·전송 파일과 실행 프로세스는 정리했다.
- 이는 migration 파일/이력 무결성 근거다. 전체 업무 데이터·전체 schema drift·첨부 상시 worker·정책 활성화·운영 브라우저 완료로 확대하지 않는다. 후속 Linux35693601984/272dafd에서 V71 대표 업무 데이터10테이블 보존·CHECK INSERT/UPDATE 거부·fresh validate가 통과해 ATT-053은 완료됐다. 시험만 보완했으며 운영 코드/기존 migration은 동일하고 재배포하지 않았다. 전체 Gate는 남는다.

## 2026-09-22 14:46 KST 승인된 코드 재배포 완료

**Decision: Not ready — 전체 첨부 서비스 활성화 기준. 승인된 코드 설치·재시작·읽기 전용 검증은 완료했다.** 아래 과거 재배포 승인 대기는 이번 코드 범위에 한해 해소됐다. 정책 게시·ENFORCE·기존 데이터 적용 및 옥천 서버 QA까지 승인된 것은 아니다.

- 사용자가 `9a1bb4569bcc3c13bf3bc30b51021b9149c67054`/추출기1.0.3 서울 코드 재배포를 승인했고 AWS 갱신 후 default/root·서울·저장소 대상 계정 일치를 확인했다. 고정 ref `codex/redeploy-9a1bb45-20260922`를 해당 SHA로 생성해 실행했으며 작업 브랜치를 되돌리거나 사용자 output을 변경하지 않았다.
- [배포 Actions35691586461](https://github.com/FrostyCityMan/saneB/actions/runs/35691586461)은 동일 SHA로 success, CodeDeploy `d-NCB2HF3YK`는 Succeeded다. `attachment_qa=false`이며 실제 hook에서 `ATTACHMENT_SERVER_QA=NOT_REQUESTED`, QA 보고서0건과 불변 QA 패키지 설치 성공을 확인했다. 옥천/공식 첨부 파일 QA를 실행하지 않았다.
- 설치 JAR SHA256 `12985f8cd4d710f24da85d2f82c9556d719264aef5f43ac1a57239687bc9c2bc`가 해당 CodeDeploy bundle의 app.jar와 일치한다. 업무 코드 지문 `d49cb22d272b66bd5d10078d33f6f698ddb89bef53b48faa72a60cafa6096780`은 성공한 Linux35681482535의 지문과 같다. 설치 catalog는 schema2/`2026-09-22-taebaek-seoul-revalidated-v2`, 참조30/기대값1이다.
- 추출기1.0.3 JAR hash `a6e09b4a405234ef4efa6f7aaee848edd610c08d58236fa3e4ec299d57a604a3`, library set hash `7ab209693fa6f8990c6643074f28d29150bd18a9bc1e58fe2b668466046117b2`다. 공용 설치/불변 QA 묶음이 일치하고, 실행 프로세스의 QA 및 worker 추출기 경로가 현재 웹 JAR별 release와 모두 일치한다.
- service active·서버 내부 health UP·JAR/실제 DB V83·migration 실패0이다. 운영787c594 대비 migration 파일 diff0이며 새 migration은 없다. 첨부 정책/ACTIVE/set/file/extraction/job/active job/batch 모두0, worker/정책QA/ProviderQA는 UNSET→기본false를 유지했다. 기존 목록/BODY/분류V2/source batch true, 활성 지자체223/목록parser41/원문2945다. 기업마당·정부24 key는 값 노출 없이 부재만 확인했다.
- 이전 JAR backup hash는 배포 전 `d8696e85c2d7c5415b8a39ea9ccb33f596f264feb5a7add966a62610b3751d74`와 일치하고 이전 extractor release도 존재한다. 실제 장애 유발/원복 실행을 검증한 것은 아니다.

실행 명령/근거: `gh workflow run deploy.yml --ref codex/redeploy-9a1bb45-20260922 -f attachment_qa=false`, 동일 Actions의 테스트·빌드·CodeDeploy 완료, 읽기 전용 Runtime/Database/DeploymentEvidence probe. 배포 전 Runtime `4e20e8db-ef81-4469-8490-6b2a7bd6b136`/DB `eeb36bac-c39a-4641-8b43-2c724001cd38`, 배포 후 Runtime `8f719daf-87ff-43c3-a919-01a81f385a06`/DB `60a71f6a-8691-4260-8198-b6b319e57828`/bundle 대조 `164de9bb-6521-4783-bd98-073c56004637` 모두 Success다. DB 확인은 READ ONLY·8초 statement/2초 lock·ROLLBACK·쓰기0이며 임시 CA/전송 파일과 감시 프로세스를 정리했다.

미실행/남은 위험: 전체 Provider·정상/형식 기대값 QA, 정책/worker 활성화, 기존 데이터 적용, 운영 업무 브라우저 E2E, 실제 rollback은 이번 코드 배포와 별개다. 브라우저는 이번 승인 범위에서 요청하지 않아 실행하지 않았다. 서버 내부 health 결과를 외부/인증 업무 E2E로 확대하지 않는다. 기존 본문 정기 수집에는 새 코드가 적용되지만 모든 공고의 본문/첨부 수집 완료나 첨부 상시 수집 시작을 의미하지 않는다.

## 2026-09-22 현재 인증·운영 읽기 전용 재확인

- 로그인 CLI는 시간 초과로 종료됐지만, 후속 STS와 배포 조회는 성공했다. 현재 default/root·서울 리전·저장소 대상 계정 일치, 대상 Ubuntu1대·SSM Online을 확인했다. 로그인 명령의 성공으로 소급 변경하지 않으며 **현재 인증 사용 가능**으로 정정한다. 인증값은 출력하지 않았고 TLS 검증을 유지했다.
- Runtime SSM `521e3255-17e7-41ef-a9c6-5c339bbdb57b`, Database SSM `c7d7d18b-dcf1-4b4e-b1fb-e20aa0c885f7` 모두 Success다. 운영은 `787c594e6ce33ad6b2ffbff994929d1faeb24dac`/CodeDeploy `d-MWENRAGTK` Succeeded, JAR `d8696e85c2d7c5415b8a39ea9ccb33f596f264feb5a7add966a62610b3751d74` 그대로다. service active·health UP·실제 DB V83/실패0, 추출기1.0.1·공용/QA library set 일치를 확인했다.
- 첨부 정책/ACTIVE/set/file/extraction/job/active job/batch는 모두0이며 첨부 worker/정책QA/ProviderQA는 UNSET→기본false다. 기존 목록·BODY·분류V2·source batch는true, 활성 지자체223/목록parser41/저장 원문2945다. 기업마당·정부24 key2종은 실행 환경에 없다. 건수는 재처리 적격 또는 승인 수량이 아니다.
- DB는 READ ONLY·8초 statement/2초 lock 제한·ROLLBACK·쓰기0이다. 소유 CA/전송 임시 파일을 정리했으며 서비스 재시작/운영 설정 변경/배포/브라우저 조작은 없었다.
- 전체 판정은 **Not ready**다. 취소된 코드 재배포 재개 승인은 별도로 미확인이다. GitHub에서 본문·상세 timeout, 로컬에서403인 옥천3공고를 서울에서 임시 검증하는 추가 범위도 승인 대기다. 기존 태백1공고 승인으로 확대하지 않으며 정책 게시·ENFORCE·기존 데이터는 실행하지 않는다.

## 2026-09-22 코드 재배포 전 판정

**Decision: Not ready.** 취소된 재배포의 정확한 SHA 재개 확인과 설치 후 동일 산출물/설정/DB/health 증거가 필요하다. 전체 첨부 서비스 활성화는 아래 코드 검증 성공과 별개로 미완료다.

| Gate | 상태 | 근거 / 필요한 조치 |
|---|---|---|
| production 후보 소스 | [x] |1a19e63, 운영787c594 대비 HWP 추출기1.0.3·BBS redirect/DNS 보완·충주 본문/첨부·catalog·추출기 release fallback이 주요 변경. migration diff0.71a05ae 이후 main 코드/리소스 변경0이며 후속은 FIXED 시험/probe/문서 변경 |
| 코드/DB 계약 | [x] | [Linux35629360526](https://github.com/FrostyCityMan/saneB/actions/runs/35629360526) success, SHA1a19e63. root2488통과/266조건부 생략, 독립PG221/221, job192·migration17·worker12·Flyway3·정책 부모2 실패/생략0·정리 성공. 특수 suite와 root의 중복을 합산하지 않음 |
| 태백 실제 파일 | [x] | 서울 임시 패키지에서 BODY/HWPX2 관측 및 production 고정 비교 각1/1 성공. 합계8요청/2,658,726bytes·검수 유지·운영 쓰기0 |
| 설치 대상/재개 | [!] | 기존 취소 이후의 코드 재배포 재개 승인은 미확인. 서울 임시 QA 승인으로 운영 설치를 대체하지 않음 |
| 플래그/운영 smoke | [ ] | 이번 임시 QA에서 운영 JAR d8696e85… 불변/health UP 확인. 실제 재배포 직전 Runtime/DB/플래그를 새로 조회하고, 배포 후 동일 SHA/JAR/QA·추출기 release/V83/health 대조 필요 |
| 전체 활성화 | [!] | 전체 Provider/정상 표본/형식 QA·정책 게시·ENFORCE·기존 데이터 승인/적용·업무 브라우저 E2E 미완료. 외부키2종 없음은09-21 조회 근거이며 배포 시 재확인 |

실행 명령은 로컬 Gradle 표적/전체 및71a05ae·1a19e63 CI의 실제 임시DB suite이고 [현재 실행 기록](../backend/announcement-attachment-end-to-end-progress-2026-09-09.md)과 [서울 임시 QA](../backend/announcement-seoul-temporary-bbs-qa-2026-09-21.md)에 횟수/생략/코드 지문을 분리했다. 두 Linux 실행의 업무 코드 지문d49cb22d…는 같지만 로컬 임시 패키지6ff295d8…와는 다르다. 설치 후 검증은 CI/실제 배포 산출물을 기준으로 한다. 후속 현재 인증·Runtime/DB 조회 결과는 위 재확인 절에 기록했으며 코드 재배포 완료 근거는 아니다.

배포·정책/worker 활성화·운영 데이터 변경·브라우저 업무 검증은 이 판정에서 실행하지 않았다. 재개 시 코드/추출기만 설치해도 기존 활성 목록·본문 정기 수집에는 새로운 BBS 검증/충주 모델이 적용될 수 있다. 첨부 worker/정책 QA/Provider QA는 비활성 유지, 기존 데이터 일괄 적용은 제외해야 한다. 원복은 기존 JAR와 그 지문에 연결된 추출기 release를 함께 선택하는 경로를 사용하며, 실제 운영 원복을 시험했다는 뜻은 아니다.

## 읽기 전용 재확인 — 2026-09-21

후속 검증에서 [BBS 리다이렉트 식별자 결함](../backend/announcement-bbs-redirect-identity-2026-09-21.md)을 발견해 로컬 보완 중이다. 아래7a5ef22 코드 재배포 요청보다 수정본 검증이 선행하며 취소된 배포를 임의 재개하지 않는다. 기존 설치/운영 데이터는 이 발견 이후에도 이번 작업에서 변경하지 않았다.

- 현재 Windows가 신뢰하는 유효한 공개 CA를 명령 범위에 적용해 TLS 검증을 유지했다. 저장된 GitHub 작업 계정도 명령 범위에서만 사용했고 전역 활성 계정은 변경하지 않았다. 서울/default root와 저장소 배포 계정 일치·대상 Ubuntu1대·SSM Online을 확인했다. 인증값은 출력/파일 보관하지 않았다.
- Runtime SSM `954cd94c-a8cc-401c-b531-86c364d24b58`, Database SSM `42b133fe-7b85-47e8-8e93-abae5663891f` 모두 Success다. 운영 revision은 여전히 `787c594e6ce33ad6b2ffbff994929d1faeb24dac`/CodeDeploy `d-MWENRAGTK`, JAR `d8696e85…`·추출기1.0.1·catalog24/기대값1이다. service active·health UP·실제 DB V83/실패0·QA/공용 추출기 library set 일치를 재확인했다.
- 첨부 정책/ACTIVE/set/file/extraction/job/active job/batch 모두0, 첨부 worker/정책QA/ProviderQA는 UNSET→기본false다. 기존 지자체 schedule/BODY/분류V2/source batch true·활성 지자체223/목록parser41/저장원문2945이며 기업마당·정부24 키는 실행 환경에 없다. 원문 건수는 재처리 승인 수량이 아니다.
- DB 조회는 READ ONLY·statement8초/lock2초·ROLLBACK·쓰기0이다. helper가 생성한 공개 CA/전송용 임시 파일은 정리했다. 로컬 Docker Linux pipe는 연결 불가이며 운영 서버에서 임의 테스트·배포·정책/환경값 변경·브라우저 조작을 하지 않았다.
- 새 코드 `7a5ef22`는 QA 브랜치 푸시/원격 일치를 확인했다. [Linux35602665310](https://github.com/FrostyCityMan/saneB/actions/runs/35602665310)은22:13 KST success로 종료했고 실제 XML의 전체 계약·독립 PG·정책 부모 연결/취소/정리와 태백 고정 비교1건/전체HWPX2파일 통과를 확인했다. 이 성공은 배포·상시 수집 활성화가 아니며 전체 Provider/정상 후보·정책 QA는 미완료다. 취소된 배포 재개 승인을 같은 SHA의 코드/추출기1.0.3 설치 범위로 요청했으며 worker 활성화·게시·ENFORCE·기존 데이터는 제외한다. 기존 목록/본문 정기 수집이 활성 상태이므로 코드 설치 후 새 본문 모델이 이후 정기 수집에 적용될 수 있다는 영향은 남는다.

## 최신 읽기 전용 재확인 — 2026-09-15 16시대

전체 출시 판정은 **Not ready**다. 재배포 승인 대기 중 Runtime SSM `d291efb8-5107-485f-9cae-6f66def58921`와 Database SSM `7547e95d-562f-4bef-baa8-15659ed0c8c6`를 실행했고 모두 Success다. 서울/default root·저장소 대상 계정 일치·운영 인스턴스1/SSM Online을 확인했다. 상태를 읽었을 뿐 배포·정책·운영 환경값은 변경하지 않았다.

- 운영 revision은 `787c594`/CodeDeploy `d-MWENRAGTK` Succeeded, 설치 JAR `d8696e85…`·추출기1.0.1이다. systemd active/health UP·JAR/실제 DB V83·migration 실패0이며 글로벌/불변 QA 추출기 library set이 일치한다.
- 첨부 정책/ACTIVE/set/file/extraction/job/active job/batch는 전부0이다. 첨부 worker/정책 QA/Provider QA는 UNSET→기본false, 기존 지자체 schedule/BODY/분류V2/source batch true다.
- 지자체223/목록parser41/저장원문2945이며 재처리 승인 건수가 아니다. 기업마당·정부24 API key는 실행 환경에 없다. 자격증명 값은 출력하지 않았다.
- DB는 READ ONLY·statement8초/lock2초 상한·ROLLBACK·쓰기0이다. 소유 임시 CA/전송 파일을 정리했다. 브라우저는 이번 재확인에서 조작하지 않았다.
- 최신cc79d59/1.0.3의 HWP/HWPX CI 성공은 위 운영 설치와 다르다. 취소된 배포의 재개 확인과 승인된 정책/기존 데이터·관리자 업무 E2E가 남는다.

추가로 JAR만 복원하던 기존 fallback에서 기본 worker 추출기도 같은 불변 release를 선택하도록 [로컬 배포 경로 보완](attachment-extractor-release-binding-2026-09-15.md)을 구현 중이다. 아직 운영 설치·실제 rollback 성공의 증거가 아니다.

## 최신 확인 — 2026-09-15 13:12~13:14 KST / 고정 기대값 catalog 설치

전체 출시 판정은 **Not ready**다. 새 catalog 설치와 실제 기대값 비교·상시 수집 활성화는 서로 다른 단계다.

- 동일 SHA `787c594e6ce33ad6b2ffbff994929d1faeb24dac`의 [Linux34926361587](https://github.com/FrostyCityMan/saneB/actions/runs/34926361587) success 및 artifact XML을 확인했다. root2648=2385통과/263조건부 생략, extractor36·패키지20·job192·migration17·worker12·runtime1·정책 부모2·Flyway3은 실패/생략0이다. 공식 사이트 opt-in은 CI 통과에 포함하지 않는다.
- [배포34927650269](https://github.com/FrostyCityMan/saneB/actions/runs/34927650269)/CodeDeploy `d-MWENRAGTK` Succeeded. 설치 웹 JAR SHA256 `d8696e85c2d7c5415b8a39ea9ccb33f596f264feb5a7add966a62610b3751d74`, 업무 코드 hash `d6e98d08cd6f6f23c059bd15fd7a0f0a7513b13fbc07a7b7efbf235b3ecf9f3c`다.
- 설치 JAR 안의 catalog는 schema2/`2026-09-15-taebaek-reviewed-v2`, 참조24/기대값1이다. 추출기1.0.1 JAR은1개이며 hash `77e0b90b99ca55904fb2f088a42d79cb893a9df0e39ff29c3cb4e03fd1eb2910`, 글로벌/불변 QA library set hash `be2ba81c8e5a7fb0e4a5ea1a2849fc07fd41b4e81420eb949e3d6badbe737728`로 일치한다. 동일 버전 재빌드의 artifact 지문을 이전 설치 값과 혼용하지 않는다.
- systemd active·health UP·JAR/DB V83·migration 실패0. 첨부 정책/ACTIVE/set/file/extraction/job/active job/batch 모두0, worker/정책QA/ProviderQA는 UNSET→기본false다. 기존 지자체 schedule·BODY·분류V2/source batch true, 지자체223/목록parser41/원문2945, 외부 API key2종 없음은 유지됐다. DB 조회는 READ ONLY/ROLLBACK/쓰기0이다.
- 배포 중 고정 파일4건 진단은4/4통과·원본 정리4/4·운영 DB 쓰기0이다. PDF OCR_REQUIRED/0자·PARTIAL_TEXT/31498자, HWP COMPLETE_TEXT/565자, HWPX COMPLETE_TEXT/2413자다. 단일 선택 파일의 진단이며 전체 공고·정책 QA·운영 활성화 성공이 아니다.
- Runtime SSM `8bc17be0-a2aa-4b02-8475-b7f5237822ec`, DB `f2e6125e-d9a8-4de4-8df7-ab5344eeff99`, 배포 metadata `3058e200-bb95-4580-ae76-26e7dbb7d58b` 모두 Success다. 임시 CA 파일을 정리했고 자격증명 값은 출력하지 않았다. 관리자 탭은 로그인 화면으로 확인했으며 새 SHA 업무 브라우저 E2E는 미완료다.

태백 전체 파일의 사전 기대값 비교와 HWP 전체 worker 재확인은 [공식 worker 기록](../backend/announcement-official-worker-db-api-qa-2026-09-15.md)의 후속 결과를 따른다. 정책 게시·ENFORCE·기존 데이터는 실행하지 않았다. 아래 기록은 이전 배포 이력이다.

## 최신 확인 — 2026-09-15 11:55~11:58 KST / 추출기1.0.1

전체 출시 판정은 **Not ready**다. 설치 배포는 성공했지만 첨부 상시 worker·정책·기존 데이터 적용은 활성화하지 않았다. 아래10시대/03시대 기록은 각각 이전 운영 snapshot이다.

| 경계 | 실제 확인 결과 |
|---|---|
| 동일 SHA Linux | [34922098905](https://github.com/FrostyCityMan/saneB/actions/runs/34922098905) success, `2bde2161328c9f876eaaddaba374889c9dedf825`. root2636=2373통과/263조건부 생략, extractor36·패키지20·job192·migration17·worker12·runtime1·부모2·Flyway3은 실패/생략0. 공식 사이트 opt-in 시험은 미실행 |
| 배포 | [34922807546](https://github.com/FrostyCityMan/saneB/actions/runs/34922807546) success, CodeDeploy `d-Q1K7M8ETK` Succeeded, revision `2bde2161328c9f876eaaddaba374889c9dedf825` |
| 설치 웹 JAR | SHA256 `b31a27aa1267310e9e71401c66c2666c0fc9f82dbe95a6eacbd284244e575ece`; 업무 코드 catalog hash `4d7dab7848cb8001d7c18d84b1676e6a8c0d33b9f4422cb4e3682034169eba1f` |
| 설치 추출기 | `attachment-extractor-1.0.1.jar`1개, SHA256 `9cb30102fbfb5d1695da8365a56614a6cd4ec5bdc281a4fe4632836b0e9a59e8`. 글로벌 lib와 해당 웹 JAR의 불변 QA lib가 동일하며 구버전 추출기 JAR 잔존 없음 |
| 가동·DB | systemd active, localhost health UP, JAR/DB V83, migration 실패0. 이 증분의 기존 migration 변경0 |
| 정책·데이터 | 첨부 정책/ACTIVE/set/file/extraction/job/active job/batch 모두0. 첨부 worker·정책 QA·Provider QA 플래그 미설정/코드 기본false 유지 |
| 기존 수집 | 지자체 schedule·본문·분류V2 true 유지. 지자체223/목록 parser41/저장 원문2945. 전체 원문 수는 재처리 적격·승인 건수가 아님 |
| 외부 설정 | 기업마당·정부24 API key는 실행 환경에 없음. 자격증명 원문은 조회 출력하지 않음 |
| 고정 파일4건 | PDF OCR_REQUIRED/0자·PARTIAL_TEXT/31498자, HWP COMPLETE_TEXT/565자, HWPX COMPLETE_TEXT/2413자. 진단4/4·원본 정리4/4·운영 DB 쓰기0. 단일 선택 파일/DRAFT 규칙/고정 역할 진단이며 공고 전체·정책 QA·자동 역할 성공은 아님 |
| 브라우저 | 새 배포 뒤 기존 관리자 세션이 만료되어 인증 차단 안내→로그인 화면 이동을 확인. 사용자 재로그인 요청. 새 SHA의 인증 관리자 업무 E2E는 아직 미완료 |

배포 전 runtime SSM `709a979c-57a3-496a-88b1-bc273d344005`, 배포 후 runtime `7775c427-7b1c-4aab-9bcd-8f8a2c10db8e`, DB `44b378b2-a982-4f6d-a2d6-ef091111f888`, 공개 파일 metadata `5eecb1b0-d569-4e8c-9c48-b3b8250a1dde`는 Success다. DB는 READ ONLY/ROLLBACK/쓰기0이다. 서울 리전/default root의 계정·배포 대상 일치를 확인했고 기존 신뢰 루트를 프로세스 범위에서 사용한 임시 CA 파일을 정리했다. TLS/IAM/보안그룹/운영 정책은 변경하지 않았다.

공식 worker 양평3건 재관측은 새 설치 JAR 지문에 고정하여 별도 실행한다. 해당 결과는 [공식 worker 기록](../backend/announcement-official-worker-db-api-qa-2026-09-15.md)을 따른다. CodeDeploy의 성공을 전체 파일 분석 완료로 바꾸지 않는다. 자동 rollback 설정은 존재하지만 실제 원복은 실행하지 않았고, start/health 실패 시 로컬 fallback은 웹 JAR 중심이므로 전체 추출기 복원까지 검증했다고 주장하지 않는다.

## 최신 확인 — 2026-09-15 10시대

전체 출시 판정은 **Not ready**다. 아래03시대 V72 관측은 과거 기준선이며 현재 설치 버전이 아니다.

| 경계 | 현재 직접 확인 |
|---|---|
| AWS 인증 | 사용자 승인 root, 실제 계정=저장소 배포 대상, ap-northeast-2. ARN/인증값은 기록하지 않음 |
| 배포 | [Actions34916976535](https://github.com/FrostyCityMan/saneB/actions/runs/34916976535) success, `d-CTAGI7DTK` Succeeded |
| revision | `476c8f722e30464ff7c903b5519d86a4be1ad4c8` |
| 실제 JAR SHA256 | `16a1bb74a13e7d88c180fe6f3eb98e97c105c8362592f9c96de572890523f437` |
| schema / 가동 | JAR V83, 실제 DB V83/실패0, systemd active, localhost health UP |
| worker / QA 설치 | 새 worker class 있음. JAR 지문별 QA release/내장 추출기 설치, 실행 환경의 QA root 일치 |
| 활성화 | 첨부 worker·정책 QA·Provider QA 변수 미설정/코드 기본false. 운영 첨부 정책0, ACTIVE0 |
| 첨부 데이터 | set/file/extraction/job/active job/batch 모두0건. 일괄 재처리·ENFORCE 미실행 |
| 기존 원문 / 대상 | LOCAL_GOV_NOTICE2,945건, 활성 지자체223/목록 parser41종. 재처리 적격 건수 아님 |
| API key | 기업마당·정부24 실행 환경에서 모두 없음 |
| 공개 파일 QA | 단일 파일4건 예상 동작 통과/원본 정리4. PDF OCR/부분, HWP/HWPX 완전 텍스트. 공고 전체·worker 저장 성공 아님 |
| 운영 서버 독립 DB QA | SSM `d10887c4-488e-47e0-a6d7-e3413a9e3d23` Success. 합성 worker/DB 계약221건 통과·실패/생략/미실행/container 실패0, 정리 성공·운영 DB 미사용·설치 JAR 불변 |
| 공식 worker/DB/API | SSM `31cf27d7-cf0c-49f7-badd-10ffc1eeefcb` Success. 고정 양평3건 통과, BODY2/실제HWPX·PDF→worker→임시DB→API. 지원2파일은 PARTIAL_TEXT로 예외/검수 상태, JPG미지원·제목 제외 요청0. 운영 DB 미사용/정리 성공 |
| 브라우저 | 사용자 HTTP 진행 승인 후 관리자 로그인·대기열 조회 확인. 최종 검증 준비0건/전체2,945건·1/148페이지, 표시20건 모두3단계 미적용. 검수·DRAFT·역할별 E2E는 미완료 |

고정 읽기 전용 SSM 근거: 배포 전 runtime `b27e1306-0838-495d-8ca3-373814830ab4`, DB `a04fbb67-015a-408c-ae88-584129b2a10c`; 배포 후 runtime `cb6687b0-1361-4169-85b0-157bc2cb0a88`, DB `c8b4deb5-005e-4230-a345-81bd393fe8c9`, QA metadata `344622fa-f4ae-48d2-b262-6c0be53c9389`, ingress `2aac8108-3f76-4b4d-a81c-139de1926efa`는 Success다. DB 조회는 READ ONLY/statement8초·lock2초/ROLLBACK이며 원문·자격증명을 반환하지 않았다. 최초 runtime 진단1회는 Python3.10에서 지원하지 않는 로컬 진단용 hash 함수 사용으로 실패했고 이식 가능한 streaming hash로 수정 후 성공했다. 서비스 코드 오류로 계산하지 않는다.

V73~V83은 새 additive migration11개이며 배포 SHA와 이전 revision 사이 V1~V72 변경0을 확인했다. 동일 SHA Linux migration 순차 시험17건·원래 Flyway3건 통과 후 승인된 기존 환경 배포를 수행했다. 배포 후 스키마 downgrade/DB 복원/이전 JAR 실제 복구 시험을 수행한 것은 아니다. 자동 CodeDeploy rollback은 DEPLOYMENT_FAILURE에 설정되어 있으나 이번 성공 배포에서 실행되지 않았다.

최초 격리 DB 요청은 Windows 한글 포함 JSON 파일 전송 단계에서 실패했지만, ASCII escape 적용 후 실제 SSM 요청이 접수되고221건이 통과했다. 기존 미실행 기록을 현재 상태로 사용하지 않는다. 이 성공은 합성 계약 시험이며 공식 공고 전체 파일→worker 성공이나 정책 게시 승인 증거가 아니다.

설치 완료와 기능 활성화는 별개다. 공식 전체 파일/역할 기대값, 정책 QA·게시 승인, 전체 기존 데이터의 정확한 승인 범위·적용·복구 증거와 관리자 업무 E2E는 남아 있다. 비밀정보·운영 원문은 문서/로그에 추가하지 않았고 IAM/보안그룹/포트/정책 설정은 변경하지 않았다.

## 판정

2026-09-15 03:30~03:50 KST 읽기 전용 관측. **Not ready**다. 서비스 가동과 새 첨부 파이프라인 가동을 구분한다. 제목 1차 → 정제 본문 2차 → 실제 PDF/HWP/HWPX 텍스트 3차 → 관리자 최종 검증 순서, 실패/UNKNOWN 분리, 자동 ACTIVE 금지는 유지한다.

| 경계 | 확인 결과 | 의미 |
|---|---|---|
| Git QA 기준 | `25e03d2bc65f61a5b063d370744b58e999c38b73` | 아래 운영 JAR와 다른 코드다 |
| 서울 CodeDeploy | `saneb / saneb-dev`, 마지막 성공 `d-VHMRNZRPK` | 2026-09-09 배포 기록 |
| 배포 revision | `69b7278a92b2de4e71c55ac35db0069f1d4fbecc` | S3 revision 경로의 SHA이며 실행 JAR 지문과 구분 |
| 실제 설치 JAR SHA256 | `0c916a3856e78481bc97fab3267a2ad099e7a7310d5f29ef177a5d8d45989702` | SSM으로 현재 파일을 읽어 계산 |
| systemd / localhost health | active / UP | 외부 공개 health·브라우저 E2E 증거가 아님 |
| JAR migration / DB 적용 migration | V72 / V72 | JAR 목록과 실제 `flyway_schema_history`를 각각 조회 |
| 새 상시 첨부 worker class | 설치 JAR에 없음 | QA 브랜치 구현이 운영에 반영되지 않음 |
| 추출기 / 정책 DB QA 패키지 | 추출기 존재 / QA 패키지 없음 | 추출기 설치만으로 정책 QA를 실행할 수 없음 |
| 운영 API key | 실행 중 서비스의 기업마당·정부24 key 모두 없음 | 로컬 코드 구현 여부와 별개의 외부 API 운영 blocker |
| 운영 플래그 | 지자체 schedule·본문·분류 V2 true | 새 첨부 worker·정책 QA·Provider QA 플래그는 없음 |
| 실제 활성 지자체 대상 | 223개 / 목록 파서 41종 | 첨부 엔진6종·등록 프로필12개와 다른 분모 |
| 저장된 전체 원문 | LOCAL_GOV_NOTICE 2,945건 | 노출·적격·배치 적용 대상 수를 뜻하지 않음 |

실제 정책/job 테이블은 존재한다. 새 worker class가 없다는 이유로 해당 테이블도 없다고 추정하지 않는다. 동일 서비스 UID의 전체 thread는 한 시점에45개였으며, 새 QA 실행의 `nproc=128` 충족·성공을 보장하지 않는다.

후속으로 같은 완료 SSM snapshot의 활성 대상과 현재 코드의 지자체 SourceBinding11개를 대조했다. 강북010·부산027·남구034·중구045·달성052·서구074·태백121·횡성125·영월126·화천130·함안233(LGS- 접두어/6자리 코드)은 각각1행이며 목록 parser도 모두 일치한다. 남은212개 활성 대상에는 현재 첨부 모델 결합이 없다. 11개 결합의 일치는 실제 첨부 텍스트 처리·운영 성공을 증명하지 않는다. 새 SQL을 실행한 것이 아니라03:30~03:50 snapshot을 현재 코드와 대조한 결과다.

## 조회 방법과 변경 경계

2026-09-15 04시대 후속 조회는 AWS 세션 만료로 실패했다. 운영 정상 여부·신규 원주 SourceBinding 일치를 현재 시각 기준으로 재확인하지 못했다. 이 문서의 운영 사실은 위03:30~03:50 snapshot이며 최신 코드/운영 동기화를 의미하지 않는다. 이번 후속에서 운영 명령·DB 쓰기·배포는 실행되지 않았다.

- GitHub repository variables → CodeDeploy deployment group/tag → 해당 EC2/SSM Online 순서로 대상을 확정했다.
- SSM `4da5c4eb-d9d6-44f8-b444-8ff5b860d2bb`: JAR/서비스/localhost health/허용된 플래그 존재만 조회, Success.
- SSM `2ab2fbef-01b1-402e-9493-eb0fc762e587`: 실행 중 서비스 환경에서 접속값을 원격 메모리로만 사용했다. `default_transaction_read_only=on`, statement timeout8초/lock timeout2초 아래 고정 SELECT로 migration/대상/건수를 조회, Success.
- 인증값·프로세스 환경 원문·DB 접속값은 응답에 내보내지 않았다. 운영 데이터 쓰기·migration·정책·서비스 restart·배포는0이다.
- Windows의 현재 신뢰 저장소에 존재하며 유효한 기존 인증서를 프로세스 범위의 `AWS_CA_BUNDLE`로 지정해 TLS 검증을 유지했다. 전역 설정·신뢰 저장소를 수정하거나 TLS 검증을 끄지 않았다. 로그인/인증값은 기록하지 않는다.
- 외부 공개 health, 현재 운영 역할별 로그인/브라우저, 새 SHA 운영 배포는 이번 관측에서 검증하지 않았다.

## QA 실행 패키지 설치 설계와 구현

기존 배포 workflow가 별도 `installAttachmentContractQa` 산출물을 포함하지 않았고, 실제 운영에도 해당 패키지가 없었다. 다음 증분은 **배포 경로** 보완이며, 실제 설치/정책 QA 실행 성공이 아니다.

1. 배포 build에 `installAttachmentContractQa`·패키징 시험을 포함한다. QA JAR가 `build/libs`에 추가되므로 웹 JAR 선택에서 plain/QA JAR를 제외하고 정확히1개를 요구한다.
2. CodeDeploy bundle에 웹 JAR와 별도 `attachment-contract-qa` 전체 distribution을 포함한다. QA/JUnit/임시 PostgreSQL 라이브러리를 웹 JAR classpath에 추가하지 않는다.
3. AfterInstall에서 현재 설치 JAR와 같은 bundle JAR의 SHA256을 대조한다. `/opt/saneb/attachment-contract-qa-releases/<JAR SHA256>`의 새 임시 디렉터리에 완전 복사 후 rename한다. 심볼릭 링크/특수 파일·필수 파일 누락·기존 release 내용 불일치는 실패다.
4. 기존 release를 덮어쓰거나 의존성을 overlay하지 않는다. 이전 release와 관련 없는 경로는 삭제하지 않는다. 실패 때 이번 호출이 만든 `.install-*` 디렉터리만 경계를 검증하고 정리한다.
5. launcher는 실제 시작하는 JAR 지문으로 QA 경로를 선택한다. 이전 JAR를 복구하면 해당 이전 release가 선택된다. `app.env`의 명시적인 QA 경로는 보존하므로, 사용자 지정 경로가 있다면 정책 QA 코드 지문 검증과 별도 운영 확인이 필요하다.
6. 설치/launch가 정책을 게시하거나 worker/ENFORCE를 켜거나 원장을 재실행하지 않는다. 기존 데이터 처리는 정확한 적격 범위·전체 요청/byte 상한·효과·원복 승인을 별도로 요구한다.

파일: `scripts/attachment-contract-release.sh`, `scripts/install-attachment-contract-qa.sh`, `scripts/start.sh`, `.github/workflows/deploy.yml`, `appspec.yml`.

## 검증과 잔여

- 로컬 Node/Git Bash 임시 파일 시험:12건 중10통과/2 Linux 전용 생략, 실패0. 완전 설치·동일 패키지 재실행·다른 JAR·누락·동일 release 변조/남은 dependency 거부·두 JAR의 복구 경로·복사 중 실패의 staging만 정리를 실제 파일시스템에서 검증했다. 사용한 Node/Bash와 임시 디렉터리는 종료·정리했다.
- Gradle workflow 계약8건과 별도 패키징20건:46초 성공. bootJar/install은 UP-TO-DATE이며 새 operating build/실행으로 세지 않는다.
- 최종 `test :attachment-extractor:test bootJar attachmentContractQaTest installAttachmentContractQa --no-daemon --max-workers=1`은3분8초 성공이다. root2417건=2169통과/248조건부 생략/실패0이다. extractor·패키징·JAR/설치는 UP-TO-DATE이므로 이 호출에서 다시 실행한 것으로 합산하지 않는다. Windows-ROOT 신뢰 설정을 명령 범위로 사용했고 TLS 검증을 끄지 않았다. 새 Node 구문 검사와 `git diff --check`도 통과했다.
- Linux CI에 새 설치 시험을 연결했다. Linux 링크/권한·실제 CodeDeploy 설치·실제 systemd 복구는 별도 검증 대상이다.
- 후속 [Linux 실행21](https://github.com/FrostyCityMan/saneB/actions/runs/34883813483), `74cc12553c3ca4ac86eb2fd0c063fbac48abc2f5`는 전체 성공이다. 설치 시험12/12·생략0, 실제 DB/독립220/부모2도 통과했다. Linux 링크/권한 검증은 완료했지만 운영 CodeDeploy 설치·systemd 복구와 동일 SHA 운영 브라우저는 미실행이다. 상세 건수는 [Linux 검증 기록](../backend/announcement-attachment-linux-contract-qa-2026-09-11.md)을 따른다.
- 새 hook 설치 실패 시 CodeDeploy 전체 복구, 기존 추출기 경로의 복구, additive migration 이후 이전 JAR 호환성까지 전부 검증했다고 주장하지 않는다. 이 증분은 QA 패키지와 JAR의 경로 결합을 다룬다. 기존 hook의 JAR 복구만으로 DB/추출기를 완전 원복할 수는 없다.
- 불변 release는 자동 삭제하지 않는다. 보관 용량·불필요한 과거 release 정리는 복구 대상이 확정된 후 승인된 별도 작업으로 수행한다.

다음 순서는 새 설치 시험의 Linux 결과 확인 → 운영223개에 대한 첨부 프로필/본문 영역의 실제 적용성·남은 대상 확정 → 공식 파일 텍스트/역할 기대값과 전체 Provider QA → 동일 SHA 운영 설치·스모크 → 정확한 범위 승인 후 활성화/기존 데이터 처리·역할별 운영 브라우저 E2E다. 전체 Gate0~8과 ATT001~062를 축소하지 않는다.
