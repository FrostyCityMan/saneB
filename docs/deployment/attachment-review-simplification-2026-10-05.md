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
- [x] 관련 Node 회귀 69건 + 신규 화면 상태 6건 통과.
- [x] JavaScript 문법 검사.
- [~] 로컬 Gradle test/bootJar 진행.
- [ ] 동일 SHA Linux CI.
- [ ] 운영 사전 진단.
- [ ] 고정 커밋 배포.
- [ ] 사후 health·정적 자산·설치 지문 확인.
- [ ] 브라우저 QA: 현재 요청 정책상 미실행. 실제 렌더링·접근성은 통과로 보고하지 않음.

## 위험·복구

DB migration 없음. 기존 배포 백업/복구 hook을 유지한다. 화면 오류 시 직전 정상 배포 cc9b0e3 코드로 되돌릴 수 있다. 재수집·정책 게시·업무 데이터 변경·초안 생성은 이번 검증에 포함하지 않는다. 자동 파일 조회는 내부 저장 근거 GET이며 외부 첨부 다운로드를 실행하지 않는다.

## 명령

`node --check src/main/resources/static/js/saneb-announcement-attachment-review.js`

`node --test scripts/qa/attachment-simple-workspace.test.mjs scripts/qa/attachment-review-ui.test.mjs scripts/qa/attachment-operations-ui.test.mjs scripts/qa/attachment-recovery-ui.test.mjs scripts/qa/attachment-segments-ui.test.mjs`

`gradlew.bat test bootJar --no-daemon --console=plain --max-workers=1`
