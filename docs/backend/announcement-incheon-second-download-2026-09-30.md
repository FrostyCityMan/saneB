# 제물포·미추홀 첨부 연결과 개별 응답·부평 목록 오류 분리

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 외부 공고 자동 활성화 금지를 유지한다. HWP 추출기 1.0.16 추가 개선은 보류하며 전체 장기 goal의 상시 worker·DB/API/UI·운영 E2E는 미완료다.

- [x] 제물포·미추홀 본문·첨부 프로필 2개 연결.
- [x] 미추홀 HWPX 3개 총 224,525byte 실제 다운로드·파일 형식 검증.
- [x] 제물포 첨부 3개의 응답 검증 오류를 성공과 분리하여 기록.
- [x] 부평 공식 목록 HTTP 400 기록. 상세·파일 요청 및 동일 요청 재시도 없음.
- [x] Java 234통과/조건부 1생략, Node 23검사, bootJar, 수집 전용 관측 완료.
- [x] 기존 239영수증/195공고 보존, 241영수증/197공고 근거 재현.
- [x] 조사 HTML 9개 1,389,573byte 길이·SHA256 대조 후 개별 삭제.
- [ ] 미등록 70지역 연결, 등록 후 다운로드 미확인 32지역 후속.
- [ ] 전국 자동 목록 유입·첨부 텍스트 추출·상시 worker·DB/API/UI·운영 E2E.

## 지역 집계

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷의 활성 223지역이다. 이번 운영 조회·변경은 없다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 실제 파일 최소 1개 다운로드 확인 | 120 | **121/223 (54.3%)** |
| 다운로드 미확인 | 103 | **102** |
| 프로필 등록 지역 | 151 | **153** |
| 미등록 지역 | 72 | **70** |
| 등록 후 다운로드 미확인 | 31 | **32** |
| 첨부 오류 관측 지역 | 39 | **40** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역 153+기업마당 1=154프로필, 카탈로그 209공고/152대상이다. 신규 expectation=null이며 기존 승인 기대값 1개를 보존했다. 오류 지역은 성공 지역과 겹칠 수 있으며 부평처럼 상세 표본 미선정 상태의 목록 조사 오류 전체를 포함하지는 않는다. 다운로드 확인률은 전체 개발·운영 완료율이 아니다.

## 실제 관측

2026-09-30 00:42 KST 로컬 Java 수집 경로의 고정 공개 표본 관측이다.

| 지역 / 공고 | 본문 | 첨부 결과 |
|---|---|---|
| 제물포 LGS-000055 / 14094 | 107자, 대상·지원 조합 확인 | HWPX 표시 3개 발견. 전송 후 3개 모두 FILE_SIGNATURE / ATTACHMENT_DISPOSITION_INVALID, 성공 0개 |
| 미추홀 LGS-000057 / 309943 | 121자, 대상·지원 조합 확인 | HWPX 3개 224,525byte 검증 통과 |

제물포 표본은 `2026년 제물포구 청년 컬처페이 지원사업 참여자 모집공고`, 미추홀 표본은 `2026년 청년커뮤니티 지원 사업 참여자 모집 공고(추가모집)`이다. 현재 모집 여부나 운영 등록을 의미하지 않는다. 본문 ACCEPTED는 규칙상 후보이지 관리자 확정이 아니다.

제물포의 전송 크기 70,106/43,372/53,406byte와 해시는 오류 근거이며 파일 형식 검증 성공을 뜻하지 않는다. 동일 요청을 반복하거나 공통 validator를 완화하지 않았다. 미추홀 3개는 70,746/86,050/67,729byte다. 파일별 SHA256·관측 시각은 수집 근거 인덱스가 보관한 영수증을 참조한다. 두 관측 모두 원본 삭제를 확인했으며 추출·정책 QA·기대값 승인·운영 E2E는 수행하지 않았다.

프로필 지문:

- 제물포 `b0da239fac3f2020998cc25951d41e54737e3b717a56e2974ad238c9b173e7fb`.
- 미추홀 `80d669d61bfcdc6d92ea1af53c73cde316a228cbb4317c97b17bf5f59d49006e`.

## 구현과 경계

`IncheonSecondNoticePage`는 제물포 `board-view`와 미추홀 `board-view-s1`에서 제목·본문·첨부 영역을 분리한다. 미추홀 본문에 동일 클래스의 중첩 요소가 있어 외부 본문을 한 번만 선택한다. 메뉴·담당자·첨부 파일명을 본문에 섞지 않는다.

제물포는 공식 첨부 영역의 개별 GET 링크만 사용한다. 다운로드 버튼·미리보기 버튼·전체 압축 다운로드는 알려진 경로와 같은 공고·파일 식별자인지 확인하고 수집 대상에서 제외한다. 버튼 JavaScript를 실행하거나 미리보기·압축 주소를 호출하지 않는다.

미추홀은 공식 `formCrawling`의 고정 POST 목적지 `/other/crawling_file_down.do`와 5개 필드 구조를 검증한다. `fnFileDown`의 5개 문자열 인자만 선형 파싱하고 실행·표현식을 허용하지 않는다. 내부 다운로드 목적지는 `https://eminwon.michuhol.go.kr/emwp/jsp/ofr/FileDownNew.jsp`로 고정하고, 기관이 제공한 불투명 파일 식별 값을 변경하지 않고 전달한다. 임의 URL 프록시를 허용하지 않는다. 실제 HTTP 요청은 승인된 대표 홈페이지 host만 사용한다.

HTTPS·443·정확한 host/path·공고 identity·동일 요청 redirect 제한을 유지한다. 10파일 상한, 미지원 형식, 중복·충돌, 잘못된 폼·함수 인자, 정상 파일과 오류 공존을 검증한다. 알 수 없는 링크나 잔여 요소는 발견 오류로 남기되 이미 판별된 정상 파일은 보존한다. 오류를 첨부 없음으로 바꾸지 않는다.

기존 프로필 지문·migration·DB/API/UI·추출기·운영 설정은 변경하지 않았다. 고정 상세 표본 성공은 전국 목록 자동 유입이나 상시 처리 완료를 대신하지 않는다.

## 실행 명령 / 결과

실제 HTML fixture 환경에서 신규 계약·본문 테스트를 먼저 실행하여 통과했다. 이후 동일 fixture 환경과 기존 inventory receipt를 사용했다.

```powershell
.\gradlew.bat :test --tests '*IncheonSecondDownloadContractTest' --tests '*IncheonFirstDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=JEMULPO,MICHUHOL' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
```

Gradle 1분 46초 exit 0. 일반 Java 테스트 235개 중 234통과/1생략, 실패 0개다. 생략은 이전 계양·강화 조사 원본이 정리된 상태의 조건부 fixture 테스트이며 이번 제물포·미추홀 실제 HTML 테스트는 실행됐다. Node 23개 통과, bootJar 성공이다. 관측 태스크 성공은 개별 파일 모두 성공을 의미하지 않으므로 제물포 오류는 위와 같이 보존한다.

inventory 재생성 직후 기존 인덱스와의 해시 불일치가 검출됐다. 새 inventory와 신규 영수증을 반영한 뒤 241영수증/197공고를 다시 검증해 통과했다. 해시 검증을 제거하거나 우회하지 않았다.

목록·검색·상세 조사 9회(제물포 4, 미추홀 4, 부평 1). 관측 요청 예약 상한 합계 12회·4,937,969byte이며 조사 포함 요청 상한은 21회다. 각 공고의 실행 한도는 6요청·23MiB다. 조사 HTML 원본은 9개 1,389,573byte를 길이·해시 대조 후 삭제했고 파일 관측 원본도 삭제 확인했다. 문서와 대장에는 원문·세션 값 대신 비식별 메타데이터만 보존한다.

운영 DB/설정/정책/worker 변경, ENFORCE, 재분류, 배포, 실제 운영 브라우저 검증은 수행하지 않았다. 브라우저는 현재 사용자 정책에 따라 미실행이다. 다음은 남은 미등록 지역 연결이며 제물포 응답 오류와 부평 목록 오류는 별도 후속으로 둔다.
