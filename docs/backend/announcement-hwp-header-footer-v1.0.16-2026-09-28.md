# HWP 머리말·꼬리말 문단 지원 1.0.16

> 사용자 후속 지시로 이 지점에서 보완을 중지한다. 이미 실행 중이던 검사만 종료했으며 실제 파일 QA와 추가 HWP 개발은 하지 않는다. [전 지역 첨부 수집 우선 계획](announcement-regional-attachment-collection-first-2026-09-28.md)이 다음 실행 기준이다. 화천 추가 QA 질문은 실행하지 않는다.

## 목표와 근거

화천32258의1.0.15 실파일 관측은 HWP3,491자/142블록·미지원record1/control1·HEADER12byte를 보였다. 현재 파서는 머리말/꼬리말의 LIST_HEADER와 control을 지원하지 않는다. 다만 실파일 payload와 실패 원인별 연결을 보관하지 않았으므로 이 변경이 해당 파일의 부분 품질을 해소한다고 단정하지 않는다.

한컴 [HWP5.0 revision1.3 표6·140·141](https://cdn.hancom.com/link/docs/%ED%95%9C%EA%B8%80%EB%AC%B8%EC%84%9C%ED%8C%8C%EC%9D%BC%ED%98%95%EC%8B%9D_5.0_revision1.3.pdf), 고정 revision6746c27의 [머리말 읽기](https://github.com/neolord0/hwplib/blob/6746c27f17ebf5277493206284aa32044a1839c4/src/main/java/kr/dogfoot/hwplib/reader/bodytext/paragraph/control/ForControlHeader.java), [문단 목록 쓰기](https://github.com/neolord0/hwplib/blob/6746c27f17ebf5277493206284aa32044a1839c4/src/main/java/kr/dogfoot/hwplib/writer/bodytext/paragraph/control/headerfooter/ForListHeaderForHeaderFooter.java), [꼬리말 쓰기](https://github.com/neolord0/hwplib/blob/6746c27f17ebf5277493206284aa32044a1839c4/src/main/java/kr/dogfoot/hwplib/writer/bodytext/paragraph/control/ForControlFooter.java)를 대조했다. 외부 라이브러리를 새 의존성으로 도입하지 않는다.

## 구현 계약

- control ID head/foot, 제어문자16·같은 문단의 직계 control 대응을 요구한다. 페이지별 렌더링이나 반복 출력·페이지 수를 추정하지 않는다.
- ID 포함8byte 구형 또는12byte(createIndex 포함)만 지원한다. 적용 페이지 값0~2 이외의 비트는 미지원으로 둔다. createIndex를 텍스트로 합성하지 않는다.
- 문단 목록은 정확히34byte: 문단수·속성·폭·높이·예약0 18byte. 임의 확장을 무시하지 않는다. 수평 방향과 알려진 줄바꿈/세로정렬 범위만 허용한다.
- 목록은 control 직계이며 문단보다 앞에 단1개 존재해야 한다. 실제 문단수·문자 단위 수·마지막 문단 표식·깊이를 검증한다. 텍스트가 없는 정상 빈 문단은 허용하지만 문자열을 만들지 않는다.
- 머리말/꼬리말 문단은 별도 locator와 evidence scope로 보존한다. 본문 앞뒤도 별도 scope이며 대상/지원 키워드를 서로 다른 문단에서 임의 결합하지 않는다. 반복 control도 전역 인덱스로 구분한다.
- 누락·중복·잘못된 앵커·알 수 없는 하위 레코드·손상 목록은 PARTIAL_TEXT 및 불신뢰 근거로 남긴다. 중첩 미지원 번호·그림을 포함한 문서를 완전 텍스트로 올리지 않는다.
- 추출기/런타임 identity1.0.16, HWP/PDF IPC 진단 필수 버전 목록을 동기화한다. 과거1.0.15 고정 probe·영수증·정책은 변경하지 않는다. 새 버전은 기존 고정 worker 모드의1.0.15 실행 조건을 통과하지 못하는 것이 정상이다.
- DB/API/migration/분류 규칙/운영 설정 변경 없음. 정상 기대값 자동 등록·공고 자동 활성화 없음.

## 작업·검증 체크리스트

1. [x] AGENTS·장기 goal 스킬·실제 파서·공개 원본 구현 확인.
2. [x] 제한된 머리말/꼬리말 문단 지원·버전/IPC 계약 구현.
3. [x] 표적133검사 통과: 새 머리말/꼬리말29 + 기존 각주19/표53/쪽번호32. 압축·비압축 OLE, Unicode offset/scope, 손상/누락/중복/깊이/중첩 미지원 구조 회귀 포함.
4. [x] 이미 실행 중이던 전체 추출기251·서버 연동200·패키징20 실패/생략0, bootJar 성공(1분40초). 완료 후 Gradle 프로세스 종료 확인.
5. [ ] QA 브랜치 커밋/푸시·동일SHA Linux 확인.
6. [ ] 새 버전 실파일/구간worker DB/API·정상 기대값·전체Provider/운영 E2E 검증.

성공 기준은 검증된 구조의 텍스트를 보존하고 손상/미지원은 부분 품질로 남기는 것이다. 실패 기준은 길이만 보고 승인, 원문 손실, 문단 간 키워드 조합, 페이지 합성, 실파일 미실행을 성공으로 보고하는 것이다.

## 검증 명령

```powershell
.\gradlew.bat :attachment-extractor:test --tests '*HwpHeaderFooterTextTest' --tests '*HwpNoteTextTest' --tests '*HwpTableTextTest' --tests '*HwpPageNumberLayoutTest' --no-daemon --max-workers=1
.\gradlew.bat :attachment-extractor:test :test --tests '*HwpPartialDiagnosticTest' --tests '*HwpStructureDiagnosticTest' --tests '*PdfStructureDiagnosticContractTest' --tests '*AttachmentRuntimeGateTest' --tests '*AnnouncementAttachmentBbsObservationProbeTest' --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' --tests '*AnnouncementAttachmentHamanWorkerProbeTest' --tests '*AnnouncementAttachmentJungguWorkerProbeTest' --tests '*AnnouncementAttachmentHwacheonWorkerProbeTest' --tests '*AttachmentSegmentRoleAnalyzerTest' --tests '*AttachmentSegmentClassificationEngineTest' :attachmentContractQaTest :attachmentBbsObservationProbeJar :attachmentOfficialWorkerProbeJar :bootJar --no-daemon --max-workers=1
```

Windows에서는 기존 Windows-ROOT trustStore 옵션을 함께 사용한다. 공고 외부 요청0·운영 변경0이다. 중구32/32·화천32258캠페인6/7 원장을 보존한다. 화천 잔여1회로는 최대5요청 worker를 반복할 수 없다. 브라우저는 현재 명시 요청 정책에 따라 미실행이다.

전체 목표 ATT001~062+SEG·9Gate는 그대로 유지한다. 활성223지자체 중 프로필 등록18·미등록205, catalog41참조/15프로필·정상0은 이번 추출기 변경으로 증가하지 않는다.
