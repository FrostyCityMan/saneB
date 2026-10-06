# 수집 공고 전환 중심 화면 적용

## 범위와 기준

사용자가 전환 중심 HTML 목업을 승인하고 구현·적용을 요청했다. 기준 HEAD: `3ab07e7baed2c391e1e6a21a208ce981960316d0`.

- 기존 Thymeleaf·공통 사내비 CSS 사용. 새 라이브러리 없음.
- 기본 `/app/admin/collected-announcements/{sourceId}/attachments`는 간략한 자료·분류·검수 사유·초안 설정을 표시한다.
- 제목·본문 180자 발췌, 선택 열람 본문/첨부만 노출한다. 발췌는 생성 요약이 아니다.
- 문서 역할·실패 이력·복구 도구는 기본 화면에서 제거한다. 기존 기능과 회귀 테스트는 같은 경로의 `?tools=true` 유지보수 보기에 보존한다. 일반 화면에는 도구 진입 링크를 두지 않는다.
- 한 번 확인하면 기존 검수 확인 API → 최신 확인 ID·버전 검증 → 기존 비활성 초안 API를 호출한다. 원자적 단일 API가 아니다.
- 초안 실패 후 검수는 유지하고, 현재 저장된 확인이 있으면 초안만 재시도한다. 응답 유실 시 원래 payload와 확인 멱등키를 유지한다.
- 실패·불확실 자료에 대한 서버 필수 확인 항목과 수동 원문 확인은 필요한 경우에만 노출하며 생략하지 않는다.
- 공고 입력 링크는 생성된 announcementId로 기존 입력 화면의 상세 GET을 호출한다. 자동 저장·공개 없음.
- DB/API 계약·migration·정책·worker·수집 설정 변경 없음. 기존 사용자 미추적 파일은 포함하지 않는다.

## 수용 기준

- [x] 승인된 기존 디자인을 사용하고 전환 행동을 중심에 배치
- [x] 버전/역할/필수 근거/불확실 응답/중복 클릭 방어의 Node 테스트
- [x] SSR 권한·템플릿 15건 실패·생략 0, bootJar 성공
- [x] 동일 SHA Linux 전체 CI 및 임시 DB 계약 259건 성공
- [x] 운영 사전 진단·배포·health·자산 동일성 확인
- [ ] 브라우저 QA: 사용자 현재 요청에 명시되지 않아 정책상 미실행

## 검증 및 복구 계획

Node `attachment-conversion.test.mjs`와 기존 첨부 UI 회귀 테스트를 실행한다. SSR 테스트는 기본 화면과 별도 유지보수 보기 모두 실행한다. Linux CI와 배포 workflow에도 새 Node 테스트를 필수로 연결한다.

배포는 고정 SHA 태그에서 `attachment_qa=false`로 수행한다. 업무 데이터 변경·테스트 초안 생성·재수집·정책 활성화는 하지 않는다. 직전 운영 코드 `37063cc5cce357606eb9d758e2df03245c89ed48` 및 기존 backup/rollback hook을 복구 기준으로 사용한다.

운영 health와 공개 정적 자산 비교는 HTTP 읽기 전용 검증이며, 실제 관리자 클릭이나 반응형 검증을 대체하지 않는다. 최초 3차 판정이 미적용인 공고는 기존 서버 정책에 따라 계속 차단된다.

## 사전 검증 결과

- Node 신규 전환 14건, 기존 첨부 UI 75건 통과. 생성/유실 재시도/부분 저장/버전 변경/권한/중복 클릭/공고 입력 딥링크/판정 전 최신 첨부 조회를 포함한다.
- `gradlew.bat :test --tests '*AnnouncementAttachmentViewControllerSmokeTest' --tests '*AnnouncementConversionViewSmokeTest' :bootJar --no-daemon --console=plain --max-workers=1`: 성공, HTTP 15건.
- 사전 진단 GitHub run `37403793751`: 성공, V91·migration 실패 0, active/waiting 첨부 작업 0, COLLECT_ONLY 개정2 유지, 업무 쓰기 0. 9월 미종결 일정 1건은 연결 run 없음인 기존 상태이며 정상 종료로 표시하지 않는다.
- 설치 JAR과 직전 배포 원본 SHA-256 `d67c4830cf60efa999c7499c1af716a71c3bfbdf4a517ec8b2a61e1691056681` 일치.

## 최종 코드와 CI

- 코드 SHA: `a47959e8c49f8deb0af46444d01bc50e3dfbe1ec`, 태그 `deploy-approved-a47959e-20261006`.
- [Linux CI 37403984465](https://github.com/FrostyCityMan/saneB/actions/runs/37403984465): 전체 성공. 격리 DB·worker 계약 259건 통과, 실패/생략/미실행 0. 정책 부모 DB QA 연결·취소·정리 검증 성공.
- 이전 후보 `362563c`의 CI는 최신 첨부 조회 보완 후 취소했다. 취소 실행을 통과로 집계하지 않는다.
- 배포 직전 추가 진단 `37405375236`: 성공, V91, migration 실패 0, active/waiting 첨부 작업 0, 설치/배포 원본 지문 일치, 복구 JAR 존재, COLLECT_ONLY, 업무 쓰기 0.
- [운영 배포 37405457148](https://github.com/FrostyCityMan/saneB/actions/runs/37405457148): 고정 태그·`attachment_qa=false`로 시작. 최종 결과는 아래 사후 검증에 기록한다.

## 운영 사후 검증

- CodeDeploy `d-QAA52L77L`: Succeeded, revision `deployments/FrostyCityMan/saneB/a47959e8c49f8deb0af46444d01bc50e3dfbe1ec.zip`.
- 배포 workflow 전체 테스트/bootJar 성공. 서비스 시작 2026-10-06 11:47:52 KST.
- `/actuator/health`: UP. 운영 `saneb-announcement-conversion.js`, `saneb-announcement-input.js`, `saneb-announcement-attachment-review.css`는 커밋된 파일과 개행 정규화 비교 일치.
- [사후 진단 37406022445](https://github.com/FrostyCityMan/saneB/actions/runs/37406022445): 성공. 설치/배포 원본 JAR 지문 일치, V91·migration 실패 0, active/waiting 첨부 작업 0, COLLECT_ONLY 개정2 유지. worker/source-batch=true, GOV24 본문 상세 설정 UNSET 유지. 읽기 전용·업무 쓰기 0.
- 설치 JAR SHA-256: `ad5851f2f74be0c7af8a23b31e9ed9736999a7acf1a2aeda9fbdb3a1ec7933d5`.
- 이전 JAR SHA-256: `d67c4830cf60efa999c7499c1af716a71c3bfbdf4a517ec8b2a61e1691056681`. 배포 전 운영 JAR과 일치한다.
- Decision: **Conditionally ready**. 승인된 코드 적용과 비브라우저 검증은 완료. 현재 사용자 요청 정책에 따라 실브라우저 시각·모바일·실제 클릭 QA는 미실행이다.
- 실제 공고 검수 저장·초안 생성·재수집은 검증 과정에서 실행하지 않았다. 서버 판정 미적용 등 기존 차단 조건은 유지한다.
- 사용한 Node, 일회성 Gradle, CLI 감시 프로세스는 종료되었다. 기존 사용자 파일과 프로세스는 보존했다.

## 브라우저 QA 후속 수정 — 로컬, 미배포

이번 절은 위 배포 기록과 별개인 후속 변경이다. 작업 기준 HEAD는 `bb7691be459717bce9d4b5e51891290585247181`이다.

- Design Read: 기존 Thymeleaf·공통 CSS를 유지하며, 관리자가 전환 차단 사유를 이해하고 여러 첨부 문단을 연속 열람하도록 한다.
- 사용자/과업: 관리자·운영자의 자료 확인, 분류 검수, 비공개 초안 생성. 데스크톱 중심이며 모바일·키보드 접근도 유지한다.
- 위험: 화면 수정은 R1. 저장 조건을 잘못 완화하면 데이터 무결성에 영향을 주므로 서버 버전·권한·필수 확인·미확정 요청 재시도 조건은 변경하지 않는다.
- 범위: 안내·조회 UI만 수정. 본문 재수집, 판정 적용, 정책 게시, DB/API/migration 변경, 업무 데이터 쓰기, 커밋·푸시·배포는 하지 않는다.

### 변경 사항

- `saneb-announcement-conversion.js`: 첨부 20문단/쪽 연속 열람, 열람 쪽 이동, 문단별 4,000자 이어 읽기. 기존 GET API의 한도를 그대로 사용하고 자동 전체 다운로드/반복 조회는 하지 않는다.
- 긴 문단 조회 실패 시 읽은 내용 보존 및 동일 위치 재시도. 파일 변경·새로고침 뒤 늦게 도착한 응답은 폐기한다. 텍스트 위치는 서버 코드 포인트 오프셋을 사용한다.
- 판정 미적용·처리 대기·설정 문제·근거 변경을 원인/다음 행동으로 안내한다. 전환 불가 시 빈 폼 대신 읽기 전용 분류를 표시하고 미리보기와 적용 분류를 구분한다. 작성 중인 폼은 보존한다.
- `announcement-conversion-review.html`: 차단 사유 접근성 연결과 읽기 전용 분류 영역 추가.
- `saneb-announcement-attachment-review.css`: 문단 구분·쪽 이동 컨트롤 스타일. 기존 디자인 토큰과 포커스·반응형 규칙을 재사용한다.
- `attachment-conversion.test.mjs`, `AnnouncementConversionViewSmokeTest.java`: 페이지 경계, 입력 오류, 부분 조회 실패, 응답 경합, 잠금 유지 및 SSR 계약 검증 추가.

### 검증 명령과 완료 기준

```powershell
node --test scripts/qa/attachment-conversion.test.mjs scripts/qa/attachment-simple-workspace.test.mjs scripts/qa/attachment-review-ui.test.mjs scripts/qa/attachment-operations-ui.test.mjs scripts/qa/attachment-segments-ui.test.mjs scripts/qa/attachment-policy-ui.test.mjs scripts/qa/collected-attachment-overview.test.mjs scripts/qa/collected-navigation.test.mjs
.\gradlew.bat :test --tests '*AnnouncementAttachmentViewControllerSmokeTest' --tests '*AnnouncementConversionViewSmokeTest' :bootJar --no-daemon --console=plain --max-workers=1
git diff --check
```

- [x] Node 141건 통과, 실패/생략 0. 전환 화면 23건을 포함한다.
- [x] HTTP/SSR 15건 통과, 실패/생략 0. bootJar 성공.
- [x] API·DB·서버 전환 조건을 변경하지 않음.
- [ ] 수정 후 브라우저 QA: 현재 수정 요청에는 명시 지시가 없어 사용자 정책에 따라 미실행. 실제 반응형·키보드·스크린리더 확인은 남아 있다.
- [ ] 운영 적용: 미실행. 앞 절의 배포 성공을 이번 수정의 배포 근거로 사용하지 않는다.

한계: 20문단 연속 열람과 쪽 이동이며, 문서 전체 검색은 제공하지 않는다. 파일 원본 쪽수와 열람 쪽수는 다르다. `SRC-017842`에서 확인된 본문 미확보·첨부 판정 미적용 상태를 이 UI 변경이 해결하거나 우회하지 않는다.

## 후속 수정 운영 배포 결과 — 2026-10-06

사용자의 별도 운영 반영 요청에 따라 위 로컬 수정 6개 파일만 커밋·푸시·배포했다. 미추적 파일은 포함하지 않았다.

- 코드 SHA: `381c2c9f58b9124f80c208c0ab607732e27cf2d9`, 고정 태그 `deploy-approved-381c2c9-20261006`.
- [사전 읽기 전용 점검 37412677869](https://github.com/FrostyCityMan/saneB/actions/runs/37412677869): 성공. V91·migration 실패 0, 첨부 active/waiting 0, COLLECT_ONLY 개정2. 설치 JAR이 이전 배포 원본과 일치하며 복구 JAR 존재. 기존 미종결 일정 1건은 별도 미해결 기록으로 유지한다.
- [Linux CI 37412677980](https://github.com/FrostyCityMan/saneB/actions/runs/37412677980): 전체 성공. 독립 DB·worker 계약 259건 통과, 실패/생략/미실행 0. 연결·취소·정리 검증 통과.
- [배포 37413943953](https://github.com/FrostyCityMan/saneB/actions/runs/37413943953): 고정 태그, `attachment_qa=false`, 전체 테스트·빌드·배포 성공.
- CodeDeploy `d-3R44LH97L`: Succeeded, revision `deployments/FrostyCityMan/saneB/381c2c9f58b9124f80c208c0ab607732e27cf2d9.zip`.
- 서비스 재시작: 2026-10-06 13:35:39 KST. `/actuator/health`: UP.
- 공개 JS `saneb-announcement-conversion.js` 및 CSS `saneb-announcement-attachment-review.css`: HTTP 200, 로컬 커밋 파일과 개행 정규화 비교 일치.
- [사후 읽기 전용 점검 37414560555](https://github.com/FrostyCityMan/saneB/actions/runs/37414560555): 성공. 현재 배포 그룹 일치 및 lifecycle 검증, V91·migration 실패 0, 첨부 active/waiting 0, COLLECT_ONLY 개정2 유지, 진단 쓰기 0·트랜잭션 rollback.
- 설치/배포 원본 JAR SHA-256: `33d930a44423158674cb9c49d87b0c55b6023ec26282a87cc3765a494dc09c3f`.
- 복구 JAR SHA-256: `ad5851f2f74be0c7af8a23b31e9ed9736999a7acf1a2aeda9fbdb3a1ec7933d5`. 배포 직전 설치 파일과 일치한다.
- worker/source-batch=true, GOV24 본문 상세 설정 UNSET 유지. 정책 게시·재수집·판정 적용·검수 저장·초안 생성은 실행하지 않았다.

Decision: **Conditionally ready**. 승인된 UI 배포 및 비브라우저 사후 검증 완료. 수정 후 브라우저·모바일·실제 전환 QA는 현재 요청에 명시되지 않아 정책상 미실행이며, `SRC-017842`의 본문 미확보·판정 미적용 조건은 해결하지 않았다. 배포 전 필수 blocker는 없으며, 실제 전환 가능 상태와 UI 시각 검증은 별도 확인 항목이다.

실행 명령: 기존 Node 회귀 141건, `git diff --cached --check`, 범위 지정 commit/push, `gh workflow run deploy.yml --ref deploy-approved-381c2c9-20261006 -f attachment_qa=false`, 배포 전후 `diagnose-codedeploy.yml`의 `ssm_deployment_preflight=true`, health·정적 파일 HTTP GET 비교. 브라우저나 상주 로컬 서버는 시작하지 않았으며 일회성 Node/CLI 실행은 종료했다.
