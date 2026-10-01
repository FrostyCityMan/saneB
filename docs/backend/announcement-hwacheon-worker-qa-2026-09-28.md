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

## 실제 서울 실행 결과 — 2026-09-28 18:20 KST

- 코드 `2b42461f0b71b382164a549e93d55dd4fd495696`, 실행 `51446298d341474fb6ea0aeaa8b3f5b6`, SSM `b1778b28-dda0-416a-81fd-9e84af5b5f70`: terminal Success/exit0, 44.844초, 실제 worker 검사1통과/실패0/생략0.
- 제목 COMBINATION_MATCHED → 본문 AVAILABLE/1회·ACCEPTED(TARGET_SUPPORT_CONFIRMED) → 공식 HWP1개 발견·POST 다운로드82,944byte → 추출기1.0.15·구간1.0.4 → 임시DB/API·평가입력 FK·검수context 대조 통과. 제목 단계의 BODY_UNAVAILABLE는 제목만 입력한 시점의 사유이며 후속 본문 수집 실패가 아니다.
- 첨부는 PARTIAL_TEXT, 3,491자/142블록, 대체문자0, 미지원record1/control1이다. HEADER12byte가 관측됐으나 구조별 실패 원인의 직접 연결은 아직 검증하지 않았다. 이를 원인 확정이나 완전 추출로 표현하지 않는다.
- textHash `cef605e0b2aa5867852714123c99d5eaf43813e29573382c3dcbc6dac96dc1a1`, segmentAnalysisHash `000a14b4b9f2a7b0c11ffb798cb93e7adee5ea174b3b7ae3faf54fe4d910c87d`. 전체 추출 범위는 UNKNOWN1/COMPLETE_TEXT_REQUIRED로 보존됐다. NOTICE0, 최종 REVIEW_REQUIRED/ATTACHMENT_INCOMPLETE, job PARTIAL_FAILED/processing TECHNICAL_EXCEPTION이며 원문 수동 확인·관리자 최종 검증이 필요하다.
- 성공한 것은 부분 실패를 포함한 실제 worker 저장/API 계약이다. 완전 텍스트 분석·기대값 승인·정책 QA·운영 인증 브라우저 E2E는 모두 false다. catalog41참조/15프로필·보관 기대값1/정상0과 전체9Gate8부분/1차단은 유지한다.
- 이번 사용량은 본문 상한 포함 보수적 예약4/5요청·2,190,062/25,165,824byte. 사전 검증과 합한 32258 캠페인 누적6/7요청·2,282,972/25,258,734byte다. 중구32/32는 재실행하지 않았다.
- 원본/lease0/unit inactive/서버 전송파일 정리, 운영DB사용false·쓰기0·설치JAR불변·healthUP를 확인했다. TemporaryCleanup으로 소유 S3 객체만 삭제했고 plan.cleaned=true다. 경로·길이·SHA 일치를 검증한 소유 ZIP1개도 삭제했다. ZIP은 재빌드 가능하며 계획·예약·결과 영수증은 보존한다.
- 영수증 `build/temporary-bbs-qa-51446298d341474fb6ea0aeaa8b3f5b6/result.json`, SHA256 `704b95716be252947dcf1fae5c060f4c88178e011be43207c490c1257135d90f`.
- 동일 코드 [Linux36401934960](https://github.com/FrostyCityMan/saneB/actions/runs/36401934960)는 실행 후 조회 시 in_progress였다. 로컬/실파일 검증 성공과 CI 최종 성공을 혼동하지 않는다.

## 체크리스트

- [x] 고정 worker 모드·추출 전 입력 검증·구간 저장/API 검증 계약 구현.
- [x] 임시 실행 launcher/서버 검증기·음성 회귀·일회 요청 원장 구현.
- [x] 추가 화천 임시 DB 준비 경로·표적 로컬 검증·패키징. 전체 Linux CI와 실제 외부 검증은 별도다.
- [x] 실제 서울 단일 실행·실파일 구간 결과·원본/전송 자원 정리. 부분 품질 검수 유지까지 확인했으며 정상 기대값 승인은 아니다.
- [ ] 정상 표본 추가 확보·검토된 기대값·전체Provider QA·정책 승인·운영 반영/브라우저 E2E.

다음 작업은 미지원 HWP 구조의 정확한 연결 분석과 지원 계약·회귀 검증이다. 관측되지 않은 payload를 추정해 허용하거나 부분 품질을 정상으로 바꾸지 않는다. 추가 외부 실행은 남은 예산 또는 별도 범위 확정 뒤 진행한다.

## 2026-10-01 후속 — 과거 실행 계약과 기본 단위 테스트 분리

- 위 HWP 고도화 다음 단계는 이후 사용자 지시에 따라 보류 중이다. 이번 작업은 추출기·실제 수집 코드·운영 계약을 변경하지 않는다.
- 수정 전 `4912b2e`의 Linux CI `36795394963`은 4,144통과·화천1실패·320생략, 전체 빌드8분19초 실패로 종료됐다. 기본 단위 테스트가 `selectFixedCases(HWACHEON_SEGMENT)`를 호출하면서 과거 profileHash와 현재 코드 지문 불일치에 대한 정상 차단을 테스트 실패로 취급했다.
- 표본 구조 검사는 관측 표본 정의를 직접 읽고, 별도 음성 테스트는 실제 worker 표본 선택 경로가 `HWACHEON_PROFILE_CHANGED`로 현재 지문을 거부하는지 검사한다. 예외의 기대값·실제값도 각각 과거 지문·현재 지문과 대조한다. 과거 profileHash·추출기1.0.15·고정 파일·예산·실행 차단 조건은 수정하지 않았다.
- 과거 영수증 계약의 수용은 유지하며, 현재 profileHash 또는 추출기1.0.16을 대입한 영수증은 거부한다. 현재 화천 worker 재실행 허가나 검증 완료로 해석하지 않는다. 해당 실제 QA Gate는 미완료다.
- 표적 실행: `./gradlew.bat :test --tests com.saneb.db.AnnouncementAttachmentHwacheonWorkerProbeTest --tests com.saneb.db.AnnouncementAttachmentOfficialWorkerProbeTest --tests com.saneb.domain.announcementattachment.discovery.HwacheonPostAttachmentDiscoveryProfileTest --tests com.saneb.domain.announcementattachment.qa.HwacheonSupportReferencePreflightTest --no-daemon --console=plain`: 49통과·외부 HTTP1조건부 생략,18초.
- 전체 기본 실행: `./gradlew.bat :test --no-daemon --console=plain`: XML 합계4,524건 중 **4,146통과·실패0·생략378**,12분35초. Windows 기본 테스트 결과이며 별도 Linux 임시DB/migration suite나 운영 QA 성공이 아니다. `bootJar`는 이번 테스트 전용 변경에서 별도 실행하지 않았다. 수정 후 Linux CI 결과는 별도 확인한다.
- Node 대장 검증은704기록/289표본을 재현했다. 과거 성공208/223·최신 표본 미확보18·현재 코드 실파일16/223은 변동 없다. 새 공고 요청·새 파일 확보·운영 쓰기는0이며 브라우저는 사용자 실행 정책에 따라 미실행이다.
- 서울 격리 QA 준비: 현재 Windows에서 신뢰하고 유효한 공개 루트61개를 명령 한정 CA로 검증한 뒤 STS 조회는 `AUTHENTICATION_REQUIRED`였다. 인증서 검증 우회나 전역 설정 변경 없이 로그인 갱신을 기다린다.

### 수정 후 Linux 최종 결과

- SHA `415ad3204f315002da5affdaede62098dafd1faf`, [run36797221074](https://github.com/FrostyCityMan/saneB/actions/runs/36797221074), job110163315265, 최초 실행이 최종 success로 종료됐다. artifact11134792281의 XML을 `build/qa-github-runs/36797221074`에 내려받아 검사했다.
- 기본4,524건 중4,146통과/378생략/실패0. 별도 추출기251·패키징20·job210·migration18·runtime5·worker12·Flyway3·정책 부모2는 모두 실패/생략0. suite별 실행 수이며 중복 없는 고유 요구사항 수로 합산하지 않는다.
- `bootJar` 실제 실행과 전체 Gradle8분57초 성공을 확인했다. 독립 산출물의 `SYNTHETIC_WORKER_DB_CONTRACTS_V2`는240개 발견/통과·실패/생략/미실행0이다. 정책 부모의 실제 임시DB 연결·취소 검증은PASSED, 정리는SUCCEEDED다. 합성 계약을 공식 사이트 실파일 검증이나 운영 DB 처리로 확대하지 않는다.
- 화천 과거 승인 지문 차단은 그대로다. 새 화천 외부 실행·HWP 고도화·운영 배포·정책 활성화·브라우저 검증은 실행하지 않았다. 완료된 CI 감시 프로세스와 로컬 작업 프로세스가 남지 않았음을 확인했다. 이 문서 갱신은 로컬 미커밋이며, 동일 코드에 대해 새 CI를 유발하는 추가 push는 하지 않았다.
