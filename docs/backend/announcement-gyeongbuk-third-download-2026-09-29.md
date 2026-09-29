# 영주·성주·예천 첨부 연결과 부분 수집 결과

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드를 먼저 연결한다. 정상 파일은 보존하고 미지원 형식·다운로드 실패는 별도 오류로 남긴다. 한 지역의 모든 오류 해결이나 3표본 확보를 다음 지역 착수 조건으로 삼지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지와 HWP 추출기 1.0.16 추가 개선 보류를 유지한다.

- [x] 공식 목록·검색·상세 구조와 첨부 다운로드 방식 확인.
- [x] 공통 처리기1개·시스템 프로필3개·미승인 참조3개 추가.
- [x] 영주 PDF/HWP2개·예천 HWP1개 실제 다운로드 및 signature 검증.
- [x] 성주 권한 오류 응답·영주 JPG 미지원을 별도 기록.
- [x] 계약168건·Node23건·bootJar·실제 관측3공고 실행 완료.
- [x] 최초 실패 포함147영수증/최신111공고 재현, 임시 원본 정리.
- [ ] 경북 잔여 지역과 나머지 전국 미등록152지역 연결.
- [ ] 본문 정제 품질·첨부 추출·상시 worker·DB/API·운영 E2E 전체 Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 현재 운영 설정을 새로 조회하거나 DB·정책·worker 설정을 변경한 결과가 아니다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 첫 다운로드 관측 지역 | 56 | **58/223 (약26.0%)** |
| 다운로드 미관측 지역 | 167 | **165** |
| 로컬 프로필 등록 지역 | 68 | **71** |
| 미등록 지역 | 155 | **152** |
| 등록 후 다운로드 미관측 | 12 | **13** |
| 등록 지역 중 파일·발견 오류 근거 있음 | 11 | **13** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

약26.0%는 다운로드 관측률이며 전체 구현·운영 완료율이 아니다. 지역71+기업마당1=72프로필, 카탈로그126참조/지역68개다. 기존 expectation1개를 보존하고 신규 기대값은 승인하지 않았다. 성주는 연결만 등록됐으며 실제 다운로드 성공 지역에는 포함하지 않는다.

## 실제 결과

| 지역 / 공고 | 본문 확보 길이 | 첨부 발견 | 최종 다운로드 |
|---|---:|---:|---|
| 영주 LGS-000206 / 24194 | 75자 | 3개 | PDF315,632byte·HWP52,224byte 성공, JPG 미지원 |
| 성주 LGS-000217 / 586507 | 1,842자 | 2개 | 두 파일 모두374byte HTML 권한 오류, 성공0개 |
| 예천 LGS-000219 / 30761 | 4,567자 | 1개 | HWP93,696byte 성공 |

검증된 파일은 총3개 **461,552byte**다. 모든 표본은 임시 DB의 DRAFT seed로 제목 조합 통과, 상세 제목 식별·첨부 발견 완료다. 영주·성주는 `COLLECTION_ONLY_PARTIAL_NOT_APPROVED`, 예천은 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`다. 관리자 최종 승인·운영 활성화·첨부 텍스트 추출 성공을 뜻하지 않는다.

본문 중간 판정은 모두 `REVIEW_REQUIRED`다. 영주는 `BODY_COMBINATION_NOT_CONFIRMED`, 성주는 `BODY_GROUP_A_MATCHED`, 예천은 `BODY_GROUP_B_MATCHED`로 기록됐다. **본문 확보 길이만으로 정제 품질이나 공고 전체 본문 확보를 보증하지 않는다.** 영주75자는 공식 상세의 긴 본문과 비교해 누락 가능성이 있고, 예천4,567자는 실제 공고 외 영역 혼입 여부를 후속 확인해야 한다. 이번에는 본문 파서·분류 규칙을 변경하지 않았다.

### 공식 다운로드 연결과 오류

- 영주: `news_view` 제목과 `data_add` 첨부 영역의 직접 링크만 읽는다. PDF·HWP는 내려받고 JPG는 기존 지원 형식 밖이므로 다운로드하지 않는다. 미리보기는 같은 공고·파일 ID의 보조 링크로만 구분한다.
- 영주 첫 관측은 `ATTACHMENT_HOST_NOT_APPROVED`였다. HEAD404와 달리 실제 GET은302로 같은 기관 `eminwon.yeongju.go.kr/emwp/jsp/ofr/FileDown.jsp`에 연결됐다. 공식 다운로드 요청에서만 고정 호스트·경로·공개 파일3query·날짜 디렉터리로의 이동을 허용한 뒤2파일이 통과했다. 임의 redirect·다른 기관·경로 이동은 차단한다.
- 성주: 공식 목록의 `cmd=2`는 화면 script에서 같은 공고의 `cmd=258`로 이동한다. 고정 전환만 구현하고 외부 script를 실행하지 않는다. `form#frm`의 `bod_view`/`view_file` 영역에서 직접 다운로드 링크를 읽고 파일명 뒤 용량 표시를 제거한다.
- 성주 서버는 HWP 대신 **“조회 권한이 없습니다”**라는 HTML을 반환했다. 두 파일 모두 같은374byte/hash였고 `FILE_SIGNATURE / ATTACHMENT_SIGNATURE_UNSUPPORTED`로 분리했다. 권한 우회나 HTML의 HWP 성공 처리를 하지 않는다. 다른 정상 지역 수집은 계속한다.
- 예천: 공식 `km-view` 제목·`eminwon-files`와 `form1`의 고정 HTTPS POST 경로·빈 hidden3필드를 확인한다. 공식 `/js/announcement.js`는 선언 확인용으로만 읽었다. `goDownLoad`의 문자열3개를 검증해 POST에 전달하며 JS·뷰어를 실행하지 않는다. 전송 인자 원문은 대장·로그에 보관하지 않는다.

공통 처리기는 정상 descriptor를 보존하면서 미확인 링크·중복 충돌·미지원 형식·10파일 상한을 구분한다. 문서 역할은 UNKNOWN으로 유지한다. 기존 프로필 실행 코드·hash, migration·DB/API·화면·정책·운영 worker·추출기는 변경하지 않았다. 고정 상세 표본 검증이며 목록 스케줄러부터의 자동 유입 E2E는 검증하지 않았다.

## 요청 예산과 정리

사전 목록·검색·상세·공식 JS 조사와 진단은17회(GET16·HEAD1)다. 관측 예약은 최초14회+재실행16회=30회, 조사 포함47회다. 예약에는 본문 요청 상한이 포함되어 실제 HTTP 횟수와 동일하다고 표현하지 않는다.

| 지역 | 조사·진단 | 최초+재관측 예약 | 이번 합계 |
|---|---:|---:|---:|
| 영주 | 5 | 5+7 | 17 |
| 성주 | 8 | 5+5 | 18 |
| 예천 | 4 | 4+4 | 12 |

각 조사15초/연결7초/1MiB/자동 redirect0/TLS 검증 유지. 최초 관측 각6요청·23MiB, 최종 영주는2파일의 공식 redirect를 포함해7요청·23MiB, 다른2지역은6요청·23MiB다. 두 실행의 예약 byte 합계13,931,172. 최초3영수증과 최종3영수증을 모두 보존하며 최신3공고만 집계한다.

조사·진단 임시 원본19개 총1,807,961byte와 관측 임시 원본을 삭제했다. 복구 사본은 없고 hash·비식별 metadata만 보존한다. 단발 Node와 Gradle은 종료하며 기존 사용자 프로세스·미추적 `output/`·`scripts/qa/__pycache__/`는 유지한다.

## 검증 명령 / 결과

```powershell
.\gradlew.bat :test --tests '*GyeongbukThirdDownloadContractTest' --tests '*GyeongbukSecondDownloadContractTest' --no-daemon
.\gradlew.bat :test --tests '*GyeongbukThirdDownloadContractTest' --tests '*GyeongbukSecondDownloadContractTest' --tests '*GyeongbukFirstDownloadContractTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=YEONGJU,SEONGJU,YECHEON' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

초기 계약34건 중 테스트 변형 입력이 원문과 같았던1건을 수정했다(53초). 첫 확장167건·bootJar·관측 실행 완료(1분28초, 예천1파일). 영주 연결 보완 후 최종168건·bootJar·관측 실행 완료(1분40초,3파일 성공). 최종 실패·오류·skip0, Node23/23. 보관147영수증/111공고 재현 통과, 이전141영수증·기존 실행 metadata·기존 지역 profile hash를 보존했다.

전체 테스트·Linux worker 임시 DB/API·AWS 운영 조회·운영 배포·브라우저 QA는 실행하지 않았다. 브라우저는 현재 지역 연결 작업에 명시적 요청이 없어 정책상 생략했다. `[skip deploy]` 범위이며 장기 Goal은 계속 진행 중이다.
