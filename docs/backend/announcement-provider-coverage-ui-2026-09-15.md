# 수집원별 첨부 검증 범위 조회 화면

## 목적과 범위

제목 1차 판정 → 정제 본문 2차 판정 → 실제 PDF/HWP/HWPX 텍스트 3차 판정 → 관리자 최종 검증 순서를 유지한다. 이 증분은 전체 고정 표본의 부족한 기대값을 찾는 읽기 전용 화면이며 크롤링·QA 예약·정책 게시·기존 데이터 적용이 아니다.

Design Read: 기존 사내비 Thymeleaf·Bootstrap 업무 화면을 재사용해 전체 대상과 현재 페이지, 미관측·부족·계획 준비·실제 검증을 분리한다. 새 디자인/상태/차트 라이브러리는 도입하지 않는다.

- 사용자: 활성 ADMIN/OPERATOR/APPROVER, 비밀번호 변경 완료는 API에서 검증. USER/PARTNER/REVIEWER·익명은 거부한다.
- 핵심 과업: 정책 상세에서 진입 → 전체 분모/부족 형식 확인 → 기관별 정상 공고·다중 첨부 기대값 확인 → 담당자에게 구현·표본 보완 요청.
- 위험: R0 읽기 전용이나 잘못된 통과 표현의 운영 영향은 높다. 계획 조회를 실제 검증·정책 승인으로 대체하면 실패다.
- 재사용: 실제 layout·정책 상세·`saneb-announcement-attachment-review.css`·정책 요청 클라이언트. 바이너리 문서/사용자 output은 변경하지 않는다.
- 상태: 초기/로딩/빈 결과/범위 밖 페이지/권한/로그인 만료/409 설치·계획 불가/네트워크 실패/계약 오류/페이지 간 계획 변경/정상 조회. 실패 시 이전 결과를 지우고 주소는 유지하며 자동 재시도하지 않는다.
- 기기·접근성: 모바일 320/360/375/768px 및 데스크톱 1024/1440px, 큰 글자/확대·키보드·focus-visible·status/alert. 기존 반응형 카드와 dl 사용, 가로표/색상만의 성공 표시는 사용하지 않는다.
- 검증: 순수 Node 계약·상호작용, MockMvc 서버 렌더링/권한/무변경 컨트롤, 전체 회귀/bootJar. 목표에 포함된 브라우저 QA는 실제 렌더링을 합성 fixture로 검증하고 운영 검증과 분리한다.
- 측정: 조회 성공/권한 실패/계획 변경의 시나리오별 회귀 검증으로 측정하며 새 외부 분석 SDK·수집 이벤트는 도입하지 않는다.

## DB/API/UI 계약

- DDL·기존 migration·DB/API 응답 변경 없음. 새 화면 `/app/admin/announcement-attachment-provider-coverage?policyId={UUID}&page={n}`.
- 기존 정책 상세에서 선택한 ID를 연결한다. 편집 입력·정책 QA의 의존 요청에 추가하지 않아 Linux 설치 미완료로 편집 화면 전체가 실패하지 않는다.
- GET `/api/v2/admin/announcement-attachment-policies/{policyId}/provider-qa-runs/execution-plan/targets?page={n}&size=20`만 요청한다. 같은 출처/no-store/redirect:error. 정책 저장/예약/파일 HTTP 요청은 없다.
- 정책 ID·행 버전·3개 지문·현재 페이지/전체 건수·형식 집합·기관별 기대값 논리·isQaPassed=false를 검증한다. 사이트가 제공하지 않는 형식이라고 추측하지 않는다.
- 정상 공고 최소3, 정상 다중 첨부 최소1. 부분/OCR 파일의 부족 형식을 유지한다. 기대값 준비 완료도 실제 QA 통과로 표현하지 않는다.
- 페이지 사이 입력/정책/표본/계획/전체 대상 지문이 바뀌면 결과를 섞지 않고 첫 페이지 재조회를 요구한다. 응답 원문 오류나 임의 URL/파일명/본문은 표시하지 않는다.
- 기관명이 API에 없으면 공개 코드만 표시하며 이름을 추측하거나 별도 광범위 조회를 하지 않는다.
- 수집원 QA 예약·취소·실행 이력 UI는 후속 별도 화면에 연결했다(`announcement-provider-qa-ui-2026-09-15.md`). 이 coverage 화면은 계속 읽기 전용이며 전체 공식 기대값·실파일/worker/운영·최종 관리자 검증 Gate를 통과시킨 것으로 보지 않는다.

## 검증 기록

- [x] 새 화면·정책 상세 링크·읽기 전용 클라이언트 구현.
- [x] 새 조회46건 + 기존 정책 UI36건 = Node82건 통과, 실패/생략0. 단독 재실행46건도 통과.
- [x] MockMvc42건(새 SSR·권한7, 기존 정책8, Provider 관리27)·bootJar37초 성공, 실패/생략0.
- [x] 실제 SSR+합성 응답의 Chrome 조회·페이지 이동·초점 복귀·계획 변경 차단·401/403/409/계약 오류/연결 종료 검증. 오류 때 결과0행, 오류 초점, 명시적 재조회 유지. 변경 요청0·외부 요청0·운영 쓰기0.
- [x] 1440/1024/768/375/360/320px의 scrollWidth/clientWidth 일치, 가로 넘침 없음. 1440·375·768 스크린샷 보관, 1440·375 직접 시각 확인. Tab/Shift+Tab/Enter 재조회와 focus-visible(solid) 확인. 정상 페이지 콘솔 오류/경고0.
- [~] 확대 검증은 부분적이다. CSS zoom2배/4배 모의는 scrollWidth1426/2851 대 clientWidth1425로 넘침을 관측했다. 이를 정상 통과로 세지 않으며, 브라우저 기본 200%/400% 확대·보조공학 검증은 미실행이다. 좁은 viewport 통과가 이 미검증을 대체하지 않는다. 공통 레이아웃을 추측으로 수정하지 않았다.
- [x] 최종 전체 `:test :attachment-extractor:test attachmentContractQaTest bootJar installAttachmentContractQa --no-daemon --max-workers=1` 3분19초 성공. root2491=2239통과/252조건부 생략/실패0, QA 패키지20/20. 추출기25건·bootJar는 UP-TO-DATE이며 재실행 성공으로 세지 않는다. 현재 소스 bootJar는 앞 표적37초 실행에서 생성했다. 전체 Node200건 통과/생략0, 최종 정책 상세 진입 링크 assertion 추가 후 동일200건 재통과.
- [ ] 같은 SHA Linux CI·운영 환경·운영 브라우저 검증.

전체 장기 목표는 Not ready다. 이 문서의 조회 UI 구현 완료와 전체 수집 프로세스 완료를 혼동하지 않는다.

현재 bootJar SHA256: `c3fc4d4f1b7b2b5c95c066fefcf6dc96d174468704efd199365052fb9256406e`. 실제 운영 JAR가 아니다. 일반 push의 Linux QA에 새46건을 포함했으며 공식 파일 관측·배포 표식은 추가하지 않았다.

브라우저 도구: `browser-qa`/`playwright` 지침에 따라 이름 있는 독립 Chrome 세션과 loopback 합성 서버를 사용했다. 처음 npx 메타데이터 확인 지연 후 로컬 캐시 `npx.cmd --offline --yes --package @playwright/cli playwright-cli`로 진행했다. 첫 복합 시나리오 명령은 PowerShell 인용 문제로 실행되지 않았고 수정 후5개 오류 시나리오를 실행했다. 정상 앱 오류와 구분한다. 도구가 만든 snapshot/합성 로그는 `build/attachment-coverage-ui-qa/browser-session-20260915`로 옮겨 보존했으며 브라우저와 Node 서버는 종료했다. 운영 인증·쿠키는 사용하거나 저장하지 않았다.
