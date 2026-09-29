# 안산·인제 파일 응답 형식 복구와 HWP 다운로드

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드를 우선하고 개별 실패는 분리한다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지는 유지한다. HWP 추출기 개선은 보류 상태이며 이번 변경은 다운로드 응답 호환이다.

- [x] 안산 LGS-000092·인제 LGS-000132의 기존 MIME 오류 실측·복구.
- [x] 두 기관만 응답 정규화 연결. 공통 validator·전송·발견 엔진과 다른 지역 근거 보존.
- [x] 실제 Java 수집 경로에서 본문230자/447자와 HWP 각1개 다운로드 확인.
- [x] 관련 Java317통과·조건부2생략, bootJar, Node23/23,275영수증/224최신공고 재현.
- [x] 조사 원본8개919,825byte 정리, 이전 실패 보고서 보존.
- [ ] 잔여78개 수집원: 미등록43 + 등록 후 다운로드 성공 미확인35.
- [ ] 추출·구간 분석·상시 worker·운영 DB/API/UI·DRAFT·기존 데이터·운영 E2E 전체 Gate.

## 원인과 변경 범위

공식 상세·파일을 기관별 각1회 조회했다. 모두200이었고 두 파일은 HWP로 표시되며 OLE prefix를 가졌다. 안산 응답은 `application/unknown;charset=UTF-8`, 인제 응답은 `application/x-tika-msoffice;charset=UTF-8`였다. 기본 validator는 두 MIME을 허용하지 않는다. 안산 파일명 헤더는 Latin-1 해석 시 제어문자가 생겼으며 엄격한 UTF-8 복원이 필요했다. 실제 파일명·전체 헤더·원문은 문서에 복사하지 않는다.

`ObservedBinaryMimeAttachmentDiscoveryProfile`은 다음 조건을 적용한다.

1. 기존 승인된 요청·호스트·전송 예산 안에서 파일을 한 번만 수신한다.
2. 해당 기관에 설정된 실측 MIME만 정규화한다. HTML·다른 기관 MIME·설정하지 않은 구형 MIME은 추가 허용하지 않는다.
3. 안산의 unknown은 PDF/OLE/ZIP 기본 서명과 파일명을 대조한다. 인제 Office MIME은 OLE/HWP 경로만 허용하며 PDF·ZIP은 거부한다.
4. `attachment` disposition, 파일명 확장자, 경로·제어문자 검증을 통과한 뒤 기존 legacy-binary 검증 표현인 `application/x-msdownload`로 내부 정규화한다. 이 값은 서버가 보낸 MIME이 아니다. 실제 MIME은 이번 조사 대장에 별도 기록했다.
5. 기존 호출자는 이후에도 descriptor의 예상 형식과 대조한다. 안산에만 기존 UTF-8 파일명 복원 wrapper를 함께 적용한다.

공통 validator 확장 초안은 지역 전체의 실행 지문을 바꾸는 것이 테스트에서 확인되어 철회했다. 공통 validator·gateway·fingerprint·전송 코드는 변경하지 않았고 두 설정에만 adapter를 연결했다. 다른 수집원의 기존 표본과 지역 집계는 프로그램으로 동일성을 검증했다. 시흥 지문 `322e1bcba46d62483cecbbaab6a7f6102602cb173d28f22bf5227d5929fb320f`, 양구 지문 `ad236849cd3a20da25b4dc4c32e66a94927478258b1fb0e205a392dccd0e3a37`도 회귀 테스트로 유지했다.

기본 서명 검증은 문서 내부의 온전함이나 텍스트 추출 성공을 뜻하지 않는다. OLE 내부 HWP 구조·ZIP 내부 HWPX 구조는 격리 추출 단계의 책임이다. DB/migration·API/UI·추출기·운영 설정은 이번에 변경하지 않았다.

## 실제 관측

| 항목 | 안산 | 인제 |
|---|---|---|
| 공고 | ANSAN-1660335 | INJE-245926 |
| 관측 UTC | 2026-09-29T23:02:54.259916300Z | 2026-09-29T23:03:08.208465500Z |
| 관측 KST | 2026-09-30 08:02 | 2026-09-30 08:03 |
| 본문 | AVAILABLE230자·ACCEPTED | AVAILABLE447자·ACCEPTED |
| 첨부 | FOUND·complete=true·HWP1개 | FOUND·complete=true·HWP1개 |
| 다운로드 | DOWNLOADED110,080byte | DOWNLOADED81,920byte |

두 제목은 COMBINATION_MATCHED, 본문 근거는 TARGET_SUPPORT_CONFIRMED였다. 결과는 COLLECTION_ONLY_OBSERVED_NOT_APPROVED, 원본 정리true, 운영 쓰기0이다. 과거 표본의 다운로드 확인을 현재 신청 가능 공고·관리자 확정·운영 성공으로 간주하지 않는다.

- 안산 프로필 지문: `a1b1b1c5432a5dfb1e18a786d0d55c7c0e39d3236c2cf635bbb46b7d0ef8f7f1`
- 안산 파일 SHA256: `039bf09bedc5e08627b65db14d83a2155c54b9db518520db9c1b98e5f03aa08e`
- 인제 프로필 지문: `98a22d2fd986339652447d6048d6adc93c353f84c1b8aeeefec1cf27ba156186`
- 인제 파일 SHA256: `3e7feb24bb08de48abe8d485af783e83cd8d94eede3ad7cc4b4673d44c1087cf`

이전 보고서는 각각 `ANSAN-1660335-BEFORE-MIME.json`, `INJE-245926-BEFORE-MIME.json`으로 해시 일치 보존했다. 기존 실패 근거를 삭제하거나 성공으로 덮지 않고 새 관측2건을 추가했다.

## 검증·집계·정리

```powershell
# 실제 진단 fixture SANEB_ANSAN_INJE_MIME_FIXTURE=true와 기존 읽기 전용 inventory 환경변수 사용
.\gradlew.bat :test --tests '*AnsanInjeMimeCompatibilityTest' --tests '*CapitalSixthDownloadContractTest' --tests '*GangwonSecondDownloadContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCaseExecutorTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=ANSAN,INJE' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

최종 확장 실행은319건 중317통과·실패0·조건부2생략, bootJar와 실제 관측2건 성공(2분11초)이다. 생략2건은 이전 수도권6차·강원2차 HTML fixture의 조건부 테스트다. 이번 두 실파일 fixture는 모두 실행했다. 원본 정리 후에는 해당 fixture 환경변수를 켜지 않는다. Node23건 및275영수증/224최신공고 재현은 모두 통과했다.

분모는2026-09-28 15:56:12 KST 읽기 전용 스냅샷의 활성223지역 수집원이다. 최소1파일 다운로드 확인143→145/223(65.0%), 잔여80→78, 미등록43 유지, 등록 미확인37→35다. 현재 오류가 기록된 수집원은43개이며 성공 수집원과 겹칠 수 있다. 지역180+기업마당1=181프로필과236카탈로그 공고/179대상은 유지한다. 기존3표본·전체 파일 Gate는16충족/207잔여이며 전체 구현·운영 완료율과 구분한다.

진단4요청(각15초, HTML2MiB·파일10MiB 상한), 실제 관측 기관별1회(각 최대6요청/23MiB·상세1MiB)였다. 실제 관측의 본문 포함 예약은8회/5,115,392byte, 진단 포함 예약 상한은12회다. 예약 수와 실제 wire 요청 수는 구분한다. 조사 원본8개919,825byte는 승인된 두 임시 디렉터리의 정확한 경로·크기·SHA256 대조 후 개별 삭제했다. 복구에는 공식 재조회가 필요하며 보고서·해시는 보존했다. 실행기가 관측 원본을 정리했고 이번 Node·Gradle·Java도 종료했다. 기존 프로세스와 무관한 미추적 파일은 보존했다.

전체 프로젝트 테스트·추출·AWS·운영 DB·상시 유입·운영 E2E는 이번에 실행하지 않았다. 브라우저는 현재 단계의 명시적 요청이 없어 정책상 생략했다. `[skip deploy]` 작업 브랜치 범위이며 운영 배포·정책 게시·ENFORCE·기존 데이터 처리는 하지 않았다.
