# PDF 부분 추출 수치 진단 v1.0.14

## 실제 서울 관측·정리 완료

- 소스 `8e56931e30e71a50959d5d59bf36065eeea9bf21`, 실행 `6aa2931a32f148559969b70b0a367323`, SSM `25104c81-e5d1-43ad-9a59-f92fe299aa2e` Success/exit0,28.434초·1발견/1통과·실패/생략/중단/컨테이너실패0이다. 최초 패키지 업로드 세션70041을 재제출하지 않고 추적했다. 전송 중 읽기 전용 exact-key 조회에서는 당시 완성 객체가 없었고, 이후 기존 세션이 정상 제출됐음을 확인했다.
- 본문 AVAILABLE/127자·전체 첨부2개 발견·다운로드/추출 완료. HWP는 COMPLETE_TEXT/3,120자/178블록, PDF는 PARTIAL_TEXT/4,241자/5블록이다. 두 파일의 binaryHash/textHash 및 품질은 선행1.0.13과 모두 동일하다. HWP 구간은 여전히 기본1.0.0 UNKNOWN3/FORM1이며1.0.4 worker 저장 증거가 아니다.
- PDF 진단: pageCount5, reliablePageCount0, externalObjectInvocationCount1, inlineImageInvocationCount0, blankPageCount0, replacementCharacterCount0. 현재 품질 코드에서 확인된 부분 추출 요인은 Do 호출1회다. 객체 유형(이미지/Form)·실제 시각 정보·장식 여부는 미확인이므로 COMPLETE_TEXT로 완화하지 않는다. 구조 신뢰0페이지는 별도의 자동 문단/표 결합 한계다.
- 본문 ACCEPTED와 달리 종합 REVIEW_REQUIRED/ATTACHMENT_INCOMPLETE·isWholeTextAnalysisComplete=false·requiresFinalAdminVerification=true다. isPolicyQaPassed/isExpectationApproved=false·productionDatabaseUsed=false·productionWriteCount0이다.
- 이번 예약5회/2,439,945byte, 실제 본문 시도1회. 누적 **27/30회·14,051,834/81,788,928예약byte**이며 남은3회로 전체 본문/두파일 실행을 반복할 수 없다. 최대6회 예약의 최종 사용량 정산 근거는 아래 영수증이다. 예약 파일은 삭제하지 않는다.
- 영수증 `build/temporary-bbs-qa-6aa2931a32f148559969b70b0a367323/result-utf8.json`, SHA256 `6b3cb92728f400d65afd1e4484734c927eaf64710e3a42e7e3d4f885f9d8fbfa`. unitInactive/installedJarUnchanged/healthUp/probeCleanupSucceeded/transportTemporaryFilesRemoved/originalFilesRemoved=true다. 소유 S3 객체 삭제·로컬 package.zip 정리 완료, plan/영수증/예약 보존. 운영 객체 삭제0·운영 설치/DB/정책/worker 변경0이다. 사용한 AWS/Java 프로세스는 종료됐고 기존 사용자Java PID35696은 보존했다.
- PDF 진단 소스3053028의 Linux36385233189 completed/success. 보관XML root3078=2784통과/294조건부 생략·실패/오류0, extractor190·패키지20·jobDB209·migration18·정책부모2·runtime5·worker12·Flyway3 실패/오류/생략0이다. 단일 모드 추가 소스8e56931의 Linux36385736473은 현재 실행 중이며 별도다.
- 다음 구현 판단은 실행된 객체 유형/시각 정보 및 신뢰할 수 없는 PDF 구조 확인이다. OCR은 기존 설계의 후속 범위이며 근거 없이 로고로 간주하지 않는다. 공고 정상화·전체 catalog 기대값·구간worker DB/API·정책 게시·운영E2E는 미완료다. 브라우저는 현재 사용자 명시 요청 정책상 미실행이다.

## 목적과 완료 경계

중구33626의 실제 PDF는 extractor1.0.13에서 PARTIAL_TEXT/4,241자/5블록이었다. 당시 영수증에는 PDF 부분 추출 원인 수치가 없으므로 이미지 때문이라고 단정하지 않는다. 이번 변경은 원인을 구분할 수 있는 격리 IPC 및 QA 관측 metadata 추가다. 텍스트 추출·구조 신뢰·부분 품질·관리자 검수 정책을 완화하지 않는다.

전체9Gate=8부분/1차단, catalog40참조/14지역/기대값1/정상0을 유지한다. 외부 공고 추가 요청과 운영 DB/정책/worker/설치 변경은 없다. 브라우저는 현재 사용자 명시 요청 정책상 미실행이다.

## 내부 계약

`ExtractionResult.pdfStructure`에 아래 정수6개를 추가한다. 공개 /api/v1 계약이나 DB schema 변경이 아니므로 migration은 추가하지 않는다. 추출기와 실행 식별 버전은 함께1.0.14로 변경한다.

| 필드 | 의미 / 상한 |
|---|---|
| pageCount | 문서 페이지 수,0~200, 최상위 pageCount와 일치 |
| reliablePageCount | 기존 구조 검증을 통과한 페이지 수,0~pageCount |
| externalObjectInvocationCount | 실제 실행한 Do 호출 수, 이미지 및 Form 포함. 이미지 개수가 아님 |
| inlineImageInvocationCount | 실제 실행한 BI 호출 수 |
| blankPageCount | 기존 페이지 추출 결과가 공백인 페이지 수,0~pageCount |
| replacementCharacterCount | 최종 추출 텍스트 U+FFFD 수,0~1,000,000 |

호출 수 합계는 기존 페이지당200,000연산 상한×페이지 수 이하로 검증한다. 문자열·음수·누락·미지 필드·범위 초과·본문/품질과 모순된 값은 INVALID_PDF_STRUCTURE_DIAGNOSTIC으로 거부한다. 원문·리소스명·파일명·URL을 진단에 넣지 않는다. 반환 수치는 deep copy로 분리한다.

현재1.0.14 PDF IPC에는 진단이 필수다. 구1.0.13 IPC는 선택 필드가 없어도 읽을 수 있다. 필드가 있으면 구버전도 동일하게 검증한다. 암호화·손상 등 실패 결과는 기존 실패 계약을 유지한다.

## 품질 정책 보존

- 빈 텍스트는 OCR_REQUIRED다. 이번 변경은 OCR을 구현하지 않는다.
- Do/BI 실행·빈 페이지·대체문자가 있고 텍스트가 있으면 기존과 동일하게 PARTIAL_TEXT다.
- 등록만 되고 실행되지 않은 이미지 리소스는 호출 수0이며 부분 품질을 새로 유발하지 않는다.
- 문단 구조가 불명확하다는 이유만으로 COMPLETE_TEXT를 PARTIAL_TEXT로 바꾸지 않는다. scopeReliable=false가 자동 구간 결합을 별도로 제한한다.
- QA 관측은 부분 추출에서도 수치 진단을 보존하되 역할/구간 성공 근거를 새로 생성하지 않는다.

## 검증 계획

1. [x] 합성 PDF의 미사용/실행 이미지·인라인 이미지·Form·반복 호출·빈/0페이지·태그 문단 수치를 검증한다.
2. [x] IPC 입력의 필드·범위·품질·신구 호환 및 원문 비노출 회귀를 작성한다.
3. [x] 전체 extractor, 관련 Java 회귀, 패키징, bootJar 결과를 아래 실행 기록으로 확정했다.
4. [x] 진단 구현3053028 Linux 전체 계약 결과를 별도로 확인했다. 단일 QA 모드8e56931의 후속 CI는 실행 중이다.
5. [x] 중구 동일 binaryHash 실파일의1.0.14 수치를 새 서울 실행으로 확인했다. 이전1.0.13 성공을 재사용하지 않았다.

실패 기준은 부분 품질 완화, 합성 시험을 실파일 증거로 표현, 임의 진단 필드 통과, 운영 변경, 누적 요청 예산 초기화다.

## 로컬 검증 결과

```powershell
.\gradlew.bat :attachment-extractor:test :test --tests '*extraction.*' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AnnouncementAttachmentBbsObservationProbeTest' --tests '*AnnouncementAttachmentJungguObservationContractTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*Segment*Test' :attachmentContractQaTest :bootJar --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'
node --test scripts/qa/attachment-contract-release.test.mjs scripts/qa/attachment-official-worker-probe.test.mjs scripts/qa/attachment-bbs-observation-probe.test.mjs
```

- Gradle1분28초 성공. extractor190건·패키징20건 실패/오류/생략0, root288건 중281통과/7조건부 생략/실패·오류0이다. 생략은 Linux runtime5건·격리 추출1건·중구 DB seed1건이며 해당 검증을 Windows 통과로 보고하지 않는다.
- bootJar는 선행1차 검증에서 생성했고 확대 검증은 동일 업무 소스 UP-TO-DATE다. Node35건 중32통과/Windows의 Linux symlink3건 생략/실패0,17.2초다.
- git diff --check 오류0. 단발 Node와 Gradle 시험 프로세스는 종료했고 기존 사용자 Java PID35696 및 output/·scripts/qa/__pycache__/는 보존했다.
- 직전 소스b88a6fd의 Linux36383959813은 completed/success다. 보관 XML root3071=2777통과/294조건부 생략·실패/오류0, extractor188·패키징20·jobDB209·migration18·정책부모2·runtime5·worker12·Flyway3 실패/오류/생략0이다. 이 결과는 이번1.0.14 소스의 Linux 성공으로 대체하지 않는다.

## 후속 실파일 범위

중구 누적22/30요청·11,611,889/81,788,928예약byte를 보존한다. 세 공고 전체 재실행 상한12회는 잔여8회에 들어가지 않는다. 다음 실행은33626 단일 고정 공고·본문과 전체2첨부·최대6요청/23MiB로 좁힌 별도 실행 경로와 기존 영수증 기반 단일 실행 guard를 먼저 검증해야 한다. 이 문서 작성은 재실행을 뜻하지 않는다. worker 저장·구간1.0.4·DB/API와 정상 기대값은 별도 검증 대상이다.

### 단일 실행 경로 구현

- [x] `JUNGGU_PDF` 명시 모드를 Java probe/Bash/Python manifest·결과 검증에 연결했다. 대상은33626 하나이며 전체 HWP/PDF2개와 기존 binaryHash를 고정한다. 기존3공고 모드와 제목 음성 표본을 삭제하거나 변경하지 않는다.
- [x] 합계 상한6요청/24,117,248byte이며 최대 누적28/30회·35,729,137/81,788,928byte다. CI push에서 이 모드를 자동 실행하지 않는다.
- [x] PDF 진단 필드6개·정수·범위·품질 일치를 원문 없는 전송에서도 확인한다. 단일 결과 대신3건을 제출하거나 일부 파일 누락·다른 identity·잘못된 지문·허위 전체완료는 실패한다. 진단 관측을 구간 worker 저장/DB/API 또는 정상 기대값 승인으로 표현하지 않는다.
- [x] 전송 helper는 직전 실행c2ca9c52f36342e6afd7818ac199e9e7의 최종 영수증 SHA256·서버/S3 정리·누적 예약·다른 미확인 실행 부재를 검사한다. 별도 CreateNew 예약으로 중복 제출을 막고 CheckOnly 검사는 파일을 생성하지 않는다. 오프라인 정상1/거부8=9건 통과·외부 호출0·plan변경0이다.
- [x] 단일 모드 추가 후 Java 확대168건 중167통과/Linux seed1생략·실패/오류0, 패키지20통과·bootJar 및 probe JAR 검증1분5초 성공이다. Node8/Python30 통과와 PowerShell4개 구문 오류0, git diff --check 오류0이다.
- [x] 실행 전 로컬 package135파일/88,402,823byte 생성, archiveHash `ec4373bc739ebf28659ca68155edc933411d5e26563b10ef6d76a1e726585869`, codeHash `a15049147cd15f81eb45d07615e5b20c87c3785659b850d41641f39c19c8d549`. plan은 `build/temporary-bbs-qa-6aa2931a32f148559969b70b0a367323/plan.json`이다. 이 시점 서버 전송/공고 재요청은 아직0이다.
- [x] 실제 서울 실행과 정리 결과는 문서 최상단의 새 영수증으로 확인했다. AWS 읽기 전용 inventory에서 서울/root 대상 일치·Ubuntu1대/SSM Online·기존운영revision9a1bb45를 재확인했다.
