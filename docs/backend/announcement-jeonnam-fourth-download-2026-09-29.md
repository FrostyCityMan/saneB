# 보성 본문/첨부 연결과 주변 지역 오류 분리

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 우선 단계다. 정상 파일을 먼저 확보하고 발견·전송·형식·파싱·본문 오류는 별도로 기록한다. 개별 지역 오류나 3표본 확보를 다음 지역 진행 조건으로 삼지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지, HWP 추출기 1.0.16 개선 보류를 유지한다.

- [x] 보성 공식 제목·본문·첨부 영역과 POST 수집 프로필 연결.
- [x] HWPX 1개 129,631byte 다운로드와 signature/MIME/파일명 검증.
- [x] 장흥·완도 HTTP 400, 광양 고시 전용 목록 범위를 후속으로 분리.
- [x] Java 213통과·조건부 1생략, Node 23통과, bootJar·실제 관측 1건 성공.
- [x] 221영수증/177공고 재현, 기존 220영수증/176공고 보존.
- [x] 조사 HTML 10개·응답 헤더 1개 994,003byte 길이·SHA256 검증 후 개별 삭제.
- [ ] 미등록 90지역 연결, 등록 후 다운로드 미확인 27지역 후속.
- [ ] 전국 목록 유입·상시 worker·추출·DB/API/UI·정책 승인·운영 E2E Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 운영 설정을 다시 조회하거나 변경하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 최소 1파일 다운로드 확인 | 105 | **106/223 (47.5%)** |
| 다운로드 미확인 | 118 | **117** |
| 프로필 등록 지역 | 132 | **133** |
| 미등록 지역 | 91 | **90** |
| 등록 후 다운로드 미확인 | 27 | **27** |
| 현재 프로필 hash와 일치하는 오류 관측 지역 | 32 | **32** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역 133+기업마당 1=134프로필, 카탈로그 189참조/132대상이다. 신규 기대값은 null, 기존 승인 기대값 1개를 유지한다. 오류 지역은 성공 파일과 공존할 수 있으므로 미확인 수에 합산하지 않는다. 이번 미등록 지역 조사 오류는 별도 기록이며 위 32지역에 가산하지 않는다. 다운로드 비율은 전체 개발·운영 완료율이 아니다.

## 실제 관측과 후속

| 대상 | 확인 결과 | 다음 처리 |
|---|---|---|
| 보성 LGS-000187 / 37905 | 본문 285자 AVAILABLE / ACCEPTED, HWPX 1개 129,631byte | 정상 다운로드 근거 반영, 내용 추출·분류는 미검증 |
| 장흥 LGS-000189 | 공식 목록 HTTP 400, 166byte | 전송 오류로 분리, 다른 지역 수집 계속 |
| 완도 LGS-000197 | 공식 목록 HTTP 400, 166byte | 전송 오류로 분리, 다른 지역 수집 계속 |
| 광양 LGS-000182 | 등록 메뉴가 `/saeol/gosi.es?mid=a10909010000&type_code=01`로 안내 | 현재 고시 전용 목록과 일반 지원사업 공고 메뉴의 수집 범위 비교 필요 |

보성 표본 제목은 `2026년 전남청년 문화복지카드 지원사업 3차 모집 공고`다. 제목·본문 조합 통과와 첨부 다운로드를 확인했지만 첨부 텍스트 추출·분류·정책 승인·운영 DB 저장을 실행한 것은 아니다. 역할은 UNKNOWN으로 유지한다.

- 보성 파일 SHA256: `55f21b6937d78635b003b3aedbbdc5736eb06341040bbdcdb491442b92106a12`
- 보성 프로필 hash: `c340ba8013242c118a3893ea637ea926e611afb645eba6bfa510a2cec9f248ce`
- 관측 producer class hash: `1ed9f7c1b9573650552cb9666baf76d721098ce83db9598fcabac87633c65b25`

광양의 지원 검색 결과에는 과거 고시·기관 관련 글이 섞여 있었다. 이 관측만으로 지원사업 공고가 없다고 단정하지 않는다. 공식 일반공고 메뉴 연결과 현재 등록 범위 비교가 다음 조치다. 이번에 운영 URL·parser·분모를 임의 변경하거나 불일치 표본을 성공 처리하지 않았다.

## 구현 계약과 한계

- `BoseongNoticePage`는 `#content > #board_basic_view` 아래 제목 `news_tit > h3`, 본문 `board_cont`, 첨부 `file_attach`를 분리한다. 메뉴·푸터·담당자·파일명은 본문에서 제외하고 본문 표 안의 지원 제외 조건은 유지한다.
- 첨부 수 표시와 공식 `nnn` POST form의 host/path/method/빈 hidden 3필드를 확인한다. `goDownLoad`의 고정 문자열 세 인자만 읽으며 JavaScript를 실행하지 않는다. 공식 폼의 opaque 값은 복호화·재구성하지 않고 요청에만 전달하며 locator에는 파일/경로 결합 해시만 남긴다.
- 첨부 링크가 일부 바뀌거나 실패하더라도 정상 descriptor는 보존한다. 미지원 확장자는 다운로드하지 않고 별도 기록한다. 첨부 개수 불일치, 미해석 링크·동작·이미지는 오류로 남긴다.
- HTTPS/host/port/path/중복 query/source identity/POST 필드 검증, 상세 1MiB·파일 20MiB·최대 10파일 제한을 유지한다. 미리보기·일괄 ZIP은 호출하지 않는다.
- 관측 상세 페이지에 공공누리 제4유형 및 AI유형 표기가 있었다. 제한된 기술 QA 성공은 상업적 이용·재배포 권한 확인을 의미하지 않는다. 서비스 공개 전 개별 자료 이용조건은 별도 확인해야 한다.
- 공통 프로필 인터페이스·fingerprint·validator·기존 migration·DB/API/UI·HWP 추출기·운영 정책·worker 설정은 변경하지 않았다. 기존 프로필 근거는 그대로 보존했다.

## 요청량과 정리

조사 요청 총 10회: 보성 목록 GET·공식 제목 검색 POST 2회·상세 GET 4회, 광양 최초 목록·Location 확인 재조회·공식 이동 대상·지원 제목 검색 4회, 장흥·완도 목록 각 1회다. 요청당 15초·연결 7초·2MiB·자동 redirect 0·TLS 검증을 유지했다. 광양 Location 재조회에서만 응답 헤더를 임시 보관했으며 원문 값은 기록하지 않았다.

Java 실제 관측은 최대 6요청/23MiB 범위에서 1회 실행했다. 본문 포함 예약 합계 4회·2,390,623byte, 조사 포함 예약 상한은 14회다. 예약 수는 실제 HTTP 호출 수와 같지 않다. 다운로드 원본은 관측 finally에서 정리했고 조사 원본 11개는 길이·hash 대조 후 삭제했다. 비식별 metadata만 보존한다.

## 검증 명령 / 결과 / 미검증

```powershell
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_AUDIT='true'
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_RECEIPT='C:\PersonalProject\saneB\build\qa-results\target-inventory-20260928-receipt.txt'
$env:SANEB_JEONNAM_FOURTH_SURVEY_FIXTURE='true'
.\gradlew.bat :test --tests '*JeonnamFourthDownloadContractTest' --tests '*JeonbukThirdDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=BOSEONG' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

환경변수는 실행 전 값을 보관하고 finally에서 복원했다. 첫 Gradle 실행은 1분 43초에 성공했다. Java JUnit 214건 중 213통과·이전 전주/전북도 원본 fixture 컨테이너 1건 조건부 생략, 신규 실제 HTML 구조 검사 통과, 실제 지역 수집 관측 1건 통과, Node 23통과, bootJar 성공이다. 원본 삭제 후 신규 fixture 재실행 시 환경변수를 켜지 않는다.

전체 테스트·전국 상시 worker·첨부 텍스트 추출·운영 DB/API/UI·브라우저 E2E는 이번에 검증하지 않았다. 브라우저는 현재 요청의 명시적 실행 지시가 없어 정책상 생략했다. 이번 커밋·푸시는 `[skip deploy]` 범위이며 운영 배포·정책 활성화·재분류는 실행하지 않는다.
