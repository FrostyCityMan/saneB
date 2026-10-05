# 첨부 검수 화면 단순화 배포 기록

## 범위

사용자가 HTML 목업 확인 후 실제 구현과 운영 반영을 승인했다. 사내비 기존 스타일과 Thymeleaf 구조를 유지한다.

- 현재 공고·상태·다음 행동을 상단에 표시한다.
- 자료 확인과 관리자 검수를 두 영역으로 배치한다. 좁은 화면에서는 한 열이다.
- 현재 적용 판정의 파일을 우선 자동 조회한다. 없으면 미리보기, 연결 근거가 없으면 최신 수집 이력을 조회한다. 선택한 근거의 용도를 명시한다.
- 파일 ID·해시·처리 이력·중간 판정을 상세 영역으로 접는다. 실패와 역할 미확정 경고는 숨기지 않는다.
- 검수 불가 시 빈 입력 폼을 숨기되 사유·미저장 입력·불확실 요청 재시도는 보존한다. 현재 확인이 저장된 경우 편집 폼을 접고 별도 초안 폼을 표시한다.
- 재수집·원복·구간 근거는 기존 기능으로 유지한다. 검수 단계 UI만 변경하고 별도 관리 페이지 신설은 하지 않는다.
- 권한·버전·중복·필수 사유·원문 직접 확인·멱등성 Gate, DB/API, 정책·worker 설정, 자동 활성화 금지는 변경하지 않는다.

## Gate

- [x] 기준선 fd86c49, 기존 사용자 미추적 파일 보존.
- [x] 관련 Node 회귀·목록 복귀·신규 화면 상태 총 85건 통과.
- [x] JavaScript 문법 검사.
- [x] 로컬 화면 HTTP 테스트 9건, `:test --tests '*AnnouncementAttachmentViewControllerSmokeTest' :bootJar` 성공.
- [x] 수정 SHA 37063cc5cce357606eb9d758e2df03245c89ed48 Linux CI [37322105316](https://github.com/FrostyCityMan/saneB/actions/runs/37322105316) 성공. 독립 임시 DB/worker 계약 259건 전부 통과, 실패·생략 0.
- [x] 운영 사전 진단 37320301091 성공. V91, migration 실패 0, active/waiting 작업 0, COLLECT_ONLY 개정2 유지. 9월의 과거 미종결 일정은 연결 run 없음 상태로 이전 진단과 동일.
- [x] 고정 태그 deploy-approved-37063cc-20261005 배포 [37324180264](https://github.com/FrostyCityMan/saneB/actions/runs/37324180264) 2회차 성공. CodeDeploy d-TOBA5EV6L. attachment_qa=false, 외부 표본 재수집 없음.
- [x] 사후 health UP·JS/CSS 일치·설치/배포 원본 지문 일치 확인.
- [ ] 브라우저 QA: 현재 요청 정책상 미실행. 실제 렌더링·접근성은 통과로 보고하지 않음.

## 위험·복구

DB migration 없음. 기존 배포 백업/복구 hook을 유지한다. 화면 오류 시 직전 정상 배포 cc9b0e3 코드로 되돌릴 수 있다. 재수집·정책 게시·업무 데이터 변경·초안 생성은 이번 검증에 포함하지 않는다. 자동 파일 조회는 내부 저장 근거 GET이며 외부 첨부 다운로드를 실행하지 않는다.

## 명령

`node --check src/main/resources/static/js/saneb-announcement-attachment-review.js`

`node --test scripts/qa/attachment-simple-workspace.test.mjs scripts/qa/attachment-review-ui.test.mjs scripts/qa/attachment-operations-ui.test.mjs scripts/qa/attachment-recovery-ui.test.mjs scripts/qa/attachment-segments-ui.test.mjs`

`gradlew.bat test bootJar --no-daemon --console=plain --max-workers=1`

## 재검증 이력

첫 Linux CI 37320413214에서 기존 화면 안내 문구 계약 3건 실패. 테스트를 약화하지 않고 자동 활성화 금지·과거 집합 기준 불변 안내를 복원했다. 로컬 전체 테스트는 이 실패 원인 확인 후 본 작업의 Java 프로세스만 종료했고 전체 통과로 보고하지 않는다.

이후 HTTP 테스트 9건은 실패 0으로 끝났으나 `test --tests`가 하위 추출기 모듈에도 적용되어 명령 전체는 실패했다. 루트 `:test`로 지정한 재실행은 BUILD SUCCESSFUL. 수정 SHA의 Linux 전체 CI는 별도로 재실행한다.

재실행 CI 성공 후 배포를 시작했다. 최초 실패 후보 84bc6f6 태그는 운영 설치하지 않았다. 배포 직전 추가 사전 진단 37323509063도 성공: V91, 실패 0, active/waiting 0, COLLECT_ONLY 유지, 업무 쓰기 0. 설치/원본 JAR 지문 a433d1ddc000929022d6977f106536e494ab3a85b7e02b35d4107a28c8a01259 일치.

배포 실행 37324180264 첫 시도는 서버 설치 전 빌드에서 `AttachmentDownloadBoundaryTest.redirectsShareOneDeadlineInsteadOfResettingTimeout` IOException 1건으로 실패했다. 동일 SHA 전체 CI에서는 성공한 테스트다. 로컬 `:test --tests '*AttachmentDownloadBoundaryTest'` 12건 실패 0, BUILD SUCCESSFUL(25초). 원인은 확정하지 않았으며 테스트를 변경하거나 생략하지 않고 동일 배포 실행의 실패 job을 재실행했다.

## 운영 사후 결과

- 배포 2회차는 전체 빌드·테스트·CodeDeploy 성공. 코드 SHA 37063cc5cce357606eb9d758e2df03245c89ed48.
- 서비스 시작: 2026-10-05 23:36:18 KST. `/actuator/health` UP.
- 운영 `saneb-announcement-attachment-review.js` 및 `saneb-announcement-attachment-review.css`가 고정 코드와 일치(개행 정규화 비교).
- [사후 진단 37326277355](https://github.com/FrostyCityMan/saneB/actions/runs/37326277355) 성공, 읽기 전용·업무 쓰기 0.
- 설치/배포 원본 SHA-256: d67c4830cf60efa999c7499c1af716a71c3bfbdf4a517ec8b2a61e1691056681.
- 이전 JAR: a433d1ddc000929022d6977f106536e494ab3a85b7e02b35d4107a28c8a01259. 배포 전 설치 파일과 일치한다.
- DB V91·실패 0, active/waiting 작업 0. COLLECT_ONLY 개정2·row_version2 및 worker/source batch/GOV24 플래그 유지.
- 브라우저 QA는 현재 요청 정책상 미실행. 운영 인증 화면의 직접 클릭·시각·모바일 검증은 성공으로 주장하지 않는다.
- Decision: **Conditionally ready**. 승인된 화면 구현·코드 배포·운영 정적 자산 검증 완료. 남은 위험은 실브라우저 미검증과 재현되지 않은 1회성 다운로드 경계 테스트 실패다. 테스트 기준은 완화하지 않았다.
- 현재 공고의 판정 미적용 상태나 정책을 이번 UI 변경으로 해소하지 않는다. 운영 공고/검수/초안 생성·재수집을 실행하지 않았다.
- 이번 실행의 Node/Gradle 및 CI 감시 프로세스는 종료했다. 기존 사용자 프로세스와 미추적 파일은 보존했다.
