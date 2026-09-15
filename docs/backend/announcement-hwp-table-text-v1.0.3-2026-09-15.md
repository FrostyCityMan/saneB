# HWP 표·셀 텍스트 처리 1.0.3

## 목표와 완료 경계

제목 1차 → 정제 본문 2차 → 실제 첨부 텍스트 3차 → 관리자 최종 검증의 순서를 유지한다. HWP 표가 있다는 이유만으로 항상 부분 추출하던 경로에서, 구조와 전체 셀을 검증할 수 있는 표는 실제 문단 텍스트를 순서대로 제공한다. 서로 다른 셀/문단을 하나의 키워드 AND 근거로 합치지 않는다. 최종 후보 확정·정책 게시·자동 활성화는 이 변경의 효과가 아니다.

이번 변경은 공통 HWP 추출기다. 이 변경 당시 엔진6/첨부 프로필17/전용 본문16/형식3의 수를 늘리는 수집처 모델 추가가 아니다. 전체 ATT-001~062 및 Gate0~8의 미완료 범위는 보존한다. 후속 철원 모델 추가와 구분한다. 태백 실제 HWP는 새1.0.3에서도9,734자/495블록의 부분 추출이며, 전체 완전성 문제는 해소되지 않았다.

## 확인한 형식 근거

한컴 HWP5 문서의 레코드 계층 설명과 공개 HWP 라이브러리의 작성·읽기 구현을 대조했다. 공개 구현은 `neolord0/hwplib` 커밋 `6746c27f17ebf5277493206284aa32044a1839c4`에 고정한다. 외부 라이브러리 의존성을 추가하거나 해당 구현을 복사하지 않고, 기존 POI OLE 스트림 위에서 필요한 레코드 형식을 직접 처리한다.

- [문단 작성 계층](https://github.com/neolord0/hwplib/blob/6746c27f17ebf5277493206284aa32044a1839c4/src/main/java/kr/dogfoot/hwplib/writer/bodytext/paragraph/ForParagraph.java): PARA_HEADER 자식 깊이에 텍스트와 CTRL_HEADER가 위치한다.
- [표 작성 계층](https://github.com/neolord0/hwplib/blob/6746c27f17ebf5277493206284aa32044a1839c4/src/main/java/kr/dogfoot/hwplib/writer/bodytext/paragraph/control/ForControlTable.java), [셀 읽기](https://github.com/neolord0/hwplib/blob/6746c27f17ebf5277493206284aa32044a1839c4/src/main/java/kr/dogfoot/hwplib/reader/bodytext/paragraph/control/tbl/ForCell.java): TABLE, 셀 LIST_HEADER, 셀 PARA_HEADER는 같은 깊이이고, 셀의 텍스트는 그 다음 깊이다. LIST_HEADER에 문단 수 INT32와 속성 UINT32의 8바이트 기본 영역이 있다.
- [표 메타데이터](https://github.com/neolord0/hwplib/blob/6746c27f17ebf5277493206284aa32044a1839c4/src/main/java/kr/dogfoot/hwplib/writer/bodytext/paragraph/control/tbl/ForTable.java): 행·열 수와 행별 셀 개수가 저장된다. 셀의 행·열 및 span과 대조해 누락·중복을 판별한다.
- [인라인/확장 문자 작성](https://github.com/neolord0/hwplib/blob/6746c27f17ebf5277493206284aa32044a1839c4/src/main/java/kr/dogfoot/hwplib/writer/bodytext/paragraph/ForParaText.java): 16바이트 컨트롤 문자 안의 ID와 문단 아래 실제 컨트롤을 연결한다. 표는 코드11의 `tbl ` ID를 사용한다.

## 구현 계약

1. `HwpDocumentTextExtractor`는 기존 OLE/압축/레코드 바이트 상한과 숫자 구조 진단을 유지하며 레코드를 `HwpSectionText`로 전달한다.
2. 문단/컨트롤 프레임으로 실제 깊이를 추적한다. 최상위 문단이 끝날 때 결과를 기록하고 보관한 문단 트리를 해제한다. 큰 헤더 payload는 보관하지 않고 길이·문단 플래그만 저장한다.
3. 부모 문단의 텍스트를 표 앵커 위치에서 분리한다. 표 앞 연속 구간 → 셀별 문단/중첩 표 → 표 뒤 연속 구간 순서이며 같은 ID의 여러 표도 앵커 순서대로 연결한다.
4. 표의 행별 셀 수, 행/열 범위, span, 전체 격자 피복, 중복/누락/역순, 셀별 문단 수, 마지막 문단 플래그, 텍스트 문자 수를 검증한다. 검증된 직사각형 병합 셀은 한 번만 추출한다.
5. 근거 위치는 section/표 번호/셀 행·열/문단 번호/연속 구간 번호로 구성하고 code-point offset을 사용한다. 각 문단과 분리 구간은 고유 `evidenceScopeId`다. 셀 간 의미 관계·행의 라벨과 값·페이지 번호를 추정하지 않는다.
6. 앵커 없는 표, 소속/순서를 확정하지 못한 셀, 불일치 문단은 텍스트를 보존하되 `PARTIAL_TEXT`와 불확실한 근거를 유지한다. 앵커에 대응하는 컨트롤이 없어도 앞/뒤 구간을 합치지 않는다.
7. 지원하지 않는 그림·수식·도형·차트·미래 레코드, 표 앞 캡션, 미해석 셀 필드 확장, 기타 텍스트 컨트롤은 여전히 부분 추출이다. 이미지 OCR이나 일반 도형 내 텍스트 완전 지원을 추가한 것이 아니다.
8. 셀 헤더는 확인된 34/38/47바이트 형식만 완전 검증한다. 47바이트 형식에서 알려지지 않은 필드 데이터는 무시해 완전 처리하지 않는다. 전체 표 격자 최대20,000칸, section 프레임 노드 최대20,000개·동시 깊이64, 입력 텍스트 단위 최대2,000,000을 추가 적용한다. 기존 출력100만 code point/20,000블록·파일/압축/시간/메모리/격리 제한은 유지한다.
9. 알려진 bare PARA_TEXT 입력의 기존 기본 위치 형식은 유지한다. 표/중첩 처리의 품질·순서·근거 변화는 추출기1.0.3과 새 runtime 지문으로 분리한다. 역할 규칙 버전은 변경하지 않는다.

## 영향과 검증 계획

- DB/schema/migration/v1/v2 공개 필드·관리자 UI 변경 없음. `ExtractionResult`와 서버 `AttachmentRuntimeIdentity`의 기대 추출기 버전만 함께1.0.3으로 변경한다.
- 이전1.0.1/1.0.2의 QA 결과로1.0.3의 정책 검증을 충족시킬 수 없다. 실제 고정 HWPX 비교와 HWP 전체 worker/임시 DB/API 경로를 새 runtime으로 재실행해야 한다.
- [x] 압축/비압축 실제 OLE 합성 표, 전후 문장, 복수 표, 다중 문단, 병합/중첩, 서로 다른 셀/문단의 scope 분리, code-point offset 시험 추가.
- [x] 앵커/헤더/문단 수·종료 플래그·span·행별 셀 수·중복/누락·미지원 레코드·노드/깊이/격자 예산 시험 추가.
- [x] 최종 로컬 회귀·패키지·bootJar/probe 검증(2026-09-15 15:12 KST). root2653=2391통과/262조건부 생략/실패0, 추출기88건(신규 표30건 포함)·패키지20건은 모두 실패/생략0이다. Node 배포/공식 probe15통과·Linux2생략이다.
- [x] `ee72b97`의 [Linux34936106328](https://github.com/FrostyCityMan/saneB/actions/runs/34936106328) 성공 및 보관 XML 확인. root2653=2390통과/263조건부 생략/실패0, 추출기88·패키지20·job192·migration17·worker12·runtime1·정책 부모2·Flyway3은 실패/생략0. 공식 사이트 opt-in/배포는 이 실행에서 미실행이다.
- [~] 새 runtime의 실제 HWP1건은 `cc79d59`/Linux34939913277에서 worker·임시 DB·API까지 통과했으나 PARTIAL_TEXT다. 개체/그림 레코드가 실제로 존재하며 누락 구조 전체·내용 순서의 독립 검토와 HWPX 재비교는 남는다. [실행 결과와 해석 한계](announcement-taebaek-hwp-ci-qa-2026-09-15.md)를 따른다.
- [ ] 같은 SHA 운영 설치·브라우저 업무 E2E. 직전 배포 취소 후 재실행 여부 확인은 대기다.

정책 게시·ENFORCE·기존 데이터는 정확한 대상/건수/요청·byte 상한/효과/복구 승인 후 수행한다. 이번 로컬 구현으로 운영 worker를 활성화하거나 운영 데이터를 수정하지 않는다.

## 실행 기록

명령은 `.\gradlew.bat :test :attachment-extractor:test attachmentContractQaTest bootJar attachmentOfficialWorkerProbeJar --no-daemon --console=plain --max-workers=1`이다. 첫 전체 실행4분47초에서 root 시험은 통과했지만, 실행 중 추가한 방어 테스트1건이 먼저 컴파일된 구소스 class를 사용해 실패했다. 소스 수정06:07:50Z/class06:07:05Z/시험 class06:11:26Z를 확인했다. assertion을 바꾸지 않고 소스를 고정한 뒤 같은 명령을 재실행했다.

최종35초 실행은 BUILD SUCCESSFUL이다. 추출기/패키지를 실행했고 root/웹 JAR/probe는 앞선 실행 결과를 재사용(UP-TO-DATE)했다. XML 전체를 다시 합산했으며 최종 HWP class06:12:07Z가 소스보다 최신이고 추출기 JAR 내부 class와 로컬 컴파일 class 두 개의 hash가 일치한다. 이는 실제 외부 파일의 정확도나 Linux 격리 성공을 대체하지 않는다. 사용한 단발 Node/JVM 종료·Java 프로세스0을 확인했다.
