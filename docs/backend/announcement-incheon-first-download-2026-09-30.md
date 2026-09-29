# 계양·강화 첨부 수집과 개별 파일·옹진 목록 오류 분리

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계다. 전체 장기 goal의 상시 worker·DB/API/UI·운영 E2E Gate는 미완료다. 제목 → 본문 → 첨부 → 관리자 검증, 제목 제외 원문 비저장, 자동 활성화 금지를 유지한다. HWP 추출기 1.0.16 추가 개선은 보류한다.

- [x] 계양·강화 공통 게시판 본문·첨부 프로필 2개 연결.
- [x] 계양 HWP 3개·강화 HWPX 1개, 총 403,776byte 실제 다운로드 및 파일 형식 검증.
- [x] 계양의 나머지 파일 1개 HTTP 400을 별도 오류로 기록하고 정상 3개 보존.
- [x] 옹진 목록 HTTP 400 기록, 상세·첨부 요청 및 동일 목록 반복 요청 없음.
- [x] Java 238통과/조건부 2생략, Node 23검사, bootJar, 실제 수집 전용 관측 통과.
- [x] 기존 237영수증/193공고 보존, 239영수증/195공고 재현.
- [x] 조사 원본 8개 717,809byte 길이·SHA256 대조 후 개별 삭제.
- [ ] 미등록 72지역 연결, 등록 후 다운로드 미확인 31지역 후속.
- [ ] 전국 자동 목록 유입·첨부 텍스트 추출·상시 worker·DB/API/UI·운영 E2E.

## 지역 집계

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷이다. 이번 운영 조회·변경은 없다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 실제 파일 최소 1개 다운로드 확인 | 118 | **120/223 (53.8%)** |
| 다운로드 미확인 | 105 | **103** |
| 프로필 등록 지역 | 149 | **151** |
| 미등록 지역 | 74 | **72** |
| 등록 후 다운로드 미확인 | 31 | **31** |
| 첨부 오류 관측 지역 | 38 | **39** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역 151+기업마당 1=152프로필, 카탈로그 207공고/150대상이다. 신규 expectation=null, 기존 승인 기대값 1개를 보존했다. 다운로드 확인률은 전체 개발·운영 완료율이 아니다. 계양처럼 일부 파일만 성공한 지역도 포함한다. 오류 지역은 성공 지역과 겹치며 옹진처럼 상세 표본을 선정하지 못한 모든 목록 조사 오류를 포함하지는 않는다.

## 실제 관측

2026-09-30 00:26 KST, 로컬 Java 수집 경로에서 관측했다.

| 지역 / 고정 공고 | 본문 | 첨부 결과 |
|---|---|---|
| 계양 LGS-000061 / 53151 | 91자, 대상·지원 조합 확인 | HWP 4개 발견. 3개 301,056byte 검증 통과, 1개 FILE_DOWNLOAD / ATTACHMENT_HTTP_400 |
| 강화 LGS-000064 / 52786 | 405자, 대상·지원 조합 확인 | HWPX 1개 102,720byte 검증 통과 |

계양은 `2026년도 중소기업육성기금 융자지원 계획 공고`, 강화는 `소상공인 경영안정지원금 지급사업 공고`를 사용했다. 고정 공개 표본의 기술 검증이며 현재 모집 여부 확인이나 운영 등록을 의미하지 않는다. 본문의 ACCEPTED는 규칙상 후보 판정이지 관리자 최종 확정이 아니다.

계양은 COLLECTION_ONLY_PARTIAL_NOT_APPROVED, 강화는 COLLECTION_ONLY_OBSERVED_NOT_APPROVED다. 첨부 역할은 UNKNOWN이며 추출·정책 기대값 승인·운영 쓰기는 수행하지 않았다. 계양의 HTTP 400 파일은 성공으로 세지 않았고, 같은 요청을 반복하거나 파일 주소를 임의로 복구하지 않았다.

프로필 지문:

- 계양 `55aec8432a9a7524f24ed3cd09b5177e1ea116517d19ab722aaed05bcc32063a`.
- 강화 `534e0edcdc09cdcd83f2c468d82190cfd76ea11d2e75459afc56d6a1d4e94c37`.

파일별 크기·SHA256·관측 시각은 `attachment-collection-receipt-index-2026-09-28.json`을 참조한다. 원문·세션 원문·개인정보는 저장소에 복사하지 않았다.

## 구현과 오류 처리

`IncheonPortalNoticePage`는 두 사이트의 `board_view`에서 제목·본문 `con`·첨부 `dl.file`을 분리한다. 계양은 `tit` 내부 제목과 첨부, 강화는 직접 자식 제목과 첨부만 선택한다. 파일명·담당자 메타데이터·메뉴·푸터를 본문에 섞지 않는다.

공식 `FileDownNew.jsp` GET 링크의 암호화된 세 query 값을 해석하거나 복호화하지 않고 그대로 전송한다. 고정 HTTPS 기관 host, 경로, 중복 없는 세 query, 길이·문자 범위, 동일 최초 요청의 redirect 제한을 유지한다. 감사 locator에는 공고번호와 파일 식별 hash만 남긴다. 미리보기·아이콘·JavaScript는 요청하거나 실행하지 않는다.

초기 실제 HTML fixture 검증에서 강화 아이콘 주소의 세션 경로 때문에 첨부가 인식되지 않았다. 저장 원본에서 원인을 확인하고 고정 아이콘 경로·형식과 제한된 세션 형식만 허용하도록 수정했다. 세션 값을 파일 다운로드 요청에 사용하지 않으며 테스트는 인위적인 값만 사용한다. 외부 host·경로 이탈·실행 속성은 계속 거부한다. 수정 전 실패를 통과로 덮지 않았으며 실제 외부 파일 관측은 최종 수정 뒤 1회만 실행했다.

계양의 첫 소상공인 제목 검색에는 표본이 없어서 지원 제목 검색으로 표본을 찾았다. 이 검색 결과를 전체 지원 공고 부재로 간주하지 않았다. 옹진 LGS-000065 공식 목록은 HTTP 400이어서 미등록 상태를 유지하며 별도 후속으로 남긴다.

10파일 상한, 미지원 확장자, 중복·충돌, query/host/method/redirect/경로 변조, 정상 파일과 오류의 공존, 본문·제목 경계, 실제 HTML을 테스트했다. 기존 프로필 지문·migration·DB/API/UI·추출기·운영 설정은 변경하지 않았다. 고정 상세 관측은 전체 목록의 자동 유입 성공을 대신하지 않는다.

## 실행·검증·정리

```powershell
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_AUDIT='true'
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_RECEIPT='C:\PersonalProject\saneB\build\qa-results\target-inventory-20260928-receipt.txt'
$env:SANEB_INCHEON_FIRST_SURVEY_FIXTURE='true'
.\gradlew.bat :test --tests '*IncheonFirstDownloadContractTest' --tests '*SeoulSeventhDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=GYEYANG,GANGHWA' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git -c core.autocrlf=false diff --check
```

최종 Gradle 1분50초, Java 240건 중 238통과/과거 서울7차 원본 fixture 2건 조건부 생략. Node 23검사·JAR 생성·영수증 재현 통과. 초기 계약 검증 실패와 로컬 진단은 위 수정 이력으로 구분한다. 수집 전용 태스크 성공은 계양의 실패 파일까지 성공했다는 뜻이 아니다. 환경변수는 finally에서 원래 값으로 복원했고 원본 삭제 후 fixture 환경변수는 켜지 않는다.

조사 8요청(계양 4·강화 3·옹진 1), 각 15초/2MiB·TLS 검증·자동 redirect 0. 검색 명령 옵션 오타 1회는 로컬 파싱 실패로 HTTP 요청이 없었고 수정 후 실행했다. Java 관측 상한은 계양 7회, 강화 6회, 각각 23MiB다. 본문 포함 예약 합계 11회·4,802,880byte, 조사 포함 예약 상한 19회이며 실제 HTTP 호출 수와 구분한다. 임시 파일 finally 정리와 조사 원본 8개 개별 삭제를 확인했다.

전체 테스트·첨부 추출·Linux worker/DB/API·운영 UI·배포·정책 활성화·재분류는 이번 검증 범위가 아니다. 브라우저는 현재 요청에 별도 지시가 없어 사용자 정책상 미실행했다. 검증한 범위만 `[skip deploy]`로 커밋·푸시하고 다른 미등록 지역 연결을 계속한다.
