# 양천·관악 첨부 수집과 강서 검색 오류 분리

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계이며 전체 Gate는 미완료다. 제목 → 본문 → 첨부 → 관리자 검증, 제목 제외 원문 비저장, 외부 공고 자동 활성화 금지를 유지한다. HWP 추출기 1.0.16 추가 개선은 보류한다.

- [x] 양천·관악 시스템 프로필2개와 공식 본문 경계 연결.
- [x] HWP3개·HWPX2개, 총5파일1,240,688byte 실제 다운로드 및 signature 확인.
- [x] 강서 지원 검색15초 시간 초과를 별도 조사 오류로 보존하고 반복 요청하지 않음.
- [x] Java234통과/조건부1생략·Node23·bootJar·고정 공고2건 수집 관측 통과.
- [x] 기존232영수증/188공고를 보존하여 234영수증/190공고 재현.
- [x] 조사 HTML8개1,664,435byte 길이·SHA256 대조 후 개별 삭제.
- [ ] 미등록77지역 연결, 등록 후 다운로드 미확인30지역 후속.
- [ ] 전국 자동 목록 유입·상시 worker·첨부 텍스트 추출·DB/API/UI·운영 E2E.

## 지역 집계

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷이다. 이번 운영 조회·변경은 없다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 실제 파일 최소1개 다운로드 확인 | 114 | **116/223 (52.0%)** |
| 다운로드 미확인 | 109 | **107** |
| 프로필 등록 지역 | 144 | **146** |
| 미등록 지역 | 79 | **77** |
| 등록 후 다운로드 미확인 | 30 | **30** |
| 첨부 오류 관측 지역 | 37 | **37** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역146+기업마당1=147프로필, 카탈로그202공고/145대상이다. 신규 expectation=null, 기존 승인 기대값1개를 유지한다. 다운로드 확인률은 전체 개발·운영 완료율이 아니다. 오류 지역은 성공 지역과 겹치며, 강서처럼 상세 표본을 선택하지 못한 목록 조사 오류를 모두 포함한 수치도 아니다.

## 실제 관측

| 지역 / 고정 공고 | 본문 | 첨부 결과 |
|---|---|---|
| 양천 LGS-000016 / 46207 | 692자, 대상·지원 조합 확인 | HWP3개222,208byte 전부 다운로드 |
| 관악 LGS-000022 / 41842 | 442자, 대상·지원 조합 확인 | HWPX2개1,018,480byte 전부 다운로드 |

양천 표본은 `2026년 양천구 북한이탈주민 학생 학습지원사업 추가모집 공고`, 관악 표본은 `『관악구 소상공인 원스톱 지원사업』 소상공인 모집 공고`다. 모집 중인 공고임을 별도 확인하거나 운영에 등록한 결과가 아니라 고정 공개 표본의 기술 검증이다. 본문의 ACCEPTED는 규칙상 후보 판정이며 관리자 최종 확정·공개가 아니다. 두 결과 모두 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`, 파일 역할 UNKNOWN이며 첨부 텍스트 추출·정책 기대값 승인·운영 쓰기는 없다.

프로필 지문:

- 양천 `82a0249242ed8a9caeca04a170a8843f0914bc91b8dc4557b0d43a27bbbe5552`.
- 관악 `775f5e3a00ca8626808886bd8343ecb9ea19e89302341f8a39bc5c1eb95f5b4a`.

파일별 크기·SHA256·관측 시각은 `attachment-collection-receipt-index-2026-09-28.json`의 해당 공고 근거를 참조한다. 원문·개인정보·세션값은 저장소에 복사하지 않는다.

## 구현 계약과 조사 오류

`SeoulSixthNoticePage`는 양천의 `SeolCollectVo` 안 공식 제목·본문·첨부 영역과 관악의 `board-view` 안 공식 영역을 분리한다. 첨부명·담당자 메타데이터·메뉴를 본문에 섞지 않는다.

`SeoulSixthAttachmentDiscoveryProfile`은 기존 새올 GET 파일 검증기를 재사용한다. 양천의 `doUrlDownload`에서 고정 세 문자열만 읽고 공식 함수의 HTTPS 파일 URL 및 UTF-8 인코딩을 재현한다. JavaScript·iframe을 실행하지 않는다. 관악은 공개 파일 URL의 세 query 값을 보존하되 화면 표시 파일명 비교에는 앞뒤 공백을 제거한다. 미리보기·점자·음성 링크는 공고번호·파일 순번·확장자·파일명을 대조만 하며 요청하지 않는다.

정상 descriptor는 다른 링크의 오류가 있어도 유지한다. 10파일 상한, 미지원 확장자, 중복·충돌, 잘못된 host/HTTP/query/method, 경로 이탈, 다운로드 호출 변조, 미리보기 식별 변경, 공식 HTML·본문·제목 경계를 테스트했다. 기존 프로필 지문·migration·DB/API/UI·HWP 추출기·운영 설정은 변경하지 않았다.

강서 LGS-000017: 공식 목록200, 소상공인 제목 검색200이나 결과 표에 상세 링크0개, 지원 제목 검색은15초 후 응답0byte 시간 초과였다. 이 한정 검색을 전체 지원 공고 부재로 보지 않는다. 상세·첨부를 확인하지 못했으므로 미등록 상태를 유지하며 같은 실패 검색을 반복하지 않는다. 양천의 첫 소상공인 검색에는 과거 공고1건이 있었으나 추가 지원 검색에서2026년 표본을 선택했다.

## 실행·검증·정리

```powershell
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_AUDIT='true'
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_RECEIPT='C:\PersonalProject\saneB\build\qa-results\target-inventory-20260928-receipt.txt'
$env:SANEB_SEOUL_SIXTH_SURVEY_FIXTURE='true'
.\gradlew.bat :test --tests '*SeoulSixthDownloadContractTest' --tests '*SeoulFifthDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=YANGCHEON,GWANAK' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git -c core.autocrlf=false diff --check
```

Gradle2분11초, Java235건 중234통과/과거 서울5차 원본 fixture1건 조건부 생략. JAR 생성 및 실제 관측2건 성공, Node23검사와 증거 재현 통과. 환경변수는 실행 전 값을 finally에서 복원했다. 원본 삭제 후 실제 HTML fixture 환경변수는 켜지 않는다.

조사9요청(양천4·관악2·강서3), 각15초/2MiB·TLS 검증·자동 redirect0. 성공 HTML8개, 강서 시간 초과1건은 원본 파일이 생성되지 않았다. Java 관측은 지역당6요청/23MiB, 본문 포함 예약 합계11회·5,860,976byte다. 조사 포함 예약 상한20회이며 예약 수와 실제 HTTP 호출 수는 다르다. 실제 파일의 finally 정리와 조사 원본8개 길이·hash 대조 후 개별 삭제를 확인했다.

전체 테스트·첨부 텍스트 추출·Linux worker/DB/API·운영 UI·배포·정책 활성화·재분류는 이번 검증 범위가 아니다. 브라우저는 현재 지역 확대 요청에 별도 지시가 없어 사용자 정책상 미실행했다. 검증한 범위만 `[skip deploy]`로 커밋·푸시하고 미등록 지역 연결을 계속한다.
