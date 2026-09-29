# 안성·의정부·경기 광주 첨부 수집 및 안성 부분 실패 분리

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계다. 정상 파일은 수집하고 본문·발견·다운로드·형식·파싱 오류는 분리한다. 모든 오류 해결이나 3표본 확보를 다음 지역 착수 조건으로 삼지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 외부 공고 자동 활성화 금지, HWP 추출기 1.0.16 개선 보류를 유지한다.

- [x] 안성·의정부·경기 광주 공식 본문/첨부 경계와 시스템 프로필 3개 연결.
- [x] 세 지역에서 HWPX·PDF·HWP 각각 1개, 합계 200,278byte 다운로드.
- [x] 안성 본문 TIMEOUT 및 첨부 2개 TRANSPORT_TIMEOUT을 정상 파일과 별도 기록.
- [x] Java 214건 통과·조건부 1건 생략, Node 23건, bootJar 및 외부 수집 전용 관측 태스크 통과.
- [x] 199영수증/157공고 재현 및 이전 196영수증/154공고 보존.
- [x] 조사 원본 14개 3,123,546byte와 관측 임시 원본 정리. 길이·SHA256 메타데이터 보존.
- [ ] 미등록 107지역 연결, 등록 후 다운로드 미확인 21지역 후속.
- [ ] 전국 목록 자동 유입·상시 worker·추출·DB/API·정책 승인·운영 E2E Gate.

대상 분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 이번 작업은 로컬 구현과 공개 공식 공고 고정 표본의 수집 검증이며 현재 운영 설정을 재조회하거나 변경하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 최소 1파일 다운로드 확인 지역 | 92 | **95/223 (42.6%)** |
| 다운로드 미확인 지역 | 131 | **128** |
| 프로필 등록 지역 | 113 | **116** |
| 미등록 지역 | 110 | **107** |
| 등록 후 다운로드 미확인 | 21 | **21** |
| 최신 표본의 발견·파일 오류 지역 | 24 | **25** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역 116+기업마당 1=117프로필, 카탈로그 172참조/115대상이다. 신규 expectation은 null이며 기존 승인 기대값 1개를 유지한다. 다운로드 관측률은 전체 구현·운영 완료율이 아니다.

## 실제 결과

| 지역 / 표본 | 본문 | 첨부 결과 |
|---|---|---|
| 안성 LGS-000106 / 72476 | 2회 시도 후 TIMEOUT, REVIEW_REQUIRED | 발견 3개, HWPX 1개 61,805byte 성공·2개 전송 시간 초과, PARTIAL |
| 의정부 LGS-000098 / 66681 | 385자, TARGET_SUPPORT_CONFIRMED | PDF 1개 72,425byte 다운로드 |
| 경기 광주 LGS-000099 / 75337 | 121자, TARGET_SUPPORT_CONFIRMED | HWP 1개 66,048byte 다운로드 |

- 안성의 조사 단계에서도 검색·상세에서 각각 연결 시간 초과 1회 후 재시도 1회로 공식 구조를 확보했다. 수집 관측에서는 본문과 파일 일부가 다시 시간 초과했다. 정상 HWPX를 보존하고 실패를 별도 기록했으며 완료 후보로 승인하지 않았다. 원인은 확정하지 않는다.
- 의정부 소상공인 제목 검색 결과는 없었다. 공식 청년 검색 목록의 AI소프트웨어 구독비용 지원 공고를 선택했다. 파일 다운로드는 POST이며 과거 주석 처리된 GET 코드를 사용하지 않는다.
- 경기 광주의 기존 포털 진입은 공식 게시판으로 이동한다. 이동 응답의 일시 query 값을 보관하거나 재사용하지 않고, 확인된 공식 경로와 메뉴로 직접 목록을 조회했다.
- 고정 공식 표본 기반 검증이다. 운영 목록 parser가 실제 신규 공고를 같은 상세 identity로 유입시키는지는 별도 Gate이며 운영 source URL, migration, 정책, worker 설정을 변경하지 않았다.
- 안성 부분 실패를 허용하는 수집 전용 관측 태스크는 성공했다. 이것은 안성 전체 첨부 성공, 본문 성공, 추출 성공 또는 운영 전체 E2E 성공을 뜻하지 않는다.

## 구현 계약

- `CapitalFifthNoticePage`: 지역별 HTTPS443 host·상세 경로·mId·공고 ID·허용 검색 인자를 검증한다. `detailForm` 내부의 `bod_view` 제목과 `view_cont` 본문, `view_file` 첨부만 읽는다. 의정부의 직접 `bod_view`와 안성·광주의 `bod_wrap` 내부 구조를 구분한다. 경계 변경 시 전체 페이지를 본문으로 대체하지 않는다.
- 안성: 첨부 `p` 내부 `javascript:goDownload`의 고정 3리터럴을 `eminwon.anseong.go.kr/emwp/jsp/ofr/FileDown.jsp` GET으로 변환한다. 미리보기 공고 ID·파일 ID 형식·이름·저장명·경로를 대조하고, 화면의 역순 배치를 파일 순번으로 추정하지 않는다.
- 의정부: 첨부 `ul > li`와 공식 `gosiFiledownFrm`의 HTTPS 고정 action·POST·비어 있는 hidden 3필드를 검증한다. 실제 사이트처럼 전송용 user_file_nm의 공백만 제거하며 표시 파일명은 보존한다. 공식 빈 다운로드 iframe만 경계에서 제외하고 URL이 있는 iframe은 오류로 남긴다.
- 경기 광주: `ul#updateFileList`의 고정 `goDownload` 호출을 `eminwon.gjcity.go.kr` GET으로 연결한다. 바로보기·바로듣기는 파일 3인자와 N/Y가 일치하는 보조 링크로만 구분한다.
- 스크립트는 실행하지 않는다. 미확인 링크·미리보기 충돌은 오류로 기록하면서 정상 descriptor는 보존한다. 미지원 확장자는 별도 기록하고 내려받지 않는다. HTTPS·고정 출처·동일 요청 redirect·signature/MIME·10파일 상한·UNKNOWN 역할을 유지한다.
- 기존 공유 프로필·리터럴 파서·Flyway·DB/API·관리자 UI·추출기는 변경하지 않았다. 운영 게시·ENFORCE·기존 데이터 적용·배포 없음.

Profile hash: 안성 `140ba1d16e1b5f51b67dafbdb8250ebaff754b92b12675c09ceb4057a52eb4e4`, 의정부 `e6a1508edaf8fc04f64e7f8c4e7bf2da3fc3790a5a10aa1e070da1f52a3dc74b`, 경기 광주 `c3ad18062c8ca44255d1b39e387380aff968861bfaea7b71bacc3a22551eefbb`.

Producer class: `3e388d7974923391f9cd58c69a6b6a472f17b9b3f53086709be7ea99f83564c3`.

## 요청량 / 정리

공식 조사 GET 16회: 안성 5·의정부 6·경기 광주 5. 안성 연결 실패 2회도 포함한다. 요청당 15초/연결 7초/1MiB/자동 redirect 0/TLS 검증 유지. 조사 원본은 14개이며 빈 redirect 응답 2개를 포함한다.

관측 상한은 지역당 6요청·23MiB, 실제 예약 합계는 본문 포함 14회·7,368,278byte다. 조사 포함 예약 상한 30회는 실제 HTTP 호출 수와 동일시하지 않는다. 원본은 길이·SHA256 대조 후 개별 삭제했고 관측 임시 원본 정리도 보고서에서 확인했다. 사용자 output·__pycache__ 및 기존 프로세스는 보존한다.

## 검증 명령 / 결과

```powershell
.\gradlew.bat :test --tests '*CapitalFifthDownloadContractTest' --tests '*CapitalFourthDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=ANSEONG,UIJEONGBU,GG_GWANGJU' -PsanebCollectionWindowsTrust=true --no-daemon
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
git diff --check
```

Java XML 합계 215건 중 214통과·1조건부 생략·실패/오류0, 복합 Gradle 명령 2분18초 성공. 이번 조사 fixture 3개도 통과했으나 원본 정리 후 기본 실행에서는 조건부 생략된다. Node 23통과, 199영수증/157공고 재현 통과. 전체 테스트·새 운영 조회·AWS·운영 배포·텍스트 추출은 실행하지 않았다. 브라우저 QA는 현재 지역별 수집 요청의 정책에 따라 미실행이다.

## 다음 작업

미등록 107지역을 우선 연결하고, 안성 부분 실패를 포함한 오류는 별도 후속 처리한다. 등록 후 다운로드 미확인 21지역을 완료로 세지 않는다. 수집 범위 확대 후 전국 목록 유입·상시 worker·추출·DB/API/UI·운영 E2E를 본래 goal 범위에서 계속 검증한다.
