# 노원 개별 첨부 연결과 담양 JSON 후속 조사

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결을 우선하고, 가능한 파일은 수집하며 개별 오류·미지원 형식은 분리한다. 제목→본문→첨부→최종 관리자 검증, 상시 worker·DB/API·관리자 UI·운영 E2E 전체 Goal은 유지한다. HWP 추출기 추가 개선은 이번 범위가 아니다.

- [x] 노원 공식 고시공고 목록·상세·개별 첨부 경로 확인 및 시스템 프로필 구현.
- [x] 본문 이미지와 메타데이터·첨부 영역을 분리하는 본문 선택기 구현.
- [x] PDF/HWP/HWPX 개별 파일과 PNG 등 미지원 형식 분리. 일괄 ZIP·미리보기·본문 이미지 요청 없음.
- [x] 확장 계약338건·bootJar·실파일 오류 회귀8건 검증.
- [!] 노원 실파일51,156byte 수신 후 파일명 HWP/서명 ZIP 불일치. 정상 다운로드로 집계하지 않음.
- [!] 노원 본문 BODY_TEXT_EMPTY 및 PNG 미지원은 각각 별도 보존.
- [x] 담양 현재 목록·상세 JSON 계약 조사. 첨부 수집 성공으로 집계하지 않음.
- [ ] 나머지 수집원, 첨부 추출·상시 운영·전체 운영 E2E Gate.

## 노원 구현

LGS-000012 / NOWON_NOTICE_TABLE에 `LOCAL_NOWON_BOARD_V1`을 추가했다. 기존 V61 목록 parser의 `opView` 단일 인수·상세 URL template과 일치하는 경로를 사용한다. 기존 migration·DB·API·정책은 변경하지 않는다.

- 상세: `www.nowon.kr/www/user/bbs/BD_selectBbs.do`, 게시판1003·구분0·사이트11·공고 ID 고정 검증.
- 제목: `div.article-view > h1.article-subject`.
- 본문: `div.article-view > div.article-body > div.txt`. 이미지만 있으면 텍스트 확보 성공으로 꾸미지 않는다.
- 첨부: 공식 `table.table-article`의 ‘첨부파일’ 셀과 `ul.file-list`만 사용한다.
- 개별 다운로드: 동일 호스트 `/component/file/ND_fileDownload.do`의 `q_fileSn` 숫자·`q_fileId` UUID 검증. GET·HTTPS443만 사용하고 redirect에서 다른 파일로 이동하지 않는다.
- 발견된 각 파일은 UNKNOWN 역할로 남긴다. 미지원 형식·미해석 링크가 있더라도 식별한 정상 파일은 유지한다. 누락 영역은 NO_FILES로 바꾸지 않는다.
- 미리보기 버튼은 동일 파일의 정확한 인수와 일치할 때만 보조 UI로 식별하며 실행하지 않는다. 첨부 영역의 실측 접근성 스크립트는 기존 버튼의 aria-label만 바꾸는 코드임을 확인하고 코드 해시가 같은 경우에만 보조 UI로 제외한다. 변경된/다른 스크립트는 미해석으로 보존한다.

고정 표본은 NOWON-20260915151630474, ‘2026년 미취업청년 어학·자격증 응시료 지원사업 4분기 모집 공고’다. ‘소상공인’ 검색에서 표본이 없었던 과거 기록을 지우지 않고 공식 제목 ‘지원’ 검색으로 다른 대상 공고를 확보했다. 시스템 분류 규칙은 바꾸지 않았으며 기존 DRAFT 제목 판정을 통과하는지 별도 검사했다. 공식 첨부 목록은 HWP1·PNG1이고 PNG는 현재 지원 형식이 아니다.

카탈로그는 새 참조 1개만 추가하고 expectation=null을 유지했다. 237참조 중 기존 승인 기대값1개·참조전용236개이며, 참조가 있는 대상은180개다. 프로필은 지역181+기업마당1=182개다. 이 숫자는 운영 적용이나 전체 검증 통과를 뜻하지 않는다.

## 실제 결과와 집계

2026-09-30 09:06:52 KST, 제목 `COMBINATION_MATCHED`, 첨부 `FOUND/complete`로 관측했다. 본문은 이미지 중심이며 `FETCH_FAILED/BODY_TEXT_EMPTY`, 본문 판정은 `REVIEW_REQUIRED/BODY_FETCH_FAILED`다. 이 오류가 식별된 첨부의 다운로드 시도까지 막지 않았다.

HWP 링크에서51,156byte를 수신했지만 파일 서명이 ZIP(`504b0304`)이므로 `FILE_SIGNATURE/ATTACHMENT_FORMAT_MISMATCH`를 기록했다. 파일명·응답 파일명 확장자는 HWP였으며 Content-Type은 관측하지 못했다. SHA-256은 `f9683fedcec21ae72f244cc049188b3fb36e9010d6eefc78598bcab0c88294c4`다. 사후 진단1회에서도 같은 길이·해시·서명이 확인됐다. ZIP 내부를 추출하거나 HWPX라고 확정하지 않았으며, 검증기를 완화하지 않았다. PNG1개는 `UNSUPPORTED_NOT_DOWNLOADED`로 남겼다.

최종 상태는 `COLLECTION_ONLY_PARTIAL_NOT_APPROVED`, 정상 다운로드0개다. 관측 태스크가 성공한 것은 오류 분리 동작이 수행됐다는 뜻이며 파일 수집 성공이 아니다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 실제 다운로드 확인 수집원 | 151 | 151/223 (67.7%) |
| 다운로드 미확인 | 72 | 72 |
| 모델 미등록 | 43 | 42 |
| 등록 후 다운로드 미확인 | 29 | 30 |
| 오류를 가진 수집원 | 37 | 38 |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16/207 | 16/207 |

분모는 2026-09-28 15:56:12 KST 읽기 전용 스냅샷의 활성 수집원223개다. 고유 행정구역 수가 아니며 첫 파일 다운로드 확인률을 전체 개발·추출·운영 완료율로 바꾸지 않는다. 기존 다른 수집원 증거는 동일하게 재현했다.

## 담양 후속 근거

LGS-000183 / DAMYANG_NOTICE_JSON의 현재 공식 목록은 `/eminwon/searchList`이며 GET `/eminwon/getSearchList` 응답의 `RSLT_DATA.searchList.dataMap`을 사용한다. 검색 파라미터는 공식 화면의 `listType=01`, `searchKey=B_Subject`, `searchVal`에 따른다. 소상공인 검색에서 공고6건을 확인했다.

상세 표본37086(2026년 담양군 소상공인 야간경관 전기료 지원사업)은 `/eminwon/searchDetail`에서 GET `/eminwon/getSearchDetail?notAncmtMgtNo=37086`을 호출한다. 응답에는 `RSLT_DATA.searchDetail`, 본문·제목 필드, `fileNameArrList`와 `fileScriptArrList`의 HWP1개가 있다. 페이지는 `needsRefresh=true`이면 `/eminwon/refreshSearchDetail`로 최신화를 시도한다.

이번에는 읽기용 목록·상세4요청만 실행했다. 최신화 endpoint·첨부 다운로드·서버 JS는 실행하지 않았다. 운영 DB의 parser endpoint·필드 설정도 조회/변경하지 않았으므로 저장소 V38 계약과 현재 사이트의 차이를 운영 오설정으로 단정하지 않는다. 다음 작업은 최신화·파일 요청 계약을 확인하고 본문/첨부를 연결하는 것이다. 담양은 아직 미등록·미수집으로 유지한다.

## 검증 기록

초기 노원 계약7건은 성공했다(46초). 첫 확장337건에서는 카탈로그 고정 건수 단언3건이 실패했다. 건수·inventory 등록을 갱신한 재검증108건에서 테스트용 카탈로그 프로필 목록 누락으로 대상 수 단언1건이 실패했다. 노원 프로필과 참조 대상 연결 검사를 보완했으며, 이 두 실패 실행에서는 실제 파일 관측 단계가 실행되지 않았다.

실행 명령:

```powershell
# SANEB_NOWON_SURVEY_FIXTURE=true 및 저장된 읽기 전용 inventory 영수증 환경은 실행 후 복원
.\gradlew.bat :test --tests '*NowonDownloadContractTest' --no-daemon
.\gradlew.bat :test --tests '*NowonDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderQaCaseExecutorTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=NOWON' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

최종 확장 Java338/338·실패/오류/생략0, bootJar 및 실제 오류 분리 관측 성공(2분24초). 파일 형식 불일치 회귀 검사를 추가한 표적 Java8/8·실패/오류/생략0(32초), Node23/23, 282영수증/225최신 공고 재현을 통과했다. 실측 fixture2건은 이번에 실행했으며 원본 정리 후 기본 검사에서는 환경변수 조건으로 생략된다. 전체 프로젝트 테스트·추출기·원격 worker DB/API 검증을 실행한 것은 아니다.

이전 커밋7bdecd4의 [Linux CI](https://github.com/FrostyCityMan/saneB/actions/runs/36644914723)와08f70af의 [Linux CI](https://github.com/FrostyCityMan/saneB/actions/runs/36645930671)는 성공으로 확인했지만 이번 변경의 CI·운영 성공으로 대신하지 않는다.

## 요청 예산과 정리

- 조사 GET9회: 노원5회(목록·지원 검색·공식 함수 정의·상세·사후 파일 진단), 담양4회(목록 페이지·검색 JSON·상세 페이지·상세 JSON).
- 요청당15초, 자동 redirect0, TLS 검증 유지. 조사 본문2MiB·파일 진단10MiB 상한.
- 실제 노원 관측1회: 최대6요청/23MiB, 실제 예약4회/2,549,716byte. 본문 최대2시도 예약을 포함한다. 조사 포함 예약 합계13회이며 실제 HTTP 횟수와 같다고 주장하지 않는다.
- 조사 원본17개/1,577,372byte를 검증된 정확한 경로·크기·해시로 대조 후 개별 삭제했다. 복구에는 공식 사이트 재조회가 필요하며 비식별 영수증·해시는 보존했다. 관측 원본도 `originalFilesRemoved=true`다.
- 사용한 단발 Node·Gradle은 종료하며 기존 사용자 프로세스·`output/`·`scripts/qa/__pycache__/`는 보존한다.

다음은 담양 JSON 최신화·개별 파일 계약의 연결과 나머지 미등록 수집원 확장이다. 노원의 본문 이미지·PNG·파일명/서명 불일치는 후속 오류로 분리하고 같은 파일을 변경 없이 반복 요청하지 않는다.

운영 데이터·설정·정책·ENFORCE·배포·재분류 변경은 없다. 브라우저는 현재 사용자 정책에 따라 미실행이다.
