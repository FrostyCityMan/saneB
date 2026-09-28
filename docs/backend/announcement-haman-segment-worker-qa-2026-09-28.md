# 함안 고정 HWP의 구간 worker·임시 DB·API 검증

## 최종 실파일 실행 결과 — 2026-09-28 13:55 KST

아래의 준비·대기 기록은 실행 전 이력이다. 소스 `059d3ec05b4c0d68801beac6b458446362a01aee`의 서울 격리 실행과 Linux 회귀를 완료했다. **부분 추출의 저장·조회 경로 검증 성공이며 정상 후보·전체 목표 완료가 아니다.**

- [x] 실행 ID `62f16ff545f64bb9ac3b58cd9c2f1a33`, SSM `245d1acb-b6c8-4b65-b6f7-2b9680da7811` Success/exit0. probe35.965초, found1/passed1/failed0/skipped0/aborted0/failedContainers0이다. 약10분은 패키지 전송 시간이므로 사이트 수집 시간과 구분한다.
- [x] 본문 AVAILABLE/1회·본문 단계 완료, 전체 HWP1개 발견·다운로드·처리·봉인. 제목 입력은 FIXED_OFFICIAL_SAMPLE이며 새 목록 수집을 입증하지 않는다. 이 보고서는 본문 길이·hash를 제공하지 않으므로 이전89자와 같다고 단정하지 않는다.
- [x] HWP101,888byte·4,644자·213블록, 고정 binary/text/locator hash 일치. extractor1.0.12, engine attachment-segment-1.0.0, segment-role-1.0.4이다. 분석 hash `66dc29aa96b9399d0d4467e4f541e6b61dff2b2701f7721e271655bfc744f5e3`, 전체 범위 UNKNOWN1/NOTICE0·COMPLETE_TEXT_REQUIRED를 유지했다.
- [x] 실제 IPC 입력 분석과 저장 결과, 평가 입력 FK, 버전 선택 API/평가 결합 API, 이전·다른 버전 GET 무변경, 검수 컨텍스트 일치 검증이 모두 true다. MockMvc 기반 임시 DB/API 검증이지 인증된 브라우저 E2E가 아니다.
- [!] workerStatus EVALUATED이지만 jobStatus PARTIAL_FAILED / processingStatus TECHNICAL_EXCEPTION / REVIEW_REQUIRED / ATTACHMENT_INCOMPLETE다. 본문도 BODY_COMBINATION_NOT_CONFIRMED이며 부분 결과를 정상 후보로 승격하지 않았다. 원문 확인·최종 관리자 검증 요구를 유지한다.
- [x] 실제 사용4요청·2,204,906예약byte를 더해 누적 **23/60요청·15,111,080/100,663,296byte**, 잔여37요청·85,552,216byte다. 이전 모드 원장을 초기화하지 않았다.
- [x] 운영 DB 미사용/쓰기0, 정책QA·기대값 승인·전체 텍스트 완료·브라우저E2E 모두 false. 원본·lease·임시 DB·전송 디렉터리·unit 정리, 운영JAR불변/healthUP을 확인했다. 종료 확인 뒤 소유S3 객체와 지문이 일치하는 로컬 package.zip만 제거했으며 plan/result 영수증은 보존했다. 운영 설정 전체나 DB migration 현황을 재조회한 것은 아니다.
- [x] [Linux36378798262](https://github.com/FrostyCityMan/saneB/actions/runs/36378798262) 동일 소스 SHA completed/success. 내려받은 XML root3053=2760통과/293조건부 생략/실패·오류0, 추출기168·패키지20·jobDB209·migration18·정책 부모2·runtime5·worker12·Flyway3도 실패·오류0이다. 신규 HamanWorkerProbe3/준비 fixture7 통과. CI 준비 fixture는 외부 실파일 요청을 하지 않으며 서울 결과와 구분한다.
- [!] 보고서 reviewPhrasePresence의 고정 한글 키가 전송/표시 과정에서 깨져 해당 네 문구 존재 여부는 판정 근거에서 제외한다. 로컬 source와 컴파일 class의 한글 UTF-8은 확인했고 PYTHONIOENCODING=utf-8 재조회만으로 해결되지 않았다. 손실 발생 지점은 미확정이며 원문 추출 손상으로 단정하지 않는다. 추출 textHash와 replacementCharacterCount0 및 ASCII 필수 계약 검증과 별개다. 이 진단만을 위해 실파일을 재수집하지 않았다.

실행 증거:

- package135파일/88,401,862byte; SHA256 `934e36d2e057a63fc14a36497767fbd0f57f40961d885815c98a270a480fcf38`.
- executionCodeHash `9fb08bcab4b5596cd7ff2911e1a7775aa3d94cb13474e1ca67ddb72b67e60c2a`; probeHash `5720102a09990c347d3c89261665904e656db6212f828694c7fbb0ab392ab060`.
- 로컬 영수증 `build/temporary-bbs-qa-62f16ff545f64bb9ac3b58cd9c2f1a33/result.json`, SHA256 `c9e6a8699ea52fab3e547acc512582023d7b75733b460bdce55df4e0cac4c511`.
- Linux XML `build/qa-results/linux-36378798262`; 실행 명령은 소유 plan을 대상으로 TemporaryRun → 같은 command의 TemporaryPoll → terminal 확인 후 TemporaryCleanup이다. 중복 제출하지 않았다.

전체9Gate=8부분/1차단은 유지한다. 다음은 부분 품질의 원인별 지원 범위, 참조 없는 수집원과 정상 기대값, 운영 적용·기존 데이터·동일SHA 업무 E2E다. 이번에는 운영 배포·정책 게시·재분류·브라우저 실행을 하지 않았다.

## 목표와 승인 범위

함안41306의 제목→본문→전체HWP1개를 실제 worker에 입력하고, 부분 추출 결과와 구간 분석이 임시 PostgreSQL 및 v2 조회 API에 그대로 결합되는지 검증한다. 선행1.0.12 추출 관측 성공은 이 저장 경로의 성공 근거가 아니다.

모드는 `HAMAN_SEGMENT`, 고정 참조는 기존 `HAMAN-41306` 하나다. 명시 `segment-role-1.0.4`/현재 고정 지문을 사용하며 기본 정책·기존 보은 모드·운영 데이터는 바꾸지 않는다. 추출기가 아직 PARTIAL_TEXT를 반환하므로 UNKNOWN/COMPLETE_TEXT_REQUIRED·검수·원문 확인 요구가 정답이다. 정상 기대값 승인이나 전체 Provider QA 통과가 아니다.

현재 누적19/60요청·12,906,174/100,663,296byte를 보존한다. 새 실행은 최대5요청/24MiB이며 본문 최대2회 예약을 포함한다. 실제 실행 전 영수증·인증·설치 기준선을 재확인하고 같은 실행을 중복 제출하지 않는다. 코드는 실행 가능 범위를 정의할 뿐 추가 승인이나 자동 실행을 만들지 않는다.

## 구현·검증 순서

1. [x] 기존 공식 worker 시험에 명시 고정 함안 모드를 추가했다. 임의 URL·자동 CI 외부 호출은 제공하지 않는다.
2. [~] 실제 IPC와 저장 text/blocks, 분석 재현 결과, 평가 입력 FK, 선택 버전 GET/평가 결합 GET을 대조하도록 구현했다. 실제 실행은 대기다.
3. [~] 이전 버전 GET의 NOT_ANALYZED 및 조회 무생성, 다른 source 거부를 기존 검증 경로로 연결했다. 실제 실행은 대기다.
4. [~] PARTIAL_TEXT 전체 범위 UNKNOWN, 검수·원문 확인·확정/link0을 검사하도록 구현했다. 이 항목은 실파일 성공 전까지 미완료다.
5. [x] 표적 Java32·패키지20·Node10·Python26 통과, probe/bootJar 준비를 확인했다. Windows AppControl 차단이 있는 실제 PG 검증은 Linux에서 별도로 수행한다.
6. [~] Python 서울 실행기의 고정 범위/보고서 검증을 연결했다. 로컬 전송 helper의 새 누적 보호 조건·실제 실행·정리·이번 SHA의 원격 CI 결과는 대기다.

## 고정 증거와 판정 경계

- binary SHA256 `c8d37ea0142d19f7270c8231cde80028e8e40a3b1a73d02038dca01207a5bb97`,101,888byte.
- text SHA256 `ea24e32e9c049cf8a2a7d3ffae300ffdee864ac33939a78ef520cbfa334fb5f7`,4,644자/213블록.
- 새 구간 analysis hash는 원본 IPC 전체 입력으로 재현하고 DB/API와 대조한다. 선행 관측에 없는 구간 hash를 승인된 기대값으로 꾸미지 않는다.
- 보고서는 원문 없이 고정 식별자·지문·범위 검증 여부·검수 상태만 반환한다. MockMvc API 검증은 인증·실제 브라우저 검증이 아니다.
- 성공: 실제 worker EVALUATED,전체1개 처리/봉인,동일 지문·분석/FK/API 일치,UNKNOWN/검수·원문 확인 유지,운영쓰기0,원본·lease·임시 DB 정리.
- 실패: 고정 파일 변경·누락·예산 초과·다른 버전 대체·관측값을 승인 기대값으로 사용·부분 결과 정상 승격·조회에 의한 변경·운영 데이터 사용·자원 잔류.

DB/migration/v1 계약·서비스 분류 정책·추출기 지원 범위는 이번 검증 연결에서 변경하지 않는다. 전체9Gate와 ATT/SEG 완료 조건을 축소하지 않는다.

## 로컬 검증과 남은 실행

```powershell
.\gradlew.bat :test --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' --tests '*AnnouncementAttachmentHamanWorkerProbeTest' :attachmentContractQaTest :attachmentOfficialWorkerProbeJar :bootJar --no-daemon --max-workers=1
node --test scripts/qa/attachment-official-worker-probe.test.mjs
# 확인된 Python 런타임에서 -B -m unittest discover -s scripts/qa -p test_temporary_bbs_observation.py
```

Gradle36초 성공, Java기존29/신규3·패키지20 실패/오류/생략0이다. DB 준비 fixture 추가 뒤31초에 같은 표적32/패키지20을 다시 통과했다. bootJar/추출기는 업무 코드 변경이 없어 기존 동일 소스 결과를 재사용했다. Node10/Python26도 통과했다. 누락된 저장 근거, 숫자 대신 문자열/boolean, 바뀐 파일·버전·형식·구간 수, 정상 후보/전체 완료 위조, 초과 예산을 거부한다. 합성 보고서의 분석 hash는 형식만 검사하며 실제 저장 실행에서는 전체 IPC 원문/block 재현 결과와 DB/API 분석을 직접 대조한다.

실제 Linux 임시 DB의 준비 fixture 시험에도 함안1건을 추가했다. Windows의 기존 pg_ctl 실행 차단을 우회하거나 해당 시험을 성공 처리하지 않는다. 일반 CI는 합성 본문으로 준비 경로만 검사하고 실제 공고 요청은 하지 않는다. 새 `attachmentHamanSegmentWorkerIntegrationTest`와 `HAMAN_SEGMENT` 서버 모드는 명시 실행 전용이다.

현재 추가 외부 공고 요청0·운영 변경0·함안 누적19회 유지다. 이전835bd1a Linux 실행은 이번 추가된 시험 코드보다 앞선 코드이며 성공하더라도 새 함안 worker 실증을 대신하지 않는다. 브라우저는 현재 요청 정책상 미실행이다.

코드 `059d3ec05b4c0d68801beac6b458446362a01aee`를 QA 전용 브랜치에 푸시했고 원격SHA 일치를 확인했다. 새 [Linux36378798262](https://github.com/FrostyCityMan/saneB/actions/runs/36378798262)는 실행 중이며 완료 여부는 후속 확인한다. 선행835bd1a의 Linux36377684922는 최종 성공과 XML을 확인했지만 이 새 시험/함안 실제 worker의 성공을 의미하지 않는다. 자체 Gradle/Node/Python 실행 종료 후 기존 사용자 프로세스와 output/·scripts/qa/__pycache__/는 보존한다.
