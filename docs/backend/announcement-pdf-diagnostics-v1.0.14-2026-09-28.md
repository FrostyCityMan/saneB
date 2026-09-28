# PDF 부분 추출 수치 진단 v1.0.14

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
4. [ ] 동일 소스 Linux 전체 계약 결과를 별도로 확인한다.
5. [ ] 중구 동일 binaryHash 실파일의1.0.14 수치를 확인한다. 이전1.0.13 성공을 재사용하지 않는다.

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
