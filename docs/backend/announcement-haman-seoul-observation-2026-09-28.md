# 함안41306 서울 임시 격리 QA — 2026-09-28

## 재개 범위

사용자의 모든 격리 QA 승인 및 로그인 갱신 시작 지시에 따라 기존 고정 공고 `HAMAN-41306`을 관측한다. 제목→본문→전체 HWP1개→격리 텍스트 추출·구간 근거를 확인하되, 운영 DB·정책·worker 설정·설치 파일은 변경하지 않는다. 관리자 최종 검증과 자동 활성화 금지는 유지한다.

- 기준 브랜치 `codex/attachment-three-stage-linux-qa`, 소스 HEAD `d44faa903c5bfb24e1b15ad09817c5afcefc3bc0`.
- 기존 사용자 미추적 `output/`, `scripts/qa/__pycache__/`는 보존한다.
- `AGENTS.md`와 `long-goal-operating-protocol`을 읽고 이전 영수증·누적 한도와 현재 환경을 대조했다.
- AWS 로그인 갱신 후 별도 STS 확인은 `AWS_AUTH_AVAILABLE`. root principal/저장소 계정 일치, 서울 리전, Ubuntu 대상1대/SSM Online을 확인했다.
- 마지막 배포는 `d-NCB2HF3YK`, Succeeded, revision `9a1bb4569bcc3c13bf3bc30b51021b9149c67054`로 기존 기준선과 같다. 이것만으로 설치 파일/health의 현재성을 대신하지 않는다. 실행기는 별도로 전후 확인한다.

## 예산과 산출물

이전 Windows/GitHub 사용량 **11/60요청·8,496,362/100,663,296byte**를 유지한다. 이번 최대6요청·24,117,248byte를 더한 예약 상한은17요청·32,613,610byte다. 실제 결과 확인 전 예약을 해제하거나 새 모드로 원장을 초기화하지 않는다.

- 실행 ID `fb465b13f469423e9fae3945583bde6e`.
- 로컬 계획 `build/temporary-bbs-qa-fb465b13f469423e9fae3945583bde6e/plan.json`.
- 전송 패키지138파일/188,246,922byte, SHA256 `c3e5d491b84f20acadf5203bc58f0e2b305bb71673fea7dbb8abb11e29d40e36`.
- 실행 코드 지문 `d124081862880f6b2445d212026c924b62158e8e3dd59c9e5629c611209320df`.
- 서버 한도 CPU1개·메모리768MiB·임시공간1GiB·최대20분. 이 서버 실행 한도와 제출 전 로컬 패키지 전송 소요 시간은 구분한다.

## 준비 검증

```powershell
.\gradlew.bat :attachmentBbsObservationProbeJar :attachmentContractQaTest :bootJar --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'
node --test scripts/qa/attachment-bbs-observation-probe.test.mjs
```

Gradle36초 BUILD SUCCESSFUL/20task 모두 UP-TO-DATE다. 새 Java 회귀 실행으로 집계하지 않는다. Node6건, 확인된 bundled Python으로 `test_haman_observation_report.py`4건·`test_temporary_bbs_observation.py`25건을 새로 실행해 실패/생략0을 확인했다. TLS 신뢰 저장소 경로 관련 기존 Gradle 경고는 있었으나 의존성 갱신 실패는 없었다.

## 완료 판정 경계

- [x] 인증·대상·배포 기준선·누적 예산·로컬 준비 확인.
- [x] 단일 패키지 전송 및 같은 실행 ID의 서버 결과 확인.
- [x] 고정 제목 입력·본문·첨부 지문/부분 품질·원본 정리 및 설치JAR/health 대조. 완전 추출·구간 판정 완료는 아니다.
- [x] 사용량 반영, 자기 임시 객체/로컬 전송 패키지 정리.

전체9Gate=8부분/1차단이라는 직전 판정을 본 관측만으로 완료로 바꾸지 않는다. 함안 관측은 구간 엔진 worker/DB/API 저장 검증·기대값 승인·추가 표본·전체 수집원·운영 적용/브라우저 E2E를 대신하지 않는다. 현재 요청 정책에 따라 브라우저 검증은 실행하지 않는다.

## 실제 결과와 정리

SSM `a3f55bc4-c2dc-4512-9048-eb50cb85a7bd`는 Success/exit0다. 관측 시각2026-09-28 13:02:21 KST, 실제 source probe24.801초, 관측 시험1통과/실패·생략0이다. 패키지 전송에는 약20분이 소요됐으며 이 전송 지연을 공고 추출 시간으로 표현하지 않는다.

- 고정 제목 입력은 COMBINATION_MATCHED. 본문AVAILABLE/1시도/89자/redirect0이나 지원 조합은 본문만으로 확인되지 않았다. 실제 페이지 제목을 새로 검증했다는 증거로 고정 입력 판정을 대신하지 않는다.
- 전체 첨부 목록1개·발견1개·discoveryComplete=true. HWP101,888byte의 binary hash는 기존 `c8d37ea0142d19f7270c8231cde80028e8e40a3b1a73d02038dca01207a5bb97`와 일치한다.
- 추출기1.0.11로4,644자·213블록을 추출했다. text hash `ea24e32e9c049cf8a2a7d3ffae300ffdee864ac33939a78ef520cbfa334fb5f7`.
- 품질PARTIAL_TEXT, 부분 사유UNSUPPORTED_RECORD3/UNSUPPORTED_CONTROL4다. 구조 metadata에는 각주·도형·하이퍼링크·쪽 번호 등이 있으나 개별 사유와 해당 컨트롤의 일대일 대응을 검증 없이 단정하지 않는다. 완전 구간 summary 또는 worker 저장/DB/API 판정 완료가 아니다.
- 최종REVIEW_REQUIRED/ATTACHMENT_INCOMPLETE, wholeTextAnalysisComplete=false, ATTACHMENT_ROLE_UNKNOWN/ATTACHMENT_TEXT_INCOMPLETE를 유지했다. target BUSINESS, support GENERAL_SUPPORT/POLICY_FINANCE는 관측 분류이지 최종 승인 결과가 아니다.
- 운영DB미사용·쓰기0·정책QA승인false·기대값승인false·최종관리자검증필수다.

이번4요청·2,204,906예약byte를 합산해 **누적15/60요청·10,701,268/100,663,296byte**, 잔여45요청·89,962,028byte다. 기존 GitHub 실패 원장은 보존하고 이번 계획/결과를 추가 증거로 유지한다. 첫 서울 실행 보호 조건이 후속 자동 반복을 막으므로 재실행 전 반드시 새 누적 검토가 필요하다.

결과 영수증은 `build/temporary-bbs-qa-fb465b13f469423e9fae3945583bde6e/result.json`, SHA256 `871b870d51918f825d1ddf2163f0817078ca072e840b5255f7926d71144fd513`이다. unitInactive/원본정리/probe정리/전송 임시파일정리 모두true, 설치JAR불변/healthUp=true를 확인했다. S3 자기 객체 삭제와 로컬 `package.zip` 삭제도 완료했으며 계획과 metadata 영수증은 보존했다. 운영 객체는 삭제하지 않았다.

인증 blocker는 해소됐다. GitHub timeout과 달리 이번 서울 실행은 접속·다운로드·부분 추출에 성공했지만, 이전 실패 원인을 IP차단으로 확정하지 않는다. 다음 작업은 실제 HWP 부분 사유의 구조별 대응 분석과 검증된 지원 범위 보완, 이후 동일 파일 회귀/명시 구간 엔진 worker·DB·API 연결이다. 정책/운영 활성화나 정상 기대값 승인은 이번 관측에 포함되지 않는다.
