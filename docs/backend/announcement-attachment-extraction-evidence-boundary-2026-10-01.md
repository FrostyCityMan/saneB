# 첨부 추출 근거 오류의 파일 단위 분리

## 현재 단계 / Gate

전 지역 첨부 연결 우선 작업 중 공통 오류 처리 경계를 보완했다. 최초 기준 HEAD는 `65ea41595656dfcfc0ed26a45ae02a0fe05d821c`다. 신규 지역 연결 또는 운영 검증 완료로 계산하지 않는다.

- [x] 격리 추출 응답과 DB 저장 검증의 불일치 재현
- [x] 근거 순번·범위·위치 검증을 격리 응답 수신 경계에 추가
- [x] 정상 다운로드 보존·다음 파일 처리·원본 정리 단위 검증
- [x] Linux 실제 격리 프로세스 오류/복구 시험 추가 및 합성 실행기 컴파일
- [ ] 변경 SHA의 Linux 실제 격리·DB/API 통합 검증
- [ ] 미확보 13개 수집처, 운영 반영·상시 수집·운영 E2E

## 확인한 결함과 변경

`IsolatedAttachmentExtractor.validateResult`는 offset 순서와 길이는 검사했지만 block index, 고유한 evidenceScopeId, 비어 있지 않은 locator를 완전히 검사하지 않았다. 반면 `AnnouncementAttachmentEvidenceServiceImpl.validateExtraction`은 이 조건을 요구한다. 따라서 비정상 격리 응답이 최초 검증을 통과한 뒤 checkpoint 또는 전체 집합 저장에서 거부될 수 있었다. 실제 운영 발생 빈도를 확인한 것은 아니다.

새 경계 테스트 18건 중 수정 전 15건이 실패했다. 누락·중복 index, 문자열/소수/정수 범위 초과 수치, 누락·빈값·숫자·중복·길이 초과 범위 ID, 누락·빈값·숫자 위치 정보가 기존 검사에서 거부되지 않는 경우를 재현했다.

격리 응답 수신 시 DB와 동일한 근거 순서·범위·위치 조건을 검사한다. 위반하면 기존 `selectExtraction` 오류 처리에서 원문을 버리고 고정 `FAILED` 결과만 반환한다. worker는 다운로드 성공의 byte/hash를 보존하면서 추출 품질만 FAILED로 저장하고 다음 파일을 처리한다. DB 오류·lease 상실·원본 정리 실패를 무시하는 변경은 없다.

합성 Linux 추출기에 `INVALID_EVIDENCE` 시나리오를 추가했다. 실제 격리 자식의 잘못된 응답이 FAILED가 되고, 원문 표식이 반환되지 않으며, 다음 RECOVER 추출이 정상 처리되고 임시 원본이 정리되는지 검사한다. 기존 합성 정상 응답에는 실제 생산 IPC/DB 계약의 index·evidenceScopeId·locator를 명시했다. Linux 필수 runtime suite의 최소 시험 수를 5→6으로 높였다.

HWP 파서 1.0.16·PDF/HWPX 추출 알고리즘·분류 규칙·제목 제외·관리자 최종 검증 정책은 변경하지 않았다. DB/API/Flyway 변경도 없다. 이 변경은 기존 IPC 근거 계약의 조기 검증이지 정책 완화나 자동 활성화가 아니다.

## 실행·검증

```powershell
.\gradlew.bat --no-daemon --max-workers=1 :test --tests '*AttachmentExtractorEvidenceBoundaryTest'
.\gradlew.bat --no-daemon --max-workers=1 :test --tests 'com.saneb.domain.announcementattachment.extraction.*' --tests '*AnnouncementAttachmentWorkerServiceTest' --tests '*AnnouncementAttachmentEvidenceServiceTest' --tests '*AttachmentContractWorkflowTest' :bootJar
.\gradlew.bat --no-daemon --max-workers=1 :test --tests '*AnnouncementAttachmentDiscoveryEvidenceTest' --tests '*AnnouncementAttachmentRetryEvidenceTest' --tests '*AttachmentExtractorEvidenceBoundaryTest' --tests '*AnnouncementAttachmentWorkerServiceTest'
node --test scripts/qa/attachment-contract-report.test.mjs scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs scripts/qa/attachment-collection-diagnostics.test.mjs scripts/qa/attachment-github-collection-receipts.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
git -c core.safecrlf=false diff --check
```

첫 명령은 수정 전 실패 재현이다. 수정 후 확대 검증은 157건 중 **150통과·7생략·실패0**, bootJar 생성 및 후속 UP-TO-DATE를 확인했다. 생략은 Linux runtime 6건과 Linux 전용 보안 파일 mount 1건이다. Windows에서 실행한 합성 추출기 컴파일 검사는 실제 Linux 격리 실행을 대신하지 않는다.

확대 명령의 `*AnnouncementAttachmentEvidenceServiceTest`에 해당하는 클래스가 없음을 확인해 실제 `DiscoveryEvidenceTest`·`RetryEvidenceTest`를 후속 명령에 명시했다. 후속은 **76통과·실패/생략0**(근거 경계18·발견 근거7·재시도 근거5·worker46)이다. Node는 **59통과·실패/생략0**이다. 외부 공고 요청·운영 쓰기·브라우저 실행은 0이다. 브라우저는 현재 명시 요청이 없어 정책상 미실행이다.

기존 수집 대장 **963영수증/293표본**, 현재 프로필 다운로드 확보 **210/223·미확보13**, 엄격 전체첨부 **16/223**은 그대로다. 이번에 외부 파일을 재다운로드하지 않았고 수집 프로필을 변경하지 않았다. 이 기록은 과거 다운로드 근거이지 변경된 응용 코드의 운영 검증이 아니다. 전체 goal은 미완료다.
