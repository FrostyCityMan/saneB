# 자동 제외 이력 집계 운영 반영

## 승인 범위와 기준

- 코드 SHA: `5f67880e07d776c52f8795f79d8ac947f81ab4d4` (구현 `7a9aad5`, 화면 테스트 계약 보완 `5f67880`).
- 고정 태그: `deploy-approved-5f67880-20261012`.
- 대상: 서울 `ap-northeast-2`, 기존 `saneb` / `saneb-dev` CodeDeploy.
- 저장 공고 목록의 EXCLUDED 건수 대신 기존 제외 tombstone의 고유 기록을 조회한다. `provider_code + identity_hash` 중복 제거 기준이며 반복 횟수를 합산하지 않는다.
- 최근 연결된 수집 실행에서 PRODUCTION으로 확인된 기록만 본 집계에 포함한다. QA는 제외하고 연결 정보가 없는 기록은 용도 미확인으로 분리한다. 영구적인 최초 용도나 기간별 통계로 해석하지 않는다.
- 제목·본문·URL·식별 해시를 API에 제공하지 않는다. 사유는 마지막 저장값이고 제외 이력은 현재 후보 수와 합산하지 않는다.
- 읽기 전용 v2 API를 신설하고 v1 계약을 유지한다. 카드별 로딩·실제 0건·오류·재시도를 분리한다.
- DB migration, 규칙·정책 변경, 기존 데이터 재처리, 원문 복원, 자동 활성화는 없다. 기존 미추적 파일은 커밋에 포함하지 않는다.

## Gate

- [x] 관련 코드·계약·회귀 테스트 20개 파일만 커밋·푸시.
- [x] 로컬 Java/API 테스트 16/16 및 `bootJar` 성공. 보완한 서버 화면 테스트 추가 10/10 성공.
- [x] 로컬 Node 회귀 테스트 88/88, 읽기 전용 진단 Python 테스트 14/14, `git diff --check` 성공.
- [x] 사전 운영 읽기 전용 점검 [38203879369](https://github.com/FrostyCityMan/saneB/actions/runs/38203879369) 성공. 운영 용도 확인 29,233건, QA 0건, 용도 미확인 8,291건. 시점별 스냅샷으로 상시 수집에 따라 변할 수 있다.
- [x] 사전 DB V91·migration 실패 0, 활성/대기 첨부 작업 0. 정책 COLLECT_ONLY v2 및 정책 hash, worker/source batch 설정 유지. 오래된 RUNNING 스케줄 1건은 보존한다. 진단 writes=0/ROLLBACK.
- [x] 사전 설치 JAR와 기존 배포 bundle SHA-256 `93ce5711b5e5d97483488a75072770f5c709b29a52ba021d8bfde40619c04624` 일치. 이전 복구 JAR 존재.
- [x] 최초 Linux CI [38203866941](https://github.com/FrostyCityMan/saneB/actions/runs/38203866941) 실패 근거 보존. 기존 화면 테스트가 제거된 EXCLUDED 탭을 요구하여 1건 실패했다. 후속 필수 DB 검증도 선행 실패로 일부 실행되지 않았다. 탭 부재 및 새 요약·스크립트 존재를 검사하도록 기대 계약을 수정했다. 실패를 무시하고 배포하지 않았다.
- [x] 후속 Linux CI [38204744747](https://github.com/FrostyCityMan/saneB/actions/runs/38204744747) 전체 성공. 단위·HTTP·실제 임시 DB·migration·artifact, 독립 실행기, 정책 QA 연결·취소·정리 통과. 필수 DB 7개 suite 281/281(실패·오류·생략 0). 실제 PostgreSQL 집계와 QA 분리·중복 횟수 비합산 회귀를 포함한다.
- [x] 배포 직전 갱신 점검 [38205606695](https://github.com/FrostyCityMan/saneB/actions/runs/38205606695) 성공. 최초 사전 점검 대비 JAR·DB·정책·설정·제외 용도별 건수 및 작업 상태 동일. writes=0/ROLLBACK.
- [x] 승인 태그 코드 운영 배포 [38205994492](https://github.com/FrostyCityMan/saneB/actions/runs/38205994492) 성공. 배포 workflow 재테스트·빌드 통과, CodeDeploy `d-WBYM6A8BL` Succeeded. `attachment_qa=false`.
- [x] 사후 HTTP health 200/UP, 새 v2 요약 API 비인증 접근 401. 운영 JavaScript는 승인 소스와 줄바꿈 정규화 후 일치한다. `saneb-collected-summary.js` SHA-256 `36706f41f68b97fd2b005e7d9a19cb08cbf6fbb321b02ab7528ae31600bb8a05`, `saneb-collected-announcements.js` SHA-256 `cdb07f8e8488966d9edb7c1c1f4088ba90bed8d464512150fd4dc8742fd1f5b5`.
- [x] 사후 읽기 전용 점검 [38206449633](https://github.com/FrostyCityMan/saneB/actions/runs/38206449633) 성공. 설치/bundle SHA-256 `6ad82b18c7fa5cd85c9f4bec283bfa9053cc78571becee1e37a8c3bce4bbe45b` 일치. 복구 JAR는 배포 직전 지문과 일치한다. DB V91·migration 실패 0, 정책 COLLECT_ONLY v2/hash와 worker/source batch 설정 유지, 활성 첨부 작업 0. 제외 용도별 건수도 사전 점검과 동일하다. 진단 writes=0/ROLLBACK. 서비스 시작 2026-10-12 14:03:40 KST.

## 결과와 실행 명령

Decision: **Conditionally ready** — 승인된 코드 운영 설치와 서버 사후 검증 완료. 남은 배포 차단 사항은 없으며 실제 브라우저 렌더링·인증 후 화면 조작은 미실행이다.

- `gradlew.bat :test --tests '*AnnouncementSourceExclusion*' --tests '*AnnouncementSourceControllerSmokeTest' bootJar --no-daemon --max-workers=1` → Java/API 16/16 및 빌드 성공.
- `gradlew.bat :test --tests '*AnnouncementSourceViewControllerSmokeTest' --no-daemon --max-workers=1` → 서버 화면 렌더링 10/10 성공.
- `node --test scripts/qa/attachment-conversion.test.mjs scripts/qa/attachment-simple-workspace.test.mjs scripts/qa/attachment-review-ui.test.mjs scripts/qa/collected-summary.test.mjs scripts/qa/collected-navigation.test.mjs` → 88/88 성공. 가상 DOM 검증이며 실제 브라우저 QA는 아니다.
- 번들 Python `-B scripts/qa/test_deployment_preflight.py` → 14/14 성공.
- `gh workflow run deploy.yml --ref deploy-approved-5f67880-20261012 -f attachment_qa=false` → 성공.
- 승인된 진단 workflow 사전·사후 실행, Node HTTP health/JS/401 확인 → 성공. 실행한 단일 Node·Gradle 프로세스는 종료됐다.

## 검증 경계와 복구

현재 요청에 브라우저 QA 명시 지시가 없어 정책상 미실행이다. Node 가상 DOM 테스트와 정적 파일 HTTP 비교를 실제 브라우저 렌더링으로 표현하지 않는다. 인증된 운영 API 응답 및 관리자 화면 조작은 미검증이다.

기존 GitHub Actions OIDC 및 CodeDeploy 경로에서 고정 태그로 배포한다. 운영 외부 파일 QA는 `attachment_qa=false`로 비활성화한다. 기존 `backup.sh`의 `app.jar.previous` 보존 및 `validate.sh`의 health 실패 복구 경로를 유지한다. DB schema 변경이 없어 이번 UI/API 변경에 따른 역방향 migration은 필요하지 않다.
