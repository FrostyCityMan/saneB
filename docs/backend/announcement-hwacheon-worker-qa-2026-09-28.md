# 화천 32258 본문·첨부·구간 worker 검증

## 현재 단계 / 승인 경계

전체9Gate8부분/1차단이며 목표를 줄이지 않는다. 직전 [사전 검증](announcement-hwacheon-support-reference-2026-09-28.md)은 제목·POST HWP signature까지만 확인했다. 이번에는 실제 상시 worker 경로를 별도 임시 DB/API까지 연결하는 고정 실행 모드를 추가한다. 운영 설치·DB·정책·worker 설정을 변경하지 않으며 catalog 정상 기대값을 자동 생성하지 않는다.

## 실행 계약

- 모드 `HWACHEON_SEGMENT`, 공고 `HWACHEON-32258` 한 건·공식 HWP 한 개. 새 엔진/프로필은 추가하지 않는다.
- 제목→실제 본문 수집·기본 판정→공식 첨부 발견→POST 다운로드→격리 추출→구간1.0.4→worker 저장→DB/API·검수 context 대조 순서다. 제목 제외를 우회하지 않고 본문 정보 부족은 첨부 진단을 차단하는 조건으로 삼지 않는다.
- 추출기1.0.15를 DB 시작/외부 요청 전에 고정한다. profileHash는 직전 관측과 일치해야 한다. 파일82,944byte·binaryHash `dbeba265406d420c2a396e6f7d40368f499ab5ec25bc510004e4f27b964d3ea0`는 추출 전에 대조한다. 저장 locator는 `5ea8a8ffafeb7e36cd0a438eff350f6cfc6bc31b2ac1369ab0316ce0627baf88`에 묶는다.
- 실제 추출 입력에서 구간 분석을 독립 계산해 저장 분석·API 응답·평가입력 FK와 비교한다. 구간 offset의 누락·겹침을 허용하지 않는다. 기존 버전 GET의 fallback/쓰기, 다른 source 조회, 자동확정/공고 링크가 없어야 한다.
- 미관측 textHash·역할·구간 수를 미리 정답으로 만들지 않는다. COMPLETE_TEXT와 PARTIAL_TEXT를 구분하고 부분 품질은 전체 UNKNOWN·COMPLETE_TEXT_REQUIRED·REVIEW_REQUIRED를 유지한다. 완전 텍스트라도 UNKNOWN이 있으면 원문 검수를 요구한다.
- 이 시험의 API는 MockMvc다. 운영 인증/브라우저 E2E와 다르며 block API 비교는 기존 첫10개 범위다. 전체 페이지 전수 검증으로 표현하지 않는다.

## 요청 원장·자원

사용자의 전체 격리 QA 승인 안에서 기존 표본과 다른 새 고정 단일 실행이다. `Confirm-SanebHwacheonWorkerBudget.ps1`은 직전 영수증SHA·소비2요청/92,910byte·원본 정리를 검증한다. 다른 실행 계획이나 이미 존재하는 예약이 있으면 중단한다.

- 추가 최대5요청/24MiB(25,165,824byte). 본문 최대2요청/2MiB를 선예약한다.
- 32258 누적 예약 상한7요청/25,258,734byte. 과거 화천33897/33895 및 기존 지역 원장과 혼동하지 않는다.
- 서울 단일 임시 unit: CPU1·메모리768MiB·임시공간1GiB·최대20분. 원본·lease·unit·전송 객체는 종료 후 정리한다.
- `build/qa-results/hwacheon-worker-1.0.15-reservation.json`을 CREATE_NEW로 생성한다. 업로드가 지연돼도 같은 실행 handle을 추적하고 재제출하지 않는다.
- 운영 정책 게시·ENFORCE·기존 데이터 실행은 이 QA 승인에 포함되지 않는다.

## 검증 기록

- 최초 Java worker/준비 경로48검사 통과(2분12초). 새 지역 고정값·음성 사례·부분 검수 보존 검증 포함.
- 화천을 포함한 임시 DB 준비9건 및 제목/worker 회귀 묶음39통과·HTTP1조건부 생략, 패키징20통과(3분49초). `bootJar`는 변경된 생산 코드가 없어 UP-TO-DATE였으며 이를 재실행한 빌드로 표현하지 않는다.
- Node14·Python37 통과, Bash/PowerShell 구문 및 `git diff --check` 통과. 기본 `python`은 Windows 실행 별칭이어서 실제 실행되지 않았고 확인된 번들 Python으로 정정했다.
- Node 최초1실패는 정확한 launcher 모드 문자열 기대값에 신규 화천이 빠진 것이었다. 허용 목록을 명시적으로 갱신하고 재검증했다.
- 선행 a034d02 [Linux36400704333](https://github.com/FrostyCityMan/saneB/actions/runs/36400704333)는 실패다. `AttachmentPolicyValidationSnapshotFactoryTest`가 이전40참조를 기대했다. 신규41건·화천 미승격 검사를 추가하며 생산 동작/기대값 승인 기준을 낮추지 않는다.
- 해당 수정 후 snapshot14·catalog63·화천3검사, 합계80건 통과(34초). Linux 재실행 결과와 구분한다.
- 준비 패키지 `51446298d341474fb6ea0aeaa8b3f5b6`:135파일/88,422,873byte, archiveSHA256 `d5ad11c8a3918812ee9d0e3c082493599fb6b6a6e2e1f52ef5a5be30f96d7b09`, codeHash `bb17a531479d43b43d489a6fb6b09155348afce7ed5ad2f23b76cc44adffb3ca`. 예산CheckOnly 통과·새 helper class의 probe JAR 포함 확인. 서버 실행 성공 근거는 아니다.
- AWS 읽기 전용 확인: root·저장소 계정 일치, 서울 Ubuntu1대/SSM online, 최신 배포는 기존 `d-NCB2HF3YK` / SHA `9a1bb4569bcc3c13bf3bc30b51021b9149c67054`. 새 코드를 운영 반영했다는 의미가 아니다.

## 체크리스트

- [x] 고정 worker 모드·추출 전 입력 검증·구간 저장/API 검증 계약 구현.
- [x] 임시 실행 launcher/서버 검증기·음성 회귀·일회 요청 원장 구현.
- [x] 추가 화천 임시 DB 준비 경로·표적 로컬 검증·패키징. 전체 Linux CI와 실제 외부 검증은 별도다.
- [ ] 실제 서울 단일 실행·실파일 구간 결과·원본/전송 자원 정리.
- [ ] 정상 표본 추가 확보·검토된 기대값·전체Provider QA·정책 승인·운영 반영/브라우저 E2E.

이 문서의 구현·로컬 검사 상태는 실제 서울 worker 성공 영수증이 아니다.
