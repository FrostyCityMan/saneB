# HWP 저장 자동 번호 텍스트 복원 1.0.13

## 목표·근거·범위

함안 실제 HWP에서 AUTO_NUMBER 컨트롤이 관측됐지만 현재 추출기는 번호 텍스트를 복원하지 않는다. 한컴 [HWP5.0 revision1.3](https://cdn.hancom.com/link/docs/%ED%95%9C%EA%B8%80%EB%AC%B8%EC%84%9C%ED%8C%8C%EC%9D%BC%ED%98%95%EC%8B%9D_5.0_revision1.3.pdf)의 표6/142/143과 고정 revision의 [읽기](https://github.com/neolord0/hwplib/blob/6746c27f17ebf5277493206284aa32044a1839c4/src/main/java/kr/dogfoot/hwplib/reader/bodytext/paragraph/control/ForControlAutoNumber.java)/[쓰기](https://github.com/neolord0/hwplib/blob/6746c27f17ebf5277493206284aa32044a1839c4/src/main/java/kr/dogfoot/hwplib/writer/bodytext/paragraph/control/ForControlAutoNumber.java)를 대조했다. ID 포함16byte의 속성/저장 번호/기호/앞뒤 장식과 제어문자18 앵커를 사용한다.

첫 지원은 저장 번호1~65535, 십진 모양, 각주·미주·그림·표·수식 번호다. 페이지 번호0종류는 실제 레이아웃 계산 없이 추정하지 않는다. 예약 비트·미지원 모양·사용자 기호·손상 문자·추가 자식·잘못된 앵커는 부분 품질로 남긴다. 위첨자는 각주 번호의 표시 속성만 허용하며 숫자값은 그대로 보존한다. 쪽 번호 위치는 명세와 공개 구현의 필드 차이가 있어 이번 지원에서 제외한다. 그림/OCR·기존 부분 품질 정책도 변경하지 않는다.

번호를 삭제하거나 양옆 문장을 합치지 않는다. 번호·장식을 독립적인 코드포인트 근거 블록으로 추출하고 문단/번호 위치를 보존한다. 앵커가 없거나 구조가 잘못된 경우 복원 가능한 값은 신뢰하지 않는 블록으로 남기며 정상 텍스트로 승격하지 않는다.

## 실행 체크리스트

1. [x] 현재 코드·함안 실파일 진단·공개 형식/읽기/쓰기 근거 확인.
2. [x] 헤더/속성/앵커/직계 깊이/자식 검증과 번호 텍스트 복원 구현.
3. [x] 압축/비압축 OLE, 반복 번호·각주 중첩·Unicode 위치, 손상 헤더/누락·잘못된 앵커/미지원 자식·형식 회귀. 신규20건·기존 각주19/표53, 총92건 실패·오류·생략0.
4. [x] 추출기1.0.13·runtime identity·IPC 진단 필수 조건 동기화, 전체 추출기188·계약77·패키지20·bootJar 검증. 모두 실패·오류·생략0.
5. [~] QA 브랜치 커밋·푸시, 동일 SHA Linux 회귀를 진행한다. 실파일 재관측은 기존 예산/고정 실행기 버전 경계를 재검토한 후 별도로 수행.

성공 기준: 저장된 번호·장식을 빠뜨리지 않고 순서/범위/독립 scope를 보존하며 미지원 입력은 검수로 남는다. 실패 기준: 임의 번호 추정, 미확인 형식을 성공 처리, 문단 간 조합 합성, 기존 텍스트 손실, 이전 버전의 실파일 결과를 새 버전 성공으로 사용.

DB/API/migration/구간 규칙/운영 설정·데이터는 변경하지 않는다. 함안 누적23/60요청·15,111,080byte를 유지한다. 기존 HAMAN_SEGMENT 실행기는1.0.12/기존 지문에 고정돼 있으며 새 버전의 변경된 텍스트를 자동 승인하지 않는다. 전체9Gate=8부분/1차단과 ATT001~062·운영 E2E 요구를 유지한다.

## 로컬 검증 기록

```powershell
.\gradlew.bat :attachment-extractor:test --tests '*HwpAutoNumberTextTest' --tests '*HwpNoteTextTest' --tests '*HwpTableTextTest' --no-daemon --max-workers=1
.\gradlew.bat :attachment-extractor:test :test --tests '*HwpPartialDiagnosticTest' --tests '*HwpStructureDiagnosticTest' --tests '*AnnouncementAttachmentBbsObservationProbeTest' --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' --tests '*AnnouncementAttachmentHamanWorkerProbeTest' :attachmentContractQaTest :attachmentBbsObservationProbeJar :attachmentOfficialWorkerProbeJar :bootJar --no-daemon --max-workers=1
node --test scripts/qa/attachment-bbs-observation-probe.test.mjs scripts/qa/attachment-official-worker-probe.test.mjs
.\gradlew.bat :test --tests '*AttachmentSegmentRoleAnalyzerTest' --tests '*AttachmentSegmentClassificationEngineTest' --tests '*AttachmentRuntimeGateTest' --no-daemon --max-workers=1
```

표적25초/92건 성공, 확대1분22초/추출기188·IPC/관측/worker계약77·패키지20 성공이다. Node16건도 통과했다. 기존 Log4j provider/unchecked 경고는 별개로 남는다. Windows AppControl의 PostgreSQL 실행 차단을 우회하지 않았으며 실제 DB 회귀는 동일SHA Linux CI로 확인한다. 위 결과를 함안 실제 파일의1.0.13 성공·기대값 승인·정책QA·운영 배포로 해석하지 않는다. 공고 요청0·운영 변경0, 브라우저는 현재 명시 요청 정책상 미실행이다.

후속 구간 역할/구간 분류/runtime 회귀97건도29초에 실패·오류·생략0으로 통과했다. 기존 VM class-sharing 경고는 별도로 남는다. 실행한 Gradle single-use daemon·시험·Node는 종료하고 기존 사용자 프로세스와 output/·scripts/qa/__pycache__/는 보존한다.
