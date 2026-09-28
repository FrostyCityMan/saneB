# 함안 고정 HWP의 구간 worker·임시 DB·API 검증

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
