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
2. OKCHEON-193297 PARTIAL_TEXT 원인을 특정한다. 현재 추출기 코드에서 그림·OLE·수식 및 추출 텍스트의 대체 문자(U+FFFD)는 부분 추출 표식을 남기지만 실제 파일의 해당 요소 종류/개수는 이번 보고서로 확인되지 않았다. 장식 그림이라고 가정하여 품질을 승격하지 않는다.
3. PDF의 전체 텍스트와 문단/표 구조 신뢰도를 분리한다. 합성 tagged PDF 시험 성공을 BOEUN-218812의 구조 확정으로 간주하지 않는다.
4. 진단 근거에 따라 최소 개선→회귀→승인 상한 내 재관측→독립 검토한 기대값 고정→동일 버전 Provider QA를 수행한다.
5. 이후 최신 worker/임시 DB/API, 관리자 최종 검수/DRAFT와 운영 업무 E2E를 이어간다. 운영 정책·기존 데이터는 별도 범위 승인 경계를 지킨다.

네트워크 접근/옥천 승인 대기는 이번 두 실행의 blocker에서 해소됐다. 남은 핵심은 파일 수집 가능 여부가 아니라 문서 구조 판정의 근거 품질과 최신 업무 경로 검증이다.

## 후속 1.0.5 실파일 원인 진단

이 절은 앞의 1.0.4 관측과 별개다. production 코드 기준 `b4fa280`, 감축 예산 probe/실행기 기준 `ff930d5737a390571272cfa1919cb9e04b943a02`이며 공통 executionCodeHash는 `5f76392be3eb0cfcd2c5db52a61f6d6d7af0c7b0b777d2fbab859ef2114115b3`이다. 기존 고정 공고는 그대로이며 각 공고20요청/32MiB, 지역60요청/96MiB로 낮췄다. 선행 영수증·정리 상태를 재조회했고 지역별 진단 중복 실행을 차단한다.

### 옥천 재관측

- executionId `698cde56f7274380ba8a97716ff25ad4`, SSM `bc38b132-0111-481e-8086-2896f52ffe35`: Success/0, 47.375초, 3/3·실패/생략0.
- 패키지138파일/188,201,489byte, SHA-256 `d1dc175b0d046b600584e51d1cf07139ab2ca02fe2d63b6ff9984481fac10177`.
- 요청 예약8회/5,630,612byte, 첨부469,652byte. 선행을 합친 누적16회/11,261,224byte로 승인132회/251,658,240byte 이내다. 예약 byte를 실제 총 트래픽으로 표현하지 않는다.
- 193369: section1/paragraph414, 그림·OLE·수식·대체 문자 모두0. COMPLETE_TEXT 6,221자/312block은 그대로다. 구간9개 중 선행1개는 초기 표제 없음, NOTICE 후보1개는 NOTICE_HEADING+TARGET_SECTION 이후 SUPPORT_SECTION을 충족하지 못했다. FORM 후보7개 중6개는 APPLICANT_FIELD, 1개는 SIGNATURE_FIELD를 첫 부족 조건으로 갖는다. 필수 조건 순서 중 앞에서 멈춘 결과이므로 뒤쪽 조건의 원문 부재를 뜻하지 않는다.
- 193297: section1/paragraph4,377, **그림2·수식1·OLE0·대체 문자0**. PARTIAL_TEXT 48,760자/3,117block의 원인을 수치로 확인했다. 그림 의미·수식 내용의 완전성을 확인한 것이 아니므로 장식으로 간주하거나 COMPLETE_TEXT로 승격하지 않는다. OCR 후속 범위와 문서 구조 인식 개선을 구분한다.
- 193187: 제목 조합 미충족 유지, 본문/첨부 요청0회. 음성 사례를 분모에서 빼지 않는다.
- 양성2건의 본문·binary·text hash는 1.0.4 관측과 일치한다. 193369의 segmentAnalysisHash도 같다. 진단 추가로 판정/텍스트를 조용히 변경하지 않았음을 확인했으며 부분 파일에 구간 분석 성공 자료는 없다.
- 실제 CPU quota/period100000/100000·memory805306368·tmp1073741824, unitInactive/installedJarUnchanged/healthUp/probeCleanupSucceeded/transportTemporaryFilesRemoved 모두true. 운영 DB 미사용·정책 QA 미통과다.
- S3 자기 객체 삭제/부재와 plan.cleaned=true 확인 후 로컬 package.zip을 정확한 경로 검증으로 제거했다. `build/temporary-bbs-qa-698cde56f7274380ba8a97716ff25ad4/{plan,result}.json`을 보존했다.

### 보은 재관측

- executionId `7aaaee228eda4381a56f18f6bc23f273`, SSM `4cf323b6-28b1-4ee1-8304-04ff4c2ae8ed`: Success/0, 56.429초, 3/3·실패/생략0.
- 패키지138파일/188,201,487byte, SHA-256 `5ba922ab5b23ee197b697ba2bc8d461ccd1d59bb98fe2cc9ab46c4606e08675c`.
- 요청 예약12회/7,391,673byte, 첨부412,089byte. 선행을 합친 누적24회/14,783,346byte로 승인132회/251,658,240byte 이내다.
- 221499: HWPX section1/paragraph481, 그림·OLE·수식·대체 문자 모두0. COMPLETE_TEXT 8,129자/397block, 7구간 중 UNKNOWN5/FORM2 유지다. GUIDE 후보는 GUIDE_HEADING 이후 TARGET_SECTION이 첫 부족 조건이다. 미완성 FORM 후보2개는 APPLICANT_FIELD, 1개는 SIGNATURE_FIELD에서 멈췄다. 선행1구간은 초기 표제가 없다.
- 221497: HWPX section1/paragraph437, 그림·OLE·수식·대체 문자 모두0. COMPLETE_TEXT 7,821자/378block, 5구간 중 UNKNOWN4/FORM1 유지다. GUIDE 후보는 TARGET_SECTION, FORM 후보 각1개는 APPLICANT_FIELD·SIGNATURE_FIELD가 첫 부족 조건이다. 선행1구간은 초기 표제가 없다.
- 218812: PDF COMPLETE_TEXT 5,566자/6block, UNKNOWN1/STRUCTURE_UNCERTAIN 유지다. HWPX 전용 수치가 없는 것은 정상이다. 추출 텍스트 존재만으로 PDF 문단·표의 읽기 순서/경계를 확정하지 않는다.
- 세 공고의 본문·binary·text·segmentAnalysis hash가 모두 1.0.4 관측과 같다. 누락 조건 진단을 추가했지만 규칙 또는 품질을 완화하지 않았다.
- 실제 CPU quota/period100000/100000·memory805306368·tmp1073741824, unitInactive/installedJarUnchanged/healthUp/probeCleanupSucceeded/transportTemporaryFilesRemoved 모두true. 운영 DB 미사용·정책 QA 미통과다.
- S3 자기 객체 삭제/부재와 plan.cleaned=true 확인 후 정확한 소유 경로의 로컬 package.zip을 제거했다. `build/temporary-bbs-qa-7aaaee228eda4381a56f18f6bc23f273/{plan,result}.json`은 보존했다. 패키지는 코드에서 재생성할 수 있다.

### 후속 결론과 검증 경계

- 두 지역 재관측으로 본문·첨부 확보와 원인 진단은 재확인했다. 양성5건은 여전히 REVIEW_REQUIRED이며 정상 후보 증가나 검수량 감소를 입증하지 못했다.
- 다음 개선 대상은 HWPX의 실제 표제/지원 조건/양식 필드 구조다. 현재 metadata는 첫 미충족 규칙만 특정하므로 원문 표현·구간 경계 오류를 단정하지 않는다. 동일 파일을 무작정 재다운로드하거나 실제 근거 없이 정규식을 넓히지 않는다.
- 옥천193297의 그림2/수식1은 현재 범위에서 부분 추출을 유지한다. OCR 실행은 기존 설계의 후속 범위이며 모든 격리 QA 승인만으로 OCR 기능/외부 서비스를 추가하지 않는다.
- 새 버전 최신 worker→업무 DB/API→관리자 최종 검수/DRAFT 및 전체 Provider 정상 표본·검토된 기대값은 여전히 별도 잔여다. 수집 성공과 운영 업무 E2E를 구분한다.
- 로컬 감축 예산 probe24/24·패키지20/20·Python17/17·Node/Bash4/4 통과. 실제 seed 계약6건은 Windows CreateProcess4551로 실패했으며 성공으로 치환하지 않았다. 추출기1.0.5의 Linux35978778828은 실행 중, 감축 예산 ff930d5의 Linux35979527273은 대기 중인 시점의 기록이며 최종 성공은 별도 확인한다.

### 추출기1.0.5 Linux 최종 회귀 확인

- 후속 조회에서 `b4fa280481881d538f04aeafa05d8a32c578c47c`의 [Linux35978778828](https://github.com/FrostyCityMan/saneB/actions/runs/35978778828) 최종 success와 artifact XML을 확인했다. root2,884건=2,609통과/275조건부 생략/실패·오류0이다.
- extractor122·패키지20·job PostgreSQL196·migration18·정책 부모 PostgreSQL2·runtime5·worker PostgreSQL12·Flyway3건은 각각 실패·오류·생략0이다. XML은 `build/qa-results/linux-35978778828/`에 보존했다.
- 이 결과는 새 추출기 production 코드의 Linux 계약 회귀다. 후속 감축 예산 `ff930d5`의 Linux35979527273은 실행 중이며 해당 SHA의 전체 성공으로 확대하지 않는다. Windows 실행 정책 차단 사실, 실제 공고의 검수 유지 및 업무 E2E 잔여도 그대로다.

## 최신 구간 엔진 보은 worker·임시 DB·API 실파일 검증

관측 전용 시험과 별개로 `100ccb29b87520b4f02bd9633eb0938c9e1dd97e`의 BOEUN_SEGMENT를 실행했다. 운영 설치물을 바꾸지 않고 최신 임시 QA/추출기1.0.5와 `attachment-segment-1.0.0`/`segment-role-1.0.0`을 사용했다. application executionCodeHash는 `5f76392be3eb0cfcd2c5db52a61f6d6d7af0c7b0b777d2fbab859ef2114115b3`로 기존 진단과 동일하며, 새 검증 코드는 별도 probe JAR 지문과 패키지 manifest에 결합된다.

- executionId `0d283126b08f488693ab2a831e416e3d`, SSM `cc2f0970-2626-4d7c-910b-59d017d287fd`: Success/0, JUnit3/3·실패/생략/중단/컨테이너 실패0, 68.701초.
- 패키지138파일/188,201,454byte, archive SHA-256 `94e7a2073f852a1a2b9898b178eadad6100ed5d0f8e5a43a3f1ef83976c88150`.
- 제목 고정 입력→실제 본문→전체 첨부→worker EVALUATED/job SUCCEEDED→임시 PostgreSQL→MockMvc API를 검사했다. 임시 DB에서만 실행 fixture를 사용하며 운영 정책 승인 증거는 아니다.

| 공고 | 실제 첨부 | 구간 / UNKNOWN | DB·API 결합 | 최종 상태 |
|---|---|---:|---|---|
| BOEUN-221499 | HWPX / COMPLETE_TEXT / 8,129자 | 7 / 5 | 분석 재현·평가 입력 FK·API 투영 일치 | FINAL_REVIEW_EXCEPTION / REVIEW_REQUIRED |
| BOEUN-221497 | HWPX / COMPLETE_TEXT / 7,821자 | 5 / 4 | 분석 재현·평가 입력 FK·API 투영 일치 | FINAL_REVIEW_EXCEPTION / REVIEW_REQUIRED |
| BOEUN-218812 | PDF / COMPLETE_TEXT / 5,566자 | 1 / 1 | 분석 재현·평가 입력 FK·API 투영 일치 | FINAL_REVIEW_EXCEPTION / REVIEW_REQUIRED |

- 세 파일 모두 이전 관측의 binary/text/segmentAnalysis hash와 일치했다. 기존 파일 전체 역할 UNKNOWN은 유지했고 구간 근거가 실제 평가 입력에 연결됨을 확인했다.
- GET은 분석을 새로 만들지 않았고 다른 source 조회는404다. 참고/미확인 구간을 긍정 근거로 승격하지 않으며 모든 파일 입력, 확정0·공고 link0, 원본 정리·resource lease0을 확인했다.
- 본문 판정3건은 ACCEPTED였으나 첨부 미확정 때문에 최종 REVIEW_REQUIRED였다. 본문 성공을 최종 후보 준비 완료로 표현하지 않는다. 실제 검수량 감소는 여전히 미입증이다.
- 요청 예약12회/7,391,673byte, 같은 날 선행 두 관측을 포함한 누적36회/22,175,019byte로 승인132회/240MiB 이내다. 실제 첨부412,089byte와 본문 상한을 포함한 예약량은 다르다.
- CPU quota/period100000/100000·memory805306368·tmp1073741824, unitInactive/installedJarUnchanged/healthUp/probeCleanupSucceeded/transportTemporaryFilesRemoved 모두true다. 운영 DB 미사용·productionWriteCount0·정책 QA/기대값 승인/인증 브라우저 E2E=false다.
- 근거는 `build/temporary-bbs-qa-0d283126b08f488693ab2a831e416e3d/{plan,result}.json`이다. 이 시험은 상시 목록 수집·관리자 최종 확정/DRAFT·기존 데이터 배치·운영 브라우저 E2E를 대신하지 않는다.
- terminal 확인 후 S3 자기 전송 객체 삭제/부재 및 plan.cleaned=true를 확인했다. 검증한 자기 경로의 로컬 package.zip을 제거했고 JSON은 보존했다. 패키지는 코드에서 재생성 가능하다. 로컬 Java/PostgreSQL 잔여0·단기 Node 종료, 사용자 `output/` 보존을 확인했다.

후속 예산 변경 `ff930d5`의 [Linux35979527273](https://github.com/FrostyCityMan/saneB/actions/runs/35979527273)도 최종 success 및 XML을 확인했다. root2886=2611통과/275조건부 생략/실패·오류0, extractor122·패키지20·jobPG196·migration18·정책부모PG2·runtime5·workerPG12·Flyway3은 실패·오류·생략0이다. XML은 `build/qa-results/linux-35979527273/`에 보존했다. 새 `100ccb2`의 [Linux35981889914](https://github.com/FrostyCityMan/saneB/actions/runs/35981889914)는 별도 대기 상태이며 선행 성공으로 대체하지 않는다.

## 검수 수정 코드의 보은 실파일 review-context 검증

- production 코드 `bfa7c2d5654726c13fde3e6bb5824fd77f78d24d`, executionCodeHash `95146282e8cfb03e735bd3bb43f4459207dc4f218e792f617063ce9dbf99245c`와 새 probeHash `2a041092c5022db86872343b7ff46d9e93e8d2fb8421689ac7b6898d0179a4d6`로 실행했다. 새 probe는 Service/GET review-context 대조·no-store·다른 source404·반복 GET 버전 불변·확인/link0을 검사한다.
- executionId `56fd6b7de295458b9c48190f3baa2e21`, SSM `ab78764e-9d11-4508-85cb-44011bb535de`: Success/0, 실파일3/3·실패/생략/중단0, 75.662초다. 패키지138파일/188,207,246byte, archive hash `ae40a7b308ffa1614a2ca5e5a375ec8f15c5b0d17b717b36494ba3baff323d77`.
- 보은221499(HWPX),221497(HWPX),218812(PDF) 모두 segmentReviewContextVerified=true·manualSourceCheckRequired=true다. 이전 관측과 binary/text/segmentAnalysis 지문이 모두 일치하며 UNKNOWN 5/4/1 구간의 원문 확인 요구를 유지했다. 정상 후보나 최종 관리자 확인으로 승격하지 않았다.
- 전송 전 선행3회 SSM 영수증·정리 완료를 확인하고 36요청/22,175,019byte를 차감했다. 새 상한60요청/96MiB도 승인132요청/240MiB 이내이며, 실제 사용12요청/7,391,673byte를 합친 누적은48요청/29,566,692byte다. 이전과 다른 코드/probe 및 단 한 번의 신규 재검증만 허용했다.
- unitInactive·installedJarUnchanged·healthUp·probeCleanupSucceeded·transportTemporaryFilesRemoved 모두true, CPU1/768MiB/tmp1GiB를 확인했다. 운영 DB·정책·설치 변경0, 원본/lease 잔여0이며 인증·브라우저 E2E=false다.
- S3 소유 전송 객체 삭제/부재 및 plan.cleaned=true를 확인했다. 검증한 소유 경로의 로컬 package.zip도 제거했으며 코드에서 재생성 가능하다. `build/temporary-bbs-qa-56fd6b7de295458b9c48190f3baa2e21/{plan,result}.json`은 보존했다.
- 로컬 Java85·Node/Bash7·Python18·패키지20은 실패/생략0이다. 표적29초·패키지/bootJar22초 성공이며 production 변경이 없어 bootJar는 선행 산출물 UP-TO-DATE다. Python 기본 alias는 실행되지 않아 번들 Python으로 실제 재실행했다. Windows 전체 회귀11건의 initdb 차단은 별도 기록으로 유지한다.
- 이 검증은 실제 파일과 검수 조회의 연결 증거다. 정상 혼합 문서의 검수량 감소, 실사용자 최종 확인/DRAFT, 전체 지역·기존 데이터·운영 브라우저 E2E 완료를 대신하지 않는다.

## 후속 괄호형 조건 표제 후보 규칙 대조 — 개선 효과 미확인

### 제한된 원본 구조 진단

- 알려진 HWPX binary hash를 대조하여 보은221499/221497의 공개 XML을 메모리에서만 검사했다. 같은 HTTPS 공식 호스트·redirect 없음·기본 TLS·응답1MiB·ZIP100항목/확장8MiB·section1·DTD/외부 entity 금지로 제한했다. 원문 파일과 텍스트를 저장하지 않았으며 보고에는 사전 정의된 표제/길이/문장부호 형태만 포함했다.
- 두 파일에서 `❍(신청자격)`, `❍(지원내용)`, `❍(신청기간)`을 확인했다. 닫는 괄호 유무 추가 확인까지 2회·총8요청, 보수적 예약 상한8MiB다. 두 번째는 결과 출력/자원 정리는 완료됐으나 호출 shell에서 PowerShell 스크립트 이후의 `$LASTEXITCODE`를 잘못 판정하여 최종 wrapper 오류가 발생했다. 이를 정상 종료로 기록하지 않으며 동일 자료를 다시 받지 않았다.
- 이 진단은 설치 추출기 결과가 아니다. XML 표제 확인만으로 실제 구간의 필수 조건 충족을 단정할 수 없다는 것이 아래 대조에서 확인됐다.

### 동일 실파일·추출 결과의 구/신 규칙 대조

- 후보 `segment-role-1.0.1`, rulesHash `9bba150694efbdaa10b853f492b748f643521f8a7c82b9e8d8bd28f86ed7d9e3`는 명시 호출에만 존재한다. 기본/worker/정책은1.0.0 그대로이며 후보 결과는 저장·적용하지 않는다.
- executionId `3fdb2e7468864e0bab68142a1fdcb3eb`, SSM `88f5ece8-dfc1-46f6-afe5-83bb6cbb1c11`: Success/0, 3/3·실패/생략/중단0, 71.775초다. 패키지138파일/188,209,380byte, archive SHA256 `d4ccc0a67ad03f65f8da9820b75281d59e2994a7a2196d21622e3743384d6aa1`, executionCodeHash `fa2a1c1f0506293d10cb6aaf976104cfe019df402e9b5f734f34f73253421e9a`다.
- worker DB/API·검수 조회는 기존1.0.0 규칙으로 모두 대조됐다. 후보1.0.1은 같은 실제 text/blocks/구간 경계를 검증한 메모리 분석이다. 두 경로의 성공 의미를 혼동하지 않는다.

| 고정 공고 | 구간 수 | UNKNOWN 기존→후보 | 후보 분석 hash | 실제 결과 |
|---|---:|---:|---|---|
| BOEUN-221499 | 7 | 5→5 | `6a7cabbb9232333adc022ba4075e3d45cfb0cf9be6c181c6df45c36bbbd4ad03` | GUIDE 후보는 여전히 GUIDE_HEADING만 근거로 보유 |
| BOEUN-221497 | 5 | 4→4 | `452243ef7239546fff5e4fb29a1d4af5cd94c3f6ef428fd5c56a0b9082d3692f` | GUIDE 후보는 여전히 GUIDE_HEADING만 근거로 보유 |
| BOEUN-218812 | 1 | 1→1 | `5de4445ff80a7f6388f5da07cca2d181fd856d6ed3d837f4f2f5f65319fcff0e` | PDF STRUCTURE_UNCERTAIN 유지 |

- 원문/binary/text/기존 분석 지문은 선행 관측과 같다. 모든 공고는 FINAL_REVIEW_EXCEPTION/REVIEW_REQUIRED·원문 확인 요구를 유지했다. 합성 GUIDE+FORM 테스트는 통과하지만 **이번 실파일에서 검수량 감소는 0**이다. 괄호 표제만 추가하면 해결된다는 가설은 채택하지 않는다.
- 다음 진단은 설치 추출문에서 해당 표제의 위치·block 경계·구간 소속·규칙 일치 여부다. 현재 보고서로 세부 원인을 확정할 수 없다. 새 규칙을 worker/정책에 연결하거나 UNKNOWN/부분 추출을 완화하지 않는다.
- 앞선4개 SSM 영수증과 정리 완료를 재조회하고 수동 XML 확인8요청/8MiB를 포함하여 시작 예산56요청/37,955,300byte를 차감했다. 이번12요청/7,391,673byte를 합친 누적은 **68요청/45,346,973byte**다. 승인132요청/251,658,240byte 이내이며 예약byte를 실제 총 트래픽으로 표현하지 않는다. 같은 실행의 재전송은 차단한다.
- unitInactive·installedJarUnchanged·healthUp·probeCleanupSucceeded·transportTemporaryFilesRemoved 모두true, CPU1/768MiB/tmp1GiB다. 운영 DB/정책/설치 변경0, 원본/lease 잔여0이다. S3 자기 객체 부재와 plan.cleaned=true 확인 후 검증한 로컬 package.zip만 제거했다. JSON은 `build/temporary-bbs-qa-3fdb2e7468864e0bab68142a1fdcb3eb/{plan,result}.json`에 보존했다. 패키지는 코드에서 재생성 가능하다.
- 전체 Provider QA·새 규칙 DB/API 적용·실사용자 최종 확인/DRAFT·정책 게시·기존 데이터·브라우저 E2E는 이 대조의 완료 범위가 아니다.

## 후속 구간 위치 진단 — 주표제 인식 누락 확인

- 1.0.1의 production codeHash `fa2a1c1f0506293d10cb6aaf976104cfe019df402e9b5f734f34f73253421e9a`를 그대로 사용하고 probeHash `b50f795d95f43e7b2e8ba412bde923fb54fb1bc9187cbe191877519d5c2ef1f8`에 표제 위치 진단만 추가했다.
- executionId `d6682645ef9d4c34b65faba75b0bd485`, SSM `57af67c3-c83d-4c9d-968c-81d6a4e92fc1`: Success/0, 3/3·실패/생략/중단0, 72.753초다. 패키지138파일/188,210,943byte, archive SHA256 `cb24c9e92411be5850f0f905ac01149e8055d4ac288a809988eab94c5c375829`다.
- 221499 신청기간/신청자격/지원내용의 code point 시작 위치526/570/1108, 원본block13/14/24는 모두0번 구간이다. 221497의 대응 위치395/439/756, block12/13/19도0번 구간이다. 모두 신뢰 가능한 단일 block에 있고1.0.1 전체 줄 문법에 일치했다. 각1번 안내 구간에는 뒤쪽 신청기간만 있었다.
- 따라서 이 표제들의 정규식 불일치나 block 분리가 아니라, 앞쪽 주표제가 경계로 인식되지 않은 것이 NOTICE 구간을 만들지 못한 직접 원인이다. 안내 구간으로 앞선 조건을 복사하는 방식은 적용하지 않는다. 신청서 필드 부족/PDF 불확실성은 이 원인과 별개다.
- 공개 XML의 표제 띄어쓰기 확인1회 및 주표제 형태 확인1회는 각각 고정2파일·4요청/4MiB 보수적 상한으로 제한했다. 두 파일 모두 알려진 binary hash와 일치했다. 후자는 원문 문장이 아닌 미리 정한 업무 단어와 표제 끝 형식만 출력했다. 221499 paragraph5,221497 paragraph4에서 `모집 공고(3분기)` 형식을 확인했다. XML 관측은 설치 추출 결과로 표현하지 않는다.
- 진단 실행 후 누적84요청/56,932,950예약byte, 뒤이은 주표제 XML 확인 후 **88요청/61,127,254예약byte**다. 승인132요청/240MiB 안이며 같은 작업의 재전송은 차단한다.
- unitInactive·installedJarUnchanged·healthUp·probeCleanupSucceeded·transportTemporaryFilesRemoved=true, CPU1/768MiB/tmp1GiB, 운영 DB/정책/설치 변경0을 확인했다. S3 자기 객체 부재와 cleaned=true 이후 검증한 로컬 package.zip만 제거했다. plan/result JSON은 `build/temporary-bbs-qa-d6682645ef9d4c34b65faba75b0bd485/`에 보존했다.
- 위치 진단 표적52/52·패키지20/20·Node10/10 통과이며32초 빌드 성공이다. 이후 후보1.0.2 구현/실파일 대조 결과와 구분한다.

## 후속1.0.2 분기 공고 표제 대조 — 두 NOTICE 구간 확보

- executionId `41fc277ff44d49289f6f1066e67baf9c`, SSM `946fe091-c139-43df-8d73-2083e592d37f`: Success/0, 3/3·실패/생략/중단0, 71.630초다. 패키지138파일/188,212,263byte, archive SHA256 `bfae203e433324a33ed0da692800ddc85edfbd357a2c2cbb0f545c9be89d5585`, executionCodeHash `56dcabc7b85abe3e1a71224a772f8821d2f8bd09192e36c8e161c40180913e2b`다.
- 후보1.0.2 rulesHash `2f02f48368ce3f42557dd62094dec8e6b99d44e27d0f51f265a2fd737aabdd82`. 같은 실제 추출 입력과 전체 범위/evidence 좌표를 재검증했다. 후보는 저장·적용false이며 worker DB/API·검수 조회는 기존1.0.0이다.
- BOEUN-221499: 기존7구간에서 후보8구간으로 분리되고 NOTICE1개가 생겼다. 역할 순서는 UNKNOWN/NOTICE/UNKNOWN/UNKNOWN/UNKNOWN/FORM/UNKNOWN/FORM이다. NOTICE는 표제/대상/지원/기간의4개 근거를 갖는다. 후보 analysisHash `7b5436c08db86b5ff9fe0648b1d8c8eed01e29cb996c35ee53cba26ab3b583e2`.
- BOEUN-221497: 기존5구간에서 후보6구간, UNKNOWN/NOTICE/UNKNOWN/FORM/UNKNOWN/UNKNOWN이며 NOTICE에4개 근거가 있다. 후보 analysisHash `381f7db64f91cda2e44c5f3ee2a4d2ed5e89ecaf11a0225b6bf8bff7f9e71c7e`.
- BOEUN-218812: PDF UNKNOWN1개 유지, NOTICE0이다. 후보 analysisHash `b87ff442be093bfc05c95739a5a48b70623c1bb1256174a5b8cbb084bebbbc7d`.
- 두 HWPX의 앞쪽 공고 범위를 정상 역할로 식별한 실제 진척이다. 그러나 UNKNOWN 개수는5/4/1로 그대로이고 모든 후보·공고의 상태는 REVIEW_REQUIRED다. 서문/일부 FORM·내부 GUIDE/PDF 구조 문제를 지우지 않았다. 정상 후보 증가·사람의 검수 시간 감소·최종 확인/DRAFT 성공을 주장하지 않는다.
- 실행 전 선행6개 SSM 영수증·정리와 수동 구조4회(16요청/16MiB 상한)를 합산해88요청/61,127,254byte를 확인했다. 새 상한42요청/72MiB는 승인132요청/240MiB 이내다. 실제 예약12요청/7,391,673byte를 합친 **누적100요청/68,518,927byte**이며 잔여32요청/183,139,313byte다. 이는 예약 상한 집계이며 전체 실제 트래픽 측정값이 아니다.
- unitInactive·installedJarUnchanged·healthUp·probeCleanupSucceeded·transportTemporaryFilesRemoved=true, CPU1/768MiB/tmp1GiB, 원본/lease 잔여0·운영 DB/정책/설치 변경0을 확인했다. S3 자기 객체 삭제/부재·plan.cleaned=true 이후 검증한 로컬 package.zip을 제거했다. JSON 근거는 `build/temporary-bbs-qa-41fc277ff44d49289f6f1066e67baf9c/`에 보존했다.
- 다음 과제는 남은 미확정 서문/양식/내부 안내 구조를 근거로 해소하고, 검증된 새 규칙을 고정 정책/worker/DB/API 계약에 연결하는 것이다. 기존 버전 재현성·A/B·최종 관리자 확인·자동 활성화 금지는 유지한다. 전체 Provider/실운영 E2E 완료는 아니다.

## 후속1.0.2 실제 worker 저장·평가·버전별 조회 검증

- production 코드는 `e3ead92a90003dca267950e71744884e0ef449be`이며 executionCodeHash는 `e37456b8b256327ae0409bd7e44a0de3b4d2290b762329ba58000b61edf66236`이다. 이번 probe는 같은 고정3공고의 임시 정책/실행 snapshot을 명시1.0.2에 고정한다. 일반 정책 생성/수동 SHADOW 분석 기본값과 운영 설치물은 바꾸지 않는다.
- executionId `dc11d4e01ef3475aa64c5197a08f875f`, SSM `87d32cd1-0a36-4ea1-bb8e-3f22290187e7`: Success/0, 3/3·실패/생략/중단0, 70.279초다. 패키지138파일/188,216,029byte, archive SHA256 `f549bea6206562a3a9cb5ee97f488ae6f00600f7cd95e996c78be6b8e2373385`, probeHash `2d68eeab7196a545fddd75a18399d4f83f86c6779c70f2879500426c3b8d0dce`다.
- 기존 후보 메모리 대조와 달리 이번에는 실제 worker가1.0.2 분석을 임시 DB에 저장하고 평가 입력 FK에 결합했다. API `analysisVersion=segment-role-1.0.2` 응답 전체가 저장 분석/실제 추출 재분석과 일치하며, 위 직전 관측의 각1.0.2 analysisHash와도 일치한다.
- 기본 GET은 아직 저장하지 않은1.0.0의 NOT_ANALYZED를 반환한다. 새 버전 fallback/분석 생성이 없고 반복 GET에 따른 분석 수 증가0·잘못된 source404·no-store를 확인했다. 최종 검수 컨텍스트의 서비스/GET 투영과 조회 무변경도 일치했다. MockMvc 검증이지 인증된 운영 브라우저 E2E는 아니다.

| 공고 | 형식 | 저장 구간 수 | NOTICE | UNKNOWN | 종합 판정 |
|---|---|---:|---:|---:|---|
| BOEUN-221499 | HWPX | 8 | 1 | 5 | REVIEW_REQUIRED / ATTACHMENT_CONTEXT_REVIEW |
| BOEUN-221497 | HWPX | 6 | 1 | 4 | REVIEW_REQUIRED / ATTACHMENT_CONTEXT_REVIEW |
| BOEUN-218812 | PDF | 1 | 0 | 1 | REVIEW_REQUIRED / ATTACHMENT_CONTEXT_REVIEW |

- 세 공고 모두 파일 발견/처리1개, COMPLETE_TEXT, EVALUATED/SUCCEEDED다. 파일 전체 역할 UNKNOWN을 임의 변경하지 않고 FORM/REFERENCE/UNKNOWN의 CONTEXT_ONLY·원문 확인 요구·최종 관리자 검증을 유지했다. 확정/공고 link0이며 정상 후보 증가·검수 시간 감소를 주장하지 않는다.
- 실행 전 선행7개 SSM 영수증·정리와 수동 구조4회 상한을 다시 검증해 누적100요청/68,518,927byte를 확인했다. 잔여32요청보다 작은 총30요청/72MiB(공고별10요청/24MiB)로 실행했다. 실제 예약12요청/7,391,673byte를 더한 **누적112요청/75,910,600예약byte**, 승인132요청/240MiB 대비 잔여20요청/175,747,640byte다. 같은 작업 재전송은 차단하며 후속 실행은 더 낮은 한도와 선행 영수증 재검증이 필요하다.
- unitInactive·installedJarUnchanged·healthUp·probeCleanupSucceeded·transportTemporaryFilesRemoved=true, CPU1/768MiB/tmp1GiB, 원본/lease 잔여0을 확인했다. S3 자기 객체 부재·plan.cleaned=true 확인 후 지문/경로가 일치한 로컬 package.zip도 제거했다. 이 패키지는 재생성 가능하며 `{plan,result}.json`은 해당 build 실행 디렉터리에 보존했다. 운영 DB/정책/설치 변경0이다.
- 실행기 Java24·패키지20·Python18·Node7 및 PowerShell 구문/git diff 검사가 통과했다. production 신규 PG2건을 포함한 전체 Linux 회귀는 별도35992333497 실행으로 판정한다. 이 실파일3건 성공으로 전체 Provider/정책 게시 QA/기존 데이터/운영 브라우저 Gate를 대체하지 않는다.

## 후속1.0.3 내부 절·중복 제목 후보 대조

- 공개 XML 추가 확인1회에서 고정 HWPX2개의 기존 binary hash를 대조했다. 본문/파일4요청, 응답당1MiB로 제한했으며 원본을 저장하지 않았다. `3. 신청안내`가 내부 절이고,221497의 동의서 제목2개가 인접하며 공백만 다름을 업무 단어/문자 모양/위치로 확인했다. 서문과 서식 필수 구조 부족은 별개로 유지한다.
- 이 추가4요청/4MiB와 선행8회 서버 영수증을 반영한 시작 예산은116요청/80,104,904예약byte다. 동일한3공고를 각5요청/24MiB, 합계15요청/72MiB로 제한했다. 원격 영수증·정리 완료와 다른 코드/probe 지문을 다시 확인한 후 한 번 실행했다.
- executionId `5b81a5c5ee0d437fb4d368b7ba68aef9`, SSM `f3c8fcd9-5cd6-494e-956d-624edddc8abc`: Success/0,3/3·실패/생략/중단0,78.371초다. 패키지138파일/188,227,665byte, archive SHA256 `dd97a4f8f046800b4c502d7a9f80cdfc6b67e362dcdfead226c082700e989dd0`, codeHash `c844054b29bf61488eba6710f53bd2375c3dbe6ad0416281b008e5f11a0d85ed`, probeHash `a81c723e34bdfdae1ffc691dc32fef6ac89e8a7efe5ccb20a77de588eb56afe6`다.
- 기존1.0.2 worker/임시 DB/API/검수 조회와 저장 분석 지문은 그대로 일치했다. 후보1.0.3은 같은 추출 text/blocks의 메모리 분석이며 persistedOrApplied=false다. rulesHash는 `8b9fdd872f3eb9890146d6e360408204ff285f4b23977e07693834aceec66d43`이다.

| 공고 | UNKNOWN 기존→후보 | 후보 역할 | 후보 analysisHash |
|---|---:|---|---|
| BOEUN-221499 | 5→3 | UNKNOWN / NOTICE / UNKNOWN / UNKNOWN / FORM / FORM | `22f9d58ac8c6a8c76fe3508e58bf367358453b88684af5710437a5133e687827` |
| BOEUN-221497 | 4→2 | UNKNOWN / NOTICE / FORM / UNKNOWN | `3594adc138acae4211de819c3803295704410a8e8484a2070323042f9f3bda3a` |
| BOEUN-218812 | 1→1 | UNKNOWN | `f823ab78281f55cb16c634bde50bbc6fc84175c1058c90355c1845ea50aa3673` |

- 두 HWPX에서 불필요하게 나뉜 구간이 각각2개 줄었다. 모두 여전히 REVIEW_REQUIRED이며 정상 후보 증가나 검수 시간 감소를 실측한 것은 아니다. 남은 사유는 HWPX 서문 INITIAL_HEADING_REQUIRED·일부 역할 ROLE_STRUCTURE_INCOMPLETE와 PDF STRUCTURE_UNCERTAIN이다. 후보를 운영 가능 버전으로 승격하지 않았다.
- 이번 예약12요청/7,391,673byte를 더한 누적은 **128요청/87,496,577예약byte**, 승인132요청/240MiB 대비 **잔여4요청/164,161,663byte**다. 현재15요청짜리 전체3공고 QA를 다시 실행할 수 없다. 추가 범위 승인 없이 예산을 초기화하거나 같은 실행을 재전송하지 않는다.
- unitInactive·installedJarUnchanged·healthUp·probeCleanupSucceeded·transportTemporaryFilesRemoved=true, CPU1/768MiB/tmp1GiB, 각 원본 제거·lease0을 확인했다. S3 자기 객체 삭제/부재와 plan.cleaned=true를 확인하고 hash/절대 경로가 일치하는 로컬 package.zip만 제거했다. 패키지는 코드에서 재생성 가능하며 `build/temporary-bbs-qa-5b81a5c5ee0d437fb4d368b7ba68aef9/{plan,result}.json`은 보존했다.
- AWS CLI 경유 영수증의 정적 한국어 사전 key 일부가 문자 인코딩 손상으로 보인다. 그 key로 새 의미 판단을 하지 않았다. 이번 비교의 근거인 ASCII 버전/hash/role/reason/개수/결합·정리 boolean은 별도 구조 조회로 확인했다. 로그 전체를 원문이나 운영 성공 근거로 재사용하지 않는다.

## 후속1.0.3 실제 저장 검증 준비 — 당시 서버 미실행

- `BOEUN_STRUCTURAL`을 별도 명시 모드로 추가했다. 기존 `BOEUN` 파일 엔진과 `BOEUN_SEGMENT`의1.0.2 저장/3 후보 대조 경로를 보존한다. 새 모드도 고정3공고, 공고당5요청/24MiB·전체15요청/72MiB다. 이 한도는 코드상 최대치일 뿐 누적 승인 잔여를 갱신하지 않는다.
- `attachmentBoeunStructuralWorkerIntegrationTest`와 별도 보고서 경로를 만들었다. CI에서는 이를 자동 실행하지 않는다. 서울에서는 최신 임시 QA 패키지를 사용해야 하며 운영 설치 QA나 운영 DB를 사용하지 않는다. 기존 Windows PG 실행 차단을 우회하지 않는다.
- 새 실행은 이전 `5b81a5c5` 영수증의3개1.0.3 analysisHash를 고정 입력으로 대조한다. 실제 worker가 정책 snapshot의1.0.3 버전/hash로 저장하고, 평가 입력 FK·버전 GET·판정 결합 GET의 전체 응답이 일치해야 한다. 기본0과 명시2의 GET은 NOT_ANALYZED이며 조회 후 분석 수가 증가하면 실패한다.
- 성공 보고서는 구간 수6/4/1, UNKNOWN3/2/1, NOTICE1/1/0과 REVIEW_REQUIRED·수동 원문 확인 요구를 유지해야 한다. 저장 검증을 후보 메모리 자료로 대신하지 못하도록 `structuralCandidate`를 거부한다. 실제 검증 후에만 `structuralObservedHashMatched`, `evaluationBoundApiVerified`, `otherVersionReadOnlyVerified`를 기록한다. raw text/locator를 보고서에 새로 추가하지 않는다.
- 로컬 표적 Java26·QA 패키지20·Node/Bash8·Python19건 실패/생략0, probe JAR/bootJar 검증33초 성공이다. Python 회귀는128+15가 기존132요청을 초과함을 명시한다. 추가30요청 승인 답변·누적 원장 재검증·서울 전송 실행기 연결 전에는 새 서버 실행을 하지 않는다. 아직 실제1.0.3 저장/API·정리 결과는 없다.

## 후속 격리 QA 일괄 승인 적용 — 2026-09-24

- 사용자의 최신 `모든 격리 qa 요청 승인`을 위에서 요청한 보은 추가30요청에 적용한다. 기존132회에30회를 더한 누적162회이며 기존 사용128회/87,496,577예약byte를 초기화하지 않는다. 지역 bytes 한도240MiB는 증액하지 않는다.
- 이번 실행은 `BOEUN_STRUCTURAL`의 같은3공고1회, 최대15요청/72MiB/20분, CPU1개·메모리768MiB·임시공간1GiB다. 운영 설치·DB·정책·worker 설정은 변경하지 않고 임시 원본/DB/패키지를 정리한다. 추가 실행은 사용량과 미완료 자원을 다시 확인해야 하며 이 기록만으로 무제한 재실행하지 않는다.
- 전송 실행기는 선행9회 영수증·고정 case·정리 완료를 실시간 검증하고 수동 구조20요청 상한을 합산한다. 후보1.0.3 메모리 분석을 새 worker 저장 근거로 재사용하지 않는다. 버전/분석 hash·평가 FK·두 GET 투영·조회 무생성·미확정 검수 유지가 성공 기준이다.
- 로컬 표적/패키지/probe/bootJar 준비 명령은22초 성공했다. production 코드·bootJar·QA 패키지는 변경 없는 UP-TO-DATE이며 새 운영 배포의 증거가 아니다. Node8·Python22건 통과 및 로컬 실행기2파일 PowerShell 구문0오류를 확인했다. 서버 결과는 후속 영수증으로 별도 기록한다.

### 명시1.0.3 실제 worker 저장 결과

- executionId `24283053367d4b44be39750b2bd2d65e`, SSM `347a254f-e8a2-4829-aea7-6ba869571b4e`: Success/0,75.058초·3/3·실패/생략/중단/컨테이너 실패0이다. source code 기준선은 `c477c6552979ca6c09ac0c8746caf13abf62770b`, 추출기1.0.7이다. 이번에는 production Java/Flyway/API를 수정하지 않았다.
- 패키지138파일/188,238,871byte, archive SHA256 `5f0e217367260f8d4b2ada248c7210f4beb5aa97b8b083c263c044d60668d624`, codeHash `cf0f5142efdbffec88d0f824fd266c19702922c90c28419ce9fdb93d191e6981`, probeHash `090f9fd77826b8519fd2a26989171811c4af17065fb3f91ee8fa205cf0c0b5bf`다.

| 고정 공고 | 형식/문자 수 | 실제 저장 구간/UNKNOWN/NOTICE | 판정 |
|---|---|---|---|
| BOEUN-221499 | HWPX/8,129 | 6/3/1 | REVIEW_REQUIRED, 원문 확인 필요 |
| BOEUN-221497 | HWPX/7,821 | 4/2/1 | REVIEW_REQUIRED, 원문 확인 필요 |
| BOEUN-218812 | PDF/5,566 | 1/1/0 | REVIEW_REQUIRED, 구조 불확실6블록 |

- 본문3건 AVAILABLE·제목 조합 충족·공고별 공식 첨부1개 발견/다운로드/처리,3파일 COMPLETE_TEXT를 확인했다. 각각의 binary/text/1.0.3 analysisHash는 선행 고정값과 일치한다. 텍스트 완전성과 역할 확정은 다르며 정상 후보0·기대값 승인0·정책QA 통과false·최종 관리자 확인 필요true다.
- `segmentDatabaseApiVerified`, `segmentReviewContextVerified`, 평가 입력 FK/버전 GET/판정 결합 GET의 전체 응답 일치가3건 모두 true다. 기본0과 명시2 조회는 NOT_ANALYZED·분석 무생성을 검증했다. 이 결과는 신규1.0.3 worker 저장 근거이며 후보 메모리 대조와 구분한다. 운영 DB·정책은 사용하지 않았고 운영 쓰기0이다.
- 선행9회 SSM·정리 및 수동 구조20요청 예약을 재대조했다. 이번12요청/7,391,673예약byte를 더해 누적 **140/162요청·94,888,250/251,658,240예약byte**, 잔여22요청/156,769,990byte다. 이전 BOEUN 설치 검증은 다른 이전 범위의 이력이며 이9월24일 구간 관측 원장을 전체 보은 역사 요청량이라고 표현하지 않는다.
- 원격 unit 비활성·원본/임시 DB/lease/전송 경로 정리, 운영 JAR 불변·health UP을 확인했다. S3 자기 객체 부재와 plan.cleaned=true 이후 정확한 절대경로/hash를 확인하여 로컬 package.zip도 제거했다. 재생성 가능한 패키지와 임시 원본만 삭제했으며 `build/temporary-bbs-qa-24283053367d4b44be39750b2bd2d65e/{plan,result}.json` 영수증을 보존했다.
- Node8/Python22 및 이번 로컬 Java26 실패/생략0이다. QA 패키지20·bootJar는 변경 없는 UP-TO-DATE를 재실행으로 계산하지 않는다. 선행614850d [Linux36012304604](https://github.com/FrostyCityMan/saneB/actions/runs/36012304604) success와 다운로드 XML의 root3013=2727통과/286조건부 생략/실패·오류0을 확인했다. 별도 extractor133·패키지20·jobPG206·migration18·정책부모PG2·runtime5·workerPG12·Flyway3은 실패/오류/생략0이다. 현재 c477c65 CI는 마지막 조회 실행 중이며 이 결과로 대체하지 않는다.
- 장기 goal 스킬에 따라 실제 저장/조회 근거와 미확정 판정을 분리했다. 전체9Gate=8부분/1차단은 유지한다. 남은 업무는 문서 미확정/부분 추출·전체 Provider 정답, 운영 정책/기존 데이터/동일SHA 배포·실사용자 검수/DRAFT·브라우저 E2E다. 브라우저는 현재 명시 요청 정책에 따라 미실행이며 이번 격리 QA 승인으로 운영 활성화 권한을 확대하지 않는다.

## 긴 신청인·복수 서명란 후보 대조 준비

- 후속 공개 XML 관측4요청/4MiB 예약을 추가했다. 고정 HWPX2파일의 binary hash 일치, 원본 메모리 처리·미저장·클라이언트 종료를 확인했다. 이 시점 누적144/162요청·99,082,554/251,658,240예약byte, 잔여18회다.
- 221497은 같은 표 셀 문단의 신청인/복수 서명란187자가 확인됐다. 기존 신청인 값160자·서명 앞부분120자 규칙이 이를 받아들이지 못할 가능성이 있어 전체 줄200자 한도 안에서만 명시 후보1.0.4를 추가한다. 다른 서문/표/지원 조건을 임의로 합치지 않는다.
- 다음 BOEUN_STRUCTURAL1회 상한은 같은3공고15요청/72MiB/20분이다. 실제 저장/판정은1.0.3으로 유지하고 같은 추출문을 후보1.0.4와 메모리에서만 비교한다. `longFormCandidate`는 버전/hash·역할/사유·같은 입력/전체 범위/경계 검증·persistedOrApplied=false만 보고한다. 기존 저장 결과/상태/확인/link는 바꾸지 않는다.
- 전송 전에10개 선행 영수증과 수동 구조24요청을 합산하여144회 사용을 재검증한다. 다른 모드로 예산을 초기화하지 않으며 같은 plan의 재전송을 차단한다. 실제 개선 여부는 실행 결과 전까지 미확인이다.

### 후보1.0.4 실파일 결과 — 2026-09-25 00:04 KST

- executionId `4b3409d6de39411689693af41184453c`, SSM `251f933d-10c6-4a70-8af2-3669af5c35bb`: Success/0,73.631초·3/3·실패/생략/중단0이다. 추출기1.0.7, 실제 저장/판정은1.0.3, 후보 메모리 대조만1.0.4다. 세 파일의 binary/text/저장1.0.3 analysisHash는 직전24283053 실행과 각각 일치했다.
- codeHash `3711db9ffe7b69f0f0711a300b43da52b55e48821edb6dd2f11c1e3525824ba2`, probeHash `fa752e6858d0b7ee21e20dcbde8fac8e8f3271c398d813f60d25f5a768691bba`, archive SHA256 `35e1ff5ce750c2fc305bfd00fa0a72fa6d06b079cd9588487986fa71eb774240`,188,241,339byte다.
- 후보 rulesHash는 `27dfa69f39bea3c47e1bf40b20bf01471f2432bc08ee143355e4e849089406fe`이며 모든 결과의 입력/전체 범위/경계 불변 검증true·persistedOrApplied=false다.

| 공고 | 저장1.0.3 UNKNOWN → 후보1.0.4 UNKNOWN | 후보 역할 순서 | 후보 analysisHash |
|---|---|---|---|
| 221499 | 3 → 3 | UNKNOWN/NOTICE/UNKNOWN/UNKNOWN/FORM/FORM | `d458819cb7789b14d6214e7b8dc365b6bd78a2420599d1ece3330c4e14efb3cc` |
| 221497 | 2 → 1 | UNKNOWN/NOTICE/FORM/FORM | `9ba9e2ea3391599cb34de6b3dd8eeb394ef3a35d23954f52e16a6ac2061d1d9f` |
| 218812 | 1 → 1 | UNKNOWN | `0d67340e2320e1f3f787c4b73c2336b5657e19d8d991ce2823c33052cf0fb223` |

- 221497 동의서1구간의 긴 필드 제한을 해소했다. 서문 UNKNOWN·다른 HWPX의2미확정 구간·PDF 구조 불확실성은 보존한다. 세 공고의 후보 상태도 REVIEW_REQUIRED다. 정상 후보/운영 적용/실제 검수 시간 감소로 확대 해석하지 않는다. 다음은 후보의 명시 worker/정책/조회 계약 연결 및 남은 문서 구조 검토다.
- 이번12요청/7,391,673예약byte를 합산하여 누적 **156/162요청·106,474,227/251,658,240예약byte**, 잔여6회다. 현재 전체3건 최대15회 모드는 잔여 내 재실행 불가다. 같은 요청 재전송/예산 초기화 없이 다음 실행 범위를 다시 고정해야 한다.
- 원격 unit/원본/임시 DB/lease/전송 경로 정리, 운영 JAR 불변·health UP, S3 자기 객체 삭제/부재와 plan.cleaned=true를 확인했다. 정확한 절대경로/hash 검증 후 재생성 가능한 자기 로컬 ZIP을 제거했다. `build/temporary-bbs-qa-4b3409d6de39411689693af41184453c/{plan,result}.json`은 보존한다. 운영 쓰기·정책 게시·ENFORCE·기존 데이터·배포는0이다.
- 로컬 첫 표적/bootJar38초 성공, 확대 명령 `:test --tests 'com.saneb.domain.announcementattachment.*' --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' :attachmentContractQaTest :attachmentOfficialWorkerProbeJar :bootJar --no-daemon --max-workers=1`은3분36초 성공했다. root1999=1977통과/22조건부 생략·패키지20건 실패/오류0, Node8·Python22·PowerShell 구문0오류·diff 검사 통과다. 확대 명령의 bootJar는 첫 표적에서 생성한 동일 코드 결과 UP-TO-DATE다.
- 선행 c477c65 [Linux36014177794](https://github.com/FrostyCityMan/saneB/actions/runs/36014177794) success와 다운로드 XML root3016=2730통과/286생략,별도133/20/206/18/2/5/12/3 실패/오류/생략0을 확인했다. 새 후보 코드의 전체 Linux 증거로 대체하지 않는다. 현재 브라우저 검증은 사용자 명시 요청 정책상 미실행이다.
