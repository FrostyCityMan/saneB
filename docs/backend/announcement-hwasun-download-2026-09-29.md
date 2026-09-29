# 화순 공통 새올 엔진 연결과 실제 첨부 수집

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계이며 전체 Gate는 미완료다. 정상 파일은 수집하고 본문·발견·다운로드·파싱 실패는 별도 기록한다. HWP 추출기 1.0.16 개선 보류, 제목 → 본문 → 첨부 → 관리자 검증, 제목 제외 원문 비저장, 외부 공고 자동 활성화 금지를 유지한다.

- [x] 화순 공식 목록 리디렉션·공식 제목 검색·상세 구조 확인.
- [x] 기존 `SaeolGetAttachmentDiscoveryProfile` 재사용으로 화순 고정 시스템 bean 추가. 공통 엔진 구현·지문 생성 코드는 변경하지 않음.
- [x] HWPX 1개 134,544byte 실제 다운로드, 파일 형식 검증 및 원본 정리.
- [x] Java 219건·Node 23건 통과, bootJar 및 수집 전용 관측 성공.
- [x] 기존 222영수증/178공고 보존, 총223영수증/179공고 재현.
- [x] 조사 HTML·응답 헤더 4개를 길이·SHA256 대조 후 개별 삭제.
- [ ] 미등록 88지역 연결, 등록 후 다운로드 미확인 27지역 후속.
- [ ] 화순 등록 목록 host와 새올 상세 host 간 본문 수집 연계.
- [ ] 전국 목록 유입·상시 worker·추출·DB/API/UI·정책 승인·운영 E2E 검증.

## 지역 집계

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷이다. 이번에 운영 설정을 재조회하거나 변경하지 않았다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 실제 파일 최소1개 다운로드 확인 | 107 | **108/223 (48.4%)** |
| 다운로드 미확인 | 116 | **115** |
| 프로필 등록 지역 | 134 | **135** |
| 미등록 지역 | 89 | **88** |
| 등록 후 다운로드 미확인 | 27 | **27** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역135+기업마당1=136프로필이다. 카탈로그는191공고/134대상이며 신규 기대값은 null이다. 다운로드 확인률은 전체 개발·운영 완료율이 아니다. 현재 집계기의 오류 지역32개는 첨부 수집 오류 기준으로 본문 오류를 포함하지 않으므로, 화순 본문 실패까지 포함한 전체 오류 지역 수로 표현하지 않는다.

## 실제 관측과 한계

- 지역/표본: `LGS-000188` / `HWASUN-39230`, `2026년 전남광주 청년 문화복지카드 지원사업 3차 모집 공고`.
- 제목: `COMBINATION_MATCHED`. 공식 상세의 제목 일치 검증 통과.
- 첨부: `FOUND`, 전체 발견1개·HWPX 다운로드1개, 134,544byte. 역할 UNKNOWN 유지.
- 파일 SHA256: `5cd830e7b89d95b9bf74f0bd2dd42858f503823a161f7caea439d44160e84d95`.
- 프로필 hash: `c0d4fb0b2ccbcfcff85e7528a20e41841c8e2c7ec61bace99ac931d0dc3936e7`.
- 관측 producer class hash: `b848a3fb230cf79038dc18a4f6eab7125fbe0d5b2ab3127861fb8646ea744668`.
- 본문: `FETCH_FAILED / DETAIL_HOST_NOT_ALLOWED`, `REVIEW_REQUIRED`. 등록 주소 `www.hwasun.go.kr`와 상세 `eminwon.hwasun.go.kr`가 달라 기존 동일-host 검증에서 요청 전에 중단했다. 실제 HTML에 본문이 없는 경우가 아니다.

실측 상세는 기존 함안과 같은 form1 / width=100% 표 / word-break:break-all 본문 셀 구조다. 본문 선택기에 화순 고정 host를 추가하고 동일-host 입력의 본문/메타데이터 분리와 구조 변경 오류를 테스트했다. 그러나 등록 host를 바꾸거나 host 검증을 완화하지 않았다. 향후 목록→상세 공식 연계 계약을 검증해 해결해야 한다. 이번 수집 전용 검증은 사용자 승인대로 본문 실패와 무관하게 안전하게 발견된 파일을 수집했으며, 이를 정상 정밀 후보·전체 분석 완료로 승격하지 않는다.

## 계약·검증

- 첨부는 form1의 정확한 `첨부파일` 라벨 셀에서 고정 goDownLoad 문자열3개만 읽는다. 임의 JavaScript를 실행하지 않는다.
- 고정 수집원/목록 parser/원문 identity/HTTPS host·port·path/정확한 query 필드/다운로드 경로를 검증한다. 동적 host 확장 없음.
- 부분 발견 오류 후 정상 PDF/HWP/HWPX 보존, 미지원 JPG 미다운로드, 없음/발견 실패 구분,10파일 한도, 외부 host·HTTP·추가 query·잘못된 수집원 거부를 테스트했다.
- 실제 HTML fixture 검사, 기존 충청 공통 엔진 회귀, 본문 client, QA 카탈로그, inventory, snapshot, 파일 형식 검사 포함 Java219건 모두 통과했다. 조건부 생략0건이다.
- Node23건, `bootJar`, 화순 관측1건 성공. 전체 Gradle 명령1분53초. 전체 테스트 suite를 실행한 것은 아니다.
- 기존 migration·DB/API·UI·HWP 추출기·worker 설정·운영 정책은 변경하지 않았다.

```powershell
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_AUDIT='true'
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_RECEIPT='C:\PersonalProject\saneB\build\qa-results\target-inventory-20260928-receipt.txt'
$env:SANEB_HWASUN_SURVEY_FIXTURE='true'
.\gradlew.bat :test --tests '*HwasunDownloadContractTest' --tests '*ChungcheongFirstDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=HWASUN' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

실행 전 환경변수를 보관하고 finally에서 복원했다. 조사 원본 삭제 후 fixture 환경변수를 켜지 않는다. 조사3요청은 목록 GET302·V61 공식 검색 POST200·상세 GET200이다. 요청당15초/2MiB, TLS 검증·자동 redirect0을 유지했다. 실제 Java 관측은6요청/23MiB 상한 중 본문 예약 포함4회·2,239,522byte다. 본문 실제 요청0회이며 예약 상한을 실제 HTTP 호출 수로 표현하지 않는다. 조사 포함 예약 상한7회다.

운영 배포·정책 활성화·재분류·추출기 실행·운영 DB/API/UI 검증은 수행하지 않았다. 브라우저는 현재 명시적 실행 지시가 없어 사용자 정책상 생략했다. 커밋·푸시는 `[skip deploy]` 범위다.
