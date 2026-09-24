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
