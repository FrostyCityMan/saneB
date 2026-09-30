# 담양 JSON 본문·첨부 수집 연결

## 현재 단계 / Gate

전 지역의 첨부 발견·다운로드 연결을 우선한다. 정상 파일은 수집하고 발견·다운로드·파싱 오류를 별도로 남긴다. 제목→본문→첨부→관리자 최종 검증, 상시 worker·DB/API·관리자 UI·운영 E2E 전체 목표는 유지한다. HWP 추출기 추가 개선은 이번 범위가 아니다.

- [x] 담양 공식 최신화 JSON과 본문·첨부 계약 확인.
- [x] 본문 선택기와 시스템 첨부 프로필, 고정 표본·참조 카탈로그 추가.
- [x] 부분 실패에서 정상 첨부 보존, 경로·메서드·문자 인코딩·크기 제한 검증.
- [x] 관련 Java342건·bootJar·Node23건·실제 다운로드 검증.
- [x] 283영수증/226최신 공고 재현, 기존 타 지역 근거 보존, 조사 원본 정리.
- [ ] 나머지71수집원의 다운로드 확인 및 전체 추출·상시 운영·운영 E2E Gate.

## 구현과 근거

LGS-000183 / DAMYANG_NOTICE_JSON에 `LOCAL_DAMYANG_JSON_V1`을 연결했다. 기존 migration·DB·API 계약·분류 정책은 변경하지 않는다.

공식 화면 `/eminwon/searchDetail`은 캐시 `/eminwon/getSearchDetail` 응답이 오래되면 `/eminwon/refreshSearchDetail`을 GET으로 조회한다. 이번 프로필은 최신화 API에 숫자 `notAncmtMgtNo`만 전달하며, 캐시 fallback·다른 API 탐색·JavaScript 실행은 하지 않는다. 본문은 `RSLT_DATA.searchDetail.col8`, 제목은 `col4`다. 부서·연락처인 `col5`나 첨부 메타데이터는 본문 필터 입력에 섞지 않는다.

상세의 `fileNameArrList`와 `fileScriptArrList`를 같은 순서로 대조한다. `javascript:goDownLoad('표시명','저장명','경로');return false;`의 세 문자열만 기존 고정 호출 파서로 읽는다. 공식 화면에 명시된 HTTPS `eminwon.damyang.jeonnam.kr/emwp/jsp/ofr/FileDownNew.jsp`에만 GET 요청을 허용한다. `/ntishome/file/upload/ofr/ofr/YYYYMMDD` 경로, 세 query 필드, 파일명 제한, 동일 요청 유지 조건을 적용한다. 임의 함수·추가 스크립트·다른 호스트·POST·파일 식별자가 바뀌는 redirect는 허용하지 않는다.

PDF/HWP/HWPX만 다운로드 대상으로 분류한다. 각 파일 역할은 UNKNOWN이며 다운로드 성공만으로 신청서/공고문 역할이나 추출 완료를 주장하지 않는다. 배열 길이·파일명·호출 인수가 불일치해도 검증된 다른 파일은 보존한다. 첨부 배열이 없으면 발견 실패이고, 명시적인 빈 배열 두 개일 때만 NO_FILES다. 중복 파일은 합치되 상충은 오류로 남기며 최대10개 제한을 유지한다.

공식 JSON 응답에는 공고 ID가 없다. 따라서 응답 필드의 ID 일치를 검증했다고 주장하지 않는다. 정확한 요청 URL·등록 수집원 바인딩을 검증하며, 이번 실측 QA는 고정 표본 제목까지 대조했다. 운영 목록 parser의 현재 endpoint·필드 설정은 이번에 조회하지 않았으므로, 저장소 V38 계약과 현재 사이트 차이를 운영 오설정으로 단정하지 않는다.

JSON MIME은 실제 관측된 application/json·UTF-8만 허용하고, 빈 MIME 예외를 추가하지 않는다. 원문1MiB, 엄격한 UTF-8, 중복 키·후행 JSON 거절을 적용한다. 기존 공통 다운로드/파일 검증기와 다른 프로필 지문은 변경하지 않았다.

참조 카탈로그는238공고/181대상, 기존 승인 기대값1개·참조전용237개다. 담양 expectation=null은 유지한다. 등록 프로필은 지역182+기업마당1=183개이며, 이는 운영 활성화나 승인 완료를 의미하지 않는다.

## 실제 결과

고정 표본 DAMYANG-37086, ‘2026년 담양군 소상공인 야간경관 전기료 지원사업 공고’를 2026-09-30 09:29:59 KST에 관측했다.

- 제목: COMBINATION_MATCHED.
- 본문: AVAILABLE,662자, ACCEPTED/TARGET_SUPPORT_CONFIRMED. 최종 공고 확정 결과가 아닌 본문 단계 분류다.
- 첨부: FOUND/complete, HWP1개54,272byte, DOWNLOADED 및 파일 형식 검증 통과.
- 파일 SHA-256: `5ba68b191a88202fc0b506c0143a1e30043a2ee49fcc2eb0b332bbc35c9eeecb`.
- 프로필 지문: `20c0c8a254ece53f9ee914d1596500e65dfcd26be3c9945acc3f872ba7d96058`.
- 최종 수집 관측: COLLECTION_ONLY_OBSERVED_NOT_APPROVED, originalFilesRemoved=true.

첨부 텍스트 추출, worker 임시 DB/API, 운영 상시 유입, 정책 게시·ENFORCE·기존 데이터 적용은 이번 실행에 포함하지 않았다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 실제 다운로드 확인 수집원 | 151 | 152/223 (68.2%) |
| 다운로드 미확인 | 72 | 71 |
| 프로필 미등록 | 42 | 41 |
| 등록 후 다운로드 미확인 | 30 | 30 |
| 오류를 가진 수집원 | 38 | 38 |
| 기존 3표본·전체 첨부 Gate 충족/잔여 | 16/207 | 16/207 |

분모223은 2026-09-28 15:56:12 KST 읽기 전용 스냅샷의 활성 지역 수집원이다. 고유 행정구역 수·전체 파일 수집률·운영 완료율과 구분한다. 다른 지역의 최신 표본과 집계 결과는 바뀌지 않았음을 검증했다.

## 검증

초기 실행은 테스트의 요청 대상 객체 타입 단언 오류로 컴파일 실패했다. 이를 수정한123건 실행에서 POST 요청 fixture의 빈 form이 생성자 검증에 걸려1건 실패했다. 테스트 fixture를 유효한 POST 객체로 수정하여 프로필의 POST 거절을 검증했으며 실제 프로필 정책을 완화하지 않았다. 실패한 두 실행에서는 외부 첨부 관측을 수행하지 않았다.

최종 실행은 Java342/342, 실패·오류·생략0, bootJar 및 수집 관측 성공(2분23초)이다. 공식 JSON fixture1건도 실행했다. 원본 정리 후 기본 테스트에서는 SANEB_DAMYANG_SURVEY_FIXTURE 조건으로 해당1건을 생략하며 재실행하려면 원본 재확보가 필요하다. 전체 프로젝트 테스트 및 신규 SHA의 Linux CI 성공을 이번 로컬 결과로 대신하지 않는다.

실행 명령:

```powershell
# SANEB_DAMYANG_SURVEY_FIXTURE=true 및 보관 inventory 영수증 환경은 실행 후 복원
.\gradlew.bat :test --tests '*DamyangDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderQaCaseExecutorTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=DAMYANG' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

Node23/23,283영수증/226최신 공고를 재현했다. 수집 보고서와 기존 보고서의 해시·생성 코드 지문을 보존하며, 과거 실패를 지우거나 새 성공으로 덮어쓰지 않았다.

## 예산·정리·남은 범위

- 사전 조사 GET2회: 공식 상세 HTML·최신화 JSON. 요청당15초·2MiB·redirect0·TLS 검증 유지.
- 수집 관측1회: 최대6요청/23MiB. 본문 최대2시도를 포함한 실제 예약4회/2,159,616byte, 조사 포함 예약 합계6회. 예약 상한을 실제 HTTP 요청 횟수와 같다고 주장하지 않는다.
- 조사 원본4개/155,466byte는 정확한 경로·크기·해시 대조 후 개별 삭제했다. 복구에는 공식 사이트 재조회가 필요하다. 비식별 영수증·해시·실패 이력은 보존했다.
- 단발 Node·Gradle 자원은 종료하고 기존 사용자 프로세스·output/·scripts/qa/__pycache__/는 보존한다.
- 이번 커밋은 [skip deploy]로 지역 연결 코드·검증·근거만 전달한다. 운영 데이터·설정·정책·배포·재분류 변경은 없다.
- 브라우저 검증은 현재 사용자 정책에 따라 미실행이다.

다음은 나머지 미등록41개 수집원 연결과 등록 후 미확인30개에서 원인이 달라진 경우의 복구다. 같은 외부 차단·실패를 반복 요청하지 않고 정상 수집 가능한 지역을 먼저 확장한다.
