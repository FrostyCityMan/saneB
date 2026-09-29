# 전북권 추가 5지역 연결과 4지역 실제 첨부 수집

## 현재 단계 / Gate

전 지역의 첫 첨부 수집을 우선한다. 개별 실패는 오류로 보존하고 다음 지역 연결을 계속한다. HWP 추출기 1.0.16 추가 개선은 보류하며, 본문·첨부 구간 분석·worker DB/API·최종 관리자 검증·운영 E2E 전체 목표는 유지한다.

- [x] 군산·정읍·남원·부안·고창 시스템 프로필 5개 연결.
- [x] 군산·정읍·남원·고창 HWP 2개·HWPX 2개 다운로드와 signature 확인.
- [x] 5지역 본문 확보. 첨부 수집 성공 및 최종 승인과 별도 기록.
- [!] 부안 첨부 발견 성공 / 파일 다운로드 `ATTACHMENT_PATH_NOT_APPROVED`. 다운로드 성공 0개.
- [!] 김제 공식 게시판 반복 리디렉션 관측. 미등록·미수집 유지.
- [x] 로컬 계약 114건·Node 23건·bootJar·116영수증/93공고 재현 검증.
- [ ] 나머지 지역 연결, 구간 분석·운영 배포·운영 E2E 등 전체 Goal Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**의 활성 223지역이다. 이번에는 운영 대상·설정·DB를 조회하거나 변경하지 않았다.

| 지표 | 이전 | 현재 |
|---|---:|---:|
| 실제 다운로드 관측 지역 | 39 | **43** |
| 다운로드 미관측 지역 | 184 | **180** |
| 로컬 프로필 등록 지역 | 48 | **53** |
| 미등록 지역 | 175 | **170** |
| 등록 후 다운로드 미관측 | 9 | **10** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

43/223(약19.3%)는 첫 파일 다운로드 관측률이다. 전체 Goal 진행률이나 운영 적용률이 아니다. 현재 등록 프로필 근거의 발견/파일 오류 지역은 9개이며, 김제 같은 미등록 지역의 조사 오류를 모두 포함하는 지표는 아니다. 기존 3표본 Gate는 보존하되 다음 지역 진입 조건으로 삼지 않는다.

## 실제 관측 결과

| 지역 / source | 고정 공고 | 첨부 결과 | 본문 |
|---|---|---|---|
| 군산 / LGS-000165 | 71315 영세 소상공인 카드수수료 | HWP 118,272byte | 496자 확보 |
| 정읍 / LGS-000167 | 50232 안정지원금 미신청자 지원사업 | HWPX 90,953byte | 378자 확보 |
| 남원 / LGS-000168 | 33037092911b4bd8be3b8c15657d8aef 희망더드림 특례보증 | HWPX 64,490byte | 786자 확보 |
| 부안 / LGS-000177 | 363827 카드수수료 추가 지원 | 발견 FOUND, 파일 다운로드 경로 검증 실패 | 302자 확보 |
| 고창 / LGS-000176 | 807493 카드수수료 지원사업 | HWP 80,896byte | 423자 확보 |

성공 파일 4개 **354,611byte**. 관측 작업 JUnit 5건은 모두 통과했지만, 이는 오류 분리 동작을 포함한 harness 통과다. **실제 파일 성공은 4/5지역이며 부안은 실패**다. 부안은 `COLLECTION_ONLY_PARTIAL_NOT_APPROVED`이고 다운로드된 파일은 없다. 정상 후보·첨부 없음·파일 수집 성공으로 바꾸지 않는다. 경로 검증을 완화하거나 반복 외부 요청하지 않았다.

김제는 기존 메뉴 GET 302, HEAD의 공식 이동 주소 확인, 이동 주소 GET 302 후 동일 주소를 다시 가리켜 중단했다. 3요청의 사전 조사 오류로 기록했으며 첨부 프로필이나 파일 수집 성공으로 계수하지 않았다.

## 구현과 계약

군산·정읍은 기존 `SaeolGetAttachmentDiscoveryProfile`을 재사용한다. 저장된 HTTP 원문 identity를 유지하면서 실측한 동일 호스트·경로를 HTTPS로 요청한다. HTTP 다운로드는 승인하지 않는다.

남원·부안·고창은 `JeonbukBoardAttachmentDiscoveryProfile`을 사용한다. 공식 제목 구조와 첨부 영역, 고정 게시판·기관·공고·파일 ID를 확인한다. 동일 공고 파일의 중복 다운로드·미리보기만 보조 링크로 구분하며 미리보기 JavaScript는 실행하지 않는다. 미해석 링크·다른 공고·다른 호스트·추가 스크립트는 오류로 남기고 이미 식별한 정상 파일은 유지한다. 전체 파일이 없는 것으로 판단하려면 공식 첨부 영역이 확인되어야 한다.

DB migration·API 계약·운영 정책·자동 활성화 동작은 변경하지 않았다. 신규 QA 참조 5건의 expectation은 모두 null이다. 현재 로컬 등록은 지역53+기업마당1의 54프로필이며, 카탈로그는 108참조/지역50개다. 기존 저장 기대값 1개를 그대로 보존하고 신규 정상 승인은 추가하지 않았다. 남원 QA caseCode만 대문자 규칙을 적용하며 실제 UUID·원문 URL은 변경하지 않았다.

## 검증 및 요청 예산

```powershell
.\gradlew.bat :test --tests '*JeonbukSecondDownloadContractTest' --tests '*JeonbukFirstDownloadContractTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar --no-daemon
.\gradlew.bat :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=GUNSAN,JEONGEUP,NAMWON,BUAN,GOCHANG' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
```

계약 114/114, 실패·오류·skip 0, bootJar 성공(2분38초). 앞선 테스트 메서드명 컴파일 오류와 카탈로그 소문자 QA 코드 오류는 수정 후 재검증했다. 관측 harness 5/5 성공(1분22초), 실제 다운로드 4성공/1실패. Node 23/23. inventory export용 환경은 보관된 읽기 전용 영수증으로 임시 설정한 뒤 복원했다. 재현 결과 **116영수증/최신93공고**가 일치하며 과거 영수증과 실행 metadata를 보존했다.

- 사전 조사 24요청(GET18·POST2·HEAD4), 요청당15초/1MiB/자동 redirect0/TLS 검증 유지.
- 관측 상한은 공고별6요청/23MiB. 실제 예약22회/11,248,744byte. 본문 시도5·상세5·파일 다운로드 흐름5이며, 흐름 수를 redirect 포함 실제 HTTP 횟수와 동일하게 표현하지 않는다.
- 조사+관측 예약 합계46회. 지역별 군산7/30·정읍7/30·남원9/30·부안11/30·고창9/30·김제3/30.
- 조사 HTML20개/1,299,036byte는 삭제했다. 관측 원본도 harness에서 삭제했고 전 결과 `originalFilesRemoved=true`다. 원문 복구용 사본은 없으며 비식별 결과·hash만 보존한다.
- 운영 변경0, 추출기 실행0, 원격 서버 QA0, 브라우저 실행0. 사용한 단발 Node·Gradle은 종료했다.

전체 프로젝트 테스트·Linux worker DB/API·운영 배포·운영 브라우저 E2E는 이번에 실행하지 않았다. 브라우저는 현재 지역 연결 작업 범위에서 사용자 정책에 따라 생략했다. 상세 근거는 [영수증 index](attachment-collection-receipt-index-2026-09-28.json)의 `jeonbukSecondDownloadRun`에 기록했다.

다음은 미등록170지역 연결 확대다. 부안 경로 검증 실패·김제 반복 리디렉션·과거 순창400·본문 호스트 오류·미지원 형식·추가 표본은 별도 후속 오류로 유지한다.
