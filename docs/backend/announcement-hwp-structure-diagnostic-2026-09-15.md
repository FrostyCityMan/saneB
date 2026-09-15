# HWP 구조 진단과 추출 품질 보완

이 문서는1.0.2의 이력이다. 후속 로컬1.0.3의 표·셀 처리와 검증은 [표 텍스트 처리 기록](announcement-hwp-table-text-v1.0.3-2026-09-15.md)을 따른다.1.0.3 운영 설치/실파일 성공을 의미하지 않는다.

## 목표와 현재 경계

제목 1차 → 정제 본문 2차 → 실제 첨부 텍스트 3차 → 관리자 최종 검증 순서를 유지한다. 이번 증분은 HWP에서 미해석 구조를 완전 추출로 오판하지 않도록 보완하고, 실제 파일의 부분 추출 원인을 원문 없이 관측하는 추출기 1.0.2다. 표·도형의 완전한 해석을 구현한 것으로 보고하지 않는다. 전체 ATT-001~062와 Gate0~8은 미완료다.

기준 증거는 [공식 worker 기록](announcement-official-worker-db-api-qa-2026-09-15.md)의 태백176153이다. 추출기1.0.1에서 9,743자/480블록을 확보했으나 PARTIAL_TEXT이며 U+FFFD는0개였다. 따라서 기존 구조 레코드 경로를 조사한다. 개별 구조가 실제 누락을 일으켰는지는 아직 확인하지 않았다.

## 계약과 구현

- 기존 parser는 레코드76~88만 PARTIAL_TEXT의 구조 사유로 처리했다. 범위 밖 글맵시·양식·차트·새로운 레코드가 무시되어 COMPLETE_TEXT가 될 수 있는 공백을 보완했다. 현재 문단·레이아웃66~75 외 레코드는 별도 구조 지원 검증 전까지 부분 추출을 유지한다. 66~75의 모든 내용 의미를 완전히 해석했다는 보증은 아니다.
- HWP 성공적인 파싱 결과에만 내부 격리 IPC의 선택 필드 `hwpStructure`를 추가했다. `sectionCount`, `recordCount`, `maximumLevel`, 정렬된 `recordTypes`의 `tagId`/`count`만 포함한다. 원문·파일명·주소·컨트롤 payload는 포함하지 않는다.
- 부모 프로세스가 정확한 필드 집합, 정수·범위, 중복/순서, 합계를 검증한다. tag/level은0~1023, record 상한은 기존128MiB 입력 예산에서 유도한33,554,432이며 진단 배열은 최대1,024행이다. 실패 메시지는 입력 원문을 반사하지 않는 고정 코드다.
- 기존7인자 결과 생성자는 유지하고 PDF/HWPX/실패 결과에는 새 필드를 넣지 않는다. 선택 필드가 없는 이전 형식의 해석은 유지한다. 실제 실행 호환성은 별도의 버전·runtime 지문 검증을 계속 따른다.
- 추출기와 애플리케이션의 기대 버전을 함께1.0.2로 올린다. 이전 runtime QA 근거를 새 설치 버전에 재사용하지 않는다. 실제 HWP worker 시험은 새 진단의 존재와 계약 검증을 요구한다.
- DB 저장, 공개 v1/v2 API, DTO, 관리자 UI 및 migration 변경은 없다. worker는 기존 품질·텍스트·블록을 사용하며 구조 진단은 시험 보고서에서만 사용한다.
- Linux 격리, 네트워크/프로세스/파일시스템/메모리/시간 제한, 임시 원본 정리, 자동 활성화 금지 및 정책 승인 경계는 유지한다.

## 검증 체크리스트

- [x] 압축/비압축·복수 section·확장 길이·최대 level·빈 본문·미해석/미래 tag 합성 시험.
- [x] 진단 숫자 계약·깊은 복사·추가 필드/원문 유입·잘못된 합계/범위/중복/순서 거부.
- [x] 표적 Java 시험103통과·실패0·Linux 전용1건 조건부 생략(2026-09-15 13:29 KST). root45/46통과와 추출기58/58통과다.
- [x] 전체 로컬 회귀·패키지·bootJar·probe 생성: 13:40 KST, 5분4초 성공. root2,653건=2,391통과/262조건부 생략·실패0, 추출기58건은 앞선 실행 결과 재사용(UP-TO-DATE), 패키지20건 통과·생략0. Node 배포/공식 probe15통과·Linux 전용2생략.
- [x] 새 커밋 `a860bbdf820f13bb6ed151020089f616447fb08f`의 [Linux34929791026](https://github.com/FrostyCityMan/saneB/actions/runs/34929791026) 성공 및 artifact XML 확인(14:40 KST). root2653=2390통과/263조건부 생략, 추출기58·패키지20·job192·migration17·worker12·runtime1·정책 부모2·Flyway3은 모두 실패/생략0이다. 공식 사이트 opt-in은 일반 CI 성공에 포함하지 않는다.
- [!] 새 배포는 [34933705970](https://github.com/FrostyCityMan/saneB/actions/runs/34933705970)가 14:42 KST 테스트·빌드 중 cancelled로 종료되어 미반영이다. AWS 인증·S3 업로드·CodeDeploy 단계는 모두 skipped이며 자동 재실행하지 않았다. 새 설치/DB/health/플래그 증거는 아직 없다.
- [ ] 새 추출기로 실제 태백 HWP 전체 파일→worker→임시 DB→API 경로 및 숫자 구조 진단 확인.
- [ ] 태백 HWPX2개의 사전 고정 기대값 재비교. 실행 결과로 기대값을 자동 덮어쓰지 않는다.

## 다음 구조 처리 작업과 운영 영향

로컬 명령은 `.\gradlew.bat :test :attachment-extractor:test attachmentContractQaTest bootJar attachmentOfficialWorkerProbeJar --no-daemon --console=plain --max-workers=1` 및 `node --test scripts/qa/attachment-official-worker-probe.test.mjs scripts/qa/attachment-contract-release.test.mjs`다. 로컬 웹 JAR SHA256은 `030cf195392b764d19fba589f175fdd8431f168cb5e78ee4a2d37bfae2a21f1a`, 별도 probe는 `b6b1bf5ec03327a45a81ed886e9735aedf7a9c4c67e237e646a95af5fc65c043`다. 운영 재빌드 JAR과 동일하다고 가정하지 않는다. `git diff --check` 지적0·기존 migration 변경0·사용한 Java/Node 종료를 확인했다.

실제 레코드 종류·개수와 문단 계층을 확인한 뒤 표/셀·도형·그림 등 각 구조의 텍스트 존재, 읽기 순서, 근거 범위, 누락 가능성을 구분한다. 단순히 표 tag의 PARTIAL 조건을 없애지 않는다. 손상/미지원/이미지/OCR 필요 파일과 업무상 검수 문서는 별도 상태로 유지한다.

새 버전 배포는 가능하지만 이 증분 자체는 상시 첨부 수집 시작이나 정책 QA 통과를 뜻하지 않는다. 정책 게시·ENFORCE·기존 데이터 적용은 정확한 범위·건수·요청/byte 상한·효과·복구 방법을 승인받은 뒤 수행한다. 기존1.0.1 운영 증거와 새 버전 검증은 구분하며, 검증되지 않은 부분을 완료로 계산하지 않는다.

14:44 KST AWS 읽기 전용 inventory는 기존 `d-MWENRAGTK`/`787c594` Succeeded·서울·SSM Online을 재확인했다. 취소 annotation은 `The operation was canceled.`이며 취소 주체/원인은 특정할 수 없다. 별도의 Node/action 지원 종료 경고를 취소 원인으로 단정하지 않는다. 새 버전의 실제 HWP 구조 진단·고정 HWPX 재검증은 배포 재개 여부를 확인한 뒤 수행한다. 브라우저는 승인된 HTTP 운영 주소의 로그인 화면이며 관리자 업무 E2E를 성공으로 계산하지 않는다. 사용한 Java/Node/gh 대기 프로세스는 종료됐고 사용자 로그인 탭만 handoff로 보존했다.
