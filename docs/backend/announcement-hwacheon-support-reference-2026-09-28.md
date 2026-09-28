# 화천 지원사업 고정 참조·POST 사전 검증

## 단계 / Gate

전체 9 Gate는 8개 부분 완료·1개 차단이다. 이번 증분은 기존 `LOCAL_HWACHEON_POST_V1`의 지원사업 참조 공백을 한 건 줄인다. 새 엔진·프로필 등록, 첨부 텍스트 추출, 정상 후보 승인, 운영 반영을 뜻하지 않는다.

- [x] 고정 공고 32258의 제목·공식 첨부 영역·전체 파일 목록 1개·POST 다운로드·HWP signature 확인.
- [x] `expectation:null`인 참조만 catalog에 추가. 참조 40→41, 참조 보유 프로필 14→15, 저장된 기대값 1·정상 기대값 0 유지.
- [ ] 본문 2차 판정, Linux 격리 첨부 추출·구간 분석, worker→임시 DB/API 검증.
- [ ] 화천 추가 정상 표본과 다중 첨부 표본, 검토된 기대값·정책 QA·운영 E2E.

## 고정 범위·실행 제한

사용자의 전체 격리 QA 승인 아래 로컬 PC에서 신규 고정 표본을 1회만 검증했다. 서울 서버 실행으로 표현하지 않는다. 대상은 `LGS-000130` / `SAFE_SAEOL_EMINWON_LEGACY`, 기존 HTTPS host·정확한 7개 query·POST 경로만 허용한다. 검색 페이지는 후보 탐색용이며 직접 응답 검증과 구분한다.

공식 제목: **2026년 화천군 중소기업 및 소상공인 육성자금 융자추천 및 이차보전 지원계획 공고**.

1. 별도 loopback 임시 PostgreSQL에 저장소 Flyway와 DRAFT seed를 적용하고 종료한다. 운영 접속 설정은 받지 않는다. 제목 조합 통과 전에는 공고 HTTP를 호출하지 않는다.
2. `CREATE_NEW` 예약으로 같은 실행을 중복 제출하지 않는다. 상세 GET 1회·HWP POST 1회, 합계 최대 2요청·21MiB. 상세 1MiB·파일 20MiB, 각 요청 30초·시험 100초 상한이다.
3. 기존 form1/post·공식 table/th 제목을 대조한다. 주변 메뉴 제목 fallback, 다른 호스트·추가 query·redirect 후 추가 요청을 허용하지 않는다.
4. 공식 첨부 영역이 완전하고 정확히 HWP 1개인 경우에만 다운로드한다. 역할은 UNKNOWN이다. 파일 signature만 검증하며 텍스트를 추출하지 않는다.
5. 불투명 POST 값은 메모리에서만 전달한다. 임시 HTML·바이너리를 삭제하고 지문·건수·고정 상태만 남긴다.

2요청은 **32258 신규 사전 검증의 누적**이다. 9월 12일의 33897·33895 및 과거 재시도/본문 관측을 초기화하거나 화천 전체 역사 요청량을 2회로 주장하지 않는다. 중구 32/32, 함안 35/60, 강북 16/21 원장은 변경하지 않는다. 이번 예약은 소진됐으며 삭제 후 재실행하지 않는다.

## 실제 결과

관측시각 `2026-09-28T08:57:01.952742100Z` (17:57 KST).

| 항목 | 결과 |
|---|---|
| 제목 | COMBINATION_MATCHED, 고정 제목 일치 |
| 발견 | FOUND / complete=true / HWP 1개 / warning 없음 |
| 다운로드 | POST, HWP signature, 82,944 byte |
| 요청·전체 byte | 2 / 92,910 |
| 원본 정리 | temporaryOriginalRemoved=true |
| 본문 판정·첨부 추출 | 모두 미실행 |
| 정책 QA·기대값 승인 | 모두 false |
| 운영 쓰기 | 0 |

- profileHash: `4e34a0852383aad7ab5c20635340c845acc0bc43683df3d7fdf6d44429271e84`
- DRAFT rulesHash: `609cea985b8f547292340539ab68293e6148ee76fd8636d270680aa19d7227ae`
- source identity: `adef30798671fe61ce7a68021b596a06e86d843e483807d7c2a16e6c269a8d40`
- binaryHash: `dbeba265406d420c2a396e6f7d40368f499ab5ec25bc510004e4f27b964d3ea0`
- locatorHash: `5ea8a8ffafeb7e36cd0a438eff350f6cfc6bc31b2ac1369ab0316ce0627baf88`
- 영수증: `build/qa-results/hwacheon-support-32258-result.json`, SHA256 `6a22c45345e5a8580b04ed04efaeef9eea6ac92281f1fb697a6f9890779fd1cd`.
- 중복 방지 예약: `build/qa-results/hwacheon-support-32258-reservation.json`. 원본이 아니라 비식별 실행 근거이므로 보존한다.

## 검증 명령 / 결과 / 잔여

- `:test --tests '*HwacheonSupportReferencePreflightTest' --tests '*HwacheonPostAttachmentDiscoveryProfileTest'`: Java 16통과·실제 HTTP 조건부 1생략. 이 생략을 다운로드 성공으로 세지 않는다.
- 명시 `hwacheonSupportReferencePreflight`: 30초 성공, 4통과·실패/오류/생략 0. 위 실제 다운로드는 이 실행의 결과다.
- `node --test scripts/qa/attachment-official-worker-probe.test.mjs`: 13통과. 명시 task·일회 예약·자동 CI 외부 호출 금지를 검사한다.
- 참조 등록 후 `:test --tests '*HwacheonSupportReferencePreflightTest' --tests '*HwacheonPostAttachmentDiscoveryProfileTest' --tests '*AttachmentProviderQaCatalogTest' attachmentContractQaTest bootJar`: 51초 성공. 표적 Java 79통과·HTTP 조건부 1생략, 패키징 20통과. `REFERENCE_ONLY`·정상0·최소3건·기존 모든 참조 보존과 canonical source identity를 검증했다. 명시 실사이트 task를 다시 실행하지 않았다.
- `git diff --check` 통과. 이번 Gradle/Node 및 임시 PostgreSQL 종료 확인. 작업 전부터 존재한 사용자 Java/Node/PostgreSQL과 미추적 파일은 보존했다.
- Gradle 공통 인자: `--no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`. TLS 검증을 해제하지 않았다.
- 브라우저는 현재 명시 실행 지시 정책상 미실행이다. 운영 설치·DB·정책·worker 설정과 기존 migration·v1 계약은 변경하지 않았다.

등록된 프로필은 여전히 19개·엔진 7개다. catalog 참조 없는 프로필은 5→4개이며 실제 추출 정상 지원 기관이 늘었다고 보고하지 않는다. 후속은 제목→본문→전체 첨부→구간 분석→최종 관리자 검증 순서를 보존하고, 이번 해시·요청 소비를 후속 고정 범위에 연결해야 한다.
