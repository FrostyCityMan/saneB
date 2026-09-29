# 곡성·진도 첨부 연결과 수집 오류 분리

## 현재 단계 / Gate

지역별 첫 첨부 수집 확대를 진행한다. 제목 → 본문 → 첨부 → 관리자 최종 검증 정책을 유지하면서, 본문·개별 파일 오류가 다른 정상 파일과 다음 지역 수집을 막지 않게 한다. HWP 추출기 1.0.16 추가 개선은 보류 상태다.

- [x] 곡성·진도 공식 첨부 프로필 2개, 공통 처리기 1개 추가.
- [x] 곡성 HWPX 1개 다운로드·signature 검증 성공.
- [!] 곡성 기존 본문 경로 HTTP 오류와 진도 파일 형식 검증 실패를 별도로 기록.
- [x] 최초 계약91건, 확장 계약121건, Node23건, bootJar 검증.
- [x] 미지원 확장자 버튼 중복 처리 보완 후 계약121건·bootJar·관측2건 재검증. 130영수증/최신100공고 재현.
- [ ] 나머지 지역 수집 확대와 전체 Goal 분석·운영 E2E.

지역 분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 이번 작업에서 운영 DB·설정·정책을 조회하거나 변경하지 않았다. 파일 전송만으로 운영 저장·첨부 분석·정책 승인 완료를 의미하지 않는다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 다운로드 관측 지역 | 48 | **49/223 (약22.0%)** |
| 다운로드 미관측 지역 | 175 | **174** |
| 로컬 프로필 등록 지역 | 58 | **60** |
| 미등록 지역 | 165 | **163** |
| 등록 후 다운로드 미관측 | 10 | **11** |
| 등록 지역 중 오류 근거 있음 | 9 | **10** |
| 기존 3표본·전체 파일 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

로컬 프로필은 지역60+기업마당1=61개, 카탈로그는115참조/지역57개다. 약22.0%는 첫 다운로드 관측률이며 전체 개발 진행률이 아니다. 기존 엄격 Gate는 다음 지역 착수 조건으로 사용하지 않는다. 미등록 지역의 사전 조사 오류는 위 등록 지역 오류10개에 포함되지 않는다.

## 실제 관측과 남은 오류

| 지역 | 공고 | 결과 |
|---|---|---|
| 곡성 LGS-000184 | 34854, 2026년 소상공인 온라인 마케팅 비용 지원 사업 알림 | HWPX 85,169byte 다운로드·signature 성공. 본문 `HTTP_STATUS_ERROR`는 별도 유지 |
| 진도 LGS-000198 | 25159, 2026년 소상공인 융자금 이차보전 지원사업 공고 | 첨부 1개 발견, 148,992byte 전송. `FILE_SIGNATURE` 단계 `TRANSPORT_FAILED`, 성공 집계 제외 |

진도는 본문 3,318자를 확보했고 중간 판정은 `BODY_GROUP_B_MATCHED`다. 본문 정제 품질과 첨부 분석은 검증하지 않았다. 진도 파일 오류는 HWP 파싱 오류로 단정하지 않는다. 파일 형식 검증 단계에서 실패했으며 이 실행에서는 추출기를 호출하지 않았다. 후속 MIME·파일명·signature 진단은 별도 업무로 남긴다.

곡성의 현재 목록 파서 `SAFE_SAEOL_EMINWON`은 대표 누리집에 새올 상세 경로를 조합한다. 실제 공식 목록의 `searchDetail`은 `/board/GosiView.do`로 이동한다. 신규 프로필은 정확히 일치하는 기존 source URL의 공고 ID만 공식 상세 경로에 연결하며 source identity는 보존한다. 임의 호스트·임의 경로 변환은 허용하지 않는다. **기존 본문 수집기의 경로는 이번에 수정하지 않았으므로 본문 오류가 남는다.** 공식 주소 직접 입력과 기존 파서 형식 둘 다 계약 테스트한다.

사전 조사에서는 구례 검색 연결 시간 초과(목록 성공 후 검색 실패, 총2요청), 화순 HTTP302(1요청, 미추적), 해남 HTTP400(1요청), 함평 목록 확보·표본 미선정(1요청)을 확인했다. 이 지역에 첨부가 없다는 의미는 아니다. 반복 시도나 접근 우회 없이 다음 지역으로 넘긴다.

## 구현과 검증 경계

- `JeonnamCountyAttachmentDiscoveryProfile`: 곡성 공식 `board_view > file_down`의 파일명 링크·다운로드 버튼을 중복 제거한다. 공개 다운로드 POST 인자는 요청 메모리에만 보관하며 locator에는 공고 ID와 파일 식별 hash만 저장한다.
- 진도는 `board_view > view_foot > board_file`의 `/doc/dataDown.jsp`만 다운로드한다. 동일 파일의 `/doc/indexDown.jsp` 미리보기는 다운로드하지 않는다. 공고·파일 번호가 다른 링크와 미해석 제어는 오류로 보존한다.
- 정확한 source/목록 parser 결합, HTTPS·호스트·경로·query 검증, 최초 요청과 동일한 후속 요청만 허용, 파일10개 제한을 유지한다.
- 미지원 확장자는 `downloadAllowed=false`로 보존한다. 미지원 파일의 다운로드 버튼 때문에 발견 오류가 추가 발생하지 않도록 보완했다.
- 카탈로그에 승인되지 않은 참조2건만 추가했다. 기존 expectation1개를 보존하며 신규 기대값·정책 자동 승인은 없다.
- 기존 migration·DB/API·UI·운영 정책·외부 공고 자동 활성화 조건은 변경하지 않았다.

```powershell
.\gradlew.bat :test --tests '*JeonnamSecondDownloadContractTest' --tests '*JeonnamFirstDownloadContractTest' --tests '*AttachmentProviderQaCatalogTest' --no-daemon
.\gradlew.bat :test --tests '*JeonnamSecondDownloadContractTest' --tests '*JeonnamFirstDownloadContractTest' --tests '*JeonbukSecondDownloadContractTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=GOKSEONG,JINDO' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
```

최초 로컬91건 성공(1분15초), 확장121건·bootJar·관측 harness2건 성공(1분31초), Node23건 성공. **관측 harness2건 성공은 실파일2건 성공을 뜻하지 않는다. 곡성1성공·진도1실패를 각각 확인했다.** 최초 결과는 `-INITIAL.json`으로 보존한다. 후속 관측은 같은 표본의 최신 근거이며 지역·공고 수를 중복 집계하지 않는다.

최종 재검증121건·bootJar·관측2건은 1분36초에 완료했다. 실패·오류·skip0이며 파일 결과는 곡성 성공·진도 형식 검증 실패로 동일하다. 최초/최종 각각 동일 binary hash를 확인했다. 영수증130개/최신100공고와 지역 대장의 재현이 일치하며 기존126영수증·실행 metadata를 보존했다. 대장의 현재 inventory hash와 importedReceiptCount에 남아 있던 과거 값을 새 기준선에 맞췄다.

공개 페이지 조사10요청(GET9·POST1), 요청당15초/연결7초/1MiB/자동 redirect0/TLS 검증 유지. 관측은 실행당 공고별 최대6요청·23MiB로 제한했다. 최초+최종 관측 예약16회/9,296,226byte, 조사 포함 예약26회다. 지역별 곡성10/30·진도11/30요청, 나머지 조사 지역은 위 횟수를 사용했다. 원본 HTML9개/920,060byte와 관측 상세·파일 원본을 삭제했다. 원문 복구 사본은 없으며 hash·비식별 영수증만 남긴다. 단발 Node·Gradle 작업은 종료했고 기존 사용자 프로세스는 건드리지 않았다.

전체 프로젝트 테스트·Linux worker 임시 DB/API·운영 배포·운영 브라우저 E2E는 이번에 실행하지 않았다. 브라우저는 현재 지역 연결 요청에 명시적 지시가 없어 사용자 정책에 따라 생략했다. 변경은 로컬 구현·공개 자료 한정 QA이며 `[skip deploy]` 배포 제외 범위다.
