# 첨부 DB·API 계약의 현재 검증 근거

## 판정과 범위

2026-09-16 저장소·실제 JUnit 산출물 기준의 계약 정합성 점검이다. 전체 ATT-001~062·Gate0~8과 제목→본문→첨부→관리자 최종 검증을 유지한다. DB/API 구현과 임시 환경 검증을 운영 활성화·실제 관리자 업무 E2E로 바꾸지 않는다. 전체 출시 판정은 Not ready이며 개별 계약 문서 정정으로 Gate를 자동 통과 처리하지 않는다.

계약 정정의 기준 SHA는 `85c7f65e80338e21e4e304559db02320ede1e917`, [Linux35050057694](https://github.com/FrostyCityMan/saneB/actions/runs/35050057694) success다. 후속 `9fe892c`는 제천 고정 공고의 worker 시험/CI/문서만 추가했고 production Java·추출기·migration을 바꾸지 않았다. 그 SHA의 [Linux35051444985](https://github.com/FrostyCityMan/saneB/actions/runs/35051444985)도 success이며 root2717=2451통과/266생략, 아래 별도 계약 시험군의 건수와 실패·생략0을 재확인했다. 제천 실제 파일3개→worker/임시 DB/API의 별도 JUnit3/3과 검수 필요 유지 결과는 [제천 기록](announcement-jecheon-worker-db-api-qa-2026-09-16.md)에 분리한다.

## 확인한 불일치와 정정

| 문서의 이전 설명 | 현재 직접 근거 | 정정 범위 |
|---|---|---|
| DB 상단이 V72/운영 DB 미반영으로 설명 | 최신 migration V83, Linux 순차 업그레이드 성공; 운영 기록은09-15 V83 | 저장소/시험/마지막 운영 확인 시점을 분리 |
| V73의 동시성·검수·배치·복구 SQL이 Windows 차단으로 미실행 | Linux job192건 실패·생략0 | Linux 근거로 갱신, 운영 적용 완료는 주장하지 않음 |
| V76~V81의 게시 잠금·Provider 원장·계획이 PG 미실행 | job192건 안에 실제 게시/계획/원장 경합·불변 시험 존재 | 실행 성공과 전체 공식 Provider QA 미완료를 구분 |
| V82/V83 제목에 운영 미반영 표기 |09-15 운영 DB V83 기록, 현재 운영 재조회는 인증 만료로 미실행 | 단정형 제목 제거, 날짜가 있는 운영 근거 참조 |
| catalog15참조/기대값0 | 현재 classpath catalog30참조/기대값1 | 정상 기대 공고0·전체 QA 미완료는 유지 |
| API24가 정책 게시·배치 API/UI를 미구현으로 설명 | Controller·후속 API 절·템플릿·HTTP 시험 존재 | 남은 범위를 공식 QA·승인된 운영 적용·업무 E2E로 정정 |
| API 게시 잠금 설명이 V77의18테이블에 머무름 | V81 함수에 Provider 원장·항목·계획3개 추가 | 기존18+추가3의 실제 잠금 범위를 명시 |
| ATT037/053/058의 DB 검증 자체가 차단으로 표시 | 실제 migration3건·Flyway3건 및 복합 소속 시험 통과 | DB 차단 표기를 해소하고 운영 경로 미완료는 유지 |

## 직접 확인한 실행 근거

보관 XML: `build/qa-results/run-35050057694-contracts/`. 이 디렉터리는 Git 제외된 검증 산출물이며 공개 원문·첨부를 포함하지 않는다. 아래 건수는 서로 다른 시험군이다. root의 조건부 생략을 별도 DB 실행 성공과 합쳐 일반 테스트 모두 통과로 계산하지 않는다.

| 시험군 | 결과 | 증명하는 범위 |
|---|---|---|
| root `test` |2713건=2447통과/266생략/실패0 | 단위·HTTP 등 해당 assertion; 외부 사이트/운영 아님 |
| `AnnouncementAttachmentMigrationTest` |3/3 | 빈 DB, V71→V83 순차 적용·checksum, 복합 소속/불변·역할 지문/좌표 |
| `AnnouncementAttachmentBackfillIntegrationTest` |14/14 |1001건 전체 분할·동시 예약·삭제·소속 동등성·정부24 코드 |
| `AnnouncementAttachmentJobIntegrationTest` |192/192 | 실제 SQL/trigger, lease·멱등·버전·검수·DRAFT·게시·원장·배치·원복 |
| `AnnouncementAttachmentWorkerIntegrationTest` |12/12 | 합성 HTTP/실제 격리 추출·DB/API, 실패·복구·역할 변경·검수 보호 |
| `FlywayMigrationIntegrationTest` |3/3 | V63 legacy 분류 backfill 포함 실제 migration/Mapper/서버 golden 경로 |
| 추출기/패키징/runtime/정책 부모 |88/20/1/2 통과 | 해당 합성 입력·패키징·실제 격리·부모 연결/취소/정리 |

DB 중요 assertion은 다음 현재 소스와 JUnit testcase 이름을 대조했다.

- `freshSchemaAndV71UpgradePreservePriorChecksums`: V71부터 V83까지 각각 migrate/validate, 이전 checksum 불변, 빈 DB, 기존 trigger/FK 유지, 정책 자동 ACTIVE 없음.
- `immutableEvidenceRejectsMismatchedBindingsAndCascadesWithSource`, `textRoleEvidenceRejectsForgedHashesPositionsVersionsAndPreservesCodePointBinding`: 다른 source·set·정책 근거와 위조 역할·텍스트/좌표의 거부.
- `att029SimultaneousSameKeyReturnsExactlyOneJob`, `att031TwoWorkersCannotClaimTheSameJob`, `att031ExpiredOwnerCannotHeartbeatChargeOrFinishAndAttemptsAreBounded`: 중복·경합·늦은 소유자 차단.
- `reviewConfirmationAndDraftAreVersionBoundImmutableAndNeverAutoActivated`, `concurrentSameDraftRequestCreatesOneAnnouncement`, `roleChangeCreatesNewGenerationStalesConfirmationAndPreservesExtractionEvidence`: 버전 고정 확인·STALE·DRAFT 멱등·자동 활성화 금지.
- `publicationLockBlocksProviderWritersButAllowsReadersUntilCommit`, `activeProviderWriterRejectsPublicationNowaitWithoutPartialLocks`, `publicationDatabaseRejectsReceiptOnlyOrPartialPolicySwap`: 실제 게시 잠금·역방향 충돌·부분 적용 거부.
- `providerQaEvidenceQueriesSelectLatestAttemptIncludingPendingInsteadOfPriorCompleted`, `providerQaEvidenceProjectionPreservesPendingAndFinishedRowsWithRealTimesAndHashes`: 최신 실패/대기와 과거 성공·전체 항목 조회의 구분.
- `full1001InventoryReservesBothSegmentsWithoutDuplicateOrNewArrivalAndAccountsAllDimensions`, `orderedMembershipComparisonMatchesPreviousGuardForWholeTuplesNullsDuplicatesAndOrder`: 전체 분모·V83 tuple 비교·경합 검증.

HTTP 확인: 정책 게시15/영향10/범위21, 배치24/preview18/적용18/원복16/이력12, 전체 목록19/분할20, 검수14/역할11/재시도9건의 ControllerSmokeTest XML은 모두 실패·생략0이다. 실제 보안 필터·Controller와 Service 대역의 계약 시험이며 운영 인증·실제 DB를 통과한 HTTP 서버·브라우저와 같지 않다. 실제 임시 DB/API 대조는 별도 worker 시험이며 그 MockMvc도 인증된 운영 브라우저를 대신하지 않는다.

## 운영과 남은 업무

- 마지막 운영 DB 직접 확인은 **2026-09-15 16시대 V83/실패0**이다. [운영 기준선](../deployment/attachment-runtime-baseline-2026-09-15.md)에 SSM 기록이 있다. 이번 문서 정정에서 AWS·운영 DB·브라우저를 재조회하지 않았다.
- 그때 설치된 코드는787c594/추출기1.0.1이며 첨부 worker/QA 비활성, 정책·첨부 데이터0이었다. 이 과거 관측을 현재 정상 상태 또는 최신 코드 설치로 주장하지 않는다.
- 외부 전체 Provider/형식·정상 다중 첨부 근거, 현재 ACTIVE와 정책 결합, 최신 배포 및 운영 검수·DRAFT·역할별 E2E는 미완료다. 승인된 기존 데이터 전체 처리와 최종 집계도 남는다.
- 이 점검은 계약 문서의 오래된 **상태 설명**을 정정한다. schema/API 의미·migration·정책·키워드·운영 데이터는 변경하지 않는다. 수집 모델 수나 QA 성공률을 높이기 위해 UNKNOWN/부분/미실행을 정상으로 바꾸지 않는다.
