# 금산·부여 본문/첨부 연결 및 부여 HWPX 수집

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계다. 정상 파일은 먼저 수집하고 발견·전송·형식·파싱·본문 오류는 분리한다. 한 지역의 오류 해결이나 3표본 확보를 다음 지역 진행 조건으로 삼지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지, HWP 추출기 1.0.16 개선 보류를 유지한다.

- [x] 금산·부여 시스템 프로필 2개, 본문 경계, 공식 파일 연결 추가.
- [x] 부여 HWPX 2개 264,627byte 다운로드와 signature/MIME/파일명 검증.
- [x] 금산 본문·첨부 링크 2개 확보, 파일 형식 오류를 별도 기록.
- [x] Java 216통과·조건부 1생략, Node 23통과, bootJar·수집 전용 관측 태스크 통과.
- [x] 216영수증/172공고 재현, 기존 214영수증/170공고 보존.
- [x] 조사 원본 6개 4,292,687byte 길이·SHA256 대조 후 개별 삭제.
- [ ] 미등록 95지역 연결, 등록 후 다운로드 미확인 27지역 후속.
- [ ] 전국 목록 유입·상시 worker·추출·DB/API/UI·정책 승인·운영 E2E Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 이번 작업에서 운영 설정을 새로 조회하거나 변경하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 최소 1파일 다운로드 확인 | 100 | **101/223 (45.3%)** |
| 다운로드 미확인 | 123 | **122** |
| 프로필 등록 지역 | 126 | **128** |
| 미등록 지역 | 97 | **95** |
| 등록 후 다운로드 미확인 | 26 | **27** |
| 현재 프로필 hash와 일치하는 오류 관측 지역 | 31 | **32** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역 128+기업마당 1=129프로필, 카탈로그 184참조/127대상이다. 신규 기대값은 null, 기존 승인 기대값 1개를 유지한다. 성공 파일과 오류가 공존하는 지역도 있으므로 오류 관측 수를 미확인 지역 수에 합산하지 않는다. 다운로드 비율은 전체 개발·운영 완료율이 아니다.

## 실제 관측

| 지역 / 공고 | 본문 | 첨부 결과 |
|---|---|---|
| 금산 LGS-000156 / ea6a4e53c07c7f05a9e9240dbb006d43 | 265자, AVAILABLE | 링크 2개 FOUND, 두 파일 모두 FILE_SIGNATURE / ATTACHMENT_CONTENT_TYPE_MISMATCH |
| 부여 LGS-000157 / 2125452 | 828자, AVAILABLE | HWPX 2개 177,683byte·86,944byte 성공 |

금산은 HTTP 전송에서 131,940byte·96,901byte를 받았지만 MIME 검증을 통과하지 못했다. 수신 byte/hash 또는 파일명만으로 정상 HWPX라고 판정하지 않는다. 재요청·허용 형식 확대는 하지 않고 후속 형식 오류로 분리한다. 부여의 성공 파일은 기존 production pinned transport와 signature/MIME/파일명 validator를 통과했다. HWPX 내부 전체 구조·텍스트 추출·분류·운영 DB 저장·관리자 검증을 완료했다는 의미는 아니다. 파일 역할은 UNKNOWN이다.

부여 파일 byte hash:

- `2d392abca3d17bb507bdcef20b3f68201f93ad197607d283ab9730efdaefeba3`
- `7eb3ba999b7cd85b0566b28c16c325b0f896958d68974a7cb76e459a5365d791`

현재 profile hash:

- 금산: `9b03d6e9ca1520d84b6af3927a54797e454800ef3b571016b48e21920ca800a3`
- 부여: `80091b38b74952c8a14acb3d5d6df68b92848f2b789da09c0eb6b0b1a22f0090`
- 관측 producer class: `5e5870792496eeae8455d8cc8907e56e57292c1a2e2b9b190e0d43647d9f97dc`

## 구현 계약

- `ChungcheongFifthNoticePage`: 금산은 `program--contents > ui.bbs--view`의 제목·본문·첨부를 분리한다. 부여는 `section#con_body > div#txt`의 `board_viewTit`, `board_viewDetail`, `board_viewInfo > li.file` 중 파일 라벨 아래 영역만 사용한다. 메뉴·푸터·담당자·파일명은 본문에 섞지 않으며 본문 표와 지원제외 조건은 보존한다.
- 금산은 같은 공식 host의 `/_prog/download/`와 `func_gbn_cd=gosi`, `site_dvs_cd=kr`, 저장명·실제 파일명 네 파라미터만 GET으로 허용한다. 중복 query·다른 host·경로 조작·제어문자·파일명 불일치는 거부한다. 실제 상세 1,985,278byte에 맞춰 기존 선택형 `AttachmentDetailLimitProfile`의 2MiB 한도를 금산 프로필에 적용했다.
- 부여 공식 상세가 참조한 `/_prog/saeol_gosi/js/script.js`를 확인했다. `fn_saeol_downFile`은 `https://eminwon.buyeo.go.kr/emwp/jsp/ofr/FileDown.jsp`에 `user_file_nm`, `sys_file_nm`, `file_path`를 평문 form 값으로 POST한다. 다른 지역의 이중 인코딩을 적용하지 않는다. 공식 스크립트는 읽기만 했으며 실행하지 않는다.
- 부여의 세 고정 문자열만 기존 선형 호출 파서를 재사용해 읽는다. 파일명 내 이중 공백은 요청 값에서 보존하고 화면 라벨 비교에서만 공백을 정규화한다. 크기 badge와 동일 인자 미리보기는 다운로드 링크와 구분하고 미리보기는 요청하지 않는다. 임의 JavaScript·변경된 form·추가 요청 필드는 허용하지 않는다.
- 정상 descriptor는 다른 미해결 링크와 함께 보존한다. 10파일 제한·미지원 확장자 미다운로드·UNKNOWN 역할·SSRF/TLS/host 검증·요청/byte 상한은 유지한다.
- 기존 공통 프로필 인터페이스·fingerprint·validator·Flyway·DB/API/UI·HWP 추출기·운영 정책·worker 설정은 변경하지 않았다. 신규 코드와 참조 카탈로그는 운영 활성화 또는 정책 승인 근거가 아니다.

## 요청량 및 정리

조사 GET 6회: 금산 제목검색/상세 2회, 부여 목록/제목검색/상세/공식 참조 스크립트 4회. 요청당 15초·연결7초·자동 redirect0·TLS 검증 유지. 초기 4요청은 2MiB, 부여 상세/스크립트 2요청은 1MiB 상한이다.

실제 Java 관측은 금산 6요청/26MiB, 부여 6요청/23MiB 이내로 각 1회 실행했다. 본문 포함 실제 예약 합계는 10회·8,882,076byte, 조사 포함 예약 상한은 16회다. 예약과 실제 HTTP 호출 수는 동일하지 않다. 테스트 실패 중에는 외부 관측 태스크가 실행되지 않았다. 관측 임시 파일과 조사 원본 6개는 정리하고 비식별 결과 metadata만 유지한다.

## 실행 명령 / 결과 / 미검증

```powershell
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_AUDIT='true'
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_RECEIPT='C:\PersonalProject\saneB\build\qa-results\target-inventory-20260928-receipt.txt'
$env:SANEB_CHUNGCHEONG_FIFTH_SURVEY_FIXTURE='true'
.\gradlew.bat :test --tests '*ChungcheongFifthDownloadContractTest' --tests '*ChungcheongFourthDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=GEUMSAN,BUYEO' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

실행 전 환경변수 값을 보관하고 종료 시 복원했다. 첫 실행은 신규 caseCode의 대문자 규칙·본문 구조 오류 처리·제어문자 요청 생성 테스트 문제로 실패했다. 두 번째는 신규 카탈로그 2건을 반영하지 않은 snapshot 건수 기대값 1건이 실패했다. 오류를 수정한 세 번째 실행은 1분29초에 성공했다. 최종 JUnit 합계 217건 중 216통과·1조건부 생략, 실패/오류 0. 생략은 삭제된 이전 홍성·예산 원본 fixture를 요구하는 조건부 검증이며 이번 금산·부여 원본 fixture 검증은 실행했다. Node 23통과, 216영수증/172공고 재현 성공.

수집 전용 관측 태스크 성공은 부분 오류를 보존하는 실행이 끝났다는 뜻이다. 금산 파일 형식 오류가 해결되었다는 뜻이 아니다. 원본 fixture는 검증 후 삭제했으므로 재실행에는 신규 조사가 필요하다. 운영 설치·DB·정책·ENFORCE·재분류·상시 worker·배포·추출은 실행하지 않았다. 브라우저 검증은 현재 요청 정책에 따라 미실행이다. 전체 장기 goal은 진행 중이다.
