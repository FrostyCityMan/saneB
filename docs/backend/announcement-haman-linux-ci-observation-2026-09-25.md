# 함안41306 GitHub Linux 격리 관측 — 2026-09-25

## 목표·승인·환경 구분

모든 격리 QA 승인 범위에서 함안41306 고정1공고의 실제 HWP 텍스트·구간 근거를 확보한다. AWS 로그인 갱신 대기로 서울 임시 QA는 미실행이다. 기존 GitHub Linux QA 실행 경로를 사용하며 새 서버·운영 설정·DB·자격증명을 변경하지 않는다. GitHub 결과를 서울 서버 또는 운영 적용 증거로 간주하지 않는다.

전체9Gate=8부분/1차단,7엔진/19프로필·37참조/13기관·정상 기대값0을 유지한다. [함안 관측 계약](announcement-haman-isolated-observation-2026-09-25.md)의 공고/제목/source/profile/HWP binary hash·제목→본문→전체 첨부·최종관리자 검증 경계를 그대로 사용한다.

## 실행 계획과 원장

- [x] 기존 `attachmentBbsOfficialFileObservation`의 `HAMAN` 고정1공고를 사용한다. 새 크롤러/다운로더는 추가하지 않는다.
- [x] 일반 push/수동 재실행에는 외부 요청하지 않도록 정확한 실행 표식과 `run_attempt == 1`을 요구한다.
- [x] 같은 서버 보고 계약을 재사용하는 함안 판정기와 시각/JUnit 누락 검증을 추가했다.
- [~] 로컬 검증 후 QA 브랜치의 명시된 한 커밋을 통해 실행하고 실제 run ID를 기록한다.
- [ ] 실파일/JUnit/지문/품질/구간/정리 영수증을 읽고 누적 실제 예약량을 대조한다.
- [ ] 명시 구간 엔진의 worker/DB/API 평가 결합·검토된 기대값·추가 적격 표본2건·운영 업무 E2E.

기존 사용8요청·6,399,210byte + 이번 최대6요청·24,117,248byte를 **총14요청·30,516,458byte 예약 상한**으로 관리한다. 전체 캠페인은60요청·100,663,296byte이며 이전 사용량을 초기화하지 않는다. 아직 실행하지 않은6요청은 사용 완료가 아니라 예약이다. 응답이 없거나 실패했어도 영수증 확인 없이 예약을 해제하지 않는다.

로컬 `build/qa-results/haman-ci-reservation.json`에 최초 실행 예약을 보존한다. 기존 서울 제출 도구는 이 예약이 있는 동안 함안 실행을 차단한다. 이후 GitHub 결과를 확인하고 누적량을 반영하기 전 별도 서울 실행으로 중복 소비하지 않는다. 워크플로를 재실행하면 실파일 단계는 생략되며, 다른 커밋에 같은 표식을 복사해 원장을 초기화하지 않는다.

GitHub 환경은 기존 `ubuntu-22.04` hosted runner다. 관측 step 최대10분, Gradle 단일 worker·시험 JVM 단일 fork/384MiB를 사용하며 파일 추출은 기존 네트워크 없는 bubblewrap/prlimit 격리다. 이것은 서울 transient unit의 CPU1/메모리768MiB/임시1GiB 경계와 다른 실행 환경이며 동일 환경 검증으로 보고하지 않는다. 기존 전체 CI의 DB 회귀도 계속 수행하지만 그 성공만으로 실파일 성공을 대체하지 않는다.

## 성공·실패 기준과 결과 구분

성공: 현 실행 범위의 타임스탬프, JUnit1건/실패·오류·생략0, 고정 공고·전체 파일1개·HWP 지문·완전한 본문/첨부 발견·예산·원본 정리가 일치한다. 실제 파일 품질과 구간 metadata를 별도로 기록한다.

실패: 다른 공고/지문, 누락·중복 파일, 오래된 영수증, JUnit 생략·실패, 초과 예산, 원본 미정리, 부분 품질을 완전성/ACCEPTED로 승격하는 경우다. `PARTIAL_TEXT` 관측이 통과해도 정상 후보/정책 QA 통과를 의미하지 않는다. 게시/ENFORCE/기존 데이터 적용은 실행하지 않는다.

`validate-haman-observation.py`는 기존 서버 실행기의 같은 `validate_probe_scope` 계약만 메모리에서 불러온다. 서버 main/download 함수는 실행하지 않는다. 영수증의 원문 없는 metadata와 JUnit을 읽으며 외부 요청·재분류·수정은 없다. 관측 시각은 현재 실행 시작/종료 사이여야 한다.

관측기의 `decisionStatus`는 기존 파일 엔진 결과이고 구간 summary는 독립 분석 근거다. 보은에 구현된 명시 구간 버전별 worker/DB/API 시험을 함안 성공으로 재사용하지 않는다. 실제 함안 품질과 구간 근거를 확보한 뒤 해당 고정 버전의 저장/조회 검증을 연결한다.

## 로컬 검증

```powershell
node --test scripts/qa/attachment-bbs-observation-probe.test.mjs
```

확인된 bundled Python으로 `-B -m unittest discover -s scripts/qa -p test_haman_observation_report.py`와 기존 `test_temporary_bbs_observation.py`를 실행한다. 합성 입력은 실제 함안 텍스트를 대신하지 않는다. Java/추출기 구현은 변경하지 않아 앞선387af57 표적59/패키지20 근거를 유지하되 새 Linux 실행 결과는 별도로 확인한다. 브라우저는 현재 명시 요청 정책상 미실행이다.

최종 로컬 검증은 Node6·신규 Python4·기존 Python25 모두 실패/생략0이다. PowerShell 실행기의 예약 차단 분기를 실제 로컬 예약 파일로 검사하여 `HAMAN_CI_RESERVATION_REVIEW_REQUIRED`를 확인했다. 서버나 외부 공고를 호출한 검증이 아니다. 별도 YAML parser는 설치돼 있지 않아 실행하지 못했으며 워크플로 표식/명령/최초 실행 제한은 Node 정적 계약으로 검사했다. GitHub 실행 생성과 단계 실행은 별도로 확인한다.

## 선행 Linux 계약 증거

SHA `8760141e05f07d9d938b1bdee28987b55e6268f6`의 [Linux36035318540](https://github.com/FrostyCityMan/saneB/actions/runs/36035318540)는 completed/success다. `attachment-contract-qa-8760141e05f07d9d938b1bdee28987b55e6268f6` artifact를 `build/qa-results/linux-36035318540`에 내려받고 JUnit XML을 직접 합산했다.

- root3,047=통과2,754/조건부 생략293/실패·오류0.
- 추출기149·패키지20·job DB209·migration18·정책 부모 DB2·runtime5·worker DB12·Flyway3: 실패/오류/생략0.

이 증거는 선행 SHA의 코드/DB/독립 격리 계약이다. 이번 함안 실제 파일 추출, 신규 워크플로 변경, 서울 서버 또는 운영 QA의 성공으로 재사용하지 않는다.
