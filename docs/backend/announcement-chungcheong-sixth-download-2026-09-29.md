# 아산·서산 본문/첨부 연결과 HWPX 수집

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계다. 수집 가능한 파일을 먼저 확보하고 발견·전송·형식·파싱·본문 오류는 별도로 기록한다. 한 지역의 오류나 3표본 확보를 다음 지역 진행 조건으로 삼지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지, HWP 추출기 1.0.16 개선 보류를 유지한다.

- [x] 아산·서산 시스템 프로필 2개와 공식 제목/본문/첨부 경계 연결.
- [x] 아산·서산 HWPX 각 2개, 총 4개 472,140byte 다운로드와 signature/MIME/파일명 검증.
- [x] 천안 공식 목록 HTTP 403을 조사 오류로 기록하고 우회하지 않음.
- [x] Java 217통과·조건부 1생략, Node 23통과, bootJar·수집 전용 관측 성공.
- [x] 218영수증/174공고 재현, 기존 216영수증/172공고 보존.
- [x] 조사 원본 10개 917,940byte 길이·SHA256 대조 후 개별 삭제.
- [ ] 미등록 93지역 연결, 등록 후 다운로드 미확인 27지역 후속.
- [ ] 전국 목록 유입·상시 worker·추출·DB/API/UI·정책 승인·운영 E2E Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 이번 작업에서 운영 설정을 새로 조회하거나 변경하지 않았다. 비활성 보령·계룡은 임의 활성화하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 최소 1파일 다운로드 확인 | 101 | **103/223 (46.2%)** |
| 다운로드 미확인 | 122 | **120** |
| 프로필 등록 지역 | 128 | **130** |
| 미등록 지역 | 95 | **93** |
| 등록 후 다운로드 미확인 | 27 | **27** |
| 현재 프로필 hash와 일치하는 오류 관측 지역 | 32 | **32** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역 130+기업마당 1=131프로필, 카탈로그 186참조/129대상이다. 신규 기대값은 null, 기존 승인 기대값 1개를 유지한다. 오류 관측 수는 성공 파일이 공존하는 지역도 포함하므로 미확인 지역에 합산하지 않는다. 천안 목록 403은 프로필 등록 전 조사 오류이며 위 32지역에 중복 가산하지 않는다. 다운로드 비율은 전체 개발·운영 완료율이 아니다.

## 실제 관측

| 지역 / 공고 | 본문 | 첨부 결과 |
|---|---|---|
| 아산 LGS-000151 / 80727 | 432자, AVAILABLE | HWPX 2개 217,028byte·61,760byte 성공 |
| 서산 LGS-000152 / 65309 | 437자, AVAILABLE | HWPX 2개 87,288byte·106,064byte 성공 |
| 천안 LGS-000148 | 공식 목록 HTTP 403 | 상세·첨부 미실행, 미등록 유지 |

네 파일은 기존 production pinned transport와 signature/MIME/파일명 validator를 통과했다. HWPX 내부 전체 구조·텍스트 추출·분류·운영 DB 저장·관리자 검증 완료를 뜻하지 않는다. 파일 역할은 UNKNOWN이며 신청서/공고 역할을 임의 확정하지 않는다.

파일 byte hash:

- 아산 1: `b171fc03d599842d46712d26d8c9a12a843766568ed8d42f9b928f73991649aa`
- 아산 2: `57453c98b9d36acc3e0373516c48b155a336c700e9be98f83f67bfb70f73d18c`
- 서산 1: `c24442c58df11f2d0251ad75c984b31d794341a70bd2f924e627102be7a6c257`
- 서산 2: `34a5d13d305c8cdae989070f1009849da51cae2ca78ac2d26a5831f1ce625d17`

현재 profile hash: 아산 `a9e450ec622c3a5de15c1bd96dc310d2c1ebc6dee762330837f50dd7117a8e14`, 서산 `a5989157b581386797ef29803128e88791278d6c98c8d4bc7867ee6076ea1919`.

관측 producer class hash: `4b64077c60c364610cd20590d8a27baa7de3b83e37d914663e3fbd2d0b369a85`.

## 구현 계약 및 남은 한계

- `ChungcheongSixthNoticePage`: 아산은 `customContents > viewForm`의 제목, `ct_btn04` 첨부, `field-name-body > field-items` 본문을 분리한다. 서산은 공식 iframe 경로의 `form1 > table.bbs_default.view`에서 제목, `td.bbs_content[title=내용]`, `첨부파일` 라벨의 셀을 선택한다. 메뉴·푸터·담당자·파일명은 본문과 분리하고 본문의 표/지원제외 조건은 보존한다.
- 아산은 공식 `nnn` form의 HTTPS FileDown.jsp·POST·빈 hidden 세 필드를 검증한다. `goDownLoad`의 고정 문자열 세 개를 평문 form 값으로 전달하고 한글·공백·괄호·저장명의 `@`를 보존한다. 화면 크기 badge는 이름 비교에서만 분리한다.
- 서산의 공식 `goDownLoad`는 동일 세 인자를 FileDown.jsp GET query에 넣는다. 각 값을 URL 인코딩하여 전달하고 URL·host·path·필드·파일명·원본 경로·중복 query를 검증한다. 공식 미리보기는 동일 세 인자의 대응 여부만 확인하며 미리보기 서비스·페이지 스크립트는 호출하거나 실행하지 않는다.
- 아산 목록에서 `asan.go.kr` 상세 링크를 확인했고 `www.asan.go.kr` 동일 공고의 HTTP 200도 별도로 확인했다. 첨부 프로필은 두 관측 host를 허용하되 host 간 redirect는 허용하지 않는다. 이번 본문 관측은 등록 목록과 동일한 `www.asan.go.kr` 상세를 사용했다. **운영 목록이 비-www 상세를 전달하면 기존 동일-host 본문 제한에 의해 DETAIL_HOST_NOT_ALLOWED가 날 수 있다.** 별칭 자동 변환·운영 목록 설정 변경은 이번에 하지 않았고 단위 테스트로 기존 경계가 유지됨을 확인했다. 이 표본 성공으로 모든 목록 유입 경로의 본문 성공을 주장하지 않는다.
- 정상 descriptor와 미해결 링크를 함께 보존한다. 10파일·상세1MiB·파일20MiB·미지원 확장자 미다운로드·UNKNOWN 역할·SSRF/TLS/host 검증은 유지한다.
- 공통 프로필 인터페이스·fingerprint·validator·기존 migration·DB/API/UI·HWP 추출기·운영 정책·worker 설정은 변경하지 않았다. 신규 참조 카탈로그는 정책 승인 또는 운영 활성화 근거가 아니다.

## 요청량 및 정리

조사 GET 10회: 천안 목록 1회, 아산 목록/제목검색 2회/비-www·www 상세 총5회, 서산 목록/공식 iframe/제목검색/상세 총4회. 요청당 15초·연결7초·자동 redirect0·TLS 검증 유지. 초기4요청은 2MiB, 나머지6요청은 1MiB 상한이다. 천안 접근 거부는 반복 우회하지 않았다.

실제 Java 관측은 각 지역 6요청/23MiB 이내로 1회씩 실행했다. 본문 포함 실제 예약 합계는 10회·4,838,476byte, 조사 포함 예약 상한은 20회다. 예약과 실제 HTTP 호출 수는 같지 않다. 관측 임시 파일과 조사 원본 10개는 정리하고 비식별 결과 metadata만 유지한다. 일회성 Node·Gradle은 종료하며 기존 사용자 파일과 다른 프로세스는 보존한다.

## 실행 명령 / 결과 / 미검증

```powershell
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_AUDIT='true'
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_RECEIPT='C:\PersonalProject\saneB\build\qa-results\target-inventory-20260928-receipt.txt'
$env:SANEB_CHUNGCHEONG_SIXTH_SURVEY_FIXTURE='true'
.\gradlew.bat :test --tests '*ChungcheongSixthDownloadContractTest' --tests '*ChungcheongFifthDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=ASAN,SEOSAN' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

실행 전 환경변수를 보관하고 종료 시 복원했다. Gradle 1분42초 성공, 최종 JUnit 합계 218건 중 217통과·1조건부 생략, 실패/오류0. 생략은 삭제된 이전 금산·부여 원본 fixture를 요구하는 조건부 검증이며 이번 아산·서산 원본 fixture 검증은 실행했다. Node 23통과, 218영수증/174공고 재현 성공.

원본 fixture는 검증 후 삭제했으므로 재실행에는 신규 조사가 필요하다. 운영 설치·DB·정책·ENFORCE·재분류·상시 worker·배포·추출은 실행하지 않았다. 브라우저 검증은 현재 요청 정책에 따라 미실행이다. 전체 장기 goal은 진행 중이다.
