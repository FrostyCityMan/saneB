# 금산 bare HWP 응답 호환 및 실제 첨부 재검증

## 현재 단계 / Gate

- [x] 직전 강남·거창 파일 확보를 진전으로 분류하고 금산 MIME 오류 보완
- [x] 금산 전용 bare hwp 처리, 공통 MIME·요청 경계 유지
- [x] 금산 HWP 2개·265,728바이트 다운로드 및 signature/파일명 검증
- [x] 좁은 회귀 39개 중 37통과·2조건부 생략, 추가 공통/worker 회귀87통과
- [x] 실제 관측1통과·bootJar 성공·Node23통과·656영수증/285최신 표본 재현
- [~] 현재 프로필 기준 첨부 확보200/223(89.7%), 미확보23개 수집원
- [ ] 첨부 내부 구조·텍스트 추출·정책 QA·상시 운영·브라우저 E2E

기준 HEAD `6446231ce7035ce59627f8bfad614b791b52bc42`. 전 지역 첨부 발견·다운로드 우선과 HWP 추출기 고도화 보류를 유지한다. 운영 DB·worker·정책·ENFORCE·재분류·배포 변경은 없다. 브라우저는 현재 명시 지시가 없어 정책상 미실행이다.

## 원인과 변경 경계

직전 회차 금산의 고정 공고 `4df078b6fceb5d17b3162ce865ecbf3b`는 본문·파일 발견과 전송은 성공했으나 `ATTACHMENT_CONTENT_TYPE_MISMATCH`로 실패했다. 공식 첫 파일을 직접 대조한 결과 원시 Content-Type은 표준 MIME이 아닌 `hwp`였으며, OLE signature와 Java 실패 보고서의 파일 해시가 일치했다. 해당 진단 요청26회는 직전 회차 기록이며 이번 회차의 별도 조사 요청은0이다.

기존 금산 `BARE_HWPX` 호환 처리와 공통 `AttachmentFileTypeValidator`는 수정하지 않았다. 금산에만 `GeumsanFileResponseAttachmentProfile`을 연결해 기존 응답 처리 후 bare `hwp`인 경우를 추가 검증한다.

1. 기존 공식 URL·host·query·동일 요청 allowlist와 요청/바이트 한도를 그대로 사용한다.
2. bare hwp 응답은 OLE signature, attachment disposition, `.hwp` 파일명 일치를 모두 요구한다.
3. 통과 후에만 내부 검증 표현을 기존 legacy MIME으로 정규화한다. 서버가 표준 MIME을 반환했다고 주장하지 않는다.
4. 최종 공통 검증에서 descriptor의 예상 형식과 다시 비교한다. HWPX/PDF로 표시된 다른 형식을 HWP로 바꿔 승인하지 않는다.
5. HTML·다른 signature·inline·파일명 누락/경로·확장자 불일치·외부 호스트는 실패를 유지한다. 실패한 임시 바이너리 정리도 검증했다.

기존 bare hwpx·정상 MIME 처리, UTF-8 disposition 설정, 상세2MiB·한 파일 한 요청은 보존한다. OLE signature는 내부 HWP 유효 구조·텍스트 추출 성공의 증거가 아니며 추출기는 실행하지 않았다.

## 실제 관측

관측은 2026-10-01 04:01 KST의 `GEUMSAN-4DF078B6FCEB5D17B3162CE865ECBF3B-GEUMSAN-BARE-HWP-20261001.json`이다. 본문411자 AVAILABLE, 상세 식별true, 첨부FOUND/complete=true, 표시2개/처리2개 모두 DOWNLOADED/HWP다.

| 파일 | 바이트 | SHA-256 |
|---|---:|---|
| 공고문 | 190,976 | b80c7a09c256e4a8781956c7a8f26b40c5afba71e445c18b6954663ecf810b8f |
| 신청서 | 74,752 | 8312fca4381891f6ffb646c359b1a015fe7e4693eb0c5df86b759417d2edac66 |

두 해시 모두 직전 형식 검증 실패 보고서와 같다. 파일마다 transportInvocations=1/completedTransports=1이다. 최종 상태는 COLLECTION_ONLY_OBSERVED_NOT_APPROVED이며 추출·정책 QA·기대값 승인은 false다. 최대6요청·26MiB, 본문 포함 예약5회·6,450,688바이트, 원본 정리true, 운영쓰기0이다.

## 검증과 초기 실패

초기 좁은 검증은34개 중1개 실패였다. 금산 처리 오류가 아니라 기존 `limitsAndNeighborProfilesRemainUnchanged`의 오래된 서울 프로필 지문이 실패했다. 이번 변경 전에 저장해 둔 현재 인벤토리와 대조해 서울·서울중구·부여·고성의 기대 지문이 이미 오래된 값임을 확인하고 갱신했다. 용산·창원 불변 검증도 추가했다. 새 금산 처리로 이웃 지문이 변한 것처럼 숨기지 않았다.

수정 후 좁은 검증39개 중37통과·2조건부 생략, 실제 관측1통과와 bootJar 생성은1분6초에 성공했다. 생략은 로컬 조사 fixture를 요구하는 LegacyThree measuredDownloadRequiresSignatureAndMatchingAttachmentName과 ChungcheongFifth actualOfficialBoundaries다. 실제 새 금산 파일은 별도 공식 관측으로 검증했다. 추가 회귀는 AttachmentPinnedDownloadClientTest10개·AttachmentProcessingFlowTest37개·AnnouncementAttachmentWorkerServiceTest40개, 총87개 모두 통과(26초)했다.

```powershell
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_AUDIT='true'
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_RECEIPT='build/qa-results/target-inventory-20260928-receipt.txt'
.\gradlew.bat --no-daemon :test --tests '*LegacyThreeMimeCompatibilityTest' --tests '*ChungcheongFifthDownloadContractTest' --tests '*RecoveredSupportDownloadContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProfileDownloadFlowTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=GEUMSAN_SUPPORT' -PsanebCollectionReportLabel=GEUMSAN-BARE-HWP-20261001 -PsanebCollectionWindowsTrust=true
.\gradlew.bat --no-daemon :test --tests '*AttachmentPinnedDownloadClientTest' --tests '*AttachmentProcessingFlowTest' --tests '*AnnouncementAttachmentWorkerServiceTest' :bootJar
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git -c core.autocrlf=false diff --check
```

실행 시 inventory 환경변수는 finally로 해제했다. 첫 명령의 AttachmentProfileDownloadFlowTest 패턴에 해당하는 별도 클래스는 없으며 그 이름의 테스트를 통과했다고 세지 않았다. 요청 경계·실패 원본 정리는 LegacyThree와 실제 존재하는 AttachmentPinnedDownloadClientTest에서 검증했다. 전체 Java 테스트나 DB 통합/운영 E2E 통과를 주장하지 않는다.

## 지문과 대장

기존 운영 대상 스냅샷을 코드와 다시 대조했으며 새 운영 조회는 없다. 활성223개·244개 전체 지역·2026-09-28 15:56:12 KST 시각·카탈로그292참조를 유지했다. 모든 target 행을 비교한 결과 금산 LGS-000156의 profileHash만 변경됐고 다른 행은 동일했다.

- 금산 이전 지문: `e1ca2cda4cee2924bd37005d33f69a327698893215c28b407c37a40e0eff4535`
- 금산 새 지문: `4000c6cd907d84c16a962fae272aa5323d8a4762ea527f299d0e3c1c4f60f1ca`
- 인벤토리 SHA-256: `508f7f12028326fd0cd6b2b32c00ba84390c74fa02cefccdf0465bc26b2cd62e` → `f4b1e5ab5c1ed61ee1072546b1809343546555cd6497b489047f537aef07d60e`
- 관측 클래스 지문: `caeef0d8ae1727e6b4ceb96c6ab68fa1440d7dfd7549c87b297b906e60e2b148` 유지. 응용 변경은 위 프로필 지문에 반영한다.

기존655영수증은 보존하고 새1개를 추가했다. 최신285표본 중 새 금산 공고1개만 갱신했으며284개는 동일하다. 이전 금산 실패 영수증·오래된 공고 실패 표본을 삭제하지 않고 당시 지문을 유지한다. 새 지문으로 과거 성공·실패를 임의 재인증하지 않는다. 대장의 `geumsanBareHwpCompatibilityRun`에 근거를 기록했다.

현재 지문 첨부 확보199→200/223(89.7%), 미확보24→23이다. 오류가 있는 현재 지문 수집원은33→32다. 이는 금산 구지문 오류까지 해결됐다는 뜻이 아니다. 과거 포함204/223, 최초 미확인19, 엄격한 3공고 전체 첨부 집합 Gate16/223은 유지한다. 이전 공고의 구지문·미해결 관측 때문에 금산 지역 전체 Gate는 완료하지 않는다.

남은23개는 최초 미확인19개와 최신 미확보4개(미추홀·아산·봉화·남해)다. 운영 정책 승인·ENFORCE·DB/API/UI/E2E, AWS 인증 갱신 대기, 과거 임시 CA/HTML 자원 정리 차단은 별도 미완료 상태다. 이번 수집 개선을 운영 반영이나 전체 goal 완료로 표현하지 않는다.
