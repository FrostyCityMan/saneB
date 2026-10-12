# 업무 화면 공통 스타일 수정 및 운영 반영

## 목표·범위

- 사용자 요청: 외부 공고 수집과 상담 관리의 모습이 다른 원인을 다른 화면까지 점검하고 같은 문제를 수정·운영 반영한다.
- 기준 HEAD: `129e5cc4c9c188c55e6ad77bd1d01122625b19d8`, `codex/attachment-three-stage-linux-qa`.
- Design Read: 기존 크림색·청록색 사내비 업무 UI를 기준으로 바깥 여백·제목·입력 문법을 맞추되 화면별 업무 구조는 유지한다.
- 관리자·운영자 및 공통 스타일을 사용하는 회원이 대상이다. PC 업무 중심, 모바일 360px 사용을 가정한다. 변경 자체는 R0 표현 계층이며 기존 상담 배정·검수·결제 동작은 변경하지 않는다.
- DB/API/권한/업무 상태·자동 수집·실결제·운영 데이터는 변경하지 않는다. 기존 미추적 문서·목업·도구는 보존한다.
- 브라우저 QA는 이번 요청에 명시되지 않아 사용자 정책에 따라 미실행한다. 정적 선언 검사를 실제 픽셀·접근성 통과로 해석하지 않는다.

## 전체 점검 결과

`templates/app`의 HTML 37개를 모두 검사했다. 최초 진행 안내의 36개는 집계 전 수치이며 최종 집계는 37개다.

인증/공개 화면 5개(로그인·회원가입·비밀번호·접근 오류·가입 전 간단 결과)도 별도로 확인했다. `login-shell`, `invalid-access-shell`, `public-preview-shell`에 기존 여백이 정의되어 있어 업무 화면 공통 프레임을 강제하지 않았다.

| 화면군 | 템플릿 수 | 원인과 조치 |
|---|---:|---|
| 상담 관리 목록·상세·접수 | 3 | 바깥 여백·최대 너비 누락, 공통 select 배경/오른쪽 여백 덮어쓰기, 중복 H1. 공통 레이아웃과 요약 패널을 적용하고 기본 폼 스타일을 재사용 |
| 첨부 검수·전환·대기열·배치·전체 분할·정책·수집원 범위·수집원 QA | 8 | 바깥 여백 누락, 별도 회색 폼/패널 스타일, select 오른쪽 여백 축소. 공통 여백·색·폼으로 정리하고 본문 H1 2곳을 H2로 변경 |
| 알림·운영 업무 큐 | 2 | 바깥 여백·너비 누락. 공통 바깥 레이아웃 적용 |
| 기존 회원/파트너 상담 | 1 | 최상위 페이지 여백 규칙 없음. 기존 폼/업무 동작은 유지하고 바깥 레이아웃 보완 |
| 모의 결제·구독 금액 설정 | 2 | 최상위 페이지 여백 규칙 없음. 바깥 레이아웃만 적용, 결제·금액 처리 불변 |
| 나머지 대시보드·공고 입력·수집/검수·키워드·회원/기본정보·매칭·진행·검증·감사 화면 | 21 | 기존 padding 및 내부 shell 확인. 기능별 내부 너비/구조는 보존 |

- 공통 CSS에 미선언 `--line`, `--line-strong`, `--ink`, `--muted`, `--accent` 참조가 존재했다. 사내비 의미 토큰에 연결해 외부 공고 수집/검수의 테두리·텍스트·강조색 선언을 복원했다.
- 수정된 공통 바깥 레이아웃은 16개 템플릿에 적용된다. 공고 전환 화면의 1120px 전용 너비는 유지한다. 그 외 관리자·기본정보 등 기존 내부 shell의 의도적인 너비 차이는 오류로 간주하지 않는다.
- 모바일 여백은 기존 외부 공고 수집의 760px 기준 `18px 14px`을 재사용한다.
- 반복 발생 방지: 전체 app main 여백, 중복 H1, 미선언 CSS 변수, select 배경·오른쪽 여백 덮어쓰기, 포커스·체크박스·좁은 상세 너비 보존을 Node 정적 계약 6건과 CI에 추가했다.

## 변경 파일

- `static/css/saneb-dashboard.css`: 공통 레이아웃·색상 별칭·폼 토큰.
- `static/css/saneb-consultation-management.css`: 불필요한 폼 덮어쓰기 제거, 기존 요약 패널/제목/버튼 규칙 적용.
- `static/css/saneb-announcement-attachment-review.css`: 공통 폼·색·패널 사용, select 화살표 공간 보존.
- 상담 템플릿 3개, 첨부 검수/전환 템플릿 2개: 공통 요약 패널과 H2.
- `scripts/qa/workspace-style-contract.test.cjs`, 배포/계약 QA workflow: 회귀 차단.

## Gate·실행 결과

- [x] AGENTS.md 및 기존 공통/전용 CSS, 템플릿 37개 확인.
- [x] `node --test scripts/qa/workspace-style-contract.test.cjs scripts/qa/consultation-management.test.cjs`: 16건 통과.
- [x] 사람 식별·기본정보·수집 목록·첨부 전환/검수/운영/복구/구간 Node 회귀: 164건 통과. 총 180건, 실패·생략 0.
- [x] `gradlew.bat :test --tests '*ViewControllerSmokeTest' :bootJar --no-daemon --max-workers=2`: 23개 suite/107건 실패·오류·생략 0, bootJar 성공(2분 51초). MockMvc 템플릿 검증이며 실제 브라우저 검증이 아니다.
- [x] 사전 운영 진단 [38223671884](https://github.com/FrostyCityMan/saneB/actions/runs/38223671884): V91, 실패 migration 0, 현재 JAR/bundle 일치, 이전 JAR 존재, 활성/대기 첨부 작업 0, writes=0/ROLLED_BACK.
- [x] 코드 `1d6eb7e5dcba9c93ef5cdddbad0fa23fc0368bad` 커밋·푸시. 고정 태그 `deploy-approved-1d6eb7e-20261012`.
- [x] 배포 [38224254983](https://github.com/FrostyCityMan/saneB/actions/runs/38224254983): 전체 테스트·bootJar 성공(4분 37초), CodeDeploy `d-GTFTB2CBL` 성공. `attachment_qa=false`로 실제 외부 파일 수집 QA는 실행하지 않았다.
- [x] 별도 Linux DB 계약 QA [38224232182](https://github.com/FrostyCityMan/saneB/actions/runs/38224232182): 성공. 필수 DB 계약 7개 suite/281건 실패·오류·생략 0, 독립 산출물 격리 실행·정책 부모 DB 연계/취소 검증 성공, `POLICY_DB_QA_CLEANUP=SUCCEEDED`. 신규 공통 스타일 계약 6건도 통과. 로컬·CI 중복 검증 수는 합산하지 않는다.
- [x] 사후 운영 진단 [38225043904](https://github.com/FrostyCityMan/saneB/actions/runs/38225043904): 설치 JAR와 승인 bundle 일치, 직전 JAR 보존, V91/실패 0, DB endpoint hash·정책 hash·worker/source batch 설정 불변. 진단 writes=0/ROLLED_BACK.
- [x] `Invoke-WebRequest`로 운영 CSS 3개 조회: 모두 HTTP 200, UTF-8 디코딩 및 줄바꿈 정규화 후 로컬 파일과 일치. `/actuator/health`: HTTP 200, `UP`. HTTP 응답 확인이며 브라우저 렌더링 검증은 아니다.
- [ ] 브라우저 시각·키보드·실제 운영 저장 검증: 명시 지시가 없어 정책상 미실행.

## 운영 기준선·복구

- 배포 전 설치/bundle SHA-256: `b7b6116fe68daabaf6a3e8cb8c78aa0f9bcb3e98cfaac2a0e802ce81f210c3a6`.
- 배포 후 설치/bundle SHA-256: `703e7557431a78c931b00c9a5ddd14812f3b2250ee626b77abccca92b2300c53`. 직전 JAR hash는 위 배포 전 값과 일치한다.
- 운영 서비스 재시작: 2026-10-12 17:38:36 KST. 운영 코드 기준은 `1d6eb7e5dcba9c93ef5cdddbad0fa23fc0368bad`이며 후속 문서 커밋과 구분한다.
- 정책 `COLLECT_ONLY` v2, hash `88c3d36238dfe4d356186a434353ae770a6daeff491fbfb11e2bf9c4def9c4fc`, row version 2. worker/source batch 설정 유지.
- 기존 9월 RUNNING 일정 1건은 연결 실행이 없는 과거 기록으로 보존한다. 이번 활성 작업으로 해석하지 않는다.
- 새 migration 없음. 기존 CodeDeploy backup/validate로 직전 JAR 보관 및 health 실패 시 복구한다.
- 실제 브라우저의 시각적 일치·확대·작은 화면 줄바꿈은 미검증 위험으로 남는다. 정적 계약은 CSS 선언 및 템플릿 구조를 검증하며 렌더링을 대신하지 않는다.

## 릴리스 판정

| 구분 | 판정 | 근거·한계 |
|---|---|---|
| 수정 범위·기존 변경 보존 | 통과 | 표현 계층·회귀 테스트·workflow만 변경. 기존 미추적 파일 제외 |
| 로컬 단위·템플릿·빌드 | 통과 | Node 180건, Java MockMvc 107건, bootJar 성공 |
| Linux 계약·격리 실행 | 통과 | CI 성공, 필수 DB 계약 281건, 신규 공통 스타일 6건, 임시 자원 정리 성공 |
| 운영 설치·복구 파일·상태 | 통과 | 승인 SHA 설치, 직전 JAR 보존, CSS 3개 일치, health UP |
| DB·정책·설정 보존 | 통과 | V91, 실패 migration 0, 정책·설정 hash/flag 대조, 진단 쓰기 0 |
| 실제 시각·키보드·운영 저장 | 미실행 | 현재 요청의 브라우저 실행 권한 없음. 사용자 정책에 따른 검증 범위 제한 |

운영 설치는 완료했다. 시각적 수용 판정은 조건부이며 실제 브라우저 검증 완료로 보고하지 않는다. 이번 작업에서 운영 데이터 저장·수집 실행·정책 게시를 수행하지 않았다.
