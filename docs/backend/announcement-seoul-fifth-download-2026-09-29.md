# 동대문·성북·영등포 첨부 연결과 부분 성공 보존

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계이며 전체 Gate는 미완료다. 제목 → 본문 → 첨부 → 관리자 검증, 제목 제외 원문 비저장, 외부 공고 자동 활성화 금지를 유지한다. HWP 추출기 1.0.16 추가 개선은 보류한다.

- [x] 동대문·성북·영등포 시스템 프로필3개와 본문 경계 연결.
- [x] 실제 HWPX4개·HWP1개·PDF1개, 총6파일960,760byte 다운로드 및 signature 확인.
- [x] 내부망 미리보기 오류와 미지원 XLSX를 성공 파일과 분리.
- [x] Java236통과/조건부1생략, Node23·bootJar·수집 관측 실행 통과.
- [x] 기존229영수증/185공고를 보존하여 232영수증/188공고 재현.
- [x] 조사 HTML13개3,290,951byte 길이·SHA256 대조 후 개별 삭제.
- [ ] 미등록79지역 연결, 등록 후 다운로드 미확인30지역 후속.
- [ ] 전국 자동 목록 유입·상시 worker·첨부 텍스트 추출·DB/API/UI·운영 E2E.

## 지역 집계

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷이다. 이번에 운영을 조회하거나 변경하지 않았다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 실제 파일 최소1개 다운로드 확인 | 111 | **114/223 (51.1%)** |
| 다운로드 미확인 | 112 | **109** |
| 프로필 등록 지역 | 141 | **144** |
| 미등록 지역 | 82 | **79** |
| 등록 후 다운로드 미확인 | 30 | **30** |
| 첨부 오류 관측 지역 | 35 | **37** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역144+기업마당1=145프로필. 카탈로그200공고/143대상, 신규 expectation=null, 기존 승인 기대값1개를 유지한다. 다운로드 확인률은 전체 개발·운영 완료율이 아니다. 오류 지역은 성공 지역과 겹치므로 잔여 지역에 더하지 않는다.

## 실제 관측

| 지역 / 고정 공고 | 본문 | 파일 결과 | 별도 상태 |
|---|---|---|---|
| 동대문 LGS-000007 / 22587 | 972자, A그룹 검수 필요 | HWPX2개131,833byte | 미리보기 내부망 주소 미요청, 발견 부분 실패 |
| 성북 LGS-000009 / 43006 | 505자, 대상·지원 조합 확인 | HWPX2개·HWP1개232,497byte | 발견3개·다운로드3개 |
| 영등포 LGS-000020 / 38256 | 503자, 대상·지원 조합 확인 | PDF1개596,430byte | XLSX1개 미지원·미다운로드 |

본문의 ACCEPTED는 규칙상 후보 판정이며 최종 확정·공개가 아니다. 동대문·영등포는 `COLLECTION_ONLY_PARTIAL_NOT_APPROVED`, 성북은 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`다. 관측 명령 성공과 전체 첨부 처리 완료를 구분한다. 모든 파일 역할은 UNKNOWN이며 추출·정책 기대값 승인·운영 쓰기는 없다.

프로필 지문:

- 동대문 `aeeb056109ee0f3e0bf79f36d5a20e6ead2e7764f0ce9745e3e852e8371db7ac`.
- 성북 `75a02fae81ec644a68f60af9b6a435ecec8d34b75254555a2422d737791ba3d1`.
- 영등포 `687d03dccf0ccf7cd35338e1307c292d73ee538fb7233c36951a844807f492a3`.

파일별 크기·SHA256·실패 상태와 관측 시각은 `attachment-collection-receipt-index-2026-09-28.json`의 해당 공고 근거를 참조한다. 원문·연락처·내부망 주소는 이 문서에 복사하지 않는다.

## 구현 계약과 조사 결과

`SeoulFifthNoticePage`는 세 기관의 공식 표에서 제목·본문·첨부 셀을 분리한다. 메뉴·담당자 메타데이터·첨부명이 본문에 섞이지 않도록 고정 경계를 사용한다. `SeoulFifthAttachmentDiscoveryProfile`은 고정 수집원/parser/원문 hash와 공개 HTTPS host/path/query를 검증한다.

동대문은 공식 `downForm`의 공개 파일 서버 POST 및 확인한 세 필드만 사용한다. 미리보기의 내부망 URL은 요청하지 않으며, 오류가 있어도 유효한 공개 다운로드 descriptor2개를 보존한다. 성북·영등포는 공식 파일 GET 링크와 세 query 값만 사용한다. 미리보기는 파일 식별 대조만 수행하고 요청하거나 스크립트를 실행하지 않는다. 미지원 XLSX는 발견 목록에서 지우거나 처리 성공으로 승격하지 않는다.

공식 원본 HTML과 합성 fixture로 정상3형식·미지원·부분 실패·중복·충돌·10파일 상한·외부 host·HTTP·추가/중복 query·잘못된 메서드·변경된 다운로드 폼·본문/제목 경계를 검증했다. 기존 공통 프로필 지문·migration·DB/API/UI·추출기·운영 설정은 변경하지 않았다.

성북 검색은 `searchCnd2=notAncmtSj`가 있어야 제목 검색이 적용되었다. 이를 누락한 최초2검색은 일반 목록을 반환했으므로 지원 공고 부재 근거가 아니다. 동대문의 소상공인 검색에서 나온 채용 공고는 제목에서 걸러 상세를 요청하지 않았다. 이후 지원 검색에서 A그룹 제조 지원 공고를 선택했다.

## 실행·검증·정리

```powershell
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_AUDIT='true'
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_RECEIPT='C:\PersonalProject\saneB\build\qa-results\target-inventory-20260928-receipt.txt'
$env:SANEB_SEOUL_FIFTH_SURVEY_FIXTURE='true'
.\gradlew.bat :test --tests '*SeoulFifthDownloadContractTest' --tests '*SeoulFourthDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=DONGDAEMUN,SEONGBUK,YEONGDEUNGPO' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git -c core.autocrlf=false diff --check
```

첫 실행2분6초: 새 테스트가 PUT 요청 생성 시 발생하는 예외를 false 반환으로 잘못 기대하여3건 실패했다. 기존 요청 객체의 차단을 유지하고 예외 검증으로 수정했다. 첫 실행은 외부 관측에 도달하지 않았다. 두 번째1분58초: Java237건 중236통과/과거 서울4차 원본 fixture1건 조건부 생략, JAR 첫 생성/두 번째 UP-TO-DATE, 실제 수집 관측3건 완료. 모든 관측을 재실행 없이 보존했다. 환경변수는 실행 전 값을 finally에서 복원했다.

조사13요청(동대문4·성북5·영등포4): 각15초/2MiB·TLS 검증·자동 redirect0. Java 관측은 지역당6요청/23MiB, 본문을 포함한 예약 합계15회·8,014,072byte다. 조사 포함 예약 상한28회이며 예약 수는 실제 HTTP 호출 수와 다르다. 파일 관측의 finally 정리와 조사 원본13개 길이·hash 대조 후 삭제를 확인했다. 원본 삭제 후 실제 HTML fixture 환경변수를 켜지 않는다.

전체 테스트·첨부 텍스트 추출·Linux worker/DB/API·운영 UI·배포·정책 활성화·기존 데이터 재분류는 이번 검증 범위가 아니다. 브라우저는 현재 지역 확대 요청에 별도 지시가 없어 사용자 정책상 미실행했다. 검증한 범위만 `[skip deploy]`로 커밋·푸시하고 다음 미등록 지역 연결을 계속한다.
