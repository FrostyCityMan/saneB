# HWP 각주·미주 문단 지원 1.0.12

## 목표와 근거

함안41306 서울 실제 관측에서 HWP1개4,644자/213블록과 미지원 record3/control4를 확인했다. 이 수치만으로 각 사유와 컨트롤을 일대일 대응시키지는 않는다. [실측 영수증과 누적 예산](announcement-haman-seoul-observation-2026-09-28.md)은 그대로 보존한다.

각주·미주도 지원 조건을 담을 수 있으므로 버리거나 본문과 무조건 합치지 않는다. 문단 목록 구조와 앵커를 검증한 뒤 각 문단을 독립적인 근거 블록으로 추출한다. 최종 관리자 검증·제목→본문→전체 첨부 순서·자동 활성화 금지·부분 추출 검수 정책은 유지한다.

근거는 [한컴 HWP5.0 revision1.3](https://cdn.hancom.com/link/docs/%ED%95%9C%EA%B8%80%EB%AC%B8%EC%84%9C%ED%8C%8C%EC%9D%BC%ED%98%95%EC%8B%9D_5.0_revision1.3.pdf)의 제어 문자 표6, 문단 목록 표65, 각주·미주4.3.10.4와 고정 revision의 공개 읽기/쓰기 구현이다. 명세의 문단수 길이와 실제 공개 구현이 다르므로 이번 지원 형태를 아래와 같이 명시한다.

- [각주 읽기](https://github.com/neolord0/hwplib/blob/6746c27f17ebf5277493206284aa32044a1839c4/src/main/java/kr/dogfoot/hwplib/reader/bodytext/paragraph/control/ForControlFootnote.java), [미주 읽기](https://github.com/neolord0/hwplib/blob/6746c27f17ebf5277493206284aa32044a1839c4/src/main/java/kr/dogfoot/hwplib/reader/bodytext/paragraph/control/ForControlEndnote.java).
- [각주 쓰기](https://github.com/neolord0/hwplib/blob/6746c27f17ebf5277493206284aa32044a1839c4/src/main/java/kr/dogfoot/hwplib/writer/bodytext/paragraph/control/ForControlFootnote.java), [목록 헤더 쓰기](https://github.com/neolord0/hwplib/blob/6746c27f17ebf5277493206284aa32044a1839c4/src/main/java/kr/dogfoot/hwplib/writer/bodytext/paragraph/control/endnote/ForListHeaderForFootnodeEndnote.java).

## 지원 계약

- 각주/미주의 ID와 제어문자17 앵커를 연결한다. 반복 ID는 기존 표처럼 문단 내 순서대로 연결하며 누락/잘못된 앵커는 검수로 남긴다.
- 컨트롤 헤더는 ID 포함16 또는20byte, 알려진 번호 모양만 허용한다. 목록은 INT32 문단수·UINT32 속성·예약0 8byte의16byte 형태다. 가로쓰기와 알려진 줄바꿈/정렬값만 지원하며 세로쓰기·알 수 없는 확장은 추정하지 않는다.
- 목록은 문단 이전에 정확히1개, 컨트롤 바로 아래 깊이에 있어야 한다. 문단 수1~20,000, 실제 개수, 문단 헤더22/24byte, UTF-16 단위 수, 마지막 문단 플래그를 확인한다. 기존 전체 노드/깊이/텍스트 예산을 유지한다.
- 본문 앞→각주/미주 문단→본문 뒤를 별도 블록으로 유지한다. 위치는 `SectionN:footnote:M:paragraph:P` 또는 `endnote`이며 문단 사이 대상/지원 AND를 합성하지 않는다. Unicode 코드포인트 offset과 근거 scope의 고유성을 유지한다.
- 잘못된 목록/문단/앵커에서도 확보한 텍스트는 보존하되 신뢰하지 않는다. 기존 UNSUPPORTED_CONTROL/UNSUPPORTED_RECORD 등 부분 사유를 사용하며 진단 enum/응답 필드는 추가하지 않는다.
- 그림, 자동 번호, 쪽 번호, 알 수 없는 자식 구조는 이번에 지원한 것으로 취급하지 않는다. 각주 문단 처리만으로 실제 함안 HWP 전체를 COMPLETE_TEXT로 승격하지 않는다.

DB migration·v1/v2 공개 API·운영 정책·관리자 UI·분류/구간 규칙은 변경하지 않는다. 추출기와 실행 identity는1.0.12로 함께 구분하고 해당 버전의 HWP IPC에도 부분 사유 필수 검증을 적용한다. 이전1.0.11 실파일 결과는 새 파서의 성공 근거가 아니다.

## 체크리스트와 검증

- [x] 현재 파일/실측 metadata와 공개 형식을 대조했다.
- [x] 각주·미주 문단 처리, 추출기/실행 버전, IPC 필수 부분 사유를 구현했다.
- [x] 합성 OLE 비압축/압축, 반복 각주, Unicode/독립 scope, 잘못된 길이·개수·속성·예약값·순서·깊이·앵커·알 수 없는 자식 검증을 작성했다.
- [x] 표적72건(각주19·기존 표53),23초 성공. Log4j provider 기존 경고를 성공과 별개로 남긴다.
- [x] 전체 추출기168·IPC/관측 계약45·패키지20·Node6 통과 및 probe/bootJar 준비를 확인했다.
- [!] 전체 첨부 회귀는 Windows 애플리케이션 제어 정책의 pg_ctl.exe 실행 차단9건으로 실패했다. Linux CI 검증을 별도로 수행한다.
- [x] 같은 함안 실파일에서 새 버전 결과를 대조했다. 본문·binary/text/locator 지문·4,644자/213블록 유지, 미지원 record3→2/control4→3이다. 부분 품질과 검수 판정은 유지하며 누적19회로 반영했다.
- [ ] 명시 구간 worker/DB/API·검토된 기대값·전체 수집원·운영 E2E.

```powershell
.\gradlew.bat :attachment-extractor:test --tests '*HwpNoteTextTest' --tests '*HwpTableTextTest' --no-daemon --max-workers=1
.\gradlew.bat :test --tests 'com.saneb.domain.announcementattachment.*' :attachment-extractor:test :attachmentContractQaTest :attachmentBbsObservationProbeJar :bootJar --no-daemon --max-workers=1
```

성공 기준은 문단 구조·원문 보존·근거 분리·실패 시 검수·기존 표/링크 회귀·실행 버전 결합이다. 목록 누락이나 수 불일치를 정상으로 처리하거나 텍스트를 숨기는 경우, 부분 추출을 정상 후보로 바꾸는 경우, 이전 실제 결과를 새 버전 성공으로 재사용하면 실패다. 브라우저는 현재 요청 정책상 미실행이며 전체9Gate=8부분/1차단을 유지한다.

## 로컬 결과와 미검증 경계

전체 첨부 실행은3분44초에 실패했다. JUnit XML 집계1,994건=통과1,961/실패9/조건부 생략24/오류0이다. 실패9건은 모두 기존 공고 제목/DRAFT seed 시험의 EmbeddedPostgres 시작 중 `CreateProcess error=4551`이며 Windows 애플리케이션 제어 정책이 `pg_ctl.exe`를 차단했다. 정책을 끄거나 실행 파일을 바꿔 우회하지 않았다. 원본 XML을 `build/qa-results/hwp-notes-local-full-20260928`에 보존했다.

후속 명령은 DB를 시작하지 않는 계약 시험과 전체 추출기/패키지를 명시 선택했다. 이는 실패9건의 통과 대체가 아니다.

```powershell
.\gradlew.bat :attachment-extractor:test :test --tests '*HwpPartialDiagnosticTest' --tests '*HwpStructureDiagnosticTest' --tests '*AnnouncementAttachmentBbsObservationProbeTest' :attachmentContractQaTest :attachmentBbsObservationProbeJar :bootJar --no-daemon --max-workers=1
node --test scripts/qa/attachment-bbs-observation-probe.test.mjs
```

45초 성공, 추출기168/IPC·관측 계약45/패키지20/Node6 실패·오류·생략0이다. bootJar/probe는 직전 동일 소스 빌드 결과를 재사용했다. 기존 Log4j provider/unchecked/VM 경고는 남아 있다. 자체 실행 Gradle/Node 종료를 확인했으며 이전부터 실행 중인 다른 Java 프로세스는 종료하지 않았다.

운영 변경·추가 공고 요청·브라우저 실행은0이다. 함안 누적15회·10,701,268byte는 그대로다. QA 전용 브랜치 push는 Linux 계약 CI만 실행하고 master/main 운영 배포는 실행하지 않는다. 새 Linux CI 및1.0.12 실제 함안 재관측의 성공 여부는 각각 후속 결과로 확인해야 한다.

## 후속 서울 동일 파일 대조 — 2026-09-28 13:31 KST

위 로컬 구현 시점의 요청0/누적15회 기록은 보존한다. 이후 사용자 승인 범위에서 같은 함안41306 전체HWP1개를 새 추출기로 다시 관측했다. 소스 SHA는 `835bd1a52a5d1bd184570a9bd4c8aa4e09c0815d`다.

- 실행 ID `ffb5e067825e418cabca1c41b642a69f`, SSM `e7947656-83ef-4400-8ae7-00719e8570c5`, terminal Success/exit0.
- 실행 코드 지문 `9fb08bcab4b5596cd7ff2911e1a7775aa3d94cb13474e1ca67ddb72b67e60c2a`, 추출기1.0.12.
- 패키지는135파일/88,395,912byte, SHA256 `f1723e5faca9fefd46af5051417ce0c2fab62df78d4d05d287555499a55ea7a4`다. 로컬 전송 helper의 명시 `-UbuntuAmd64Only` 경로에서 PostgreSQL16.15.0의 darwin-amd64/windows-amd64/linux-amd64-alpine JAR3개만 제외했다. Linuxamd64 glibc와 모든 코드·시험·추출기 의존성은 유지한다. 로컬 원본 라이브러리는 삭제하지 않았다. 같은 현재 패키지 기준99,851,174byte를 줄였다.
- 서버 CPU1개·768MiB·임시1GiB·최대20분은 그대로다. probe24.976초, 시험1통과/실패·생략·중단·container실패0. 실제 임시 DB DRAFT seed가 성공했고 운영DB는 사용하지 않았다. 전체 DB 계약 CI 통과를 의미하지는 않는다.
- AWS 인증·서울 대상1대/SSM·기존 배포 revision을 재확인했다. 설치JAR불변/healthUP, unitInactive/probe정리/전송임시정리/원본정리 모두true다. 정책QA/기대값승인false·운영쓰기0·최종관리자검증필수는 유지했다.

| 비교 항목 | 1.0.11 | 1.0.12 |
|---|---|---|
| 본문 | AVAILABLE,89자 | 동일,본문지문 일치 |
| 전체 첨부 | HWP1개,101,888byte | 동일,binary지문 일치 |
| 추출 | 4,644자/213블록 | 동일,text/locator지문 일치 |
| 미지원 record/control | 3/4 | 2/3 |
| 품질/최종 판정 | PARTIAL_TEXT/REVIEW_REQUIRED | 동일,ATTACHMENT_INCOMPLETE |

이 비교는 지원한 구조의 부분 사유 감소를 확인한 것이다. 추가 각주 텍스트를 새로 복원했다거나 전체 문서 의미가 완전해졌다고 주장하지 않는다. 그림·도형·자동번호·쪽번호의 대응은 추가 근거가 필요하며 단순히 남은 진단 수와 일대일 연결하지 않는다. 고정 제목 입력을 사용하므로 현재 상세 페이지 제목을 새로 대조한 증거도 아니다. 명시 구간 엔진 worker/DB/API·정상 기대값 승인은 별도다.

이번4요청·2,204,906예약byte를 추가해 **누적19/60요청·12,906,174/100,663,296byte**, 잔여41요청·87,757,122byte다. 실행 보호 조건은 기존 Windows/실패CI/첫 서울 영수증과 코드 변경을 검증하고 이번 한 번만 허용한다. 추가 반복은 다시 누적 검토해야 한다.

결과는 `build/temporary-bbs-qa-ffb5e067825e418cabca1c41b642a69f/result.json`, SHA256 `061bb58686adb0a42157abe85aaf7c0fe6a3b64a92f554516d721188cc7af560`에 보존했다. S3 자기 전송 객체와 지문이 일치하는 로컬 package.zip을 삭제하고 계획/영수증은 남겼다. 운영 객체는 삭제하지 않았다. 자체 Node/Python 시험은 종료했고 기존 다른 프로세스는 보존했다. 로컬 helper/영수증은 build 아래 로컬 산출물이므로 Git으로 전달되는 공용 실행 도구로 표현하지 않는다.

실행 명령은 로컬 `Build-SanebTemporaryQaPackage.ps1 -VerificationMode HAMAN_OBSERVATION -UbuntuAmd64Only`와 `Invoke-SanebAwsReadOnly.ps1`의 TemporaryRun/Poll/Cleanup이며 모두 동일 계획 파일을 사용했다. Node6·Python 실행기25/영수증4와 PowerShell 구문 검사도 다시 통과했다. Linux CI36377684922는 같은 SHA로 별도 진행하며, 현재 관측 성공으로 미확인 CI를 통과 처리하지 않는다. 운영 변경·브라우저 실행은 없고 전체9Gate=8부분/1차단을 유지한다.
