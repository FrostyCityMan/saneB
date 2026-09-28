# HWP 쪽 번호 배치의 제한 지원 1.0.15

## 목표와 지원 경계

쪽 번호 배치만 있는 문서를 미지원 본문으로 처리하는 경우를 줄인다. 강북 실제 HWP에서 PAGE_NUMBER/16byte가 관측됐지만 payload 값은 보관하지 않았으므로 이 변경이 해당 파일의 부분 추출을 해결한다고 단정하지 않는다. 기존 1.0.14 실파일 결과와 새 버전의 합성 검증은 별개다.

한컴 [HWP5.0 revision1.3 표6·147·148](https://cdn.hancom.com/link/docs/%ED%95%9C%EA%B8%80%EB%AC%B8%EC%84%9C%ED%8C%8C%EC%9D%BC%ED%98%95%EC%8B%9D_5.0_revision1.3.pdf)과 고정 revision의 [hwplib 작성기](https://github.com/neolord0/hwplib/blob/6746c27f17ebf5277493206284aa32044a1839c4/src/main/java/kr/dogfoot/hwplib/writer/bodytext/paragraph/control/ForControlPageNumberPosition.java)를 대조했다. 양쪽의 길이와 속성은 같지만 뒤의 4개 UINT16/WCHAR 필드 설명은 다르다. 어느 한쪽을 일반화하지 않고 양 해석 모두 사용자 텍스트가 없는 교집합만 지원한다.

- ID `pgnp`/0x70676e70, ID 포함 정확히16byte.
- 속성: 십진 모양0, 위치0~10, 예약 비트0.
- ID 기준 offset8=0, offset10=0, offset12=0 또는 ASCII 하이픈45, offset14=45. 한컴 문서의 기호/장식/고정 하이픈과 공개 작성기의 번호/기호/장식 어느 해석에서도 임의 문자열을 버리지 않는 제한이다. 그 외 값은 부분 추출을 유지한다.
- 문단 직계 CTRL_HEADER, 같은 문단의 제어문자21 앵커와 순서대로 대응해야 한다. 누락·중복·잘못된 깊이·종류는 부분 추출이다.
- 자식이 없는 leaf만 허용한다. 알려진 배치 레코드라도 자식이면 거부하고, 자식 텍스트가 있으면 신뢰하지 않는 근거로 보존한다.
- 페이지 번호·페이지 수·렌더링 결과를 계산하거나 합성하지 않는다. 지원되는 배치 메타데이터는 본문 텍스트가 아니며 앵커 앞뒤 문단 조각의 독립 scope를 유지한다. AUTO_NUMBER의 동적 페이지 번호, 머리말/꼬리말, 그림/OCR 지원은 확대하지 않는다.

DB/API/migration/분류 규칙/운영 정책·worker 설정은 변경하지 않는다. 추출기 버전·runtime identity·IPC 진단 요구만1.0.15로 동기화한다. 이전 고정 공고 probe와 영수증의1.0.14 조건은 그대로 보존하며 새 버전의 외부 실행 승인을 대신하지 않는다.

## 작업·검증 체크리스트

1. [x] AGENTS·장기 goal 스킬·실제 파서·공개 근거와 기존 부분 품질 정책 확인.
2. [x] 제한된 배치 검증, 앵커/leaf 확인과 버전 계약 구현.
3. [x] 신규32건·기존 자동 번호20/각주19/표53, 총124건 실패/오류/생략0. 압축/비압축 OLE, 허용 위치·장식, 손상 길이·속성·사용자 기호·앵커·자식·Unicode scope·표/각주 회귀 통과.
4. [x] 전체 추출기222·IPC/worker/구간195·패키지20·bootJar 검증 통과. 마지막 주석 변경 후 재컴파일과 산출물 의존성 재확인 성공(17초, 나머지 UP-TO-DATE). 직접 배포판과 QA 패키지 배포판 모두1.0.15 JAR만1개 존재한다.
5. [x] QA 브랜치 소스0b565819a55022cf67504c451dc69e1129f49413 커밋·푸시와 원격 SHA 일치 확인. 동일 소스 Linux와 실제 파일 관측은 아래처럼 별도 상태다.

성공 기준: 명세의 제한 범위만 지원하고 미지원/손상 입력은 부분 품질·검수로 남으며 텍스트 순서·코드포인트 위치·독립 근거를 보존한다. 실패 기준: 길이만으로 성공 처리, 페이지 추정, 임의 장식/본문 손실, 실패 사유 제거, 실제 공고가 검증됐다고 확대 보고.

강북 누적16/21요청·3,236,178byte, 중구32/32요청은 보존한다. 추가 공고 요청은 하지 않는다. 전체9Gate=8부분/1차단과 ATT001~062·운영 E2E 완료 기준을 유지한다.

## 로컬 검증과 잔여 Gate

```powershell
.\gradlew.bat :attachment-extractor:test --tests '*HwpPageNumberLayoutTest' --tests '*HwpAutoNumberTextTest' --tests '*HwpNoteTextTest' --tests '*HwpTableTextTest' --no-daemon --max-workers=1
.\gradlew.bat :attachment-extractor:test :test --tests '*HwpPartialDiagnosticTest' --tests '*HwpStructureDiagnosticTest' --tests '*PdfStructureDiagnosticContractTest' --tests '*AttachmentRuntimeGateTest' --tests '*AnnouncementAttachmentBbsObservationProbeTest' --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' --tests '*AnnouncementAttachmentHamanWorkerProbeTest' --tests '*AnnouncementAttachmentJungguWorkerProbeTest' --tests '*AttachmentSegmentRoleAnalyzerTest' --tests '*AttachmentSegmentClassificationEngineTest' :attachmentContractQaTest :attachmentBbsObservationProbeJar :attachmentOfficialWorkerProbeJar :bootJar --no-daemon --max-workers=1
node --test scripts/qa/attachment-bbs-observation-probe.test.mjs scripts/qa/attachment-official-worker-probe.test.mjs
# 구성된 Python runtime으로 실행
python -B -m unittest discover -s scripts/qa -p test_temporary_bbs_observation.py
```

Node21·Python35도 실패/생략0이다. Windows Gradle에는 기존 Windows-ROOT trustStore 옵션을 함께 사용했다. 첫 표적 컴파일에서 새 테스트의 record 접근자2곳을 실제 계약에 맞춰 수정했다. 확대 검사에서 과거 강북 단일 진단 fixture가 현재 추출기 상수를 상속하여1건 실패했으며, fixture를 원래1.0.14로 고정하고 다른 버전 거부 테스트를 추가했다. 과거 판정기/영수증 조건은 변경하지 않았다. 재실행195건은 모두 통과했다. 중단된 빌드에서 installDist가 실행되기 전에 확인한1.0.14 설치물은 새 버전 검증으로 세지 않았다.

이 검증은 합성/로컬 계약 검증이다. 새 버전의 실제 공고 요청·서울 격리 실행·정상 기대값 승인·worker 실파일 DB/API·운영 배포는 하지 않았다. Windows의 Linux 격리/실제 DB 검증을 대신하지 않으며 동일 소스 Linux CI도 별도 결과가 필요하다. 현재 명시 요청 정책에 따라 브라우저 검증은 수행하지 않았다. `COMPLETE_TEXT`는 지원 구조의 추출 품질이지 후보 확정·자격 충족이 아니다.

후속은 기존 예산과 지문을 보존한 실제 HWP의 새 버전 대조다. 강북4번째 파일의 BRIDGE_GET HTTP400은 별도 blocker이며 이 파서 수정으로 해결되지 않는다. catalog40참조/14프로필/기대값1/정상0과 전체9Gate8부분/1차단은 변하지 않는다.

최종 명령은 `:attachment-extractor:test :attachmentContractQaTest :attachmentBbsObservationProbeJar :attachmentOfficialWorkerProbeJar :bootJar`다. 광범위 재검증1분18초 후 위17초 의존성 확인을 마쳤다. 전체 root suite는 이번 변경에서 재실행하지 않았으며 이전3,108건 결과를 새 소스의 결과로 사용하지 않는다. 기존 Log4j provider/unchecked/VM 경고는 남는다.

## QA 브랜치 전달

- [x] 소스0b565819a55022cf67504c451dc69e1129f49413을 QA 브랜치에만 push했다. master/main merge·운영 배포·정책 게시·ENFORCE·재분류는 하지 않았다.
- [~] [동일 소스 Linux36395464220](https://github.com/FrostyCityMan/saneB/actions/runs/36395464220)은 최초 조회pending이다. 이전61a9543의 Linux36393605515는in_progress, 641450c의 Linux36392919528은success로 확인했다. 다른 소스의 성공을 새 소스 통과로 사용하지 않는다.
- [x] 사용한 Node/Gradle 명령은 종료했으며 소유 Gradle JVM도 남지 않았다. 기존 사용자 Java2개와 Node 프로세스, 미추적 `output/`·`scripts/qa/__pycache__/`를 보존했다.
- [~] 다음은 새 버전의 실제 HWP payload/추출 대조다. 기존 소모·단일 실행 예약을 우회하지 않고 새 계획에서 버전과 누적 한도를 먼저 고정한다. 이 문서 기록만으로 외부 실행 완료 또는 정상 기대값을 추가하지 않는다.
