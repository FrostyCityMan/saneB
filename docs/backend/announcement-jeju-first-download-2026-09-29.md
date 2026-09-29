# 제주시·서귀포 첨부 연결 및 첫 파일 수집

## 현재 단계 / Gate

정상 파일부터 수집하고 개별 오류는 분리해 다음 지역을 진행한다. 오류 전부 해결이나 3표본 확보를 다음 지역 착수 조건으로 삼지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지, HWP 추출기1.0.16 추가 개선 보류는 유지한다.

- [x] 제주시 전용 공식 첨부 영역 연결, 서귀포 기존 새올 처리기 재사용.
- [x] 공식 표본2공고의 본문 확보·발견·다운로드 성공: HWPX2개·HWP1개, 총546,717byte.
- [x] 계약155건·Node23건·bootJar 통과,168영수증/127공고 재현.
- [x] 조사 원본10개647,454byte 삭제, 이전 근거와 사용자 변경 보존.
- [ ] 미등록136지역 연결, 등록 후 미관측18지역 후속.
- [ ] 전국 목록→상시 worker, 본문 정제·추출·DB/API·정책 승인·운영 E2E Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 이번 작업에서는 운영 설정을 새로 조회하거나 변경하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 첫 파일 다운로드 관측 지역 | 67 | **69/223 (약30.9%)** |
| 다운로드 미관측 지역 | 156 | **154** |
| 로컬 프로필 등록 지역 | 85 | **87** |
| 미등록 지역 | 138 | **136** |
| 등록 후 다운로드 미관측 | 18 | **18** |
| 등록 지역 중 발견·파일 오류 근거 있음 | 22 | **22** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

다운로드 관측률은 전체 개발·운영 완료율이 아니다. 지역87+기업마당1=88프로필, 카탈로그142참조/지역84개다. 신규 expectation은 null이고 기존 승인 기대값1개를 유지한다. 이전166영수증·125공고의 profile hash와 기존 실행 metadata는 변경하지 않았다. 제주도 본청 LGS-000242는 MANUAL_ONLY·비활성이라 활성223지역 분모에 포함하지 않으며 요청·활성화하지 않았다.

## 실제 관측 결과

| 지역 / 공고 | 본문 | 발견 / 다운로드 | 파일 |
|---|---:|---|---|
| 제주시 LGS-000243 / 108927 | 668자 | 2 / 2, 발견 완료 | HWPX121,897byte +77,684byte |
| 서귀포 LGS-000244 / 71884 | 280자 | 1 / 1, 발견 완료 | HWP347,136byte |

두 공고 모두 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`다. 제목은 고정 공식 표본과 변경하지 않은 DRAFT seed 조합 검증을 통과했고 본문 중간 판정은 `TARGET_SUPPORT_CONFIRMED`다. 최종 운영 정책의 판정·승인 근거가 아니며 텍스트 추출, 전체 문서 분석, 운영 DB/API 저장은 검증하지 않았다. PK/OLE signature와 형식 검증의 성공이 내부 문서 구조·텍스트 추출 성공을 뜻하지 않는다.

프로필 hash:

- 제주시 `df7cb5ce17aad7cffe1e30047a20fd1c09302a0ebd134d73d4dbd35481795e59`
- 서귀포 `73a96e316b485f1bdb86c1fc8bdc8c75a6bb28d60714980f7392da1b57fb5eea`
- 관측 producer class `18922b43388135fba820cc0e111c76d60ea1f173df8784929e1ee368c8d17ded`

## 연결 계약

- 제주시 `LOCAL_JEJUSI_GET_V1`: `/information/intro/notice.do`의 공식 제목 및 첨부 `div.file > dl > dd`만 읽는다. `goDownLoad`의 문자열3인자를 파싱하고 공식 `eminwon.jejusi.go.kr`의 HTTPS `FileDown.jsp` 요청으로 구성한다. 스크립트는 실행하지 않는다.
- 실제 관측된 바로보기 링크는 같은 파일3인자·공고 번호가 일치하는 짝만 인식한다. 바로보기 경로는 다운로드 허용 대상이 아니고 실제 요청하지 않는다. 공식 파일 아이콘만 인식하며 미확인 링크·요소는 오류로 남기고 정상 descriptor를 보존한다.
- 서귀포 `LOCAL_SEOGWIPO_GET_V1`: 기존 `SaeolGetAttachmentDiscoveryProfile`에 공식 `eminwon.seogwipo.go.kr`·LGS-000244·SAFE_SAEOL_EMINWON 바인딩을 추가했다. 기존 공통 엔진은 수정하지 않았다.
- source/parser/원문 URL hash, 고정 호스트·경로·query, HTTPS443, 동일 요청 redirect,10파일 상한, 미지원 형식 분리, 역할 UNKNOWN, 형식·signature 대조를 유지한다. MIME/TLS 정책 완화는 없다.
- V61의 제주시·서귀포 URL 구조와 공식 상세를 사용했지만 이번 harness는 **고정 공고에서 시작**한다. 전국 목록 수집→worker 자동 유입이나 운영 상시 성공으로 확대 해석하지 않는다.
- migration·DB/API·화면·운영 정책·worker 설정·HWP 추출기는 변경하지 않았다.

## 요청량 / 원본 정리

목록·검색·상세 조사9회(GET8·읽기 전용 검색 POST1), 요청당15초/연결7초/1MiB/자동 redirect0/TLS 검증 유지. 서귀포의 공식 리디렉션과 iframe 링크를 확인해 각 경로를 명시적으로 요청했다. 비공개 인증·관리자 상태를 변경하지 않았다.

관측 예약 상한은 제주시5+서귀포4=9회, 본문 포함 예약byte4,913,073이다. 조사 포함18회 상한이며 예약 수를 실제 HTTP 요청 수와 동일시하지 않는다. 관측당 최대6요청·23MiB 제한이었다. 추가 반복 없이 다음 지역으로 진행한다.

조사 원본10개647,454byte와 관측 임시 파일을 삭제했다. 복구 사본 없이 hash·비식별 metadata만 보존한다. 단발 Node·Gradle은 종료했고 기존 사용자 프로세스·`output/`·`scripts/qa/__pycache__/`는 유지한다.

## 검증 명령 / 결과

```powershell
.\gradlew.bat :test --tests '*JejuFirstDownloadContractTest' --tests '*GyeongnamThirdDownloadContractTest' --tests '*GyeongnamSecondDownloadContractTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=JEJUSI,SEOGWIPO' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

Inventory는 로컬 보관 `target-inventory-20260928-receipt.txt`를 사용했다. 계약155건 실패·오류·skip0, bootJar 및 관측2건을 포함해1분45초 `BUILD SUCCESSFUL`. Node23/23,168영수증/127공고 재현 통과.

전체 테스트·Linux 임시 DB/API·AWS 운영 조회·배포·브라우저 QA는 미실행이다. 현재 지역 연결 요청에 브라우저 지시가 없어 정책상 생략했다. `[skip deploy]` 범위이며 전체 장기 Goal은 진행 중이다.
