# 서울시·서울 중구 수집과 용산 파일 응답 오류 분리

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계이며 전체 Gate는 미완료다. 제목 → 본문 → 첨부 → 관리자 검증, 제목 제외 원문 비저장, 자동 활성화 금지는 유지한다. HWP 추출기 1.0.16 추가 개선은 보류한다.

- [x] 서울시·서울 중구·용산의 공식 본문 경계와 첨부 프로필 3개 연결.
- [x] 서울시·서울 중구 5파일 1,029,701byte 다운로드 및 파일 형식 검증.
- [x] 용산 3파일의 Content-Type 불일치를 별도 오류로 기록. 성공으로 집계하지 않고 동일 요청 재시도 없음.
- [x] Java 239통과/조건부 1생략, Node 23검사, bootJar, 수집 전용 관측 태스크 통과.
- [x] 기존 234영수증/190공고 보존, 237영수증/193공고 재현.
- [x] 조사 원본 HTML 13개 1,683,597byte 길이·SHA256 대조 후 개별 삭제.
- [ ] 미등록 74지역 연결, 등록 후 다운로드 미확인 31지역 후속.
- [ ] 전국 자동 목록 유입·상시 worker·첨부 텍스트 추출·DB/API/UI·운영 E2E.

## 지역 집계

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷이다. 이번에는 운영 조회·변경을 하지 않았다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 실제 파일 최소 1개 다운로드 확인 | 116 | **118/223 (52.9%)** |
| 다운로드 미확인 | 107 | **105** |
| 프로필 등록 지역 | 146 | **149** |
| 미등록 지역 | 77 | **74** |
| 등록 후 다운로드 미확인 | 30 | **31** |
| 첨부 오류 관측 지역 | 37 | **38** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역 149+기업마당 1=150프로필, 카탈로그 205공고/148대상이다. 신규 expectation=null, 기존 승인 기대값 1개는 보존한다. 다운로드 확인률은 전체 개발·운영 완료율이 아니다. 오류 지역은 성공 지역과 겹칠 수 있고, 상세 표본을 찾지 못한 모든 목록 조사 오류를 포함한 수치도 아니다.

## 실제 관측

2026-09-30 00:05~00:06 KST, 로컬 Java 수집 경로에서 관측했다.

| 지역 / 고정 공고 | 본문 | 첨부 결과 |
|---|---|---|
| 서울시 LGS-000001 / 466130 | 376자, 대상·지원 조합 확인 | HWPX 1개·PDF 1개, 537,613byte 검증 통과 |
| 서울 중구 LGS-000003 / 1475799545 | 244자, 대상·지원 조합 확인 | PDF 1개·HWP 2개, 492,088byte 검증 통과 |
| 용산 LGS-000004 / 766830 | 138자, 대상·지원 조합 확인 | HWP 표시 3개, 전송 221,696byte. 모두 FILE_SIGNATURE / ATTACHMENT_CONTENT_TYPE_MISMATCH |

서울시 표본은 `2026년 서울시 청년 마음건강 지원사업 4차 참여자 모집 공고(추가모집)`, 중구는 `2026년 4/4분기 중소기업육성기금 융자지원 계획`, 용산은 `2026년 용산구 일자리기금 청년기업 융자지원 확대 계획 공고`다. 고정 공개 표본의 기술 검증이며 현재 모집 여부 확인이나 운영 등록을 의미하지 않는다.

본문의 ACCEPTED는 규칙상 후보 판정이지 관리자 최종 확정·공개가 아니다. 두 성공 지역은 COLLECTION_ONLY_OBSERVED_NOT_APPROVED, 용산은 COLLECTION_ONLY_PARTIAL_NOT_APPROVED다. 용산의 파일 크기·hash는 전송 근거일 뿐 파일 형식 검증 성공 근거가 아니다. 원본은 정리했으며 정확한 서버 MIME 값은 이번 영수증에 보관되지 않아 단정하지 않는다. 형식 허용 목록은 변경하지 않았다.

프로필 지문:

- 서울시 `9913bc326c21ccd1eb0335c1c333d5eac3c1b0106e95c9a507543e1e5a9b7258`.
- 서울 중구 `b58c862d6922fa5cddab006b0ba5f70ba76fc35907cd2e1141fcdc578a047ae6`.
- 용산 `120740f23281e6a22618624eebd830535d8280d9f68801c482a2408920ec5a5f`.

파일별 크기·SHA256·관측 시각은 `attachment-collection-receipt-index-2026-09-28.json`의 해당 공고 근거를 참조한다. 원문·세션값·개인정보는 저장소에 복사하지 않는다.

## 구현 계약

`SeoulSeventhNoticePage`는 서울시 공식 공고 영역의 제목·scrabArea·첨부 영역, 중구 공식 표의 제목·article_body·첨부 셀, 용산 bd-view의 subject·dbdata·file-list를 분리한다. 메뉴·푸터·첨부명·외부 viewer iframe을 본문에 섞지 않는다.

첨부 프로필은 기관별 공식 HTTPS 파일 endpoint, query 집합, 공고·파일 식별자와 호스트를 검증한다. 서울시는 화면 data 속성의 공고·파일 식별자를 대조하고, 중구는 다운로드와 미리보기 파일 식별자를 대조한다. 용산은 파일 크기 표시만 파일명에서 제거한다. 미리보기·변환·음성·외부 iframe은 요청하거나 실행하지 않는다.

정상 descriptor는 다른 링크의 오류가 있어도 보존한다. 미지원 DOCX, 외부 host/HTTP/query/method, 중복·충돌, 잘못된 미리보기, 10파일 상한, 제목·본문 독립 경계, 공식 HTML을 검증했다. 서울시 첫 조사 표본 457461은 DOCX만 있어 실제 HTML 계약 테스트에서 미지원으로 확인하고 다운로드하지 않았다. HWPX·PDF가 있는 466130을 별도 표본으로 선택했다.

기존 프로필 지문·migration·DB/API/UI·추출기·운영 설정은 변경하지 않았다. 고정 상세 URL 관측은 전체 목록 자동 유입 성공을 대신하지 않는다.

## 실행·검증·정리

```powershell
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_AUDIT='true'
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_RECEIPT='C:\PersonalProject\saneB\build\qa-results\target-inventory-20260928-receipt.txt'
$env:SANEB_SEOUL_SEVENTH_SURVEY_FIXTURE='true'
.\gradlew.bat :test --tests '*SeoulSeventhDownloadContractTest' --tests '*SeoulSixthDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=SEOUL,SEOUL_JUNGGU,YONGSAN' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git -c core.autocrlf=false diff --check
```

사전 제목·본문·첨부 경계 테스트 및 실제 HTML fixture 검증은 37초, 확장 검증은 1분52초였다. Java 240건 중 239통과/과거 서울6차 원본 fixture 1건 조건부 생략. Node 23검사·JAR 생성·근거 재현 통과. 수집 전용 태스크의 성공은 오류를 분리한 관측 완료를 의미하며 용산 파일의 형식 검증 통과를 의미하지 않는다. 환경변수는 실행 전 값을 finally에서 복원했다. 원본 삭제 후 fixture 환경변수는 켜지 않는다.

조사 13요청(서울시 5·중구 4·용산 4), 각 15초/2MiB·TLS 검증·자동 redirect 0. Java 관측은 지역당 6요청/23MiB, 본문 포함 예약 합계 17회·7,930,936byte다. 조사 포함 예약 상한 30회이며 예약 수와 실제 HTTP 호출 수는 구분한다. 임시 파일의 finally 정리 및 조사 원본 13개의 개별 삭제를 확인했다.

전체 테스트·첨부 텍스트 추출·Linux worker/DB/API·운영 UI·배포·정책 활성화·재분류는 이번 검증 범위가 아니다. 브라우저는 현재 지역 확대 요청에 별도 지시가 없어 사용자 정책상 미실행했다. 검증 범위만 `[skip deploy]`로 커밋·푸시하며 미등록 지역 연결을 계속한다.
