# 수집 공고 검색 복귀 수정 배포

## 범위와 기준선

- 사용자 승인: 검색 복귀 오류 수정 후 운영 반영.
- 코드: `cc9b0e3218e4419afdddfec069d11bd535eb5a07`.
- 고정 태그: `deploy-approved-cc9b0e3-20261005`.
- 검색 조건·탭·페이지·선택 공고를 현재 방문 이력에 보관하고 pageshow에서 복원 후 조회한다. 미제출 입력은 적용 조건으로 저장하지 않는다.
- 코드·테스트·CI 설정 3개 파일만 커밋했다. 기존 사용자 미추적 파일은 제외했다.
- DB migration, API 계약, 인증, 수집 정책, 데이터 재처리 변경 없음.
- 원문·검수 입력을 방문 이력에 보관하지 않는다. 검색어는 현재 방문 이력에 보관되며 URL에는 추가하지 않는다.

## 배포 Gate

- [x] 로컬 관련 Node 회귀 57건 통과. 배포 직전 핵심 10건 재실행 통과.
- [x] JavaScript 구문 검사와 git diff --check 통과.
- [x] 로컬 `gradlew.bat bootJar --no-daemon` 성공.
- [x] 사전 진단 [37311629598](https://github.com/FrostyCityMan/saneB/actions/runs/37311629598) 성공.
- [x] 배포 [37311783795](https://github.com/FrostyCityMan/saneB/actions/runs/37311783795) 성공. Linux 테스트·빌드 5분 26초, CodeDeploy `d-Z16A3AU6L` 성공.
- [~] 동일 SHA Linux 계약 CI [37311629226](https://github.com/FrostyCityMan/saneB/actions/runs/37311629226) 진행. 신규 검색 복귀·첨부 요약 단계 성공.
- [x] 설치 지문·health 사후 확인.
- [ ] 브라우저 재검증: 현재 요청에 명시되지 않아 사용자 정책상 미실행.

## 배포 전 운영 상태

- DB V91, migration 실패 0, active/waiting 첨부 작업 0.
- COLLECT_ONLY 개정2, 정책 row_version2 유지.
- worker/source batch 활성 및 정부24 상세 플래그 UNSET 유지.
- 과거 미종결 일정 1건은 2026-09-09 갱신·연결 run 없음으로 직전 진단과 동일. 수정하지 않는다.
- 설치 JAR와 배포 원본 SHA-256: `54c682e428114bc1f0f10993c683e88b2b4fa64323a45a367beca775a835c9b7`.
- 기존 백업/자동 복구 배포 hook 유지. 실제 롤백 훈련은 실행하지 않는다.

## 미실행 범위

- 실제 브라우저 뒤로 가기 재검증, 본문 복구 적용, 첨부 재수집, 정책 게시·ENFORCE 변경, 운영 공고 생성·활성화.
- 로컬 Node 검증 프로세스는 종료됐으며, 임시 서버나 브라우저를 실행하지 않았다.

## 사후 검증

- [사후 진단 37312734457](https://github.com/FrostyCityMan/saneB/actions/runs/37312734457) 성공. READ ONLY/ROLLBACK, 업무 쓰기 0.
- 설치 JAR와 배포 원본 SHA-256 일치: `a433d1ddc000929022d6977f106536e494ab3a85b7e02b35d4107a28c8a01259`.
- 이전 JAR는 위 배포 전 V91 지문과 일치하여 보존됨.
- 서비스 시작: 2026-10-05 21:51:51 KST. health `UP`.
- 운영 `/js/saneb-collected-announcements.js` 전체 문자열이 로컬 고정 커밋 파일과 일치. 검색 복원·pageshow 코드 제공 확인.
- DB V91, migration 실패 0. 정책 id/hash/row_version 및 worker/source batch·정부24 플래그가 사전 진단과 동일.
- active/waiting 작업 0, 과거 미종결 일정은 같은 식별자·시각 유지. 전체 업무 데이터의 불변성을 전수 검사한 것은 아님.
- Decision: **Conditionally ready**. 승인된 코드 설치와 HTTP 확인 완료. 실제 브라우저 복귀 재검증은 현재 요청 정책상 미실행.
