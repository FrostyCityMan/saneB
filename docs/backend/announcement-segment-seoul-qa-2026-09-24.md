# 보은·옥천 구간 분석 서울 임시 격리 QA — 2026-09-24

## 결론과 완료 경계

- [x] 사용자의 모든 격리 QA 승인에 따라 기존 보류한 옥천을 포함하여 고정 6공고를 순차 관측했다. 서버 관측은 보은 3/3, 옥천 3/3 통과·실패/생략 0이다.
- [x] 실제 양성 5공고의 본문과 첨부 5파일을 확보했다. 추출기는 4파일을 COMPLETE_TEXT, 1파일을 PARTIAL_TEXT로 반환했다. COMPLETE_TEXT는 추출기 판정이며 사람이 원문 전체와 대조한 정확도 보증이 아니다.
- [x] 제목 조합 미충족 1공고는 본문·첨부 요청 0회였다.
- [~] COMPLETE_TEXT 4파일도 UNKNOWN 구간이 남았다. 양성 5공고 모두 REVIEW_REQUIRED이며 정상 후보·정책 QA 통과·검수량 감소를 입증하지 못했다.
- [ ] 이번 경로는 제목 고정 표본→실제 본문/첨부→추출/구간 관측과 임시 DB DRAFT seed 검증이다. 최신 worker의 업무 DB 저장/API/관리자 최종 검수/DRAFT 전환 또는 상시 목록 수집 E2E를 검증한 것이 아니다.
- 운영 정책 게시·ENFORCE·기존 데이터 적용·운영 설치/DB 변경·배포를 하지 않았다. 브라우저 검증도 현재 요청 정책상 미실행이다. 전체 Gate 8부분/1차단 및 ATT 1완료/61부분, SEG 확장 분모를 유지한다.

## 실행 기준선

- QA 코드: `6900e3cfafddd9ea65b859995a98218a3db61fb9`, 브랜치 `codex/attachment-three-stage-linux-qa`.
- 임시 패키지에 추출기 1.0.4를 포함했다. 기존 BOEUN worker 모드의 운영 설치 추출기를 새 버전 증거로 재사용하지 않고 BOEUN_OBSERVATION 모드를 별도로 사용했다.
- 공통 executionCodeHash: `d696bd26b747bf59bb2057e87aa346d068c289ae4f855908ec28fa6e75b055ca`.
- 구간 엔진 `segment-role-1.0.0`, 규칙 hash `fb807a5fcf11c102badcc35cc4b60c6abe7fa36672e2aa431e3b5f2dc16bcdde`.
- 운영 기준선은 별도다: CodeDeploy `d-NCB2HF3YK` Succeeded, SHA `9a1bb4569bcc3c13bf3bc30b51021b9149c67054`, JAR SHA-256 `12985f8cd4d710f24da85d2f82c9556d719264aef5f43ac1a57239687bc9c2bc`.
- 사전 SSM `9d3df389-9f19-4966-9fcc-c0bf80cdb827` Success. 서울 대상 1대/SSM Online, 가용 메모리 929MiB, cgroup v2 및 격리 도구를 확인했다. 운영 DB 버전/데이터를 새로 조회하지 않았다.

## 실제 파일 결과

| 고정 공고 | 본문 문자 | 첨부 / 추출 문자 / block | 품질 | 구간 분석 및 결과 |
|---|---:|---|---|---|
| BOEUN-221499 | 630 | HWPX / 8,129 / 397 | COMPLETE_TEXT | 7구간: UNKNOWN 5, FORM 2. 초기 제목 부족 1·역할 구조 부족 4. 검수 유지 |
| BOEUN-221497 | 454 | HWPX / 7,821 / 378 | COMPLETE_TEXT | 5구간: UNKNOWN 4, FORM 1. 초기 제목 부족 1·역할 구조 부족 3. 검수 유지 |
| BOEUN-218812 | 373 | PDF / 5,566 / 6 | COMPLETE_TEXT | UNKNOWN 1, STRUCTURE_UNCERTAIN. 텍스트 추출과 신뢰할 문단/표 구조 확보를 구분 |
| OKCHEON-193369 | 425 | HWPX / 6,221 / 312 | COMPLETE_TEXT | UNKNOWN 9. 초기 제목 부족 1·역할 구조 부족 8. 검수 유지 |
| OKCHEON-193297 | 402 | HWPX / 48,760 / 3,117 | PARTIAL_TEXT | 전체 텍스트 완료 false. 정상 구간 분석 성공으로 계산하지 않음 |
| OKCHEON-193187 | 미요청 | 미요청 | 해당 없음 | TITLE_NOT_ELIGIBLE_NOT_FETCHED. 제목 조합 미충족, 후속 요청 0회 |

전송 보고서는 모든 파일·역할 순서/개수·사유 개수·전체 분석 hash를 보존하는 `METADATA_SUMMARY_FULL_ANALYSIS_HASH`다. 원문·다운로드 주소·구간 좌표 전체 배열은 복제하지 않았다. `segmentAnalysisHash`는 요약 JSON이 아닌 전송 전 전체 분석의 지문이다. 관측값으로 catalog 기대값을 자동 생성하거나 승인하지 않았다. 표의 UNKNOWN 개수는 사람 검수 작업 건수와 같지 않다.

## 자원 상한·실측·정리

각 지역 승인 범위는 132요청·240MiB·20분, CPU 1개·메모리 768MiB·임시 공간 1GiB다. 실제 cgroup 값 CPU quota/period 100000/100000, memoryMax 805306368, temporarySpaceMax 1073741824를 확인했다.

| 항목 | 보은 | 옥천 |
|---|---|---|
| executionId | `93bb0f9ee3ca4a73bbf1612152acbc2c` | `8b16f4142b644f47b7cc171a9402096d` |
| SSM commandId | `7da510bd-a189-4227-a93e-d9f73987d37e` | `8069ff06-2ea0-49bd-b35f-baa7af063771` |
| 종료 상태 | Success / 0 | Success / 0 |
| probe 경과 시간 | 59.033초 | 50.436초 |
| 요청 예약 사용량 | 12 | 8 |
| byte 예약 사용량 | 7,391,673 | 5,630,612 |
| 실제 첨부 byte 합계 | 412,089 | 469,652 |
| 패키지 파일 수 / 크기 | 138 / 188,197,324 byte | 138 / 188,197,308 byte |

byte 예약은 본문 상한을 포함하며 실제 전체 네트워크 전송량이 아니다. 경과 시간은 서버 probe이며 로컬 패키징·S3 업로드를 포함하지 않는다. 추가 관측 시 승인된 누적 상한에서 사용량을 차감해야 한다.

- 보은 archive SHA-256: `0430664013baedd97ff3ace80610eb12ca9aaa4dacd6cf15fcf246bf80569767`.
- 옥천 archive SHA-256: `8d6e3c317ae6da3faf885043776edbad35ac2e87343cf54203a876dbbd2679ec`.
- 두 실행 모두 unitInactive, installedJarUnchanged, healthUp, probeCleanupSucceeded, transportTemporaryFilesRemoved=true다. `productionDatabaseUsed=false`, 공고별 `productionWriteCount=0`이다.
- terminal 확인 후 소유권 metadata/크기를 대조하여 각 S3 전송 객체를 삭제하고 부재를 확인했다. 두 plan의 cleaned=true다.
- 검증한 자기 임시 경로의 로컬 package.zip 2개만 제거했다. 코드에서 재생성할 수 있다. 임시 원본은 정리했고 plan/result JSON 및 CI XML은 보존했다. 사용자 `output/`은 변경하지 않았다.
- 로컬 증거: `build/temporary-bbs-qa-93bb0f9ee3ca4a73bbf1612152acbc2c/{plan,result}.json`, `build/temporary-bbs-qa-8b16f4142b644f47b7cc171a9402096d/{plan,result}.json`. 이 경로는 Git 추적 증거가 아니므로 이 문서에 비식별 결과와 지문을 함께 기록한다.

## 코드 검증

- Java 관측/probe 표적 31/31, 패키지 계약 20/20, Python 실행기 16/16, Node/Bash 4/4 통과. PowerShell 구문 검사·`git diff --check` 통과. `:bootJar` 성공/UP-TO-DATE 구분은 진행 기록을 따른다.
- 동일 SHA [Linux CI 35975514461](https://github.com/FrostyCityMan/saneB/actions/runs/35975514461) 최종 success 및 다운로드 XML을 확인했다. root 2,875건 = 통과 2,600·조건부 생략 275·실패/오류 0.
- extractor 114, 패키지 20, job PostgreSQL 196, migration 18, 정책 부모 PostgreSQL 2, runtime 5, worker PostgreSQL 12, Flyway 3건은 각각 실패/오류/생략 0이다. XML은 `build/qa-results/linux-35975514461/`에 보존했다.
- CI의 외부 공식 공고 opt-in 단계는 생략됐다. 위 서울 실파일 관측과 CI를 서로 대체하는 증거로 쓰지 않는다.
- 선행 관리자 UI `a6842cb`의 CI 35972307990, 관측 metadata `b517e9f`의 CI 35972899436도 최종 success를 확인했다. UI 브라우저 통과를 의미하지 않는다.

## 남은 업무와 우선순위

1. HWPX 구간별 누락 조건을 비식별 구조 metadata로 특정한다. 현재 결과는 역할 구조 부족만 알려주므로 어떤 제목/조건 변형이 원인인지는 아직 미확인이다. 실제 근거 없이 정규식을 넓히거나 UNKNOWN을 승인으로 바꾸지 않는다.
2. OKCHEON-193297 PARTIAL_TEXT 원인을 특정한다. 현재 추출기 코드에서 그림·OLE·수식은 부분 추출 표식을 남기지만 실제 파일의 해당 요소 종류/개수는 이번 보고서로 확인되지 않았다. 장식 그림이라고 가정하여 품질을 승격하지 않는다.
3. PDF의 전체 텍스트와 문단/표 구조 신뢰도를 분리한다. 합성 tagged PDF 시험 성공을 BOEUN-218812의 구조 확정으로 간주하지 않는다.
4. 진단 근거에 따라 최소 개선→회귀→승인 상한 내 재관측→독립 검토한 기대값 고정→동일 버전 Provider QA를 수행한다.
5. 이후 최신 worker/임시 DB/API, 관리자 최종 검수/DRAFT와 운영 업무 E2E를 이어간다. 운영 정책·기존 데이터는 별도 범위 승인 경계를 지킨다.

네트워크 접근/옥천 승인 대기는 이번 두 실행의 blocker에서 해소됐다. 남은 핵심은 파일 수집 가능 여부가 아니라 문서 구조 판정의 근거 품질과 최신 업무 경로 검증이다.
