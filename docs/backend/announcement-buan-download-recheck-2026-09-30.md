# 부안 고정 표본 첨부 다운로드 재검증

## 현재 단계 / Gate

전 지역 첨부 수집 연결을 우선하며 성공 파일과 개별 실패를 분리한다. 추출기 1.0.16 추가 개선은 보류 상태를 유지한다. 제목→본문→첨부→관리자 최종 검증 및 상시 worker·DB/API·운영 E2E 전체 목표는 축소하지 않는다.

- [x] 부안 기존 프로필의 공식 링크·실파일 로컬 재현 검증.
- [x] 동일 표본의 본문 302자·HWPX 1개 82,795byte 실제 다운로드.
- [x] 경로 허용 정책·프로필 해시·요청 한도 변경 없음.
- [x] 과거 실패 보존, 281개 영수증·224개 최신 공고 재현 및 원본 정리.
- [ ] 나머지 72개 수집원 연결·첫 다운로드 확인.
- [ ] 전체 첨부 텍스트 추출·운영 상시 수집·DB/API·관리자 UI·운영 E2E 등 전체 Goal Gate.

## 집계

분모는 2026-09-28 15:56:12 KST 읽기 전용 운영 스냅샷의 활성 수집원 223개다. 행정구역의 고유 개수가 아니며, 수집원별 표본 파일 1개 이상 성공을 집계한다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 실제 다운로드 확인 | 150 | 151 (67.7%) |
| 다운로드 미확인 | 73 | 72 |
| 모델 미등록 | 43 | 43 |
| 등록 후 다운로드 미확인 | 30 | 29 |
| 오류를 가진 수집원 | 38 | 37 |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | 16 / 207 |

67.7%는 첫 파일 다운로드 관측률이며 전체 개발·운영 완료율이 아니다. 정상 파일이 있는 수집원에도 다른 오류가 존재할 수 있다.

## 근거와 판단

대상은 LGS-000177, LOCAL_BUAN_GET_V1, BUAN-363827(2026년 소상공인 카드수수료 지원사업 공고(추가))다.

과거 관측은 첨부 발견에 성공했으나 `ATTACHMENT_PATH_NOT_APPROVED`로 종료했고 요청 예약 6회를 소진했다. 이 기록만으로 실제 경로 위반과 예산 소진을 구분할 수 없으므로 과거 실패 원인을 확정하지 않는다. 기존 실패 보고서를 `BUAN-363827-BEFORE-PATH-RECHECK.json`으로 보존했다.

새 진단에서 공식 상세와 첨부 링크가 각각 HTTP200을 반환했다. 확보한 상세·파일·헤더를 기존 프로필과 파일 검증기로 로컬 재현하여 통과한 뒤, 기존 6요청/23MiB 한도에서 관측 1회를 실행했다. 경로 검증을 완화하거나 한도를 늘리지 않았다.

2026-09-30 08:44:04 KST 관측 결과:

- 제목 조합 통과, 본문 AVAILABLE/302자/ACCEPTED. 이는 최종 공고 승인이 아니다.
- 첨부 FOUND/complete, HWPX 1개 DOWNLOADED, 82,795byte.
- SHA-256: `2dec4ab835470c4097bb7f55897c55b2d4f6e4b1644f4f54c8c5f8ead807e461`.
- 프로필 해시 불변: `390cceb4d63b25307da64933a0d03b1577d8fe73276251413208941deb730f78`.
- 상태 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`, `originalFilesRemoved=true`, 운영 쓰기 0.

파일 형식 판정은 파일명·응답 MIME·서명을 확인한 것이다. HWPX 내부 구조·텍스트 추출까지 검증한 것으로 표현하지 않는다. 서버 MIME은 `application/octet-stream;charset=UTF-8`이며 이번 작업에 응답 정규화 코드는 추가하지 않았다.

## 변경과 검증

운영 코드·migration·API·규칙·정책·worker 설정은 변경하지 않았다. `BuanRecordedResponseContractTest`에 현재 링크·원래 예산·다른 공고/파일/호스트 정책의 회귀 검사와 명시적 환경변수로만 실행되는 실파일 fixture 검증을 추가했다. 실파일 fixture는 이번에 실제 실행했고 원본 정리 후 기본 실행에서는 조건부 생략된다.

실행 명령:

```powershell
# 실제 fixture 검사에는 SANEB_BUAN_RESPONSE_FIXTURE=true를 일시 적용 후 복원
.\gradlew.bat :test --tests '*BuanRecordedResponseContractTest' --tests '*JeonbukSecondDownloadContractTest' --no-daemon
.\gradlew.bat :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=BUAN' -PsanebCollectionWindowsTrust=true --no-daemon
.\gradlew.bat :test --tests '*BuanRecordedResponseContractTest' --tests '*JeonbukSecondDownloadContractTest' --tests '*AnnouncementAttachmentBbsOfficialObservationContractTest' --tests '*AnnouncementAttachmentBbsObservationProbeTest' --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentFileTypeValidatorTest' :bootJar --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

결과: 최초 표적 검사 성공(33초), 실제 관측 성공(29초), 확장 Java 222/222·실패/오류/생략 0·bootJar 성공(1분58초), Node 23/23. 281영수증/224최신 공고 재현 성공. 이번 작업은 전체 프로젝트 테스트를 실행한 것이 아니다.

대장 갱신 스크립트의 import 상태명 단언 오류와 patch 문맥 오류는 파일 적용 전 중단됐으며, 실제 import 상태명 및 JSON 배열 문맥을 교정한 후 재현 검사를 통과했다. 외부 요청을 다시 실행하지 않았다.

## 요청 예산과 정리

- 사전 진단 GET 2회, 요청당15초·자동 redirect0·TLS 검증 유지. 상세2MiB·파일10MiB 상한.
- 실제 관측 1회: 최대6요청/23MiB, 실제 예약4회/2,270,059byte. 본문 최대2시도 예약을 포함하므로 실제 HTTP 횟수와 동일하게 표현하지 않는다.
- 진단 포함 요청 예약 상한 합계6회. 누적 과거 요청을 새 요청으로 집계하거나 지우지 않는다.
- 조사 원본 4개/169,151byte는 정확한 경로·크기·해시 확인 후 삭제했다. 복구하려면 공식 사이트 재조회가 필요하다. 비식별 영수증·해시와 과거 실패는 보존했다.
- 원격 서버 QA·추출 실행·운영 변경·배포·기존 데이터 처리 0. 브라우저는 현재 요청 정책에 따라 미실행.

다음 작업은 미등록43개와 등록 후 다운로드 미확인29개를 계속 연결하는 것이다. 수집 실패는 별도 유지하고 이미 수집 가능한 파일의 처리를 중단하지 않는다. 이 문서의 로컬 결과를 Linux CI나 운영 E2E 통과로 해석하지 않는다.
