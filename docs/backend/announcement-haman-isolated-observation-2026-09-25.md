# 함안41306 서울 격리 관측 — 2026-09-25

## 목표·범위·완료 조건

앞선 [함안 참조 QA](announcement-haman-support-reference-qa-2026-09-25.md)는 본문과 전체HWP1개 다운로드/signature까지 확인했다. 이번 단계는 같은 고정 원본을 Linux 격리 추출기로 처리하여 품질과 구간 근거를 관측하는 것이다. 지원사업 참조1건을 정상3건 또는 함안 전체 지원으로 승격하지 않는다.

- [x] 고정 `HAMAN-41306`/`LOCAL_HAMAN_GET_V1` 관측 모드와 원본 지문 계약을 추가했다.
- [x] 기존6요청/23MiB 상한·누적8요청/6,399,210byte 원장을 보존했다.
- [x] Java·Python·Node의 고정 범위/거짓 완료/부분 추출 거부 검증과 probe JAR 생성을 확인했다.
- [!] AWS 로그인 갱신 필요. 서울 실행·실제 추출·원본 정리 결과는 아직 없다.
- [ ] 실제 Linux 파일 품질·문자/블록·구간 근거·부분 사유와 종합 판정을 대조한다.
- [ ] 추가 적격 표본2건·관련 형식·명시 구간 엔진 worker/임시 DB/API·검토된 기대값·운영 업무 E2E.

성공 기준은 정확한 공고·실파일 지문·전체 첨부 수·제목→본문→첨부 순서·실제 격리 추출·임시 원본 정리의 일치다. `PARTIAL_TEXT` 등은 관측 성공일 수 있으나 완전 추출/정상 후보 성공이 아니다. 다른 공고·파일·초과 예산·누락 파일·부분 결과의 정상 승격·운영 변경은 실패다.

## 실행 계약

`HAMAN_OBSERVATION`은 기존 기본 모드를 바꾸지 않는 명시 모드다. Java 선택기→Bash→임시 서버 실행기 manifest/report에 동일한1공고를 고정한다. 모든 다른 모드의 범위는 유지한다. 기존 함안 Windows 사전 확인 task는 서울 모드와 별개이며 자동 실행하지 않는다.

- 공고: `HAMAN-41306`, 사전 검증된 공식 제목과 source/profile 계약 그대로 사용.
- HWP1개, SHA256 `c8d37ea0142d19f7270c8231cde80028e8e40a3b1a73d02038dca01207a5bb97`.
- 회당 최대6요청·24,117,248byte(23MiB), 전체 캠페인60요청·100,663,296byte(96MiB).
- 기존8요청·6,399,210byte를 유지하므로 이번 최대 포함14요청·30,516,458byte. 재실행 시 원장을 새 모드로 초기화하지 않는다.
- 서울 transient unit CPU1개·메모리768MiB·임시공간1GiB·최대20분. 비root probe와 네트워크 없는 격리 추출기, 고정 TLS/공인IP/URL 검증을 유지한다.
- 운영DB 미사용, 설치JAR 전후 동일성·health 확인, 자기 임시 원본/전송 객체/실행 자원만 정리한다. 정책/worker/운영 데이터/계정/보안그룹은 변경하지 않는다.

Java 관측 검증은 실행 시간·본문AVAILABLE·전체 파일1개·binary 지문·현행 추출기 버전·품질·HWP 구조/부분 사유·완전 추출 시 구간 metadata의 지문 결합을 확인한다. 불완전 텍스트는 `isWholeTextAnalysisComplete=false`/`REVIEW_REQUIRED`가 필요하며 최종 관리자 검증과 기대값 미승인을 유지한다. Python 전송 검증도 파일/범위/예산/부분 결과 승격을 거부한다.

이 관측기의 종합 `decisionStatus`는 기존 첨부 분류 엔진 결과다. `segmentSummary`는 독립 구간 분석 근거이고, 명시 구간 분류 엔진의 worker 저장/DB/API 평가 결합 성공을 대신하지 않는다. 그 검증은 다음 별도 단계로 남긴다.

로컬 `build/qa-tools`의 기존 패키지 생성기·실행기에 함안 모드를 연결했다. 업로드 전에 기존 본문 JUnit·signature 영수증과 누적 한도를 검증하고, 다른 함안 제출 이력이 있으면 자동 반복하지 않는다. 이 로컬 보조 파일은 기존과 같이 Git 추적 대상이 아니므로 다른 환경에서 실행하려면 동일 경계를 확인해야 한다.

## 검증 명령·실패 이력

```powershell
.\gradlew.bat :test --tests '*AnnouncementAttachmentBbsObservationProbeTest' --tests '*AnnouncementAttachmentBbsOfficialObservationContractTest' :attachmentContractQaTest :attachmentBbsObservationProbeJar :bootJar --no-daemon --max-workers=1
node --test scripts/qa/attachment-bbs-observation-probe.test.mjs
```

Python은 로컬 별칭이 Microsoft Store 연결이라 실행되지 않았다. 확인된 Codex bundled Python으로 `-m unittest discover -s scripts/qa -p test_temporary_bbs_observation.py`를 실행하여25건 통과했다. Node5건 통과, PowerShell 두 보조 파일 구문과 실제 기존 영수증을 이용한 누적 예산 사전 검증도 통과했다. 이 검증에는 AWS 호출·패키지 업로드·실파일 재요청이 없다.

첫 Java59건 실행은 합성 `PARTIAL_TEXT` 입력에 필수 부분 사유가 없어1건 실패했다. 필수 사유 검증을 유지하고 합성 입력에 `UNSUPPORTED_CONTROL` 사유를 명시했다. 이 사유는 테스트용이며 함안 실제 HWP의 오류로 보고하지 않는다. 수정 후 재실행 결과는 별도로 기록한다.

AWS 읽기 전용 인증 확인은 `AWS_AUTH_REFRESH_REQUIRED`였다. 임시 CA bundle은 정리했고 사용자에게 갱신을 요청했다. 서울 서버 QA는 시작하지 않았다. 브라우저는 현재 명시 요청 정책상 미실행이다. 전체9Gate=8부분/1차단을 유지한다.

최종 재실행은1분53초에 성공했다. Java59건·패키지20건은 실패/오류/생략0이며 probe JAR 생성도 확인했다. 추출기1.0.11과 운영 본체 코드는 변경하지 않아 bootJar/배포용 QA 본체는 기존 동일 코드 결과 UP-TO-DATE다. 이번에 첨부 모듈 전체 회귀나 새 Linux CI 통과를 주장하지 않는다. 기존 `8760141` Linux36035318540도 마지막 조회 in_progress였다.

추가 외부 공고 요청0·패키지 업로드0·운영 변경0이다. 소유 Node/Java/Python/PostgreSQL 잔여 프로세스0을 확인했다. 인증 갱신 후에는 최신 JAR로 전송 패키지를 생성하고, 최초 함안 실행 여부·이전 영수증·운영 기준선을 재확인한 후 같은 고정 범위로 실행한다.
